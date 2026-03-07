package com.example.notificationfetcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed — notification listener awareness restored.")
            // The NotificationListenerService is automatically re-bound by the system
            // after BOOT_COMPLETED when the user has granted notification access.
            // This receiver ensures the app is awoken so the system can rebind the service.
        }
    }
}