package com.example.routinereminder.data.dao

import androidx.room.Update
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.routinereminder.data.entities.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    // Used by MainViewModel.upsertScheduleItem()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: Schedule)

    // Optional convenience if you prefer "upsert" semantics elsewhere (ExportImport uses this)
    @Upsert
    suspend fun upsert(entity: Schedule)

    // Used by MainViewModel.deleteScheduleItem()
    @Query("DELETE FROM schedule WHERE id = :id")
    suspend fun delete(id: Long)

    // Used by MainViewModel.getScheduleItemForEditing()
    @Query("SELECT * FROM schedule WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): Schedule?

    @Query("SELECT * FROM schedule WHERE calendarEventId = :calendarEventId LIMIT 1")
    suspend fun getItemByCalendarEventId(calendarEventId: Long): Schedule?

    // Stream all schedule items
    @Query("SELECT * FROM schedule ORDER BY id DESC")
    fun observeAll(): Flow<List<Schedule>>

    @Update
    suspend fun update(schedule: Schedule)

    // Used by ExportImport.getAllOnce()
    @Query("SELECT * FROM schedule ORDER BY id DESC")
    suspend fun getAllOnce(): List<Schedule>

    // -----------------------------------------------------------------------
    // NEW: Mark schedule entry as done / undone
    // -----------------------------------------------------------------------

    @Query("UPDATE schedule SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Long)

    @Query("UPDATE schedule SET isDone = 0 WHERE id = :id")
    suspend fun unmarkDone(id: Long)

    // Optional: only active items
    @Query("SELECT * FROM schedule WHERE isDone = 0 ORDER BY id DESC")
    fun observeAllActive(): Flow<List<Schedule>>
}
