package com.mackson.delivery.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mackson.delivery.ui.common.BrandLogo
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.theme.MacksonDeliveryTheme
import com.mackson.delivery.ui.theme.MacksonNavy
import com.mackson.delivery.ui.theme.MacksonSkyBlue

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onGoToRegister: () -> Unit,
    onGoToOtpLogin: () -> Unit,
    onDismissError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (uiState.isLoading) {
        FullScreenLoading()
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(MacksonSkyBlue)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandLogo(modifier = Modifier.padding(bottom = 12.dp))

            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                color = MacksonNavy,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                "Groceries at your door, on your schedule.",
                style = MaterialTheme.typography.bodyMedium,
                color = MacksonNavy,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            uiState.errorMessage?.let {
                ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                shape = FieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MacksonNavy,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                shape = FieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MacksonNavy,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            PrimaryButton(
                text = "Log in",
                enabled = email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                onDismissError()
                onLogin(email, password)
            }

            TextButton(onClick = onGoToRegister) {
                Text("New customer? Create an account", color = MacksonNavy)
            }
            TextButton(onClick = onGoToOtpLogin) {
                Text("Sign in with a mobile OTP instead", color = MacksonNavy)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MacksonDeliveryTheme {
        LoginScreen(AuthUiState(), { _, _ -> }, {}, {}, {})
    }
}
