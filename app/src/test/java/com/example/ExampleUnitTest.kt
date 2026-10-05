package com.example

import com.example.data.db.Converters
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.MediaLinkType
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import com.example.data.model.VisitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private val converters = Converters()

  @Test
  fun `test place entity creation and coordinates`() {
    val place = PlaceEntity(
      id = "place_test_1",
      name = "Brihadeeswarar Temple",
      latitude = 10.7828,
      longitude = 79.1318,
      source = "manual"
    )
    assertEquals("place_test_1", place.id)
    assertEquals("Brihadeeswarar Temple", place.name)
    assertEquals(10.7828, place.latitude, 0.0001)
    assertEquals(79.1318, place.longitude, 0.0001)
  }

  @Test
  fun `test visit entity and states`() {
    val visit = VisitEntity(
      id = "visit_test_1",
      placeId = "place_test_1",
      startDate = 1710000000000L,
      state = VisitState.VISITED,
      categoryIds = listOf("cat_temples", "cat_pilgrimage"),
      deity = "Lord Shiva",
      notes = "Sacred Darshan"
    )
    assertEquals(VisitState.VISITED, visit.state)
    assertEquals(2, visit.categoryIds.size)
    assertEquals("Lord Shiva", visit.deity)

    val wishlist = visit.copy(state = VisitState.WISHLIST)
    assertEquals(VisitState.WISHLIST, wishlist.state)
  }

  @Test
  fun `test media link entity properties`() {
    val link = MediaLinkEntity(
      id = "media_1",
      visitId = "visit_test_1",
      type = MediaLinkType.GALLERY,
      externalId = "content://media/external/images/media/42",
      hasGps = true,
      latitude = 10.7828,
      longitude = 79.1318
    )
    assertEquals(MediaLinkType.GALLERY, link.type)
    assertTrue(link.hasGps)
    assertEquals(10.7828, link.latitude!!, 0.0001)
  }

  @Test
  fun `test room converters for category list and enums`() {
    val list = listOf("cat_temples", "cat_pilgrimage", "cat_family")
    val serialized = converters.fromStringList(list)
    assertEquals("cat_temples,cat_pilgrimage,cat_family", serialized)

    val deserialized = converters.toStringList(serialized)
    assertEquals(list, deserialized)

    // Empty or null string
    assertEquals(emptyList<String>(), converters.toStringList(null))
    assertEquals(emptyList<String>(), converters.toStringList(""))

    // Enums
    assertEquals("VISITED", converters.fromVisitState(VisitState.VISITED))
    assertEquals(VisitState.WISHLIST, converters.toVisitState("WISHLIST"))
    assertEquals(MediaLinkType.GOOGLE_PHOTOS, converters.toMediaLinkType("GOOGLE_PHOTOS"))
  }
}
