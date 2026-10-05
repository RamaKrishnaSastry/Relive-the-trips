package com.example.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signInWithGoogle(context: Context, serverClientId: String? = null) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = repository.signInWithGoogle(context, serverClientId)
            result.onFailure { exception ->
                val errorText = when {
                    exception.message?.contains("cancel", ignoreCase = true) == true ->
                        "Sign-in was cancelled"
                    exception.message?.contains("16:", ignoreCase = true) == true ||
                    exception.message?.contains("NoCredentialException", ignoreCase = true) == true ->
                        "No Google Account on this device/emulator. Use Quick Test Sign-in to test the app."
                    else -> "Sign-in error: ${exception.localizedMessage ?: "Unknown error"}. Use Quick Test Sign-in to proceed."
                }
                _errorMessage.value = errorText
            }
            _isSigningIn.value = false
        }
    }

    fun signInAsDemo(email: String = "explorer.pilgrim@templemap.app", displayName: String = "Explorer Pilgrim") {
        _isSigningIn.value = true
        _errorMessage.value = null
        repository.signInAsDemo(email, displayName)
        _isSigningIn.value = false
    }

    fun signOut() {
        repository.signOut()
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
