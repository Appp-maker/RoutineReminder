package com.example.routinereminder.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_bundle_items",
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
data class FoodBundleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val bundleId: Long,

    // SNAPSHOT of the food (NOT a foreign key!)
    val foodName: String,

    val portionSizeG: Double,

    // nutrition snapshot (important!)
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val saturatedFatG: Double,
    val addedSugarsG: Double,
    val sodiumMg: Double
)
