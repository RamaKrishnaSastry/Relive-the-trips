package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.TempleMapDatabase
import com.example.data.model.PlaceEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TempleDataTests {

    private lateinit var db: TempleMapDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TempleMapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test multi-tenancy isolation`() = runBlocking {
        val ownerA = "owner_a"
        val ownerB = "owner_b"
        val placeDao = db.placeDao()

        placeDao.insertPlace(PlaceEntity("p1", "Place A", 0.0, 0.0, "manual", 0L, ownerA))
        placeDao.insertPlace(PlaceEntity("p2", "Place B", 0.0, 0.0, "manual", 0L, ownerB))

        val placesA = placeDao.getAllPlaces(ownerA).first()
        val placesB = placeDao.getAllPlaces(ownerB).first()

        assertEquals(1, placesA.size)
        assertEquals("Place A", placesA[0].name)
        assertEquals(1, placesB.size)
        assertEquals("Place B", placesB[0].name)
    }

    @Test
    fun `test visit deletion cascades to media`() = runBlocking {
        val owner = "owner_x"
        val visitId = UUID.randomUUID().toString()
        db.visitDao().insertVisit(VisitEntity(id = visitId, placeId = "p1", startDate = 0L, endDate = null, notes = null, categoryIds = emptyList(), state = VisitState.VISITED, deity = null, architecture = null, prayers = null, nearbyFood = null, createdAt = 0L, ownerId = owner))
        db.mediaLinkDao().insertMediaLink(com.example.data.model.MediaLinkEntity(id = "m1", visitId = visitId, type = com.example.data.model.MediaLinkType.GALLERY, externalId = "ext1", dateTaken = 0L, hasGps = false, latitude = null, longitude = null, createdAt = 0L, ownerId = owner))

        assertEquals(1, db.mediaLinkDao().getMediaLinksForVisit(visitId).first().size)

        db.visitDao().deleteVisitById(visitId)

        assertEquals(0, db.mediaLinkDao().getMediaLinksForVisit(visitId).first().size)
    }
}
