package com.michalmarcin.namelist.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A person the user remembers. "Me" is the row with [isSelf] = true (docs/architecture.md, section 6). */
@Entity(
    tableName = "person",
    foreignKeys = [
        ForeignKey(entity = Tab::class, parentColumns = ["id"], childColumns = ["tabId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("tabId")],
)
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val photoFile: String? = null,
    val originCountry: String? = null,
    val metCountry: String? = null,
    val metCity: String? = null,
    val metLat: Double? = null,
    val metLon: Double? = null,
    /** Day of the first meeting as days since 1970-01-01 (local date). Null only for people saved before version 1.1. */
    val firstMetOn: Long? = null,
    /** More details (F19). All optional. [birthDate] is an epoch day, like [firstMetOn]. */
    val nativeLanguage: String? = null,
    val birthDate: Long? = null,
    val education: String? = null,
    val currentWork: String? = null,
    val askAbout: String? = null,
    /** Milliseconds of the last save from the edit screen (F19). Null for people saved before version 1.1. */
    val lastUpdatedAt: Long? = null,
    /** Milliseconds when the person moved to the archive (F18); null = not archived. */
    val archivedAt: Long? = null,
    val isSelf: Boolean = false,
    val sortIndex: Long = 0,
    val createdAt: Long = 0,
    val tabId: Long? = null,
)
