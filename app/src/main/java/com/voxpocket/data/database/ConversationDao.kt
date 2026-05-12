package com.voxpocket.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM Conversation ORDER BY createdAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM Conversation WHERE id = :id")
    suspend fun getConversationById(id: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)
}

