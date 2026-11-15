package com.michalmarcin.namelist.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao

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
