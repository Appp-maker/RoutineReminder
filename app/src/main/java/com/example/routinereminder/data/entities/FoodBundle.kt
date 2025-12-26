package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_bundles")
data class FoodBundle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String
)
