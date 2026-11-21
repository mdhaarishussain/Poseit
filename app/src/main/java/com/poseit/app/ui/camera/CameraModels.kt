package com.poseit.app.ui.camera

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import com.poseit.app.camera.FlashMode
import com.poseit.app.camera.Lens
import com.poseit.app.camera.SceneAspect
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.FramingAudience
import com.poseit.app.pose.match.MatchResult

/** What is drawn as the pose guide. The photo modes only exist for poses made from a photo. */
enum class OverlayMode(val label: String) {
    MANNEQUIN("Mannequin"),
    PHOTO_GHOST("Photo ghost"),
    PHOTO_OUTLINE("Photo outline"),
}

/** Who reads the framing hints: the person posing, the person holding the phone, or decided by the lens. */
enum class AudienceMode(val label: String) {
    AUTO("Auto"),
    SUBJECT("I'm posing"),
    PHOTOGRAPHER("I'm taking the photo");

    /** Front lens = you are probably posing yourself; back lens = someone is taking the photo. */
    fun resolve(lens: Lens): FramingAudience = when (this) {
        AUTO -> if (lens == Lens.FRONT) FramingAudience.SUBJECT else FramingAudience.PHOTOGRAPHER
        SUBJECT -> FramingAudience.SUBJECT
        PHOTOGRAPHER -> FramingAudience.PHOTOGRAPHER
    }
}

/** Self-timer choice of the top-bar timer menu. */
sealed interface TimerOption {
    /** Short label for the top bar, or null when no timer is set. */
    val shortLabel: String?

    data object Off : TimerOption {
        override val shortLabel: String? = null
    }

    data class Delay(val seconds: Int) : TimerOption {
        override val shortLabel: String = "${seconds}s"
    }

    /**
     * Hands-free interval shooting.
     * @param shots number of photos, 0 = until stopped
     * @param intervalSeconds pause between two photos
     */
    data class Photoshoot(val shots: Int, val intervalSeconds: Int) : TimerOption {
        override val shortLabel: String = if (shots == 0) "∞" else "×$shots"
    }

    companion object {
        val DELAYS = listOf(3, 5, 10)
        val SHOT_CHOICES = listOf(3, 5, 10, 0)
        val INTERVAL_CHOICES = listOf(2, 3, 5, 10)

        /** Time to get into position before the first photo of a photoshoot. */
        const val PHOTOSHOOT_START_DELAY_SECONDS = 3

        /** Delay used by the hand-raise trigger when no timer is selected. */
        const val HAND_RAISE_DEFAULT_SECONDS = 3
    }
}

/** Low-frequency screen state (changes on user actions, not per camera frame). */
data class CameraUiState(
    /** The selected pose, or null for a plain camera. */
    val pose: PoseTemplate? = null,
    /** False until the initial pose has been resolved. */
    val poseLoaded: Boolean = false,
    val lens: Lens = Lens.BACK,
    val aspect: SceneAspect = SceneAspect.RATIO_3_4,
    val flash: FlashMode = FlashMode.OFF,
    val timer: TimerOption = TimerOption.Off,
    /** Overlay transform frozen (gestures ignored, kept across pose changes). */
    val overlayLocked: Boolean = false,
    /** The user's "flip pose" toggle (anatomical mirror of the template). */
    val poseMirrored: Boolean = false,
    val overlayMode: OverlayMode = OverlayMode.MANNEQUIN,
    /** Decoded [PoseTemplate.referenceImage] (photo ghost), when the pose has one. */
    val referenceImage: ImageBitmap? = null,
    /** Decoded [PoseTemplate.outlineImage] (photo outline), when the pose has one. */
    val outlineImage: ImageBitmap? = null,
    /** Background-match snapshot (scene space, not mirrored), drawn translucent under the overlay. */
    val sceneLock: ImageBitmap? = null,
    val audienceMode: AudienceMode = AudienceMode.AUTO,
    val handRaiseEnabled: Boolean = true,
    /** The template itself has a hand above the head, so the hand-raise trigger is paused. */
    val templateRaisesHand: Boolean = false,
    val zoom: Float = 1f,
    val exposureIndex: Int = 0,
    val lastPhoto: Uri? = null,
    val detectorReady: Boolean = false,
    /** Pose detection could not be started; the camera keeps working without matching. */
    val detectorError: String? = null,
    /** Why the camera can't start or stopped working ([com.poseit.app.camera.PoseCamera.error]); null while it works. */
    val cameraError: String? = null,
    val capturing: Boolean = false,
) {
    val hasPose: Boolean get() = pose != null && pose.figures.isNotEmpty()

    /** Overlay modes that can be offered for the current pose. */
    val availableOverlayModes: List<OverlayMode>
        get() = buildList {
            add(OverlayMode.MANNEQUIN)
            if (referenceImage != null) add(OverlayMode.PHOTO_GHOST)
            if (outlineImage != null) add(OverlayMode.PHOTO_OUTLINE)
        }

    val handRaiseActive: Boolean get() = handRaiseEnabled && !templateRaisesHand
}

/**
 * High-frequency analysis result (one per processed camera frame). Only read inside draw lambdas.
 * @param people smoothed detections in SCENE space
 * @param displayPeople the same people with only confidently visible joints (for the live skeleton)
 * @param aspect scene aspect the coordinates belong to
 */
data class LiveFrame(
    val people: List<Skeleton>,
    val displayPeople: List<Skeleton>,
    val match: MatchResult,
    val aspect: SceneAspect,
) {
    companion object {
        val EMPTY = LiveFrame(emptyList(), emptyList(), MatchResult.EMPTY, SceneAspect.RATIO_3_4)
    }
}

/** The stabilised coaching hint and the current lighting hint. */
data class CoachingState(
    val hint: CoachHint? = null,
    val lighting: CoachHint? = null,
)

/** A running manual timer / photoshoot. [shot] is the photo being counted down to, [total] 0 = unlimited. */
data class SessionInfo(val shot: Int, val total: Int) {
    val isPhotoshoot: Boolean get() = total != 1
    val label: String get() = if (total == 0) "$shot" else "$shot/$total"
}

/** One-shot events for the screen. */
sealed interface CameraEvent {
    data class PhotoSaved(val uri: Uri) : CameraEvent
    data class Message(val text: String) : CameraEvent
    /** Play the white capture-flash animation. */
    data object Flash : CameraEvent
    /** Android 9 and below need WRITE_EXTERNAL_STORAGE before a photo can be saved. */
    data object RequestStoragePermission : CameraEvent
}
