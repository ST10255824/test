package com.mackson.delivery.domain

import com.google.common.truth.Truth.assertThat
import com.mackson.delivery.data.model.CartItem
import com.mackson.delivery.data.model.Product
import org.junit.Test

class StockValidatorTest {

    private val bread = Product(productId = "p1", name = "Bread", currentStockLevel = 5)

    @Test
    fun `allows adding quantity within stock`() {
        val result = StockValidator.canAddToCart(bread, requestedQuantity = 3)
        assertThat(result).isEqualTo(StockValidator.ValidationResult.Valid)
    }

    @Test
    fun `rejects quantity that exceeds stock`() {
        val result = StockValidator.canAddToCart(bread, requestedQuantity = 6)
        assertThat(result).isInstanceOf(StockValidator.ValidationResult.Rejected::class.java)
    }

    @Test
    fun `accounts for quantity already in cart`() {
        val result = StockValidator.canAddToCart(bread, requestedQuantity = 3, existingCartQuantity = 3)
        assertThat(result).isInstanceOf(StockValidator.ValidationResult.Rejected::class.java)
    }

    @Test
    fun `rejects out of stock product regardless of quantity`() {
        val outOfStock = bread.copy(currentStockLevel = 0)
        val result = StockValidator.canAddToCart(outOfStock, requestedQuantity = 1)
        assertThat(result).isInstanceOf(StockValidator.ValidationResult.Rejected::class.java)
    }

    @Test
    fun `rejects zero or negative quantity`() {
        assertThat(StockValidator.canAddToCart(bread, requestedQuantity = 0))
            .isInstanceOf(StockValidator.ValidationResult.Rejected::class.java)
        assertThat(StockValidator.canAddToCart(bread, requestedQuantity = -1))
            .isInstanceOf(StockValidator.ValidationResult.Rejected::class.java)
    }

    @Test
    fun `validateCartAgainstCatalogue flags items that exceed live stock`() {
        val cartItems = listOf(CartItem(productId = "p1", name = "Bread", unitPrice = 20.0, quantity = 10))
        val catalogue = mapOf("p1" to bread) // catalogue only has 5 left
        val problems = StockValidator.validateCartAgainstCatalogue(cartItems, catalogue)
        assertThat(problems).hasSize(1)
    }

    @Test
    fun `validateCartAgainstCatalogue flags items removed from the catalogue`() {
        val cartItems = listOf(CartItem(productId = "gone", name = "Discontinued", unitPrice = 10.0, quantity = 1))
        val problems = StockValidator.validateCartAgainstCatalogue(cartItems, emptyMap())
        assertThat(problems).hasSize(1)
    }

    @Test
    fun `validateCartAgainstCatalogue returns no problems when stock is sufficient`() {
        val cartItems = listOf(CartItem(productId = "p1", name = "Bread", unitPrice = 20.0, quantity = 2))
        val problems = StockValidator.validateCartAgainstCatalogue(cartItems, mapOf("p1" to bread))
        assertThat(problems).isEmpty()
    }
}
