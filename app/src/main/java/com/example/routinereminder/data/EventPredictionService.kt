package com.example.routinereminder.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object EventPredictionService {
    enum class TravelMode(val storedValue: String) {
        DRIVING("DRIVING"),
        CYCLING("CYCLING"),
        WALKING("WALKING"),
        PUBLIC_TRANSPORT("PUBLIC_TRANSPORT");

        companion object {
            fun fromStoredValue(value: String?): TravelMode {
                return entries.firstOrNull { it.storedValue == value } ?: DRIVING
            }
        }
    }

    private val client = OkHttpClient()
    private const val NOMINATIM_USER_AGENT = "RoutineReminder/1.0 (contact: support@routine-reminder.app)"

    suspend fun enrich(
        item: ScheduleItem,
        travelMode: TravelMode = TravelMode.DRIVING
    ): ScheduleItem = withContext(Dispatchers.IO) {
        val location = item.location?.trim().orEmpty().ifBlank { null }
        val routeStart = item.routeStart?.trim().orEmpty().ifBlank { null }
        val routeEnd = item.routeEnd?.trim().orEmpty().ifBlank { null }
        val weatherTarget = routeEnd ?: location
        val plannedEventDateTime = resolvePlannedDateTime(item)

        val (weatherLat, weatherLon) = when {
            weatherTarget != null -> geocode(weatherTarget)
            else -> null
        } ?: return@withContext item.copy(
            location = location,
            routeStart = routeStart,
            routeEnd = routeEnd,
            predictedTravelMinutes = null,
            predictedRouteDistanceKm = null,
            weatherSummary = null
        )

        val weatherSummary = fetchWeatherSummary(weatherLat, weatherLon, plannedEventDateTime)
        val predictedTravelMinutes = if (routeStart != null && routeEnd != null) {
            predictTravelMinutes(routeStart, routeEnd, travelMode)
        } else {
            null
        }

        item.copy(
            location = location,
            routeStart = routeStart,
            routeEnd = routeEnd,
            predictedTravelMinutes = routeEstimate?.durationMinutes,
            predictedRouteDistanceKm = routeEstimate?.distanceKm,
            weatherSummary = weatherSummary
        )
    }

    suspend fun estimateRoute(
        start: String,
        end: String,
        travelMode: TravelMode
    ): RouteEstimate? = withContext(Dispatchers.IO) {
        predictRouteEstimate(start, end, travelMode)
    }

    suspend fun addressSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 3) return@withContext emptyList()
        queryNominatimSuggestions(trimmed).ifEmpty {
            queryOpenMeteoSuggestions(trimmed)
        }
    }

    private fun geocode(query: String): Pair<Double, Double>? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return geocodeWithNominatim(encoded) ?: geocodeWithOpenMeteo(encoded)
    }

    private fun queryNominatimSuggestions(query: String): List<String> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&addressdetails=1&dedupe=1&limit=5"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", NOMINATIM_USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val results = org.json.JSONArray(body)
            return buildList {
                for (index in 0 until results.length()) {
                    val entry = results.optJSONObject(index) ?: continue
                    val displayName = entry.optString("display_name").trim()
                    if (displayName.isNotBlank()) add(displayName)
                }
            }.distinct()
        }
    }

    private fun queryOpenMeteoSuggestions(query: String): List<String> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
            return buildList {
                for (index in 0 until results.length()) {
                    val entry = results.optJSONObject(index) ?: continue
                    val name = entry.optString("name")
                    val admin = entry.optString("admin1")
                    val country = entry.optString("country")
                    val display = listOf(name, admin, country).filter { it.isNotBlank() }.joinToString(", ")
                    if (display.isNotBlank()) add(display)
                }
            }.distinct()
        }
    }

    private fun geocodeWithNominatim(encodedQuery: String): Pair<Double, Double>? {
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=jsonv2&limit=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", NOMINATIM_USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val results = org.json.JSONArray(body)
            if (results.length() == 0) return null
            val first = results.optJSONObject(0) ?: return null
            val lat = first.optString("lat").toDoubleOrNull() ?: return null
            val lon = first.optString("lon").toDoubleOrNull() ?: return null
            return lat to lon
        }
    }

    private fun geocodeWithOpenMeteo(encodedQuery: String): Pair<Double, Double>? {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=1&language=en&format=json"
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

    private fun fetchWeatherSummary(
        latitude: Double,
        longitude: Double,
        plannedEventDateTime: LocalDateTime?
    ): String? {
        val url = "https://api.open-meteo.com/v1/forecast?" +
            "latitude=$latitude&longitude=$longitude&hourly=temperature_2m,weather_code&timezone=auto&forecast_days=16"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = JSONObject(body)
            val hourly = root.optJSONObject("hourly") ?: return null
            val times = hourly.optJSONArray("time") ?: return null
            val temperatures = hourly.optJSONArray("temperature_2m") ?: return null
            val weatherCodes = hourly.optJSONArray("weather_code") ?: return null

            val targetDateTime = plannedEventDateTime ?: LocalDateTime.now()
            var closestIndex = -1
            var closestDeltaMinutes = Long.MAX_VALUE
            for (index in 0 until times.length()) {
                val timestamp = times.optString(index)
                val weatherDateTime = runCatching { LocalDateTime.parse(timestamp) }.getOrNull() ?: continue
                val deltaMinutes = kotlin.math.abs(
                    Duration.between(weatherDateTime, targetDateTime).toMinutes()
                )
                if (deltaMinutes < closestDeltaMinutes) {
                    closestDeltaMinutes = deltaMinutes
                    closestIndex = index
                }
            }

            if (closestIndex == -1) return null
            val temp = temperatures.optDouble(closestIndex, Double.NaN)
            val weatherCode = weatherCodes.optInt(closestIndex, -1)
            if (temp.isNaN() || weatherCode == -1) return null
            val condition = weatherCodeToText(weatherCode)
            val timeLabel = targetDateTime.toLocalTime()
                .truncatedTo(ChronoUnit.HOURS)
                .toString()
                .padEnd(5, '0')
            return String.format(Locale.getDefault(), "%s • %.1f°C • %s", timeLabel, temp, condition)
        }
    }

    private fun resolvePlannedDateTime(item: ScheduleItem): LocalDateTime? {
        val plannedDate = when {
            item.isOneTime && item.dateEpochDay != null -> LocalDate.ofEpochDay(item.dateEpochDay)
            !item.isOneTime -> resolveNextOccurrenceDate(item)
            else -> null
        } ?: return null
        return plannedDate.atTime(item.hour.coerceIn(0, 23), item.minute.coerceIn(0, 59))
    }

    private fun resolveNextOccurrenceDate(item: ScheduleItem): LocalDate? {
        val startDate = item.startEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
        val repeatDays = item.repeatOnDays?.takeIf { it.isNotEmpty() } ?: return startDate
        val today = LocalDate.now()
        val searchStart = if (today.isAfter(startDate)) today else startDate
        for (offset in 0L..365L) {
            val candidate = searchStart.plusDays(offset)
            if (candidate.dayOfWeek !in repeatDays) continue
            if (candidate.isBefore(startDate)) continue
            val weeksBetween = ChronoUnit.WEEKS.between(startDate, candidate)
            if (item.repeatEveryWeeks <= 1 || weeksBetween % item.repeatEveryWeeks == 0L) {
                return candidate
            }
        }
        return null
    }

    private fun predictTravelMinutes(start: String, end: String, travelMode: TravelMode): Int? {
        val startCoords = geocode(start) ?: return null
        val endCoords = geocode(end) ?: return null
        if (travelMode == TravelMode.PUBLIC_TRANSPORT) {
            val directKm = haversineKm(startCoords.first, startCoords.second, endCoords.first, endCoords.second)
            if (directKm <= 0.0) return null
            val minutes = ((directKm / 22.0) * 60.0) + 8.0
            return minutes.roundToInt().coerceAtLeast(1)
        }
        val profile = when (travelMode) {
            TravelMode.DRIVING -> "driving"
            TravelMode.CYCLING -> "cycling"
            TravelMode.WALKING -> "walking"
            TravelMode.PUBLIC_TRANSPORT -> "driving"
        }
        val url = "https://router.project-osrm.org/route/v1/$profile/${startCoords.second},${startCoords.first};${endCoords.second},${endCoords.first}?overview=false"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val routes = JSONObject(body).optJSONArray("routes") ?: return null
            if (routes.length() == 0) return null
            val firstRoute = routes.getJSONObject(0)
            val durationSeconds = firstRoute.optDouble("duration", Double.NaN)
            val distanceMeters = firstRoute.optDouble("distance", Double.NaN)
            if (durationSeconds.isNaN() || distanceMeters.isNaN()) return null
            return RouteEstimate(
                durationMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1),
                distanceKm = (distanceMeters / 1000.0).coerceAtLeast(0.0)
            )
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val originLat = Math.toRadians(lat1)
        val targetLat = Math.toRadians(lat2)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2) *
            kotlin.math.cos(originLat) * kotlin.math.cos(targetLat)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadiusKm * c
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
