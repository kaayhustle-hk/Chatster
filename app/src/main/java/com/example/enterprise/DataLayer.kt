package com.example.enterprise

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class StreamMetadata(
    val streamId: String = "",
    val title: String = "",
    val category: String = "",
    val platform: String = "",
    val activeViewers: Long = 0L,
    val isLive: Boolean = false,
    val startedAt: Long = 0L
)

@Serializable
@Immutable
data class ChatEvent(
    val eventId: String = "",
    val streamId: String = "",
    val authorName: String = "",
    val authorRoles: List<String> = emptyList(),
    val message: String = "",
    val timestamp: Long = 0L,
    val isHighlighted: Boolean = false,
    val sentimentScore: Float = 0f
)

@Immutable
data class AiSuggestion(
    val actionType: String,
    val reason: String,
    val confidence: Float
)
