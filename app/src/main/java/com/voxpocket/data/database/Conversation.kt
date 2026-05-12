package com.voxpocket.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Conversation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis()
)

