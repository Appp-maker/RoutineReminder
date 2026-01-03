package com.example.routinereminder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.routinereminder.data.exercisedb.ExerciseDbExercise
import com.example.routinereminder.data.exercisedb.ExerciseDbRepository
import com.example.routinereminder.data.workout.WorkoutPlan
import com.example.routinereminder.workers.ExerciseDbDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject


data class WorkoutUiState(
    val plans: List<WorkoutPlan> = emptyList(),
    val selectedPlanId: String? = null,
    val exercises: List<ExerciseDbExercise> = emptyList(),
    val bodyParts: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedBodyPart: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showRefreshPrompt: Boolean = false,
    val isExerciseDbReady: Boolean = false,
    val isExerciseDbDownloading: Boolean = false,
    val exerciseDbDownloadedCount: Int = 0,
    val exerciseDbTotalCount: Int? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ExerciseDbRepository
) : ViewModel() {
    private val requiredBodyParts = listOf(
        "back",
        "cardio",
        "chest",
        "lower arms",
        "lower legs",
        "neck",
        "shoulders",
        "upper arms",
        "upper legs",
        "waist"
    )

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var progressJob: Job? = null

    init {
        initializeExerciseDatabase()
        checkRefreshPrompt()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        scheduleRefresh()
    }

    fun selectBodyPart(bodyPart: String?) {
        _uiState.update { it.copy(selectedBodyPart = bodyPart) }
        scheduleRefresh()
    }

    fun refreshExercises() {
        viewModelScope.launch {
            if (!_uiState.value.isExerciseDbReady) return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.fetchExercises(
                query = _uiState.value.searchQuery,
                bodyPart = _uiState.value.selectedBodyPart
            )
            result
                .onSuccess { exercises ->
                    _uiState.update { it.copy(exercises = exercises, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load exercises.") }
                }
        }
    }

    fun refreshExerciseDatabase() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showRefreshPrompt = false,
                    errorMessage = null,
                    isExerciseDbReady = false,
                    isExerciseDbDownloading = true,
                    exerciseDbDownloadedCount = 0,
                    exerciseDbTotalCount = null
                )
            }
            val resetResult = runCatching { repository.resetExerciseDatabaseCache() }
            if (resetResult.isFailure) {
                val error = resetResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isExerciseDbReady = false,
                        isExerciseDbDownloading = false,
                        errorMessage = error?.message ?: "Unable to refresh ExerciseDB data."
                    )
                }
                return@launch
            }
            enqueueDownloadWork()
            monitorDownloadProgress()
        }
    }

    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(400)
            refreshExercises()
        }
    }

    fun refreshBodyParts() {
        viewModelScope.launch {
            if (!_uiState.value.isExerciseDbReady) return@launch
            repository.fetchBodyParts()
                .onSuccess { bodyParts ->
                    val mergedBodyParts = (bodyParts + requiredBodyParts).distinct().sorted()
                    _uiState.update { it.copy(bodyParts = mergedBodyParts) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            bodyParts = requiredBodyParts.sorted(),
                            errorMessage = error.message ?: "Unable to load body parts."
                        )
                    }
                }
        }
    }

    private fun checkRefreshPrompt() {
        viewModelScope.launch {
            if (repository.shouldPromptForRefresh()) {
                repository.recordRefreshPromptShown()
                _uiState.update { it.copy(showRefreshPrompt = true) }
            }
        }
    }

    private fun initializeExerciseDatabase() {
        viewModelScope.launch {
            val progress = repository.getDownloadProgress()
            if (progress.isComplete) {
                _uiState.update {
                    it.copy(
                        isExerciseDbReady = true,
                        isExerciseDbDownloading = false,
                        exerciseDbDownloadedCount = progress.downloadedCount,
                        exerciseDbTotalCount = progress.totalCount ?: progress.downloadedCount
                    )
                }
                refreshBodyParts()
                refreshExercises()
            } else {
                _uiState.update {
                    it.copy(
                        isExerciseDbReady = false,
                        isExerciseDbDownloading = true,
                        exerciseDbDownloadedCount = progress.downloadedCount,
                        exerciseDbTotalCount = progress.totalCount
                    )
                }
                enqueueDownloadWork()
                monitorDownloadProgress()
            }
        }
    }

    private fun enqueueDownloadWork() {
        val workRequest = OneTimeWorkRequestBuilder<ExerciseDbDownloadWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ExerciseDbDownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun monitorDownloadProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val progress = repository.getDownloadProgress()
                _uiState.update {
                    it.copy(
                        isExerciseDbReady = progress.isComplete,
                        isExerciseDbDownloading = !progress.isComplete,
                        exerciseDbDownloadedCount = progress.downloadedCount,
                        exerciseDbTotalCount = progress.totalCount
                    )
                }
                if (progress.isComplete) {
                    refreshBodyParts()
                    refreshExercises()
                    break
                }
                delay(1000)
            }
        }
    }

    fun dismissRefreshPrompt() {
        viewModelScope.launch {
            repository.recordRefreshPromptDismissed()
            _uiState.update { it.copy(showRefreshPrompt = false) }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun createPlan(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Workout plan name cannot be empty.") }
            return false
        }
        if (_uiState.value.plans.any { it.name.equals(trimmed, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "A workout plan with that name already exists.") }
            return false
        }
        val newPlan = WorkoutPlan(id = UUID.randomUUID().toString(), name = trimmed)
        _uiState.update {
            it.copy(
                plans = it.plans + newPlan,
                selectedPlanId = newPlan.id,
                errorMessage = null
            )
        }
        return true
    }

    fun selectPlan(planId: String?) {
        _uiState.update { it.copy(selectedPlanId = planId) }
    }

    fun removePlan(planId: String) {
        _uiState.update { state ->
            val updatedPlans = state.plans.filterNot { it.id == planId }
            val updatedSelected = if (state.selectedPlanId == planId) updatedPlans.firstOrNull()?.id else state.selectedPlanId
            state.copy(plans = updatedPlans, selectedPlanId = updatedSelected)
        }
    }

    fun addExerciseToPlan(planId: String, exercise: ExerciseDbExercise) {
        _uiState.update { state ->
            val updatedPlans = state.plans.map { plan ->
                if (plan.id == planId) {
                    if (plan.exercises.any { it.id == exercise.id }) {
                        plan
                    } else {
                        plan.copy(exercises = plan.exercises + exercise)
                    }
                } else {
                    plan
                }
            }
            state.copy(plans = updatedPlans)
        }
    }

    fun removeExerciseFromPlan(planId: String, exerciseId: String) {
        _uiState.update { state ->
            val updatedPlans = state.plans.map { plan ->
                if (plan.id == planId) {
                    plan.copy(exercises = plan.exercises.filterNot { it.id == exerciseId })
                } else {
                    plan
                }
            }
            state.copy(plans = updatedPlans)
        }
    }

    private fun filterExercises(
        exercises: List<ExerciseDbExercise>,
        query: String,
        bodyPart: String?
    ): List<ExerciseDbExercise> {
        val trimmedQuery = query.trim().lowercase()
        val normalizedBodyPart = bodyPart?.takeIf { it.isNotBlank() }
        return exercises.filter { exercise ->
            val matchesQuery = trimmedQuery.isBlank() || exercise.name.lowercase().contains(trimmedQuery)
            val matchesBodyPart = normalizedBodyPart.isNullOrBlank() ||
                exercise.bodyPart.equals(normalizedBodyPart, ignoreCase = true)
            matchesQuery && matchesBodyPart
        }
    }
}
