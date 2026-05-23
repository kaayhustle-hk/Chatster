package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platform: String, // "Twitch", "YouTube", "Kick", "Facebook Live"
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isFromMe: Boolean = false
)
