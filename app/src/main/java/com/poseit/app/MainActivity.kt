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
import com.poseit.app.input.HardwareKeys
import com.poseit.app.ui.nav.PoseitNavHost
import com.poseit.app.ui.theme.PoseitTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so the bars must not follow the system light/dark setting: light mode would give
        // dark status icons on the dark UI and a white nav-bar scrim on Android 8-9. The nav bar uses auto() forced
        // to dark rather than dark(): dark() turns off the system's contrast scrim behind 3-button navigation
        // (API 29+), so white buttons would sit on bright photo thumbnails; auto() keeps it, and gives a dark
        // translucent scrim on API 24-28.
        val navScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(navScrim, navScrim) { true },
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
