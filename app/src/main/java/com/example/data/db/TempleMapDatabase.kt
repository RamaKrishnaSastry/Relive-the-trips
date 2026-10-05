package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.MediaLinkEntity
import com.example.data.model.PlaceEntity
import com.example.data.model.TripEntity
import com.example.data.model.VisitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlaceEntity::class,
        CategoryEntity::class,
        TripEntity::class,
        VisitEntity::class,
        MediaLinkEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TempleMapDatabase : RoomDatabase() {

    abstract fun placeDao(): PlaceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tripDao(): TripDao
    abstract fun visitDao(): VisitDao
    abstract fun mediaLinkDao(): MediaLinkDao

    companion object {
        @Volatile
        private var INSTANCE: TempleMapDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TempleMapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TempleMapDatabase::class.java,
                    "temple_map_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(
                id = "cat_temples",
                name = "Temples",
                iconName = "temple",
                isDefault = true,
                colorHex = "#D97706"
            ),
            CategoryEntity(
                id = "cat_pilgrimage",
                name = "Pilgrimage",
                iconName = "pilgrimage",
                isDefault = true,
                colorHex = "#EA580C"
            ),
            CategoryEntity(
                id = "cat_family",
                name = "Family Trips",
                iconName = "family",
                isDefault = true,
                colorHex = "#2563EB"
            ),
            CategoryEntity(
                id = "cat_friends",
                name = "Friends",
                iconName = "friends",
                isDefault = true,
                colorHex = "#7C3AED"
            ),
            CategoryEntity(
                id = "cat_solo",
                name = "Solo",
                iconName = "solo",
                isDefault = true,
                colorHex = "#0D9488"
            ),
            CategoryEntity(
                id = "cat_food",
                name = "Food Trips",
                iconName = "food",
                isDefault = true,
                colorHex = "#E11D48"
            ),
            CategoryEntity(
                id = "cat_nature",
                name = "Nature",
                iconName = "nature",
                isDefault = true,
                colorHex = "#059669"
            ),
            CategoryEntity(
                id = "cat_work",
                name = "Work",
                iconName = "work",
                isDefault = true,
                colorHex = "#4B5563"
            ),
            CategoryEntity(
                id = "cat_custom",
                name = "Custom",
                iconName = "custom",
                isDefault = true,
                colorHex = "#F59E0B"
            )
        )

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        database.categoryDao().insertCategories(DEFAULT_CATEGORIES)
                    }
                }
            }
        }
    }
}
