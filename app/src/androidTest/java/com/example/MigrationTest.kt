package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.TempleDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TempleDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2To3() {
        // Create database version 1
        var db = helper.createDatabase(TEST_DB, 1)
        db.execSQL("INSERT INTO visits (id, placeId, startDate, notes, categoryIds, state) VALUES ('v1', 'p1', 1000, 'notes', '[]', 'VISITED')")
        db.close()

        // Migrate to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, TempleMapDatabase.MIGRATION_1_2)

        // Migrate to version 3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, TempleMapDatabase.MIGRATION_2_3)
    }
}
