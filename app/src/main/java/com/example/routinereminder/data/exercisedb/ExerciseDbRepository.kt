package com.example.routinereminder.data.exercisedb

import com.example.routinereminder.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


data class ExerciseDbExercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val target: String,
    val equipment: String,
    val gifUrl: String? = null
)

class ExerciseDbRepository(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {
    private var fallbackExercises: List<ExerciseDbExercise>? = null
    private var fallbackBodyParts: List<String>? = null

    suspend fun fetchBodyParts(): Result<List<String>> = fetchStringList("exercises/bodyPartList")
        .recoverCatching { loadFallbackBodyParts() }

    suspend fun fetchExercises(query: String, bodyPart: String?): Result<List<ExerciseDbExercise>> {
        val trimmedQuery = query.trim()
        val normalizedBodyPart = bodyPart?.takeIf { it.isNotBlank() }

        val path = when {
            trimmedQuery.isNotEmpty() -> "exercises/name/${trimmedQuery}"
            normalizedBodyPart != null -> "exercises/bodyPart/${normalizedBodyPart}"
            else -> "exercises"
        }

        return fetchExerciseList(path)
            .recoverCatching {
                loadFallbackExercises(trimmedQuery, normalizedBodyPart)
            }
            .map { exercises ->
                if (normalizedBodyPart != null && trimmedQuery.isNotEmpty()) {
                    exercises.filter { it.bodyPart.equals(normalizedBodyPart, ignoreCase = true) }
                } else {
                    exercises
                }
            }
    }

    private suspend fun fetchExerciseList(path: String): Result<List<ExerciseDbExercise>> =
        fetchJson(path).mapCatching { json ->
            val element = JsonParser.parseString(json)
            val array = when {
                element.isJsonArray -> element.asJsonArray
                element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.getAsJsonArray("data")
                element.isJsonObject && element.asJsonObject.has("results") -> element.asJsonObject.getAsJsonArray("results")
                else -> JsonArray()
            }
            array.mapIndexedNotNull { index, item -> parseExercise(item, index) }
        }

    private suspend fun fetchStringList(path: String): Result<List<String>> = fetchJson(path).mapCatching { json ->
        val element = JsonParser.parseString(json)
        val array = when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.getAsJsonArray("data")
            else -> JsonArray()
        }
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(array, type)
    }

    private suspend fun fetchJson(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = baseUrl.toHttpUrl().newBuilder().addPathSegments(path).build()
            val request = buildRequest(url)
            client.newCall(request).execute().use { response ->
                if (response.code == 404 || response.code == 204) {
                    return@runCatching "[]"
                }
                if (!response.isSuccessful) {
                    val message = if (response.code == 429) {
                        "ExerciseDB rate limit reached. Please try again shortly."
                    } else {
                        "ExerciseDB request failed: ${response.code}"
                    }
                    throw IOException(message)
                }
                response.body?.string() ?: throw IOException("ExerciseDB response was empty")
            }
        }
    }

    private suspend fun fetchJsonFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("ExerciseDB fallback request failed: ${response.code}")
                }
                response.body?.string() ?: throw IOException("ExerciseDB fallback response was empty")
            }
        }
    }

    private fun buildRequest(url: HttpUrl): Request {
        val builder = Request.Builder().url(url)
        val rapidApiKey = BuildConfig.EXERCISEDB_RAPIDAPI_KEY
        val rapidApiHost = BuildConfig.EXERCISEDB_RAPIDAPI_HOST
        if (rapidApiKey.isNotBlank()) {
            builder.addHeader("x-rapidapi-key", rapidApiKey)
        }
        if (rapidApiHost.isNotBlank()) {
            builder.addHeader("x-rapidapi-host", rapidApiHost)
        }
        return builder.build()
    }

    private fun parseExercise(item: JsonElement, index: Int): ExerciseDbExercise? {
        if (!item.isJsonObject) return null
        val obj = item.asJsonObject
        val name = obj.readString("name") ?: return null
        val bodyPart = obj.readString("bodyPart", "body_part") ?: "Unknown body part"
        val target = obj.readString("target") ?: "Unknown target"
        val equipment = obj.readString("equipment") ?: "Unknown equipment"
        val id = obj.readString("id", "_id", "uuid", "exerciseId")
            ?: "${name}-${bodyPart}-${target}-${equipment}-${index}"
        val gifUrl = obj.readString("gifUrl", "gif_url")
        return ExerciseDbExercise(
            id = id,
            name = name,
            bodyPart = bodyPart,
            target = target,
            equipment = equipment,
            gifUrl = gifUrl
        )
    }

    private fun JsonObject.readString(vararg keys: String): String? {
        for (key in keys) {
            val value = this.get(key)
            if (value != null && !value.isJsonNull) {
                return value.asString
            }
        }
        return null
    }

    private suspend fun loadFallbackBodyParts(): List<String> {
        val cached = fallbackBodyParts
        if (cached != null) return cached
        val exercises = loadFallbackExercisesFromDataset()
        val bodyParts = exercises.map { it.bodyPart }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        fallbackBodyParts = bodyParts
        return bodyParts
    }

    private suspend fun loadFallbackExercises(query: String, bodyPart: String?): List<ExerciseDbExercise> {
        val exercises = loadFallbackExercisesFromDataset()
        val queryNormalized = query.trim().lowercase()
        return exercises.filter { exercise ->
            val matchesQuery = queryNormalized.isBlank() || exercise.name.lowercase().contains(queryNormalized)
            val matchesBodyPart = bodyPart.isNullOrBlank() || exercise.bodyPart.equals(bodyPart, ignoreCase = true)
            matchesQuery && matchesBodyPart
        }
    }

    private suspend fun loadFallbackExercisesFromDataset(): List<ExerciseDbExercise> {
        val cached = fallbackExercises
        if (cached != null) return cached
        val json = fetchJsonFromUrl(FALLBACK_DATASET_URL).getOrThrow()
        val element = JsonParser.parseString(json)
        if (!element.isJsonArray) {
            return emptyList()
        }
        val exercises = element.asJsonArray.mapIndexedNotNull { index, item ->
            if (!item.isJsonObject) return@mapIndexedNotNull null
            val obj = item.asJsonObject
            val name = obj.readString("name") ?: return@mapIndexedNotNull null
            val equipment = obj.readString("equipment") ?: "Unknown equipment"
            val id = obj.readString("id") ?: "${name}-${index}"
            val primaryMuscles = obj.readStringList("primaryMuscles")
            val secondaryMuscles = obj.readStringList("secondaryMuscles")
            val bodyPart = primaryMuscles.firstOrNull() ?: "Unknown body part"
            val target = secondaryMuscles.firstOrNull() ?: "General"
            ExerciseDbExercise(
                id = id,
                name = name,
                bodyPart = bodyPart,
                target = target,
                equipment = equipment,
                gifUrl = null
            )
        }
        fallbackExercises = exercises
        return exercises
    }

    private fun JsonObject.readStringList(key: String): List<String> {
        val value = this.get(key)
        if (value == null || !value.isJsonArray) return emptyList()
        return value.asJsonArray.mapNotNull { item ->
            if (item.isJsonNull) null else item.asString
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://exercisedb.p.rapidapi.com/"
        const val FALLBACK_DATASET_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
    }
}
