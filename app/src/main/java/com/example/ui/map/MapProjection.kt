package com.example.ui.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Web-Mercator projection utilities for map gestures, tile calculations,
 * and precise coordinate-to-screen conversions.
 */
object MapProjection {
    const val MAX_LATITUDE = 85.05112878
    const val MIN_LATITUDE = -85.05112878

    /**
     * Calculates the total pixel dimensions of the world at a given fractional zoom level.
     */
    fun worldSize(zoomLevel: Float): Double = 256.0 * 2.0.pow(zoomLevel.toDouble())

    /**
     * Projects latitude/longitude to global Web Mercator pixel coordinates.
     */
    fun latLonToWorld(lat: Double, lon: Double, zoomLevel: Float): Pair<Double, Double> {
        val w = worldSize(zoomLevel)
        val clampedLat = lat.coerceIn(MIN_LATITUDE, MAX_LATITUDE)
        val xNorm = (lon + 180.0) / 360.0
        val latRad = Math.toRadians(clampedLat)
        val yNorm = (1.0 - ln(tan(PI / 4.0 + latRad / 2.0)) / PI) / 2.0
        return Pair(xNorm * w, yNorm * w)
    }

    /**
     * Inverts global Web Mercator pixel coordinates to latitude/longitude.
     * Uses: x -> lon linear; y -> lat via atan(sinh(PI * (1 - 2 * y / worldSize)))
     */
    fun worldToLatLon(worldX: Double, worldY: Double, zoomLevel: Float): Pair<Double, Double> {
        val w = worldSize(zoomLevel)
        var lon = (worldX / w) * 360.0 - 180.0
        while (lon > 180.0) lon -= 360.0
        while (lon < -180.0) lon += 360.0

        val yNorm = (worldY / w).coerceIn(0.0, 1.0)
        val u = PI * (1.0 - 2.0 * yNorm)
        val latRad = atan(sinh(u))
        val lat = Math.toDegrees(latRad).coerceIn(MIN_LATITUDE, MAX_LATITUDE)
        return Pair(lat, lon)
    }

    /**
     * Converts a geographic coordinate to viewport screen coordinates (in pixels).
     */
    fun latLonToScreen(
        lat: Double,
        lon: Double,
        centerLat: Double,
        centerLon: Double,
        zoomLevel: Float,
        screenWidth: Double,
        screenHeight: Double
    ): Pair<Double, Double> {
        val (worldX, worldY) = latLonToWorld(lat, lon, zoomLevel)
        val (centerWorldX, centerWorldY) = latLonToWorld(centerLat, centerLon, zoomLevel)
        val screenX = (screenWidth / 2.0) + (worldX - centerWorldX)
        val screenY = (screenHeight / 2.0) + (worldY - centerWorldY)
        return Pair(screenX, screenY)
    }

    /**
     * Converts a screen viewport pixel coordinate to latitude/longitude.
     */
    fun screenToLatLon(
        screenX: Double,
        screenY: Double,
        centerLat: Double,
        centerLon: Double,
        zoomLevel: Float,
        screenWidth: Double,
        screenHeight: Double
    ): Pair<Double, Double> {
        val (centerWorldX, centerWorldY) = latLonToWorld(centerLat, centerLon, zoomLevel)
        val worldX = centerWorldX + (screenX - (screenWidth / 2.0))
        val worldY = centerWorldY + (screenY - (screenHeight / 2.0))
        return worldToLatLon(worldX, worldY, zoomLevel)
    }

    /**
     * Computes the new map center when dragging the screen by (panDeltaX, panDeltaY) pixels.
     * Uses the exact same inverse Web-Mercator formula so dragging and tapping always agree.
     */
    fun panCenter(
        panDeltaX: Double,
        panDeltaY: Double,
        centerLat: Double,
        centerLon: Double,
        zoomLevel: Float
    ): Pair<Double, Double> {
        val (centerWorldX, centerWorldY) = latLonToWorld(centerLat, centerLon, zoomLevel)
        val newWorldX = centerWorldX - panDeltaX
        val newWorldY = centerWorldY - panDeltaY
        return worldToLatLon(newWorldX, newWorldY, zoomLevel)
    }
}
