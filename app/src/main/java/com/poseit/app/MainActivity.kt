package com.poseit.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.poseit.app.data.AppSettings
import com.poseit.app.input.HardwareKeys
import com.poseit.app.ui.nav.PoseitNavHost
import com.poseit.app.ui.theme.PoseitTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsFlow = appContainer.settingsRepository.settings
        setContent {
            PoseitTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Wait for the first settings emission so we know whether to show onboarding.
                    val onboardingDone by remember { settingsFlow.map<AppSettings, Boolean?> { it.onboardingDone } }
                        .collectAsState(initial = null)
                    onboardingDone?.let { done -> PoseitNavHost(startOnboarding = !done) }
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
