package com.example.routinereminder.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

object EventPredictionService {
    private val client = OkHttpClient()

    suspend fun enrich(item: ScheduleItem): ScheduleItem = withContext(Dispatchers.IO) {
        val location = item.location?.trim().orEmpty().ifBlank { null }
        val routeStart = item.routeStart?.trim().orEmpty().ifBlank { null }
        val routeEnd = item.routeEnd?.trim().orEmpty().ifBlank { null }

        val (weatherLat, weatherLon) = when {
            routeStart != null -> geocode(routeStart)
            location != null -> geocode(location)
            else -> null
        } ?: return@withContext item.copy(
            location = location,
            routeStart = routeStart,
            routeEnd = routeEnd,
            predictedTravelMinutes = null,
            weatherSummary = null
        )

        val weatherSummary = fetchWeatherSummary(weatherLat, weatherLon)
        val predictedTravelMinutes = if (routeStart != null && routeEnd != null) {
            predictTravelMinutes(routeStart, routeEnd)
        } else {
            null
        }

        item.copy(
            location = location,
            routeStart = routeStart,
            routeEnd = routeEnd,
            predictedTravelMinutes = predictedTravelMinutes,
            weatherSummary = weatherSummary
        )
    }

    private fun geocode(query: String): Pair<Double, Double>? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val first = results.getJSONObject(0)
            return first.optDouble("latitude") to first.optDouble("longitude")
        }
    }

    private fun fetchWeatherSummary(latitude: Double, longitude: Double): String? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val current = JSONObject(body).optJSONObject("current") ?: return null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val weatherCode = current.optInt("weather_code", -1)
            if (temp.isNaN()) return null
            val condition = weatherCodeToText(weatherCode)
            return String.format(Locale.getDefault(), "%.1f°C • %s", temp, condition)
        }
    }

    private fun predictTravelMinutes(start: String, end: String): Int? {
        val startCoords = geocode(start) ?: return null
        val endCoords = geocode(end) ?: return null
        val url = "https://router.project-osrm.org/route/v1/driving/${startCoords.second},${startCoords.first};${endCoords.second},${endCoords.first}?overview=false"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val routes = JSONObject(body).optJSONArray("routes") ?: return null
            if (routes.length() == 0) return null
            val durationSeconds = routes.getJSONObject(0).optDouble("duration", Double.NaN)
            if (durationSeconds.isNaN()) return null
            return (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
        }
    }

    private fun weatherCodeToText(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..67, in 80..82 -> "Rain"
        in 71..77, in 85..86 -> "Snow"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown"
    }
}
