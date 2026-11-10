package com.example.auth

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import org.json.JSONObject

/**
 * Loads configuration from firebase-applet-config.json located in assets,
 * falling back to BuildConfig fields generated during build.
 */
object FirebaseConfigLoader {

    private const val TAG = "FirebaseConfigLoader"
    private const val CONFIG_ASSET_NAME = "firebase-applet-config.json"

    data class FirebaseConfig(
        val projectId: String,
        val appId: String,
        val apiKey: String,
        val authDomain: String,
        val storageBucket: String,
        val oAuthClientId: String
    )

    private var cachedConfig: FirebaseConfig? = null

    @Synchronized
    fun loadConfig(context: Context): FirebaseConfig {
        cachedConfig?.let { return it }

        var projectId = ""
        var appId = ""
        var apiKey = ""
        var authDomain = ""
        var storageBucket = ""
        var oAuthClientId = ""

        try {
            context.assets.open(CONFIG_ASSET_NAME).use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)
                projectId = json.optString("projectId", "")
                appId = json.optString("appId", "")
                apiKey = json.optString("apiKey", "")
                authDomain = json.optString("authDomain", "")
                storageBucket = json.optString("storageBucket", "")
                oAuthClientId = json.optString("oAuthClientId", "")
                Log.d(TAG, "Successfully loaded firebase-applet-config.json from assets. Client ID length: ${oAuthClientId.length}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read $CONFIG_ASSET_NAME from assets: ${e.message}. Using BuildConfig fallback.")
        }

        // Fall back to BuildConfig if asset reading didn't provide client ID
        if (oAuthClientId.isBlank() && BuildConfig.OAUTH_WEB_CLIENT_ID.isNotBlank()) {
            oAuthClientId = BuildConfig.OAUTH_WEB_CLIENT_ID
            Log.d(TAG, "Using OAUTH_WEB_CLIENT_ID from BuildConfig")
        }

        val config = FirebaseConfig(
            projectId = projectId,
            appId = appId,
            apiKey = apiKey,
            authDomain = authDomain,
            storageBucket = storageBucket,
            oAuthClientId = oAuthClientId
        )
        cachedConfig = config
        return config
    }

    fun getWebClientId(context: Context): String {
        return loadConfig(context).oAuthClientId
    }
}
