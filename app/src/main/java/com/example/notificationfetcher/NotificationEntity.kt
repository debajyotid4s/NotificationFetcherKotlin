package com.example.notificationfetcher

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a single captured notification.
 * Each row in the "notifications" table corresponds to one notification event.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(

    /** Auto-generated unique ID for each row */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Unix timestamp (ms) when the notification was posted */
    val postedAt: Long,

    /** Package name of the app that posted the notification (e.g. com.whatsapp) */
    val packageName: String,

    /** Human-readable app name resolved from packageName (e.g. "WhatsApp") */
    val appName: String,

    /** Notification title — can be null if not provided by the app */
    val title: String?,

    /** Short notification body text */
    val text: String?,

    /** Expanded big text (e.g. full email body) — null if not provided */
    val bigText: String?,

    /** Sub-text shown below the main text — null if not provided */
    val subText: String?,

    /** Notification category (e.g. "msg", "email", "alarm") — null if unset */
    val category: String?,

    /** Notification channel ID — null on Android < 8.0 */
    val channelId: String?,

    /** Unique system key for this notification (used for deduplication) */
    val notificationKey: String,

    /** True if this is an ongoing notification (e.g. music player, call) */
    val isOngoing: Boolean,

    /** True if this is a group summary notification (header of a bundle) */
    val isGroupSummary: Boolean,

    /**
     * Importance level from the RankingMap (0=NONE … 4=HIGH … 5=MAX).
     * Null if ranking was unavailable at capture time.
     */
    val importanceHint: Int?
)