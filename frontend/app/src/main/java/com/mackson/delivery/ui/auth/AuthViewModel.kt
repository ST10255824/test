package com.mackson.delivery.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mackson.delivery.data.model.Customer
import com.mackson.delivery.data.model.UserRole
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.data.remote.FirestoreRepository
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedInUserId: String? = null,
    val role: UserRole = UserRole.CUSTOMER
)

/**
 * ViewModel (MVVM pattern, Part 1 section 9.3.12): exposes state via StateFlow and formats
 * repository results for the View, which never talks to Firebase directly.
 */
class AuthViewModel(
    private val authManager: FirebaseAuthManager = FirebaseAuthManager,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        authManager.getCurrentUserId()?.let { uid ->
            _uiState.value = _uiState.value.copy(signedInUserId = uid)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = resultOf { authManager.loginUser(email.trim(), password) }) {
                is AppResult.Success -> onSignedIn(result.data)
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun register(name: String, email: String, mobile: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = resultOf { authManager.registerUser(email.trim(), password) }) {
                is AppResult.Success -> {
                    val customer = Customer(
                        customerId = result.data,
                        name = name.trim(),
                        email = email.trim(),
                        mobileNumber = mobile.trim()
                    )
                    resultOf { firestoreRepository.saveCustomerProfile(customer) }
                    onSignedIn(result.data)
                }
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    private suspend fun onSignedIn(uid: String) {
        val role = resultOf { authManager.verifyAdminPermissions() }
            .let { if (it is AppResult.Success) it.data else UserRole.CUSTOMER }
        _uiState.value = _uiState.value.copy(isLoading = false, signedInUserId = uid, role = role)
    }

    fun signOut() {
        authManager.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
