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

sealed class ScheduleResult {
    object Success : ScheduleResult()
    object ExactAlarmPermissionNeeded : ScheduleResult()
    object Error : ScheduleResult()
}

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(item: ScheduleItem): ScheduleResult {
        Log.i("AlarmScheduler", "Attempting to schedule item id: ${item.id}, notifyEnabled: ${item.notifyEnabled}")

        if (!item.notifyEnabled) {
            cancel(item.id)
            Log.w("AlarmScheduler", "Notifications disabled for item id: ${item.id}. Scheduling skipped, permission check bypassed.")
            return ScheduleResult.Success
        }

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

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_ITEM_ID, item.id)
            putExtra(NotificationReceiver.EXTRA_ITEM_TITLE, item.name)
            putExtra(NotificationReceiver.EXTRA_ITEM_NOTES, item.notes)
            putExtra(NotificationReceiver.EXTRA_ITEM_SHOW_DETAILS, item.showDetailsInNotification)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextOccurrenceMillis,
                pendingIntent
            )
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
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i("AlarmScheduler", "Canceled alarm for item id: $itemId")
        } else {
            Log.i("AlarmScheduler", "No alarm found to cancel for item id: $itemId")
        }
    }
}
