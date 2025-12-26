package com.example.routinereminder.data.dao

import androidx.room.*

/**
 * Generic DAO that provides common CRUD operations.
 */

@Dao
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>)

    @Update
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T)

    @Upsert
    suspend fun upsert(entity: T)
}
