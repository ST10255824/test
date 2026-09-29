package com.mackson.delivery.ui.driver

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mackson.delivery.data.model.Order
import com.mackson.delivery.data.model.OrderStatus
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirebaseRealtimeService
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.common.statusLabel
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Manual status milestones + turn-by-turn nav handoff — Part 1 US-20/US-21. */
class DriverActiveRunViewModel(
    private val orderId: String,
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    fun start() {
        repository.listenToOrderUpdates(orderId).onEach { _order.value = it }.launchIn(viewModelScope)
    }

    fun advanceStatus() {
        val current = _order.value?.orderStatus ?: return
        val next = current.nextForDriver() ?: return
        viewModelScope.launch {
            val extra = if (next == OrderStatus.DELIVERED) mapOf("deliveredTime" to System.currentTimeMillis()) else emptyMap()
            repository.updateOrderStatus(orderId, next, extra)
        }
    }
}

@Composable
fun DriverActiveRunScreen(orderId: String, onNeedsProofOfDelivery: () -> Unit) {
    val viewModel: DriverActiveRunViewModel = viewModel(factory = viewModelFactory { DriverActiveRunViewModel(orderId) })
    val order by viewModel.order.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }
    if (!started) {
        viewModel.start()
        started = true
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Streams a GPS ping roughly every 5 seconds while the run is EN_ROUTE (Part 1 non-functional
    // requirement). FusedLocationProviderClient is the standard, battery-efficient Android API
    // for this rather than raw LocationManager polling.
    val driverId = FirebaseAuthManager.getCurrentUserId()
    DisposableEffect(order?.orderStatus, driverId) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        var callback: LocationCallback? = null
        if (order?.orderStatus == OrderStatus.EN_ROUTE && driverId != null) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L).build()
            callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    coroutineScope.launch {
                        FirebaseRealtimeService.streamDriverLocation(driverId, orderId, location.latitude, location.longitude)
                    }
                }
            }
            try {
                client.requestLocationUpdates(request, callback, context.mainLooper)
            } catch (_: SecurityException) {
                // Location permission not granted yet — MainActivity requests it for driver accounts.
            }
        }
        onDispose { callback?.let { client.removeLocationUpdates(it) } }
    }

    val current = order ?: run {
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
        Text(statusLabel(current.orderStatus), style = MaterialTheme.typography.titleLarge)

        current.deliveryAddress?.let { address ->
            Text(address.formattedAddress, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            PrimaryButton("Navigate", modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                val gmmIntentUri = Uri.parse("google.navigation:q=${address.latitude},${address.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply { setPackage("com.google.android.apps.maps") }
                ContextCompat.startActivity(context, mapIntent, null)
            }
        }

        // WIL group requirement: a driver who accepts a run "has to show it to admin in order
        // to go on delivery" — the order's own short ID stands in for that unique identifier,
        // since it's already what every screen (and the physical picking slip) labels the
        // order with. Store staff match it against the physical order at the counter, then tap
        // "Confirm dispatch" in the admin console (admin-web/src/pages/OrdersPage.tsx), which
        // gates this button the same way confirmDispatch gates it below.
        if (current.orderStatus == OrderStatus.READY_FOR_COLLECTION && !current.dispatchConfirmedByAdmin) {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Show this code to staff before leaving", style = MaterialTheme.typography.titleMedium)
                    Text(
                        orderId.takeLast(6).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Waiting for staff to confirm this matches the physical order…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        val nextLabel = when (current.orderStatus) {
            OrderStatus.READY_FOR_COLLECTION -> "Mark as collected"
            OrderStatus.EN_ROUTE -> "Mark as arrived"
            OrderStatus.ARRIVED_AT_NODE -> "Confirm proof of delivery"
            else -> null
        }
        val canAdvance = current.orderStatus != OrderStatus.READY_FOR_COLLECTION || current.dispatchConfirmedByAdmin

        nextLabel?.let { label ->
            com.mackson.delivery.ui.common.AccentButton(
                label,
                enabled = canAdvance,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                if (current.orderStatus == OrderStatus.ARRIVED_AT_NODE) {
                    onNeedsProofOfDelivery()
                } else {
                    viewModel.advanceStatus()
                }
            }
        }
    }
}
