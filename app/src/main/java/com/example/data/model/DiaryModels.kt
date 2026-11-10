package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val isEncrypted: Boolean = true,
    val mood: String = Mood.PEACEFUL.name,
    val tags: String = "", // Comma-separated list of tags
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val weather: String = WeatherType.SUNNY.name,
    val locationName: String? = null,
    val photoPathsJson: String = "[]", // JSON array of internal photo file paths
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val isFavorite: Boolean = false,
    val wordCount: Int = 0
)

enum class Mood(
    val displayName: String,
    val auraColor: Color,
    val icon: ImageVector,
    val description: String
) {
    JOY("Joy", Color(0xFFFFD166), Icons.Default.SentimentSatisfiedAlt, "Joyful & uplifted"),
    PEACEFUL("Peaceful", Color(0xFF06D6A0), Icons.Default.Spa, "Serene & balanced"),
    REFLECTIVE("Reflective", Color(0xFF9D4EDD), Icons.Default.SelfImprovement, "Deep thoughts & insights"),
    ENERGETIC("Energetic", Color(0xFFFF6B6B), Icons.Default.Bolt, "Active & high energy"),
    GRATEFUL("Grateful", Color(0xFFF77F00), Icons.Default.VolunteerActivism, "Thankful & blessed"),
    CREATIVE("Creative", Color(0xFF4CC9F0), Icons.Default.Palette, "Inspired & inventing"),
    SAD("Gentle Blue", Color(0xFF4A90E2), Icons.Default.SentimentDissatisfied, "Healing & reflective"),
    STRESSED("Venting", Color(0xFFE63946), Icons.Default.CrisisAlert, "Overwhelmed & venting");

    companion object {
        fun fromName(name: String?): Mood {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PEACEFUL
        }
    }
}

enum class WeatherType(
    val displayName: String,
    val icon: ImageVector
) {
    SUNNY("Sunny", Icons.Default.WbSunny),
    CLOUDY("Cloudy", Icons.Default.Cloud),
    RAINY("Rainy", Icons.Default.WaterDrop),
    THUNDER("Stormy", Icons.Default.Thunderstorm),
    COZY_NIGHT("Starlit Night", Icons.Default.NightsStay);

    companion object {
        fun fromName(name: String?): WeatherType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SUNNY
        }
    }
}

enum class ThemeMode(val displayName: String) {
    AMOLED("True AMOLED Black"),
    DARK("Deep Obsidian"),
    LIGHT("Minimal Pearl"),
    PASTEL_WARM("Cozy Warm")
}

data class DiaryStats(
    val totalEntries: Int = 0,
    val favoriteEntries: Int = 0,
    val totalWords: Int = 0,
    val totalAudioMemos: Int = 0,
    val totalPhotos: Int = 0,
    val currentStreakDays: Int = 0,
    val dominantMood: Mood = Mood.PEACEFUL
)
