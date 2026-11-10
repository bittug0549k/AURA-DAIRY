package com.example

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.AuraDiaryTheme
import com.example.viewmodel.AuraViewModel
import com.example.viewmodel.ScreenState

class MainActivity : ComponentActivity() {

    private val viewModel: AuraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global uncaught exception guardrail to prevent fatal process death
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log and gracefully handle without process hard-crash
                throwable.printStackTrace()
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        enableEdgeToEdge()
        applyPrivacyWindowFlag()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            AuraDiaryTheme(themeMode = themeMode) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isLocked by viewModel.isAppLocked.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearToastMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(
                            targetState = Pair(currentScreen, isLocked),
                            label = "ScreenTransition"
                        ) { (screen, locked) ->
                            when {
                                screen == ScreenState.SPLASH -> {
                                    SplashScreen(
                                        onSplashFinished = {
                                            viewModel.navigateTo(ScreenState.TIMELINE)
                                        }
                                    )
                                }
                                locked && viewModel.securityManager.isPinEnabled -> {
                                    PinLockScreen(
                                        onPinEntered = { pin ->
                                            viewModel.unlockWithPin(pin)
                                        }
                                    )
                                }
                                screen == ScreenState.TIMELINE -> {
                                    TimelineScreen(viewModel = viewModel)
                                }
                                screen == ScreenState.EDITOR -> {
                                    EditorScreen(viewModel = viewModel)
                                }
                                screen == ScreenState.SETTINGS -> {
                                    SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyPrivacyWindowFlag()
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock when minimized if PIN security is enabled
        if (viewModel.securityManager.isPinEnabled) {
            viewModel.lockApp()
        }
    }

    private fun applyPrivacyWindowFlag() {
        if (viewModel.securityManager.isPrivacyFlagSecureEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
