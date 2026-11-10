package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.MediaLinkType
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.repository.TempleRepository
import com.example.sync.DriveSyncManager
import com.example.sync.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

data class BackupPreview(
    val visitsCount: Int,
    val photosCount: Int,
    val placesCount: Int,
    val jsonString: String
)

class TempleViewModel(
    private val context: Context,
    private val repository: TempleRepository,
    private val driveSyncManager: DriveSyncManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("temple_app_prefs", Context.MODE_PRIVATE)

    private val _currentOwnerId = MutableStateFlow<String>("")
    val currentOwnerId: StateFlow<String> = _currentOwnerId.asStateFlow()

    fun setOwnerId(ownerId: String) {
        _currentOwnerId.value = ownerId
    }

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

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allPlaces: StateFlow<List<PlaceEntity>> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getAllPlaces(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allVisits: StateFlow<List<VisitEntity>> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getAllVisits(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val visitedVisits: StateFlow<List<VisitEntity>> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getVisitedVisits(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val wishlistVisits: StateFlow<List<VisitEntity>> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getWishlistVisits(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTrips: StateFlow<List<TripEntity>> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getAllTrips(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val placesCount: StateFlow<Int> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getPlacesCount(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val visitedCount: StateFlow<Int> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getVisitedCount(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val wishlistCount: StateFlow<Int> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getWishlistCount(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allMediaLinks: StateFlow<List<MediaLinkEntity>> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getAllMediaLinks(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaCount: StateFlow<Int> = _currentOwnerId
        .flatMapLatest { ownerId -> repository.getMediaCount(ownerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncState: StateFlow<SyncState> = driveSyncManager.syncState

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
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
            driveSyncManager.syncOnceOnAppLaunch(userEmail, ownerId = _currentOwnerId.value)
        }
    }

    fun triggerDriveSyncManual(userEmail: String?) {
        viewModelScope.launch {
            driveSyncManager.performSync(userEmail, isAppLaunch = false, ownerId = _currentOwnerId.value)
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun seedSampleData() {
        viewModelScope.launch {
            _isSeeding.value = true
            repository.seedSampleTempleVisits(_currentOwnerId.value)
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
            repository.clearAllUserData(_currentOwnerId.value)
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
            val owner = _currentOwnerId.value
            val place = repository.getOrCreatePlace(
                ownerId = owner,
                name = name,
                latitude = latitude,
                longitude = longitude,
                source = "manual"
            )

            val visit = VisitEntity(
                placeId = place.id,
                startDate = startDate,
                endDate = endDate,
                notes = notes,
                categoryIds = categoryIds,
                state = if (isWishlist) VisitState.WISHLIST else VisitState.VISITED,
                deity = deity.ifBlank { null },
                architecture = architecture.ifBlank { null },
                prayers = prayers.ifBlank { null },
                nearbyFood = nearbyFood.ifBlank { null },
                ownerId = owner
            )
            repository.addVisit(visit)

            driveSyncManager.performSync(userEmail, isAppLaunch = false, ownerId = owner)
        }
    }

    fun deleteVisit(visitId: String) {
        viewModelScope.launch {
            repository.deleteVisit(visitId)
        }
    }

    fun getMediaLinksForVisit(visitId: String) = repository.getMediaLinksForVisit(visitId)

    fun linkGalleryMedia(
        visitId: String,
        uriString: String,
        dateTaken: Long? = null,
        hasGps: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            val mediaLink = MediaLinkEntity(
                visitId = visitId,
                type = MediaLinkType.GALLERY,
                externalId = uriString,
                dateTaken = dateTaken ?: System.currentTimeMillis(),
                hasGps = hasGps,
                latitude = latitude,
                longitude = longitude,
                ownerId = _currentOwnerId.value
            )
            repository.addMediaLink(mediaLink)
        }
    }

    fun linkGooglePhotosMedia(visitId: String, linkOrId: String, dateTaken: Long? = null) {
        viewModelScope.launch {
            val mediaLink = MediaLinkEntity(
                visitId = visitId,
                type = MediaLinkType.GOOGLE_PHOTOS,
                externalId = linkOrId.trim(),
                dateTaken = dateTaken ?: System.currentTimeMillis(),
                hasGps = false,
                ownerId = _currentOwnerId.value
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
            val trip = repository.addTrip(_currentOwnerId.value, name.trim(), startDate, endDate, notes)
            onCreated?.invoke(trip.id)
        }
    }

    // Export complete data for current owner to standard versioned JSON backup
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("appName", "Temple Map")
        root.put("ownerId", _currentOwnerId.value)
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
            if (v.nearbyFood != null) obj.put("nearbyFood", v.nearbyFood)
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

        val mediaArray = JSONArray()
        allMediaLinks.value.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("visitId", m.visitId)
            obj.put("type", m.type.name)
            obj.put("externalId", m.externalId)
            obj.put("dateTaken", m.dateTaken)
            obj.put("hasGps", m.hasGps)
            if (m.latitude != null) obj.put("latitude", m.latitude)
            if (m.longitude != null) obj.put("longitude", m.longitude)
            mediaArray.put(obj)
        }
        root.put("mediaLinks", mediaArray)

        return root.toString(2)
    }

    // Storage Access Framework: Export backup to document Uri
    fun exportBackupToUri(context: Context, uri: Uri, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Could not open destination storage document.")
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    // Storage Access Framework: Inspect & validate document Uri to preview contents
    fun parseBackupPreviewFromUri(context: Context, uri: Uri): Result<BackupPreview> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IOException("Could not read backup file from device.")

            val root = JSONObject(jsonString)
            val visits = root.optJSONArray("visits")
            val places = root.optJSONArray("places")
            if (visits == null && places == null) {
                return Result.failure(IllegalArgumentException("File does not contain valid temple diary records."))
            }

            val visitsCount = visits?.length() ?: 0
            val placesCount = places?.length() ?: 0
            val mediaCount = (root.optJSONArray("mediaLinks") ?: root.optJSONArray("media_links"))?.length() ?: 0

            Result.success(BackupPreview(visitsCount, mediaCount, placesCount, jsonString))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid backup file: ${e.message ?: "Not valid JSON"}"))
        }
    }

    // Import from validated backup preview
    fun importFromPreview(preview: BackupPreview, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(preview.jsonString)
                val owner = _currentOwnerId.value
                var count = 0

                val placesArray = root.optJSONArray("places")
                if (placesArray != null) {
                    for (i in 0 until placesArray.length()) {
                        val obj = placesArray.getJSONObject(i)
                        repository.getOrCreatePlace(
                            ownerId = owner,
                            name = obj.getString("name"),
                            latitude = obj.getDouble("latitude"),
                            longitude = obj.getDouble("longitude"),
                            source = obj.optString("source", "import")
                        )
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
                            ownerId = owner,
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
                            deity = if (obj.has("deity") && !obj.isNull("deity")) obj.getString("deity") else null,
                            architecture = if (obj.has("architecture") && !obj.isNull("architecture")) obj.getString("architecture") else null,
                            prayers = if (obj.has("prayers") && !obj.isNull("prayers")) obj.getString("prayers") else null,
                            nearbyFood = if (obj.has("nearbyFood") && !obj.isNull("nearbyFood")) obj.getString("nearbyFood") else null,
                            ownerId = owner
                        )
                        repository.addVisit(visit)
                        count++
                    }
                }

                val mediaArray = root.optJSONArray("mediaLinks") ?: root.optJSONArray("media_links")
                if (mediaArray != null) {
                    val linksToInsert = mutableListOf<MediaLinkEntity>()
                    for (i in 0 until mediaArray.length()) {
                        val obj = mediaArray.getJSONObject(i)
                        val media = MediaLinkEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            visitId = obj.getString("visitId"),
                            type = try {
                                MediaLinkType.valueOf(obj.optString("type", "GALLERY"))
                            } catch (e: Exception) {
                                MediaLinkType.GALLERY
                            },
                            externalId = obj.getString("externalId"),
                            dateTaken = obj.optLong("dateTaken", System.currentTimeMillis()),
                            hasGps = obj.optBoolean("hasGps", false),
                            latitude = if (obj.has("latitude")) obj.getDouble("latitude") else null,
                            longitude = if (obj.has("longitude")) obj.getDouble("longitude") else null,
                            ownerId = owner
                        )
                        linksToInsert.add(media)
                    }
                    if (linksToInsert.isNotEmpty()) {
                        repository.insertMediaLinks(linksToInsert)
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
