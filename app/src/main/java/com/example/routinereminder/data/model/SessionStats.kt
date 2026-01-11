package com.example.routinereminder.data.model

import org.maplibre.geojson.Point

data class SessionStats(
    val id: String,
    val activity: String,            // e.g. "Run/Walk", "Cycling"
    val startEpochMs: Long,          // start timestamp in ms
    val endEpochMs: Long,            // end timestamp in ms
    val durationSec: Long,           // total seconds
    val distanceMeters: Double,      // total meters
    val calories: Double,            // calories burned
    val avgPaceSecPerKm: Long,       // average pace in seconds per km
    val splitPaceSecPerKm: List<Long>? = emptyList(),
    val polyline: List<Point> = emptyList(), // map trail
    var snapshotPath: String? = null
)
