package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
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
                idToken = null,
                signedInAtMillis = prefs.getLong(KEY_SIGNED_IN_AT, System.currentTimeMillis()),
                isDemoSession = prefs.getBoolean(KEY_IS_DEMO, false)
            )
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signInWithGoogle(activityContext: Context, serverClientId: String? = null): Result<UserProfile> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)

            // Raw nonce and SHA-256 hash for replay protection
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray(Charsets.UTF_8))
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val effectiveClientId = if (!serverClientId.isNullOrBlank()) {
                serverClientId
            } else {
                FirebaseConfigLoader.getWebClientId(activityContext).ifBlank {
                    DEFAULT_OAUTH_CLIENT_ID
                }
            }

            val signInOption = GetSignInWithGoogleOption.Builder(effectiveClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Parse & validate JWT payload
                val parts = idToken.split(".")
                if (parts.size < 2) {
                    throw SecurityException("Malformed Google ID token payload structure.")
                }

                val payloadJson = String(
                    Base64.decode(
                        parts[1],
                        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                    ),
                    Charsets.UTF_8
                )
                val payload = JSONObject(payloadJson)

                // 1. Check audience equals web client ID
                val aud = payload.optString("aud", "")
                if (aud != effectiveClientId) {
                    Log.e(TAG, "Audience check failed. Expected: $effectiveClientId, found: $aud")
                    throw SecurityException("Sign-in verification failed: Token audience does not match configured web client ID.")
                }

                // 2. Check expiry is in the future
                val expSeconds = payload.optLong("exp", 0L)
                val currentSeconds = System.currentTimeMillis() / 1000
                if (expSeconds <= currentSeconds) {
                    Log.e(TAG, "Token expiry check failed. exp: $expSeconds, now: $currentSeconds")
                    throw SecurityException("Sign-in verification failed: Google ID token has expired.")
                }

                // 3. Check nonce hash matches
                val tokenNonce = payload.optString("nonce", "")
                if (tokenNonce != hashedNonce) {
                    Log.e(TAG, "Nonce check failed. Expected: $hashedNonce, found: $tokenNonce")
                    throw SecurityException("Sign-in verification failed: Cryptographic nonce mismatch.")
                }

                // 4. Stable sub as user id, never email
                val sub = payload.optString("sub", "")
                if (sub.isBlank()) {
                    Log.e(TAG, "Missing subject claim 'sub' in ID token payload.")
                    throw SecurityException("Sign-in verification failed: Missing stable user identifier (sub).")
                }

                val email = payload.optString("email", googleIdTokenCredential.id)
                val displayName = payload.optString("name", googleIdTokenCredential.displayName ?: email.substringBefore("@"))
                val photoUrl = payload.optString("picture", googleIdTokenCredential.profilePictureUri?.toString()).ifBlank { null }

                val user = UserProfile(
                    id = sub,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    idToken = null,
                    signedInAtMillis = System.currentTimeMillis(),
                    isDemoSession = false
                )
                saveSession(user)
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                val msg = "Unexpected credential returned: ${credential::class.java.simpleName}"
                Log.e(TAG, msg)
                Result.failure(IllegalStateException(msg))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.e(TAG, "Google Sign-in was cancelled by user: ${e.message}", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google CredentialManager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-in exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signInLocally(
        displayName: String = "Pilgrim Explorer",
        email: String = "local.user@templemap.app"
    ): UserProfile {
        val userId = prefs.getString(KEY_USER_ID, null) ?: "local_${UUID.randomUUID().toString().take(8)}"
        val user = UserProfile(
            id = userId,
            email = email,
            displayName = displayName.ifBlank { "Pilgrim Explorer" },
            photoUrl = prefs.getString(KEY_USER_PHOTO, null),
            idToken = null,
            signedInAtMillis = System.currentTimeMillis(),
            isDemoSession = true
        )
        saveSession(user)
        _authState.value = AuthState.Authenticated(user)
        return user
    }

    fun signInAsDemo(
        displayName: String = "Explorer Pilgrim"
    ): UserProfile? {
        return signInLocally(displayName)
    }

    fun updateUserProfile(displayName: String, email: String, photoUrl: String?): UserProfile? {
        val current = (_authState.value as? AuthState.Authenticated)?.user ?: return null
        val updated = current.copy(
            displayName = displayName.ifBlank { current.displayName },
            email = email.ifBlank { current.email },
            photoUrl = if (photoUrl?.isBlank() == true) null else photoUrl
        )
        saveSession(updated)
        _authState.value = AuthState.Authenticated(updated)
        return updated
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
            .putLong(KEY_SIGNED_IN_AT, user.signedInAtMillis)
            .putBoolean(KEY_IS_DEMO, user.isDemoSession)
            .apply()
    }

    companion object {
        private const val TAG = "AuthRepository"
        const val DEFAULT_OAUTH_CLIENT_ID = "445159597263-up7cnu2io7jpj27jhgvotl2pg8emkrjg.apps.googleusercontent.com"

        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_SIGNED_IN_AT = "signed_in_at"
        private const val KEY_IS_DEMO = "is_demo"
    }
}
