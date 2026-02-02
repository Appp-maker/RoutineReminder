package com.example.routinereminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.util.RecurrenceUtil
import java.time.ZoneId
import kotlin.math.abs

sealed class ScheduleResult {
    object Success : ScheduleResult()
    object ExactAlarmPermissionNeeded : ScheduleResult()
    object Error : ScheduleResult()
}

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val maxPreReminders = 10

    fun schedule(item: ScheduleItem): ScheduleResult {
        Log.i("AlarmScheduler", "Attempting to schedule item id: ${item.id}, notifyEnabled: ${item.notifyEnabled}")

        if (!item.notifyEnabled) {
            cancel(item.id)
            Log.w("AlarmScheduler", "Notifications disabled for item id: ${item.id}. Scheduling skipped, permission check bypassed.")
            return ScheduleResult.Success
        }

        cancel(item.id)

        val nextOccurrenceMillis = RecurrenceUtil.nextOccurrenceMillis(item, java.time.ZonedDateTime.now(ZoneId.systemDefault()))

        if (nextOccurrenceMillis == null) {
            Log.w("AlarmScheduler", "Next occurrence is null for item id: ${item.id}, not scheduling.")
            return ScheduleResult.Success
        }

        Log.i("AlarmScheduler", "Device API level: ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Log.i("AlarmScheduler", "Runtime check: alarmManager.canScheduleExactAlarms() returned: $canSchedule for item id: ${item.id}")
            if (!canSchedule) {
                Log.w("AlarmScheduler", "Exact alarm permission NOT granted. Returning ExactAlarmPermissionNeeded for item id: ${item.id}")
                return ScheduleResult.ExactAlarmPermissionNeeded
            }
            Log.i("AlarmScheduler", "Exact alarm permission IS granted according to API. Proceeding to schedule item id: ${item.id}")
        } else {
            Log.i("AlarmScheduler", "Device API level ${Build.VERSION.SDK_INT} is less than S (31). No explicit runtime permission check needed for SCHEDULE_EXACT_ALARM. Proceeding to schedule item id: ${item.id}")
        }

        return try {
            val reminderCount = item.reminderCount.coerceIn(0, maxPreReminders)
            val reminderIntervalMinutes = item.reminderIntervalMinutes.coerceAtLeast(1)

            for (index in 0..reminderCount) {
                val minutesBefore = index * reminderIntervalMinutes
                val reminderMillis = nextOccurrenceMillis - (minutesBefore * 60_000L)
                if (reminderMillis <= System.currentTimeMillis()) continue

                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra(NotificationReceiver.EXTRA_ITEM_ID, item.id)
                    putExtra(NotificationReceiver.EXTRA_ITEM_TITLE, item.name)
                    putExtra(NotificationReceiver.EXTRA_ITEM_NOTES, item.notes)
                    putExtra(NotificationReceiver.EXTRA_ITEM_SHOW_DETAILS, item.showDetailsInNotification)
                    putExtra(NotificationReceiver.EXTRA_REMINDER_MINUTES_BEFORE, minutesBefore)
                    putExtra(NotificationReceiver.EXTRA_SHOULD_RESCHEDULE, index == 0)
                    putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, stableId(makeRequestCode(item.id, index)))
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    makeRequestCode(item.id, index),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderMillis,
                    pendingIntent
                )
            }

            Log.i("AlarmScheduler", "Alarm scheduled successfully for item id: ${item.id} at $nextOccurrenceMillis")
            ScheduleResult.Success
        } catch (se: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException while scheduling alarm for item id: ${item.id}. This can happen if permission was revoked or other restrictions apply.", se)
            // Even if canScheduleExactAlarms was true, a SecurityException can still occur.
            // If API S+, this might indicate a deeper issue or a very recent permission change.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                 Log.e("AlarmScheduler", "SecurityException encountered AND canScheduleExactAlarms is now false. Returning ExactAlarmPermissionNeeded.")
                return ScheduleResult.ExactAlarmPermissionNeeded
            }
            return ScheduleResult.Error
        }
    }

    fun cancel(itemId: Long) {
        val intent = Intent(context, NotificationReceiver::class.java)
        var canceledAny = false
        for (index in 0..maxPreReminders) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                makeRequestCode(itemId, index),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                canceledAny = true
            }
        }
        if (canceledAny) {
            Log.i("AlarmScheduler", "Canceled alarms for item id: $itemId")
        } else {
            Log.i("AlarmScheduler", "No alarms found to cancel for item id: $itemId")
        }
    }

    private fun makeRequestCode(itemId: Long, index: Int): Int {
        return "$itemId-$index".hashCode()
    }

    private fun stableId(value: Int): Int {
        return if (value == Int.MIN_VALUE) 0 else kotlin.math.abs(value)
    }
}
