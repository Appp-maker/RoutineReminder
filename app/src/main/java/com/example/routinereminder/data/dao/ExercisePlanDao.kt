package com.example.routinereminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.routinereminder.data.entities.ExercisePlan
import kotlinx.coroutines.flow.Flow

@Dao
interface ExercisePlanDao {

    @Query("SELECT * FROM exercise_plan ORDER BY name ASC")
    fun observeAll(): Flow<List<ExercisePlan>>

    @Query("SELECT * FROM exercise_plan ORDER BY name ASC")
    suspend fun getAllOnce(): List<ExercisePlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: ExercisePlan): Long

    @Update
    suspend fun update(plan: ExercisePlan)

    @Delete
    suspend fun delete(plan: ExercisePlan)
}