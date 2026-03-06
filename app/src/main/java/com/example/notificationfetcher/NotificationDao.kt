package com.example.notificationfetcher

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC LIMIT 1")
    suspend fun getLastNotification(): NotificationEntity?

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    suspend fun getAllNotificationsSync(): List<NotificationEntity>
}