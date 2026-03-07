package com.example.notificationfetcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that fires when the device finishes booting.
 *
 * Important note:
 * NotificationListenerService is re-bound AUTOMATICALLY by Android after reboot
 * if the user previously granted notification access — so we don't need to
 * manually start the service here.
 *
 * This receiver's purpose is to:
 *  1. Log that the device rebooted (useful for debugging gaps in notification history)
 *  2. Ensure the notification channel is recreated (needed before any notification is shown)
 *  3. Can be extended later to show a reminder if permission was revoked
 */
class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Device booted — ensuring notification channel exists")

                // Re-create the notification channel in case it was cleared
                // (channels persist across reboots, but this is a safe no-op call)
                NotificationChannelHelper.createChannel(context)

                // The NotificationListenerService will be re-bound automatically
                // by Android if permission is still granted — no manual start needed.
                Log.d(TAG, "Boot setup complete. Waiting for system to rebind listener service.")
            }
        }
    }
}