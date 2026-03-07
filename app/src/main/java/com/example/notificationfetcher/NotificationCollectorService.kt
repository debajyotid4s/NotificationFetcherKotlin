package com.example.notificationfetcher

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationCollectorService : NotificationListenerService() {
    private val TAG = "NotificationCollector"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        NotificationChannelHelper.createChannel(this)
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val postedAt = sbn.postTime

        serviceScope.launch {
            val dao = database.notificationDao()
            val last = dao.getLastNotification()

            // Deduplication logic: skip if same package + title + text within 5 seconds
            if (last != null &&
                last.packageName == packageName &&
                last.title == title &&
                last.text == text &&
                (postedAt - last.postedAt) < 5000
            ) {
                Log.d(TAG, "Deduplicated: $packageName - $title")
                return@launch
            }

            val appName = try {
                val pm = packageManager
                val ai = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }

            val entity = NotificationEntity(
                postedAt = postedAt,
                packageName = packageName,
                appName = appName,
                title = title,
                text = text,
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                category = sbn.notification.category,
                channelId = sbn.notification.channelId,
                notificationKey = sbn.key,
                isOngoing = sbn.isOngoing,
                isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
                importanceHint = null // Deprecated or requires RankingMap
            )

            dao.insert(entity)
            Log.d(TAG, "Inserted: $appName - $title: $text")
        }
    }

    private fun buildForegroundNotification(): android.app.Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationChannelHelper.CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_service_running))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }
}