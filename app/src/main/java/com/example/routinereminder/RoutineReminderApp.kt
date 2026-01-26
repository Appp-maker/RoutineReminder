package com.example.routinereminder

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.data.ExampleDataSeeder
import com.example.routinereminder.workers.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class RoutineReminderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var exampleDataSeeder: ExampleDataSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        configureImageLoader()

        // Keep existing WorkManager setup
        applicationScope.launch {
            setupRecurringWork()
        }

        applicationScope.launch {
            exampleDataSeeder.seedIfNeeded()
        }

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

    private fun configureImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private suspend fun setupRecurringWork() {
        val isCloudSyncEnabled = settingsRepository.getCloudSyncEnabled().first()
        if (!isCloudSyncEnabled) {
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork(SyncWorker::class.java.name)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SyncWorker::class.java.name,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}
