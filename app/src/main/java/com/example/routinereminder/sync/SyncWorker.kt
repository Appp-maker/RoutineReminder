package com.example.routinereminder.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.routinereminder.data.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "RoutineReminderSyncWorker"
    }

    override suspend fun doWork(): Result {
        val syncManager = SyncManager(applicationContext, database)

        println("SyncWorker: Attempting periodic sync - NOT FULLY IMPLEMENTED (Google Sign-In check needed).")

        return Result.success()
    }
}
