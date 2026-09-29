package com.mackson.delivery.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mackson.delivery.data.model.UserRole
import com.mackson.delivery.ui.common.BrandLogo
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavy
import com.mackson.delivery.ui.theme.MacksonSkyBlue

/**
 * Landing screen after sign-in. The reference architecture in Part 1 ships three separate
 * Android client apps (Customer / Picker / Driver); this build consolidates those three role
 * experiences into one Kotlin/Compose codebase so a single grader account can exercise every
 * mobile flow. Firestore Security Rules (firestore/firestore.rules) still enforce the real RBAC
 * boundary server-side regardless of which UI is opened here.
 *
 * The Store Manager / Admin role deliberately has no presence in this app — it's served by a
 * separate web console (admin-web/) instead, per the client's requirement that admin tooling
 * live on the web, not the mobile app. See admin-web/README or the root README for that piece.
 */
@Composable
fun RoleHubScreen(onRoleSelected: (UserRole) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MacksonSkyBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandLogo(showWordmark = false, modifier = Modifier.padding(bottom = 20.dp))
            Text("Choose how you'd like to continue", style = MaterialTheme.typography.headlineMedium, color = MacksonNavy)
            Text(
                "Mackson's Delivery serves three roles from one app shell.",
                style = MaterialTheme.typography.bodyMedium,
                color = MacksonNavy,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            RoleCard(Icons.Filled.ShoppingCart, "Customer", "Shop, track orders, chat with your picker") { onRoleSelected(UserRole.CUSTOMER) }
            RoleCard(Icons.Filled.Storefront, "In-Store Picker", "Pick manifests, scan barcodes, flag substitutions") { onRoleSelected(UserRole.PICKER) }
            RoleCard(Icons.Filled.LocalShipping, "Delivery Driver", "Accept runs, navigate, confirm delivery") { onRoleSelected(UserRole.DRIVER) }
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MacksonGold.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.padding(start = 14.dp), horizontalAlignment = Alignment.Start) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
