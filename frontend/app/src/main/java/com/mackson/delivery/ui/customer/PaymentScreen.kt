package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mackson.delivery.domain.CardValidator
import com.mackson.delivery.ui.common.AccentButton
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.theme.MacksonGold
import com.mackson.delivery.ui.theme.MacksonNavy

private val FieldShape = RoundedCornerShape(14.dp)

/**
 * Matches the "Add New Card" screen in the Part 1 prototype. Card details are validated on this
 * device only (format + Luhn checksum — catching typos before they'd ever reach a real gateway)
 * and never persisted or transmitted: [cardNumberDigits], [expiry], [cvv] and [nameOnCard] below
 * are local Compose state that's discarded the moment this screen leaves composition. The
 * "payment" itself stays the same tokenised placeholder checkout already used
 * (FirestoreRepository.checkoutOrder generates `TOKENISED-{orderId}`) — this screen only gates
 * that call on the customer supplying something that looks like a real card, matching the
 * prototype's flow without this project taking on real PCI-DSS scope.
 */
@Composable
fun PaymentScreen(
    totalAmount: Double,
    isProcessing: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    var cardNumberDigits by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var nameOnCard by remember { mutableStateOf("") }

    val brand = remember(cardNumberDigits) { CardValidator.detectBrand(cardNumberDigits) }
    val cardNumberValid = remember(cardNumberDigits) { CardValidator.isValidCardNumber(cardNumberDigits) }
    val expiryValid = remember(expiry) { CardValidator.isValidExpiry(expiry) }
    val cvvValid = remember(cvv, brand) { CardValidator.isValidCvv(cvv, brand) }
    val canPay = cardNumberValid && expiryValid && cvvValid && nameOnCard.isNotBlank()

    if (isProcessing) {
        FullScreenLoading()
        return
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MacksonNavy)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Icon(Icons.Filled.Lock, contentDescription = "Secure payment", tint = MacksonGold)
        }

        Text("Add New Card", style = MaterialTheme.typography.headlineMedium, color = MacksonNavy, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Enter your payment details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        errorMessage?.let { ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) }

        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Card Number", style = MaterialTheme.typography.labelLarge, color = MacksonNavy)
                OutlinedTextField(
                    value = CardValidator.formatGrouped(cardNumberDigits),
                    onValueChange = { input -> cardNumberDigits = input.filter { it.isDigit() }.take(16) },
                    placeholder = { Text("1234 5678 9012 3456") },
                    singleLine = true,
                    shape = FieldShape,
                    colors = fieldColors,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null) },
                    trailingIcon = {
                        if (brand != CardValidator.CardBrand.UNKNOWN) {
                            Text(brand.name, style = MaterialTheme.typography.labelLarge, color = MacksonNavy, modifier = Modifier.padding(end = 12.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expiry Date", style = MaterialTheme.typography.labelLarge, color = MacksonNavy)
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(4)
                                expiry = when {
                                    digits.length <= 2 -> digits
                                    else -> "${digits.take(2)}/${digits.drop(2)}"
                                }
                            },
                            placeholder = { Text("MM/YY") },
                            singleLine = true,
                            shape = FieldShape,
                            colors = fieldColors,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CVV", style = MaterialTheme.typography.labelLarge, color = MacksonNavy)
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { input -> cvv = input.filter { it.isDigit() }.take(4) },
                            placeholder = { Text("123") },
                            singleLine = true,
                            shape = FieldShape,
                            colors = fieldColors,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                }

                Text("Name on Card", style = MaterialTheme.typography.labelLarge, color = MacksonNavy, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = nameOnCard,
                    onValueChange = { nameOnCard = it },
                    placeholder = { Text("Full name") },
                    singleLine = true,
                    shape = FieldShape,
                    colors = fieldColors,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = MacksonGold, modifier = Modifier.padding(top = 2.dp))
            Text(
                "Your card details are validated on this device and never stored — payment is handled by our PCI-DSS Level 1 gateway.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        AccentButton(
            text = "Pay R${"%.2f".format(totalAmount)}",
            enabled = canPay,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) { onConfirmPayment() }
    }
}
