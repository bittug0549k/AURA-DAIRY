package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DiaryEntry
import com.example.data.model.Mood
import com.example.data.model.WeatherType
import com.example.viewmodel.AuraViewModel
import com.example.viewmodel.ScreenState
import com.example.viewmodel.TimelineViewMode
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    viewModel: AuraViewModel
) {
    val entries by viewModel.filteredEntries.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMood by viewModel.selectedMoodFilter.collectAsState()
    val selectedTag by viewModel.selectedTagFilter.collectAsState()
    val onlyFavorites by viewModel.onlyFavoritesFilter.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Aura Diary",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${entries.size} reflections",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Button
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) viewModel.searchQuery.value = ""
                            },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // View Mode Toggle (Cards <-> List <-> Calendar)
                        IconButton(
                            onClick = {
                                val nextMode = when (viewMode) {
                                    TimelineViewMode.CARDS -> TimelineViewMode.LIST
                                    TimelineViewMode.LIST -> TimelineViewMode.CALENDAR
                                    TimelineViewMode.CALENDAR -> TimelineViewMode.CARDS
                                }
                                viewModel.setViewMode(nextMode)
                            },
                            modifier = Modifier.testTag("view_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (viewMode) {
                                    TimelineViewMode.CARDS -> Icons.Default.Dashboard
                                    TimelineViewMode.LIST -> Icons.Default.ViewAgenda
                                    TimelineViewMode.CALENDAR -> Icons.Default.CalendarMonth
                                },
                                contentDescription = "Toggle view mode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Lock Button
                        if (viewModel.securityManager.isPinEnabled) {
                            IconButton(
                                onClick = { viewModel.lockApp() },
                                modifier = Modifier.testTag("quick_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock App",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Settings Button
                        IconButton(
                            onClick = { viewModel.navigateTo(ScreenState.SETTINGS) },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Search Bar Input (Expanded)
                AnimatedVisibility(visible = isSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search by title, memory, mood, or #tag...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Filter Chips Strip (Moods, Favorites, Tags)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All filter
                    item {
                        FilterPill(
                            label = "All",
                            isSelected = selectedMood == null && !onlyFavorites && selectedTag == null,
                            onClick = {
                                viewModel.selectedMoodFilter.value = null
                                viewModel.onlyFavoritesFilter.value = false
                                viewModel.selectedTagFilter.value = null
                            }
                        )
                    }

                    // Favorites Only
                    item {
                        FilterPill(
                            label = "Favorites ★",
                            isSelected = onlyFavorites,
                            onClick = {
                                viewModel.onlyFavoritesFilter.value = !onlyFavorites
                            }
                        )
                    }

                    // Moods
                    items(Mood.entries.toTypedArray()) { mood ->
                        FilterPill(
                            label = mood.displayName,
                            color = mood.auraColor,
                            isSelected = selectedMood == mood,
                            onClick = {
                                viewModel.selectedMoodFilter.value = if (selectedMood == mood) null else mood
                            }
                        )
                    }

                    // Tags
                    items(availableTags) { tag ->
                        FilterPill(
                            label = "#$tag",
                            isSelected = selectedTag == tag,
                            onClick = {
                                viewModel.selectedTagFilter.value = if (selectedTag == tag) null else tag
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewEntry() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("create_entry_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Entry", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Write",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (viewMode) {
                TimelineViewMode.CARDS -> {
                    TimelineCardsList(
                        entries = entries,
                        onEntryClick = { viewModel.openEditEntry(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.id) }
                    )
                }
                TimelineViewMode.LIST -> {
                    TimelineDenseList(
                        entries = entries,
                        onEntryClick = { viewModel.openEditEntry(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.id) }
                    )
                }
                TimelineViewMode.CALENDAR -> {
                    CalendarTimelineView(
                        entries = entries,
                        onEntryClick = { viewModel.openEditEntry(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    color: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                if (isSelected) (color ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .minimumInteractiveComponentSize(),
        color = if (isSelected) (color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) (color ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun TimelineCardsList(
    entries: List<DiaryEntry>,
    onEntryClick: (DiaryEntry) -> Unit,
    onToggleFavorite: (DiaryEntry) -> Unit
) {
    if (entries.isEmpty()) {
        EmptyTimelineState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            DiaryCardItem(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onToggleFavorite = { onToggleFavorite(entry) }
            )
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun DiaryCardItem(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val mood = Mood.fromName(entry.mood)
    val weather = WeatherType.fromName(entry.weather)
    val firstPhoto = remember(entry.photoPathsJson) {
        try {
            val array = JSONArray(entry.photoPathsJson)
            if (array.length() > 0) array.getString(0) else null
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                mood.auraColor.copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            )
            .testTag("entry_card_${entry.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Mood Badge + Date + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(mood.auraColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mood.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = mood.auraColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${formatDate(entry.createdAt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (entry.isFavorite) Color(0xFFFF5A79) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = entry.title.ifBlank { "Untitled Memory" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Content Snippet
            if (entry.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Attached Photo Preview Thumbnail
            if (firstPhoto != null && File(firstPhoto).exists()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = File(firstPhoto),
                    contentDescription = "Attached photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Badges: Audio Memo, Weather, Location, Word Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Audio badge
                if (!entry.audioPath.isNullOrBlank()) {
                    BadgeChip(icon = Icons.Default.Mic, text = "Voice Memo")
                }

                // Weather badge
                BadgeChip(icon = weather.icon, text = weather.displayName)

                // Location badge
                if (!entry.locationName.isNullOrBlank()) {
                    Text(
                        text = "📍 ${entry.locationName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (entry.wordCount > 0) {
                    Text(
                        text = "${entry.wordCount} words",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineDenseList(
    entries: List<DiaryEntry>,
    onEntryClick: (DiaryEntry) -> Unit,
    onToggleFavorite: (DiaryEntry) -> Unit
) {
    if (entries.isEmpty()) {
        EmptyTimelineState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            val mood = Mood.fromName(entry.mood)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEntryClick(entry) }
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(mood.auraColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title.ifBlank { "Untitled" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatDate(entry.createdAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    if (entry.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFF5A79),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
fun CalendarTimelineView(
    entries: List<DiaryEntry>,
    onEntryClick: (DiaryEntry) -> Unit
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    val currentMonthYear = remember(calendar.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    // Map each day in month to list of entries on that day
    val entriesByDay = remember(entries, calendar.timeInMillis) {
        val map = mutableMapOf<Int, MutableList<DiaryEntry>>()
        val entryCal = Calendar.getInstance()
        entries.forEach { entry ->
            entryCal.timeInMillis = entry.createdAt
            if (entryCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                entryCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
            ) {
                val day = entryCal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(entry)
            }
        }
        map
    }

    val selectedDayEntries = remember(selectedDateMillis, entries) {
        if (selectedDateMillis == null) {
            emptyList()
        } else {
            val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis!! }
            val checkCal = Calendar.getInstance()
            entries.filter { entry ->
                checkCal.timeInMillis = entry.createdAt
                checkCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                        checkCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month Switcher Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonthYear,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row {
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                    calendar = newCal
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                    calendar = newCal
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekday labels
        val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Days Grid
        val firstDayCal = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based Sunday
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val totalGridSlots = 35 // 5 weeks x 7 or 42
        val rows = (daysInMonth + firstDayOfWeek + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val dayNumber = (row * 7 + col) - firstDayOfWeek + 1
                        if (dayNumber in 1..daysInMonth) {
                            val dayEntries = entriesByDay[dayNumber] ?: emptyList()
                            val isSelected = selectedDateMillis?.let {
                                val cal = Calendar.getInstance().apply { timeInMillis = it }
                                cal.get(Calendar.DAY_OF_MONTH) == dayNumber &&
                                        cal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                        cal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                            } ?: false

                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val clickCal = Calendar.getInstance().apply {
                                            timeInMillis = calendar.timeInMillis
                                            set(Calendar.DAY_OF_MONTH, dayNumber)
                                        }
                                        selectedDateMillis = clickCal.timeInMillis
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                shape = CircleShape
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 13.sp,
                                        fontWeight = if (dayEntries.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (dayEntries.isNotEmpty()) {
                                        val firstMood = Mood.fromName(dayEntries.first().mood)
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(firstMood.auraColor)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(38.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Entries List
        if (selectedDayEntries.isNotEmpty()) {
            Text(
                text = "Entries on this day (${selectedDayEntries.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayEntries) { entry ->
                    DiaryCardItem(
                        entry = entry,
                        onClick = { onEntryClick(entry) },
                        onToggleFavorite = {}
                    )
                }
            }
        } else if (selectedDateMillis != null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reflections logged on this date.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun BadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyTimelineState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Your Mind Awaits",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No thoughts recorded yet. Tap + to capture your reflections, voice notes, and moods in total offline privacy.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}
