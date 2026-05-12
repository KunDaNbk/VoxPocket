package com.voxpocket.data.repository

import com.voxpocket.data.database.*
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val db: AppDatabase) {
    fun getAllConversations(): Flow<List<Conversation>> =
        db.conversationDao().getAllConversations()

    fun getMessagesByConversationId(id: String): Flow<List<Message>> =
        db.messageDao().getMessagesByConversationId(id)

    suspend fun createConversation(title: String = "新对话"): Conversation {
        val conversation = Conversation(title = title)
        db.conversationDao().insertConversation(conversation)
        return conversation
    }

    suspend fun updateConversation(conversation: Conversation) =
        db.conversationDao().updateConversation(conversation)

    suspend fun deleteConversation(conversation: Conversation) {
        db.messageDao().deleteMessagesByConversationId(conversationId = conversation.id)
        db.conversationDao().deleteConversation(conversation)
    }

    suspend fun addMessage(conversationId: String, role: String, content: String, thinkingProcess: String? = null): Message {
        val message = Message(conversationId = conversationId, role = role, content = content, thinkingProcess = thinkingProcess)
        db.messageDao().insertMessage(message)
        return message
    }
    
    suspend fun updateMessage(message: Message) {
        db.messageDao().updateMessage(message)
    }
    
    suspend fun compressContext(conversationId: String, keepMessages: Int = 10) {
        val messages = db.messageDao().getMessagesListByConversationIdSync(conversationId)
        if (messages.size > keepMessages) {
            val oldMessages = messages.dropLast(keepMessages)
            var compressedCount = 0
            oldMessages.forEach { msg ->
                if (msg.thinkingProcess != null) {
                    val compressedThinking = if (msg.thinkingProcess.length > 200) {
                        "[已压缩] ${msg.thinkingProcess.take(200)}... (原文 ${msg.thinkingProcess.length} 字)"
                    } else {
                        msg.thinkingProcess
                    }
                    db.messageDao().updateMessage(msg.copy(thinkingProcess = compressedThinking))
                    compressedCount++
                }
            }
            android.util.Log.d("ChatRepository", "压缩了 $compressedCount 条消息的思考内容")
        }
    }
    
    suspend fun clearThinkingProcess(conversationId: String) {
        val messages = db.messageDao().getMessagesListByConversationIdSync(conversationId)
        messages.forEach { msg ->
            if (msg.thinkingProcess != null) {
                db.messageDao().updateMessage(msg.copy(thinkingProcess = null))
            }
        }
    }
    
    suspend fun getMessagesForContext(conversationId: String, maxMessages: Int = 20): List<Message> {
        val allMessages = db.messageDao().getMessagesListByConversationIdSync(conversationId)
        return allMessages.takeLast(maxMessages)
    }
}
