package com.michalmarcin.namelist.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * How one person relates to another (F13). One row per link (ADR-003):
 * the row (from = A, to = B, type = T) means "B is A's T".
 * Seen from B, it reads "A is B's T.inverse".
 */
enum class RelationType {
    FRIEND, FAMILY, PARTNER, SIBLING, PARENT, CHILD, COLLEAGUE, NEIGHBOUR, OTHER;

    val inverse: RelationType
        get() = when (this) {
            PARENT -> CHILD
            CHILD -> PARENT
            else -> this
        }
}

@Entity(
    tableName = "relationship",
    foreignKeys = [
        ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["fromPersonId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["toPersonId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["fromPersonId", "toPersonId", "type"], unique = true), Index("toPersonId")],
)
data class Relationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPersonId: Long,
    val toPersonId: Long,
    val type: RelationType,
)

/** Enums are stored by name, never by ordinal (AGENTS.md). An unknown name reads as OTHER. */
class Converters {
    @TypeConverter
    fun relationTypeToName(type: RelationType): String = type.name

    @TypeConverter
    fun nameToRelationType(name: String): RelationType =
        RelationType.entries.firstOrNull { it.name == name } ?: RelationType.OTHER
}
