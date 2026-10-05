package com.example.map

data class MapPin(
    val id: String,
    val placeId: String,
    val visitId: String?,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isWishlist: Boolean,
    val categoryIds: List<String> = emptyList(),
    val deity: String? = null,
    val architecture: String? = null,
    val notes: String? = null,
    val startDate: Long = System.currentTimeMillis()
)

sealed interface MapClusterItem {
    val latitude: Double
    val longitude: Double

    data class SinglePin(
        val pin: MapPin
    ) : MapClusterItem {
        override val latitude: Double get() = pin.latitude
        override val longitude: Double get() = pin.longitude
    }

    data class Cluster(
        val id: String,
        override val latitude: Double,
        override val longitude: Double,
        val count: Int,
        val pins: List<MapPin>,
        val hasWishlist: Boolean
    ) : MapClusterItem
}
