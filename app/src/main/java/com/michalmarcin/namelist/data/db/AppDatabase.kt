package com.michalmarcin.namelist.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.migration.AutoMigrationSpec
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version history:
 * 1 (M1): person.
 * 2 (M3): tab, person.tabId. Auto migration; tested in MigrationTest.
 * 3 (M5): relationship. Auto migration; tested in MigrationTest.
 * 4 (1.1.0): person.firstMetOn. Auto migration; tested in MigrationTest.
 * 5 (1.1.0): person.nativeLanguage, birthDate, education, currentWork, askAbout, lastUpdatedAt, archivedAt.
 *   Auto migration (all columns are nullable); tested in MigrationTest.
 * 6 (1.3.0): person_tab, many tabs per person (F28). Auto migration; [PersonTabMigration] copies each
 *   person.tabId into person_tab. The column person.tabId stays, unused, so no data is lost (ADR-035).
 */
@Database(
    entities = [Person::class, Tab::class, Relationship::class, PersonTab::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6, spec = PersonTabMigration::class)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao

    abstract fun tabDao(): TabDao

    abstract fun relationshipDao(): RelationshipDao

    companion object {
        const val NAME = "namelist.db"
        const val SELF_NAME = "Me"

        fun build(context: Context, inMemory: Boolean = false): AppDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, NAME)
            }
            return builder.addCallback(SeedCallback).build()
        }
    }

    /** Creates the "Me" row once, when the database file is created. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO person (name, notes, isSelf, sortIndex, createdAt) VALUES (?, '', 1, 0, ?)",
                arrayOf<Any>(SELF_NAME, System.currentTimeMillis()),
            )
        }
    }
}

/** Version 5 to 6: every person who had a tab is in that tab in the new table. */
class PersonTabMigration : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO person_tab (personId, tabId) SELECT id, tabId FROM person WHERE tabId IS NOT NULL")
    }
}
