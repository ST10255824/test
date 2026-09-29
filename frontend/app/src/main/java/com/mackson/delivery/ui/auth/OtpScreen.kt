package com.mackson.delivery.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mackson.delivery.ui.common.BrandLogo
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.theme.MacksonNavy
import com.mackson.delivery.ui.theme.MacksonSkyBlue

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun OtpScreen(
    initialPhone: String,
    uiState: OtpUiState,
    onRequestOtp: (String, Activity) -> Unit,
    onConfirmCode: (String) -> Unit
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf(initialPhone) }
    var code by remember { mutableStateOf("") }

    if (uiState.isLoading) {
        FullScreenLoading()
        return
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MacksonNavy,
        unfocusedContainerColor = Color.White,
        focusedContainerColor = Color.White
    )

    Box(modifier = Modifier.fillMaxSize().background(MacksonSkyBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            BrandLogo(showWordmark = false, modifier = Modifier.padding(bottom = 20.dp))
            Text("Verify your mobile number", style = MaterialTheme.typography.headlineMedium, color = MacksonNavy)
            Text(
                "We'll text you a one-time PIN. Standard SMS rates may apply.",
                style = MaterialTheme.typography.bodyMedium,
                color = MacksonNavy,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            uiState.errorMessage?.let { ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) }

            when (uiState.phase) {
                OtpUiState.Phase.ENTER_PHONE -> {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile number, e.g. +27821234567") },
                        singleLine = true,
                        shape = FieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryButton(
                        text = "Send OTP",
                        enabled = phone.length >= 10,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        (context as? Activity)?.let { onRequestOtp(phone, it) }
                    }
                }
                OtpUiState.Phase.ENTER_CODE -> {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("6-digit code") },
                        singleLine = true,
                        shape = FieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryButton(
                        text = "Confirm",
                        enabled = code.length == 6,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) { onConfirmCode(code) }
                }
                OtpUiState.Phase.VERIFIED -> {
                    Text("Verified! Redirecting...", style = MaterialTheme.typography.bodyLarge, color = MacksonNavy)
                }
            }
        }
    }
}
