
package com.example.routinereminder.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class DayOfWeekSetConverter {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String? {
        return days?.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String?): Set<DayOfWeek>? {
        return data?.takeIf { it.isNotBlank() }?.split(',')?.mapNotNull {
            try {
                DayOfWeek.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }?.toSet()
    }
}
