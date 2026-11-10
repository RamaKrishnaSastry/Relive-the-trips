package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun signInWithGoogle(context: Context, serverClientId: String? = null): Result<UserProfile> {
        _isSigningIn.value = true
        _errorMessage.value = null
        val result = repository.signInWithGoogle(context, serverClientId)
        result.onFailure { exception ->
            Log.e(TAG, "Google Sign-In failed with technical error: ${exception.message}", exception)
            _errorMessage.value = mapToFriendlyMessage(exception)
        }
        _isSigningIn.value = false
        return result
    }

    private fun mapToFriendlyMessage(exception: Throwable): String {
        val msg = exception.message.orEmpty()
        return when {
            exception is GetCredentialCancellationException || msg.contains("cancel", ignoreCase = true) -> {
                "Sign-in was cancelled."
            }
            msg.contains("NoCredentialException", ignoreCase = true) ||
            msg.contains("16:", ignoreCase = true) ||
            msg.contains("No credentials available", ignoreCase = true) -> {
                "No Google account found on this device."
            }
            exception is IOException ||
            msg.contains("network", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("UnknownHost", ignoreCase = true) -> {
                "No internet connection. Please check your network."
            }
            exception is SecurityException ||
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) ||
            msg.contains("10:", ignoreCase = true) ||
            msg.contains("UNREGISTERED_ON_API_CONSOLE", ignoreCase = true) ||
            msg.contains("audience", ignoreCase = true) ||
            msg.contains("nonce", ignoreCase = true) -> {
                "Sign-in isn't configured for this app build."
            }
            else -> {
                "Unable to sign in. Please try again."
            }
        }
    }

    fun signInAsDemo(displayName: String = "Explorer Pilgrim") {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "Demo sign-in attempted in release build.")
            return
        }
        _isSigningIn.value = true
        _errorMessage.value = null
        repository.signInAsDemo(displayName)
        _isSigningIn.value = false
    }

    fun signOut() {
        repository.signOut()
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
