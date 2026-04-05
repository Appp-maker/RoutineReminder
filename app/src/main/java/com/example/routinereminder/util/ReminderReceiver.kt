package com.example.routinereminder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.example.routinereminder.MainActivity
import com.example.routinereminder.R
import com.example.routinereminder.data.DefaultEventSettings
import kotlinx.serialization.json.Json
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

private fun loadSettings(context: Context): DefaultEventSettings? {
    return try {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val json = prefs.getString("default_event_settings", null)
        if (json != null) Json.decodeFromString<DefaultEventSettings>(json) else null
    } catch (e: Exception) {
        null
    }
}


class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        private const val CHANNEL_ID = "routine_channel"
        private const val CHANNEL_NAME = "Routine Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val showDetails = intent.getBooleanExtra("showDetails", true)


        // If CLOSE button tapped
        if (intent.action == "CLOSE_NOTIFICATION") {
            val id = intent.getIntExtra("notificationId", -1)
            if (id != -1) {
                NotificationManagerCompat.from(context).cancel(id)
            }
            return
        }

        // Normal reminder trigger
        val title = intent.getStringExtra("title") ?: "Reminder"
        val notes = intent.getStringExtra("notes") ?: ""

        // Build text: notes only, no duration, no repeats
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 0)
        val message =
            if (showDetails && notes.isNotBlank()) notes
            else if (minutesBefore > 0) {
                context.getString(R.string.notification_event_starts_in, minutesBefore)
            } else {
                context.getString(R.string.notification_event_starting_now)
            }

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, System.currentTimeMillis().toInt())

        // Close button
        val closeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "CLOSE_NOTIFICATION"
            putExtra("notificationId", notificationId)
        }

        val closePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        ensureReminderChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("ReminderReceiver", "Skipping notification: POST_NOTIFICATIONS not granted")
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .extend(
                NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true)
            )
            .addAction(0, "Close", closePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled routines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}

