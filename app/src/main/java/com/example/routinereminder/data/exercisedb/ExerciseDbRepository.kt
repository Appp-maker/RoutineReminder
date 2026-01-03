package com.example.routinereminder.data.exercisedb

import android.content.Context
import com.example.routinereminder.BuildConfig
import com.example.routinereminder.data.SettingsRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


data class ExerciseDbExercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val target: String,
    val equipment: String,
    val gifUrl: String? = null
)

data class ExerciseDbDownloadProgress(
    val downloadedCount: Int,
    val totalCount: Int?,
    val isComplete: Boolean
)

@Singleton
class ExerciseDbRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val client: OkHttpClient,
    private val gson: Gson,
    @Named("exerciseDbBaseUrl") private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val cacheMutex = Mutex()
    private val refreshMutex = Mutex()
    private val downloadMutex = Mutex()
    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)

    private var cachedExercises: List<ExerciseDbExercise>? = null

    suspend fun fetchBodyParts(): Result<List<String>> = runCatching {
        val exercises = loadCachedExercises()
        exercises.map { it.bodyPart }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    suspend fun fetchExercises(query: String, bodyPart: String?): Result<List<ExerciseDbExercise>> = runCatching {
        val exercises = loadCachedExercises()
        val trimmedQuery = query.trim().lowercase()
        val normalizedBodyPart = bodyPart?.takeIf { it.isNotBlank() }
        exercises.filter { exercise ->
            val matchesQuery = trimmedQuery.isBlank() || exercise.name.lowercase().contains(trimmedQuery)
            val matchesBodyPart = normalizedBodyPart.isNullOrBlank() ||
                exercise.bodyPart.equals(normalizedBodyPart, ignoreCase = true)
            matchesQuery && matchesBodyPart
        }
    }

    suspend fun refreshExerciseDatabase(): Result<List<ExerciseDbExercise>> = refreshMutex.withLock {
        cacheMutex.withLock {
            cachedExercises = null
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            settingsRepository.saveExerciseDbCacheComplete(false)
            settingsRepository.saveExerciseDbCacheTotal(null)
        }
        downloadExerciseDatabase()
    }

    suspend fun getDownloadProgress(): ExerciseDbDownloadProgress {
        val cached = readCache().orEmpty()
        val total = settingsRepository.getExerciseDbCacheTotal().first()
        val isComplete = settingsRepository.getExerciseDbCacheComplete().first()
        if (cached.isNotEmpty() && !isComplete && total == null) {
            settingsRepository.saveExerciseDbCacheComplete(true)
            settingsRepository.saveExerciseDbCacheTotal(cached.size)
            return ExerciseDbDownloadProgress(cached.size, cached.size, true)
        }
        val complete = (isComplete && cached.isNotEmpty()) || (total != null && cached.size >= total && cached.isNotEmpty())
        return ExerciseDbDownloadProgress(cached.size, total, complete)
    }

    suspend fun downloadExerciseDatabase(
        onProgress: (ExerciseDbDownloadProgress) -> Unit = {}
    ): Result<List<ExerciseDbExercise>> = downloadMutex.withLock {
        runCatching {
            val cached = readCache().orEmpty().toMutableList()
            val cachedComplete = settingsRepository.getExerciseDbCacheComplete().first()
            if (cached.isNotEmpty() && cachedComplete) {
                cachedExercises = cached
                onProgress(ExerciseDbDownloadProgress(cached.size, cached.size, true))
                return@runCatching cached
            }

            val totalFromPrefs = settingsRepository.getExerciseDbCacheTotal().first()
            onProgress(ExerciseDbDownloadProgress(cached.size, totalFromPrefs, false))

            var offset = cached.size
            var requestCount = 0
            var total = totalFromPrefs
            val accumulated = cached
            var completed = false

            while (requestCount < MAX_DOWNLOAD_REQUESTS) {
                val json = fetchJson(
                    "exercises",
                    mapOf(
                        "limit" to DOWNLOAD_PAGE_SIZE.toString(),
                        "offset" to offset.toString()
                    )
                ).getOrThrow()
                val exercises = parseExerciseListFromJson(json)
                total = total ?: parseTotalCount(json)
                if (exercises.isEmpty()) {
                    completed = true
                    break
                }
                accumulated.addAll(exercises)
                offset = accumulated.size
                requestCount += 1
                saveCache(accumulated)
                settingsRepository.saveExerciseDbCacheTotal(total)
                onProgress(ExerciseDbDownloadProgress(accumulated.size, total, false))
                if (total != null && accumulated.size >= total) {
                    completed = true
                    break
                }
                if (exercises.size < DOWNLOAD_PAGE_SIZE) {
                    completed = true
                    break
                }
                kotlinx.coroutines.delay(DOWNLOAD_REQUEST_DELAY_MS)
            }

            val isComplete = completed || (total != null && accumulated.size >= total)
            settingsRepository.saveExerciseDbCacheComplete(isComplete)
            val resolvedTotal = if (isComplete) {
                total ?: accumulated.size
            } else {
                total
            }
            settingsRepository.saveExerciseDbCacheTotal(resolvedTotal)
            if (isComplete) {
                settingsRepository.saveExerciseDbLastRefresh(System.currentTimeMillis())
            }
            cachedExercises = accumulated
            onProgress(ExerciseDbDownloadProgress(accumulated.size, resolvedTotal, isComplete))
            accumulated
        }
    }

    suspend fun shouldPromptForRefresh(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val lastRefresh = settingsRepository.getExerciseDbLastRefresh().first()
        val lastPrompt = settingsRepository.getExerciseDbLastPrompt().first()
        val baseline = maxOf(lastRefresh, lastPrompt)
        return baseline > 0 && nowEpochMs - baseline >= REFRESH_PROMPT_INTERVAL_MS
    }

    suspend fun recordRefreshPromptShown(nowEpochMs: Long = System.currentTimeMillis()) {
        settingsRepository.saveExerciseDbLastPrompt(nowEpochMs)
    }

    suspend fun recordRefreshPromptDismissed(nowEpochMs: Long = System.currentTimeMillis()) {
        settingsRepository.saveExerciseDbLastPrompt(nowEpochMs)
    }

    private suspend fun fetchJson(
        path: String,
        queryParams: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val urlBuilder = baseUrl.toHttpUrl().newBuilder().addPathSegments(path)
            queryParams.forEach { (key, value) ->
                urlBuilder.addQueryParameter(key, value)
            }
            val url = urlBuilder.build()
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

    private suspend fun loadCachedExercises(): List<ExerciseDbExercise> = cacheMutex.withLock {
        val cached = cachedExercises
        if (cached != null) return cached
        val diskExercises = readCache()
        if (diskExercises != null && diskExercises.isNotEmpty()) {
            cachedExercises = diskExercises
            val total = settingsRepository.getExerciseDbCacheTotal().first()
            val isComplete = settingsRepository.getExerciseDbCacheComplete().first()
            if (!isComplete && (total == null || diskExercises.size >= total)) {
                settingsRepository.saveExerciseDbCacheComplete(true)
                settingsRepository.saveExerciseDbCacheTotal(diskExercises.size)
            }
            if (settingsRepository.getExerciseDbLastRefresh().first() == 0L) {
                settingsRepository.saveExerciseDbLastRefresh(cacheFile.lastModified())
            }
            return diskExercises
        }
        throw IOException("Exercise database has not finished downloading yet.")
    }

    private suspend fun readCache(): List<ExerciseDbExercise>? = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext null
        val text = cacheFile.readText()
        if (text.isBlank()) return@withContext null
        val type = object : TypeToken<List<ExerciseDbExercise>>() {}.type
        gson.fromJson<List<ExerciseDbExercise>>(text, type)
    }

    private suspend fun saveCache(exercises: List<ExerciseDbExercise>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(exercises)
        cacheFile.writeText(json)
    }

    private fun parseExerciseListFromJson(json: String): List<ExerciseDbExercise> {
        val element = JsonParser.parseString(json)
        val array = when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.getAsJsonArray("data")
            element.isJsonObject && element.asJsonObject.has("results") -> element.asJsonObject.getAsJsonArray("results")
            else -> JsonArray()
        }
        return array.mapIndexedNotNull { index, item -> parseExercise(item, index) }
    }

    private fun parseTotalCount(json: String): Int? {
        val element = JsonParser.parseString(json)
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val keys = listOf("total", "count", "totalCount")
        return keys.firstNotNullOfOrNull { key ->
            if (obj.has(key) && obj.get(key).isJsonPrimitive) {
                obj.get(key).asInt
            } else {
                null
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://exercisedb.p.rapidapi.com/"
        private const val CACHE_FILE_NAME = "exercisedb_cache.json"
        private val REFRESH_PROMPT_INTERVAL_MS = TimeUnit.DAYS.toMillis(28)
        private const val DOWNLOAD_PAGE_SIZE = 250
        private const val MAX_DOWNLOAD_REQUESTS = 20
        private const val DOWNLOAD_REQUEST_DELAY_MS = 500L
    }
}
