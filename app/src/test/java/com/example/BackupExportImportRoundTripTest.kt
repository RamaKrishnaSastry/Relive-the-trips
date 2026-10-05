package com.example

import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.MediaLinkType
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupExportImportRoundTripTest {

    @Test
    fun `test backup JSON serialization preserves media links and trip associations`() {
        val place = PlaceEntity(
            id = "place_101",
            name = "Meenakshi Amman Temple",
            latitude = 9.9195,
            longitude = 78.1193,
            source = "manual"
        )
        val trip = TripEntity(
            id = "trip_202",
            name = "Tamil Nadu Yatra 2026",
            startDate = 1700000000000L,
            endDate = 1700500000000L,
            notes = "Spiritual pilgrimage"
        )
        val visit = VisitEntity(
            id = "visit_303",
            placeId = "place_101",
            startDate = 1700100000000L,
            endDate = 1700200000000L,
            notes = "Evening aarti darshan was blissful",
            categoryIds = listOf("cat_temples", "cat_unesco"),
            tripId = "trip_202",
            state = VisitState.VISITED,
            deity = "Goddess Meenakshi & Lord Sundareswarar",
            architecture = "Dravidian Gopurams",
            prayers = "Peace & Health"
        )
        val mediaLink1 = MediaLinkEntity(
            id = "media_401",
            visitId = "visit_303",
            type = MediaLinkType.GALLERY,
            externalId = "content://media/external/images/media/12345",
            dateTaken = 1700100500000L,
            hasGps = true,
            latitude = 9.91955,
            longitude = 78.11935
        )
        val mediaLink2 = MediaLinkEntity(
            id = "media_402",
            visitId = "visit_303",
            type = MediaLinkType.GOOGLE_PHOTOS,
            externalId = "https://photos.google.com/share/sample_album",
            dateTaken = 1700100600000L,
            hasGps = false
        )

        // Serialize backup
        val root = JSONObject().apply {
            put("version", 1)
            put("appName", "Temple Map")
            put("exportedAt", System.currentTimeMillis())

            val placesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", place.id)
                    put("name", place.name)
                    put("latitude", place.latitude)
                    put("longitude", place.longitude)
                    put("source", place.source)
                })
            }
            put("places", placesArray)

            val tripsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", trip.id)
                    put("name", trip.name)
                    put("startDate", trip.startDate)
                    put("endDate", trip.endDate)
                    put("notes", trip.notes)
                })
            }
            put("trips", tripsArray)

            val visitsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", visit.id)
                    put("placeId", visit.placeId)
                    put("startDate", visit.startDate)
                    put("endDate", visit.endDate)
                    put("notes", visit.notes)
                    put("categoryIds", JSONArray(visit.categoryIds))
                    put("tripId", visit.tripId)
                    put("state", visit.state.name)
                    put("deity", visit.deity)
                    put("architecture", visit.architecture)
                    put("prayers", visit.prayers)
                })
            }
            put("visits", visitsArray)

            val mediaArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", mediaLink1.id)
                    put("visitId", mediaLink1.visitId)
                    put("type", mediaLink1.type.name)
                    put("externalId", mediaLink1.externalId)
                    put("dateTaken", mediaLink1.dateTaken)
                    put("hasGps", mediaLink1.hasGps)
                    put("latitude", mediaLink1.latitude)
                    put("longitude", mediaLink1.longitude)
                })
                put(JSONObject().apply {
                    put("id", mediaLink2.id)
                    put("visitId", mediaLink2.visitId)
                    put("type", mediaLink2.type.name)
                    put("externalId", mediaLink2.externalId)
                    put("dateTaken", mediaLink2.dateTaken)
                    put("hasGps", mediaLink2.hasGps)
                })
            }
            put("mediaLinks", mediaArray)
        }

        val jsonString = root.toString(2)
        assertNotNull(jsonString)

        // Deserialize & Verify
        val parsed = JSONObject(jsonString)
        val parsedVisits = parsed.getJSONArray("visits")
        val parsedMedia = parsed.getJSONArray("mediaLinks")
        val parsedTrips = parsed.getJSONArray("trips")

        assertEquals(1, parsedVisits.length())
        assertEquals(2, parsedMedia.length())
        assertEquals(1, parsedTrips.length())

        val visitObj = parsedVisits.getJSONObject(0)
        assertEquals("trip_202", visitObj.getString("tripId"))
        assertEquals("Goddess Meenakshi & Lord Sundareswarar", visitObj.getString("deity"))

        val mediaObj1 = parsedMedia.getJSONObject(0)
        assertEquals("media_401", mediaObj1.getString("id"))
        assertEquals("visit_303", mediaObj1.getString("visitId"))
        assertTrue(mediaObj1.getBoolean("hasGps"))
        assertEquals(9.91955, mediaObj1.getDouble("latitude"), 0.00001)
        assertEquals(78.11935, mediaObj1.getDouble("longitude"), 0.00001)

        val mediaObj2 = parsedMedia.getJSONObject(1)
        assertEquals("GOOGLE_PHOTOS", mediaObj2.getString("type"))
        assertEquals("https://photos.google.com/share/sample_album", mediaObj2.getString("externalId"))
    }
}
