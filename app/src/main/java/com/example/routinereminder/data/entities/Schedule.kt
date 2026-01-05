package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.DayOfWeek

@Entity(
    tableName = "schedule",
    indices = [Index(value = ["name", "hour", "minute", "dateEpochDay"])]
)
@TypeConverters // Keep if you already use converters for DayOfWeek sets
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int = 60,
    val isOneTime: Boolean = false,
    val dateEpochDay: Long? = null,
    val startEpochDay: Long? = null,
    val repeatOnDays: Set<DayOfWeek>? = null,
    val repeatEveryWeeks: Int = 1,
    val notifyEnabled: Boolean = true,
    val showDetailsInNotification: Boolean = false,
    val addToCalendarOnSave: Boolean = false,
    val calendarEventId: Long? = null,
    val origin: String = "APP_CREATED",
    val targetCalendarSystem: String? = null,

    // NEW FIELD — Marks event as completed.
    // If true: UI shows it as finished AND notifications must not fire.
    val isDone: Boolean = false
)
