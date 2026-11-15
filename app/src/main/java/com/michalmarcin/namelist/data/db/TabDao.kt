package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {

    @Query("SELECT * FROM tab ORDER BY sortIndex, id")
    fun observeAll(): Flow<List<Tab>>

    @Query("SELECT COALESCE(MAX(sortIndex), 0) FROM tab")
    suspend fun maxSortIndex(): Long

    @Insert
    suspend fun insert(tab: Tab): Long

    @Query("UPDATE tab SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** People in the tab keep their data; their tabId becomes null (foreign key SET NULL). */
    @Query("DELETE FROM tab WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE tab SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setSortIndex(id: Long, sortIndex: Long)

    @Transaction
    suspend fun reorder(idsInNewOrder: List<Long>) {
        idsInNewOrder.forEachIndexed { index, id -> setSortIndex(id, index.toLong()) }
    }
}
