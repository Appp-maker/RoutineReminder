package com.example.routinereminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val mealType: String,
    val foodId: Long
)
