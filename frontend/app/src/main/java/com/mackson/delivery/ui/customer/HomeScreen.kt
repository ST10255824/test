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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.mackson.delivery.data.model.StoreNode
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.AccentButton
import com.mackson.delivery.ui.common.BrandLogo
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.ProductThumbnail
import com.mackson.delivery.ui.theme.MacksonNavy
import com.mackson.delivery.ui.theme.MacksonSkyBlue
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val stores: List<StoreNode> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(private val repository: FirestoreRepository = FirestoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadStores() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = resultOf { repository.fetchNearbyStores() }) {
                is AppResult.Success -> _uiState.value = HomeUiState(isLoading = false, stores = result.data)
                is AppResult.Error -> _uiState.value = HomeUiState(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

/** Store selection screen — Part 1 US-03: "explicitly select a nearby retail store". Matches
 * the "Choose nearest store" screen in the Part 1 prototype, including its real photo of the
 * Estcourt branch. */
@Composable
fun HomeScreen(onStoreSelected: (StoreNode) -> Unit) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory { HomeViewModel() })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadStores() }

    Box(modifier = Modifier.fillMaxSize().background(MacksonSkyBlue)) {
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.errorMessage != null -> ErrorBanner(uiState.errorMessage!!, modifier = Modifier.padding(24.dp))
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                BrandLogo(showWordmark = false, modifier = Modifier.padding(bottom = 16.dp))
                Text("Choose nearest store", style = MaterialTheme.typography.headlineMedium, color = MacksonNavy)
                Text(
                    "We'll show stock and prices for the branch you pick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MacksonNavy,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.stores, key = { it.storeId }) { store ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row {
                                    ProductThumbnail(imageUrl = store.imageUrl, modifier = Modifier.size(96.dp))
                                    Column(modifier = Modifier.padding(start = 14.dp)) {
                                        Text(store.branchName, style = MaterialTheme.typography.titleMedium)
                                        if (store.address.isNotBlank()) {
                                            Text(
                                                store.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "Delivers within ${store.serviceRadiusKm.toInt()} km",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                AccentButton(
                                    text = "Confirm",
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                                ) { onStoreSelected(store) }
                            }
                        }
                    }
                }
            }
        }
    }
}
