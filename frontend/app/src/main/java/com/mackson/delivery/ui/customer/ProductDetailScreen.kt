package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.common.ProductThumbnail
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val errorMessage: String? = null
)

/** Part 1 US-06: high-res images, sizes, weights, nutritional content, real-time-parity pricing. */
class ProductDetailViewModel(
    private val storeId: String,
    private val productId: String,
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState(isLoading = true)
            when (val result = resultOf { repository.fetchProduct(storeId, productId) }) {
                is AppResult.Success -> _uiState.value = ProductDetailUiState(isLoading = false, product = result.data)
                is AppResult.Error -> _uiState.value = ProductDetailUiState(isLoading = false, errorMessage = result.message)
            }
        }
    }
}

@Composable
fun ProductDetailScreen(storeId: String, productId: String, onAddedToCart: () -> Unit) {
    val viewModel: ProductDetailViewModel =
        viewModel(factory = viewModelFactory { ProductDetailViewModel(storeId, productId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var quantity by remember { mutableIntStateOf(1) }
    var addError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) { viewModel.load() }

    when {
        uiState.isLoading -> FullScreenLoading()
        uiState.errorMessage != null -> ErrorBanner(uiState.errorMessage!!)
        uiState.product != null -> {
            val product = uiState.product!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                ProductThumbnail(
                    imageUrl = product.imageUrl,
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
                Text(
                    product.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text("R${"%.2f".format(product.unitPrice)}", style = MaterialTheme.typography.titleLarge)
                Text(product.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "${product.weightGrams}g · Aisle ${product.aisleNumber} · ${if (product.inStock) "${product.currentStockLevel} in stock" else "Out of stock"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(modifier = Modifier.padding(top = 16.dp)) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.semantics { contentDescription = "Decrease quantity" }
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text("$quantity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.semantics { contentDescription = "Increase quantity" }
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }

                addError?.let { ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }

                PrimaryButton(
                    text = "Add to cart",
                    enabled = product.inStock,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    val existingQty = CartStore.cart.value.items
                        .find { it.productId == product.productId }?.quantity ?: 0
                    when (val validation = StockValidator.canAddToCart(product, quantity, existingQty)) {
                        is StockValidator.ValidationResult.Valid -> {
                            CartStore.setStore(storeId)
                            CartStore.addOrIncrement(product, quantity)
                            addError = null
                            onAddedToCart()
                        }
                        is StockValidator.ValidationResult.Rejected -> addError = validation.reason
                    }
                }
            }
        }
    }
}
