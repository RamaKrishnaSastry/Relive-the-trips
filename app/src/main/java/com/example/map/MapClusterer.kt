package com.example.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MapClusterer {

    /**
     * Groups nearby map pins into clusters based on zoom level.
     * At lower zoom (zoomed out globe), nearby sites merge and display count (e.g. "14").
     * At higher zoom (zoomed in), clusters split into individual pins.
     */
    fun clusterPins(
        pins: List<MapPin>,
        zoomLevel: Float
    ): List<MapClusterItem> {
        if (pins.isEmpty()) return emptyList()

        // When zoomed in very close (e.g. street / temple precinct level), do not cluster
        if (zoomLevel >= 13.0f) {
            return pins.map { MapClusterItem.SinglePin(it) }
        }

        // Distance threshold in degrees derived from zoom level
        val thresholdDegrees = calculateThresholdDegrees(zoomLevel)

        val remaining = pins.toMutableList()
        val result = mutableListOf<MapClusterItem>()

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val clusterGroup = mutableListOf(seed)

            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val dist = distanceDegrees(
                    seed.latitude, seed.longitude,
                    candidate.latitude, candidate.longitude
                )
                if (dist <= thresholdDegrees) {
                    clusterGroup.add(candidate)
                    iterator.remove()
                }
            }

            if (clusterGroup.size == 1) {
                result.add(MapClusterItem.SinglePin(seed))
            } else {
                val avgLat = clusterGroup.map { it.latitude }.average()
                val avgLng = clusterGroup.map { it.longitude }.average()
                val hasWishlist = clusterGroup.any { it.isWishlist }

                result.add(
                    MapClusterItem.Cluster(
                        id = "cluster_${seed.id}_${clusterGroup.size}",
                        latitude = avgLat,
                        longitude = avgLng,
                        count = clusterGroup.size,
                        pins = clusterGroup,
                        hasWishlist = hasWishlist
                    )
                )
            }
        }

        return result
    }

    fun calculateThresholdDegrees(zoomLevel: Float): Double {
        val effectiveZoom = zoomLevel.coerceIn(1.0f, 15.0f)
        val denominator = Math.pow(2.0, effectiveZoom.toDouble()) * 4.0
        return 360.0 / denominator
    }

    private fun distanceDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = lat1 - lat2
        val dLon = lon1 - lon2
        return sqrt(dLat * dLat + dLon * dLon)
    }

    /**
     * Approximate distance in kilometers between two geo-coordinates.
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
