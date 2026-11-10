package com.example.sync

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.repository.HabitRepository
import com.example.util.BackupExportUtil
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupItem(
    val id: String,
    val name: String,
    val timestamp: Long,
    val habitCount: Int,
    val entryCount: Int,
    val jsonData: String
)

class CloudBackupManager(
    private val context: Context,
    private val repository: HabitRepository
) {
    private val TAG = "CloudBackupManager"

    private val firestore: FirebaseFirestore by lazy {
        val dbId = try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            null
        }
        if (!dbId.isNullOrEmpty()) {
            Firebase.firestore(dbId)
        } else {
            Firebase.firestore
        }
    }

    private val backupDir: File by lazy {
        File(context.filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Creates an emergency local snapshot before any destructive action
     */
    suspend fun createEmergencyLocalBackup(reason: String = "safety"): String = withContext(Dispatchers.IO) {
        try {
            val habits = repository.getAllActiveHabitsOnce()
            val entries = repository.getAllActiveEntriesOnce()
            val settings = repository.getSettingsOnce()

            val json = BackupExportUtil.generateBackupJson(habits, entries, settings)
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "emergency_${reason}_$timeStr.json")
            file.writeText(json)
            Log.d(TAG, "Emergency local backup saved to: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create emergency backup: ${e.message}", e)
            ""
        }
    }

    /**
     * Back up now: Saves JSON backup to cloud Firestore under users/{uid}/backups and local filesDir,
     * maintaining max 7 backups.
     */
    suspend fun createBackup(userId: String): Result<BackupItem> = withContext(Dispatchers.IO) {
        try {
            val habits = repository.getAllActiveHabitsOnce()
            val entries = repository.getAllActiveEntriesOnce()
            val settings = repository.getSettingsOnce()

            val json = BackupExportUtil.generateBackupJson(habits, entries, settings)
            val timestamp = System.currentTimeMillis()
            val dateFormatted = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date(timestamp))
            val backupId = "backup_$timestamp"
            val backupName = "P_Habits_Backup_$dateFormatted.json"

            val backupItem = BackupItem(
                id = backupId,
                name = backupName,
                timestamp = timestamp,
                habitCount = habits.size,
                entryCount = entries.size,
                jsonData = json
            )

            // 1. Save to cloud Firestore
            val backupDoc = firestore.collection("users").document(userId)
                .collection("backups").document(backupId)

            backupDoc.set(
                mapOf(
                    "id" to backupId,
                    "name" to backupName,
                    "timestamp" to timestamp,
                    "habitCount" to habits.size,
                    "entryCount" to entries.size,
                    "jsonData" to json
                )
            ).await()

            // 2. Save copy locally
            val localFile = File(backupDir, "$backupId.json")
            localFile.writeText(json)

            // 3. Update last backup timestamp in settings
            repository.updateSettings(settings.copy(lastBackupTimestamp = timestamp))

            // 4. Prune older backups keeping only the last 7
            pruneBackups(userId)

            Result.success(backupItem)
        } catch (e: Exception) {
            Log.e(TAG, "Backup creation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * List available backups for user ordered newest to oldest
     */
    suspend fun listBackups(userId: String): Result<List<BackupItem>> = withContext(Dispatchers.IO) {
        try {
            val querySnap = firestore.collection("users").document(userId)
                .collection("backups")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val cloudList = querySnap.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: "Backup"
                val ts = doc.getLong("timestamp") ?: 0L
                val hc = (doc.getLong("habitCount") ?: 0L).toInt()
                val ec = (doc.getLong("entryCount") ?: 0L).toInt()
                val json = doc.getString("jsonData") ?: ""
                BackupItem(id, name, ts, hc, ec, json)
            }

            // Also check local backup directory if cloud is empty or offline
            if (cloudList.isEmpty()) {
                val localFiles = backupDir.listFiles { _, name -> name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

                val localList = localFiles.take(7).mapNotNull { file ->
                    try {
                        val text = file.readText()
                        val (h, e, _) = BackupExportUtil.parseBackupJson(text)
                        BackupItem(
                            id = file.nameWithoutExtension,
                            name = file.name,
                            timestamp = file.lastModified(),
                            habitCount = h.size,
                            entryCount = e.size,
                            jsonData = text
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                Result.success(localList)
            } else {
                Result.success(cloudList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list backups: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restore from backup: creates an emergency backup first, then restores habits, entries, and settings.
     */
    suspend fun restoreBackup(backup: BackupItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // SAFETY: Save emergency local backup before wiping current state
            createEmergencyLocalBackup("pre_restore")

            val (habits, entries, settings) = BackupExportUtil.parseBackupJson(backup.jsonData)
            if (habits.isEmpty() && entries.isEmpty()) {
                return@withContext Result.failure(Exception("Backup file contains no habits or check-ins"))
            }

            repository.restoreBackup(habits, entries)
            if (settings != null) {
                repository.updateSettings(settings)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Prune backups keeping the last 7
     */
    private suspend fun pruneBackups(userId: String) {
        try {
            val querySnap = firestore.collection("users").document(userId)
                .collection("backups")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            if (querySnap.size() > 7) {
                val toDelete = querySnap.documents.drop(7)
                for (doc in toDelete) {
                    doc.reference.delete().await()
                }
            }

            val localFiles = backupDir.listFiles { _, name -> name.startsWith("backup_") && name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()

            if (localFiles.size > 7) {
                localFiles.drop(7).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Prune failed: ${e.message}")
        }
    }

    /**
     * Check if auto daily backup is needed (> 24 hours since last backup)
     */
    suspend fun checkDailyAutoBackup(userId: String) {
        val settings = repository.getSettingsOnce()
        if (!settings.autoBackupEnabled) return

        val now = System.currentTimeMillis()
        val oneDayMillis = 24L * 60L * 60L * 1000L
        if (now - settings.lastBackupTimestamp >= oneDayMillis) {
            Log.d(TAG, "Triggering automatic daily backup")
            createBackup(userId)
        }
    }
}
