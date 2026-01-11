package com.example.routinereminder.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.SettingsRepository
import com.example.routinereminder.data.DEFAULT_SERIES_COLOR_ARGB
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

data class CalendarMeta(
    val id: Long,
    val accountType: String?,
    val accountName: String?,
    val isPrimary: Boolean
)

data class CalendarEventDetails(
    val id: Long,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long,
    val rrule: String?,
    val calendarId: Long
)

object CalendarSyncManager {
    private val CALENDAR_PROJECTION = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.ACCOUNT_TYPE,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.IS_PRIMARY
    )

    private val EVENT_PROJECTION = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.RRULE,
        CalendarContract.Events.CALENDAR_ID
    )

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val writeGranted = context.checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun queryCalendars(context: Context): List<CalendarMeta> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val calendars = mutableListOf<CalendarMeta>()
        val resolver = context.contentResolver
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            CALENDAR_PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val accountTypeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            val accountNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val isPrimaryIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarMeta(
                        id = cursor.getLong(idIndex),
                        accountType = cursor.getString(accountTypeIndex),
                        accountName = cursor.getString(accountNameIndex),
                        isPrimary = cursor.getInt(isPrimaryIndex) == 1
                    )
                )
            }
        }
        return calendars
    }

    fun resolveImportCalendarIds(
        calendars: List<CalendarMeta>,
        importTarget: String,
        importTargetForBoth: String,
        selectedGoogleAccountName: String?
    ): List<Long> {
        return when (importTarget) {
            SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL -> {
                calendars.filter { it.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL }.map { it.id }
            }
            SettingsRepository.IMPORT_TARGET_CALENDAR_BOTH -> {
                val localIds = calendars.filter { it.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL }.map { it.id }
                val googleIds = resolveSingleCalendarIds(calendars, importTargetForBoth, selectedGoogleAccountName)
                (localIds + googleIds).distinct()
            }
            else -> resolveSingleCalendarIds(calendars, importTarget, selectedGoogleAccountName)
        }
    }

    fun resolveTargetCalendarId(
        calendars: List<CalendarMeta>,
        targetSystem: String?,
        selectedGoogleAccountName: String?
    ): Long? {
        val target = targetSystem ?: SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL
        return resolveSingleCalendarIds(calendars, target, selectedGoogleAccountName).firstOrNull()
    }

    private fun resolveSingleCalendarIds(
        calendars: List<CalendarMeta>,
        target: String,
        selectedGoogleAccountName: String?
    ): List<Long> {
        return when {
            target == SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL -> {
                calendars.filter { it.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL }.map { it.id }
            }
            target == SettingsRepository.IMPORT_TARGET_CALENDAR_PRIMARY -> {
                calendars.filter { it.accountType == GOOGLE_ACCOUNT_TYPE && it.isPrimary }.map { it.id }
            }
            target == SettingsRepository.IMPORT_TARGET_SELECTED_GOOGLE_ACCOUNT_PRIMARY -> {
                val filtered = calendars.filter { it.accountType == GOOGLE_ACCOUNT_TYPE }
                val byAccount = selectedGoogleAccountName?.let { name ->
                    filtered.filter { it.accountName == name }
                } ?: filtered
                byAccount.filter { it.isPrimary }.map { it.id }
            }
            target.startsWith(GOOGLE_CALENDAR_ID_PREFIX) -> {
                target.removePrefix(GOOGLE_CALENDAR_ID_PREFIX).toLongOrNull()?.let { listOf(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    fun queryEvents(
        context: Context,
        calendarIds: List<Long>,
        rangeStartMillis: Long,
        rangeEndMillis: Long
    ): List<CalendarEventDetails> {
        if (calendarIds.isEmpty()) return emptyList()
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val resolver = context.contentResolver
        val selectionArgs = mutableListOf<String>()
        val calendarPlaceholders = calendarIds.joinToString(",") { "?" }
        selectionArgs.addAll(calendarIds.map { it.toString() })
        selectionArgs.add(rangeEndMillis.toString())
        selectionArgs.add(rangeStartMillis.toString())
        val selection = """
            ${CalendarContract.Events.CALENDAR_ID} IN ($calendarPlaceholders)
            AND ${CalendarContract.Events.DELETED} = 0
            AND ${CalendarContract.Events.DTSTART} <= ?
            AND (${CalendarContract.Events.DTEND} IS NULL OR ${CalendarContract.Events.DTEND} >= ?)
        """.trimIndent()
        val events = mutableListOf<CalendarEventDetails>()
        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            EVENT_PROJECTION,
            selection,
            selectionArgs.toTypedArray(),
            CalendarContract.Events.DTSTART + " ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val rruleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
            val calendarIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
            while (cursor.moveToNext()) {
                val startMillis = cursor.getLong(startIndex)
                val endMillis = cursor.getLong(endIndex)
                events.add(
                    CalendarEventDetails(
                        id = cursor.getLong(idIndex),
                        title = cursor.getString(titleIndex) ?: "",
                        description = cursor.getString(descriptionIndex),
                        startMillis = startMillis,
                        endMillis = endMillis,
                        rrule = cursor.getString(rruleIndex),
                        calendarId = cursor.getLong(calendarIdIndex)
                    )
                )
            }
        }
        return events
    }

    fun eventExists(context: Context, eventId: Long): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val resolver = context.contentResolver
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        resolver.query(uri, arrayOf(CalendarContract.Events._ID), null, null, null)?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    fun upsertEvent(
        context: Context,
        item: ScheduleItem,
        calendarId: Long
    ): Long? {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val resolver = context.contentResolver
        val values = buildEventValues(item, calendarId)
        return if (item.calendarEventId != null) {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, item.calendarEventId)
            val updated = resolver.update(uri, values, null, null)
            if (updated > 0) item.calendarEventId else null
        } else {
            resolver.insert(CalendarContract.Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
        }
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val resolver = context.contentResolver
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return resolver.delete(uri, null, null) > 0
    }

    fun findMatchingEventId(
        context: Context,
        calendarId: Long,
        item: ScheduleItem
    ): Long? {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val resolver = context.contentResolver
        val startMillis = eventStartMillis(item)
        val endMillis = startMillis + item.durationMinutes * 60_000L
        val selection = """
            ${CalendarContract.Events.CALENDAR_ID} = ?
            AND ${CalendarContract.Events.DTSTART} = ?
            AND ${CalendarContract.Events.DTEND} = ?
            AND ${CalendarContract.Events.TITLE} = ?
            AND ${CalendarContract.Events.DELETED} = 0
        """.trimIndent()
        val args = arrayOf(
            calendarId.toString(),
            startMillis.toString(),
            endMillis.toString(),
            item.name
        )
        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            selection,
            args,
            null
        )?.use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
        return null
    }

    fun eventToScheduleItem(
        event: CalendarEventDetails,
        calendarMetaMap: Map<Long, CalendarMeta>,
        existingId: Long = 0,
        existingOrigin: String? = null,
        existingTargetCalendarSystem: String? = null,
        existingColorArgb: Int? = null
    ): ScheduleItem {
        val zoneId = ZoneId.systemDefault()
        val endMillis = if (event.endMillis > event.startMillis) {
            event.endMillis
        } else {
            event.startMillis + 60_000L
        }
        val startDateTime = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalDateTime()
        val endDateTime = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDateTime()
        val durationMinutes = kotlin.math.max(
            1,
            java.time.Duration.between(startDateTime, endDateTime).toMinutes().toInt()
        )
        val rrule = event.rrule
        val (isOneTime, dateEpochDay, startEpochDay, repeatOnDays, repeatEveryWeeks) =
            parseRecurrence(rrule, startDateTime.toLocalDate())
        val calendarMeta = calendarMetaMap[event.calendarId]
        val isGoogleCalendar = calendarMeta?.accountType == GOOGLE_ACCOUNT_TYPE
        val origin = existingOrigin ?: if (isGoogleCalendar) "IMPORTED_GOOGLE" else "IMPORTED_LOCAL"
        val targetCalendarSystem = existingTargetCalendarSystem ?: if (isGoogleCalendar) {
            "$GOOGLE_CALENDAR_ID_PREFIX${event.calendarId}"
        } else {
            SettingsRepository.IMPORT_TARGET_CALENDAR_LOCAL
        }
        return ScheduleItem(
            id = existingId,
            name = event.title.ifBlank { "Untitled Event" },
            notes = event.description,
            hour = startDateTime.hour,
            minute = startDateTime.minute,
            durationMinutes = durationMinutes,
            isOneTime = isOneTime,
            dateEpochDay = dateEpochDay,
            startEpochDay = startEpochDay,
            repeatOnDays = repeatOnDays,
            repeatEveryWeeks = repeatEveryWeeks,
            addToCalendarOnSave = false,
            calendarEventId = event.id,
            origin = origin,
            targetCalendarSystem = targetCalendarSystem,
            colorArgb = existingColorArgb ?: DEFAULT_SERIES_COLOR_ARGB
        )
    }

    fun isGoogleCalendar(calendarId: Long, calendars: List<CalendarMeta>): Boolean {
        return calendars.firstOrNull { it.id == calendarId }?.accountType == GOOGLE_ACCOUNT_TYPE
    }

    private fun buildEventValues(item: ScheduleItem, calendarId: Long): ContentValues {
        val startMillis = eventStartMillis(item)
        val endMillis = startMillis + item.durationMinutes * 60_000L
        return ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, item.name)
            put(CalendarContract.Events.DESCRIPTION, item.notes)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            if (!item.isOneTime && !item.repeatOnDays.isNullOrEmpty()) {
                val byDayPart = item.repeatOnDays.joinToString(",") { day ->
                    when (day) {
                        DayOfWeek.MONDAY -> "MO"
                        DayOfWeek.TUESDAY -> "TU"
                        DayOfWeek.WEDNESDAY -> "WE"
                        DayOfWeek.THURSDAY -> "TH"
                        DayOfWeek.FRIDAY -> "FR"
                        DayOfWeek.SATURDAY -> "SA"
                        DayOfWeek.SUNDAY -> "SU"
                    }
                }
                val interval = item.repeatEveryWeeks.coerceAtLeast(1)
                put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;INTERVAL=$interval;BYDAY=$byDayPart")
            } else {
                putNull(CalendarContract.Events.RRULE)
            }
        }
    }

    private fun eventStartMillis(item: ScheduleItem): Long {
        val date = (if (item.isOneTime) item.dateEpochDay else item.startEpochDay)?.let {
            LocalDate.ofEpochDay(it)
        } ?: LocalDate.now()
        val time = LocalTime.of(item.hour, item.minute)
        val dateTime = LocalDateTime.of(date, time)
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseRecurrence(
        rrule: String?,
        startDate: LocalDate
    ): ParsedRecurrence {
        if (rrule.isNullOrBlank() || !rrule.contains("FREQ=WEEKLY")) {
            return ParsedRecurrence(
                isOneTime = true,
                dateEpochDay = startDate.toEpochDay(),
                startEpochDay = null,
                repeatOnDays = null,
                repeatEveryWeeks = 1
            )
        }
        val repeatOnDays = parseRRuleDays(rrule)
        val interval = parseRRuleInterval(rrule)
        return ParsedRecurrence(
            isOneTime = false,
            dateEpochDay = null,
            startEpochDay = startDate.toEpochDay(),
            repeatOnDays = repeatOnDays,
            repeatEveryWeeks = interval
        )
    }

    private fun parseRRuleInterval(rrule: String): Int {
        val match = Regex("INTERVAL=([0-9]+)").find(rrule)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    private fun parseRRuleDays(rrule: String): Set<DayOfWeek>? {
        val match = Regex("BYDAY=([^;]+)").find(rrule) ?: return null
        val tokens = match.groupValues[1].split(",")
        val days = tokens.mapNotNull { token ->
            when (token.trim()) {
                "MO" -> DayOfWeek.MONDAY
                "TU" -> DayOfWeek.TUESDAY
                "WE" -> DayOfWeek.WEDNESDAY
                "TH" -> DayOfWeek.THURSDAY
                "FR" -> DayOfWeek.FRIDAY
                "SA" -> DayOfWeek.SATURDAY
                "SU" -> DayOfWeek.SUNDAY
                else -> null
            }
        }.toSet()
        return days.takeIf { it.isNotEmpty() }
    }

    private data class ParsedRecurrence(
        val isOneTime: Boolean,
        val dateEpochDay: Long?,
        val startEpochDay: Long?,
        val repeatOnDays: Set<DayOfWeek>?,
        val repeatEveryWeeks: Int
    )

    private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    private const val GOOGLE_CALENDAR_ID_PREFIX = "google_calendar_id_"
}
