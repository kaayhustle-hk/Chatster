package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Represents a user profile in Firebase.
 */
data class FirebaseUser(
    @DocumentId
    val userId: String = "",
    val displayName: String = "Anonymous Streamer",
    val email: String = "",
    val photoUrl: String = "",
    val status: String = "offline",
    val lastSeen: Timestamp = Timestamp.now()
)

/**
 * Represents a specific chat stream (e.g., a Twitch or YouTube broadcast session).
 */
data class FirebaseChatStream(
    @DocumentId
    val streamId: String = "",
    val title: String = "Untitled Broadcast",
    val platform: String = "generic",
    val hostId: String = "",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Represents a message within a specific stream's subcollection.
 */
data class FirebaseChatMessage(
    @DocumentId
    val messageId: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "Guest",
    val timestamp: Timestamp = Timestamp.now(),
    @get:PropertyName("isFromMe")
    @set:PropertyName("isFromMe")
    var isFromMe: Boolean = false
)
