package com.michalmarcin.namelist.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The relationship types the app ships with (F13). Since 1.3.0 the types are rows in
 * [RelationKind] (F30); this enum names the built-in rows. [seedId] is the fixed row id of
 * each built-in type, so a migration and an old backup can find it.
 */
enum class RelationType(val seedId: Long) {
    FRIEND(1), FAMILY(2), PARTNER(3), SIBLING(4), PARENT(5), CHILD(6), COLLEAGUE(7), NEIGHBOUR(8), OTHER(9);

    val inverse: RelationType
        get() = when (this) {
            PARENT -> CHILD
            CHILD -> PARENT
            else -> this
        }
}

/**
 * A relationship type the user can rename, add or delete in Settings (F30, ADR-037).
 * [name] is null for a built-in type the user did not rename: it then shows the built-in
 * label in the app language. [builtIn] is null for a type the user made.
 */
@Entity(tableName = "relation_type")
data class RelationKind(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String? = null,
    val builtIn: RelationType? = null,
    val sortIndex: Long = 0,
) {
    /** "Other" is where links of a deleted type go; parent and child are a pair. These three stay. */
    val canDelete: Boolean get() = builtIn !in KEPT

    companion object {
        private val KEPT = setOf(RelationType.OTHER, RelationType.PARENT, RelationType.CHILD)

        val builtIns: List<RelationKind> =
            RelationType.entries.mapIndexed { index, type -> RelationKind(type.seedId, null, type, index.toLong()) }

        /** Writes the nine built-in rows. Used when the database is created and by the 6 to 7 migration. */
        fun seed(db: SupportSQLiteDatabase) {
            builtIns.forEach { kind ->
                db.execSQL(
                    "INSERT OR IGNORE INTO relation_type (id, name, builtIn, sortIndex) VALUES (?, NULL, ?, ?)",
                    arrayOf<Any>(kind.id, kind.builtIn?.name.orEmpty(), kind.sortIndex),
                )
            }
        }
    }
}

/**
 * How one person relates to another (F13). One row per link (ADR-003):
 * the row (from = A, to = B, type = T) means "B is A's T".
 * Seen from B, it reads "A is B's inverse of T".
 */
@Entity(
    tableName = "relationship",
    foreignKeys = [
        ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["fromPersonId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["toPersonId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RelationKind::class, parentColumns = ["id"], childColumns = ["typeId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["fromPersonId", "toPersonId", "typeId"], unique = true), Index("toPersonId"), Index("typeId")],
)
data class Relationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPersonId: Long,
    val toPersonId: Long,
    val typeId: Long,
)

/** Enums are stored by name, never by ordinal (AGENTS.md). An unknown name reads as OTHER. */
class Converters {
    @TypeConverter
    fun relationTypeToName(type: RelationType?): String? = type?.name

    @TypeConverter
    fun nameToRelationType(name: String?): RelationType? =
        name?.let { value -> RelationType.entries.firstOrNull { it.name == value } ?: RelationType.OTHER }
}
