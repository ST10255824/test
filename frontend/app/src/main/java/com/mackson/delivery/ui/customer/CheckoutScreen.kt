package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.DeliveryAddress
import com.mackson.delivery.data.model.DeliverySlotType
import com.mackson.delivery.data.model.StoreNode
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.data.repository.CartStore
import com.mackson.delivery.domain.CheckoutCalculator
import com.mackson.delivery.domain.DeliverySlot
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val placedOrderId: String? = null,
    val store: StoreNode? = null,
    val savedAddresses: List<DeliveryAddress> = emptyList()
)

/** Runs the atomic checkout transaction — the stock-decrement path (Part 1 section 2.3). See
 * FirestoreRepository.checkoutOrder for why this runs client-side instead of through the
 * (undeployed) checkoutOrder Cloud Function. */
class CheckoutViewModel(
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun loadStore(storeId: String) {
        viewModelScope.launch {
            when (val result = resultOf { repository.fetchStore(storeId) }) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(store = result.data)
                is AppResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
        }
    }

    fun loadSavedAddresses() {
        val uid = FirebaseAuthManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            when (val result = resultOf { repository.fetchCustomerProfile(uid) }) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(savedAddresses = result.data?.savedAddresses ?: emptyList())
                is AppResult.Error -> {} // non-fatal — the address search screen still works without this
            }
        }
    }

    /** Upserts a delivery address into the customer's profile for one-tap reuse next time —
     * matches the "saved addresses" list in the Part 1 prototype's own address screen. Dedupes
     * on formattedAddress so re-selecting an already-saved address (or re-searching the same
     * one) doesn't pile up duplicates or write anything. */
    fun saveAddressForReuse(address: DeliveryAddress) {
        if (_uiState.value.savedAddresses.any { it.formattedAddress == address.formattedAddress }) return
        val uid = FirebaseAuthManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val existing = resultOf { repository.fetchCustomerProfile(uid) }
            val customer = (existing as? AppResult.Success)?.data ?: return@launch
            val updatedAddresses = customer.savedAddresses + address
            val updated = customer.copy(
                savedAddresses = updatedAddresses,
                defaultAddressId = customer.defaultAddressId ?: address.addressId
            )
            resultOf { repository.saveCustomerProfile(updated) }
            _uiState.value = _uiState.value.copy(savedAddresses = updatedAddresses)
        }
    }

    fun submitOrder(
        storeId: String,
        slotType: DeliverySlotType,
        slotLabel: String,
        hasLoyaltyCard: Boolean,
        tip: Double,
        deliveryAddress: DeliveryAddress
    ) {
        val cart = CartStore.cart.value
        if (cart.items.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            when (
                val result = resultOf {
                    repository.checkoutOrder(storeId, cart.items, slotType, slotLabel, hasLoyaltyCard, tip, deliveryAddress)
                }
            ) {
                is AppResult.Success -> {
                    CartStore.clear()
                    _uiState.value = _uiState.value.copy(isSubmitting = false, placedOrderId = result.data)
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isSubmitting = false, errorMessage = result.message)
            }
        }
    }
}

private val demoSlots = listOf(
    DeliverySlot("As soon as possible (rolling)", capacity = 999, bookedCount = 0),
    DeliverySlot("17:00 - 18:00", capacity = 20, bookedCount = 18),
    DeliverySlot("18:00 - 19:00", capacity = 20, bookedCount = 20),
    DeliverySlot("19:00 - 20:00", capacity = 20, bookedCount = 4)
)

/** Part 1 US-09, US-10, US-11, US-12: delivery slot, loyalty, tip, tokenised payment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(storeId: String, onOrderPlaced: (String) -> Unit) {
    val viewModel: CheckoutViewModel = viewModel(factory = viewModelFactory { CheckoutViewModel() })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cart by CartStore.cart.collectAsStateWithLifecycle()

    LaunchedEffect(storeId) {
        viewModel.loadStore(storeId)
        viewModel.loadSavedAddresses()
    }

    var selectedSlot by remember { mutableStateOf(demoSlots.first()) }
    var loyaltyLinked by remember { mutableStateOf(true) }
    var tipText by remember { mutableStateOf("0") }
    var showPayment by remember { mutableStateOf(false) }
    var deliveryAddress by remember { mutableStateOf<DeliveryAddress?>(null) }
    val tip = tipText.toDoubleOrNull() ?: 0.0

    val breakdown = remember(cart, loyaltyLinked, tip) {
        CheckoutCalculator.calculate(cart, loyaltyLinked, driverTip = tip)
    }

    uiState.placedOrderId?.let {
        onOrderPlaced(it)
        return
    }

    val store = uiState.store
    if (store == null) {
        FullScreenLoading()
        return
    }

    val address = deliveryAddress
    if (address == null) {
        AddressSearchScreen(
            storeLatitude = store.latitude,
            storeLongitude = store.longitude,
            storeServiceRadiusKm = store.serviceRadiusKm,
            savedAddresses = uiState.savedAddresses
        ) {
            deliveryAddress = it
            viewModel.saveAddressForReuse(it)
        }
        return
    }

    if (showPayment) {
        PaymentScreen(
            totalAmount = breakdown.total,
            isProcessing = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onBack = { showPayment = false }
        ) {
            viewModel.submitOrder(
                storeId = storeId,
                slotType = if (selectedSlot == demoSlots.first()) DeliverySlotType.IMMEDIATE_ROLLING else DeliverySlotType.SCHEDULED_HOURLY,
                slotLabel = selectedSlot.label,
                hasLoyaltyCard = loyaltyLinked,
                tip = tip,
                deliveryAddress = address
            )
        }
        return
    }

    if (uiState.isSubmitting) {
        FullScreenLoading()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Checkout", style = MaterialTheme.typography.headlineMedium)
        uiState.errorMessage?.let { ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }

        Text("Delivery address", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                address.formattedAddress,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            androidx.compose.material3.TextButton(onClick = { deliveryAddress = null }) { Text("Change") }
        }

        Text("Delivery slot", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        LazyRow {
            items(demoSlots) { slot ->
                FilterChip(
                    selected = slot == selectedSlot,
                    enabled = !slot.isFull,
                    onClick = { selectedSlot = slot },
                    label = { Text(if (slot.isFull) "${slot.label} (full)" else slot.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Checkbox(checked = loyaltyLinked, onCheckedChange = { loyaltyLinked = it })
            Text("Apply my Mackson's Loyalty Card discount")
        }

        OutlinedTextField(
            value = tipText,
            onValueChange = { tipText = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Driver tip (R)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        SummaryLine("Subtotal", breakdown.subtotal)
        if (breakdown.loyaltyDiscount > 0) SummaryLine("Loyalty discount", -breakdown.loyaltyDiscount)
        SummaryLine("Service fee", breakdown.serviceFee)
        if (breakdown.driverTip > 0) SummaryLine("Driver tip", breakdown.driverTip)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SummaryLine("Total", breakdown.total, emphasise = true)

        Text(
            "Card details are handled by our PCI-DSS Level 1 payment partner and never stored on this device.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )

        PrimaryButton(
            text = "Pay R${"%.2f".format(breakdown.total)} and place order",
            enabled = cart.items.isNotEmpty() && FirebaseAuthManager.isSignedIn(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { showPayment = true }
    }
}

@Composable
private fun SummaryLine(label: String, amount: Double, emphasise: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text(
            "${if (amount < 0) "-" else ""}R${"%.2f".format(kotlin.math.abs(amount))}",
            style = if (emphasise) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
        )
    }
}
