package com.example.routinereminder.data.repository

import com.example.routinereminder.data.dao.LoggedFoodDao
import com.example.routinereminder.data.entities.LoggedFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class LoggedFoodRepository @Inject constructor(
    private val loggedFoodDao: LoggedFoodDao
) {

    fun getAllLoggedFoods(): Flow<List<LoggedFood>> =
        loggedFoodDao.observeAll().map { it }

    suspend fun getLoggedFoodById(id: Long): LoggedFood? =
        loggedFoodDao.getItemById(id)

    /**
     * Add a new food entry respecting repetition and one-time settings.
     */
    suspend fun addLoggedFood(item: LoggedFood) = withContext(Dispatchers.IO) {
        // Determine valid start anchor
        val startDate = item.startEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()

        // ----- One-time food entry -----
        if (item.isOneTime || item.repeatOnDays.isNullOrEmpty()) {
            loggedFoodDao.insert(
                item.copy(
                    startEpochDay = startDate.toEpochDay(),
                    dateEpochDay = item.dateEpochDay ?: startDate.toEpochDay()
                )
            )
            return@withContext
        }

        // ----- Recurring food entry -----
        val repeatEveryWeeks = item.repeatEveryWeeks.coerceAtLeast(1)
        val maxWeeksToGenerate = 12 // safety limit

        for (weekOffset in 0 until maxWeeksToGenerate step repeatEveryWeeks) {
            val baseDate = startDate.plusWeeks(weekOffset.toLong())
            for (day in item.repeatOnDays) {
                val targetDate = baseDate.with(day)
                if (!targetDate.isBefore(startDate)) {
                    val repeatedItem = item.copy(
                        dateEpochDay = targetDate.toEpochDay(),
                        startEpochDay = startDate.toEpochDay()
                    )
                    loggedFoodDao.insert(repeatedItem)
                }
            }
        }
    }

    suspend fun updateLoggedFood(item: LoggedFood) {
        loggedFoodDao.update(item)
    }

    suspend fun deleteLoggedFood(item: LoggedFood) {
        loggedFoodDao.delete(item.id)
    }
}
