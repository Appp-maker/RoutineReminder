package com.example.routinereminder.ui

data class RoutineInsights(
    val todayCompletionPercent: Int,
    val weeklyCompletionPercent: Int,
    val averageDailyConsistencyPercent: Int,
    val overallCompletionPercent: Int,
    val currentStreakDays: Int,
    val totalScheduled: Int,
    val totalCompleted: Int
)
