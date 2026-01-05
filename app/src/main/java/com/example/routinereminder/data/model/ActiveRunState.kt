package com.example.routinereminder.data.model

data class ActiveRunState(
    val sessionId: String,
    val activity: String,
    val isRecording: Boolean,
    val trackingMode: String,
    val startEpochMs: Long,
    val distanceMeters: Double,
    val durationSec: Long,
    val calories: Double,
    val splitDurationsSec: List<Long> = emptyList()
)


data class TrailPoint(
    val lat: Double,
    val lng: Double
)
