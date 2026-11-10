package com.example

import com.example.ui.map.MapProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MapProjectionTest {

    private val delta = 0.0001

    @Test
    fun testWorldToLatLonRoundTrip() {
        val testCoords = listOf(
            Pair(51.5074, -0.1278),   // London
            Pair(13.0827, 80.2707),   // Chennai
            Pair(-33.8688, 151.2093), // Sydney
            Pair(0.0, 0.0),           // Equator / Prime Meridian
            Pair(20.5937, 78.9629)    // India center
        )

        val zoomLevels = listOf(3.0f, 5.0f, 10.5f, 14.0f)

        for (zoom in zoomLevels) {
            for ((lat, lon) in testCoords) {
                val (worldX, worldY) = MapProjection.latLonToWorld(lat, lon, zoom)
                val (recoveredLat, recoveredLon) = MapProjection.worldToLatLon(worldX, worldY, zoom)

                assertEquals("Lat roundtrip mismatch for ($lat, $lon) at zoom $zoom", lat, recoveredLat, delta)
                assertEquals("Lon roundtrip mismatch for ($lat, $lon) at zoom $zoom", lon, recoveredLon, delta)
            }
        }
    }

    @Test
    fun testScreenToLatLonRoundTrip() {
        val screenW = 1080.0
        val screenH = 2400.0
        val centerLat = 13.0827
        val centerLon = 80.2707
        val zoom = 8.5f

        // Center should map to (screenW / 2, screenH / 2)
        val (centerX, centerY) = MapProjection.latLonToScreen(
            centerLat, centerLon, centerLat, centerLon, zoom, screenW, screenH
        )
        assertEquals(screenW / 2.0, centerX, 0.001)
        assertEquals(screenH / 2.0, centerY, 0.001)

        // Known points on screen
        val points = listOf(
            Pair(51.5074, -0.1278),   // London
            Pair(13.0827, 80.2707),   // Chennai
            Pair(-33.8688, 151.2093)  // Sydney
        )

        for ((lat, lon) in points) {
            val (screenX, screenY) = MapProjection.latLonToScreen(
                lat, lon, centerLat, centerLon, zoom, screenW, screenH
            )
            val (recoveredLat, recoveredLon) = MapProjection.screenToLatLon(
                screenX, screenY, centerLat, centerLon, zoom, screenW, screenH
            )

            assertEquals("Screen lat mismatch for $lat", lat, recoveredLat, delta)
            assertEquals("Screen lon mismatch for $lon", lon, recoveredLon, delta)
        }
    }

    @Test
    fun testPanCenterPreservesInverseAgreement() {
        val centerLat = 20.5937
        val centerLon = 78.9629
        val zoom = 6.0f

        val panX = 150.0
        val panY = -80.0

        val (newCenterLat, newCenterLon) = MapProjection.panCenter(
            panX, panY, centerLat, centerLon, zoom
        )

        // Dragging by panX, panY moves what was at (centerX - panX, centerY - panY) to center
        val screenW = 800.0
        val screenH = 1200.0
        val origTapped = MapProjection.screenToLatLon(
            (screenW / 2.0) - panX, (screenH / 2.0) - panY, centerLat, centerLon, zoom, screenW, screenH
        )

        assertEquals("Pan lat must agree with screen tap", origTapped.first, newCenterLat, delta)
        assertEquals("Pan lon must agree with screen tap", origTapped.second, newCenterLon, delta)
    }

    @Test
    fun testPinchZoomLog2Formula() {
        val baseZoom = 5.0f
        val zoomFactor = 2.0f
        // Pinch formula: zoomLevel += log2(zoom)
        val newZoom = baseZoom + kotlin.math.log2(zoomFactor.toDouble()).toFloat()
        assertEquals(6.0f, newZoom, 0.0001f)

        val halfZoomFactor = 0.5f
        val zoomedOut = baseZoom + kotlin.math.log2(halfZoomFactor.toDouble()).toFloat()
        assertEquals(4.0f, zoomedOut, 0.0001f)
    }
}
