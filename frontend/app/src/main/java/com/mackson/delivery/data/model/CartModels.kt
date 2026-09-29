package com.mackson.delivery.data.model

/**
 * Temporary session container for the customer's chosen products before checkout.
 * Held client-side (and mirrored to Firestore users/{uid}/cart/current) so it survives
 * app restarts. See Part 1 domain class diagram — "Shopping Cart".
 */
data class CartItem(
    val productId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val maxAvailable: Int = Int.MAX_VALUE
) {
    val lineTotal: Double get() = unitPrice * quantity
}

data class ShoppingCart(
    val storeId: String = "",
    val items: List<CartItem> = emptyList()
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { it.lineTotal }

    fun withUpdatedQuantity(productId: String, quantity: Int): ShoppingCart {
        val updated = if (quantity <= 0) {
            items.filterNot { it.productId == productId }
        } else {
            if (items.any { it.productId == productId }) {
                items.map { if (it.productId == productId) it.copy(quantity = quantity) else it }
            } else {
                items
            }
        }
        return copy(items = updated)
    }
}
