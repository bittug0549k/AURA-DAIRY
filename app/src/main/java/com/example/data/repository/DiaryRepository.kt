package com.example.data.repository

import com.example.data.local.DiaryDao
import com.example.data.local.SecurityManager
import com.example.data.model.DiaryEntry
import com.example.data.model.DiaryStats
import com.example.data.model.Mood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Calendar

class DiaryRepository(
    private val diaryDao: DiaryDao,
    private val securityManager: SecurityManager
) {

    /**
     * Flow of all entries with decrypted content for UI presentation.
     * Decryption executes on background dispatcher (Dispatchers.Default) to keep UI 60/120 FPS.
     */
    fun getAllEntries(): Flow<List<DiaryEntry>> {
        return diaryDao.getAllEntriesFlow().map { list ->
            list.map { decryptEntryIfNeeded(it) }
        }.flowOn(Dispatchers.Default)
    }

    fun getFavoriteEntries(): Flow<List<DiaryEntry>> {
        return diaryDao.getFavoriteEntriesFlow().map { list ->
            list.map { decryptEntryIfNeeded(it) }
        }.flowOn(Dispatchers.Default)
    }

    fun searchEntries(query: String): Flow<List<DiaryEntry>> {
        return diaryDao.getAllEntriesFlow().map { list ->
            val decryptedList = list.map { decryptEntryIfNeeded(it) }
            if (query.isBlank()) {
                decryptedList
            } else {
                decryptedList.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true) ||
                            it.tags.contains(query, ignoreCase = true) ||
                            it.mood.contains(query, ignoreCase = true)
                }
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getEntryById(id: String): DiaryEntry? = withContext(Dispatchers.IO) {
        val entry = diaryDao.getEntryById(id) ?: return@withContext null
        decryptEntryIfNeeded(entry)
    }

    suspend fun saveEntry(entry: DiaryEntry): Unit = withContext(Dispatchers.IO) {
        val encryptedContent = if (entry.isEncrypted && entry.content.isNotBlank()) {
            securityManager.encryptText(entry.content)
        } else {
            entry.content
        }

        val wordCount = calculateWordCount(entry.content)
        val entryToSave = entry.copy(
            content = encryptedContent,
            wordCount = wordCount,
            updatedAt = System.currentTimeMillis()
        )
        diaryDao.insertOrUpdate(entryToSave)
    }

    suspend fun toggleFavorite(id: String): Unit = withContext(Dispatchers.IO) {
        val entry = diaryDao.getEntryById(id) ?: return@withContext
        val updated = entry.copy(isFavorite = !entry.isFavorite)
        diaryDao.update(updated)
    }

    suspend fun deleteEntry(id: String): Unit = withContext(Dispatchers.IO) {
        diaryDao.deleteById(id)
    }

    suspend fun getStats(): DiaryStats = withContext(Dispatchers.IO) {
        val all = diaryDao.getAllEntriesSync()
        val totalEntries = all.size
        val favCount = all.count { it.isFavorite }
        var totalWords = 0
        var totalAudio = 0
        var totalPhotos = 0
        val moodFrequencies = mutableMapOf<String, Int>()

        for (item in all) {
            totalWords += item.wordCount
            if (!item.audioPath.isNullOrEmpty()) totalAudio++
            try {
                val array = JSONArray(item.photoPathsJson)
                totalPhotos += array.length()
            } catch (_: Exception) {
            }
            moodFrequencies[item.mood] = (moodFrequencies[item.mood] ?: 0) + 1
        }

        val dominantMoodStr = moodFrequencies.maxByOrNull { it.value }?.key
        val dominantMood = Mood.fromName(dominantMoodStr)

        val streak = calculateStreakDays(all)

        DiaryStats(
            totalEntries = totalEntries,
            favoriteEntries = favCount,
            totalWords = totalWords,
            totalAudioMemos = totalAudio,
            totalPhotos = totalPhotos,
            currentStreakDays = streak,
            dominantMood = dominantMood
        )
    }

    private fun decryptEntryIfNeeded(entry: DiaryEntry): DiaryEntry {
        return if (entry.isEncrypted && entry.content.isNotBlank()) {
            entry.copy(content = securityManager.decryptText(entry.content))
        } else {
            entry
        }
    }

    private fun calculateWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    private fun calculateStreakDays(entries: List<DiaryEntry>): Int {
        if (entries.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        var streak = 0

        val daysWithEntries = entries.map {
            calendar.timeInMillis = it.createdAt
            val y = calendar.get(Calendar.YEAR)
            val d = calendar.get(Calendar.DAY_OF_YEAR)
            "$y-$d"
        }.toSet()

        val checkCalendar = Calendar.getInstance()
        // Check today or yesterday as streak start
        var checkKey = "${checkCalendar.get(Calendar.YEAR)}-${checkCalendar.get(Calendar.DAY_OF_YEAR)}"
        if (!daysWithEntries.contains(checkKey)) {
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = "${checkCalendar.get(Calendar.YEAR)}-${checkCalendar.get(Calendar.DAY_OF_YEAR)}"
            if (!daysWithEntries.contains(checkKey)) {
                return 0
            }
        }

        while (daysWithEntries.contains(checkKey)) {
            streak++
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = "${checkCalendar.get(Calendar.YEAR)}-${checkCalendar.get(Calendar.DAY_OF_YEAR)}"
        }

        return streak
    }
}
