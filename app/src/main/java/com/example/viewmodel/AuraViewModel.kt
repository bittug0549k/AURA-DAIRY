package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AudioRecordManager
import com.example.data.local.AuraDatabase
import com.example.data.local.BackupRestoreManager
import com.example.data.local.ImageStorageManager
import com.example.data.local.SecurityManager
import com.example.data.model.DiaryEntry
import com.example.data.model.DiaryStats
import com.example.data.model.Mood
import com.example.data.model.ThemeMode
import com.example.data.model.WeatherType
import com.example.data.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

enum class ScreenState {
    SPLASH,
    TIMELINE,
    EDITOR,
    SETTINGS
}

enum class TimelineViewMode {
    CARDS,
    LIST,
    CALENDAR
}

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AuraDatabase.getDatabase(application)
    val securityManager = SecurityManager(application)
    val diaryRepository = DiaryRepository(database.diaryDao(), securityManager)
    val audioRecordManager = AudioRecordManager(application)
    val imageStorageManager = ImageStorageManager(application)
    val backupRestoreManager = BackupRestoreManager(application, database.diaryDao(), securityManager)

    // Screen Navigation
    private val _currentScreen = MutableStateFlow(ScreenState.SPLASH)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // Lock State
    private val _isAppLocked = MutableStateFlow(securityManager.isPinEnabled)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Theme Mode
    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(securityManager.themeModeName)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // View Mode (Cards, List, Calendar)
    private val _viewMode = MutableStateFlow(TimelineViewMode.CARDS)
    val viewMode: StateFlow<TimelineViewMode> = _viewMode.asStateFlow()

    // Search and Filters with Debounce
    val searchQuery = MutableStateFlow("")
    val selectedMoodFilter = MutableStateFlow<Mood?>(null)
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val onlyFavoritesFilter = MutableStateFlow(false)

    // Base Entries Flow
    private val allEntriesFlow = diaryRepository.getAllEntries()

    // Debounced Search and Filtered Entries
    val filteredEntries: StateFlow<List<DiaryEntry>> = combine(
        allEntriesFlow,
        searchQuery.debounce(300),
        selectedMoodFilter,
        selectedTagFilter,
        onlyFavoritesFilter
    ) { entries, query, mood, tag, favoritesOnly ->
        entries.filter { entry ->
            val matchesQuery = query.isBlank() ||
                    entry.title.contains(query, ignoreCase = true) ||
                    entry.content.contains(query, ignoreCase = true) ||
                    entry.tags.contains(query, ignoreCase = true)

            val matchesMood = mood == null || entry.mood.equals(mood.name, ignoreCase = true)
            val matchesTag = tag == null || entry.tags.split(",").map { it.trim() }.contains(tag)
            val matchesFavorites = !favoritesOnly || entry.isFavorite

            matchesQuery && matchesMood && matchesTag && matchesFavorites
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available tags
    val availableTags: StateFlow<List<String>> = allEntriesFlow.combine(MutableStateFlow(Unit)) { entries, _ ->
        val tagSet = sortedSetOf<String>()
        entries.forEach { entry ->
            entry.tags.split(",")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
                .forEach { tagSet.add(it) }
        }
        tagSet.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics
    private val _stats = MutableStateFlow(DiaryStats())
    val stats: StateFlow<DiaryStats> = _stats.asStateFlow()

    // Current Entry Editor State
    private val _editingEntry = MutableStateFlow<DiaryEntry?>(null)
    val editingEntry: StateFlow<DiaryEntry?> = _editingEntry.asStateFlow()

    val editorTitle = MutableStateFlow("")
    val editorContent = MutableStateFlow("")
    val editorMood = MutableStateFlow(Mood.PEACEFUL)
    val editorTags = MutableStateFlow("")
    val editorWeather = MutableStateFlow(WeatherType.SUNNY)
    val editorLocation = MutableStateFlow<String?>(null)
    val editorPhotos = MutableStateFlow<List<String>>(emptyList())
    val editorAudioPath = MutableStateFlow<String?>(null)
    val editorAudioDurationMs = MutableStateFlow(0L)
    val editorIsFavorite = MutableStateFlow(false)
    val isAutoSaved = MutableStateFlow(true)

    private var autoSaveJob: Job? = null

    // UI Feedback Messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshStats()
        // Ensure sample warm welcome entry on very first launch if DB is empty
        viewModelScope.launch(Dispatchers.IO) {
            val count = database.diaryDao().getTotalCount()
            if (count == 0) {
                seedInitialEntry()
            }
        }
    }

    private suspend fun seedInitialEntry() {
        val welcomeEntry = DiaryEntry(
            title = "Welcome to Aura Diary ✨",
            content = "This is your private, offline sanctuary engineered with 256-bit AES client-side encryption.\n\n" +
                    "Your memories, thoughts, photos, and voice notes stay 100% on this device—no trackers, no leaks, zero thermal throttling.\n\n" +
                    "Tap + anytime to write, record, or track your moods. Created with care by DUMP KODER 🇮🇳.",
            mood = Mood.JOY.name,
            tags = "welcome, mind, aura",
            weather = WeatherType.SUNNY.name,
            locationName = "New Delhi, India",
            isFavorite = true,
            wordCount = 52
        )
        diaryRepository.saveEntry(welcomeEntry)
        refreshStats()
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun setViewMode(mode: TimelineViewMode) {
        _viewMode.value = mode
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        securityManager.themeModeName = mode.name
    }

    // --- Authentication & Locking ---

    fun unlockWithPin(pin: String): Boolean {
        val success = securityManager.verifyPin(pin)
        if (success) {
            _isAppLocked.value = false
        }
        return success
    }

    fun lockApp() {
        if (securityManager.isPinEnabled) {
            _isAppLocked.value = true
        }
    }

    fun updatePin(pin: String) {
        securityManager.setMasterPin(pin)
        _isAppLocked.value = false
        showMessage("Master PIN updated securely")
    }

    fun removePin() {
        securityManager.disablePin()
        _isAppLocked.value = false
        showMessage("PIN lock disabled")
    }

    // --- Editor & Auto-Save ---

    fun openNewEntry() {
        _editingEntry.value = null
        editorTitle.value = ""
        editorContent.value = ""
        editorMood.value = Mood.PEACEFUL
        editorTags.value = ""
        editorWeather.value = WeatherType.SUNNY
        editorLocation.value = null
        editorPhotos.value = emptyList()
        editorAudioPath.value = null
        editorAudioDurationMs.value = 0L
        editorIsFavorite.value = false
        isAutoSaved.value = true
        navigateTo(ScreenState.EDITOR)
    }

    fun openEditEntry(entry: DiaryEntry) {
        _editingEntry.value = entry
        editorTitle.value = entry.title
        editorContent.value = entry.content
        editorMood.value = Mood.fromName(entry.mood)
        editorTags.value = entry.tags
        editorWeather.value = WeatherType.fromName(entry.weather)
        editorLocation.value = entry.locationName
        editorPhotos.value = try {
            val array = JSONArray(entry.photoPathsJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) list.add(array.getString(i))
            list
        } catch (_: Exception) {
            emptyList()
        }
        editorAudioPath.value = entry.audioPath
        editorAudioDurationMs.value = entry.audioDurationMs
        editorIsFavorite.value = entry.isFavorite
        isAutoSaved.value = true
        navigateTo(ScreenState.EDITOR)
    }

    /**
     * Debounced auto-save triggers 800ms after user pauses typing.
     */
    fun onEditorTextChanged(newTitle: String, newContent: String) {
        editorTitle.value = newTitle
        editorContent.value = newContent
        isAutoSaved.value = false

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(800)
            persistCurrentEditorDraft(isSilent = true)
        }
    }

    fun persistCurrentEditorDraft(isSilent: Boolean = false) {
        val title = editorTitle.value.trim()
        val content = editorContent.value.trim()

        if (title.isBlank() && content.isBlank() && editorPhotos.value.isEmpty() && editorAudioPath.value == null) {
            return
        }

        val existing = _editingEntry.value
        val entryId = existing?.id ?: java.util.UUID.randomUUID().toString()
        val createdAt = existing?.createdAt ?: System.currentTimeMillis()

        val photosJson = JSONArray(editorPhotos.value).toString()

        val entry = DiaryEntry(
            id = entryId,
            title = if (title.isBlank()) "Untitled Thought" else title,
            content = content,
            mood = editorMood.value.name,
            tags = editorTags.value.trim(),
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            weather = editorWeather.value.name,
            locationName = editorLocation.value,
            photoPathsJson = photosJson,
            audioPath = editorAudioPath.value,
            audioDurationMs = editorAudioDurationMs.value,
            isFavorite = editorIsFavorite.value
        )

        viewModelScope.launch(Dispatchers.IO) {
            diaryRepository.saveEntry(entry)
            _editingEntry.value = entry
            isAutoSaved.value = true
            refreshStats()
            if (!isSilent) {
                showMessage("Entry saved securely ✨")
            }
        }
    }

    fun attachPhotoUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = imageStorageManager.saveAndCompressImage(uri)
            if (savedPath != null) {
                editorPhotos.value = editorPhotos.value + savedPath
                isAutoSaved.value = false
                persistCurrentEditorDraft(isSilent = true)
            } else {
                showMessage("Could not attach image")
            }
        }
    }

    fun removePhoto(path: String) {
        editorPhotos.value = editorPhotos.value.filter { it != path }
        imageStorageManager.deleteImage(path)
        isAutoSaved.value = false
        persistCurrentEditorDraft(isSilent = true)
    }

    fun attachAudioRecording(path: String, durationMs: Long) {
        editorAudioPath.value = path
        editorAudioDurationMs.value = durationMs
        isAutoSaved.value = false
        persistCurrentEditorDraft(isSilent = true)
    }

    fun removeAudioRecording() {
        val path = editorAudioPath.value
        editorAudioPath.value = null
        editorAudioDurationMs.value = 0L
        if (path != null) {
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }
        isAutoSaved.value = false
        persistCurrentEditorDraft(isSilent = true)
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            diaryRepository.toggleFavorite(id)
            refreshStats()
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(id)
            refreshStats()
            showMessage("Entry deleted")
            if (_editingEntry.value?.id == id) {
                navigateTo(ScreenState.TIMELINE)
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = diaryRepository.getStats()
            _stats.value = s
        }
    }

    fun showMessage(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        audioRecordManager.releaseAll()
    }
}
