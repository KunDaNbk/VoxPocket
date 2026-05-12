package com.voxpocket.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM Message WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversationId(conversationId: String): Flow<List<Message>>
    
    @Query("SELECT * FROM Message WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesListByConversationIdSync(conversationId: String): List<Message>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Query("DELETE FROM Message WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversationId(conversationId: String)
}

