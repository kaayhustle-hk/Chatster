package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StreamPlatformRepository(
    private val dao: StreamingPlatformDao,
    private val userDao: UserDao,
    private val chatDao: ChatMessageDao
) {

    val allPlatforms: Flow<List<StreamingPlatformEntity>> = dao.getAllPlatforms()

    // Destinations management
    suspend fun insert(platform: StreamingPlatformEntity) {
        dao.insertPlatform(platform)
    }

    suspend fun update(platform: StreamingPlatformEntity) {
        dao.updatePlatform(platform)
    }

    suspend fun delete(platform: StreamingPlatformEntity) {
        dao.deletePlatform(platform)
    }

    suspend fun deleteById(id: Int) {
        dao.deletePlatformById(id)
    }

    suspend fun toggleActive(id: Int, isActive: Boolean) {
        dao.toggleActiveState(id, isActive)
    }

    suspend fun syncMetadata(id: Int, title: String, description: String, category: String, timestamp: Long) {
        dao.updateSyncStatus(id, title, description, category, timestamp)
    }

    // User authentication & social accounts link
    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    fun getUserById(userId: Int): Flow<UserEntity?> {
        return userDao.getUserById(userId)
    }

    suspend fun registerUser(user: UserEntity): Long {
        return userDao.registerUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun resetPassword(email: String, newPasswordHash: String): Boolean {
        return userDao.resetPassword(email, newPasswordHash) > 0
    }

    // Cross-platform chat management
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>> {
        return chatDao.getAllMessages()
    }

    fun getChatMessagesByPlatform(platform: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesByPlatform(platform)
    }

    suspend fun insertChatMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    suspend fun clearAllChatMessages() {
        chatDao.clearAllMessages()
    }

    suspend fun checkAndPrepopulateIfEmpty() {
        val currentList = dao.getAllPlatforms().first()
        if (currentList.isEmpty()) {
            val defaults = listOf(
                StreamingPlatformEntity(
                    name = "Twitch",
                    streamUrl = "rtmp://live.twitch.tv/app/",
                    streamKey = "live_98374241_fka982q4p98ashfkhaskjhas",
                    username = "StreamerChannel",
                    isActive = true,
                    latencyMode = "Low"
                ),
                StreamingPlatformEntity(
                    name = "YouTube",
                    streamUrl = "rtmp://a.rtmp.youtube.com/live2",
                    streamKey = "abcd-efgh-ijkl-mnop-qrst",
                    username = "StreamerStudio",
                    isActive = true,
                    latencyMode = "Normal"
                ),
                StreamingPlatformEntity(
                    name = "Kick",
                    streamUrl = "rtmp://live.kick.com/app/",
                    streamKey = "sk_us_live_8321dafae904b7bcaaa",
                    username = "StreamerKick",
                    isActive = true,
                    latencyMode = "Low"
                ),
                StreamingPlatformEntity(
                    name = "Facebook Live",
                    streamUrl = "rtmps://live-api-s.facebook.com:443/rtmp/",
                    streamKey = "FB-1234567890-a1b2c3d4e5",
                    username = "StreamerFB",
                    isActive = false,
                    latencyMode = "Normal"
                )
            )
            for (platform in defaults) {
                dao.insertPlatform(platform)
            }
        }
    }
}
