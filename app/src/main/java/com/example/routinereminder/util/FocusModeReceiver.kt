package com.example.routinereminder.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class FocusModeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Notification policy access is missing; cannot change focus mode")
            return
        }

        when (intent.action) {
            ACTION_ENABLE_FOCUS -> enableFocusMode(context, manager)
            ACTION_DISABLE_FOCUS -> disableFocusMode(context, manager)
        }
    }

    private fun enableFocusMode(context: Context, manager: NotificationManager) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeCount = prefs.getInt(KEY_ACTIVE_COUNT, 0)
        if (activeCount == 0) {
            prefs.edit().putInt(KEY_PREVIOUS_FILTER, manager.currentInterruptionFilter).apply()
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    @Suppress("DEPRECATION")
                    manager.setNotificationPolicy(
                        NotificationManager.Policy(
                            0,
                            0,
                            0,
                            0
                        )
                    )
                }
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }.onFailure {
                Log.w(TAG, "Failed to enable focus mode", it)
            }
        }
        prefs.edit().putInt(KEY_ACTIVE_COUNT, activeCount + 1).apply()
    }

    private fun disableFocusMode(context: Context, manager: NotificationManager) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeCount = prefs.getInt(KEY_ACTIVE_COUNT, 0)
        if (activeCount <= 1) {
            val previousFilter = prefs.getInt(
                KEY_PREVIOUS_FILTER,
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            runCatching {
                manager.setInterruptionFilter(previousFilter)
            }.onFailure {
                Log.w(TAG, "Failed to restore interruption filter", it)
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            prefs.edit().remove(KEY_PREVIOUS_FILTER).putInt(KEY_ACTIVE_COUNT, 0).apply()
        } else {
            prefs.edit().putInt(KEY_ACTIVE_COUNT, activeCount - 1).apply()
        }
    }

    companion object {
        const val ACTION_ENABLE_FOCUS = "com.example.routinereminder.action.ENABLE_FOCUS"
        const val ACTION_DISABLE_FOCUS = "com.example.routinereminder.action.DISABLE_FOCUS"

        private const val TAG = "FocusModeReceiver"
        private const val PREFS_NAME = "focus_mode_state"
        private const val KEY_ACTIVE_COUNT = "active_count"
        private const val KEY_PREVIOUS_FILTER = "previous_filter"
    }
}
