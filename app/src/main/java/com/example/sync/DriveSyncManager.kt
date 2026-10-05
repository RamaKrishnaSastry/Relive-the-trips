package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.repository.TempleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val message: String, val timestamp: Long) : SyncState
    data class Offline(val message: String) : SyncState
    data class Error(val error: String) : SyncState
}

class DriveSyncManager(
    private val context: Context,
    private val repository: TempleRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("temple_map_drive_sync_prefs", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var hasSyncedThisSession = false

    fun getLastSyncTimeString(): String {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        if (lastSync == 0L) return "Not backed up yet"
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return "Backup saved ${sdf.format(Date(lastSync))}"
    }

    suspend fun syncOnceOnAppLaunch(userEmail: String?): Boolean {
        if (hasSyncedThisSession) {
            Log.d(TAG, "Already backed up during this app session, skipping duplicate run.")
            return true
        }
        return performSync(userEmail = userEmail, isAppLaunch = true)
    }

    suspend fun performSync(userEmail: String?, isAppLaunch: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing
        try {
            // 1. Gather all local Room records including media links and trips
            val places = repository.allPlaces.first()
            val visits = repository.allVisits.first()
            val categories = repository.allCategories.first()
            val trips = repository.allTrips.first()
            val mediaLinks = repository.allMediaLinks.first()

            val jsonPayload = serializeBackup(
                userEmail = userEmail ?: "unknown",
                places = places,
                visits = visits,
                categories = categories,
                trips = trips,
                mediaLinks = mediaLinks
            )

            // 2. Save snapshot locally to private app storage for cache/offline recovery
            val syncFile = File(context.filesDir, "travel_diary_autosave.json")
            syncFile.writeText(jsonPayload)

            // 3. Mark last backup timestamp
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_LAST_SYNC, now)
                .putString(KEY_LAST_SYNC_USER, userEmail ?: "")
                .apply()

            hasSyncedThisSession = true
            val successMsg = if (isAppLaunch) {
                "Auto-Backup synced on launch (${visits.size} visits, ${mediaLinks.size} photos)"
            } else {
                "Auto-Backup saved (${visits.size} visits, ${mediaLinks.size} photos)"
            }
            _syncState.value = SyncState.Success(successMsg, now)
            Log.i(TAG, "Backup sync succeeded: $successMsg")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Backup sync error", e)
            _syncState.value = SyncState.Offline("Working offline with local Room DB")
            false
        }
    }

    private fun serializeBackup(
        userEmail: String,
        places: List<PlaceEntity>,
        visits: List<VisitEntity>,
        categories: List<CategoryEntity>,
        trips: List<TripEntity>,
        mediaLinks: List<MediaLinkEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Travel Temple Map")
        root.put("userEmail", userEmail)
        root.put("timestamp", System.currentTimeMillis())

        val placesArr = JSONArray()
        places.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("latitude", p.latitude)
            obj.put("longitude", p.longitude)
            obj.put("source", p.source)
            placesArr.put(obj)
        }
        root.put("places", placesArr)

        val visitsArr = JSONArray()
        visits.forEach { v ->
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("placeId", v.placeId)
            obj.put("startDate", v.startDate)
            v.endDate?.let { obj.put("endDate", it) }
            obj.put("notes", v.notes ?: "")
            obj.put("categoryIds", JSONArray(v.categoryIds))
            if (v.tripId != null) obj.put("tripId", v.tripId)
            obj.put("state", v.state.name)
            obj.put("deity", v.deity ?: "")
            obj.put("architecture", v.architecture ?: "")
            obj.put("prayers", v.prayers ?: "")
            visitsArr.put(obj)
        }
        root.put("visits", visitsArr)

        val categoriesArr = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("isDefault", c.isDefault)
            obj.put("colorHex", c.colorHex)
            categoriesArr.put(obj)
        }
        root.put("categories", categoriesArr)

        val tripsArr = JSONArray()
        trips.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("startDate", t.startDate)
            t.endDate?.let { obj.put("endDate", it) }
            obj.put("notes", t.notes ?: "")
            tripsArr.put(obj)
        }
        root.put("trips", tripsArr)

        val mediaArr = JSONArray()
        mediaLinks.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("visitId", m.visitId)
            obj.put("type", m.type.name)
            obj.put("externalId", m.externalId)
            obj.put("dateTaken", m.dateTaken)
            obj.put("hasGps", m.hasGps)
            if (m.latitude != null) obj.put("latitude", m.latitude)
            if (m.longitude != null) obj.put("longitude", m.longitude)
            mediaArr.put(obj)
        }
        root.put("mediaLinks", mediaArr)

        return root.toString(2)
    }

    companion object {
        private const val TAG = "DriveSyncManager"
        private const val KEY_LAST_SYNC = "key_last_sync_timestamp"
        private const val KEY_LAST_SYNC_USER = "key_last_sync_user"
    }
}
