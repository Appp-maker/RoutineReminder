package com.example.routinereminder.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class FoodBundleWithItems(
    @Embedded
    val bundle: FoodBundle,

    @Relation(
        parentColumn = "id",
        entityColumn = "bundleId"
    )
    val items: List<FoodBundleItem>
)
