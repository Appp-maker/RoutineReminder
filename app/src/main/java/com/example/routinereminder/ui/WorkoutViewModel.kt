package com.example.routinereminder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routinereminder.data.exercisedb.ExerciseDbExercise
import com.example.routinereminder.data.exercisedb.ExerciseDbRepository
import com.example.routinereminder.data.workout.WorkoutPlan
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
    val showRefreshPrompt: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
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

    init {
        viewModelScope.launch {
            repository.preloadExerciseDatabase()
        }
        refreshBodyParts()
        refreshExercises()
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
            _uiState.update { it.copy(isLoading = true, showRefreshPrompt = false, errorMessage = null) }
            repository.refreshExerciseDatabase()
                .onSuccess { exercises ->
                    val bodyParts = (exercises.map { it.bodyPart } + requiredBodyParts)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    val filtered = filterExercises(exercises, _uiState.value.searchQuery, _uiState.value.selectedBodyPart)
                    _uiState.update { it.copy(exercises = filtered, bodyParts = bodyParts, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Unable to refresh ExerciseDB data.")
                    }
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
