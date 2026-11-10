package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "places",
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["name"])
    ]
)
data class PlaceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val source: String = "manual", // "manual", "places_lookup", "photo_gps"
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = ""
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "temple",
    val isDefault: Boolean = false,
    val colorHex: String = "#D97706"
)

@Entity(
    tableName = "trips",
    indices = [Index(value = ["ownerId"])]
)
data class TripEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: Long,
    val endDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = ""
)

@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["placeId"]),
        Index(value = ["tripId"]),
        Index(value = ["state"]),
        Index(value = ["startDate"])
    ]
)
data class VisitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val startDate: Long,
    val endDate: Long? = null,
    val notes: String? = null,
    val categoryIds: List<String> = emptyList(),
    val tripId: String? = null,
    val state: VisitState = VisitState.VISITED,
    val deity: String? = null,
    val architecture: String? = null,
    val prayers: String? = null,
    val nearbyFood: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = ""
)

@Entity(
    tableName = "media_links",
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["visitId"]),
        Index(value = ["type"])
    ]
)
data class MediaLinkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val visitId: String,
    val type: MediaLinkType,
    val externalId: String,
    val dateTaken: Long? = null,
    val hasGps: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = ""
)
