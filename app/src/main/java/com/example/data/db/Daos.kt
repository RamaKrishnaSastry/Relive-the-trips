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

    @Query("SELECT * FROM places ORDER BY name ASC")
    fun getAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT COUNT(*) FROM places")
    fun getPlacesCount(): Flow<Int>

    @Query("DELETE FROM places WHERE id = :id")
    suspend fun deletePlaceById(id: String)

    @Query("DELETE FROM places")
    suspend fun deleteAllPlaces()
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

    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: String): TripEntity?

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: String)

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()
}

@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("SELECT * FROM visits ORDER BY startDate DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE state = :state ORDER BY startDate DESC")
    fun getVisitsByState(state: VisitState): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: String): VisitEntity?

    @Query("SELECT * FROM visits WHERE placeId = :placeId ORDER BY startDate DESC")
    fun getVisitsForPlace(placeId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE tripId = :tripId ORDER BY startDate ASC")
    fun getVisitsForTrip(tripId: String): Flow<List<VisitEntity>>

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)

    @Query("DELETE FROM visits")
    suspend fun deleteAllVisits()

    @Query("SELECT COUNT(*) FROM visits WHERE state = 'VISITED'")
    fun getVisitedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM visits WHERE state = 'WISHLIST'")
    fun getWishlistCount(): Flow<Int>
}

@Dao
interface MediaLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaLink(link: MediaLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaLinks(links: List<MediaLinkEntity>)

    @Query("SELECT * FROM media_links WHERE visitId = :visitId ORDER BY dateTaken DESC, createdAt DESC")
    fun getMediaLinksForVisit(visitId: String): Flow<List<MediaLinkEntity>>

    @Query("SELECT * FROM media_links ORDER BY dateTaken DESC")
    fun getAllMediaLinks(): Flow<List<MediaLinkEntity>>

    @Query("DELETE FROM media_links WHERE id = :id")
    suspend fun deleteMediaLinkById(id: String)

    @Query("DELETE FROM media_links WHERE visitId = :visitId")
    suspend fun deleteMediaLinksForVisit(visitId: String)

    @Query("DELETE FROM media_links")
    suspend fun deleteAllMediaLinks()

    @Query("SELECT COUNT(*) FROM media_links")
    fun getMediaLinksCount(): Flow<Int>
}
