package com.mackson.delivery.domain

import com.mackson.delivery.data.model.ShoppingCart
import kotlin.math.round

/**
 * Pure, side-effect-free checkout arithmetic. Kept out of the ViewModel/Firebase layers
 * specifically so it can be unit tested per the Task 2 rubric ("unit tests verify isolated
 * domain logic such as fee rules and subtotal computations").
 */
object CheckoutCalculator {

    private const val SERVICE_FEE_RATE = 0.02 // 2% platform/service fee
    private const val MIN_SERVICE_FEE = 5.0
    private const val LOYALTY_DISCOUNT_RATE = 0.05 // 5% off subtotal when a loyalty card is linked

    data class Breakdown(
        val subtotal: Double,
        val loyaltyDiscount: Double,
        val serviceFee: Double,
        val driverTip: Double,
        val total: Double
    )

    fun calculate(
        cart: ShoppingCart,
        hasLinkedLoyaltyCard: Boolean,
        voucherAmount: Double = 0.0,
        driverTip: Double = 0.0
    ): Breakdown {
        require(driverTip >= 0.0) { "Driver tip cannot be negative" }
        require(voucherAmount >= 0.0) { "Voucher amount cannot be negative" }

        val subtotal = round2(cart.subtotal)
        val loyaltyDiscount = if (hasLinkedLoyaltyCard) round2(subtotal * LOYALTY_DISCOUNT_RATE) else 0.0
        val discountedSubtotal = (subtotal - loyaltyDiscount - voucherAmount).coerceAtLeast(0.0)
        val serviceFee = if (discountedSubtotal <= 0.0) 0.0 else
            maxOf(MIN_SERVICE_FEE, round2(discountedSubtotal * SERVICE_FEE_RATE))
        val total = round2(discountedSubtotal + serviceFee + driverTip)

        return Breakdown(
            subtotal = subtotal,
            loyaltyDiscount = loyaltyDiscount,
            serviceFee = serviceFee,
            driverTip = round2(driverTip),
            total = total
        )
    }

    private fun round2(value: Double): Double = round(value * 100) / 100
}
