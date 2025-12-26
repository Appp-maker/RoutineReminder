package com.example.routinereminder.data

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object SnapshotStorage {

    fun saveSnapshotForSession(context: Context, sessionId: String, bmp: Bitmap): String {
        val dir = File(context.filesDir, "session_snapshots")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "${sessionId}.png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun getSnapshotForSession(context: Context, sessionId: String): String? {
        val file = File(context.filesDir, "session_snapshots/${sessionId}.png")
        return if (file.exists()) file.absolutePath else null
    }
}
