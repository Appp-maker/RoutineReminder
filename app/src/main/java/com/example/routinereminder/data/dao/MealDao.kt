package com.example.routinereminder.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.routinereminder.data.entities.Meal

@Dao
interface MealDao {
    @Query("SELECT * FROM meal")
    fun getAll(): List<Meal>
}