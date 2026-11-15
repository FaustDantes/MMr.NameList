package com.michalmarcin.namelist.data.db

import android.content.Context
import androidx.room.AutoMigration
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
 */
@Database(
    entities = [Person::class, Tab::class, Relationship::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
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
