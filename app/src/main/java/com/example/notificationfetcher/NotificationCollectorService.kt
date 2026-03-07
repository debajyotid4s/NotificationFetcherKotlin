package com.example.notificationfetcher

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Core background service that listens for ALL notifications on the device.
 *
 * This extends NotificationListenerService — a special Android service that is
 * BOUND BY THE SYSTEM (not started by the app). This means:
 *  - It survives app process death
 *  - It is automatically reconnected by Android after crashes
 *  - It requires explicit user permission via Settings > Notification Access
 *
 * To keep it alive during Doze mode / phone idle, we promote it to a
 * Foreground Service using startForeground() with a silent, minimal notification.
 */
class NotificationCollectorService : NotificationListenerService() {

    private val TAG = "NotificationCollector"

    /**
     * SupervisorJob ensures that if one child coroutine fails,
     * it doesn't cancel all other running coroutines.
     * Dispatchers.IO routes all DB work off the main thread.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Room database instance — initialized once in onCreate() */
    private lateinit var database: AppDatabase

    // ─────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        // Initialize the database singleton
        database = AppDatabase.getDatabase(this)
        // Ensure the notification channel exists before calling startForeground()
        NotificationChannelHelper.createChannel(this)
    }

    /**
     * Called by Android when the listener is successfully connected.
     * This is the right place to call startForeground() — it guarantees
     * the service is truly active before we promote it.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected — starting foreground")
        startForeground(
            FOREGROUND_NOTIFICATION_ID,
            buildForegroundNotification()
        )
    }

    /**
     * Cancel all coroutines when the service is destroyed to prevent memory leaks.
     * This is called when the user revokes notification access permission.
     */
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed — coroutine scope cancelled")
    }

    // ─────────────────────────────────────────────
    // NOTIFICATION CAPTURE
    // ─────────────────────────────────────────────

    /**
     * Called by Android every time ANY app posts a notification.
     * Runs on the main thread — all heavy work is dispatched to IO via coroutines.
     *
     * @param sbn StatusBarNotification — the full notification object from Android
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Extract raw metadata from the notification bundle
        val packageName = sbn.packageName
        val extras      = sbn.notification.extras
        val title       = extras.getString(Notification.EXTRA_TITLE)
        val text        = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val postedAt    = sbn.postTime

        // Dispatch DB work to IO thread — never block the main thread
        serviceScope.launch {
            val dao  = database.notificationDao()
            val last = dao.getLastNotification()

            // ── Deduplication ──
            // Some apps re-post the same notification rapidly (e.g. updating progress).
            // Skip if same app + title + text was already saved within the last 5 seconds.
            if (last != null &&
                last.packageName == packageName &&
                last.title       == title &&
                last.text        == text &&
                (postedAt - last.postedAt) < 5_000L
            ) {
                Log.d(TAG, "Deduplicated: $packageName — $title")
                return@launch
            }

            // ── Resolve human-readable app name ──
            // packageManager.getApplicationLabel() gives "WhatsApp" from "com.whatsapp"
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                // Fallback to raw package name if resolution fails
                packageName
            }

            // ── Resolve notification importance from RankingMap ──
            // getRanking() populates a Ranking object with metadata about this notification
            val importance = try {
                val ranking = android.service.notification.NotificationListenerService.Ranking()
                if (currentRanking?.getRanking(sbn.key, ranking) == true) {
                    ranking.importance
                } else null
            } catch (e: Exception) {
                null // Gracefully handle any ranking API failures
            }

            // ── Build the entity and save to Room ──
            val entity = NotificationEntity(
                postedAt         = postedAt,
                packageName      = packageName,
                appName          = appName,
                title            = title,
                text             = text,
                bigText          = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                subText          = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                category         = sbn.notification.category,
                channelId        = sbn.notification.channelId,
                notificationKey  = sbn.key,
                isOngoing        = sbn.isOngoing,
                isGroupSummary   = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
                importanceHint   = importance
            )

            dao.insert(entity)
            Log.d(TAG, "Saved: [$appName] $title — $text")
        }
    }

    // ─────────────────────────────────────────────
    // FOREGROUND SERVICE NOTIFICATION
    // ─────────────────────────────────────────────

    /**
     * Builds the persistent (but silent and minimal) foreground notification.
     * This is required by Android to keep the service alive during idle/Doze mode.
     * IMPORTANCE_MIN + setSilent(true) makes it invisible in the status bar on most devices.
     */
    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationChannelHelper.CHANNEL_ID)
            .setContentTitle("Notification Collector")
            .setContentText("Running silently in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper small icon
            .setPriority(NotificationCompat.PRIORITY_MIN)    // Lowest priority — no heads-up
            .setSilent(true)                                 // No sound or vibration
            .setOngoing(true)                                // User cannot swipe it away
            .setShowWhen(false)                              // Don't show timestamp
            .build()
    }

    companion object {
        /** Notification ID for the foreground service notification — must be > 0 */
        private const val FOREGROUND_NOTIFICATION_ID = 1001
    }
}