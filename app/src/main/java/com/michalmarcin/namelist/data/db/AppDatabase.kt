package com.michalmarcin.namelist.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.migration.AutoMigrationSpec
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
 * 7 (1.3.0): relation_type, editable relationship types (F30); relationship.type becomes relationship.typeId.
 *   Tags with the same name are merged (F29). Hand-written [Migration6To7]; tested in MigrationTest (ADR-037).
 */
@Database(
    entities = [Person::class, Tab::class, Relationship::class, PersonTab::class, RelationKind::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6, spec = PersonTabMigration::class)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao

    abstract fun tabDao(): TabDao

    abstract fun relationshipDao(): RelationshipDao

    abstract fun relationKindDao(): RelationKindDao

    companion object {
        const val NAME = "namelist.db"
        const val SELF_NAME = "Me"

        fun build(context: Context, inMemory: Boolean = false): AppDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, NAME)
            }
            return builder.addMigrations(Migration6To7).addCallback(SeedCallback).build()
        }
    }

    /** Creates the "Me" row once, when the database file is created. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO person (name, notes, isSelf, sortIndex, createdAt) VALUES (?, '', 1, 0, ?)",
                arrayOf<Any>(SELF_NAME, System.currentTimeMillis()),
            )
            RelationKind.seed(db)
        }
    }
}

/** Version 5 to 6: every person who had a tab is in that tab in the new table. */
class PersonTabMigration : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT OR IGNORE INTO person_tab (personId, tabId) SELECT id, tabId FROM person WHERE tabId IS NOT NULL")
    }
}

/**
 * Version 6 to 7 (ADR-037). Room cannot change a column with an auto migration and keep its data, so this is by hand:
 * 1. relation_type gets the nine built-in types with fixed ids.
 * 2. relationship is built again with typeId in place of the type name. An unknown name becomes "Other".
 * 3. Tags whose names differ only in case or outer spaces are merged into the first one (F29).
 */
object Migration6To7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `relation_type` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT, `builtIn` TEXT, `sortIndex` INTEGER NOT NULL)",
        )
        RelationKind.seed(db)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `relationship_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`fromPersonId` INTEGER NOT NULL, `toPersonId` INTEGER NOT NULL, `typeId` INTEGER NOT NULL, " +
                "FOREIGN KEY(`fromPersonId`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`toPersonId`) REFERENCES `person`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`typeId`) REFERENCES `relation_type`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO relationship_new (id, fromPersonId, toPersonId, typeId) " +
                "SELECT r.id, r.fromPersonId, r.toPersonId, " +
                "COALESCE((SELECT t.id FROM relation_type t WHERE t.builtIn = r.type), ${RelationType.OTHER.seedId}) " +
                "FROM relationship r",
        )
        db.execSQL("DROP TABLE relationship")
        db.execSQL("ALTER TABLE relationship_new RENAME TO relationship")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_relationship_fromPersonId_toPersonId_typeId` " +
                "ON `relationship` (`fromPersonId`, `toPersonId`, `typeId`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_relationship_toPersonId` ON `relationship` (`toPersonId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_relationship_typeId` ON `relationship` (`typeId`)")
        mergeSameNamedTags(db)
    }

    private fun mergeSameNamedTags(db: SupportSQLiteDatabase) {
        val tags = mutableListOf<Pair<Long, String>>()
        db.query("SELECT id, name FROM tab ORDER BY sortIndex, id").use { cursor ->
            while (cursor.moveToNext()) tags += cursor.getLong(0) to cursor.getString(1)
        }
        tags.groupBy({ labelKey(it.second) }, { it.first }).values.filter { it.size > 1 }.forEach { ids ->
            val keep = ids.first()
            ids.drop(1).forEach { duplicate ->
                db.execSQL(
                    "INSERT OR IGNORE INTO person_tab (personId, tabId) SELECT personId, ? FROM person_tab WHERE tabId = ?",
                    arrayOf<Any>(keep, duplicate),
                )
                db.execSQL("DELETE FROM person_tab WHERE tabId = ?", arrayOf<Any>(duplicate))
                db.execSQL("UPDATE person SET tabId = ? WHERE tabId = ?", arrayOf<Any>(keep, duplicate))
                db.execSQL("DELETE FROM tab WHERE id = ?", arrayOf<Any>(duplicate))
            }
        }
    }
}
