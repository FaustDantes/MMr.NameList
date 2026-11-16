package com.michalmarcin.namelist.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-made group, such as "Work" or "Family" (F4). The app calls it a "tag" since 1.3.0 (F29);
 * the code and the table keep the old name "tab", so no data moves (ADR-037).
 */
@Entity(tableName = "tab")
data class Tab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortIndex: Long = 0,
)

/** Two tag or relationship type names are the same when this key is the same: no case, no outer spaces (F29). */
fun labelKey(name: String): String = name.trim().lowercase()
