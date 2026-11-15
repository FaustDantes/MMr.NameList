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
    val isSelf: Boolean = false,
    val sortIndex: Long = 0,
    val createdAt: Long = 0,
    val tabId: Long? = null,
)
