package com.michalmarcin.namelist.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** A person is in a tab (F4, F28). One row per pair, so a person can be in many tabs. */
@Entity(
    tableName = "person_tab",
    primaryKeys = ["personId", "tabId"],
    foreignKeys = [
        ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tab::class, parentColumns = ["id"], childColumns = ["tabId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tabId")],
)
data class PersonTab(val personId: Long, val tabId: Long)
