package com.example.enterprise

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // PKCE compliant auth requires OAuthProvider
    // Example for Twitch, Kickstarter or Google
    fun getOAuthProvider(providerId: String): com.google.firebase.auth.OAuthProvider.Builder {
        return com.google.firebase.auth.OAuthProvider.newBuilder(providerId)
    }

    // Expose AuthState as a Flow for Jetpack Compose side-effects
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        
        // Initial state
        trySend(auth.currentUser)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
