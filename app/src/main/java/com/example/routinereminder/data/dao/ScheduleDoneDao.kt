package com.example.routinereminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.routinereminder.data.entities.ScheduleDone

@Dao
interface ScheduleDoneDao {

    // INSERT A DONE RECORD
    @Insert
    suspend fun insert(done: ScheduleDone)

    // DELETE A DONE RECORD
    @Query("DELETE FROM schedule_done WHERE scheduleId = :scheduleId AND dateEpochDay = :day")
    suspend fun deleteDone(scheduleId: Long, day: Long)

    // CHECK IF DONE FOR A DAY
    @Query("SELECT COUNT(*) FROM schedule_done WHERE scheduleId = :scheduleId AND dateEpochDay = :day")
    suspend fun isDone(scheduleId: Long, day: Long): Int


    // 🔥 MARK DONE
    @Query("INSERT OR REPLACE INTO schedule_done (scheduleId, dateEpochDay) VALUES (:scheduleId, :day)")
    suspend fun markDone(scheduleId: Long, day: Long)

    // 🔥 UNMARK DONE
    @Query("DELETE FROM schedule_done WHERE scheduleId = :scheduleId AND dateEpochDay = :day")
    suspend fun unmarkDone(scheduleId: Long, day: Long)

    // 🔥 GET ALL DONE STATES FOR A SINGLE DAY
    @Query("SELECT scheduleId FROM schedule_done WHERE dateEpochDay = :day")
    suspend fun getDoneStatesForDay(day: Long): List<Long>
}
