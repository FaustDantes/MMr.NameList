package com.michalmarcin.namelist.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.michalmarcin.namelist.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** N4: data survives app updates. Each schema version must migrate without loss. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun version1To2KeepsPeopleAndAddsTabs() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                "INSERT INTO person (id, name, notes, photoFile, isSelf, sortIndex, createdAt) VALUES " +
                    "(1, 'Me', '', NULL, 1, 0, 0), (2, 'Anna', 'met in Brno', 'a.jpg', 0, 1, 5)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true)

        db.query("SELECT name, notes, photoFile, sortIndex, tabId FROM person WHERE id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("Anna", it.getString(0))
            assertEquals("met in Brno", it.getString(1))
            assertEquals("a.jpg", it.getString(2))
            assertEquals(1L, it.getLong(3))
            assertTrue(it.isNull(4))
        }
        db.query("SELECT COUNT(*) FROM person").use {
            it.moveToFirst()
            assertEquals(2, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM tab").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun version2To3AddsRelationships() {
        helper.createDatabase(DB, 2).apply {
            execSQL("INSERT INTO tab (id, name, sortIndex) VALUES (1, 'Work', 1)")
            execSQL(
                "INSERT INTO person (id, name, notes, isSelf, sortIndex, createdAt, tabId) VALUES " +
                    "(1, 'Me', '', 1, 0, 0, NULL), (2, 'Anna', '', 0, 1, 5, 1)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 3, true)
        db.query("SELECT tabId FROM person WHERE id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals(1L, it.getLong(0))
        }
        db.execSQL("INSERT INTO relationship (fromPersonId, toPersonId, type) VALUES (2, 1, 'FRIEND')")
    }

    @Test
    fun version3To4AddsFirstMetDateAndKeepsPeople() {
        helper.createDatabase(DB, 3).apply {
            execSQL("INSERT INTO person (id, name, notes, isSelf, sortIndex, createdAt) VALUES (2, 'Anna', 'x', 0, 1, 5)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 4, true)
        db.query("SELECT name, firstMetOn FROM person WHERE id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("Anna", it.getString(0))
            assertTrue("old people have no stored date", it.isNull(1))
        }
    }

    @Test
    fun version4To5AddsMoreDetailsAndArchiveColumns() {
        helper.createDatabase(DB, 4).apply {
            execSQL("INSERT INTO person (id, name, notes, isSelf, sortIndex, createdAt, firstMetOn) VALUES (2, 'Anna', 'x', 0, 1, 5, 19000)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 5, true)
        db.query("SELECT name, firstMetOn, nativeLanguage, birthDate, education, currentWork, askAbout, lastUpdatedAt, archivedAt FROM person WHERE id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("Anna", it.getString(0))
            assertEquals(19_000L, it.getLong(1))
            (2..8).forEach { column -> assertTrue("column $column starts empty", it.isNull(column)) }
        }
        // Old people are not archived: they show in the list.
        db.query("SELECT COUNT(*) FROM person WHERE archivedAt IS NULL").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun version1To3InOneUpdate() {
        helper.createDatabase(DB, 1).apply {
            execSQL("INSERT INTO person (id, name, notes, isSelf, sortIndex, createdAt) VALUES (2, 'Anna', 'x', 0, 1, 5)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 5, true)
        db.query("SELECT name FROM person WHERE id = 2").use {
            assertTrue(it.moveToFirst())
            assertEquals("Anna", it.getString(0))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
