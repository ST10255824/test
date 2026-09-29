package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mackson.delivery.data.model.DriverProfile
import com.mackson.delivery.data.model.DriverTelemetry
import com.mackson.delivery.data.model.Order
import com.mackson.delivery.data.model.OrderStatus
import com.mackson.delivery.data.remote.FirebaseRealtimeService
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.ProductThumbnail
import com.mackson.delivery.ui.common.statusColor
import com.mackson.delivery.ui.common.statusLabel
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TrackingUiState(
    val order: Order? = null,
    val driverLocation: DriverTelemetry? = null,
    val driver: DriverProfile? = null
)

/**
 * Observer pattern in action (Part 1 section 9.3.12): both flows below are backed by Firestore
 * / Realtime Database snapshot listeners, so this screen redraws the instant the picker updates
 * order status or the driver app pushes a new GPS ping — no polling, no manual refresh.
 */
class TrackingViewModel(
    private val orderId: String,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository,
    private val realtimeService: FirebaseRealtimeService = FirebaseRealtimeService
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private var driverJob: kotlinx.coroutines.Job? = null
    private var loadedDriverId: String? = null

    fun start() {
        firestoreRepository.listenToOrderUpdates(orderId).onEach { order ->
            _uiState.value = _uiState.value.copy(order = order)
            val driverId = order?.driverId
            if (driverId != null && order.orderStatus == OrderStatus.EN_ROUTE && driverJob == null) {
                driverJob = realtimeService.observeDriverLocation(driverId).onEach { telemetry ->
                    _uiState.value = _uiState.value.copy(driverLocation = telemetry)
                }.launchIn(viewModelScope)
            }
            // Fetched once a driver is assigned so the customer can verify them at the door —
            // WIL group requirement: name, photo and license plate shown for verification.
            if (driverId != null && driverId != loadedDriverId) {
                loadedDriverId = driverId
                viewModelScope.launch {
                    val profile = com.mackson.delivery.util.resultOf { firestoreRepository.fetchDriverProfile(driverId) }
                    if (profile is com.mackson.delivery.util.AppResult.Success) {
                        _uiState.value = _uiState.value.copy(driver = profile.data)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}

@Composable
fun TrackingScreen(orderId: String, onOpenChat: () -> Unit) {
    val viewModel: TrackingViewModel = viewModel(factory = viewModelFactory { TrackingViewModel(orderId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }
    if (!started) {
        viewModel.start()
        started = true
    }

    val order = uiState.order

    if (order == null) {
        FullScreenLoading()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Order #${orderId.takeLast(6)}", style = MaterialTheme.typography.headlineMedium)

        Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            com.mackson.delivery.ui.common.StatusChip(
                text = statusLabel(order.orderStatus),
                color = statusColor(order.orderStatus),
                modifier = Modifier.padding(16.dp)
            )
        }

        uiState.driver?.let { driver ->
            if (order.orderStatus == OrderStatus.EN_ROUTE || order.orderStatus == OrderStatus.ARRIVED_AT_NODE) {
                Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        ProductThumbnail(imageUrl = driver.photoUrl, modifier = Modifier.size(56.dp))
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(driver.driverName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${driver.vehicleType}${if (driver.licensePlate.isNotBlank()) " · ${driver.licensePlate}" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Verify this matches your driver before accepting delivery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (order.orderStatus == OrderStatus.EN_ROUTE) {
            val driverLatLng = uiState.driverLocation?.let { LatLng(it.latitude, it.longitude) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(driverLatLng ?: LatLng(-26.2041, 28.0473), 14f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 16.dp),
                cameraPositionState = cameraPositionState
            ) {
                driverLatLng?.let {
                    Marker(state = MarkerState(position = it), title = "Your driver")
                }
            }
            Text(
                "Live location updates every 5 seconds while your order is en route.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        StatusTimeline(order.orderStatus)

        if (order.items.any { it.isSubstituted && it.substitutionApproved == null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your picker has a substitution to discuss", style = MaterialTheme.typography.titleMedium)
                    com.mackson.delivery.ui.common.PrimaryButton("Open chat", onClick = onOpenChat)
                }
            }
        }
    }
}

private val timelineSteps = listOf(
    OrderStatus.RECEIVED, OrderStatus.PICKING, OrderStatus.READY_FOR_COLLECTION,
    OrderStatus.EN_ROUTE, OrderStatus.ARRIVED_AT_NODE, OrderStatus.DELIVERED
)

@Composable
private fun StatusTimeline(current: OrderStatus) {
    val currentIndex = timelineSteps.indexOf(current)
    Column(modifier = Modifier.padding(top = 16.dp)) {
        timelineSteps.forEachIndexed { index, step ->
            val reached = index <= currentIndex
            Text(
                text = (if (reached) "● " else "○ ") + statusLabel(step),
                color = if (reached) statusColor(step) else Color.Gray,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
