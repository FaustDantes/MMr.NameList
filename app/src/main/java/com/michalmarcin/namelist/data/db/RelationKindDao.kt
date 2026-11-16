package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationKindDao {

    @Query("SELECT * FROM relation_type ORDER BY sortIndex, id")
    fun observeAll(): Flow<List<RelationKind>>

    @Query("SELECT * FROM relation_type ORDER BY sortIndex, id")
    suspend fun getAll(): List<RelationKind>

    @Query("SELECT * FROM relation_type WHERE id = :id")
    suspend fun get(id: Long): RelationKind?

    @Query("SELECT COALESCE(MAX(sortIndex), 0) FROM relation_type")
    suspend fun maxSortIndex(): Long

    @Insert
    suspend fun insert(kind: RelationKind): Long

    @Insert
    suspend fun insertAll(kinds: List<RelationKind>)

    @Query("UPDATE relation_type SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE OR IGNORE relationship SET typeId = :to WHERE typeId = :from")
    suspend fun moveLinks(from: Long, to: Long)

    @Query("DELETE FROM relationship WHERE typeId = :id")
    suspend fun deleteLinks(id: Long)

    @Query("DELETE FROM relation_type WHERE id = :id")
    suspend fun deleteRow(id: Long)

    /**
     * Deletes the type. Its links become [fallbackId] ("Other"). A link that would then be the same
     * as one that exists already is dropped, so no information is lost.
     */
    @Transaction
    suspend fun delete(id: Long, fallbackId: Long) {
        moveLinks(id, fallbackId)
        deleteLinks(id)
        deleteRow(id)
    }

    @Query("DELETE FROM relation_type")
    suspend fun deleteAll()
}
