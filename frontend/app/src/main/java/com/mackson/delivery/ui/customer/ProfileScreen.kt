package com.mackson.delivery.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mackson.delivery.data.model.Customer
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.ui.common.ErrorBanner
import com.mackson.delivery.ui.common.FullScreenLoading
import com.mackson.delivery.ui.common.PrimaryButton
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import com.mackson.delivery.util.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val customer: Customer? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(private val repository: FirestoreRepository = FirestoreRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        val uid = FirebaseAuthManager.getCurrentUserId() ?: run {
            _uiState.value = ProfileUiState(isLoading = false, errorMessage = "You're not signed in.")
            return
        }
        viewModelScope.launch {
            when (val result = resultOf { repository.fetchCustomerProfile(uid) }) {
                is AppResult.Success -> {
                    val existing = result.data
                    if (existing != null) {
                        _uiState.value = ProfileUiState(isLoading = false, customer = existing)
                    } else {
                        // No Firestore profile yet — happens for any account that signed in via
                        // phone OTP (startPhoneVerification) rather than registerUser, since only
                        // the email/password registration path writes this document. Self-heal
                        // by creating a minimal one from whatever FirebaseAuth already knows.
                        val healed = Customer(
                            customerId = uid,
                            name = FirebaseAuthManager.getCurrentUserEmail()?.substringBefore("@")
                                ?: FirebaseAuthManager.getCurrentUserPhone()
                                ?: "Mackson's customer",
                            email = FirebaseAuthManager.getCurrentUserEmail().orEmpty(),
                            mobileNumber = FirebaseAuthManager.getCurrentUserPhone().orEmpty()
                        )
                        when (val saveResult = resultOf { repository.saveCustomerProfile(healed) }) {
                            is AppResult.Success -> _uiState.value = ProfileUiState(isLoading = false, customer = healed)
                            is AppResult.Error -> _uiState.value = ProfileUiState(isLoading = false, errorMessage = saveResult.message)
                        }
                    }
                }
                is AppResult.Error -> _uiState.value = ProfileUiState(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        val customer = _uiState.value.customer ?: return
        val updated = customer.copy(biometricEnabled = enabled)
        _uiState.value = _uiState.value.copy(customer = updated)
        viewModelScope.launch { resultOf { repository.saveCustomerProfile(updated) } }
    }

    fun linkLoyaltyCard(cardNumber: String) {
        val customer = _uiState.value.customer ?: return
        val updated = customer.copy(loyaltyCardNumber = cardNumber)
        _uiState.value = _uiState.value.copy(customer = updated)
        viewModelScope.launch { resultOf { repository.saveCustomerProfile(updated) } }
    }
}

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    val viewModel: ProfileViewModel = viewModel(factory = viewModelFactory { ProfileViewModel() })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    val signOutButton: @Composable () -> Unit = {
        PrimaryButton("Sign out", modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
            FirebaseAuthManager.signOut()
            onSignOut()
        }
    }

    when {
        uiState.isLoading -> FullScreenLoading()
        uiState.customer != null -> {
            val customer = uiState.customer!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(customer.name, style = MaterialTheme.typography.headlineMedium)
                if (customer.email.isNotBlank()) Text(customer.email, style = MaterialTheme.typography.bodyLarge)
                if (customer.mobileNumber.isNotBlank()) Text(customer.mobileNumber, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Loyalty points: ${customer.loyaltyPoints}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("Biometric sign-in", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = customer.biometricEnabled, onCheckedChange = viewModel::toggleBiometric)
                }

                OutlinedTextField(
                    value = customer.loyaltyCardNumber ?: "",
                    onValueChange = { viewModel.linkLoyaltyCard(it) },
                    label = { Text("Loyalty card number") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                signOutButton()
            }
        }
        // Any other outcome (a genuine Firestore error, or a profile that couldn't be healed)
        // still renders something — critically, still offering Sign out, since Profile is the
        // only place that button lives.
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Couldn't load your profile", style = MaterialTheme.typography.headlineMedium)
            uiState.errorMessage?.let {
                ErrorBanner(it, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            signOutButton()
        }
    }
}
