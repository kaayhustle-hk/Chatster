package com.example.enterprise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseChatView(
    viewModel: EnterpriseViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Automatically trigger navigation when session becomes active
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onNavigateToDashboard()
        }
    }

    val chatEvents by viewModel.chatEvents.collectAsState()
    val suggestion by viewModel.aiActionSuggestion.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    // Slot-based architecture: we compose modular UI blocks
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        
        // AI Suggestion Header Slot
        suggestion?.let {
            AiSuggestionSlot(it)
        }

        // Virtualized List Slot for High-Velocity Events
        LazyColumn(
            modifier = Modifier.weight(1f).padding(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            reverseLayout = false
        ) {
            items(chatEvents, key = { it.eventId }) { event ->
                ChatEventRow(event)
            }
        }

        // Input Slot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Send a message...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                    focusedBorderColor = Color(0xFF6200EA),
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (inputText.isNotBlank()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun AiSuggestionSlot(suggestion: AiSuggestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF261D3B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Gemini Pro Insights ✦", color = Color(0xFFE2B714), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("${(suggestion.confidence * 100).toInt()}% Conf", color = Color.Gray, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Action: ${suggestion.actionType}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(suggestion.reason, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ChatEventRow(event: ChatEvent) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(event.authorName, fontWeight = FontWeight.Bold, color = Color(0xFF9146FF), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(event.message, color = Color.White, fontSize = 14.sp)
        }
    }
}
