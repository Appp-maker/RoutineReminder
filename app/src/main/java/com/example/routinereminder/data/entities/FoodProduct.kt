package com.example.routinereminder.data.entities

import kotlinx.serialization.Serializable

@Serializable
data class FoodProduct(
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    val saturatedFatPer100g: Double,
    val addedSugarsPer100g: Double,
    val sodiumPer100g: Double,

    val servingSizeG: Double? = null,
    val caloriesPerServing: Double? = null,
    val proteinPerServing: Double? = null,
    val carbsPerServing: Double? = null,
    val fatPerServing: Double? = null,
    val fiberPerServing: Double? = null,
    val saturatedFatPerServing: Double? = null,
    val addedSugarsPerServing: Double? = null,
    val sodiumPerServing: Double? = null
)
