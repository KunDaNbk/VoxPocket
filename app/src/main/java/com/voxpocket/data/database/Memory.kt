package com.voxpocket.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Memory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: MemoryType = MemoryType.GENERAL,
    val importance: Int = 5,
    val sourceConversationId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val accessCount: Int = 0
)

enum class MemoryType {
    USER_PREFERENCE,
    FACT,
    CONVERSATION_SUMMARY,
    GENERAL
}

