package com.example.routinereminder.data

import com.example.routinereminder.utils.ScheduleUtils
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB
import java.time.DayOfWeek
import java.time.LocalDate

data class ScheduleItem(
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
    val colorArgb: Int = DEFAULT_SERIES_COLOR_ARGB,

    // ⭐ REQUIRED for Done/Undo UI
    val isDone: Boolean = false
) {
    fun occursOnDate(date: LocalDate): Boolean {
        return ScheduleUtils.occursOnDate(this, date)
    }
}
