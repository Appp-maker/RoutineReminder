package com.example.routinereminder.util

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Registers the app in Android's "Notification access" list.
 *
 * This enables users to grant notification-listener access so companion apps/devices
 * (including smartwatch ecosystems) can work with app notifications more reliably.
 */
class AppNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // Intentionally no-op for now; service is present so users can grant
        // notification access from system settings when needed.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val TAG = "AppNotificationListener"
    }
}
