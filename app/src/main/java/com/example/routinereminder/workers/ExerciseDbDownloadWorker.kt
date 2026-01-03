package com.example.routinereminder.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.data.exercisedb.ExerciseDbRepository
import com.example.routinereminder.ui.AppTab
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ExerciseDbDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ExerciseDbRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val enabledTabs = settingsRepository.getEnabledTabs().first()
        val workoutEnabled = enabledTabs?.contains(AppTab.Workout.id) ?: true
        if (!workoutEnabled) {
            return Result.success()
        }
        return repository
            .downloadExerciseDatabase(shouldContinue = { shouldContinueDownload() })
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
    }

    private suspend fun shouldContinueDownload(): Boolean {
        if (isStopped) return false
        val enabledTabs = settingsRepository.getEnabledTabs().first()
        return enabledTabs?.contains(AppTab.Workout.id) ?: true
    }

    companion object {
        const val UNIQUE_WORK_NAME = "exercise_db_download"
    }
}
