package com.example.routinereminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.routinereminder.data.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val alarmScheduler = AlarmScheduler(context.applicationContext)
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                try {
                    val tasks = scheduleRepository.getAllTasks().first()
                    tasks.forEach { item ->
                        if (item.notifyEnabled) {
                            alarmScheduler.schedule(item)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
