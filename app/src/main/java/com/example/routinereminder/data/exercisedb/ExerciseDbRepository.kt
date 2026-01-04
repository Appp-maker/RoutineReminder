package com.example.routinereminder.data.exercisedb

import android.content.Context
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
    val gifUrl: String? = null,
    val videoUrl: String? = null,
    val instructions: List<String> = emptyList()
)

data class ExerciseDbDownloadProgress(
    val downloadedCount: Int,
    val totalCount: Int?,
    val isComplete: Boolean
)

data class ExerciseDbGifDownloadProgress(
    val downloadedCount: Int,
    val totalCount: Int,
    val isComplete: Boolean
)

data class ExerciseDbResponse(
    val exercises: List<ExerciseDbExercise>,
    val totalCount: Int?
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
    private val gifDownloadMutex = Mutex()
    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
    private val gifDirectory = File(context.filesDir, GIF_DIRECTORY_NAME)

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
        runCatching {
            val exercises = loadCachedExercises()
            settingsRepository.saveExerciseDbLastRefresh(System.currentTimeMillis())
            exercises
        }
    }

    suspend fun addCustomExercise(exercise: ExerciseDbExercise): Result<List<ExerciseDbExercise>> = cacheMutex.withLock {
        runCatching {
            val exercises = loadCachedExercises().toMutableList()
            if (exercises.any { it.id == exercise.id }) {
                return@runCatching exercises
            }
            exercises.add(exercise)
            saveCache(exercises)
            cachedExercises = exercises
            settingsRepository.saveExerciseDbCacheTotal(exercises.size)
            settingsRepository.saveExerciseDbCacheComplete(true)
            exercises
        }
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
        onProgress: (ExerciseDbDownloadProgress) -> Unit = {},
        onGifProgress: (ExerciseDbGifDownloadProgress) -> Unit = {}
    ): Result<List<ExerciseDbExercise>> = downloadMutex.withLock {
        runCatching {
            val cached = readCache().orEmpty().toMutableList()
            val cachedComplete = settingsRepository.getExerciseDbCacheComplete().first()
            if (cached.isNotEmpty() && cachedComplete) {
                cachedExercises = cached
                onProgress(ExerciseDbDownloadProgress(cached.size, cached.size, true))
                onGifProgress(getGifDownloadProgress(cached))
                return@runCatching cached
            }

            val totalFromPrefs = settingsRepository.getExerciseDbCacheTotal().first()
            onProgress(ExerciseDbDownloadProgress(cached.size, totalFromPrefs, false))

            val response = downloadAllExercises(onProgress)
            val exercises = response.exercises
            saveCache(exercises)
            downloadExerciseGifs(exercises, onGifProgress)
            settingsRepository.saveExerciseDbCacheComplete(true)
            settingsRepository.saveExerciseDbCacheTotal(response.totalCount ?: exercises.size)
            settingsRepository.saveExerciseDbLastRefresh(System.currentTimeMillis())
            cachedExercises = exercises
            onProgress(ExerciseDbDownloadProgress(exercises.size, exercises.size, true))
            exercises
        }
    }

    suspend fun getGifDownloadProgress(): ExerciseDbGifDownloadProgress = gifDownloadMutex.withLock {
        getGifDownloadProgress(readCache().orEmpty())
    }

    suspend fun downloadExerciseGifsForCachedExercises(
        onProgress: (ExerciseDbGifDownloadProgress) -> Unit = {}
    ) = gifDownloadMutex.withLock {
        val exercises = loadCachedExercises()
        downloadExerciseGifs(exercises, onProgress)
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
            val urlBuilder = if (path.startsWith("http://") || path.startsWith("https://")) {
                path.toHttpUrl().newBuilder()
            } else {
                baseUrl.toHttpUrl().newBuilder().addPathSegments(path)
            }
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
        return Request.Builder()
            .url(url)
            .build()
    }

    private fun parseExercise(item: JsonElement, index: Int): ExerciseDbExercise? {
        if (!item.isJsonObject) return null
        val obj = item.asJsonObject
        val primaryMuscles = obj.readStringList("primaryMuscles", "primary_muscles")
        val category = obj.readString("category")
        val name = obj.readString("name") ?: return null
        val bodyPart = obj.readString("bodyPart", "body_part")
            ?: deriveBodyPart(primaryMuscles, category)
            ?: "Unknown body part"
        val target = obj.readString("target") ?: primaryMuscles.firstOrNull() ?: "Unknown target"
        val equipment = obj.readString("equipment") ?: "Unknown equipment"
        val id = obj.readString("id", "_id", "uuid", "exerciseId")
            ?: "${name}-${bodyPart}-${target}-${equipment}-${index}"
        val gifUrl = normalizeGifUrl(
            obj.readString("gifUrl", "gif_url")
                ?: obj.readNestedString("gif", "url", "gifUrl", "gif_url", "image")
        ) ?: normalizeFallbackImageUrl(
            obj.readStringList("images", "image", "imageUrl", "image_url")
                .firstOrNull()
        )
        val instructions = obj.readInstructionList("instructions", "instruction", "steps")
        val videoUrl = normalizeUrl(
            obj.readString("videoUrl", "video_url", "youtube", "youtubeUrl")
                ?: obj.readNestedString("video", "url", "link", "youtube", "youtubeUrl")
                ?: obj.readNestedStringFromArray("videos", "url", "link", "youtube", "youtubeUrl")
        )
        return ExerciseDbExercise(
            id = id,
            name = name,
            bodyPart = bodyPart,
            target = target,
            equipment = equipment,
            gifUrl = gifUrl,
            videoUrl = videoUrl,
            instructions = instructions
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

    private fun JsonObject.readNestedString(key: String, vararg nestedKeys: String): String? {
        val value = this.get(key) ?: return null
        if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            return value.asString
        }
        if (value.isJsonObject) {
            return value.asJsonObject.readString(*nestedKeys)
        }
        if (value.isJsonArray) {
            return value.asJsonArray.firstNotNullOfOrNull { element ->
                if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                    element.asString
                } else if (element.isJsonObject) {
                    element.asJsonObject.readString(*nestedKeys)
                } else {
                    null
                }
            }
        }
        return null
    }

    private fun JsonObject.readNestedStringFromArray(key: String, vararg nestedKeys: String): String? {
        val value = this.get(key) ?: return null
        if (!value.isJsonArray) return null
        return value.asJsonArray.firstNotNullOfOrNull { element ->
            if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                element.asString
            } else if (element.isJsonObject) {
                element.asJsonObject.readString(*nestedKeys)
            } else {
                null
            }
        }
    }

    private fun JsonObject.readInt(vararg keys: String): Int? {
        for (key in keys) {
            val value = this.get(key)
            if (value != null && !value.isJsonNull) {
                val primitive = value.asJsonPrimitive
                if (primitive.isNumber) {
                    return primitive.asInt
                }
                if (primitive.isString) {
                    return primitive.asString.toIntOrNull()
                }
            }
        }
        return null
    }

    private fun JsonObject.readStringList(vararg keys: String): List<String> {
        for (key in keys) {
            val value = this.get(key)
            if (value != null && value.isJsonArray) {
                return value.asJsonArray.mapNotNull { element ->
                    if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                        element.asString
                    } else {
                        null
                    }
                }
            }
        }
        return emptyList()
    }

    private fun JsonObject.readInstructionList(vararg keys: String): List<String> {
        for (key in keys) {
            val value = this.get(key)
            if (value == null || value.isJsonNull) continue
            if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                return value.asString
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
            if (value.isJsonArray) {
                return value.asJsonArray.mapNotNull { element ->
                    if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                        element.asString.trim()
                    } else {
                        null
                    }
                }.filter { it.isNotBlank() }
            }
        }
        return emptyList()
    }

    private fun deriveBodyPart(primaryMuscles: List<String>, category: String?): String? {
        if (category?.equals("cardio", ignoreCase = true) == true) {
            return "cardio"
        }
        return primaryMuscles.firstNotNullOfOrNull { muscle ->
            when (muscle.lowercase()) {
                "abdominals" -> "waist"
                "abductors", "adductors", "glutes", "hamstrings", "quadriceps" -> "upper legs"
                "calves" -> "lower legs"
                "biceps", "triceps" -> "upper arms"
                "forearms" -> "lower arms"
                "chest" -> "chest"
                "lats", "lower back", "middle back", "traps" -> "back"
                "shoulders" -> "shoulders"
                "neck" -> "neck"
                else -> null
            }
        }
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

    fun getExerciseGifFile(exerciseId: String, gifUrl: String?): File? {
        if (gifUrl.isNullOrBlank()) return null
        val file = gifFileForExerciseId(exerciseId)
        return file.takeIf { it.exists() }
    }

    private fun gifFileForExerciseId(exerciseId: String): File {
        return File(gifDirectory, "${sanitizeFileName(exerciseId)}.gif")
    }

    private fun sanitizeFileName(input: String): String {
        return input.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
    }

    private suspend fun downloadExerciseGifs(
        exercises: List<ExerciseDbExercise>,
        onProgress: (ExerciseDbGifDownloadProgress) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val exercisesWithGifs = exercises.filter { !it.gifUrl.isNullOrBlank() }
        val total = exercisesWithGifs.size
        if (total == 0) {
            onProgress(ExerciseDbGifDownloadProgress(0, 0, true))
            return@withContext
        }
        if (!gifDirectory.exists()) {
            gifDirectory.mkdirs()
        }
        var downloadedCount = exercisesWithGifs.count { exercise ->
            val targetFile = gifFileForExerciseId(exercise.id)
            targetFile.exists() && targetFile.length() > 0L
        }
        onProgress(ExerciseDbGifDownloadProgress(downloadedCount, total, downloadedCount >= total))
        exercisesWithGifs.forEach { exercise ->
            val gifUrl = exercise.gifUrl?.trim()
            if (gifUrl.isNullOrBlank()) return@forEach
            val targetFile = gifFileForExerciseId(exercise.id)
            if (targetFile.exists() && targetFile.length() > 0L) return@forEach
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.download")
            runCatching {
                val request = Request.Builder().url(gifUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching
                    val body = response.body ?: return@runCatching
                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.length() > 0L) {
                        tempFile.renameTo(targetFile)
                    } else {
                        tempFile.delete()
                    }
                }
            }.onFailure {
                tempFile.delete()
            }
            downloadedCount += 1
            onProgress(ExerciseDbGifDownloadProgress(downloadedCount, total, downloadedCount >= total))
        }
    }

    private fun getGifDownloadProgress(
        exercises: List<ExerciseDbExercise>
    ): ExerciseDbGifDownloadProgress {
        val exercisesWithGifs = exercises.filter { !it.gifUrl.isNullOrBlank() }
        val total = exercisesWithGifs.size
        if (total == 0) {
            return ExerciseDbGifDownloadProgress(0, 0, true)
        }
        val downloadedCount = exercisesWithGifs.count { exercise ->
            val targetFile = gifFileForExerciseId(exercise.id)
            targetFile.exists() && targetFile.length() > 0L
        }
        return ExerciseDbGifDownloadProgress(downloadedCount, total, downloadedCount >= total)
    }

    private fun normalizeGifUrl(url: String?): String? {
        val trimmed = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return baseUrl.toHttpUrl().newBuilder()
            .addPathSegments(trimmed.trimStart('/'))
            .build()
            .toString()
    }

    private fun normalizeFallbackImageUrl(path: String?): String? {
        val trimmed = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return FALLBACK_IMAGE_BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments(trimmed.trimStart('/'))
            .build()
            .toString()
    }

    private fun normalizeUrl(url: String?): String? {
        val trimmed = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return "https://$trimmed"
    }

    private fun parseExerciseResponse(json: String): ExerciseDbResponse {
        val element = JsonParser.parseString(json)
        var totalCount: Int? = null
        val array = when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject -> {
                val obj = element.asJsonObject
                totalCount = obj.readInt("total", "count", "totalCount", "total_count")
                when {
                    obj.has("data") -> obj.getAsJsonArray("data")
                    obj.has("results") -> obj.getAsJsonArray("results")
                    obj.has("exercises") -> obj.getAsJsonArray("exercises")
                    else -> JsonArray()
                }
            }
            else -> JsonArray()
        }
        val exercises = array.mapIndexedNotNull { index, item -> parseExercise(item, index) }
        return ExerciseDbResponse(exercises = exercises, totalCount = totalCount)
    }

    private suspend fun downloadAllExercises(
        onProgress: (ExerciseDbDownloadProgress) -> Unit
    ): ExerciseDbResponse {
        val primaryResult = runCatching { downloadExercisesFromApi(onProgress) }
        val primaryResponse = primaryResult.getOrNull()
        if (primaryResponse != null && primaryResponse.exercises.isNotEmpty()) {
            return primaryResponse
        }
        val fallbackResponse = downloadExercisesFromFallback()
        if (fallbackResponse.exercises.isNotEmpty()) {
            onProgress(
                ExerciseDbDownloadProgress(
                    fallbackResponse.exercises.size,
                    fallbackResponse.totalCount ?: fallbackResponse.exercises.size,
                    false
                )
            )
            return fallbackResponse
        }
        primaryResult.exceptionOrNull()?.let { throw it }
        throw IOException("Exercise database was empty.")
    }

    private suspend fun downloadExercisesFromApi(
        onProgress: (ExerciseDbDownloadProgress) -> Unit
    ): ExerciseDbResponse {
        val exercisesById = LinkedHashMap<String, ExerciseDbExercise>()
        var offset = 0
        var totalCount: Int? = null
        while (true) {
            val json = fetchJson(
                EXERCISES_PATH,
                mapOf("limit" to DOWNLOAD_BATCH_SIZE.toString(), "offset" to offset.toString())
            ).getOrThrow()
            val response = parseExerciseResponse(json)
            val startSize = exercisesById.size
            response.exercises.forEach { exercise ->
                exercisesById.putIfAbsent(exercise.id, exercise)
            }
            val added = exercisesById.size - startSize
            totalCount = totalCount ?: response.totalCount
            onProgress(ExerciseDbDownloadProgress(exercisesById.size, totalCount, false))
            if (response.exercises.isEmpty() || added == 0) {
                break
            }
            offset += response.exercises.size
            if (response.exercises.size < DOWNLOAD_BATCH_SIZE) {
                break
            }
        }
        if (exercisesById.isEmpty()) {
            throw IOException("Exercise database was empty.")
        }
        return ExerciseDbResponse(exercisesById.values.toList(), totalCount ?: exercisesById.size)
    }

    private suspend fun downloadExercisesFromFallback(): ExerciseDbResponse {
        val json = fetchJson(FALLBACK_EXERCISES_PATH).getOrThrow()
        val response = parseExerciseResponse(json)
        if (response.exercises.isEmpty()) {
            throw IOException("Exercise database was empty.")
        }
        return response
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://exercisedb.dev/"
        private const val EXERCISES_PATH = "https://exercisedb.dev/api/exercises"
        private const val FALLBACK_EXERCISES_PATH =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
        private const val FALLBACK_IMAGE_BASE_URL =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
        private const val CACHE_FILE_NAME = "exercisedb_cache.json"
        private const val GIF_DIRECTORY_NAME = "exercise_gifs"
        private const val DOWNLOAD_BATCH_SIZE = 250
        private val REFRESH_PROMPT_INTERVAL_MS = TimeUnit.DAYS.toMillis(28)
    }
}
