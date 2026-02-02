package com.example.routinereminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.routinereminder.MainActivity

import com.example.routinereminder.R
import com.example.routinereminder.data.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TITLE = "extra_item_title"
        const val EXTRA_ITEM_NOTES = "extra_item_notes"
        const val EXTRA_ITEM_SHOW_DETAILS = "extra_item_show_details"
        const val EXTRA_REMINDER_MINUTES_BEFORE = "extra_reminder_minutes_before"
        const val EXTRA_SHOULD_RESCHEDULE = "extra_should_reschedule"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        private const val CHANNEL_ID = "routine_reminders_channel"
        private const val CHANNEL_NAME = "Routine Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId == -1L) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val item = scheduleRepository.getTaskById(itemId)

            item?.let {
                if (!it.notifyEnabled) return@launch

                val minutesBefore = intent.getIntExtra(EXTRA_REMINDER_MINUTES_BEFORE, 0)
                val notificationTitle = it.name
                val notificationText = when {
                    it.showDetailsInNotification && it.notes?.isNotBlank() == true -> it.notes
                    minutesBefore > 0 -> context.getString(R.string.notification_event_starts_in, minutesBefore)
                    else -> context.getString(R.string.notification_event_starting_now)
                }

                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, itemId.toInt())
                val pendingActivityIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingActivityIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(notificationId, notification)

                val shouldReschedule = intent.getBooleanExtra(EXTRA_SHOULD_RESCHEDULE, true)
                if (shouldReschedule && !it.isOneTime) {
                    val alarmScheduler = AlarmScheduler(context.applicationContext)
                    alarmScheduler.schedule(it)
                }
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für Routinen und Termine"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
