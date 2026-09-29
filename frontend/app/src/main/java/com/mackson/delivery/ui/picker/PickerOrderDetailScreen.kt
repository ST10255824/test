package com.mackson.delivery.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.mackson.delivery.ui.common.ProductThumbnail
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import com.mackson.delivery.data.model.OrderStatus
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.theme.StatusSuccessGreen
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PickerOrderDetailUiState(
    val order: Order? = null,
    val pickProgress: Map<String, Boolean> = emptyMap()
) {
    val allResolved: Boolean
        get() = order?.items?.all { pickProgress[it.productId] == true || it.isSubstituted } ?: false
}

/** Part 1 US-16/US-18: scan-to-pick and stage-for-collection. */
class PickerOrderDetailViewModel(
    private val orderId: String,
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PickerOrderDetailUiState())
    val uiState: StateFlow<PickerOrderDetailUiState> = _uiState.asStateFlow()

    fun start() {
        combine(
            repository.listenToOrderUpdates(orderId),
            repository.observePickProgress(orderId)
        ) { order, progress -> PickerOrderDetailUiState(order, progress) }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun markItemPicked(productId: String, picked: Boolean) {
        viewModelScope.launch { repository.markItemPicked(orderId, productId, picked) }
    }

    fun markAllItemsPicked() {
        val items = _uiState.value.order?.items ?: return
        val productIds = items.map { it.productId }
        viewModelScope.launch { repository.markAllItemsPicked(orderId, productIds) }
    }

    fun stageOrder() {
        viewModelScope.launch { repository.updateOrderStatus(orderId, OrderStatus.READY_FOR_COLLECTION) }
    }
}

@Composable
fun PickerOrderDetailScreen(
    orderId: String,
    onScanToPick: (OrderItem) -> Unit,
    onFlagSubstitution: (OrderItem) -> Unit,
    onStaged: () -> Unit
) {
    val viewModel: PickerOrderDetailViewModel =
        viewModel(factory = viewModelFactory { PickerOrderDetailViewModel(orderId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var started by remember { mutableStateOf(false) }
    if (!started) {
        viewModel.start()
        started = true
    }

    val order = uiState.order ?: return
    val sortedItems = order.items.sortedBy { it.aisleNumber }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Order #${orderId.takeLast(6)}", style = MaterialTheme.typography.headlineMedium)
                Text("Sorted by aisle to minimise walking distance", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(
                onClick = { viewModel.markAllItemsPicked() },
                enabled = !uiState.allResolved
            ) {
                Text("Mark all present ✓")
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            items(sortedItems, key = { it.productId }) { item ->
                val picked = uiState.pickProgress[item.productId] == true
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProductThumbnail(imageUrl = item.imageUrl, modifier = Modifier.size(52.dp))
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "A${item.aisleNumber}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Qty ${item.quantity}" + if (item.isSubstituted) " · Substituted" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.barcode.isNotBlank()) {
                                Text(
                                    "Barcode ${item.barcode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!item.isSubstituted) {
                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(onClick = { onScanToPick(item) }) {
                                        Text("Scan")
                                    }
                                    TextButton(
                                        onClick = { viewModel.markItemPicked(item.productId, !picked) }
                                    ) {
                                        Text(if (picked) "Unpick" else "Mark present ✓")
                                    }
                                    OutlinedButton(onClick = { onFlagSubstitution(item) }) {
                                        Text("Out of stock")
                                    }
                                }
                            }
                        }
                        Icon(
                            if (picked || item.isSubstituted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (picked) "Picked" else "Not picked",
                            tint = if (picked || item.isSubstituted) StatusSuccessGreen else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        com.mackson.delivery.ui.common.AccentButton(
            text = "Stage for collection",
            enabled = uiState.allResolved,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            viewModel.stageOrder()
            onStaged()
        }
    }
}
