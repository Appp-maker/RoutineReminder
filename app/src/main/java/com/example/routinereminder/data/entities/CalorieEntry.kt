package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calorie_entry")
data class CalorieEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val calories: Int
)
