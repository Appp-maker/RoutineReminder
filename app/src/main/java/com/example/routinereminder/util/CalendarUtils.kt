package com.example.routinereminder.util

import android.content.ContentResolver
import android.content.ContentUris 
import android.content.Context
import android.provider.CalendarContract
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.example.routinereminder.data.ScheduleItem
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class CalendarInstance(
    val eventId: Long,
    val title: String?,
    val beginMillis: Long,
    val endMillis: Long,
    val calendarDisplayName: String?,
    val calendarId: Long
)

data class Conflict(
    val scheduleItem: ScheduleItem,
    val conflictingCalendarInstance: CalendarInstance,
    val nextOccurrenceMillis: Long
)

object CalendarUtils {

    private val INSTANCE_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        CalendarContract.Instances.CALENDAR_ID
    )

    fun queryInstances(context: Context, rangeStartMillis: Long, rangeEndMillis: Long): List<CalendarInstance> {
        val instances = mutableListOf<CalendarInstance>()
        val contentResolver: ContentResolver = context.contentResolver

        // Build the URI for instances within a time range
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, rangeStartMillis)
        ContentUris.appendId(builder, rangeEndMillis)
        val uri = builder.build()

        // Check for READ_CALENDAR permission before querying
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // TODO: Handle missing permission - request it or inform user
            return emptyList()
        }

        // Use the correctly built uri here
        val cursor = contentResolver.query(
            uri,
            INSTANCE_PROJECTION,
            null, // No selection clause, as the time range is part of the URI
            null, // No selection arguments
            CalendarContract.Instances.BEGIN + " ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                instances.add(
                    CalendarInstance(
                        eventId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)),
                        title = it.getStringOrNull(it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)),
                        beginMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)),
                        endMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Instances.END)),
                        calendarDisplayName = it.getStringOrNull(it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)),
                        calendarId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID))
                    )
                )
            }
        }
        return instances
    }

    fun findConflicts(
        scheduleItems: List<ScheduleItem>,
        calendarInstances: List<CalendarInstance>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()
        val now = ZonedDateTime.now(zoneId)

        scheduleItems.forEach { item ->
            if (!item.isOneTime || item.dateEpochDay != null) { // Only check items that can have a next occurrence
                RecurrenceUtil.nextOccurrenceMillis(item, now)?.let { nextOccurrenceStartMillis ->
                    val nextOccurrenceEndMillis = nextOccurrenceStartMillis + (item.durationMinutes * 60_000)

                    calendarInstances.forEach { instance ->
                        // Check for overlap: (StartA < EndB) and (StartB < EndA)
                        if (nextOccurrenceStartMillis < instance.endMillis && instance.beginMillis < nextOccurrenceEndMillis) {
                            conflicts.add(Conflict(item, instance, nextOccurrenceStartMillis))
                        }
                    }
                }
            }
        }
        return conflicts
    }
}
