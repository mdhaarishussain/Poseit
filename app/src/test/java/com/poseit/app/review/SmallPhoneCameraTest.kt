package com.poseit.app.review

import android.Manifest
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.poseit.app.MainActivity
import com.poseit.app.PoseitApp
import com.poseit.app.data.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The camera screen and its sheets on a small phone (usable area of a 360x640 dp phone) at 130 % font size. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h568dp-xhdpi", fontScale = 1.3f)
class SmallPhoneCameraTest {

    private val crashes = CrashCollector()
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(crashes).around(compose)

    private val app: PoseitApp get() = ApplicationProvider.getApplicationContext()

    private fun settle(ms: Long = 1500) {
        var t = 0L
        while (t < ms) {
            compose.mainClock.advanceTimeBy(100)
            Thread.sleep(20)
            t += 100
        }
        compose.waitForIdle()
    }

    @Test
    fun cameraAndSheets() {
        runBlocking { app.container.settingsRepository.update { AppSettings(onboardingDone = true) } }
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        ActivityScenario.launch(MainActivity::class.java).use {
            // On a screen this short the camera button is folded to its glyph, which carries the name.
            val cameraButton = (hasText("Pose camera") or hasContentDescription("Pose camera")) and hasClickAction()
            compose.waitUntil(15_000) { compose.onAllNodes(cameraButton).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodes(cameraButton).onFirst().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Take photo", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            settle(5000) // let the pose pill and snackbars time out
            captureScreenRoboImage("build/screenshots/review-small-camera.png")
            compose.onAllNodes(hasContentDescription("Quick settings") and hasClickAction()).onFirst().performClick()
            settle(800)
            captureScreenRoboImage("build/screenshots/review-small-camera-quick-settings.png")
            // Further down the sheet: the countdown's segmented pill and the guide style tiles at 130 % text.
            compose.onAllNodes(hasText("Hand-raise trigger") and hasClickAction()).onFirst().performScrollTo()
            settle(300)
            captureScreenRoboImage("build/screenshots/review-small-camera-quick-settings-countdown.png")
            compose.onAllNodes(hasText("Guide opacity")).onFirst().performScrollTo()
            settle(300)
            captureScreenRoboImage("build/screenshots/review-small-camera-quick-settings-guide.png")
            androidx.test.espresso.Espresso.pressBack()
            settle(500)
            compose.onAllNodes(hasContentDescription("All poses") and hasClickAction()).onFirst().performClick()
            settle(800)
            captureScreenRoboImage("build/screenshots/review-small-camera-all-poses.png")
            androidx.test.espresso.Espresso.pressBack()
            settle(500)
            compose.onAllNodes(hasContentDescription("Pose tips") and hasClickAction()).onFirst().performClick()
            settle(800)
            captureScreenRoboImage("build/screenshots/review-small-camera-tips.png")
            crashes.assertNoCrashes()
        }
    }
}
