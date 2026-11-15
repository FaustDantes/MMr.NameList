package com.michalmarcin.namelist.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    /** All people except "Me", in manual order. Search and A-Z sort happen in the repository. */
    @Query("SELECT * FROM person WHERE isSelf = 0 ORDER BY sortIndex, id")
    fun observeOthers(): Flow<List<Person>>

    @Query("SELECT * FROM person WHERE id = :id")
    fun observe(id: Long): Flow<Person?>

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

    /** "Me" cannot be deleted. */
    @Query("DELETE FROM person WHERE id = :id AND isSelf = 0")
    suspend fun deleteById(id: Long): Int
}
