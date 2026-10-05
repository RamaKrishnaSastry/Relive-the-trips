package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.MediaLinkType
import com.example.data.model.VisitState

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(separator = ",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromVisitState(state: VisitState?): String {
        return state?.name ?: VisitState.VISITED.name
    }

    @TypeConverter
    fun toVisitState(value: String?): VisitState {
        return try {
            if (value != null) VisitState.valueOf(value) else VisitState.VISITED
        } catch (e: Exception) {
            VisitState.VISITED
        }
    }

    @TypeConverter
    fun fromMediaLinkType(type: MediaLinkType?): String {
        return type?.name ?: MediaLinkType.GALLERY.name
    }

    @TypeConverter
    fun toMediaLinkType(value: String?): MediaLinkType {
        return try {
            if (value != null) MediaLinkType.valueOf(value) else MediaLinkType.GALLERY
        } catch (e: Exception) {
            MediaLinkType.GALLERY
        }
    }
}
