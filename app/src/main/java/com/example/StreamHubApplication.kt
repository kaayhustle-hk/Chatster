package com.example

import android.app.Application
import com.example.data.StreamDatabase
import com.example.data.StreamPlatformRepository

class StreamHubApplication : Application() {
    val database by lazy { StreamDatabase.getDatabase(this) }
    val repository by lazy { 
        StreamPlatformRepository(
            database.streamingPlatformDao(),
            database.userDao(),
            database.chatMessageDao()
        ) 
    }
}
