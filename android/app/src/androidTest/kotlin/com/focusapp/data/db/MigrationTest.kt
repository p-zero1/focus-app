package com.focusapp.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the Room database schema at each version.
 *
 * V1 only: verifies that the database can be created with the current schema
 * without errors. When a schema migration is added (v1→v2, etc.), add a
 * corresponding test case using [helper.runMigrationsAndValidate].
 *
 * To regenerate the schema JSON used by this test, rebuild with:
 *   ./gradlew :app:kspDebugKotlin
 * The generated schema will appear in `app/schemas/`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FocusDatabase::class.java,
    )

    @Test
    fun createVersion2_schemaIsValid() {
        // Opens a fresh v2 database. MigrationTestHelper validates the schema
        // against the generated JSON export and throws if there is a mismatch.
        helper.createDatabase(TEST_DB, 2).close()
    }

    // ---- Placeholder for future migrations ----
    // @Test
    // fun migrate1To2_dataIntegrity() {
    //     helper.createDatabase(TEST_DB, 1).apply {
    //         // Insert test rows in v1 format
    //         close()
    //     }
    //     val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
    //     // Assert migrated rows look correct
    //     db.close()
    // }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
