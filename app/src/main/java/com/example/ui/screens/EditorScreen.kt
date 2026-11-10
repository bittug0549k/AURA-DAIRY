package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Mood
import com.example.data.model.WeatherType
import com.example.ui.components.AudioRecordingDialog
import com.example.ui.components.AudioWaveformPlayer
import com.example.ui.components.MoodSelectorRow
import com.example.viewmodel.AuraViewModel
import com.example.viewmodel.ScreenState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: AuraViewModel
) {
    val title by viewModel.editorTitle.collectAsState()
    val content by viewModel.editorContent.collectAsState()
    val mood by viewModel.editorMood.collectAsState()
    val weather by viewModel.editorWeather.collectAsState()
    val location by viewModel.editorLocation.collectAsState()
    val photos by viewModel.editorPhotos.collectAsState()
    val audioPath by viewModel.editorAudioPath.collectAsState()
    val audioDurationMs by viewModel.editorAudioDurationMs.collectAsState()
    val isFavorite by viewModel.editorIsFavorite.collectAsState()
    val isAutoSaved by viewModel.isAutoSaved.collectAsState()
    val currentEditing = viewModel.editingEntry.collectAsState().value

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAudioRecorderModal by remember { mutableStateOf(false) }
    var showLocationPrompt by remember { mutableStateOf(false) }
    var showWeatherMenu by remember { mutableStateOf(false) }
    var showTagsPrompt by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf(location ?: "") }
    var tagsInput by remember { mutableStateOf(viewModel.editorTags.value) }

    // Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachPhotoUri(uri)
        }
    }

    // Safe BackHandler: Auto-saves draft and returns to Timeline
    BackHandler {
        viewModel.persistCurrentEditorDraft(isSilent = true)
        viewModel.navigateTo(ScreenState.TIMELINE)
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.persistCurrentEditorDraft(isSilent = true)
                                viewModel.navigateTo(ScreenState.TIMELINE)
                            },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Auto-Save Status Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAutoSaved) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38EF7D))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AES Encrypted & Saved",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Saving...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Favorite toggle
                        IconButton(
                            onClick = { viewModel.editorIsFavorite.value = !isFavorite },
                            modifier = Modifier.testTag("editor_favorite_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF5A79) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Delete option if existing entry
                        if (currentEditing != null) {
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.testTag("editor_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Done / Save Button
                        Button(
                            onClick = {
                                viewModel.persistCurrentEditorDraft(isSilent = false)
                                viewModel.navigateTo(ScreenState.TIMELINE)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("editor_save_done_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Minimalist Media & Attachment Toolbar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add Photo
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("add_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Photo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Record Audio Memo
                        IconButton(
                            onClick = {
                                val started = viewModel.audioRecordManager.startRecording()
                                if (started) {
                                    showAudioRecorderModal = true
                                } else {
                                    viewModel.showMessage("Audio permission or recorder unavailable")
                                }
                            },
                            modifier = Modifier.testTag("record_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Memo",
                                tint = if (audioPath != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Weather Picker
                        Box {
                            IconButton(
                                onClick = { showWeatherMenu = true },
                                modifier = Modifier.testTag("weather_picker_button")
                            ) {
                                Icon(
                                    imageVector = weather.icon,
                                    contentDescription = "Weather: ${weather.displayName}",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showWeatherMenu,
                                onDismissRequest = { showWeatherMenu = false }
                            ) {
                                WeatherType.entries.forEach { w ->
                                    DropdownMenuItem(
                                        text = { Text(w.displayName) },
                                        leadingIcon = { Icon(w.icon, contentDescription = null) },
                                        onClick = {
                                            viewModel.editorWeather.value = w
                                            viewModel.persistCurrentEditorDraft(isSilent = true)
                                            showWeatherMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Location Tag
                        IconButton(
                            onClick = {
                                locationInput = location ?: ""
                                showLocationPrompt = true
                            },
                            modifier = Modifier.testTag("location_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = if (location != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Tags Tag
                        IconButton(
                            onClick = {
                                tagsInput = viewModel.editorTags.value
                                showTagsPrompt = true
                            },
                            modifier = Modifier.testTag("tags_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = "Tags",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Word count indicator
                    val words = content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                    Text(
                        text = "$words words",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Mood Selector Strip
            MoodSelectorRow(
                selectedMood = mood,
                onMoodSelected = { newMood ->
                    viewModel.editorMood.value = newMood
                    viewModel.persistCurrentEditorDraft(isSilent = true)
                },
                modifier = Modifier.padding(top = 8.dp)
            )

            // Weather & Location Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(weather.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(weather.displayName, fontSize = 12.sp)
                    }
                }

                if (!location.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(location!!, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove location",
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { viewModel.editorLocation.value = null }
                            )
                        }
                    }
                }

                val currentTags = viewModel.editorTags.value
                if (currentTags.isNotBlank()) {
                    Text(
                        text = currentTags.split(",").joinToString(" ") { "#${it.trim()}" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Attached Photos Gallery
            if (photos.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(photos) { photoPath ->
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = File(photoPath),
                                contentDescription = "Attached photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removePhoto(photoPath) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Attached Voice Memo Player
            if (audioPath != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    AudioWaveformPlayer(
                        audioPath = audioPath!!,
                        audioDurationMs = audioDurationMs,
                        audioManager = viewModel.audioRecordManager,
                        onDelete = { viewModel.removeAudioRecording() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title Field
            TextField(
                value = title,
                onValueChange = { newTitle ->
                    viewModel.onEditorTextChanged(newTitle, content)
                },
                placeholder = {
                    Text(
                        text = "Title of this reflection...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("editor_title_input")
            )

            // Content Field
            TextField(
                value = content,
                onValueChange = { newContent ->
                    viewModel.onEditorTextChanged(title, newContent)
                },
                placeholder = {
                    Text(
                        text = "Pour your thoughts freely. Everything is secured with 256-bit AES encryption and saved as you type...",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(horizontal = 8.dp)
                    .testTag("editor_content_input")
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Audio Recording Modal
    if (showAudioRecorderModal) {
        AudioRecordingDialog(
            audioManager = viewModel.audioRecordManager,
            onRecordingSaved = { path, duration ->
                viewModel.attachAudioRecording(path, duration)
                showAudioRecorderModal = false
            },
            onDismiss = { showAudioRecorderModal = false }
        )
    }

    // Location Dialog Prompt
    if (showLocationPrompt) {
        AlertDialog(
            onDismissRequest = { showLocationPrompt = false },
            title = { Text("Tag Location") },
            text = {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    placeholder = { Text("e.g. Mumbai, Home, Coffee Shop") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("location_text_field")
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editorLocation.value = locationInput.trim().takeIf { it.isNotBlank() }
                    viewModel.persistCurrentEditorDraft(isSilent = true)
                    showLocationPrompt = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLocationPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Tags Dialog Prompt
    if (showTagsPrompt) {
        AlertDialog(
            onDismissRequest = { showTagsPrompt = false },
            title = { Text("Add Tags") },
            text = {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    placeholder = { Text("comma-separated e.g. personal, gratitude, work") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tags_text_field")
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editorTags.value = tagsInput.trim()
                    viewModel.persistCurrentEditorDraft(isSilent = true)
                    showTagsPrompt = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTagsPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && currentEditing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Entry?") },
            text = { Text("This will permanently remove this encrypted memory from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(currentEditing.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
