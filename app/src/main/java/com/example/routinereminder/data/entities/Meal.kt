package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)