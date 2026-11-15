package com.michalmarcin.namelist.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-made group, such as "Work" or "Family" (F4). */
@Entity(tableName = "tab")
data class Tab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortIndex: Long = 0,
)
