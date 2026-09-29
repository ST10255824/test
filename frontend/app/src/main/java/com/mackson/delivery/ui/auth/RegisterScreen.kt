package com.mackson.delivery.ui.auth

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mackson.delivery.ui.common.BrandLogo
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.ui.theme.MacksonNavy
import com.mackson.delivery.ui.theme.MacksonSkyBlue

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (name: String, email: String, mobile: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            BrandLogo(showWordmark = false, modifier = Modifier.padding(bottom = 16.dp))
            Text("Create your account", style = MaterialTheme.typography.headlineMedium, color = MacksonNavy)
            uiState.errorMessage?.let {
                ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            OutlinedTextField(
                name, { name = it }, label = { Text("Full name") }, shape = FieldShape, colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            OutlinedTextField(
                email, { email = it }, label = { Text("Email") }, shape = FieldShape, colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                mobile, { mobile = it }, label = { Text("Mobile number (+27...)") }, shape = FieldShape, colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                password, { password = it }, label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                shape = FieldShape, colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            PrimaryButton(
                text = "Register",
                enabled = listOf(name, email, mobile, password).all { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                onRegister(name, email, mobile, password)
            }
        }
    }
}
