package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: Int): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :newPasswordHash WHERE email = :email")
    suspend fun resetPassword(email: String, newPasswordHash: String): Int
}
