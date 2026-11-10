package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
    version = 3,
    exportSchema = true
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN nearbyFood TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Places: add ownerId column and index
                db.execSQL("ALTER TABLE places ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_places_ownerId ON places(ownerId)")

                // 2. Trips: add ownerId column and index
                db.execSQL("ALTER TABLE trips ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_ownerId ON trips(ownerId)")

                // 3. Visits: recreate table to add ownerId and foreign key to trips(id) ON DELETE SET_NULL
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS visits_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        placeId TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        notes TEXT,
                        categoryIds TEXT NOT NULL,
                        tripId TEXT,
                        state TEXT NOT NULL,
                        deity TEXT,
                        architecture TEXT,
                        prayers TEXT,
                        nearbyFood TEXT,
                        createdAt INTEGER NOT NULL,
                        ownerId TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO visits_new (id, placeId, startDate, endDate, notes, categoryIds, tripId, state, deity, architecture, prayers, nearbyFood, createdAt, ownerId)
                    SELECT id, placeId, startDate, endDate, notes, categoryIds, tripId, state, deity, architecture, prayers, nearbyFood, createdAt, '' FROM visits
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE visits")
                db.execSQL("ALTER TABLE visits_new RENAME TO visits")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visits_ownerId ON visits(ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visits_placeId ON visits(placeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visits_tripId ON visits(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visits_state ON visits(state)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_visits_startDate ON visits(startDate)")

                // 4. MediaLinks: recreate table to add ownerId and foreign key to visits(id) ON DELETE CASCADE
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS media_links_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        visitId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        externalId TEXT NOT NULL,
                        dateTaken INTEGER,
                        hasGps INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        createdAt INTEGER NOT NULL,
                        ownerId TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(visitId) REFERENCES visits(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO media_links_new (id, visitId, type, externalId, dateTaken, hasGps, latitude, longitude, createdAt, ownerId)
                    SELECT id, visitId, type, externalId, dateTaken, hasGps, latitude, longitude, createdAt, '' FROM media_links
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE media_links")
                db.execSQL("ALTER TABLE media_links_new RENAME TO media_links")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_links_ownerId ON media_links(ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_links_visitId ON media_links(visitId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_links_type ON media_links(type)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): TempleMapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TempleMapDatabase::class.java,
                    "temple_map_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
