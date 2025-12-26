package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calendar_import")
data class BlockedCalendarImport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val calendarId: String,
    val calendarEventId: Long
)
