package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/** One finished learning round (F35): how many cards of [total] the user knew, and when. */
@Entity(tableName = "learn_round")
data class LearnRound(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val known: Int,
    val total: Int,
) {
    /** Score in percent, 0 to 100. Rounds can have fewer than ten cards, so the chart uses percent. */
    val percent: Int get() = if (total <= 0) 0 else (known * 100 + total / 2) / total
}

@Dao
interface LearnRoundDao {
    @Insert
    suspend fun insert(round: LearnRound): Long

    /** Oldest first, for the progress chart. */
    @Query("SELECT * FROM learn_round ORDER BY finishedAt, id")
    suspend fun getAll(): List<LearnRound>
}
