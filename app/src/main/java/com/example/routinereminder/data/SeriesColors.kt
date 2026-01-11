package com.example.routinereminder.data

const val DEFAULT_SERIES_COLOR_ARGB: Int = -14776091
const val DEFAULT_SERIES_COLOR_ARGB_SQL: String = "-14776091"

val SERIES_COLOR_OPTIONS: List<Int> = listOf(
    DEFAULT_SERIES_COLOR_ARGB,
    0xFF43A047.toInt(),
    0xFFF4511E.toInt(),
    0xFF8E24AA.toInt(),
    0xFFFDD835.toInt(),
    0xFF00897B.toInt(),
    0xFF6D4C41.toInt(),
    0xFFD81B60.toInt()
)

fun defaultSeriesColorForIndex(index: Int): Int {
    if (SERIES_COLOR_OPTIONS.isEmpty()) return DEFAULT_SERIES_COLOR_ARGB
    return SERIES_COLOR_OPTIONS[index % SERIES_COLOR_OPTIONS.size]
}
