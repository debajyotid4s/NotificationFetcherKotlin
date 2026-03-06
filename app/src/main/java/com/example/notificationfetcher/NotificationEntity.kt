package com.example.notificationfetcher

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postedAt: Long,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val category: String?,
    val channelId: String?,
    val notificationKey: String,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val importanceHint: Int?,
    val label: Int? = null
)