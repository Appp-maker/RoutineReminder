package com.example.routinereminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.routinereminder.data.entities.Weight
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight")
    fun getAll(): Flow<List<Weight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weight: Weight)
    @Query("SELECT * FROM weight ORDER BY id DESC")
    suspend fun getAllOnce(): List<Weight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(weight: Weight)
}