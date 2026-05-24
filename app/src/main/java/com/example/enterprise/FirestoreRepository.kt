package com.example.enterprise

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Collection References
    private val streamsCollection = firestore.collection("StreamMetadata")
    private val chatCollection = firestore.collection("ChatEvent")

    // SnapshotListener for real-time reliable data sync
    fun getStreamMetadata(streamId: String): Flow<StreamMetadata?> = callbackFlow {
        val listener = streamsCollection.document(streamId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val metadata = snapshot.toObject(StreamMetadata::class.java)
                trySend(metadata)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    // High velocity chat event tracking
    fun getChatEvents(streamId: String, limit: Long = 100): Flow<List<ChatEvent>> = callbackFlow {
        val query = chatCollection
            .whereEqualTo("streamId", streamId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val events = snapshot.documents.mapNotNull { it.toObject(ChatEvent::class.java) }
                trySend(events.reversed()) // Order to ASC for UI display
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveChatEvent(event: ChatEvent) {
        chatCollection.document(event.eventId).set(event).await()
    }
}
