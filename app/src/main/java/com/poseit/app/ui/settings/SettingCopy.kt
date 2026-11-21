package com.poseit.app.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poseit.app.data.COUNTDOWN_CHOICES
import com.poseit.app.data.countdownChoiceFor
import com.poseit.app.ui.components.SegmentedPill
import kotlin.math.roundToInt

/**
 * The names and descriptions of the settings that both the Settings screen and the camera's Quick settings show, so
 * the same setting reads the same in both places.
 */
internal object SettingCopy {
    const val COACHING = "Coaching"
    const val GUIDE = "Guide"

    const val VOICE = "Voice guidance"
    const val VOICE_SUBTITLE = "Spoken hints and countdown"

    const val AUTO_CAPTURE = "Auto-capture"
    const val AUTO_CAPTURE_SUBTITLE = "Takes the photo when you hold the pose"

    const val STRICTNESS = "Match strictness"

    const val COUNTDOWN = "Countdown"
    const val COUNTDOWN_SUBTITLE = "Seconds between a match and the photo"

    const val GUIDE_OPACITY = "Guide opacity"

    const val LIVE_SKELETON = "Live skeleton"
    const val LIVE_SKELETON_SUBTITLE = "Shows the body Poseit detects as dots and lines"

    const val LEVEL = "Level indicator"
    const val LEVEL_SUBTITLE = "Helps you keep the horizon straight"

    const val STRICTNESS_MIN = 0.60f
    const val STRICTNESS_MAX = 0.95f
    const val OPACITY_MIN = 0.15f

    /** "80%, balanced": the value label of the strictness slider. */
    fun strictnessValue(threshold: Float): String =
        "${(threshold * 100).roundToInt()}%, ${strictnessLabel(threshold).lowercase()}"

    /** What the current strictness means, as the slider row's subtitle. */
    fun strictnessHint(threshold: Float): String = when (strictnessLabel(threshold)) {
        "Relaxed" -> "A rough match is enough. Suits kids and groups."
        "Balanced" -> "Needs a close match before the photo"
        else -> "Needs an almost exact match"
    }

    /** Snaps a strictness to 5% steps before it is stored. */
    fun snapStrictness(threshold: Float): Float = (threshold * 20f).roundToInt() / 20f

    /**
     * "Off", "2 s", "10 s": the countdown's option label. The unit stays lower case when the segmented pill draws its
     * labels in capitals ("OFF", "2 s").
     */
    fun countdownLabel(seconds: Int): String = if (seconds == 0) "Off" else "$seconds s"

    /** The spoken form of [countdownLabel]. */
    fun countdownDescription(seconds: Int): String = if (seconds == 0) "No countdown" else "$seconds seconds"
}

internal fun strictnessLabel(threshold: Float): String = when {
    threshold < 0.725f -> "Relaxed"
    threshold < 0.875f -> "Balanced"
    else -> "Strict"
}

/**
 * The countdown choices ([COUNTDOWN_CHOICES]) as one segmented pill across the width it is given, used by Settings
 * (inside a dark card) and by the camera's Quick settings (on the cream sheet). The selected option is lavender;
 * screen readers hear "No countdown", "3 seconds" for each option. Each option fills the 48dp track (a full touch
 * target in the accessibility tree too); 6dp option padding lets all five fit a 360dp phone at 130% text.
 */
@Composable
internal fun CountdownChips(seconds: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selected = countdownChoiceFor(seconds)
    SegmentedPill(
        options = COUNTDOWN_CHOICES.map(SettingCopy::countdownLabel),
        descriptions = COUNTDOWN_CHOICES.map(SettingCopy::countdownDescription),
        selectedIndex = COUNTDOWN_CHOICES.indexOf(selected),
        onSelect = { onSelect(COUNTDOWN_CHOICES[it]) },
        modifier = modifier.fillMaxWidth(),
        optionPadding = 6.dp,
    )
}
