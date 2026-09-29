package com.mackson.delivery.domain

import com.google.common.truth.Truth.assertThat
import com.mackson.delivery.data.model.CartItem
import com.mackson.delivery.data.model.ShoppingCart
import org.junit.Test

class CheckoutCalculatorTest {

    private val cart = ShoppingCart(
        storeId = "store-1",
        items = listOf(
            CartItem(productId = "p1", name = "Bread", unitPrice = 20.0, quantity = 2),
            CartItem(productId = "p2", name = "Milk", unitPrice = 18.5, quantity = 1)
        )
    )

    @Test
    fun `subtotal sums line totals`() {
        val breakdown = CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = false)
        assertThat(breakdown.subtotal).isEqualTo(58.5)
    }

    @Test
    fun `loyalty discount applies 5 percent off subtotal`() {
        val breakdown = CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = true)
        assertThat(breakdown.loyaltyDiscount).isEqualTo(2.92)
    }

    @Test
    fun `no loyalty card means zero discount`() {
        val breakdown = CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = false)
        assertThat(breakdown.loyaltyDiscount).isEqualTo(0.0)
    }

    @Test
    fun `service fee never drops below the minimum`() {
        val tinyCart = ShoppingCart(items = listOf(CartItem(productId = "p1", name = "Gum", unitPrice = 5.0, quantity = 1)))
        val breakdown = CheckoutCalculator.calculate(tinyCart, hasLinkedLoyaltyCard = false)
        assertThat(breakdown.serviceFee).isEqualTo(5.0)
    }

    @Test
    fun `driver tip is added on top of the total untaxed`() {
        val withTip = CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = false, driverTip = 10.0)
        val withoutTip = CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = false, driverTip = 0.0)
        assertThat(withTip.total).isEqualTo(withoutTip.total + 10.0)
    }

    @Test
    fun `total combines subtotal minus discounts plus fee and tip`() {
        val breakdown = CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = true, driverTip = 5.0)
        val expectedDiscountedSubtotal = breakdown.subtotal - breakdown.loyaltyDiscount
        val expectedTotal = expectedDiscountedSubtotal + breakdown.serviceFee + breakdown.driverTip
        assertThat(breakdown.total).isEqualTo(expectedTotal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative tip is rejected`() {
        CheckoutCalculator.calculate(cart, hasLinkedLoyaltyCard = false, driverTip = -1.0)
    }

    @Test
    fun `empty cart produces zero subtotal and zero fee`() {
        val breakdown = CheckoutCalculator.calculate(ShoppingCart(), hasLinkedLoyaltyCard = true)
        assertThat(breakdown.subtotal).isEqualTo(0.0)
        assertThat(breakdown.serviceFee).isEqualTo(0.0)
        assertThat(breakdown.total).isEqualTo(0.0)
    }
}
