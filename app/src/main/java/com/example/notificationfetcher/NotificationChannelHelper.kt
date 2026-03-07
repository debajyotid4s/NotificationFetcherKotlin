package com.example.notificationfetcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Helper object to create the notification channel required for the foreground service.
 *
 * On Android 8.0 (Oreo, API 26) and above, every notification MUST belong to a channel.
 * Channels must be created before posting any notification — calling this in
 * MainActivity.onCreate() and NotificationCollectorService.onCreate() is safe
 * because createNotificationChannel() is idempotent (safe to call multiple times).
 */
object NotificationChannelHelper {

    /** Channel ID used when building the foreground service notification */
    const val CHANNEL_ID = "notification_collector_channel"

    /**
     * Creates the notification channel for the foreground service.
     * Must be called before startForeground() is invoked.
     * Safe to call multiple times — Android ignores duplicate channel creation.
     *
     * @param context Any valid context (Application, Activity, or Service)
     */
    fun createChannel(context: Context) {
        // Channels are only required on Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Collector", // Visible name in system Settings > Notifications
                NotificationManager.IMPORTANCE_MIN  // IMPORTANCE_MIN = silent, no pop-up, no sound
            ).apply {
                description = "Silent background service that captures notification metadata"
                setShowBadge(false)      // Don't show a badge dot on the launcher icon
                enableLights(false)      // No LED flash
                enableVibration(false)   // No vibration
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}