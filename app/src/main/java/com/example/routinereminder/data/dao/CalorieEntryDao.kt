package com.example.routinereminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.routinereminder.data.entities.CalorieEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieEntryDao {
    @Query("SELECT * FROM calorie_entry")
    fun getAll(): Flow<List<CalorieEntry>>

    @Query("SELECT COALESCE(SUM(calories), 0) FROM calorie_entry WHERE dateEpochDay = :dateEpochDay")
    suspend fun totalCaloriesForDate(dateEpochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(calorieEntry: CalorieEntry)
}
