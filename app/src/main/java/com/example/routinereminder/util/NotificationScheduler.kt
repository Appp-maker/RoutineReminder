package com.example.routinereminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import android.content.Context
import android.content.Intent
import com.example.routinereminder.data.ScheduleItem
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class NotificationScheduler(private val context: Context) {

    companion object {
        private const val TAG = "NotificationScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val maxPreReminders = 10
    private val timerCompletionIndex = maxPreReminders + 2
    private val focusStartIndex = maxPreReminders + 3
    private val focusEndIndex = maxPreReminders + 4

    fun scheduleSingleOccurrence(
        item: ScheduleItem,
        epochDay: Long,
        departureLeadMinutes: Int = 0
    ) {
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
        val travelLead = departureLeadMinutes.coerceAtLeast(0)
        val scheduledMinutesBefore = mutableSetOf<Int>()

        for (index in 0..totalReminders) {
            val minutesBefore = index * reminderIntervalMinutes
            val reminderMillis = triggerMillis - (minutesBefore * 60_000L)
            if (reminderMillis <= System.currentTimeMillis()) continue
            scheduledMinutesBefore.add(minutesBefore)

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

            scheduleAlarm(reminderMillis, pendingIntent)
        }

        if (travelLead > 0 && travelLead !in scheduledMinutesBefore) {
            val reminderMillis = triggerMillis - (travelLead * 60_000L)
            if (reminderMillis > System.currentTimeMillis()) {
                val extraIndex = maxPreReminders + 1
                val notificationId = stableId(makeRequestCode(item.id, epochDay, extraIndex))

                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("title", item.name)
                    putExtra("notes", item.notes ?: "")
                    putExtra("duration", item.durationMinutes)
                    putExtra("repeat", item.repeatEveryWeeks)
                    putExtra("scheduleId", item.id)
                    putExtra("epochDay", epochDay)
                    putExtra("title", item.name)
                    putExtra("showDetails", item.showDetailsInNotification)
                    putExtra(ReminderReceiver.EXTRA_MINUTES_BEFORE, travelLead)
                    putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    makeRequestCode(item.id, epochDay, extraIndex),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleAlarm(reminderMillis, pendingIntent)
            }
        }

        if (item.autoTimerEnabled && (item.autoTimerAlertNotification || item.autoTimerAlertSound)) {
            val timerDurationMinutes = if (item.autoTimerUseEventDuration) {
                item.durationMinutes
            } else {
                item.autoTimerCustomMinutes
            }.coerceAtLeast(1)
            val timerTriggerMillis = triggerMillis + (timerDurationMinutes * 60_000L)
            if (timerTriggerMillis > System.currentTimeMillis()) {
                val notificationId = stableId(makeRequestCode(item.id, epochDay, timerCompletionIndex))
                val timerIntent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_TIMER_COMPLETED
                    putExtra("title", item.name)
                    putExtra("scheduleId", item.id)
                    putExtra("epochDay", epochDay)
                    putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(ReminderReceiver.EXTRA_TIMER_ALERT_SOUND, item.autoTimerAlertSound)
                    putExtra(
                        ReminderReceiver.EXTRA_TIMER_ALERT_NOTIFICATION,
                        item.autoTimerAlertNotification
                    )
                }
                val timerPendingIntent = PendingIntent.getBroadcast(
                    context,
                    makeRequestCode(item.id, epochDay, timerCompletionIndex),
                    timerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleAlarm(timerTriggerMillis, timerPendingIntent)
            }
        }

        if (item.focusModeEnabled) {
            val focusStartIntent = Intent(context, FocusModeReceiver::class.java).apply {
                action = FocusModeReceiver.ACTION_ENABLE_FOCUS
            }
            val focusStartPendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(item.id, epochDay, focusStartIndex),
                focusStartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(triggerMillis, focusStartPendingIntent)

            val focusEndMillis = triggerMillis + (item.durationMinutes.coerceAtLeast(1) * 60_000L)
            if (focusEndMillis > System.currentTimeMillis()) {
                val focusEndIntent = Intent(context, FocusModeReceiver::class.java).apply {
                    action = FocusModeReceiver.ACTION_DISABLE_FOCUS
                }
                val focusEndPendingIntent = PendingIntent.getBroadcast(
                    context,
                    makeRequestCode(item.id, epochDay, focusEndIndex),
                    focusEndIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleAlarm(focusEndMillis, focusEndPendingIntent)
            }
        }
    }


    private fun scheduleAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                Log.w(
                    TAG,
                    "Exact alarm permission missing; scheduling inexact alarm fallback at $triggerAtMillis"
                )
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (securityException: SecurityException) {
            Log.w(
                TAG,
                "Exact alarm scheduling threw SecurityException; retrying with inexact fallback",
                securityException
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelSingleOccurrence(item: ScheduleItem, epochDay: Long) {
        for (index in 0..focusEndIndex) {
            val receiverClass = if (index == focusStartIndex || index == focusEndIndex) {
                FocusModeReceiver::class.java
            } else {
                ReminderReceiver::class.java
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(item.id, epochDay, index),
                Intent(context, receiverClass),
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
