package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight")
data class Weight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weight: Float
)
