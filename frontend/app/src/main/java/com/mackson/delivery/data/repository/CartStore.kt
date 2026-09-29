package com.mackson.delivery.data.repository

import com.mackson.delivery.data.model.CartItem
import com.mackson.delivery.data.model.Product
import com.mackson.delivery.data.model.ShoppingCart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory session container shared across the Browse/ProductDetail/Cart/Checkout screens
 * (Part 1 domain class "Shopping Cart" — "a temporary session container"). It is intentionally
 * client-only: nothing is written to Firestore until checkout, matching how a real shopping
 * cart behaves and avoiding a write on every quantity tap.
 */
object CartStore {
    private val _cart = MutableStateFlow(ShoppingCart())
    val cart: StateFlow<ShoppingCart> = _cart.asStateFlow()

    fun setStore(storeId: String) {
        if (_cart.value.storeId != storeId) {
            _cart.value = ShoppingCart(storeId = storeId)
        }
    }

    fun addOrIncrement(product: Product, quantity: Int = 1) {
        val current = _cart.value
        val existing = current.items.find { it.productId == product.productId }
        val updatedItems = if (existing != null) {
            current.items.map {
                if (it.productId == product.productId) it.copy(quantity = it.quantity + quantity) else it
            }
        } else {
            current.items + CartItem(
                productId = product.productId,
                name = product.name,
                imageUrl = product.imageUrl,
                unitPrice = product.unitPrice,
                quantity = quantity,
                maxAvailable = product.currentStockLevel
            )
        }
        _cart.value = current.copy(items = updatedItems)
    }

    fun setQuantity(productId: String, quantity: Int) {
        _cart.value = _cart.value.withUpdatedQuantity(productId, quantity)
    }

    fun clear() {
        _cart.value = ShoppingCart()
    }
}
