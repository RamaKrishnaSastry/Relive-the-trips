package com.example

import com.example.map.MapClusterItem
import com.example.map.MapClusterer
import com.example.map.MapPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapClustererTest {

    private val thanjavurPin = MapPin(
        id = "pin_1",
        placeId = "p_1",
        visitId = "v_1",
        name = "Brihadeeswarar Temple",
        latitude = 10.7828,
        longitude = 79.1318,
        isWishlist = false
    )

    // A nearby temple (~35km away in Kumbakonam)
    private val kumbakonamPin = MapPin(
        id = "pin_2",
        placeId = "p_2",
        visitId = "v_2",
        name = "Airavatesvara Temple",
        latitude = 10.9200,
        longitude = 79.3556,
        isWishlist = false
    )

    // A far-away temple in northern India (~2500km away in Himalayas)
    private val kedarnathPin = MapPin(
        id = "pin_3",
        placeId = "p_3",
        visitId = "v_3",
        name = "Kedarnath Temple",
        latitude = 30.7352,
        longitude = 79.0669,
        isWishlist = true
    )

    @Test
    fun `zoomed out view clusters nearby temples into a single cluster item with count`() {
        val pins = listOf(thanjavurPin, kumbakonamPin, kedarnathPin)

        // At zoom level 4 (country/subcontinent level), Thanjavur and Kumbakonam should cluster
        val clusteredItems = MapClusterer.clusterPins(pins, zoomLevel = 4.0f)

        // We expect 2 items: 1 cluster of 2 (Thanjavur + Kumbakonam), and 1 single pin (Kedarnath)
        assertEquals(2, clusteredItems.size)

        val cluster = clusteredItems.filterIsInstance<MapClusterItem.Cluster>().firstOrNull()
        assertTrue(cluster != null)
        assertEquals(2, cluster?.count)

        val single = clusteredItems.filterIsInstance<MapClusterItem.SinglePin>().firstOrNull()
        assertTrue(single != null)
        assertEquals("Kedarnath Temple", single?.pin?.name)
    }

    @Test
    fun `zoomed in view splits clusters into individual pins`() {
        val pins = listOf(thanjavurPin, kumbakonamPin, kedarnathPin)

        // At zoom level 14 (detailed city/precinct level), every temple must split into a SinglePin
        val items = MapClusterer.clusterPins(pins, zoomLevel = 14.0f)

        assertEquals(3, items.size)
        assertTrue(items.all { it is MapClusterItem.SinglePin })
    }

    @Test
    fun `empty pins list returns empty cluster result`() {
        val result = MapClusterer.clusterPins(emptyList(), zoomLevel = 5.0f)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `distance calculation between coordinates is accurate`() {
        val distKm = MapClusterer.distanceKm(
            thanjavurPin.latitude, thanjavurPin.longitude,
            kumbakonamPin.latitude, kumbakonamPin.longitude
        )
        // Actual geodesic distance between Thanjavur and Kumbakonam is approx 35-40 km
        assertTrue(distKm in 25.0..45.0)
    }
}
