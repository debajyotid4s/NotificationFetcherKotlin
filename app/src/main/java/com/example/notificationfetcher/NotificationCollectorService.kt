package com.example.notificationfetcher

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
}