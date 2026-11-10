package com.example.data.local

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class AudioRecordManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playingAudioPath = MutableStateFlow<String?>(null)
    val playingAudioPath: StateFlow<String?> = _playingAudioPath.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var recordingStartTime = 0L

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (_isRecording.value && mediaRecorder != null) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    _currentAmplitude.value = maxAmp
                    _recordingDurationMs.value = System.currentTimeMillis() - recordingStartTime
                } catch (_: Exception) {
                    // Ignore transient state
                }
                handler.postDelayed(this, 100)
            }
        }
    }

    private val playbackRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (_isPlaying.value && player.isPlaying) {
                    try {
                        val current = player.currentPosition
                        val total = player.duration
                        if (total > 0) {
                            _playbackProgress.value = current.toFloat() / total.toFloat()
                        }
                    } catch (_: Exception) {
                        // Safe playback polling
                    }
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    /**
     * Start recording an audio memo safely.
     * Returns true if recording started successfully.
     */
    fun startRecording(): Boolean {
        stopRecording(save = false) // clean any previous state
        stopPlayback()

        return try {
            val audioDir = File(context.filesDir, "aura_audio").apply { if (!exists()) mkdirs() }
            val file = File(audioDir, "memo_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            handler.post(amplitudeRunnable)
            true
        } catch (e: Exception) {
            cancelRecording()
            false
        }
    }

    /**
     * Stops the active recording.
     * Returns Pair(absoluteFilePath, durationMs) if saved, or null if cancelled/failed.
     */
    fun stopRecording(save: Boolean = true): Pair<String, Long>? {
        if (!_isRecording.value && mediaRecorder == null) return null

        _isRecording.value = false
        handler.removeCallbacks(amplitudeRunnable)

        val duration = System.currentTimeMillis() - recordingStartTime
        val file = currentRecordingFile

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                    // Ignored if stopped prematurely
                }
                reset()
                release()
            }
        } catch (_: Exception) {
            // Safe release
        } finally {
            mediaRecorder = null
        }

        return if (save && file != null && file.exists() && file.length() > 0 && duration >= 800) {
            currentRecordingFile = null
            Pair(file.absolutePath, duration)
        } else {
            // Buffer cleanup: delete incomplete or cancelled audio file immediately
            file?.delete()
            currentRecordingFile = null
            null
        }
    }

    fun cancelRecording() {
        stopRecording(save = false)
    }

    /**
     * Plays or pauses playback of the audio file at [path].
     */
    fun togglePlayAudio(path: String) {
        if (_isPlaying.value && _playingAudioPath.value == path) {
            pausePlayback()
            return
        }

        startPlayback(path)
    }

    fun startPlayback(path: String) {
        stopPlayback()

        val file = File(path)
        if (!file.exists()) return

        try {
            val player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    _playingAudioPath.value = null
                    handler.removeCallbacks(playbackRunnable)
                }
                start()
            }
            mediaPlayer = player
            _playingAudioPath.value = path
            _isPlaying.value = true
            handler.post(playbackRunnable)
        } catch (e: IOException) {
            stopPlayback()
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            handler.removeCallbacks(playbackRunnable)
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (_: Exception) {
            // Safe release
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _playbackProgress.value = 0f
            _playingAudioPath.value = null
            handler.removeCallbacks(playbackRunnable)
        }
    }

    /**
     * Strict lifecycle cleanup. Disposes active recorders, players, and timers.
     */
    fun releaseAll() {
        cancelRecording()
        stopPlayback()
        handler.removeCallbacksAndMessages(null)
    }
}
