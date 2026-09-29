package com.mackson.delivery.ui.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.mackson.delivery.data.model.SubstitutionMessage
import com.mackson.delivery.data.model.SubstitutionSenderRole
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirebaseRealtimeService
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScanMode { PICK, SUB }

data class ScanRouteUiState(
    val order: Order? = null,
    val outcomeMessage: String? = null,
    val done: Boolean = false
)

class PickerScanViewModel(
    private val orderId: String,
    private val mode: ScanMode,
    private val targetProductId: String,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanRouteUiState())
    val uiState: StateFlow<ScanRouteUiState> = _uiState.asStateFlow()

    fun loadOrder() {
        viewModelScope.launch {
            firestoreRepository.listenToOrderUpdates(orderId).collect { order ->
                if (order != null && _uiState.value.order == null) {
                    _uiState.value = _uiState.value.copy(order = order)
                }
            }
        }
    }

    fun markPresentWithoutScan() {
        val order = _uiState.value.order ?: return
        val targetItem = order.items.find { it.productId == targetProductId } ?: return
        viewModelScope.launch {
            resultOf { firestoreRepository.markItemPicked(orderId, targetProductId, true) }
            _uiState.value = _uiState.value.copy(
                outcomeMessage = "${targetItem.name} marked as picked",
                done = true
            )
        }
    }

    fun onBarcodeScanned(code: String) {
        val order = _uiState.value.order ?: return
        val targetItem = order.items.find { it.productId == targetProductId } ?: return
        viewModelScope.launch {
            when (mode) {
                ScanMode.PICK -> {
                    if (code == targetItem.barcode) {
                        resultOf { firestoreRepository.markItemPicked(orderId, targetProductId, true) }
                        _uiState.value = _uiState.value.copy(outcomeMessage = "${targetItem.name} marked as picked", done = true)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            outcomeMessage = "That barcode doesn't match ${targetItem.name}. Try again."
                        )
                    }
                }
                ScanMode.SUB -> {
                    val replacement = resultOf { firestoreRepository.fetchProductByBarcode(order.storeId, code) }
                    when (replacement) {
                        is AppResult.Success -> {
                            val product = replacement.data
                            if (product == null) {
                                _uiState.value = _uiState.value.copy(outcomeMessage = "No product found for that barcode.")
                            } else {
                                val pickerId = FirebaseAuthManager.getCurrentUserId()
                                resultOf { firestoreRepository.proposeSubstitution(orderId, targetProductId, product.productId) }
                                if (pickerId != null) {
                                    FirebaseRealtimeService.sendLiveChatMessage(
                                        orderId,
                                        SubstitutionMessage(
                                            senderId = pickerId,
                                            senderRole = SubstitutionSenderRole.PICKER,
                                            timestamp = System.currentTimeMillis(),
                                            payloadText = "${product.name} is out of stock. Would you like ${product.name} instead?",
                                            proposedProductId = product.productId
                                        )
                                    )
                                }
                                _uiState.value = _uiState.value.copy(
                                    outcomeMessage = "Substitution with ${product.name} sent to the customer for approval.",
                                    done = true
                                )
                            }
                        }
                        is AppResult.Error -> _uiState.value = _uiState.value.copy(outcomeMessage = replacement.message)
                    }
                }
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
fun PickerScanRoute(orderId: String, mode: ScanMode, targetProductId: String, onDone: () -> Unit) {
    val viewModel: PickerScanViewModel =
        viewModel(factory = viewModelFactory { PickerScanViewModel(orderId, mode, targetProductId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadOrder() }

    if (uiState.done) {
        onDone()
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BarcodeScanScreen(
            instructionText = if (mode == ScanMode.PICK) "Scan the item's barcode to confirm the pick" else "Scan the replacement item's barcode",
            onBarcodeDetected = viewModel::onBarcodeScanned,
            extraContent = {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (mode == ScanMode.PICK) {
                        PrimaryButton(
                            text = "Mark item present (Skip scan)",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.markPresentWithoutScan()
                        }
                    }
                    uiState.outcomeMessage?.let {
                        ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
        )
    }
}
