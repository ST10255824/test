package com.mackson.delivery.ui.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.Order
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManifestUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null
)

/** Digital order manifests structured by aisle — Part 1 US-15. */
class PickerManifestViewModel(private val repository: FirestoreRepository = FirestoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ManifestUiState())
    val uiState: StateFlow<ManifestUiState> = _uiState.asStateFlow()

    fun load() {
        val shopperId = FirebaseAuthManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = resultOf { repository.fetchPickerManifest(shopperId) }) {
                is AppResult.Success -> _uiState.value = ManifestUiState(isLoading = false, orders = result.data)
                is AppResult.Error -> _uiState.value = ManifestUiState(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

@Composable
fun PickerManifestScreen(onOrderClick: (Order) -> Unit) {
    val viewModel: PickerManifestViewModel = viewModel(factory = viewModelFactory { PickerManifestViewModel() })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your manifest", style = MaterialTheme.typography.headlineMedium)
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.errorMessage != null -> ErrorBanner(uiState.errorMessage!!)
            uiState.orders.isEmpty() -> Text(
                "No orders assigned right now.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                items(uiState.orders, key = { it.orderId }) { order ->
                    Card(onClick = { onOrderClick(order) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Order #${order.orderId.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                            Text("${order.items.sumOf { it.quantity }} items · ${order.deliverySlotLabel}")
                        }
                    }
                }
            }
        }
    }
}
