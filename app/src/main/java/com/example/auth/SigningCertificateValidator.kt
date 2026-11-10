package com.example.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import java.security.MessageDigest

/**
 * Validates the runtime app signing certificate (SHA-1 / SHA-256) against the expected
 * debug/release keystore fingerprints, ensuring accurate diagnostics for Google Sign-In with Credential Manager.
 */
object SigningCertificateValidator {

    private const val TAG = "SigningValidator"

    // Default expected SHA-1 for the development debug keystore
    val EXPECTED_DEBUG_SHA1: String = BuildConfig.EXPECTED_DEBUG_SHA1.ifBlank {
        "83:D7:48:9A:3C:E4:0B:B5:33:AA:0F:EB:81:5C:50:38:AD:CC:7E:52"
    }

    data class SigningValidationInfo(
        val packageName: String,
        val currentSha1: String,
        val currentSha256: String,
        val expectedSha1: String,
        val isSha1Matched: Boolean,
        val oAuthClientId: String,
        val isClientIdConfigured: Boolean,
        val diagnosticSummary: String
    )

    fun getSigningInfo(context: Context): SigningValidationInfo {
        val packageName = context.packageName
        val oAuthClientId = FirebaseConfigLoader.getWebClientId(context)
        val currentSha1 = calculateCertFingerprint(context, "SHA-1") ?: "UNAVAILABLE"
        val currentSha256 = calculateCertFingerprint(context, "SHA-256") ?: "UNAVAILABLE"

        val isSha1Matched = currentSha1.equals(EXPECTED_DEBUG_SHA1, ignoreCase = true)
        val isClientIdConfigured = oAuthClientId.isNotBlank() && !oAuthClientId.contains("placeholder", ignoreCase = true)

        val summary = when {
            !isClientIdConfigured ->
                "Web Client ID is not configured in firebase-applet-config.json."
            isSha1Matched ->
                "SHA-1 fingerprint validated successfully: matches development keystore."
            currentSha1 != "UNAVAILABLE" ->
                "App is signed with certificate $currentSha1. Ensure this fingerprint is registered in Google Cloud Console."
            else ->
                "Could not extract signing certificate from package manager."
        }

        return SigningValidationInfo(
            packageName = packageName,
            currentSha1 = currentSha1,
            currentSha256 = currentSha256,
            expectedSha1 = EXPECTED_DEBUG_SHA1,
            isSha1Matched = isSha1Matched,
            oAuthClientId = oAuthClientId,
            isClientIdConfigured = isClientIdConfigured,
            diagnosticSummary = summary
        )
    }

    private fun calculateCertFingerprint(context: Context, algorithm: String): String? {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(cert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating $algorithm for ${context.packageName}: ${e.message}", e)
            null
        }
    }

    /**
     * Intercepts Google Sign-In and Credential Manager exceptions and returns a human-readable,
     * actionable diagnostic explanation.
     */
    fun diagnoseSignInException(context: Context, exception: Throwable): String {
        val msg = exception.message.orEmpty()
        val info = getSigningInfo(context)

        return when {
            msg.contains("cancel", ignoreCase = true) -> {
                "Sign-in was cancelled by the user."
            }
            // Status 10 in Google Play Services is DEVELOPER_ERROR: package or SHA-1 mismatch
            msg.contains("10:", ignoreCase = true) ||
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) ||
            msg.contains("UNREGISTERED_ON_API_CONSOLE", ignoreCase = true) -> {
                "Google Play Services: DEVELOPER_ERROR (Status 10).\n" +
                "Google Cloud requires this app's SHA-1 fingerprint (${info.currentSha1}) and package name (${info.packageName}) " +
                "to be registered for OAuth Client ID ${info.oAuthClientId}.\n" +
                "Use Quick Test Sign-in to test the app without Cloud Console changes."
            }
            msg.contains("16:", ignoreCase = true) ||
            msg.contains("NoCredentialException", ignoreCase = true) ||
            msg.contains("No credentials available", ignoreCase = true) -> {
                "No Google Account found on this device or emulator.\n" +
                "Please add a Google account in Android Settings, or use Quick Test Sign-in to test the app immediately."
            }
            msg.contains("NetworkException", ignoreCase = true) -> {
                "Network connection error while contacting Google authentication servers. Please verify your internet connection."
            }
            else -> {
                "Sign-in error: ${exception.localizedMessage ?: "Unknown error"}.\n" +
                "Use Quick Test Sign-in to proceed with testing the app."
            }
        }
    }
}
