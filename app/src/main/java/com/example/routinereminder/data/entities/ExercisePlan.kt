package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_plan")
data class ExercisePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
