package com.example.auth

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val signedInAtMillis: Long = System.currentTimeMillis(),
    val isDemoSession: Boolean = false
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
    data class Error(val message: String) : AuthState
}
