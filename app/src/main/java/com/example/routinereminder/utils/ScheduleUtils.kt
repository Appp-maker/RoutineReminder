package com.example.routinereminder.utils

import com.example.routinereminder.data.ScheduleItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ScheduleUtils {

    fun occursOnDate(item: ScheduleItem, date: LocalDate): Boolean {
        // ----- One-time event -----
        if (item.isOneTime) {
            val oneTimeDate = item.dateEpochDay?.let { LocalDate.ofEpochDay(it) }
            return oneTimeDate == date
        }

        // ----- Repeating event -----
        val repeatDays = item.repeatOnDays
        val startDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) }

        // If no repeat days or no start date → can't occur
        if (repeatDays.isNullOrEmpty() || startDate == null) return false

        // Must match one of the selected weekdays
        if (date.dayOfWeek !in repeatDays) return false

        // Cannot occur before the start date
        if (date.isBefore(startDate)) return false

        // Determine how many full weeks have passed since the anchor start
        val weeksBetween = ChronoUnit.WEEKS.between(startDate, date)

        // If every week (1), always true
        if (item.repeatEveryWeeks <= 1) return true

        // Occurs only on multiples of N weeks
        return weeksBetween % item.repeatEveryWeeks == 0L
    }
}
