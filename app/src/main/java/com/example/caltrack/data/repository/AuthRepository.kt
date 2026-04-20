package com.example.caltrack.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class AuthRepository {

    private fun authOrNull(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (_: IllegalStateException) {
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return authOrNull()?.currentUser != null
    }

    fun currentUserEmail(): String? {
        return authOrNull()?.currentUser?.email
    }

    fun register(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val auth = authOrNull() ?: run {
            onError(FIREBASE_NOT_CONFIGURED)
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onError(mapAuthError(exception)) }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val auth = authOrNull() ?: run {
            onError(FIREBASE_NOT_CONFIGURED)
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onError(mapAuthError(exception)) }
    }

    fun signOut() {
        authOrNull()?.signOut()
    }

    private fun mapAuthError(exception: Exception): String {
        if (exception is FirebaseNetworkException) {
            return "Network error. Check your connection and try again."
        }

        if (exception is FirebaseAuthException) {
            return when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already registered."
                "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
                "ERROR_USER_NOT_FOUND" -> "No account found with that email."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
                else -> exception.localizedMessage ?: "Authentication failed."
            }
        }

        return exception.localizedMessage ?: "Authentication failed."
    }

    companion object {
        const val FIREBASE_NOT_CONFIGURED =
            "Firebase is not configured yet. Add app/google-services.json, then sync project."
    }
}
