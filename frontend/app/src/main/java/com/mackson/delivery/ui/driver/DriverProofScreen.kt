package com.mackson.delivery.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.Order
import com.mackson.delivery.data.model.OrderItem
import com.mackson.delivery.data.model.ProofOfDeliveryMethod
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.AccentButton
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.ProductThumbnail
import com.mackson.delivery.ui.theme.StatusSuccessGreen
import com.mackson.delivery.ui.picker.BarcodeScanScreen
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ProofUiState(
    val order: Order? = null,
    val deliveryProgress: Map<String, Boolean> = emptyMap(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val confirmed: Boolean = false
) {
    val allItemsChecked: Boolean
        get() = order?.items?.all { deliveryProgress[it.productId] == true } ?: false
}

/**
 * Part 1 US-22: "order status cannot clear to Delivered until the customer token handshakes
 * successfully with the driver's app." The OTP/QR comparison this needs was meant to run
 * server-side in the confirmProofOfDelivery Cloud Function so the driver's device could never
 * unilaterally mark an order Delivered — see FirestoreRepository.confirmProofOfDelivery for why
 * it now runs client-side instead (that Function is written but undeployed, no Blaze plan).
 *
 * Also gates that handshake behind scanning every basket item off (WIL group requirement:
 * "driver must have a barcode scanner similar to the in store picker that automatically ticks
 * off the items in the basket... and that there wasn't any mishaps") — mirrors
 * PickerOrderDetailViewModel's pickProgress pattern, just for delivery instead of picking.
 */
class DriverProofViewModel(
    private val orderId: String,
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProofUiState())
    val uiState: StateFlow<ProofUiState> = _uiState.asStateFlow()

    fun start() {
        combine(
            repository.listenToOrderUpdates(orderId),
            repository.observeDeliveryProgress(orderId)
        ) { order, progress -> _uiState.value.copy(order = order, deliveryProgress = progress) }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun checkItem(item: OrderItem, scannedBarcode: String): Boolean {
        if (scannedBarcode != item.barcode) return false
        viewModelScope.launch { repository.markItemDelivered(orderId, item.productId, true) }
        return true
    }

    fun markItemDelivered(productId: String, delivered: Boolean) {
        viewModelScope.launch { repository.markItemDelivered(orderId, productId, delivered) }
    }

    fun markAllItemsDelivered() {
        val items = _uiState.value.order?.items ?: return
        val productIds = items.map { it.productId }
        viewModelScope.launch { repository.markAllItemsDelivered(orderId, productIds) }
    }

    fun confirm(method: ProofOfDeliveryMethod, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            when (val result = resultOf { repository.confirmProofOfDelivery(orderId, method.name, code) }) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(isSubmitting = false, confirmed = true)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
fun DriverProofScreen(orderId: String, onConfirmed: () -> Unit) {
    val viewModel: DriverProofViewModel = viewModel(factory = viewModelFactory { DriverProofViewModel(orderId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }
    if (!started) {
        viewModel.start()
        started = true
    }

    var useQrScan by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var scanningProductId by remember { mutableStateOf<String?>(null) }
    var scanOutcome by remember { mutableStateOf<String?>(null) }

    if (uiState.confirmed) {
        onConfirmed()
        return
    }
    if (uiState.isSubmitting) {
        FullScreenLoading()
        return
    }

    val order = uiState.order ?: run {
        FullScreenLoading()
        return
    }

    scanningProductId?.let { productId ->
        val item = order.items.first { it.productId == productId }
        Column(modifier = Modifier.fillMaxSize()) {
            BarcodeScanScreen(
                instructionText = "Scan ${item.name} to confirm it's in the basket",
                onBarcodeDetected = { code ->
                    if (viewModel.checkItem(item, code)) {
                        scanningProductId = null
                        scanOutcome = null
                    } else {
                        scanOutcome = "That barcode doesn't match ${item.name}. Try again."
                    }
                },
                extraContent = {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        com.mackson.delivery.ui.common.PrimaryButton(
                            text = "Mark item present (Skip scan)",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.markItemDelivered(item.productId, true)
                            scanningProductId = null
                            scanOutcome = null
                        }
                        scanOutcome?.let { ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                    }
                }
            )
        }
        return
    }

    if (useQrScan) {
        BarcodeScanScreen(
            instructionText = "Scan the QR code shown in the customer's app",
            onBarcodeDetected = { code -> viewModel.confirm(ProofOfDeliveryMethod.QR_CODE, code) }
        )
        return
    }

    if (!uiState.allItemsChecked) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Check off the basket", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Scan every item before confirming delivery, so nothing goes missing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                TextButton(
                    onClick = { viewModel.markAllItemsDelivered() }
                ) {
                    Text("Mark all present ✓")
                }
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                items(order.items, key = { it.productId }) { item ->
                    val checked = uiState.deliveryProgress[item.productId] == true
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProductThumbnail(imageUrl = item.imageUrl, modifier = Modifier.size(48.dp))
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("Qty ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!checked) {
                                        OutlinedButton(
                                            onClick = { scanningProductId = item.productId }
                                        ) { Text("Scan") }
                                    }
                                    TextButton(
                                        onClick = { viewModel.markItemDelivered(item.productId, !checked) }
                                    ) { Text(if (checked) "Uncheck" else "Mark present ✓") }
                                }
                            }
                            Icon(
                                if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (checked) "Checked" else "Not checked",
                                tint = if (checked) StatusSuccessGreen else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Confirm delivery", style = MaterialTheme.typography.headlineMedium)
        Text(
            "All items checked ✓ — now hand over the order.",
            style = MaterialTheme.typography.bodyMedium,
            color = StatusSuccessGreen,
            modifier = Modifier.padding(top = 4.dp)
        )
        uiState.errorMessage?.let { ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }

        OutlinedTextField(
            value = otpCode,
            onValueChange = { otpCode = it },
            label = { Text("Customer's delivery code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        AccentButton(
            "Confirm with code",
            enabled = otpCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) { viewModel.confirm(ProofOfDeliveryMethod.OTP, otpCode) }

        TextButton(onClick = { useQrScan = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Scan QR code instead")
        }
    }
}
