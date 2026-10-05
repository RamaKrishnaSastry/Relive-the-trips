package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("temple_map_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val user = UserProfile(
                id = prefs.getString(KEY_USER_ID, "") ?: "",
                email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
                displayName = prefs.getString(KEY_USER_NAME, "Temple Pilgrim") ?: "Temple Pilgrim",
                photoUrl = prefs.getString(KEY_USER_PHOTO, null),
                idToken = prefs.getString(KEY_ID_TOKEN, null),
                signedInAtMillis = prefs.getLong(KEY_SIGNED_IN_AT, System.currentTimeMillis()),
                isDemoSession = prefs.getBoolean(KEY_IS_DEMO, false)
            )
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInWithGoogle(activityContext: Context, serverClientId: String?): Result<UserProfile> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            
            // Build raw nonce
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Use configured server client ID or standard OAuth web client ID
            val effectiveClientId = if (!serverClientId.isNullOrBlank()) {
                serverClientId
            } else {
                "774619379658-placeholder.apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(effectiveClientId)
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = UserProfile(
                    id = googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    idToken = googleIdTokenCredential.idToken,
                    signedInAtMillis = System.currentTimeMillis(),
                    isDemoSession = false
                )
                saveSession(user)
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                val msg = "Unexpected credential type returned: ${credential::class.java.simpleName}"
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Google Sign-in was cancelled by user: ${e.message}")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google CredentialManager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "General sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signInAsDemo(email: String = "ramakrishna.cds.nitk@gmail.com", displayName: String = "Ramakrishna"): UserProfile {
        val user = UserProfile(
            id = "demo_${UUID.randomUUID().toString().take(8)}",
            email = email,
            displayName = displayName,
            photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
            idToken = null,
            signedInAtMillis = System.currentTimeMillis(),
            isDemoSession = true
        )
        saveSession(user)
        _authState.value = AuthState.Authenticated(user)
        return user
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    private fun saveSession(user: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .putString(KEY_ID_TOKEN, user.idToken)
            .putLong(KEY_SIGNED_IN_AT, user.signedInAtMillis)
            .putBoolean(KEY_IS_DEMO, user.isDemoSession)
            .apply()
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_SIGNED_IN_AT = "signed_in_at"
        private const val KEY_IS_DEMO = "is_demo"
    }
}
