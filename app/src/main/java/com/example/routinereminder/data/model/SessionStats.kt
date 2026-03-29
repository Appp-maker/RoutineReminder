package com.example.routinereminder.data.model

import org.maplibre.geojson.Point

data class WeatherTimelineEntry(
    val epochMs: Long,
    val weatherCode: Int,
    val temperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val lightCondition: String
)

data class TerrainDuration(
    val terrain: String,
    val durationSec: Long
)

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
    val weatherTimeline: List<WeatherTimelineEntry> = emptyList(),
    val terrainDurations: List<TerrainDuration> = emptyList(),
    var snapshotPath: String? = null
)
