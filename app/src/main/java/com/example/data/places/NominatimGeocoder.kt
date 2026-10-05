package com.example.data.places

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class GeocodedSearchResult(
    val displayName: String,
    val shortTitle: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "place"
)

object NominatimGeocoder {
    private const val TAG = "NominatimGeocoder"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun searchPlaces(query: String): List<GeocodedSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=6&addressdetails=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TempleMapTravelDiary/1.0 (Android App; contact: support@templemap.app)")
                .header("Accept-Language", "en")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Nominatim search returned ${response.code}")
                return@withContext emptyList()
            }

            val bodyString = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(bodyString)
            val results = mutableListOf<GeocodedSearchResult>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val displayName = item.optString("display_name", "")
                val lat = item.optDouble("lat", 0.0)
                val lon = item.optDouble("lon", 0.0)
                val type = item.optString("type", "place")
                val shortTitle = displayName.substringBefore(",")

                if (lat != 0.0 && lon != 0.0 && displayName.isNotBlank()) {
                    results.add(
                        GeocodedSearchResult(
                            displayName = displayName,
                            shortTitle = shortTitle,
                            latitude = lat,
                            longitude = lon,
                            type = type
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim geocoding failed for '$trimmed': ${e.message}")
            emptyList()
        }
    }
}
