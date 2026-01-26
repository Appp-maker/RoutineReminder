package com.example.routinereminder.sync

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.routinereminder.data.AppDatabase
import com.example.routinereminder.util.ExportImportUtil
// import com.google.android.gms.auth.api.signin.GoogleSignInAccount
// import com.google.android.gms.drive.Drive
// import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

interface CloudProvider {
    suspend fun uploadSnapshot(fileName: String, jsonData: String): Boolean
    suspend fun downloadSnapshot(fileName: String): String?
}

class DriveAppDataProvider(
    private val context: Context,
    private val backupUri: Uri
) : CloudProvider {
    override suspend fun uploadSnapshot(fileName: String, jsonData: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(backupUri, "wt")?.bufferedWriter()?.use {
                it.write(jsonData)
            } ?: return@runCatching false
            true
        }.getOrElse { false }
    }

    override suspend fun downloadSnapshot(fileName: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(backupUri)?.bufferedReader()?.use {
                it.readText()
            }
        }.getOrNull()
    }
}

class SyncManager(private val context: Context, private val database: AppDatabase) {

    private val workManager = WorkManager.getInstance(context)
    private val snapshotFileName = "routine_snapshot.json"
    private val exportImportUtil = ExportImportUtil(database)

    // Placeholder for cloudProvider, would be initialized after Google Sign-In
    private var cloudProvider: CloudProvider? = null 

    fun setCloudProvider(provider: CloudProvider) {
        this.cloudProvider = provider
    }

    suspend fun performBackup(): Boolean {
        val currentCloudProvider = cloudProvider ?: return false // Or throw exception/handle no provider
        val jsonData = exportImportUtil.exportAllDataToJson()
        return currentCloudProvider.uploadSnapshot(snapshotFileName, jsonData)
    }

    suspend fun performRestore(): Boolean {
        val currentCloudProvider = cloudProvider ?: return false
        val jsonData = currentCloudProvider.downloadSnapshot(snapshotFileName)
        return if (jsonData != null) {
            exportImportUtil.importAllDataFromJson(jsonData)
        } else {
            false
        }
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Or REPLACE if parameters change
            periodicSyncRequest
        )
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
