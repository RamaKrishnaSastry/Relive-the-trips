package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.repository.TempleRepository
import com.example.sync.DriveSyncManager
import com.example.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

class TempleViewModel(
    private val context: Context,
    private val repository: TempleRepository,
    private val driveSyncManager: DriveSyncManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("temple_app_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val _defaultMapLayer = MutableStateFlow(
        prefs.getString("default_map_layer", "SATELLITE") ?: "SATELLITE"
    )
    val defaultMapLayer: StateFlow<String> = _defaultMapLayer.asStateFlow()

    fun setDefaultMapLayer(layer: String) {
        _defaultMapLayer.value = layer
        prefs.edit().putString("default_map_layer", layer).apply()
    }

    val allPlaces: StateFlow<List<PlaceEntity>> = repository.allPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVisits: StateFlow<List<VisitEntity>> = repository.allVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visitedVisits: StateFlow<List<VisitEntity>> = repository.visitedVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistVisits: StateFlow<List<VisitEntity>> = repository.wishlistVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val placesCount: StateFlow<Int> = repository.placesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val visitedCount: StateFlow<Int> = repository.visitedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wishlistCount: StateFlow<Int> = repository.wishlistCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mediaCount: StateFlow<Int> = repository.mediaCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncState: StateFlow<SyncState> = driveSyncManager.syncState

    private val _selectedCategoryId = MutableStateFlow<String?>(null) // null = All
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    fun syncWithDriveOnLaunch(userEmail: String?) {
        viewModelScope.launch {
            driveSyncManager.syncOnceOnAppLaunch(userEmail)
        }
    }

    fun triggerDriveSyncManual(userEmail: String?) {
        viewModelScope.launch {
            driveSyncManager.performSync(userEmail, isAppLaunch = false)
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun seedSampleData() {
        viewModelScope.launch {
            _isSeeding.value = true
            repository.seedSampleTempleVisits()
            _isSeeding.value = false
        }
    }

    fun addCustomCategory(name: String, iconName: String = "custom", colorHex: String = "#EA580C") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name.trim(), iconName, colorHex)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllUserData()
            repository.ensureDefaultCategories()
        }
    }

    fun addFullTempleVisit(
        name: String,
        latitude: Double,
        longitude: Double,
        startDate: Long,
        endDate: Long?,
        categoryIds: List<String>,
        deity: String,
        architecture: String,
        prayers: String,
        nearbyFood: String,
        notes: String,
        isWishlist: Boolean,
        userEmail: String? = null
    ) {
        viewModelScope.launch {
            val place = PlaceEntity(
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                source = "manual"
            )
            repository.addPlace(place)

            val visit = VisitEntity(
                placeId = place.id,
                startDate = startDate,
                endDate = endDate,
                notes = notes,
                categoryIds = categoryIds,
                state = if (isWishlist) VisitState.WISHLIST else VisitState.VISITED,
                deity = deity.ifBlank { null },
                architecture = architecture.ifBlank { null },
                prayers = prayers.ifBlank { null }
            )
            repository.addVisit(visit)

            // Auto-sync with Drive
            driveSyncManager.performSync(userEmail, isAppLaunch = false)
        }
    }

    fun deleteVisit(visitId: String) {
        viewModelScope.launch {
            repository.deleteVisit(visitId)
        }
    }

    fun getMediaLinksForVisit(visitId: String) = repository.getMediaLinksForVisit(visitId)

    fun linkGalleryMedia(visitId: String, uriString: String, dateTaken: Long? = null, hasGps: Boolean = false) {
        viewModelScope.launch {
            val mediaLink = com.example.data.model.MediaLinkEntity(
                visitId = visitId,
                type = com.example.data.model.MediaLinkType.GALLERY,
                externalId = uriString,
                dateTaken = dateTaken ?: System.currentTimeMillis(),
                hasGps = hasGps
            )
            repository.addMediaLink(mediaLink)
        }
    }

    fun linkGooglePhotosMedia(visitId: String, linkOrId: String, dateTaken: Long? = null) {
        viewModelScope.launch {
            val mediaLink = com.example.data.model.MediaLinkEntity(
                visitId = visitId,
                type = com.example.data.model.MediaLinkType.GOOGLE_PHOTOS,
                externalId = linkOrId.trim(),
                dateTaken = dateTaken ?: System.currentTimeMillis(),
                hasGps = false
            )
            repository.addMediaLink(mediaLink)
        }
    }

    fun unlinkMedia(mediaLinkId: String) {
        viewModelScope.launch {
            repository.deleteMediaLink(mediaLinkId)
        }
    }

    fun toggleVisitState(visit: VisitEntity) {
        viewModelScope.launch {
            val newState = if (visit.state == VisitState.VISITED) VisitState.WISHLIST else VisitState.VISITED
            repository.updateVisit(visit.copy(state = newState))
        }
    }

    fun updateVisit(visit: VisitEntity) {
        viewModelScope.launch {
            repository.updateVisit(visit)
        }
    }

    fun assignVisitToTrip(visitId: String, tripId: String?) {
        viewModelScope.launch {
            val visit = allVisits.value.firstOrNull { it.id == visitId } ?: return@launch
            repository.updateVisit(visit.copy(tripId = tripId))
        }
    }

    fun createTrip(name: String, startDate: Long, endDate: Long?, notes: String? = null, onCreated: ((String) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val trip = repository.addTrip(name.trim(), startDate, endDate, notes)
            onCreated?.invoke(trip.id)
        }
    }

    // Export complete data to standard JSON backup
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Temple Map")
        root.put("exportedAt", System.currentTimeMillis())

        val placesArray = JSONArray()
        allPlaces.value.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("latitude", p.latitude)
            obj.put("longitude", p.longitude)
            obj.put("source", p.source)
            placesArray.put(obj)
        }
        root.put("places", placesArray)

        val visitsArray = JSONArray()
        allVisits.value.forEach { v ->
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("placeId", v.placeId)
            obj.put("startDate", v.startDate)
            if (v.endDate != null) obj.put("endDate", v.endDate)
            obj.put("notes", v.notes ?: "")
            obj.put("categoryIds", JSONArray(v.categoryIds))
            if (v.tripId != null) obj.put("tripId", v.tripId)
            obj.put("state", v.state.name)
            if (v.deity != null) obj.put("deity", v.deity)
            if (v.architecture != null) obj.put("architecture", v.architecture)
            if (v.prayers != null) obj.put("prayers", v.prayers)
            visitsArray.put(obj)
        }
        root.put("visits", visitsArray)

        val categoriesArray = JSONArray()
        allCategories.value.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("isDefault", c.isDefault)
            obj.put("colorHex", c.colorHex)
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        val tripsArray = JSONArray()
        allTrips.value.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("startDate", t.startDate)
            if (t.endDate != null) obj.put("endDate", t.endDate)
            if (t.notes != null) obj.put("notes", t.notes)
            tripsArray.put(obj)
        }
        root.put("trips", tripsArray)

        return root.toString(2)
    }

    // Import from JSON backup
    fun importBackupJson(jsonString: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                var count = 0

                val placesArray = root.optJSONArray("places")
                if (placesArray != null) {
                    for (i in 0 until placesArray.length()) {
                        val obj = placesArray.getJSONObject(i)
                        val place = PlaceEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                            source = obj.optString("source", "import")
                        )
                        repository.addPlace(place)
                    }
                }

                val categoriesArray = root.optJSONArray("categories")
                if (categoriesArray != null) {
                    for (i in 0 until categoriesArray.length()) {
                        val obj = categoriesArray.getJSONObject(i)
                        repository.addCategory(
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "custom"),
                            colorHex = obj.optString("colorHex", "#D97706")
                        )
                    }
                }

                val tripsArray = root.optJSONArray("trips")
                if (tripsArray != null) {
                    for (i in 0 until tripsArray.length()) {
                        val obj = tripsArray.getJSONObject(i)
                        repository.addTrip(
                            name = obj.getString("name"),
                            startDate = obj.getLong("startDate"),
                            endDate = if (obj.has("endDate")) obj.getLong("endDate") else null,
                            notes = obj.optString("notes", null)
                        )
                    }
                }

                val visitsArray = root.optJSONArray("visits")
                if (visitsArray != null) {
                    for (i in 0 until visitsArray.length()) {
                        val obj = visitsArray.getJSONObject(i)
                        val catIdsList = mutableListOf<String>()
                        val catArr = obj.optJSONArray("categoryIds")
                        if (catArr != null) {
                            for (j in 0 until catArr.length()) {
                                catIdsList.add(catArr.getString(j))
                            }
                        }
                        val visit = VisitEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            placeId = obj.getString("placeId"),
                            startDate = obj.getLong("startDate"),
                            endDate = if (obj.has("endDate")) obj.getLong("endDate") else null,
                            notes = obj.optString("notes", null),
                            categoryIds = catIdsList,
                            tripId = obj.optString("tripId", null),
                            state = try {
                                VisitState.valueOf(obj.optString("state", "VISITED"))
                            } catch (e: Exception) {
                                VisitState.VISITED
                            },
                            deity = obj.optString("deity", null),
                            architecture = obj.optString("architecture", null),
                            prayers = obj.optString("prayers", null)
                        )
                        repository.addVisit(visit)
                        count++
                    }
                }
                onResult(Result.success(count))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    class Factory(
        private val context: Context,
        private val repository: TempleRepository,
        private val driveSyncManager: DriveSyncManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TempleViewModel(context, repository, driveSyncManager) as T
        }
    }
}
