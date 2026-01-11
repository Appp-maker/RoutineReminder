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
