package com.example.routinereminder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.routinereminder.workers.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class RoutineReminderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize MapLibre globally before any MapView is created
        try {
            // This works on all SDK versions — no API key, no enum needed
            MapLibre.getInstance(applicationContext)
        } catch (_: Exception) {
            // Ignore if already initialized
        }
        createNotificationChannel()

        // Keep existing WorkManager setup
        setupRecurringWork()
    }
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "routine_channel",
                "Routine Reminders",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for scheduled routines"

            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupRecurringWork() {
        val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SyncWorker::class.java.name,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}
