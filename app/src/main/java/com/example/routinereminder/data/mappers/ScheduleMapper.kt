package com.example.routinereminder.data.mappers

import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.entities.Schedule

// Map from UI model → Room entity
fun ScheduleItem.toEntity(): Schedule = Schedule(
    id = id,
    name = name,
    notes = notes,
    hour = hour,
    minute = minute,
    durationMinutes = durationMinutes,
    isOneTime = isOneTime,
    dateEpochDay = dateEpochDay,
    startEpochDay = startEpochDay,
    repeatOnDays = repeatOnDays,
    repeatEveryWeeks = repeatEveryWeeks,
    notifyEnabled = notifyEnabled,
    showDetailsInNotification = showDetailsInNotification,
    reminderCount = reminderCount,
    reminderIntervalMinutes = reminderIntervalMinutes,
    addToCalendarOnSave = addToCalendarOnSave,
    calendarEventId = calendarEventId,
    origin = origin,
    targetCalendarSystem = targetCalendarSystem,
    colorArgb = colorArgb,
    setId = setId,
    isMuted = isMuted
)

// Map from Room entity → UI model
fun Schedule.toItem(): ScheduleItem =
    ScheduleItem(
        id = id,
        name = name,
        notes = notes,
        hour = hour,
        minute = minute,
        durationMinutes = durationMinutes,
        isOneTime = isOneTime,
        dateEpochDay = dateEpochDay,
        startEpochDay = startEpochDay,
        repeatOnDays = repeatOnDays,
        repeatEveryWeeks = repeatEveryWeeks,
        notifyEnabled = notifyEnabled,
        showDetailsInNotification = showDetailsInNotification,
        reminderCount = reminderCount,
        reminderIntervalMinutes = reminderIntervalMinutes,
        addToCalendarOnSave = addToCalendarOnSave,
        calendarEventId = calendarEventId,
        origin = origin,
        targetCalendarSystem = targetCalendarSystem,
        colorArgb = colorArgb,
        setId = setId,
        isDone = isDone,
        isMuted = isMuted
    )
