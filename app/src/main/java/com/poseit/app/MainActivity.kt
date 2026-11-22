package com.poseit.app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.poseit.app.input.HardwareKeys
import com.poseit.app.ui.nav.PoseitNavHost
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.PoseitColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light (the cream canvas), so the bars must not follow the system light/dark setting: dark
        // icons on a transparent status bar. The nav bar uses auto() forced to light rather than light(): light()
        // turns off the system's contrast scrim behind 3-button navigation (API 29+), which on API 35+ is a
        // translucent canvas strip matching the window background; auto() keeps it and leaves gesture navigation
        // transparent. API 26-28 get the canvas scrim below, API 24-25 (no dark nav icons) the dark one. Dark screens
        // (camera, photo viewer) switch to light icons while they show (LightSystemBarIconsEffect).
        val paperScrim = PoseitColors.backgroundBody.copy(alpha = 0.8f).toArgb()
        val inkScrim = PoseitColors.backgroundCard.copy(alpha = 0.5f).toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(paperScrim, inkScrim) { false },
        )
        val settingsFlow = appContainer.settingsRepository.settings
        setContent {
            PoseitTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Read the onboarding flag ONCE. The NavHost's start destination must never change while it is
                    // shown: a new start destination rebuilds the graph and wipes the back stack (finishing the
                    // intro would then push Library twice, and "Show intro again" would throw the user out of
                    // Settings). Saved, so a recreated activity keeps the start its restored back stack belongs to.
                    var startOnboarding by rememberSaveable { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) {
                        if (startOnboarding == null) {
                            startOnboarding = try {
                                !settingsFlow.first().onboardingDone
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Log.w("MainActivity", "Could not read settings", e)
                                true
                            }
                        }
                    }
                    startOnboarding?.let { PoseitNavHost(startOnboarding = it) }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) && HardwareKeys.captureVolumeKeys) {
            if (event.repeatCount == 0) HardwareKeys.onVolumeKey()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) && HardwareKeys.captureVolumeKeys) return true
        return super.onKeyUp(keyCode, event)
    }
}
