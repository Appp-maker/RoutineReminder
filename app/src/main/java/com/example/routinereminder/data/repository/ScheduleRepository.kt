package com.example.routinereminder.data.repository

import com.example.routinereminder.data.dao.ScheduleDao
import com.example.routinereminder.data.ScheduleItem
import com.example.routinereminder.data.mappers.toEntity
import com.example.routinereminder.data.mappers.toItem
import com.example.routinereminder.model.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val apiService: ApiService
) {
    fun getAllTasks(): Flow<List<ScheduleItem>> =
        scheduleDao.observeAll().map { list -> list.map { it.toItem() } }

    suspend fun getTaskById(taskId: Long): ScheduleItem? =
        scheduleDao.getItemById(taskId)?.toItem()

    /**
     * Add a new task respecting repetition and one-time settings.
     */
    suspend fun addTask(item: ScheduleItem) {
        // One-time event → insert only once
        if (item.isOneTime || item.repeatOnDays.isNullOrEmpty()) {
            scheduleDao.insert(
                item.copy(
                    origin = "APP_CREATED",
                    repeatOnDays = emptySet()
                ).toEntity()
            )
            return
        }

        // Recurring event → insert for each selected day of the week
        val startDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
        val repeatEveryWeeks = item.repeatEveryWeeks.coerceAtLeast(1)
        val maxWeeksToGenerate = 12 // limit, adjust if needed

        for (weekOffset in 0 until maxWeeksToGenerate step repeatEveryWeeks) {
            val baseDate = startDate.plusWeeks(weekOffset.toLong())
            for (day in item.repeatOnDays) {
                val targetDate = baseDate.with(day)
                // Only insert future dates or current week and forward
                if (!targetDate.isBefore(startDate)) {
                    val repeatedItem = item.copy(
                        dateEpochDay = targetDate.toEpochDay(),
                        origin = "APP_CREATED"
                    )
                    scheduleDao.insert(repeatedItem.toEntity())
                }
            }
        }
    }

    suspend fun updateTask(item: ScheduleItem) {
        scheduleDao.update(item.toEntity())
    }

    suspend fun deleteTask(item: ScheduleItem) {
        scheduleDao.delete(item.id)
    }

    suspend fun toggleCompletion(item: ScheduleItem) {
        val updated = item.copy(origin = "APP_UPDATED")
        scheduleDao.update(updated.toEntity())
    }

    /**
     * Example sync stub. Currently does nothing but could be extended
     * to upload schedule entries to a server.
     */
    suspend fun syncTasks() {
        val allItems = scheduleDao.getAllOnce()
        if (allItems.isEmpty()) return
        try {
            // apiService.syncTasks(allItems.map { it.toItem() })
        } catch (e: Exception) {
            // Safe to ignore for now
        }
    }
}
