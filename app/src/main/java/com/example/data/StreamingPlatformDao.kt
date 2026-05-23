package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamingPlatformDao {
    @Query("SELECT * FROM streaming_platforms ORDER BY id ASC")
    fun getAllPlatforms(): Flow<List<StreamingPlatformEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: StreamingPlatformEntity)

    @Update
    suspend fun updatePlatform(platform: StreamingPlatformEntity)

    @Delete
    suspend fun deletePlatform(platform: StreamingPlatformEntity)

    @Query("DELETE FROM streaming_platforms WHERE id = :id")
    suspend fun deletePlatformById(id: Int)

    @Query("UPDATE streaming_platforms SET isActive = :isActive WHERE id = :id")
    suspend fun toggleActiveState(id: Int, isActive: Boolean)

    @Query("UPDATE streaming_platforms SET streamTitle = :title, streamDescription = :description, category = :category, lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, title: String, description: String, category: String, timestamp: Long)
}
