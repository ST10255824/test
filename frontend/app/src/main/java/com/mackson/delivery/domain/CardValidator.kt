package com.mackson.delivery.domain

import java.time.YearMonth

/**
 * Pure, side-effect-free card format validation. Deliberately does nothing else: no network
 * call, no Firestore write, no logging of the raw values. Part 1's checkout screen already
 * describes payment as going through "our PCI-DSS Level 1 payment partner" — actually collecting
 * and storing raw card numbers ourselves would be a real compliance and security problem even
 * for a student prototype, so this only validates the format client-side (catching typos before
 * the "gateway" call) and PaymentScreen discards the entered values the moment that call
 * resolves; nothing card-shaped ever reaches a ViewModel, repository, or database write.
 */
object CardValidator {

    enum class CardBrand { VISA, MASTERCARD, AMEX, UNKNOWN }

    fun detectBrand(digitsOnly: String): CardBrand = when {
        digitsOnly.startsWith("4") -> CardBrand.VISA
        digitsOnly.take(4).toIntOrNull()?.let { it in 2221..2720 } == true -> CardBrand.MASTERCARD
        digitsOnly.take(2).toIntOrNull()?.let { it in 51..55 } == true -> CardBrand.MASTERCARD
        digitsOnly.take(2) in listOf("34", "37") -> CardBrand.AMEX
        else -> CardBrand.UNKNOWN
    }

    /** The standard Luhn checksum every real card issuer uses — catches fat-finger typos before
     * they ever reach a payment gateway. */
    fun passesLuhnCheck(digitsOnly: String): Boolean {
        if (digitsOnly.isEmpty() || !digitsOnly.all { it.isDigit() }) return false
        var sum = 0
        var alternate = false
        for (i in digitsOnly.length - 1 downTo 0) {
            var d = digitsOnly[i] - '0'
            if (alternate) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun isValidCardNumber(digitsOnly: String): Boolean {
        val expectedLength = if (detectBrand(digitsOnly) == CardBrand.AMEX) 15 else 16
        return digitsOnly.length == expectedLength && passesLuhnCheck(digitsOnly)
    }

    /** @param expiry "MM/YY" as typed by the user. */
    fun isValidExpiry(expiry: String, reference: YearMonth = YearMonth.now()): Boolean {
        val parts = expiry.split("/")
        if (parts.size != 2) return false
        val month = parts[0].trim().toIntOrNull() ?: return false
        val yearSuffix = parts[1].trim().toIntOrNull() ?: return false
        if (month !in 1..12 || parts[1].trim().length != 2) return false
        val cardExpiry = try {
            YearMonth.of(2000 + yearSuffix, month)
        } catch (e: java.time.DateTimeException) {
            return false
        }
        return !cardExpiry.isBefore(reference)
    }

    fun isValidCvv(cvv: String, brand: CardBrand): Boolean {
        val expectedLength = if (brand == CardBrand.AMEX) 4 else 3
        return cvv.length == expectedLength && cvv.all { it.isDigit() }
    }

    /** Groups digits into 4s for display as the user types, e.g. "4242424242424242" ->
     * "4242 4242 4242 4242". */
    fun formatGrouped(digitsOnly: String): String = digitsOnly.chunked(4).joinToString(" ")
}
