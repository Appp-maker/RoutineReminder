package com.example.routinereminder.data.workout

import com.example.routinereminder.data.exercisedb.ExerciseDbExercise

data class WorkoutPlanExercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val target: String,
    val equipment: String,
    val gifUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val instructions: List<String> = emptyList(),
    val sets: Int? = null,
    val repetitions: Int? = null,
    val durationMinutes: Int? = null,
    val restSeconds: Int? = null,
    val weight: Double? = null
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
    gifUrl = gifUrl,
    imageUrls = imageUrls,
    videoUrl = videoUrl,
    instructions = instructions
)
