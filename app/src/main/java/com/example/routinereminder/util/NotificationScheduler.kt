package com.example.routinereminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.routinereminder.data.ScheduleItem
import java.time.LocalDate
import java.time.ZoneId

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleSingleOccurrence(item: ScheduleItem, epochDay: Long) {
        val triggerDateTime = LocalDate.ofEpochDay(epochDay)
            .atTime(item.hour, item.minute)

        val triggerMillis = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", item.name)
            putExtra("notes", item.notes ?: "")
            putExtra("duration", item.durationMinutes)
            putExtra("repeat", item.repeatEveryWeeks)
            putExtra("scheduleId", item.id)
            putExtra("epochDay", epochDay)
            putExtra("title", item.name)
            putExtra("showDetails", item.showDetailsInNotification)

        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            makeRequestCode(epochDay, item.hour, item.minute),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun cancelSingleOccurrence(item: ScheduleItem, epochDay: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            makeRequestCode(epochDay, item.hour, item.minute),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    private fun makeRequestCode(epochDay: Long, hour: Int, minute: Int): Int {
        // unique per (date + time)
        val key = epochDay * 24 * 60 + (hour * 60 + minute)
        return key.hashCode()
    }
}
