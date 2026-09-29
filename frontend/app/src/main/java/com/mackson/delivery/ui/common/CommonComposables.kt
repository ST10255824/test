package com.mackson.delivery.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mackson.delivery.data.model.OrderStatus
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavyDeep
import com.mackson.delivery.ui.theme.StatusErrorRed
import com.mackson.delivery.ui.theme.StatusInfoBlue
import com.mackson.delivery.ui.theme.StatusNeutralGray
import com.mackson.delivery.ui.theme.StatusSuccessGreen
import com.mackson.delivery.ui.theme.StatusWarningAmber

private val ButtonShape = RoundedCornerShape(percent = 50)

/**
 * Chunky, fully-rounded primary action button — matches the pill-shaped CTAs ("Start shopping",
 * "Confirm location", "Checkout") throughout the Part 1 Figma prototype. Minimum 56dp height
 * comfortably clears the 24x24dp WCAG 2.2 AA touch target.
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        modifier = modifier.height(56.dp).padding(vertical = 4.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Gold accent variant for the single standout action on a screen — e.g. "I've Arrived",
 * "Confirm location" — matching the driver app / location-picker screens in the prototype. */
@Composable
fun AccentButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MacksonGold,
            contentColor = MacksonNavyDeep
        ),
        modifier = modifier.height(56.dp).padding(vertical = 4.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading" }
        )
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Small rounded status pill — matches the "IN PROGRESS" / "COMPLETE" / "Delivered" chips used
 * throughout the prototype's order and picking screens. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

fun statusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.RECEIVED -> StatusNeutralGray
    OrderStatus.PICKING -> StatusWarningAmber
    OrderStatus.READY_FOR_COLLECTION -> StatusInfoBlue
    OrderStatus.EN_ROUTE -> StatusInfoBlue
    OrderStatus.ARRIVED_AT_NODE -> StatusWarningAmber
    OrderStatus.DELIVERED -> StatusSuccessGreen
    OrderStatus.CANCELLED -> StatusErrorRed
}

fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.RECEIVED -> "Order received"
    OrderStatus.PICKING -> "Being picked"
    OrderStatus.READY_FOR_COLLECTION -> "Ready for collection"
    OrderStatus.EN_ROUTE -> "En route"
    OrderStatus.ARRIVED_AT_NODE -> "Driver has arrived"
    OrderStatus.DELIVERED -> "Delivered"
    OrderStatus.CANCELLED -> "Cancelled"
}
