package com.example.routinereminder.data

const val DEFAULT_SERIES_COLOR_ARGB: Int = -14575885
const val DEFAULT_SERIES_COLOR_ARGB_SQL: String = "-14575885"

const val NO_EVENT_FOOD_COLOR_ARGB: Int = 0
const val NO_EVENT_FOOD_COLOR_ARGB_SQL: String = "0"

val SERIES_COLOR_OPTIONS: List<Int> = listOf(
    DEFAULT_SERIES_COLOR_ARGB,
    0xFF4CAF50.toInt(),
    0xFFFFC107.toInt(),
    0xFFE91E63.toInt(),
    0xFF00BCD4.toInt(),
    0xFFFF5722.toInt(),
    0xFFFF5252.toInt(),
    0xFF64B5F6.toInt()
)

fun defaultSeriesColorForIndex(index: Int): Int {
    if (SERIES_COLOR_OPTIONS.isEmpty()) return DEFAULT_SERIES_COLOR_ARGB
    return SERIES_COLOR_OPTIONS[index % SERIES_COLOR_OPTIONS.size]
}
