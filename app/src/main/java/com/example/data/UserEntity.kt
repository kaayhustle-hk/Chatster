package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String, // Solves standard secure local database persistence
    val isTwitchLinked: Boolean = false,
    val twitchUsername: String = "",
    val isYoutubeLinked: Boolean = false,
    val youtubeUsername: String = "",
    val isKickLinked: Boolean = false,
    val kickUsername: String = "",
    val isFacebookLinked: Boolean = false,
    val facebookUsername: String = ""
)
