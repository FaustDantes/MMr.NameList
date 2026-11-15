package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    /** All people except "Me", in manual order. Search and A-Z sort happen in the repository. */
    @Query("SELECT * FROM person WHERE isSelf = 0 ORDER BY sortIndex, id")
    fun observeOthers(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE id = :id")
    fun observe(id: Long): Flow<Person?>

    /** Everyone, "Me" included. */
    @Query("SELECT * FROM person ORDER BY isSelf DESC, sortIndex, id")
    fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun get(id: Long): Person?

    @Query("SELECT * FROM person WHERE isSelf = 1 LIMIT 1")
    suspend fun getSelf(): Person?

    @Query("SELECT COALESCE(MAX(sortIndex), 0) FROM person")
    suspend fun maxSortIndex(): Long

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Query("SELECT id, sortIndex FROM person WHERE id IN (:ids)")
    suspend fun sortIndexes(ids: List<Long>): List<IdSortIndex>

    @Query("UPDATE person SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setSortIndex(id: Long, sortIndex: Long)

    /**
     * Puts [idsInNewOrder] in that order, using the sort slots they hold now.
     * People not in the list (other tabs) keep their places.
     */
    @Transaction
    suspend fun reorder(idsInNewOrder: List<Long>) {
        val slots = sortIndexes(idsInNewOrder).map { it.sortIndex }.sorted()
        if (slots.size != idsInNewOrder.size) return
        idsInNewOrder.forEachIndexed { index, id -> setSortIndex(id, slots[index]) }
    }

    /** "Me" cannot be deleted. */
    @Query("DELETE FROM person WHERE id = :id AND isSelf = 0")
    suspend fun deleteById(id: Long): Int
}

data class IdSortIndex(val id: Long, val sortIndex: Long)
