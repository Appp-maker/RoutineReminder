package com.example.routinereminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.routinereminder.data.entities.BlockedCalendarImport
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCalendarImportDao {
    @Query("SELECT * FROM blocked_calendar_import")
    fun getAll(): Flow<List<BlockedCalendarImport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedCalendarImport: BlockedCalendarImport)

    @Query("SELECT * FROM blocked_calendar_import ORDER BY id DESC")
    suspend fun getAllOnce(): List<BlockedCalendarImport>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(blockedImport: BlockedCalendarImport) // ✅ renamed parameter

    @Query("DELETE FROM blocked_calendar_import WHERE calendarEventId = :calendarEventId")
    suspend fun removeBlockedImport(calendarEventId: Long)
}
