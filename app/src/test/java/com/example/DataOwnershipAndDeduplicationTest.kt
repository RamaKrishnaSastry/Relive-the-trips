package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.TempleMapDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import com.example.data.repository.TempleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataOwnershipAndDeduplicationTest {

    private lateinit var database: TempleMapDatabase
    private lateinit var repository: TempleRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TempleMapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TempleRepository(
            placeDao = database.placeDao(),
            categoryDao = database.categoryDao(),
            tripDao = database.tripDao(),
            visitDao = database.visitDao(),
            mediaLinkDao = database.mediaLinkDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test place deduplication by name and coordinates rounded to 4 decimals`() = runBlocking {
        val ownerId = "user_123"

        // Insert first place
        val place1 = repository.getOrCreatePlace(
            ownerId = ownerId,
            name = "Meenakshi Amman Temple",
            latitude = 9.9195123,
            longitude = 78.1193123,
            source = "manual"
        )
        assertNotNull(place1)

        // Request again with slightly different coords (same rounded to 4 decimals)
        val place2 = repository.getOrCreatePlace(
            ownerId = ownerId,
            name = "Meenakshi Amman Temple",
            latitude = 9.9195444, // rounds to 9.9195
            longitude = 78.1193444, // rounds to 78.1193
            source = "photo_gps"
        )

        assertEquals("Place IDs must match due to deduplication", place1.id, place2.id)

        val allPlaces = repository.getAllPlaces(ownerId).first()
        assertEquals("Only one place should exist for this owner", 1, allPlaces.size)
    }

    @Test
    fun `test orphaned place cleanup on last visit deletion`() = runBlocking {
        val ownerId = "user_456"

        val place = repository.getOrCreatePlace(
            ownerId = ownerId,
            name = "Brihadeeswarar Temple",
            latitude = 10.7828,
            longitude = 79.1318
        )

        val visit = VisitEntity(
            id = "visit_single",
            placeId = place.id,
            startDate = 1000L,
            ownerId = ownerId,
            categoryIds = listOf("cat_heritage")
        )
        repository.addVisit(visit)

        val placesBefore = repository.getAllPlaces(ownerId).first()
        assertEquals(1, placesBefore.size)

        // Delete the only visit referencing the place
        repository.deleteVisit("visit_single")

        val placesAfter = repository.getAllPlaces(ownerId).first()
        assertEquals("Orphaned place should be deleted when its last visit is deleted", 0, placesAfter.size)
    }

    @Test
    fun `test category deletion scrubs category ID from visits`() = runBlocking {
        val ownerId = "user_789"
        val cat = repository.addCategory("Pilgrimage", "pilgrimage", "#FF5500")

        val place = repository.getOrCreatePlace(ownerId, "Kedarnath", 30.7352, 79.0669)
        val visit = VisitEntity(
            id = "visit_kedarnath",
            placeId = place.id,
            startDate = 2000L,
            ownerId = ownerId,
            categoryIds = listOf(cat.id, "cat_other")
        )
        repository.addVisit(visit)

        val visitBefore = database.visitDao().getVisitById("visit_kedarnath")
        assertEquals(2, visitBefore?.categoryIds?.size)

        // Delete the category
        repository.deleteCategory(cat.id)

        val visitAfter = database.visitDao().getVisitById("visit_kedarnath")
        assertEquals(1, visitAfter?.categoryIds?.size)
        assertEquals(listOf("cat_other"), visitAfter?.categoryIds)
    }

    @Test
    fun `test data scoped strictly by ownerId`() = runBlocking {
        val userA = "user_A"
        val userB = "user_B"

        val placeA = repository.getOrCreatePlace(userA, "Temple A", 12.0, 77.0)
        val placeB = repository.getOrCreatePlace(userB, "Temple B", 13.0, 78.0)

        val visitA = VisitEntity(id = "visit_A", placeId = placeA.id, startDate = 1L, ownerId = userA)
        val visitB = VisitEntity(id = "visit_B", placeId = placeB.id, startDate = 2L, ownerId = userB)

        repository.addVisit(visitA)
        repository.addVisit(visitB)

        val userAPlaces = repository.getAllPlaces(userA).first()
        val userBPlaces = repository.getAllPlaces(userB).first()
        assertEquals(1, userAPlaces.size)
        assertEquals("Temple A", userAPlaces[0].name)
        assertEquals(1, userBPlaces.size)
        assertEquals("Temple B", userBPlaces[0].name)

        val userAVisits = repository.getAllVisits(userA).first()
        val userBVisits = repository.getAllVisits(userB).first()
        assertEquals(1, userAVisits.size)
        assertEquals("visit_A", userAVisits[0].id)
        assertEquals(1, userBVisits.size)
        assertEquals("visit_B", userBVisits[0].id)
    }
}
