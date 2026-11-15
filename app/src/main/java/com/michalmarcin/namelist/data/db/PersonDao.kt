package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    /** All people except "Me" and the archive, in manual order. Search and A-Z sort happen in the repository. */
    @Query("SELECT * FROM person WHERE isSelf = 0 AND archivedAt IS NULL ORDER BY sortIndex, id")
    fun observeOthers(): Flow<List<Person>>

    /** The archive (F18), the newest first. */
    @Query("SELECT * FROM person WHERE isSelf = 0 AND archivedAt IS NOT NULL ORDER BY archivedAt DESC, id")
    fun observeArchived(): Flow<List<Person>>

    /** "Me" and everyone who is not archived. */
    @Query("SELECT * FROM person WHERE archivedAt IS NULL ORDER BY isSelf DESC, sortIndex, id")
    fun observeActive(): Flow<List<Person>>

    /** "Me" cannot be archived. People already in the archive keep their first timestamp. */
    @Query("UPDATE person SET archivedAt = :now WHERE id IN (:ids) AND isSelf = 0 AND archivedAt IS NULL")
    suspend fun archive(ids: List<Long>, now: Long): Int

    @Query("UPDATE person SET archivedAt = NULL WHERE id IN (:ids)")
    suspend fun restore(ids: List<Long>): Int

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

    // Backup (M7)
    @Query("SELECT * FROM person ORDER BY id")
    suspend fun getAll(): List<Person>

    @Insert
    suspend fun insertAll(people: List<Person>)

    @Query("DELETE FROM person")
    suspend fun deleteAll()

    // Multi-select (M8)
    @Query("SELECT photoFile FROM person WHERE id IN (:ids) AND isSelf = 0 AND photoFile IS NOT NULL")
    suspend fun photoFiles(ids: List<Long>): List<String>

    /** "Me" is never deleted, even when selected. */
    @Query("DELETE FROM person WHERE id IN (:ids) AND isSelf = 0")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("UPDATE person SET tabId = :tabId WHERE id IN (:ids)")
    suspend fun setTab(ids: List<Long>, tabId: Long?)

    /** "Me" cannot be deleted. */
    @Query("DELETE FROM person WHERE id = :id AND isSelf = 0")
    suspend fun deleteById(id: Long): Int
}

data class IdSortIndex(val id: Long, val sortIndex: Long)
