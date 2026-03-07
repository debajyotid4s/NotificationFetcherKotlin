package com.example.notificationfetcher

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the notifications table.
 * All database operations go through this interface.
 * Room generates the implementation at compile time via KSP.
 */
@Dao
interface NotificationDao {

    /**
     * Insert a new notification record into the database.
     * IGNORE strategy means if a duplicate primary key somehow occurs, it's silently skipped.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity)

    /**
     * Observe all notifications in real-time, newest first.
     * Returns a Flow — emits a new list every time the table changes.
     * Use this in a ViewModel/UI to reactively update a list.
     */
    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    /**
     * Get the single most recently inserted notification.
     * Used by the deduplication logic in NotificationCollectorService.
     * Returns null if the table is empty.
     */
    @Query("SELECT * FROM notifications ORDER BY postedAt DESC LIMIT 1")
    suspend fun getLastNotification(): NotificationEntity?

    /**
     * Delete all rows from the notifications table.
     * Use this for a "Clear All" feature in the UI.
     */
    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    /**
     * Synchronously fetch all notifications as a plain List (not Flow).
     * Use this for one-shot operations like JSON export.
     */
    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    suspend fun getAllNotificationsSync(): List<NotificationEntity>
}