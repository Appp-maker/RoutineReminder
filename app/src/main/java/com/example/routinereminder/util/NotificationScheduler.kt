package com.example.routinereminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.routinereminder.data.ScheduleItem
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val maxPreReminders = 10

    fun scheduleSingleOccurrence(item: ScheduleItem, epochDay: Long) {
        cancelSingleOccurrence(item, epochDay)
        val triggerDateTime = LocalDate.ofEpochDay(epochDay)
            .atTime(item.hour, item.minute)

        val triggerMillis = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) {
            return
        }

        val reminderCount = item.reminderCount.coerceIn(0, maxPreReminders)
        val reminderIntervalMinutes = item.reminderIntervalMinutes.coerceAtLeast(1)
        val totalReminders = reminderCount.coerceAtLeast(0)

        for (index in 0..totalReminders) {
            val minutesBefore = index * reminderIntervalMinutes
            val reminderMillis = triggerMillis - (minutesBefore * 60_000L)
            if (reminderMillis <= System.currentTimeMillis()) continue

            val notificationId = stableId(makeRequestCode(item.id, epochDay, index))

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("title", item.name)
                putExtra("notes", item.notes ?: "")
                putExtra("duration", item.durationMinutes)
                putExtra("repeat", item.repeatEveryWeeks)
                putExtra("scheduleId", item.id)
                putExtra("epochDay", epochDay)
                putExtra("title", item.name)
                putExtra("showDetails", item.showDetailsInNotification)
                putExtra(ReminderReceiver.EXTRA_MINUTES_BEFORE, minutesBefore)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(item.id, epochDay, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderMillis,
                pendingIntent
            )
        }
    }

    fun cancelSingleOccurrence(item: ScheduleItem, epochDay: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        for (index in 0..maxPreReminders) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(item.id, epochDay, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun makeRequestCode(itemId: Long, epochDay: Long, index: Int): Int {
        return "$itemId-$epochDay-$index".hashCode()
    }

    private fun stableId(value: Int): Int {
        return if (value == Int.MIN_VALUE) 0 else kotlin.math.abs(value)
    }
}
