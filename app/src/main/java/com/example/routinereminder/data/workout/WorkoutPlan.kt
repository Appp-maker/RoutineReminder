package com.example.routinereminder.data.workout

import com.example.routinereminder.data.exercisedb.ExerciseDbExercise


data class WorkoutPlan(
    val id: String,
    val name: String,
    val exercises: List<ExerciseDbExercise> = emptyList()
)
