package com.example.routinereminder.data.entities

import androidx.room.Entity
@Entity(
    tableName = "schedule_done",
    primaryKeys = ["scheduleId", "dateEpochDay"]
)
data class ScheduleDone(
    val scheduleId: Long,
    val dateEpochDay: Long
)
