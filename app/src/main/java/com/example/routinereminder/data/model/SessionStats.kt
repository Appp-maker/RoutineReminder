package com.example.routinereminder.data.model

import org.maplibre.geojson.Point

data class SessionStats(
    val id: String,
    val activity: String,            // e.g. "Running", "Walking"
    val startEpochMs: Long,          // start timestamp in ms
    val endEpochMs: Long,            // end timestamp in ms
    val durationSec: Long,           // total seconds
    val distanceMeters: Double,      // total meters
    val avgPaceSecPerKm: Long,       // average pace in seconds per km
    val polyline: List<Point> = emptyList(), // map trail
    var snapshotPath: String? = null
)