package com.example.routinereminder.data

import androidx.room.TypeConverter
import com.example.routinereminder.data.entities.FoodProduct
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FoodProductConverter {
    @TypeConverter
    fun fromFoodProduct(foodProduct: FoodProduct?): String? {
        return foodProduct?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toFoodProduct(json: String?): FoodProduct? {
        return json?.let { Json.decodeFromString(it) }
    }
}
