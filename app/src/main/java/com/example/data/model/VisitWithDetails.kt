package com.example.data.model

data class VisitWithDetails(
    val visit: VisitEntity,
    val place: PlaceEntity?,
    val categories: List<CategoryEntity> = emptyList(),
    val mediaLinks: List<MediaLinkEntity> = emptyList(),
    val trip: TripEntity? = null
)
