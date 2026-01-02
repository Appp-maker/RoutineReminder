package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FoodBundle::class,
            parentColumns = ["id"],
            childColumns = ["bundleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bundleId")]
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bundleId: Long,
    val name: String,
    val portionSizeG: Int,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val saturatedFatG: Double,
    val addedSugarsG: Double,
    val sodiumMg: Double
)
