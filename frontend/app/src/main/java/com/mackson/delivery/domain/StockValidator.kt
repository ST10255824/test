package com.mackson.delivery.domain

import com.mackson.delivery.data.model.CartItem
import com.mackson.delivery.data.model.Product

/**
 * Client-side stock guard so the UI can reject an over-quantity add-to-cart before it ever
 * reaches Firestore. This is a defence-in-depth check only — the authoritative check is the
 * Firestore transaction in Cloud Functions (see functions/src/stock.ts), which prevents the
 * race condition described in Part 1 section 2.3 (two customers buying the last unit).
 */
object StockValidator {

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Rejected(val reason: String) : ValidationResult()
    }

    fun canAddToCart(product: Product, requestedQuantity: Int, existingCartQuantity: Int = 0): ValidationResult {
        if (requestedQuantity <= 0) return ValidationResult.Rejected("Quantity must be at least 1")
        if (!product.inStock) return ValidationResult.Rejected("${product.name} is out of stock")
        val totalRequested = requestedQuantity + existingCartQuantity
        if (totalRequested > product.currentStockLevel) {
            return ValidationResult.Rejected(
                "Only ${product.currentStockLevel} of ${product.name} left in stock"
            )
        }
        return ValidationResult.Valid
    }

    fun validateCartAgainstCatalogue(items: List<CartItem>, catalogue: Map<String, Product>): List<String> {
        val problems = mutableListOf<String>()
        for (item in items) {
            val product = catalogue[item.productId]
            if (product == null) {
                problems += "${item.name} is no longer available"
                continue
            }
            if (item.quantity > product.currentStockLevel) {
                problems += "${item.name}: only ${product.currentStockLevel} left, ${item.quantity} requested"
            }
        }
        return problems
    }
}
