package com.mackson.delivery.ui.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavyDeep
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DriverJobsUiState(
    val isLoading: Boolean = true,
    val jobs: List<Order> = emptyList(),
    val errorMessage: String? = null,
    val acceptedOrderId: String? = null
)

/** Proximity dashboard of available runs — Part 1 US-19. */
class DriverJobsViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverJobsUiState())
    val uiState: StateFlow<DriverJobsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val driverId = FirebaseAuthManager.getCurrentUserId()
        viewModelScope.launch {
            // Resume an already-accepted, in-progress run first — otherwise a driver who left
            // that screen mid-delivery would see an empty jobs list with no way back into it.
            val active = driverId?.let { resultOf { firestoreRepository.fetchActiveRunForDriver(it) } }
            if (active is AppResult.Success && active.data != null) {
                _uiState.value = _uiState.value.copy(isLoading = false, acceptedOrderId = active.data.orderId)
                return@launch
            }
            firestoreRepository.listenToAvailableRuns()
                .onEach { jobs -> _uiState.value = _uiState.value.copy(isLoading = false, jobs = jobs) }
                .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message) }
                .launchIn(viewModelScope)
        }
    }

    fun acceptRun(order: Order) {
        val driverId = FirebaseAuthManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            when (resultOf { firestoreRepository.acceptDeliveryRun(order.orderId, driverId) }) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(acceptedOrderId = order.orderId)
                is AppResult.Error -> {} // someone else likely took it first — the live listener already refreshes the list
            }
        }
    }
}

@Composable
fun DriverJobsScreen(onRunAccepted: (String) -> Unit) {
    val viewModel: DriverJobsViewModel = viewModel(factory = viewModelFactory { DriverJobsViewModel() })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    uiState.acceptedOrderId?.let {
        onRunAccepted(it)
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Available runs", style = MaterialTheme.typography.headlineMedium)
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.errorMessage != null -> ErrorBanner(uiState.errorMessage!!)
            uiState.jobs.isEmpty() -> Text(
                "No runs ready for collection right now.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
            else -> LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                items(uiState.jobs, key = { it.orderId }) { job ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Order #${job.orderId.takeLast(6)}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${job.items.sumOf { it.quantity }} items",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MacksonGold)
                                ) {
                                    Text(
                                        "Tip R${"%.2f".format(job.driverTip)}",
                                        color = MacksonNavyDeep,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            PrimaryButton("Accept run", modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                viewModel.acceptRun(job)
                            }
                        }
                    }
                }
            }
        }
    }
}
