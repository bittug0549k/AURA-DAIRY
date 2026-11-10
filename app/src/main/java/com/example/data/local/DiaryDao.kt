package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

data class MoodCount(
    val mood: String,
    val count: Int
)

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    fun getAllEntriesFlow(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesSync(): List<DiaryEntry>

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteEntriesFlow(): Flow<List<DiaryEntry>>

    @Query("""
        SELECT * FROM diary_entries 
        WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE mood = :mood ORDER BY createdAt DESC")
    fun getEntriesByMood(mood: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getEntriesForDateRange(startTime: Long, endTime: Long): Flow<List<DiaryEntry>>

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun getTotalCount(): Int

    @Query("SELECT mood, COUNT(*) as count FROM diary_entries GROUP BY mood")
    suspend fun getMoodCounts(): List<MoodCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: DiaryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DiaryEntry>)

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAll()
}
