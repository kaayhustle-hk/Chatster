package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaming_platforms")
data class StreamingPlatformEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,             // "Twitch", "YouTube", "Kick", "Facebook Live", "Custom RTMP"
    val streamUrl: String,        // Ingestion RTMP URL
    val streamKey: String,        // Stream Key (masked in UI by default)
    val username: String = "",     // Optional custom channel handle
    val isActive: Boolean = true,  // Toggle active destination multiplier
    val latencyMode: String = "Normal", // "Normal", "Low", "Ultra-Low"
    val streamTitle: String = "", 
    val streamDescription: String = "",
    val category: String = "",
    val lastSyncedAt: Long = 0L
)
