package com.example.routinereminder.util

import com.example.routinereminder.data.ScheduleItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

object RecurrenceUtil {
    fun nextOccurrenceMillis(item: ScheduleItem, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): Long? {
        return if (item.isOneTime) {
            val d = item.dateEpochDay ?: return null
            val dt = LocalDate.ofEpochDay(d).atTime(item.hour, item.minute).atZone(now.zone)
            if (dt.isBefore(now)) null else dt.toInstant().toEpochMilli()
        } else {
            // For recurring events
            val daysToRepeatOn = item.repeatOnDays
            if (daysToRepeatOn.isNullOrEmpty()) {
                return null
            }

            var earliestNextDt: ZonedDateTime? = null

            // Iterate through each selected day of the week to find the soonest future occurrence
            for (day in daysToRepeatOn.sortedBy { it.value }) { // Sort for consistent processing
                var dtCandidate = now.with(ChronoField.DAY_OF_WEEK, day.value.toLong())
                    .withHour(item.hour).withMinute(item.minute).withSecond(0).withNano(0)

                if (dtCandidate.isBefore(now)) {
                    dtCandidate = dtCandidate.plusWeeks(1) // Move to the same day next week
                }

                if (earliestNextDt == null || dtCandidate.isBefore(earliestNextDt)) {
                    earliestNextDt = dtCandidate
                }
            }

            if (earliestNextDt == null) {
                // This should not happen if daysToRepeatOn is not empty
                return null
            }

            // earliestNextDt is now guaranteed to be non-null due to the check above.
            // So, finalNextDt can be declared as non-nullable ZonedDateTime.
            var finalNextDt: ZonedDateTime = earliestNextDt

            // Align to anchor day if provided and interval is greater than 1 week
            val interval = if (item.repeatEveryWeeks <= 0) 1 else item.repeatEveryWeeks
            if (interval > 1) {
                // Use startEpochDay as the primary anchor.
                // If startEpochDay is null, we need a stable reference.
                // Using a date far in the past aligned to the determined day of week of finalNextDt
                // provides a stable base for modulo arithmetic.
                val anchorDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) }
                    ?: finalNextDt.toLocalDate().minusYears(10) // finalNextDt is non-null here
                                    // It's better if startEpochDay is always set for N-weekly items.

                // Align the anchor to the *actual day of the week* of our calculated `finalNextDt`
                // This ensures the week difference calculation is consistent for the modulo operation.
                var alignedAnchorDate = anchorDate.with(ChronoField.DAY_OF_WEEK, finalNextDt.dayOfWeek.value.toLong())
                
                // Ensure the aligned anchor is not after the event day we are calculating from
                // For instance, if anchor was a future date but same day of week.
                while (alignedAnchorDate.atStartOfDay(now.zone).isAfter(finalNextDt.toLocalDate().atStartOfDay(now.zone))) {
                    alignedAnchorDate = alignedAnchorDate.minusWeeks(interval.toLong());
                }
                
                // Calculate weeks between the (past or current) aligned anchor and our determined next event date
                val weeksBetween = ChronoUnit.WEEKS.between(alignedAnchorDate, finalNextDt.toLocalDate()) // finalNextDt is non-null
                val mod = weeksBetween % interval

                if (mod != 0L) {
                    finalNextDt = finalNextDt.plusWeeks(interval - mod) // finalNextDt is non-null
                }

                // Crucial: After N-week alignment, ensure it's truly in the future relative to 'now'.
                // This handles cases where the anchor was far in the past, and the N-week alignment
                // might have landed on a date that's still before 'now'.
                while (finalNextDt.isBefore(now)) { // finalNextDt is non-null
                    finalNextDt = finalNextDt.plusWeeks(interval.toLong()) // finalNextDt is non-null
                }
            }
            return finalNextDt.toInstant().toEpochMilli() // finalNextDt is non-null
        }
    }
}
