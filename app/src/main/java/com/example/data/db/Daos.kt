package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<PlaceEntity>)

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    suspend fun getPlaceById(id: String): PlaceEntity?

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    fun getPlaceByIdFlow(id: String): Flow<PlaceEntity?>

    @Query("SELECT * FROM places WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getAllPlaces(ownerId: String): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE ownerId = :ownerId")
    suspend fun getAllPlacesForOwner(ownerId: String): List<PlaceEntity>

    @Query("SELECT COUNT(*) FROM places WHERE ownerId = :ownerId")
    fun getPlacesCount(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM visits WHERE placeId = :placeId")
    suspend fun countVisitsForPlace(placeId: String): Int

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deletePlaceById(id: String)

    @Query("DELETE FROM places WHERE ownerId = :ownerId")
    suspend fun deletePlacesByOwner(ownerId: String)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id IN (:ids)")
    suspend fun getCategoriesByIds(ids: List<String>): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCategoryById(id: String)
}

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE ownerId = :ownerId ORDER BY startDate DESC")
    fun getAllTrips(ownerId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: String): TripEntity?

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: String)

    @Query("DELETE FROM trips WHERE ownerId = :ownerId")
    suspend fun deleteTripsByOwner(ownerId: String)
}

@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE ownerId = :ownerId ORDER BY startDate DESC")
    fun getAllVisits(ownerId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE ownerId = :ownerId AND state = :state ORDER BY startDate DESC")
    fun getVisitsByState(ownerId: String, state: VisitState): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: String): VisitEntity?

    @Query("SELECT * FROM visits WHERE ownerId = :ownerId AND placeId = :placeId ORDER BY startDate DESC")
    fun getVisitsForPlace(ownerId: String, placeId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE ownerId = :ownerId AND tripId = :tripId ORDER BY startDate ASC")
    fun getVisitsForTrip(ownerId: String, tripId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE ownerId = :ownerId")
    suspend fun getAllVisitsList(ownerId: String): List<VisitEntity>

    @Query("SELECT * FROM visits")
    suspend fun getAllVisitsAcrossOwners(): List<VisitEntity>

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)

    @Query("DELETE FROM visits WHERE ownerId = :ownerId")
    suspend fun deleteVisitsByOwner(ownerId: String)

    @Query("SELECT COUNT(*) FROM visits WHERE ownerId = :ownerId AND state = 'VISITED'")
    fun getVisitedCount(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM visits WHERE ownerId = :ownerId AND state = 'WISHLIST'")
    fun getWishlistCount(ownerId: String): Flow<Int>
}

@Dao
interface MediaLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaLink(link: MediaLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaLinks(links: List<MediaLinkEntity>)

    @Query("SELECT * FROM media_links WHERE visitId = :visitId ORDER BY dateTaken DESC, createdAt DESC")
    fun getMediaLinksForVisit(visitId: String): Flow<List<MediaLinkEntity>>

    @Query("SELECT * FROM media_links WHERE ownerId = :ownerId ORDER BY dateTaken DESC")
    fun getAllMediaLinks(ownerId: String): Flow<List<MediaLinkEntity>>

    @Query("DELETE FROM media_links WHERE id = :id")
    suspend fun deleteMediaLinkById(id: String)

    @Query("DELETE FROM media_links WHERE visitId = :visitId")
    suspend fun deleteMediaLinksForVisit(visitId: String)

    @Query("DELETE FROM media_links WHERE ownerId = :ownerId")
    suspend fun deleteMediaLinksByOwner(ownerId: String)

    @Query("SELECT COUNT(*) FROM media_links WHERE ownerId = :ownerId")
    fun getMediaLinksCount(ownerId: String): Flow<Int>
}
