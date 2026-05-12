package com.voxpocket.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM Memory ORDER BY importance DESC, lastAccessed DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM Memory WHERE content LIKE '%' || :query || '%' ORDER BY importance DESC")
    fun searchMemories(query: String): Flow<List<Memory>>

    @Query("SELECT * FROM Memory WHERE importance >= :minImportance ORDER BY lastAccessed DESC LIMIT :limit")
    suspend fun getTopMemories(minImportance: Int, limit: Int): List<Memory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory)

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)
}

