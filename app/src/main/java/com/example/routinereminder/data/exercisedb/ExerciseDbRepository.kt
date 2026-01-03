package com.example.routinereminder.data.exercisedb

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    suspend fun fetchBodyParts(): Result<List<String>> = fetchStringList("exercises/bodyPartList")

    suspend fun fetchExercises(query: String, bodyPart: String?): Result<List<ExerciseDbExercise>> {
        val trimmedQuery = query.trim()
        val normalizedBodyPart = bodyPart?.takeIf { it.isNotBlank() }

        val path = when {
            trimmedQuery.isNotEmpty() -> "exercises/name/${trimmedQuery}"
            normalizedBodyPart != null -> "exercises/bodyPart/${normalizedBodyPart}"
            else -> "exercises"
        }

        return fetchExerciseList(path)
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
            val type = object : TypeToken<List<ExerciseDbExercise>>() {}.type
            gson.fromJson(array, type)
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
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("ExerciseDB request failed: ${response.code}")
                }
                response.body?.string() ?: throw IOException("ExerciseDB response was empty")
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://exercisedb-api.vercel.app/api/v1/"
    }
}
