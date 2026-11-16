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

    /** People in the tab keep their data; only their links to the tab go (foreign key CASCADE). */
    @Query("DELETE FROM tab WHERE id = :id")
    suspend fun delete(id: Long)

    // Backup (M7)
    @Query("SELECT * FROM tab ORDER BY id")
    suspend fun getAll(): List<Tab>

    @Insert
    suspend fun insertAll(tabs: List<Tab>)

    @Query("DELETE FROM tab")
    suspend fun deleteAll()

    @Query("UPDATE tab SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setSortIndex(id: Long, sortIndex: Long)

    @Transaction
    suspend fun reorder(idsInNewOrder: List<Long>) {
        idsInNewOrder.forEachIndexed { index, id -> setSortIndex(id, index.toLong()) }
    }
}
