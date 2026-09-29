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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.Product
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.data.repository.CartStore
import com.mackson.delivery.domain.StockValidator
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.StatusChip
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavyDeep
import com.mackson.delivery.ui.theme.StatusSuccessGreen
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseUiState(
    val isLoading: Boolean = true,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null
)

/** Hierarchical category navigation — Part 1 US-04. */
class BrowseViewModel(
    private val storeId: String,
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val categoriesResult = resultOf { repository.fetchCategories(storeId) }) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(categories = categoriesResult.data)
                    selectCategory(categoriesResult.data.firstOrNull())
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = categoriesResult.message)
            }
        }
    }

    fun selectCategory(category: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedCategory = category)
            when (val result = resultOf { repository.fetchProducts(storeId, category) }) {
                is AppResult.Success -> _uiState.value =
                    _uiState.value.copy(isLoading = false, products = result.data)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    storeId: String,
    cartItemCount: Int,
    onSearch: () -> Unit,
    onCartClick: () -> Unit,
    onProductClick: (Product) -> Unit
) {
    val viewModel: BrowseViewModel = viewModel(factory = viewModelFactory { BrowseViewModel(storeId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(storeId) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search products") }
                    Box {
                        IconButton(onClick = onCartClick) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart, $cartItemCount items")
                        }
                        if (cartItemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(18.dp)
                                    .background(MacksonGold, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$cartItemCount",
                                    color = MacksonNavyDeep,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories) { category ->
                    FilterChip(
                        selected = category == uiState.selectedCategory,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category) },
                        shape = RoundedCornerShape(percent = 50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MacksonGold.copy(alpha = 0.22f),
                            selectedLabelColor = MacksonNavyDeep
                        )
                    )
                }
            }

            when {
                uiState.isLoading -> FullScreenLoading()
                uiState.errorMessage != null -> ErrorBanner(uiState.errorMessage!!)
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.products, key = { it.productId }) { product ->
                        ProductRow(product, onClick = { onProductClick(product) })
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRow(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.mackson.delivery.ui.common.ProductThumbnail(
                imageUrl = product.imageUrl,
                modifier = Modifier.size(64.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aisle ${product.aisleNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusChip(
                    text = if (product.inStock) "In stock" else "Out of stock",
                    color = if (product.inStock) StatusSuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("R${"%.2f".format(product.unitPrice)}", style = MaterialTheme.typography.titleMedium)
                QuickAddButton(product)
            }
        }
    }
}

@Composable
private fun QuickAddButton(product: Product) {
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .size(32.dp)
            .background(if (product.inStock) MacksonGold else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            enabled = product.inStock,
            onClick = {
                val existingQty = CartStore.cart.value.items.find { it.productId == product.productId }?.quantity ?: 0
                if (StockValidator.canAddToCart(product, 1, existingQty) is StockValidator.ValidationResult.Valid) {
                    CartStore.setStore(product.storeId)
                    CartStore.addOrIncrement(product, 1)
                }
            }
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add ${product.name} to cart", tint = MacksonNavyDeep)
        }
    }
}
