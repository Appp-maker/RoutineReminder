package com.example.routinereminder.data

import androidx.room.TypeConverter
import com.example.routinereminder.data.entities.FoodProduct
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val gson = Gson()

    // --- LocalDate Converter ---
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? =
        dateString?.let { LocalDate.parse(it, dateFormatter) }

    // --- LocalDateTime Converter ---
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? =
        dateTime?.format(dateTimeFormatter)

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? =
        dateTimeString?.let { LocalDateTime.parse(it, dateTimeFormatter) }

    // --- FoodProduct Converter (serialize to JSON) ---
    @TypeConverter
    fun fromFoodProduct(foodProduct: FoodProduct?): String? = gson.toJson(foodProduct)

    @TypeConverter
    fun toFoodProduct(json: String?): FoodProduct? {
        val type = object : TypeToken<FoodProduct>() {}.type
        return json?.let { gson.fromJson(it, type) }
    }
}
