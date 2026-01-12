package com.example.routinereminder.data

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val customCaloriesTarget: Double,
    val customProteinTargetG: Double = 0.0,
    val customCarbsTargetG: Double = 0.0,
    val customFatTargetG: Double = 0.0,
    val customFiberTargetG: Double = 0.0,
    val customSaturatedFatTargetG: Double = 0.0,
    val customAddedSugarsTargetG: Double = 0.0,
    val customSodiumTargetMg: Double = 0.0,
    val calorieGoal: CalorieGoal = CalorieGoal.MAINTAIN
)

enum class Gender {
    MALE, FEMALE
}

enum class ActivityLevel {
    SEDENTARY, LIGHT, MODERATE, ACTIVE
}

enum class CalorieGoal {
    MAINTAIN, LOSE_WEIGHT, GAIN_WEIGHT
}

@Serializable
data class DefaultEventSettings(
    val hour: Int,
    val minute: Int,
    val durationHours: Int,
    val durationMinutes: Int,
    val isOneTime: Boolean,
    val createCalendarEntry: Boolean,
    val systemNotification: Boolean,
    val showDetailsInNotification: Boolean,
    val startTimeOptionName: String,
    val targetCalendarId: String
)
