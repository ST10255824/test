package com.mackson.delivery.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * Product photo with a graceful fallback: a plain grocery icon while loading, on a broken URL,
 * or when a product simply has no `imageUrl` set (e.g. anything added via the Admin console
 * without a photo). Used everywhere a product appears — browse cards, product detail, cart.
 */
@Composable
fun ProductThumbnail(imageUrl: String, modifier: Modifier = Modifier, cornerRadius: Dp = 14.dp) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isBlank()) {
            FallbackIcon()
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { FallbackIcon() },
                error = { FallbackIcon() },
                success = { SubcomposeAsyncImageContent() }
            )
        }
    }
}

@Composable
private fun FallbackIcon() {
    Icon(
        Icons.Filled.LocalGroceryStore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
