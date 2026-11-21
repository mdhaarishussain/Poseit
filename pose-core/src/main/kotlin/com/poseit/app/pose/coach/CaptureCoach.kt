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
 *
 * Timelines:
 * - AUTO: Idle -> Holding (score >= threshold continuously for holdMillis) -> Countdown(countdownSeconds) -> Capture
 *   (exactly once) -> Cooldown(0f) until [onCaptured] -> Cooldown(progress) for cooldownMillis -> Idle. The auto
 *   countdown is aborted (back to Idle) when the score drops below threshold - abortMargin; with countdownSeconds = 0
 *   the hold goes straight to Capture.
 * - MANUAL ([startTimer]): Countdown(manual) -> Capture -> [onCaptured] -> next interval Countdown (shot + 1) while
 *   shots remain (0 = unlimited) -> Cooldown when the session is over. The score is ignored and never aborts it.
 *
 * Not thread-safe: drive it from one thread.
 */
class CaptureCoach(settings: CoachSettings = CoachSettings()) {

    var settings: CoachSettings = settings

    private enum class Phase { IDLE, HOLDING, COUNTDOWN, AWAITING_CAPTURE, COOLDOWN }

    private var phase = Phase.IDLE
    private var current: CaptureState = CaptureState.Idle

    private var holdStartMs = 0L
    private var countdownStartMs = 0L
    private var countdownMs = 0L
    private var countdownManual = false
    private var cooldownStartMs = 0L

    private var sessionActive = false
    private var sessionShots = 1
    private var sessionInterval = 3
    private var shotsTaken = 0

    val state: CaptureState get() = current

    /** Advances the state machine. Returns the new state. */
    fun update(score: Float, nowMillis: Long): CaptureState {
        val s = if (score.isFinite()) score else 0f
        current = step(s, nowMillis)
        return current
    }

    private fun step(score: Float, now: Long): CaptureState {
        val cfg = settings
        return when (phase) {
            // Capture was handed out; wait for the app to confirm so one match can never shoot twice.
            Phase.AWAITING_CAPTURE -> CaptureState.Cooldown(0f)

            Phase.COUNTDOWN -> {
                if (!countdownManual && (!cfg.autoCapture || score < cfg.threshold - cfg.abortMargin)) {
                    goIdle()
                    return idleStep(score, now)
                }
                val elapsed = (now - countdownStartMs).coerceAtLeast(0L)
                if (elapsed >= countdownMs) {
                    beginCapture()
                } else {
                    countdownState(elapsed)
                }
            }

            Phase.COOLDOWN -> {
                val total = cfg.cooldownMillis
                val elapsed = (now - cooldownStartMs).coerceAtLeast(0L)
                if (total <= 0L || elapsed >= total) {
                    goIdle()
                    idleStep(score, now)
                } else {
                    CaptureState.Cooldown((elapsed.toFloat() / total).coerceIn(0f, 1f))
                }
            }

            Phase.HOLDING -> {
                if (!cfg.autoCapture || score < cfg.threshold) {
                    goIdle()
                    CaptureState.Idle
                } else {
                    holdingStep(now)
                }
            }

            Phase.IDLE -> idleStep(score, now)
        }
    }

    private fun idleStep(score: Float, now: Long): CaptureState {
        val cfg = settings
        if (!cfg.autoCapture || score < cfg.threshold) return CaptureState.Idle
        phase = Phase.HOLDING
        holdStartMs = now
        return holdingStep(now)
    }

    private fun holdingStep(now: Long): CaptureState {
        val cfg = settings
        val elapsed = (now - holdStartMs).coerceAtLeast(0L)
        if (cfg.holdMillis > 0L && elapsed < cfg.holdMillis) {
            return CaptureState.Holding((elapsed.toFloat() / cfg.holdMillis).coerceIn(0f, 1f))
        }
        // Held long enough: start the auto countdown (or shoot straight away).
        startCountdown(now, cfg.countdownSeconds, manual = false)
        return if (countdownMs <= 0L) beginCapture() else countdownState(0L)
    }

    private fun startCountdown(now: Long, seconds: Int, manual: Boolean) {
        phase = Phase.COUNTDOWN
        countdownStartMs = now
        countdownMs = seconds.coerceAtLeast(0) * 1000L
        countdownManual = manual
    }

    private fun countdownState(elapsed: Long): CaptureState {
        val remaining = (countdownMs - elapsed).coerceAtLeast(0L)
        val secondsLeft = ((remaining + 999L) / 1000L).toInt()
        val progress = if (countdownMs > 0L) (elapsed.toFloat() / countdownMs).coerceIn(0f, 1f) else 1f
        return CaptureState.Countdown(
            secondsLeft = secondsLeft,
            progress = progress,
            manual = countdownManual,
            shot = if (countdownManual) shotsTaken + 1 else 1,
            totalShots = if (countdownManual) sessionShots else 1,
        )
    }

    private fun beginCapture(): CaptureState {
        phase = Phase.AWAITING_CAPTURE
        return CaptureState.Capture
    }

    private fun goIdle() {
        phase = Phase.IDLE
    }

    /**
     * Starts a manual self-timer regardless of the score. With [shots] > 1 (or 0 = unlimited) it becomes a
     * hands-free PHOTOSHOOT (interval shooting, like a self-timer camera): the first photo after [seconds], then one
     * every [intervalSeconds] until [shots] photos were taken or [cancel] is called. Auto-capture is suspended while
     * a manual session runs.
     *
     * Starting a timer replaces whatever was going on (including an auto countdown). A negative [shots] counts as 1;
     * negative durations count as 0 (the photo is taken on the next [update]).
     */
    fun startTimer(seconds: Int, nowMillis: Long, shots: Int = 1, intervalSeconds: Int = 3) {
        sessionActive = true
        sessionShots = if (shots < 0) 1 else shots
        sessionInterval = intervalSeconds.coerceAtLeast(0)
        shotsTaken = 0
        startCountdown(nowMillis, seconds, manual = true)
        current = countdownState(0L)
    }

    /** True while a manual timer / photoshoot session is running. */
    val isSessionActive: Boolean get() = sessionActive

    /** Stops a running timer / photoshoot (and an auto countdown); returns to Idle. */
    fun cancel() {
        sessionActive = false
        shotsTaken = 0
        goIdle()
        current = CaptureState.Idle
    }

    /**
     * Must be called after the app has taken (or failed to take) the photo following a [CaptureState.Capture].
     *
     * Starts the cooldown, or, in a manual photoshoot with shots remaining, the next interval countdown. A call while
     * no capture is pending (e.g. after a manual shutter press) just pauses auto-capture with a cooldown; it is
     * ignored while a timer session is counting down.
     */
    fun onCaptured(nowMillis: Long) {
        val pending = phase == Phase.AWAITING_CAPTURE
        if (!pending && sessionActive) return
        if (pending && sessionActive) {
            shotsTaken++
            if (sessionShots == 0 || shotsTaken < sessionShots) {
                startCountdown(nowMillis, sessionInterval, manual = true)
                current = countdownState(0L)
                return
            }
            sessionActive = false
        }
        phase = Phase.COOLDOWN
        cooldownStartMs = nowMillis
        current = CaptureState.Cooldown(0f)
    }

    /** Back to Idle (e.g. pose changed, screen paused). */
    fun reset() {
        sessionActive = false
        sessionShots = 1
        shotsTaken = 0
        goIdle()
        current = CaptureState.Idle
    }
}
