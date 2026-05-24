package com.example.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Repository handling all Firebase Firestore operations for the Multistream Chat Hub.
 * Optimized with Kotlin Flows and Coroutines for lifecycle-aware performance.
 */
class FirebaseChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "FirebaseChatRepository"

    /**
     * Creates a new chat stream document in the top-level 'streams' collection.
     */
    suspend fun createStream(title: String, platform: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val stream = FirebaseChatStream(
                title = title,
                platform = platform,
                hostId = userId,
                isActive = true
            )
            val docRef = firestore.collection("streams").add(stream).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating stream", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a message to a specific stream's 'messages' subcollection.
     */
    suspend fun sendMessage(streamId: String, text: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val message = FirebaseChatMessage(
                text = text,
                senderId = user.uid,
                senderName = user.displayName ?: "User ${user.uid.take(4)}",
                isFromMe = true
            )
            firestore.collection("streams")
                .document(streamId)
                .collection("messages")
                .add(message)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * Observes real-time message updates from a specific stream using a callbackFlow.
     * Automatically handles registration/unregistration to prevent memory leaks.
     */
    fun observeMessages(streamId: String): Flow<List<FirebaseChatMessage>> = callbackFlow {
        val query = firestore.collection("streams")
            .document(streamId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(FirebaseChatMessage::class.java)
            } ?: emptyList()

            trySend(messages)
        }

        // Ensuring the listener is removed when the flow is no longer collected
        awaitClose {
            subscription.remove()
        }
    }

    /**
     * Updates user presence/profile info in the 'users' collection.
     */
    suspend fun updateUserProfile(displayName: String, photoUrl: String = "") {
        val userId = auth.currentUser?.uid ?: return
        val userMap = mapOf(
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "lastSeen" to com.google.firebase.Timestamp.now(),
            "status" to "online"
        )
        try {
            firestore.collection("users").document(userId).set(userMap).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
        }
    }
}
