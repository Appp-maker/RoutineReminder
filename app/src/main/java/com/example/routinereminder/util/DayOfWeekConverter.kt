package com.example.routinereminder.util

import androidx.room.TypeConverter
import java.time.DayOfWeek

class DayOfWeekConverter {
    @TypeConverter
    fun fromDayOfWeek(dayOfWeek: DayOfWeek?): String? {
        return dayOfWeek?.name
    }

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? {
        return value?.let { DayOfWeek.valueOf(it) }
    }
}
