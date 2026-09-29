package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.mackson.delivery.ui.common.statusColor
import com.mackson.delivery.ui.common.statusLabel
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null
)

class HistoryViewModel(private val repository: FirestoreRepository = FirestoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun load() {
        val uid = FirebaseAuthManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = resultOf { repository.fetchOrderHistory(uid) }) {
                is AppResult.Success -> _uiState.value = HistoryUiState(isLoading = false, orders = result.data)
                is AppResult.Error -> _uiState.value = HistoryUiState(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

@Composable
fun HistoryScreen(onOrderClick: (Order) -> Unit) {
    val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory { HistoryViewModel() })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Order history", style = MaterialTheme.typography.headlineMedium)
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.errorMessage != null -> ErrorBanner(uiState.errorMessage!!)
            uiState.orders.isEmpty() -> Text(
                "No past orders yet.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                items(uiState.orders, key = { it.orderId }) { order ->
                    Card(onClick = { onOrderClick(order) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Order #${order.orderId.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                                Text(statusLabel(order.orderStatus), color = statusColor(order.orderStatus))
                            }
                            Text("R${"%.2f".format(order.totalAmount)}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
