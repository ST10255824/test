package com.mackson.delivery.ui.customer

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mackson.delivery.data.repository.CartStore
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavyDeep

/** Part 1 US-07: responsive cart that updates totals instantly, no page refresh. */
@Composable
fun CartScreen(onCheckout: () -> Unit, onContinueShopping: () -> Unit) {
    val cart by CartStore.cart.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Your cart", style = MaterialTheme.typography.headlineMedium)

        if (cart.items.isEmpty()) {
            Text(
                "Your cart is empty.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
            PrimaryButton("Continue shopping", modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                onContinueShopping()
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            items(cart.items, key = { it.productId }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.mackson.delivery.ui.common.ProductThumbnail(
                            imageUrl = item.imageUrl,
                            modifier = Modifier.size(52.dp)
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "R${"%.2f".format(item.unitPrice)} each",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    QuantityStepper(
                        quantity = item.quantity,
                        canIncrement = item.quantity < item.maxAvailable,
                        onDecrement = { CartStore.setQuantity(item.productId, item.quantity - 1) },
                        onIncrement = { CartStore.setQuantity(item.productId, item.quantity + 1) }
                    )
                }
                HorizontalDivider()
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", style = MaterialTheme.typography.titleMedium)
                Text("R${"%.2f".format(cart.subtotal)}", style = MaterialTheme.typography.titleMedium)
            }
        }

        PrimaryButton("Checkout", modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { onCheckout() }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, canIncrement: Boolean, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(percent = 50))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton("−", label = "Decrease quantity", onClick = onDecrement)
        Text("$quantity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 10.dp))
        StepperButton("+", label = "Increase quantity", onClick = onIncrement, enabled = canIncrement)
    }
}

@Composable
private fun StepperButton(symbol: String, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier.size(30.dp).background(if (enabled) MacksonGold else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = label }) {
            Text(symbol, style = MaterialTheme.typography.titleMedium, color = MacksonNavyDeep)
        }
    }
}
