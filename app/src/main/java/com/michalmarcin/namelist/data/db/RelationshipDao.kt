package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {

    @Query("SELECT * FROM relationship WHERE fromPersonId = :personId OR toPersonId = :personId")
    fun observeFor(personId: Long): Flow<List<Relationship>>

    @Query(
        "SELECT * FROM relationship WHERE (fromPersonId = :a AND toPersonId = :b) " +
            "OR (fromPersonId = :b AND toPersonId = :a)",
    )
    suspend fun between(a: Long, b: Long): List<Relationship>

    @Query("SELECT * FROM relationship WHERE id = :id")
    suspend fun get(id: Long): Relationship?

    @Insert
    suspend fun insert(relationship: Relationship): Long

    @Query("UPDATE relationship SET typeId = :typeId WHERE id = :id")
    suspend fun setType(id: Long, typeId: Long)

    @Query("DELETE FROM relationship WHERE id = :id")
    suspend fun delete(id: Long)

    // Backup (M7)
    @Query("SELECT * FROM relationship ORDER BY id")
    suspend fun getAll(): List<Relationship>

    @Insert
    suspend fun insertAll(relationships: List<Relationship>)

    @Query("DELETE FROM relationship")
    suspend fun deleteAll()
}
