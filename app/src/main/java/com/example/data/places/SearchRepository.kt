package com.example.data.places

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.example.data.network.NetworkResult
import com.example.data.repository.TempleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

enum class SearchResultType {
    SAVED, TEMPLE, LANDMARK, CITY, OTHER
}

data class UnifiedSearchResult(
    val id: String,
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val type: SearchResultType,
    val cityState: String? = null,
    val country: String? = null,
    val source: String
)

data class DiscoveredPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val religion: String?,
    val denomination: String?,
    val address: String? = null
)

class SearchRepository(
    private val context: Context,
    private val templeRepository: TempleRepository
) {
    private val TAG = "SearchRepository"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val searchCache = mutableMapOf<String, List<UnifiedSearchResult>>()
    private val overpassCache = mutableMapOf<String, List<DiscoveredPlace>>()
    private var lastRequestTime = 0L
    private var lastOverpassRequestTime = 0L

    suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        // 1. Photon Reverse
        try {
            val url = "https://photon.komoot.io/reverse?lat=$lat&lon=$lon"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TempleMap/1.0 (Android)")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val props = features.getJSONObject(0).optJSONObject("properties")
                        val name = props?.optString("name") ?: props?.optString("city") ?: props?.optString("street")
                        if (!name.isNullOrBlank()) return@withContext name
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Photon reverse failed", e)
        }

        // 2. Nominatim Reverse
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TempleMap/1.0 (Android)")
                .header("Accept-Language", "en")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val displayName = json.optString("display_name")
                    if (displayName.isNotBlank()) {
                        return@withContext displayName.substringBefore(",")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim reverse failed", e)
        }

        // 3. Android Geocoder
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val name = addr.featureName ?: addr.locality ?: addr.adminArea
                if (!name.isNullOrBlank()) return@withContext name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Android reverse failed", e)
        }

        "%.4f, %.4f".format(lat, lon)
    }

    suspend fun searchOverpass(
        south: Double, west: Double, north: Double, east: Double
    ): NetworkResult<List<DiscoveredPlace>> = withContext(Dispatchers.IO) {
        val bboxKey = "%.4f,%.4f,%.4f,%.4f".format(south, west, north, east)
        overpassCache[bboxKey]?.let { return@withContext NetworkResult.Success(it) }

        val now = System.currentTimeMillis()
        val elapsed = now - lastOverpassRequestTime
        if (elapsed < 5000L) {
            delay(5000L - elapsed)
        }
        lastOverpassRequestTime = System.currentTimeMillis()

        try {
            val query = """
                [out:json][timeout:15];
                (
                  node["amenity"="place_of_worship"]($south,$west,$north,$east);
                  way["amenity"="place_of_worship"]($south,$west,$north,$east);
                  relation["amenity"="place_of_worship"]($south,$west,$north,$east);
                );
                out center 100;
            """.trimIndent()

            val requestBody = okhttp3.FormBody.Builder()
                .add("data", query)
                .build()

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", "TempleMap/1.0 (Android)")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext NetworkResult.Error("Overpass error: ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext NetworkResult.Empty
            val json = JSONObject(body)
            val elements = json.getJSONArray("elements")
            val results = mutableListOf<DiscoveredPlace>()

            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags") ?: continue
                val name = tags.optString("name")
                if (name.isNullOrBlank()) continue

                val lat = el.optDouble("lat", el.optJSONObject("center")?.optDouble("lat") ?: 0.0)
                val lon = el.optDouble("lon", el.optJSONObject("center")?.optDouble("lon") ?: 0.0)

                results.add(
                    DiscoveredPlace(
                        id = "overpass_${el.optLong("id", i.toLong())}",
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        religion = tags.optString("religion"),
                        denomination = tags.optString("denomination")
                    )
                )
            }

            if (results.isNotEmpty()) {
                overpassCache[bboxKey] = results
                NetworkResult.Success(results)
            } else {
                NetworkResult.Empty
            }
        } catch (e: java.net.UnknownHostException) {
            NetworkResult.Offline
        } catch (e: Exception) {
            Log.e(TAG, "Overpass search failed", e)
            NetworkResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun search(
        query: String,
        mapLat: Double?,
        mapLon: Double?,
        isOnline: Boolean,
        ownerId: String
    ): NetworkResult<List<UnifiedSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 3) return@withContext NetworkResult.Empty

        // 1. Check Cache
        searchCache[trimmed]?.let { return@withContext NetworkResult.Success(it) }

        val results = mutableListOf<UnifiedSearchResult>()

        // 2. Local Saved Places
        try {
            val savedPlaces = templeRepository.getAllPlaces(ownerId).first()
            savedPlaces.forEach { place ->
                if (place.name.contains(trimmed, ignoreCase = true)) {
                    results.add(
                        UnifiedSearchResult(
                            id = place.id,
                            name = place.name,
                            displayName = place.name,
                            latitude = place.latitude,
                            longitude = place.longitude,
                            type = SearchResultType.SAVED,
                            source = "local_db"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching saved places", e)
        }

        // 3. TemplePlacesCatalog (Local Offline)
        try {
            val catalogResults = TemplePlacesCatalog.searchTemples(trimmed)
            catalogResults.forEach { temple ->
                results.add(
                    UnifiedSearchResult(
                        id = "catalog_${temple.name}_${temple.latitude}",
                        name = temple.name,
                        displayName = temple.name,
                        latitude = temple.latitude,
                        longitude = temple.longitude,
                        type = SearchResultType.TEMPLE,
                        cityState = temple.cityState,
                        source = "catalog"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching catalog", e)
        }

        if (isOnline) {
            // Enforce rate limiting: at most 1 request per second to public geocoders
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < 1000L) {
                delay(1000L - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()

            val onlineResults = fetchOnlineResults(trimmed, mapLat, mapLon)
            results.addAll(onlineResults)
        }

        // Deduplicate: Name + coordinates rounded to 4 decimals
        val deduplicated = results.distinctBy { res ->
            val rLat = (res.latitude * 10000.0).roundToLong()
            val rLng = (res.longitude * 10000.0).roundToLong()
            "${res.name.lowercase().trim()}_${rLat}_${rLng}"
        }

        val finalResults = deduplicated.take(15) // Fetch more then UI filters to 8
        if (finalResults.isNotEmpty()) {
            searchCache[trimmed] = finalResults
        }

        if (finalResults.isEmpty() && !isOnline) return@withContext NetworkResult.Offline
        if (finalResults.isEmpty()) return@withContext NetworkResult.Empty

        NetworkResult.Success(finalResults)
    }

    private suspend fun fetchOnlineResults(
        query: String,
        mapLat: Double?,
        mapLon: Double?
    ): List<UnifiedSearchResult> {
        // 4. Photon API
        val photonResults = fetchPhoton(query, mapLat, mapLon)
        if (photonResults.isNotEmpty()) return photonResults

        // 5. Nominatim Fallback
        val nominatimResults = fetchNominatim(query)
        if (nominatimResults.isNotEmpty()) return nominatimResults

        // 6. Android Geocoder Fallback
        return fetchAndroidGeocoder(query)
    }

    private suspend fun fetchPhoton(
        query: String,
        mapLat: Double?,
        mapLon: Double?
    ): List<UnifiedSearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            var url = "https://photon.komoot.io/api/?q=$encodedQuery&limit=8"
            if (mapLat != null && mapLon != null) {
                url += "&lat=$mapLat&lon=$mapLon"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TempleMap/1.0 (Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val features = json.getJSONArray("features")
            val results = mutableListOf<UnifiedSearchResult>()

            for (i in 0 until features.length()) {
                val feat = features.getJSONObject(i)
                val props = feat.getJSONObject("properties")
                val geom = feat.getJSONObject("geometry")
                val coords = geom.getJSONArray("coordinates") // Photon returns [lon, lat]

                val name = props.optString("name", props.optString("city", "Unknown"))
                val typeStr = props.optString("osm_value", "")
                val type = when {
                    typeStr.contains("temple", true) || typeStr.contains("place_of_worship", true) -> SearchResultType.TEMPLE
                    typeStr.contains("city", true) || typeStr.contains("town", true) -> SearchResultType.CITY
                    else -> SearchResultType.LANDMARK
                }

                results.add(
                    UnifiedSearchResult(
                        id = "photon_${props.optLong("osm_id", i.toLong())}",
                        name = name,
                        displayName = name,
                        latitude = coords.getDouble(1),
                        longitude = coords.getDouble(0),
                        type = type,
                        cityState = props.optString("city", props.optString("state", null)),
                        country = props.optString("country", null),
                        source = "photon"
                    )
                )
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Photon fetch failed", e)
            emptyList()
        }
    }

    private suspend fun fetchNominatim(query: String): List<UnifiedSearchResult> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=8&addressdetails=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TempleMap/1.0 (Android)")
                .header("Accept-Language", "en")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val array = JSONArray(body)
            val results = mutableListOf<UnifiedSearchResult>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val addr = obj.optJSONObject("address")
                val name = addr?.optString("temple", addr.optString("place_of_worship", addr.optString("amenity", obj.optString("display_name").substringBefore(",")))) ?: obj.optString("display_name").substringBefore(",")
                
                results.add(
                    UnifiedSearchResult(
                        id = "nominatim_${obj.optLong("osm_id", i.toLong())}",
                        name = name,
                        displayName = obj.getString("display_name"),
                        latitude = obj.getDouble("lat"),
                        longitude = obj.getDouble("lon"),
                        type = SearchResultType.LANDMARK,
                        cityState = addr?.optString("city", addr?.optString("state", null)),
                        country = addr?.optString("country", null),
                        source = "nominatim"
                    )
                )
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim fetch failed", e)
            emptyList()
        }
    }

    private fun fetchAndroidGeocoder(query: String): List<UnifiedSearchResult> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 8) ?: return emptyList()
            addresses.mapIndexed { index, addr ->
                UnifiedSearchResult(
                    id = "geocoder_$index",
                    name = addr.featureName ?: query,
                    displayName = addr.getAddressLine(0) ?: query,
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    type = SearchResultType.OTHER,
                    cityState = addr.locality ?: addr.adminArea,
                    country = addr.countryName,
                    source = "android_geocoder"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Android Geocoder failed", e)
            emptyList()
        }
    }
}
