package com.poseit.app.pose.coach

/**
 * Settings for hands-free capture.
 * @param autoCapture take the photo automatically when the pose matches
 * @param threshold match score (0..1) that counts as "matched"
 * @param holdMillis how long the score must stay >= threshold before the countdown starts
 * @param countdownSeconds countdown length (0 = capture immediately after the hold)
 * @param cooldownMillis pause after a capture before auto-capture can trigger again
 * @param abortMargin during an AUTO countdown, if the score drops below threshold - abortMargin the countdown is
 *   cancelled (the manual self-timer is never cancelled)
 */
data class CoachSettings(
    val autoCapture: Boolean = true,
    val threshold: Float = 0.80f,
    val holdMillis: Long = 700L,
    val countdownSeconds: Int = 3,
    val cooldownMillis: Long = 2500L,
    val abortMargin: Float = 0.12f,
)

sealed interface CaptureState {
    /** Waiting for a match. */
    data object Idle : CaptureState

    /** Score is above threshold; [progress] 0..1 until the countdown starts. */
    data class Holding(val progress: Float) : CaptureState

    /**
     * Counting down. [secondsLeft] is what to display (3, 2, 1). [manual] = started by the self-timer / photoshoot
     * button. [shot] is the 1-based number of the photo being counted down to, [totalShots] the session size
     * (1 for a plain timer or auto-capture, 0 = unlimited photoshoot until [CaptureCoach.cancel]).
     */
    data class Countdown(
        val secondsLeft: Int,
        val progress: Float,
        val manual: Boolean,
        val shot: Int = 1,
        val totalShots: Int = 1,
    ) : CaptureState

    /** Emitted for exactly one update: the app must take the photo now and then call [CaptureCoach.onCaptured]. */
    data object Capture : CaptureState

    /** Recently captured; auto-capture paused. */
    data class Cooldown(val progress: Float) : CaptureState
}

/**
 * Pure, clock-driven state machine for auto-capture and the self-timer. Call [update] for every analysed frame
 * (or on a ~30 Hz ticker) with the latest match score.
 */
class CaptureCoach(settings: CoachSettings = CoachSettings()) {

    var settings: CoachSettings = settings

    val state: CaptureState get() = TODO("implemented by the engine agent")

    /** Advances the state machine. Returns the new state. */
    fun update(score: Float, nowMillis: Long): CaptureState {
        TODO("implemented by the engine agent")
    }

    /**
     * Starts a manual self-timer regardless of the score. With [shots] > 1 (or 0 = unlimited) it becomes a
     * hands-free PHOTOSHOOT (interval shooting, like a self-timer camera): the first photo after [seconds], then one
     * every [intervalSeconds] until [shots] photos were taken or [cancel] is called. Auto-capture is suspended while
     * a manual session runs.
     */
    fun startTimer(seconds: Int, nowMillis: Long, shots: Int = 1, intervalSeconds: Int = 3) {
        TODO("implemented by the engine agent")
    }

    /** True while a manual timer / photoshoot session is running. */
    val isSessionActive: Boolean get() = TODO("implemented by the engine agent")

    /** Stops a running timer / photoshoot (and an auto countdown); returns to Idle. */
    fun cancel() {
        TODO("implemented by the engine agent")
    }

    /** Must be called after the app has taken (or failed to take) the photo following a [CaptureState.Capture]. */
    fun onCaptured(nowMillis: Long) {
        TODO("implemented by the engine agent")
    }

    /** Back to Idle (e.g. pose changed, screen paused). */
    fun reset() {
        TODO("implemented by the engine agent")
    }
}
