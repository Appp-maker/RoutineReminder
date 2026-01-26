package com.example.routinereminder.ui

data class RoutineInsights(
    val weeklyAdherencePercent: Int,
    val consistencyScorePercent: Int,
    val currentStreakDays: Int,
    val totalScheduled: Int,
    val totalCompleted: Int
)
