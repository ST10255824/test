package com.mackson.delivery.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShoppingCartTest {

    private val cart = ShoppingCart(
        storeId = "store-1",
        items = listOf(
            CartItem(productId = "p1", name = "Bread", unitPrice = 20.0, quantity = 2),
            CartItem(productId = "p2", name = "Milk", unitPrice = 18.5, quantity = 1)
        )
    )

    @Test
    fun `itemCount sums quantities across all lines`() {
        assertThat(cart.itemCount).isEqualTo(3)
    }

    @Test
    fun `subtotal sums line totals`() {
        assertThat(cart.subtotal).isEqualTo(58.5)
    }

    @Test
    fun `withUpdatedQuantity updates an existing line`() {
        val updated = cart.withUpdatedQuantity("p1", 5)
        assertThat(updated.items.first { it.productId == "p1" }.quantity).isEqualTo(5)
    }

    @Test
    fun `withUpdatedQuantity of zero removes the line`() {
        val updated = cart.withUpdatedQuantity("p1", 0)
        assertThat(updated.items.map { it.productId }).doesNotContain("p1")
    }

    @Test
    fun `withUpdatedQuantity ignores products not already in the cart`() {
        val updated = cart.withUpdatedQuantity("unknown", 3)
        assertThat(updated.items).isEqualTo(cart.items)
    }
}
