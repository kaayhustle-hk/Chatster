package com.example.enterprise

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GeminiChatAnalyzer {

    // Using Firebase AI Logic SDK for production-grade security w/ App Check
    private val generativeModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel("gemini-3.1-pro-preview")
    }

    /**
     * Real-time semantic analysis of the chat feed to suggest optimizations.
     */
    suspend fun analyzeChatContext(chatWindow: List<ChatEvent>, currentTitle: String): AiSuggestion? = withContext(Dispatchers.IO) {
        if (chatWindow.isEmpty()) return@withContext null

        val promptBuilder = StringBuilder()
        promptBuilder.append("Current Stream Title: $currentTitle\n")
        promptBuilder.append("Recent Chat History (${chatWindow.size} messages):\n")
        
        chatWindow.forEach { event ->
            promptBuilder.append("[${event.timestamp}] ${event.authorName}: ${event.message}\n")
        }

        promptBuilder.append("\nAnalyze the above chat. Based on the audience sentiment and topics discussed, suggest ONE action to moderate the chat or optimize the stream title. Reply ONLY in strict JSON format: { \"actionType\": \"TITLE_CHANGE|MODERATE|ENGAGEMENT\", \"reason\": \"<brief explanation>\", \"confidence\": <float 0.0-1.0> }")

        try {
            val response = generativeModel.generateContent(promptBuilder.toString())
            val text = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```") ?: return@withContext null
            
            // Deserialize response directly into our internal AiSuggestion format
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            json.decodeFromString<AiSuggestion>(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
