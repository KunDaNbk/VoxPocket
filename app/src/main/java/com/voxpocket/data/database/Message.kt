package com.voxpocket.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String,
    val content: String,
    val thinkingProcess: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

