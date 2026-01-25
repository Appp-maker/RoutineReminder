package com.example.routinereminder.ui

import android.content.Context
import com.example.routinereminder.data.model.SessionStats
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object SessionStore {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val cache = mutableMapOf<String, SessionStats>()

    /** Load all sessions stored as individual JSON files in /files/sessions/ */
    suspend fun loadAllSessions(context: Context): List<SessionStats> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "sessions")
                if (!dir.exists()) return@runCatching emptyList()
                dir.listFiles()
                    ?.sortedByDescending { it.lastModified() }
                    ?.mapNotNull { file ->
                        runCatching {
                            val txt = file.readText()
                            JsonCodec.decode(txt, SessionStats::class.java)
                        }.getOrNull()
                    }
                    ?: emptyList()
            }.getOrElse {
                it.printStackTrace()
                emptyList()
            }
        }
    }

    /** Save one session as its own file */
    fun saveSession(context: Context, s: SessionStats) {
        // Ensure start time and end time are valid
        val session = if (s.startEpochMs <= 0L || s.endEpochMs <= 0L) {
            s.copy(endEpochMs = System.currentTimeMillis())
        } else s


        cache[session.id] = session
        val dir = File(context.filesDir, "sessions")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "session_${session.id}.json")
        file.writeText(JsonCodec.encode(session))
    }

    fun updateSessionSnapshotPath(context: Context, sessionId: String, path: String) {
        // Load the single session
        val session = getSession(context, sessionId) ?: return

        // Update the snapshot path
        val updated = session.copy(snapshotPath = path)

        // Save back to the same file
        saveSession(context, updated)

        // Update cache
        cache[sessionId] = updated
    }


    /** Get a specific session by ID */
    fun getSession(context: Context, id: String): SessionStats? {
        cache[id]?.let { return it }

        val dir = File(context.filesDir, "sessions")
        val file = dir.listFiles()?.find { it.name.contains(id) } ?: return null

        return try {
            val txt = file.readText()
            val session = JsonCodec.decode(txt, SessionStats::class.java)
            if (session != null) cache[id] = session
            session
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Delete a session */
    suspend fun deleteSession(context: Context, session: SessionStats) {
        val dir = File(context.filesDir, "sessions")
        dir.listFiles()?.find { it.name.contains(session.id) }?.delete()
        cache.remove(session.id)
    }

    /** Format timestamps nicely */
    fun formatTime(ts: Long): String = sdf.format(java.util.Date(ts))
}
