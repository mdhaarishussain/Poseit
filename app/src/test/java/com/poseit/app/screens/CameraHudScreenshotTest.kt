package com.poseit.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind
import com.poseit.app.ui.camera.CameraErrorNotice
import com.poseit.app.ui.camera.CountdownNumber
import com.poseit.app.ui.camera.DetectorNotice
import com.poseit.app.ui.camera.HintBanner
import com.poseit.app.ui.camera.LightingChip
import com.poseit.app.ui.camera.PosePill
import com.poseit.app.ui.camera.RoundControl
import com.poseit.app.ui.camera.RoundToggle
import com.poseit.app.ui.camera.ScoreRing
import com.poseit.app.ui.camera.SessionInfo
import com.poseit.app.ui.camera.ShotCounter
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the camera HUD in the states the JVM camera never reaches (a live score, the hold ring, every hint kind,
 * the countdown) over the black surround and over a bright scene, into app/build/screenshots/camera-hud.png, for
 * design review of legibility.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w860dp-h900dp-xhdpi")
class CameraHudScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun hud() {
        compose.setContent {
            PoseitTheme {
                Row(Modifier.fillMaxSize()) {
                    Panel(Studio.LiveView, Modifier.width(430.dp))
                    // A pale wall: the hardest case for white chrome.
                    Panel(Color(0xFFE8E6DF), Modifier.width(430.dp))
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/camera-hud.png")
    }

    @Composable
    private fun Panel(scene: Color, modifier: Modifier) {
        Column(
            modifier
                .background(scene)
                .padding(Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                ScoreRing(percent = null, detectorReady = false, detectorError = null, holdProgress = { null })
                ScoreRing(percent = null, detectorReady = true, detectorError = null, holdProgress = { null })
                ScoreRing(percent = 34, detectorReady = true, detectorError = null, holdProgress = { null })
                ScoreRing(percent = 67, detectorReady = true, detectorError = null, holdProgress = { null })
                ScoreRing(percent = 100, detectorReady = true, detectorError = null, holdProgress = { 0.6f })
                ScoreRing(percent = null, detectorReady = true, detectorError = "x", holdProgress = { null })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.CenterVertically) {
                RoundControl(Icons.Outlined.Info, "Pose tips", onClick = {})
                RoundControl(Icons.Outlined.Timer, "Timer", onClick = {}, active = true)
                RoundToggle(Icons.Outlined.LockOpen, Icons.Filled.Lock, "Lock", checked = false, onCheckedChange = {})
                RoundToggle(Icons.Outlined.LockOpen, Icons.Filled.Lock, "Lock", checked = true, onCheckedChange = {})
                ShotCounter(SessionInfo(3, 10), onStop = {})
            }
            HintBanner(CoachHint("Perfect, hold it", HintKind.PERFECT, 0f))
            HintBanner(CoachHint("Raise your left arm a little", HintKind.POSE, 0.5f))
            HintBanner(CoachHint("Step back so your feet are in the picture", HintKind.VISIBILITY, 0.8f))
            HintBanner(CoachHint("Step into the picture", HintKind.NO_PERSON, 1f))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
                LightingChip(CoachHint("Backlit: turn towards the light", HintKind.FRAMING, 0.5f))
                DetectorNotice()
            }
            PosePill(BuiltInPoses.all.first(), visible = true, onClick = {})
            CameraErrorNotice("No camera is available on this device")
            Box(Modifier.height(140.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xl)) {
                    CountdownNumber(3)
                    CountdownNumber(10)
                }
            }
            Text("Scene", style = StudioType.label, color = Studio.Graphite)
        }
    }
}
