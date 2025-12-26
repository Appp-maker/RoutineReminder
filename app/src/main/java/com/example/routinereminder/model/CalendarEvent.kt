package com.example.routinereminder.model

data class CalendarEvent(
    val id: String,
    val summary: String,
    val description: String?,
    val start: String,
    val end: String
)
