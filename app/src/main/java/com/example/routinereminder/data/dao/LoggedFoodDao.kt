package com.example.routinereminder.data.dao

import androidx.room.*
import com.example.routinereminder.data.entities.LoggedFood
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggedFoodDao {

    // Observe all logged foods reactively
    @Query("SELECT * FROM logged_food ORDER BY id DESC")
    fun observeAll(): Flow<List<LoggedFood>>

    // Get all once (non-reactive)
    @Query("SELECT * FROM logged_food ORDER BY id DESC")
    suspend fun getAllOnce(): List<LoggedFood>

    // Get food entries for a specific date (for legacy date string)
    @Query("SELECT * FROM logged_food WHERE date = :date")
    suspend fun getFoodsForDate(date: String): List<LoggedFood>

    // Get one item by ID (for editing or deletion)
    @Query("SELECT * FROM logged_food WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): LoggedFood?

    // Insert or replace
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LoggedFood)

    // Update existing entry
    @Update
    suspend fun update(entity: LoggedFood)

    // Delete by passing object
    @Delete
    suspend fun delete(entity: LoggedFood)

    // Delete by ID (for repository use)
    @Query("DELETE FROM logged_food WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("SELECT * FROM logged_food WHERE startEpochDay = :startEpochDay")
    suspend fun getAllRecurringFromStart(startEpochDay: Long): List<LoggedFood>

    @Query("""
DELETE FROM logged_food
WHERE startEpochDay = :startEpochDay
AND mealSlot = :mealSlot
AND fp_name = :foodName

AND dateEpochDay >= :fromEpochDay
""")
    suspend fun deleteFoodSeriesFromDate(
        startEpochDay: Long,
        mealSlot: String,
        foodName: String,
        fromEpochDay: Long
    )


    @Query("""
DELETE FROM logged_food
WHERE startEpochDay = :startEpochDay
AND mealSlot = :mealSlot
AND fp_name = :foodName

""")
    suspend fun deleteFoodSeries(
        startEpochDay: Long,
        mealSlot: String,
        foodName: String
    )



    @Transaction
    suspend fun upsert(food: LoggedFood) {
        insert(food) // onConflict = REPLACE already updates if it exists
    }

}
