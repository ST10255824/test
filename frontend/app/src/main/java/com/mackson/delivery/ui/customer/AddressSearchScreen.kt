package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mackson.delivery.data.model.DeliveryAddress
import com.mackson.delivery.data.remote.AddressSearchService
import com.mackson.delivery.data.remote.AddressSuggestion
import com.mackson.delivery.domain.GeofenceValidator
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavy
import kotlinx.coroutines.delay
import java.util.UUID

private val FieldShape = RoundedCornerShape(16.dp)

/**
 * Matches the "Select Delivery Address" screen in the Part 1 prototype. Address search/geocoding
 * is OpenStreetMap's free Nominatim API (see AddressSearchService) rather than Google Places
 * Autocomplete, which needs a billing-enabled Google Cloud project — same trade-off as Cloud
 * Functions needing Blaze. Keystrokes are debounced 450ms before searching, both to stay well
 * under Nominatim's ~1 request/second usage policy and to avoid a request per character typed.
 */
@Composable
fun AddressSearchScreen(
    storeLatitude: Double,
    storeLongitude: Double,
    storeServiceRadiusKm: Double,
    savedAddresses: List<DeliveryAddress> = emptyList(),
    onAddressConfirmed: (DeliveryAddress) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var selected by remember { mutableStateOf<AddressSuggestion?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (selected != null) return@LaunchedEffect
        if (query.trim().length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(450)
        isSearching = true
        suggestions = runCatching { AddressSearchService.search(query) }.getOrDefault(emptyList())
        isSearching = false
    }

    val withinRadius = selected?.let {
        GeofenceValidator.isWithinServiceRadius(it.latitude, it.longitude, storeLatitude, storeLongitude, storeServiceRadiusKm)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MacksonGold, modifier = Modifier.size(28.dp))
            Text(
                "Select Delivery Address",
                style = MaterialTheme.typography.headlineMedium,
                color = MacksonNavy,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            "Search for your street address — we'll check it's within our delivery area.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (savedAddresses.isNotEmpty()) {
            Text("Saved addresses", style = MaterialTheme.typography.titleMedium, color = MacksonNavy)
            savedAddresses.forEach { saved ->
                Card(
                    onClick = { onAddressConfirmed(saved) },
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MacksonGold)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(saved.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                saved.formattedAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Text(
                "Or search for a new address",
                style = MaterialTheme.typography.titleMedium,
                color = MacksonNavy,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; selected = null },
            label = { Text("Delivery address") },
            singleLine = true,
            shape = FieldShape,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MacksonNavy),
            modifier = Modifier.fillMaxWidth()
        )

        if (isSearching) {
            Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text("Searching…", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (selected == null && suggestions.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                suggestions.forEach { suggestion ->
                    Card(
                        onClick = {
                            selected = suggestion
                            query = suggestion.displayName
                            suggestions = emptyList()
                        },
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(suggestion.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            }
        }

        selected?.let {
            if (withinRadius == false) {
                ErrorBanner(
                    "This address is outside our ${storeServiceRadiusKm.toInt()}km delivery area — try a different address.",
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Within our delivery area ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        PrimaryButton(
            text = "Use this address",
            enabled = selected != null && withinRadius == true,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            val s = selected ?: return@PrimaryButton
            onAddressConfirmed(
                DeliveryAddress(
                    addressId = UUID.randomUUID().toString(),
                    label = s.displayName.substringBefore(",").trim().ifBlank { "Delivery address" },
                    latitude = s.latitude,
                    longitude = s.longitude,
                    formattedAddress = s.displayName,
                    withinServiceRadius = true
                )
            )
        }
    }
}
