package com.mackson.delivery.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.YearMonth

class CardValidatorTest {

    private val reference = YearMonth.of(2026, 1)

    @Test
    fun `detects visa by leading 4`() {
        assertThat(CardValidator.detectBrand("4242424242424242")).isEqualTo(CardValidator.CardBrand.VISA)
    }

    @Test
    fun `detects mastercard by 51 to 55 prefix`() {
        assertThat(CardValidator.detectBrand("5555555555554444")).isEqualTo(CardValidator.CardBrand.MASTERCARD)
    }

    @Test
    fun `detects amex by 34 or 37 prefix`() {
        assertThat(CardValidator.detectBrand("378282246310005")).isEqualTo(CardValidator.CardBrand.AMEX)
    }

    @Test
    fun `unrecognised prefix is unknown brand`() {
        assertThat(CardValidator.detectBrand("6011000000000004")).isEqualTo(CardValidator.CardBrand.UNKNOWN)
    }

    @Test
    fun `passes luhn check for a valid test card number`() {
        assertThat(CardValidator.passesLuhnCheck("4242424242424242")).isTrue()
    }

    @Test
    fun `fails luhn check when a digit is wrong`() {
        assertThat(CardValidator.passesLuhnCheck("4242424242424241")).isFalse()
    }

    @Test
    fun `valid 16 digit visa passes full validation`() {
        assertThat(CardValidator.isValidCardNumber("4242424242424242")).isTrue()
    }

    @Test
    fun `valid 15 digit amex passes full validation`() {
        assertThat(CardValidator.isValidCardNumber("378282246310005")).isTrue()
    }

    @Test
    fun `16 digit number fails validation for an amex prefix expecting 15`() {
        assertThat(CardValidator.isValidCardNumber("3782822463100050")).isFalse()
    }

    @Test
    fun `expiry this month or later is valid`() {
        assertThat(CardValidator.isValidExpiry("01/26", reference)).isTrue()
        assertThat(CardValidator.isValidExpiry("12/26", reference)).isTrue()
    }

    @Test
    fun `expiry before the reference month is invalid`() {
        assertThat(CardValidator.isValidExpiry("12/25", reference)).isFalse()
    }

    @Test
    fun `malformed expiry strings are invalid`() {
        assertThat(CardValidator.isValidExpiry("13/26", reference)).isFalse()
        assertThat(CardValidator.isValidExpiry("June", reference)).isFalse()
        assertThat(CardValidator.isValidExpiry("no slash here", reference)).isFalse()
    }

    @Test
    fun `cvv length depends on brand`() {
        assertThat(CardValidator.isValidCvv("123", CardValidator.CardBrand.VISA)).isTrue()
        assertThat(CardValidator.isValidCvv("1234", CardValidator.CardBrand.VISA)).isFalse()
        assertThat(CardValidator.isValidCvv("1234", CardValidator.CardBrand.AMEX)).isTrue()
        assertThat(CardValidator.isValidCvv("123", CardValidator.CardBrand.AMEX)).isFalse()
    }

    @Test
    fun `groups digits into fours for display`() {
        assertThat(CardValidator.formatGrouped("4242424242424242")).isEqualTo("4242 4242 4242 4242")
    }
}
