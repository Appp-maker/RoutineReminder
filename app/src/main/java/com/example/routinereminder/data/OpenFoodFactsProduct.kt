package com.example.routinereminder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsProduct(
    val product: Product?,
    val status: Int
)

@Serializable
data class Product(
    @SerialName("product_name") val productName: String?,
    val nutriments: Nutriments?
)

@Serializable
data class Nutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("saturated-fat_100g") val saturatedFat100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("sugars_100g") val sugars100g: Double? = null,
    @SerialName("sodium_100g") val sodium100g: Double? = null,
    @SerialName("serving_size") val servingSize: String? = null
)
