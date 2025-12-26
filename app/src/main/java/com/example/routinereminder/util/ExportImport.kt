package com.example.routinereminder.util

import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.data.entities.LoggedFood
import com.example.routinereminder.data.entities.Schedule
import com.example.routinereminder.data.entities.Weight
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

// ✅ Snapshot now uses the Room entity types
data class AppSnapshot(
    val schedule: List<Schedule>,
    val foods: List<LoggedFood>,
    val weights: List<Weight>,
    val ts: Long = System.currentTimeMillis()
)

class ExportImportUtil @Inject constructor(private val database: AppDatabase) {

    private val gson = Gson()

    suspend fun exportAllDataToJson(): String {
        val snapshot = AppSnapshot(
            schedule = database.scheduleDao().getAllOnce(),
            foods = database.loggedFoodDao().getAllOnce(),
            weights = database.weightDao().getAllOnce()
        )
        return gson.toJson(snapshot)
    }

    suspend fun exportAllDataToStream(outputStream: OutputStream) {
        outputStream.bufferedWriter().use { it.write(exportAllDataToJson()) }
    }

    suspend fun importAllDataFromJson(jsonData: String): Boolean {
        return try {
            val type = object : TypeToken<AppSnapshot>() {}.type
            val snapshot: AppSnapshot = gson.fromJson(jsonData, type)

            val scheduleDao = database.scheduleDao()
            snapshot.schedule.forEach { scheduleDao.upsert(it) }

            val loggedFoodDao = database.loggedFoodDao()
            snapshot.foods.forEach { loggedFoodDao.upsert(it) }

            val weightDao = database.weightDao()
            snapshot.weights.forEach { weightDao.upsert(it) } // ← use insert(...) if you don't have upsert
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importAllDataFromStream(inputStream: InputStream): Boolean =
        try {
            val jsonData = inputStream.bufferedReader().use { it.readText() }
            importAllDataFromJson(jsonData)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
}
