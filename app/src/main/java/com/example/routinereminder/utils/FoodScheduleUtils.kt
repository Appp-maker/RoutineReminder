package com.example.routinereminder.utils

import com.example.routinereminder.data.entities.LoggedFood
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object FoodScheduleUtils {

    fun occursOnDate(item: LoggedFood, date: LocalDate): Boolean {
        // ---- One-time food entry ----
        if (item.isOneTime) {
            val oneTimeDate = item.dateEpochDay?.let { LocalDate.ofEpochDay(it) }
            return oneTimeDate == date
        }

        // ---- Recurring food entry ----
        val repeatDays = item.repeatOnDays ?: return false
        val startDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: return false

        // Skip before start date
        if (date.isBefore(startDate)) return false
        // Must be one of the chosen weekdays
        if (date.dayOfWeek !in repeatDays) return false

        val weeksBetween = ChronoUnit.WEEKS.between(startDate, date)
        return weeksBetween % item.repeatEveryWeeks == 0L
    }
}
