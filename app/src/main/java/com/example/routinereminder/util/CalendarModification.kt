
package com.example.routinereminder.util

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.utils.dayOfWeekToRRuleDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

object CalendarModification {

    fun createCalendarEvent(context: Context, item: ScheduleItem): Long? {
        val cr = context.contentResolver
        val values = ContentValues()

        // Determine the correct start date for the event's first instance.
        val startDate = (if (item.isOneTime) item.dateEpochDay else item.startEpochDay)?.let {
            LocalDate.ofEpochDay(it)
        } ?: LocalDate.now()

        val startTime = LocalTime.of(item.hour, item.minute)
        val startDateTime = startDate.atTime(startTime)

        // Calculate start and end times for the first occurrence in UTC milliseconds.
        // Using a specific DTEND is more compatible across different calendar apps than using DURATION.
        val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = startMillis + item.durationMinutes * 60_000L

        // --- Set values common to ALL events ---
        values.put(CalendarContract.Events.DTSTART, startMillis)
        values.put(CalendarContract.Events.DTEND, endMillis)
        values.put(CalendarContract.Events.TITLE, item.name)
        values.put(CalendarContract.Events.DESCRIPTION, item.notes)
        values.put(CalendarContract.Events.CALENDAR_ID, 1L)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)

        // --- Add recurrence rule ONLY for recurring events ---
        if (!item.isOneTime) {
            item.repeatOnDays?.takeIf { it.isNotEmpty() }?.let { days ->
                val byDayPart = days.joinToString(",") { dayOfWeekToRRuleDay(it) }
                val interval = item.repeatEveryWeeks.coerceAtLeast(1)
                val rrule = "FREQ=WEEKLY;INTERVAL=$interval;BYDAY=$byDayPart"
                values.put(CalendarContract.Events.RRULE, rrule)
                Log.d("CalendarModification", "Final RRULE for recurring event: $rrule")
            }
        }

        return try {
            val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()
            Log.i("CalendarModification", "Successfully inserted event. ID: $eventId")
            eventId
        } catch (e: Exception) {
            Log.e("CalendarModification", "Error inserting calendar event", e)
            null
        }
    }
}
