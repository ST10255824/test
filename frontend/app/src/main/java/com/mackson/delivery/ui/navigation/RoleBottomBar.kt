package com.mackson.delivery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.mackson.delivery.ui.theme.MacksonGold

data class BottomTab(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val customerTabs = listOf(
    BottomTab("Home", Routes.CUSTOMER_HOME, Icons.Filled.Home),
    BottomTab("Orders", Routes.CUSTOMER_HISTORY, Icons.Filled.History),
    BottomTab("Profile", Routes.CUSTOMER_PROFILE, Icons.Filled.Person)
)

val pickerTabs = listOf(
    BottomTab("Manifest", Routes.PICKER_MANIFEST, Icons.AutoMirrored.Filled.ListAlt)
)

val driverTabs = listOf(
    BottomTab("Jobs", Routes.DRIVER_JOBS, Icons.Filled.LocalShipping)
)

@Composable
fun RoleBottomBar(navController: NavHostController, currentRoute: String?, tabs: List<BottomTab>) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = NavigationBarDefaults.Elevation) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(tabs.first().route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MacksonGold.copy(alpha = 0.20f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
