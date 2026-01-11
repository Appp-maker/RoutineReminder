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
import com.example.routinereminder.data.workout.WorkoutPlanExercise
import com.example.routinereminder.data.workout.toPlanExercise
import com.example.routinereminder.data.workout.WorkoutPlanRepository
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
import java.io.File
import java.util.UUID
import javax.inject.Inject

private const val EXERCISE_DB_DOWNLOAD_MB = 6
private const val EXERCISE_DB_GIF_DOWNLOAD_MB = 85
private const val EXERCISE_DB_TOTAL_DOWNLOAD_MB = EXERCISE_DB_DOWNLOAD_MB + EXERCISE_DB_GIF_DOWNLOAD_MB


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
    val showDownloadPrompt: Boolean = false,
    val isExerciseDbReady: Boolean = false,
    val isExerciseDbDownloading: Boolean = false,
    val exerciseDbDownloadedCount: Int = 0,
    val exerciseDbTotalCount: Int? = null,
    val gifDownloadedCount: Int = 0,
    val gifTotalCount: Int = 0,
    val isGifDownloading: Boolean = false,
    val exerciseDbDownloadSizeMb: Int = EXERCISE_DB_DOWNLOAD_MB,
    val exerciseDbGifDownloadSizeMb: Int = EXERCISE_DB_GIF_DOWNLOAD_MB,
    val exerciseDbTotalDownloadSizeMb: Int = EXERCISE_DB_TOTAL_DOWNLOAD_MB
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ExerciseDbRepository,
    private val workoutPlanRepository: WorkoutPlanRepository
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
        observeWorkoutPlans()
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

    fun getExerciseGifFile(exerciseId: String, gifUrl: String?): File? {
        return repository.getExerciseGifFile(exerciseId, gifUrl)
    }

    fun refreshExerciseDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showRefreshPrompt = false, errorMessage = null) }
            repository.refreshExerciseDatabase()
                .onSuccess { exercises ->
                    val bodyParts = (exercises.map { it.bodyPart } + requiredBodyParts)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    val filtered = filterExercises(exercises, _uiState.value.searchQuery, _uiState.value.selectedBodyPart)
                    _uiState.update {
                        it.copy(
                            exercises = filtered,
                            bodyParts = bodyParts,
                            isLoading = false,
                            isExerciseDbReady = true,
                            isExerciseDbDownloading = false,
                            exerciseDbDownloadedCount = exercises.size,
                            exerciseDbTotalCount = exercises.size
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Unable to refresh ExerciseDB data.")
                    }
                }
            enqueueDownloadWork()
            monitorDownloadProgress()
        }
    }

    private fun enqueueDownloadWork() {
        val request = OneTimeWorkRequestBuilder<ExerciseDbDownloadWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ExerciseDbDownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun monitorDownloadProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val progress = repository.getDownloadProgress()
                val gifProgress = if (progress.isComplete) repository.getGifDownloadProgress() else null
                _uiState.update {
                    it.copy(
                        isExerciseDbReady = progress.isComplete,
                        isExerciseDbDownloading = !progress.isComplete,
                        exerciseDbDownloadedCount = progress.downloadedCount,
                        exerciseDbTotalCount = progress.totalCount,
                        gifDownloadedCount = gifProgress?.downloadedCount ?: it.gifDownloadedCount,
                        gifTotalCount = gifProgress?.totalCount ?: it.gifTotalCount,
                        isGifDownloading = gifProgress?.isComplete == false
                    )
                }
                if (progress.isComplete) {
                    refreshBodyParts()
                    refreshExercises()
                    return@launch
                }
                delay(1000)
            }
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
                val gifProgress = repository.getGifDownloadProgress()
                _uiState.update {
                    it.copy(
                        gifDownloadedCount = gifProgress.downloadedCount,
                        gifTotalCount = gifProgress.totalCount,
                        isGifDownloading = !gifProgress.isComplete
                    )
                }
                if (!gifProgress.isComplete) {
                    downloadExerciseGifs()
                }
                refreshBodyParts()
                refreshExercises()
            } else {
                val downloadAccepted = repository.isExerciseDbDownloadAccepted()
                val hasPartialDownload = progress.downloadedCount > 0
                if (!downloadAccepted && !hasPartialDownload) {
                    _uiState.update {
                        it.copy(
                            showDownloadPrompt = true,
                            isExerciseDbReady = false,
                            isExerciseDbDownloading = false,
                            exerciseDbDownloadedCount = progress.downloadedCount,
                            exerciseDbTotalCount = progress.totalCount
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isExerciseDbReady = false,
                        isExerciseDbDownloading = true,
                        exerciseDbDownloadedCount = progress.downloadedCount,
                        exerciseDbTotalCount = progress.totalCount
                    )
                }
                downloadExerciseDatabase()
            }
        }
    }

    private fun downloadExerciseDatabase() {
        viewModelScope.launch {
            repository.downloadExerciseDatabase(
                onProgress = { progress ->
                    _uiState.update {
                        it.copy(
                            isExerciseDbReady = progress.isComplete,
                            isExerciseDbDownloading = !progress.isComplete,
                            exerciseDbDownloadedCount = progress.downloadedCount,
                            exerciseDbTotalCount = progress.totalCount
                        )
                    }
                },
                onGifProgress = { progress ->
                    _uiState.update {
                        it.copy(
                            gifDownloadedCount = progress.downloadedCount,
                            gifTotalCount = progress.totalCount,
                            isGifDownloading = !progress.isComplete
                        )
                    }
                }
            ).onSuccess { exercises ->
                val progress = repository.getDownloadProgress()
                val gifProgress = repository.getGifDownloadProgress()
                if (progress.isComplete && exercises.isNotEmpty()) {
                    val bodyParts = (exercises.map { it.bodyPart } + requiredBodyParts)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    val filtered = filterExercises(exercises, _uiState.value.searchQuery, _uiState.value.selectedBodyPart)
                    _uiState.update {
                        it.copy(
                            exercises = filtered,
                            bodyParts = bodyParts,
                            isLoading = false,
                            isExerciseDbReady = true,
                            isExerciseDbDownloading = false,
                            exerciseDbDownloadedCount = exercises.size,
                            exerciseDbTotalCount = exercises.size,
                            gifDownloadedCount = gifProgress.downloadedCount,
                            gifTotalCount = gifProgress.totalCount,
                            isGifDownloading = !gifProgress.isComplete,
                            showDownloadPrompt = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isExerciseDbReady = false,
                            isExerciseDbDownloading = true,
                            exerciseDbDownloadedCount = progress.downloadedCount,
                            exerciseDbTotalCount = progress.totalCount,
                            isGifDownloading = !gifProgress.isComplete,
                            gifDownloadedCount = gifProgress.downloadedCount,
                            gifTotalCount = gifProgress.totalCount,
                            showDownloadPrompt = false
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isExerciseDbReady = false,
                        isExerciseDbDownloading = false,
                        errorMessage = error.message ?: "Unable to download ExerciseDB data.",
                        isGifDownloading = false,
                        showDownloadPrompt = false
                    )
                }
            }
        }
    }

    private fun downloadExerciseGifs() {
        viewModelScope.launch {
            repository.downloadExerciseGifsForCachedExercises { progress ->
                _uiState.update {
                    it.copy(
                        gifDownloadedCount = progress.downloadedCount,
                        gifTotalCount = progress.totalCount,
                        isGifDownloading = !progress.isComplete
                    )
                }
            }
        }
    }

    fun dismissRefreshPrompt() {
        viewModelScope.launch {
            repository.recordRefreshPromptDismissed()
            _uiState.update { it.copy(showRefreshPrompt = false) }
        }
    }

    fun requestExerciseDbDownloadPrompt() {
        _uiState.update { it.copy(showDownloadPrompt = true) }
    }

    fun deferExerciseDbDownload() {
        _uiState.update { it.copy(showDownloadPrompt = false) }
    }

    fun acceptExerciseDbDownload() {
        viewModelScope.launch {
            repository.recordExerciseDbDownloadAccepted()
            _uiState.update { it.copy(showDownloadPrompt = false, isExerciseDbDownloading = true) }
            downloadExerciseDatabase()
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun addCustomExercise(
        name: String,
        bodyPart: String,
        target: String,
        equipment: String,
        gifUrl: String?,
        videoUrl: String?,
        instructions: List<String>,
        addToPlanId: String?
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Custom exercise name cannot be empty.") }
            return
        }
        val trimmedBodyPart = bodyPart.trim()
        val trimmedTarget = target.trim()
        val trimmedEquipment = equipment.trim()
        val exercise = ExerciseDbExercise(
            id = "custom-${UUID.randomUUID()}",
            name = trimmedName,
            bodyPart = trimmedBodyPart,
            target = trimmedTarget,
            equipment = trimmedEquipment,
            gifUrl = gifUrl?.trim().orEmpty().ifBlank { null },
            videoUrl = videoUrl?.trim().orEmpty().ifBlank { null },
            instructions = instructions
        )
        viewModelScope.launch {
            repository.addCustomExercise(exercise)
                .onSuccess { exercises ->
                    val bodyParts = (exercises.map { it.bodyPart } + requiredBodyParts)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    val filtered = filterExercises(
                        exercises,
                        _uiState.value.searchQuery,
                        _uiState.value.selectedBodyPart
                    )
                    _uiState.update {
                        it.copy(
                            exercises = filtered,
                            bodyParts = bodyParts,
                            errorMessage = null
                        )
                    }
                    addToPlanId?.let { planId ->
                        addExerciseToPlan(planId, exercise)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to save custom exercise.")
                    }
                }
        }
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
        val updatedPlans = _uiState.value.plans + newPlan
        _uiState.update {
            it.copy(
                plans = updatedPlans,
                selectedPlanId = newPlan.id,
                errorMessage = null
            )
        }
        persistPlans(updatedPlans, newPlan.id)
        return true
    }

    fun selectPlan(planId: String?) {
        _uiState.update { it.copy(selectedPlanId = planId) }
        persistPlans(_uiState.value.plans, planId)
    }

    fun removePlan(planId: String) {
        val updatedPlans = _uiState.value.plans.filterNot { it.id == planId }
        val updatedSelected = if (_uiState.value.selectedPlanId == planId) {
            updatedPlans.firstOrNull()?.id
        } else {
            _uiState.value.selectedPlanId
        }
        _uiState.update { state -> state.copy(plans = updatedPlans, selectedPlanId = updatedSelected) }
        persistPlans(updatedPlans, updatedSelected)
    }

    fun addExerciseToPlan(planId: String, exercise: ExerciseDbExercise) {
        val updatedPlans = _uiState.value.plans.map { plan ->
            if (plan.id == planId) {
                if (plan.exercises.any { it.id == exercise.id }) {
                    plan
                } else {
                    plan.copy(exercises = plan.exercises + exercise.toPlanExercise())
                }
            } else {
                plan
            }
        }
        _uiState.update { state -> state.copy(plans = updatedPlans) }
        persistPlans(updatedPlans, _uiState.value.selectedPlanId)
    }

    fun removeExerciseFromPlan(planId: String, exerciseId: String) {
        val updatedPlans = _uiState.value.plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(exercises = plan.exercises.filterNot { it.id == exerciseId })
            } else {
                plan
            }
        }
        _uiState.update { state -> state.copy(plans = updatedPlans) }
        persistPlans(updatedPlans, _uiState.value.selectedPlanId)
    }

    fun updateExerciseSettings(
        planId: String,
        exerciseId: String,
        sets: Int?,
        repetitions: Int?,
        durationMinutes: Int?,
        restSeconds: Int?,
        weight: Double?
    ) {
        val updatedPlans = _uiState.value.plans.map { plan ->
            if (plan.id == planId) {
                val updatedExercises = plan.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = sets,
                            repetitions = repetitions,
                            durationMinutes = durationMinutes,
                            restSeconds = restSeconds,
                            weight = weight
                        )
                    } else {
                        exercise
                    }
                }
                plan.copy(exercises = updatedExercises)
            } else {
                plan
            }
        }
        _uiState.update { state -> state.copy(plans = updatedPlans) }
        persistPlans(updatedPlans, _uiState.value.selectedPlanId)
    }

    fun updatePlanCalories(planId: String, caloriesPerWorkout: Int?) {
        val updatedPlans = _uiState.value.plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(caloriesPerWorkout = caloriesPerWorkout)
            } else {
                plan
            }
        }
        _uiState.update { state -> state.copy(plans = updatedPlans) }
        persistPlans(updatedPlans, _uiState.value.selectedPlanId)
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

    private fun observeWorkoutPlans() {
        viewModelScope.launch {
            workoutPlanRepository.planState.collect { state ->
                _uiState.update { uiState ->
                    uiState.copy(plans = state.plans, selectedPlanId = state.selectedPlanId)
                }
            }
        }
    }

    private fun persistPlans(plans: List<WorkoutPlan>, selectedPlanId: String?) {
        viewModelScope.launch {
            workoutPlanRepository.savePlans(plans, selectedPlanId)
        }
    }
}
