package com.example.enterprise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EnterpriseViewModel(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val repository: FirestoreRepository = FirestoreRepository(),
    private val geminiAnalyzer: GeminiChatAnalyzer = GeminiChatAnalyzer()
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authService.getAuthStateFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _streamId = MutableStateFlow("global_stream_123") // Hardcoded for demo

    // Combining real-time stream metadata with UI state mapping
    val streamMetadata: StateFlow<StreamMetadata?> = repository.getStreamMetadata(_streamId.value)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatEvents: StateFlow<List<ChatEvent>> = repository.getChatEvents(_streamId.value)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiActionSuggestion = MutableStateFlow<AiSuggestion?>(null)
    val aiActionSuggestion: StateFlow<AiSuggestion?> = _aiActionSuggestion

    fun sendChatMessage(message: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val event = ChatEvent(
                eventId = java.util.UUID.randomUUID().toString(),
                streamId = _streamId.value,
                authorName = user.displayName ?: "Anonymous",
                message = message,
                timestamp = System.currentTimeMillis()
            )
            repository.saveChatEvent(event)
        }
    }

    fun requestAiAnalysis() {
        val currentContext = chatEvents.value
        val title = streamMetadata.value?.title ?: "Untitled Stream"
        
        // Ensure analysis happens without blocking UI
        viewModelScope.launch {
            val suggestion = geminiAnalyzer.analyzeChatContext(currentContext.takeLast(30), title)
            if (suggestion != null) {
                _aiActionSuggestion.value = suggestion
            }
        }
    }
}
