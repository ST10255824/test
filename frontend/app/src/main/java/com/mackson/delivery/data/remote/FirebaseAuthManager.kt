package com.mackson.delivery.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mackson.delivery.data.model.UserRole
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Singleton wrapper around the FirebaseAuth SDK (Part 1, section 9.3.9: "FirebaseAuthManager").
 * Kotlin's `object` declaration guarantees a single instance and a single connection context
 * for the app's lifetime — the Singleton pattern documented in section 9.3.12.
 *
 * Two sign-in paths are wired up: email/password (always available, used for the marking demo
 * and CI) and SMS OTP via PhoneAuthProvider (the path documented in Part 1 US-01). Real OTP
 * delivery requires a Blaze-plan Firebase project with SHA-1/SHA-256 fingerprints registered —
 * see README "Firebase project setup" for the exact steps once you have your own project.
 */
object FirebaseAuthManager {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Best-effort contact details for the signed-in user — used to self-heal a missing
     * Firestore profile document (see ProfileViewModel), since only the email/password
     * `registerUser` path creates one; a phone-OTP sign-in has no such step. */
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    fun getCurrentUserPhone(): String? = auth.currentUser?.phoneNumber

    fun isSignedIn(): Boolean = auth.currentUser != null

    suspend fun registerUser(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: error("Registration succeeded but no user id was returned")
    }

    suspend fun loginUser(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: error("Login succeeded but no user id was returned")
    }

    fun signOut() = auth.signOut()

    /** Starts SMS OTP verification for the given phone number, e.g. "+27821234567". */
    fun startPhoneVerification(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(120L, TimeUnit.SECONDS) // matches US-01 acceptance criteria: 120s timeout
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun confirmOtpCode(verificationId: String, smsCode: String): String {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        return signInWithPhoneCredential(credential)
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): String {
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid ?: error("Phone sign-in succeeded but no user id was returned")
    }

    /**
     * Reads the `role` custom claim set on the user's ID token by the `assignUserRole`
     * Cloud Function (functions/src/auth.ts). Falls back to CUSTOMER if unset, which is the
     * safe default — every other role must be explicitly granted server-side.
     */
    suspend fun verifyAdminPermissions(): UserRole {
        val tokenResult = auth.currentUser?.getIdToken(true)?.await()
        val role = tokenResult?.claims?.get("role") as? String
        return UserRole.fromString(role)
    }
}
