package com.sualtikasifi.cizimhafiza.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every supported old schema (3..9) must reach the current one through the real
 * migration chain, keeping its data and passing Room's own schema validation.
 * A forgotten or broken migration fails here instead of wiping a player's
 * progress after an update.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val all = arrayOf(
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7,
        AppDatabase.MIGRATION_7_8,
        AppDatabase.MIGRATION_8_9,
        AppDatabase.MIGRATION_9_10
    )

    @Test
    fun everyOldVersionMigratesToCurrent() {
        for (from in 3..9) {
            val name = "migration-test-$from"
            helper.createDatabase(name, from).close()
            helper.runMigrationsAndValidate(name, 10, true, *all).close()
        }
    }

    @Test
    fun wordsSurviveTheFullChain() {
        val name = "migration-test-words"
        helper.createDatabase(name, 3).apply {
            execSQL("INSERT INTO words (id, text, category, difficulty) VALUES (1, 'kedi', 'Hayvanlar', 'EASY')")
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 10, true, *all)
        db.query("SELECT COUNT(*) FROM words").use { c ->
            c.moveToFirst()
            check(c.getInt(0) == 1) { "word row was lost by the migration chain" }
        }
        db.close()
    }
}
