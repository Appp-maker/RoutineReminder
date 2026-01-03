package com.example.routinereminder.data.workout

import com.example.routinereminder.data.exercisedb.ExerciseDbExercise

data class WorkoutPlanExercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val target: String,
    val equipment: String,
    val gifUrl: String? = null,
    val sets: Int? = null,
    val repetitionsPerSet: Int? = null,
    val durationMinutesPerSet: Int? = null,
    val restSecondsBetweenSets: Int? = null,
    val weightKgPerSet: Double? = null
)

data class WorkoutPlan(
    val id: String,
    val name: String,
    val exercises: List<WorkoutPlanExercise> = emptyList()
)

fun ExerciseDbExercise.toPlanExercise(): WorkoutPlanExercise = WorkoutPlanExercise(
    id = id,
    name = name,
    bodyPart = bodyPart,
    target = target,
    equipment = equipment,
    gifUrl = gifUrl
)
