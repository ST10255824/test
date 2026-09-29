package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.Product
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Low-latency search across the product catalogue — Part 1 US-04. */
class SearchViewModel(
    private val storeId: String,
    private val repository: FirestoreRepository = FirestoreRepository
) : ViewModel() {
    private val _results = MutableStateFlow<List<Product>>(emptyList())
    val results: StateFlow<List<Product>> = _results.asStateFlow()

    fun search(term: String) {
        if (term.isBlank()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            when (val result = resultOf { repository.searchProducts(storeId, term) }) {
                is AppResult.Success -> _results.value = result.data
                is AppResult.Error -> _results.value = emptyList()
            }
        }
    }
}

@Composable
fun SearchScreen(storeId: String, onProductClick: (Product) -> Unit) {
    val viewModel: SearchViewModel = viewModel(factory = viewModelFactory { SearchViewModel(storeId) })
    val results by viewModel.results.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.search(it)
            },
            label = { Text("Search groceries") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            items(results, key = { it.productId }) { product ->
                ProductRow(product, onClick = { onProductClick(product) })
            }
        }
        if (query.isNotBlank() && results.isEmpty()) {
            Text("No matches for \"$query\"", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
