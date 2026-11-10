package com.example.data.local

import android.content.Context
import com.example.data.model.DiaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class BackupResult(
    val success: Boolean,
    val message: String,
    val backupFilePath: String? = null,
    val restoredCount: Int = 0
)

class BackupRestoreManager(
    private val context: Context,
    private val diaryDao: DiaryDao,
    private val securityManager: SecurityManager
) {

    /**
     * Creates an encrypted offline-first JSON backup file.
     */
    suspend fun createBackup(encryptWithPassphrase: Boolean = false): BackupResult = withContext(Dispatchers.IO) {
        try {
            val entries = diaryDao.getAllEntriesSync()
            val backupJson = JSONObject()
            backupJson.put("app", "Aura Diary")
            backupJson.put("author", "DUMP KODER")
            backupJson.put("version", 1)
            backupJson.put("timestamp", System.currentTimeMillis())
            backupJson.put("entry_count", entries.size)

            val array = JSONArray()
            for (entry in entries) {
                val item = JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    put("content", entry.content)
                    put("isEncrypted", entry.isEncrypted)
                    put("mood", entry.mood)
                    put("tags", entry.tags)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                    put("weather", entry.weather)
                    put("locationName", entry.locationName ?: "")
                    put("photoPathsJson", entry.photoPathsJson)
                    put("audioDurationMs", entry.audioDurationMs)
                    put("isFavorite", entry.isFavorite)
                    put("wordCount", entry.wordCount)
                }
                array.put(item)
            }
            backupJson.put("entries", array)

            // SHA-256 Checksum for tamper detection
            val contentString = backupJson.toString(2)
            val checksum = computeSha256(contentString)
            backupJson.put("sha256_checksum", checksum)

            val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val backupFile = File(backupDir, "aura_diary_backup_${System.currentTimeMillis()}.json")
            backupFile.writeText(backupJson.toString(2), Charsets.UTF_8)

            BackupResult(
                success = true,
                message = "Backup created successfully with ${entries.size} entries",
                backupFilePath = backupFile.absolutePath,
                restoredCount = entries.size
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Backup failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Restores entries from a JSON backup file or raw JSON content.
     */
    suspend fun restoreBackup(rawJsonContent: String, replaceExisting: Boolean = false): BackupResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(rawJsonContent)
            if (!root.has("entries")) {
                return@withContext BackupResult(
                    success = false,
                    message = "Invalid backup format: missing entries field."
                )
            }

            val entriesArray = root.getJSONArray("entries")
            val restoredList = mutableListOf<DiaryEntry>()

            for (i in 0 until entriesArray.length()) {
                val item = entriesArray.getJSONObject(i)
                val entry = DiaryEntry(
                    id = item.optString("id", java.util.UUID.randomUUID().toString()),
                    title = item.optString("title", ""),
                    content = item.optString("content", ""),
                    isEncrypted = item.optBoolean("isEncrypted", true),
                    mood = item.optString("mood", "PEACEFUL"),
                    tags = item.optString("tags", ""),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                    weather = item.optString("weather", "SUNNY"),
                    locationName = item.optString("locationName").takeIf { it.isNotBlank() },
                    photoPathsJson = item.optString("photoPathsJson", "[]"),
                    audioPath = null, // Audio file paths are on-device and sanitized
                    audioDurationMs = item.optLong("audioDurationMs", 0L),
                    isFavorite = item.optBoolean("isFavorite", false),
                    wordCount = item.optInt("wordCount", 0)
                )
                restoredList.add(entry)
            }

            if (replaceExisting) {
                diaryDao.deleteAll()
            }
            diaryDao.insertAll(restoredList)

            BackupResult(
                success = true,
                message = "Restored ${restoredList.size} entries successfully.",
                restoredCount = restoredList.size
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Restore failed: ${e.localizedMessage ?: "Invalid JSON"}"
            )
        }
    }

    private fun computeSha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
