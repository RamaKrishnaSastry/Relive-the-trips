package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.MediaLinkDao
import com.example.data.db.PlaceDao
import com.example.data.db.TempleMapDatabase
import com.example.data.db.TripDao
import com.example.data.db.VisitDao
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.MediaLinkType
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.model.VisitWithDetails
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TempleRepository(
    private val placeDao: PlaceDao,
    private val categoryDao: CategoryDao,
    private val tripDao: TripDao,
    private val visitDao: VisitDao,
    private val mediaLinkDao: MediaLinkDao
) {
    val allPlaces: Flow<List<PlaceEntity>> = placeDao.getAllPlaces()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()
    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits()
    val visitedVisits: Flow<List<VisitEntity>> = visitDao.getVisitsByState(VisitState.VISITED)
    val wishlistVisits: Flow<List<VisitEntity>> = visitDao.getVisitsByState(VisitState.WISHLIST)
    val allMediaLinks: Flow<List<MediaLinkEntity>> = mediaLinkDao.getAllMediaLinks()

    val placesCount: Flow<Int> = placeDao.getPlacesCount()
    val visitedCount: Flow<Int> = visitDao.getVisitedCount()
    val wishlistCount: Flow<Int> = visitDao.getWishlistCount()
    val mediaCount: Flow<Int> = mediaLinkDao.getMediaLinksCount()

    suspend fun ensureDefaultCategories() {
        val count = categoryDao.getCategoriesCount()
        if (count == 0) {
            categoryDao.insertCategories(TempleMapDatabase.DEFAULT_CATEGORIES)
        }
    }

    suspend fun addPlace(place: PlaceEntity) {
        placeDao.insertPlace(place)
    }

    suspend fun addCategory(name: String, iconName: String, colorHex: String): CategoryEntity {
        val category = CategoryEntity(
            id = "cat_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            iconName = iconName,
            isDefault = false,
            colorHex = colorHex
        )
        categoryDao.insertCategory(category)
        return category
    }

    suspend fun addTrip(name: String, startDate: Long, endDate: Long?, notes: String?): TripEntity {
        val trip = TripEntity(
            id = "trip_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            startDate = startDate,
            endDate = endDate,
            notes = notes
        )
        tripDao.insertTrip(trip)
        return trip
    }

    suspend fun addVisit(visit: VisitEntity) {
        visitDao.insertVisit(visit)
    }

    suspend fun updateVisit(visit: VisitEntity) {
        visitDao.updateVisit(visit)
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteCategoryById(categoryId)
    }

    suspend fun clearAllUserData() {
        mediaLinkDao.deleteAllMediaLinks()
        visitDao.deleteAllVisits()
        tripDao.deleteAllTrips()
        placeDao.deleteAllPlaces()
    }

    suspend fun deleteVisit(visitId: String) {
        mediaLinkDao.deleteMediaLinksForVisit(visitId)
        visitDao.deleteVisitById(visitId)
    }

    suspend fun addMediaLink(link: MediaLinkEntity) {
        mediaLinkDao.insertMediaLink(link)
    }

    suspend fun insertMediaLinks(links: List<MediaLinkEntity>) {
        if (links.isNotEmpty()) {
            mediaLinkDao.insertMediaLinks(links)
        }
    }

    suspend fun deleteMediaLink(linkId: String) {
        mediaLinkDao.deleteMediaLinkById(linkId)
    }

    fun getMediaLinksForVisit(visitId: String): Flow<List<MediaLinkEntity>> {
        return mediaLinkDao.getMediaLinksForVisit(visitId)
    }

    suspend fun getVisitWithDetails(visitId: String): VisitWithDetails? {
        val visit = visitDao.getVisitById(visitId) ?: return null
        val place = placeDao.getPlaceById(visit.placeId)
        val categories = if (visit.categoryIds.isNotEmpty()) {
            categoryDao.getCategoriesByIds(visit.categoryIds)
        } else {
            emptyList()
        }
        val trip = visit.tripId?.let { tripDao.getTripById(it) }
        return VisitWithDetails(
            visit = visit,
            place = place,
            categories = categories,
            trip = trip
        )
    }

    suspend fun seedSampleTempleVisits() {
        ensureDefaultCategories()

        // 1. Brihadeeswarar Temple, Thanjavur (Visited)
        val place1 = PlaceEntity(
            id = "place_thanjavur",
            name = "Brihadeeswarar Temple",
            latitude = 10.7828,
            longitude = 79.1318,
            source = "manual"
        )
        placeDao.insertPlace(place1)

        val visit1 = VisitEntity(
            id = "visit_thanjavur",
            placeId = place1.id,
            startDate = System.currentTimeMillis() - 86400000L * 18,
            endDate = System.currentTimeMillis() - 86400000L * 17,
            notes = "Magnificent 11th-century Chola granite architecture. The Kumbam shadow never falls on the ground at noon. Sublime evening aarti.",
            categoryIds = listOf("cat_temples", "cat_pilgrimage", "cat_family"),
            tripId = null,
            state = VisitState.VISITED,
            deity = "Lord Shiva (Peruvudaiyar)",
            architecture = "Dravidian Chola Granite",
            prayers = "Peace, health, and family prosperity"
        )
        visitDao.insertVisit(visit1)

        val media1 = MediaLinkEntity(
            id = "media_thanjavur_1",
            visitId = visit1.id,
            type = MediaLinkType.GALLERY,
            externalId = "content://media/external/images/media/1001",
            dateTaken = visit1.startDate,
            hasGps = true,
            latitude = 10.7828,
            longitude = 79.1318
        )
        mediaLinkDao.insertMediaLink(media1)

        // 2. Meenakshi Amman Temple, Madurai (Visited)
        val place2 = PlaceEntity(
            id = "place_madurai",
            name = "Meenakshi Amman Temple",
            latitude = 9.9195,
            longitude = 78.1193,
            source = "manual"
        )
        placeDao.insertPlace(place2)

        val visit2 = VisitEntity(
            id = "visit_madurai",
            placeId = place2.id,
            startDate = System.currentTimeMillis() - 86400000L * 15,
            notes = "Incredible thousand-pillar hall and multi-tiered towering gopurams filled with vibrant sculptured deities. Famous jigarthanda nearby.",
            categoryIds = listOf("cat_temples", "cat_food"),
            tripId = null,
            state = VisitState.VISITED,
            deity = "Goddess Meenakshi & Lord Sundareswarar",
            architecture = "Dravidian Nayaka",
            prayers = "Wisdom, inner clarity, and success"
        )
        visitDao.insertVisit(visit2)

        // 3. Kedarnath Temple, Himalayas (Wishlist)
        val place3 = PlaceEntity(
            id = "place_kedarnath",
            name = "Kedarnath Temple",
            latitude = 30.7352,
            longitude = 79.0669,
            source = "manual"
        )
        placeDao.insertPlace(place3)

        val visit3 = VisitEntity(
            id = "visit_kedarnath",
            placeId = place3.id,
            startDate = System.currentTimeMillis() + 86400000L * 60,
            notes = "Ancient Jyotirlinga high in the Garhwal Himalayas. Planning the trek during the auspicious pilgrimage window next spring.",
            categoryIds = listOf("cat_temples", "cat_pilgrimage", "cat_nature"),
            tripId = null,
            state = VisitState.WISHLIST,
            deity = "Lord Shiva (Kedarnath Jyotirlinga)",
            architecture = "Katyuri Himalayan Stone",
            prayers = "Spiritual fortitude and safe high-altitude pilgrimage"
        )
        visitDao.insertVisit(visit3)
    }
}
