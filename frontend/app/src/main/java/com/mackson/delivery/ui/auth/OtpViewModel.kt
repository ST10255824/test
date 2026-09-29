package com.mackson.delivery.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.mackson.delivery.data.remote.FirebaseAuthManager
import com.mackson.delivery.util.AppResult
import com.mackson.delivery.util.resultOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Implements the SMS OTP flow from Part 1 US-01 ("system must send an SMS OTP within 10 seconds
 * of request; account generation fails on OTP mismatch or a 120-second timeout"). Requires a
 * Blaze-plan Firebase project with SHA-1/SHA-256 fingerprints registered before real SMS will
 * send — see README "Firebase project setup".
 */
data class OtpUiState(
    val phase: Phase = Phase.ENTER_PHONE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedInUserId: String? = null
) {
    enum class Phase { ENTER_PHONE, ENTER_CODE, VERIFIED }
}

class OtpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var verificationId: String? = null

    fun requestOtp(phoneNumber: String, activity: Activity) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Android auto-retrieved the SMS code — sign in immediately without user entry.
                viewModelScope.launch {
                    when (val result = resultOf { FirebaseAuthManager.signInWithPhoneCredential(credential) }) {
                        is AppResult.Success -> _uiState.value = _uiState.value.copy(
                            isLoading = false, phase = OtpUiState.Phase.VERIFIED, signedInUserId = result.data
                        )
                        is AppResult.Error -> _uiState.value =
                            _uiState.value.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "OTP request failed")
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                _uiState.value = _uiState.value.copy(isLoading = false, phase = OtpUiState.Phase.ENTER_CODE)
            }
        }
        FirebaseAuthManager.startPhoneVerification(phoneNumber, activity, callbacks)
    }

    fun confirmCode(code: String) {
        val id = verificationId ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "Request a code first")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = resultOf { FirebaseAuthManager.confirmOtpCode(id, code) }) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false, phase = OtpUiState.Phase.VERIFIED, signedInUserId = result.data
                )
                is AppResult.Error -> _uiState.value =
                    _uiState.value.copy(isLoading = false, errorMessage = "Incorrect code, please try again")
            }
        }
    }
}
