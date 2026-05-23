package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StreamingPlatformEntity::class,
        UserEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StreamDatabase : RoomDatabase() {
    abstract fun streamingPlatformDao(): StreamingPlatformDao
    abstract fun userDao(): UserDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: StreamDatabase? = null

        fun getDatabase(context: Context): StreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StreamDatabase::class.java,
                    "stream_hub_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
