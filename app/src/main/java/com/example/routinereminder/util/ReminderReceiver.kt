package com.example.routinereminder.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.routinereminder.R
import com.example.routinereminder.data.DefaultEventSettings
import kotlinx.serialization.json.Json

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

        val notification = NotificationCompat.Builder(context, "routine_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Close", closePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
