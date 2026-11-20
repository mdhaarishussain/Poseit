package com.poseit.app.voice

/**
 * Decides whether a spoken hint may play now (pure logic, clock injected, so it is unit-testable).
 *
 * - A message identical to the last one spoken within `repeatGapMs` is always dropped (urgent or not).
 * - A non-urgent message is dropped while something is being spoken, and within `minGapMs` of the last
 *   utterance (measured from when it finished, or started if it hasn't reported finishing).
 * - An urgent message plays immediately (the caller flushes the queue).
 *
 * Not thread-safe; callers synchronize.
 */
internal class SpeechGate {

    private var lastText: String? = null
    private var lastStartedAt = NEVER
    private var lastFinishedAt = NEVER

    /** Returns true (and records the utterance) if [text] should be spoken at [nowMs]. */
    fun tryAcquire(
        text: String,
        urgent: Boolean,
        nowMs: Long,
        speaking: Boolean,
        minGapMs: Long,
        repeatGapMs: Long,
    ): Boolean {
        if (text == lastText && lastStartedAt != NEVER && nowMs - lastStartedAt < repeatGapMs) return false
        if (!urgent) {
            if (speaking) return false
            val lastActivity = maxOf(lastStartedAt, lastFinishedAt)
            if (lastActivity != NEVER && nowMs - lastActivity < minGapMs) return false
        }
        lastText = text
        lastStartedAt = nowMs
        return true
    }

    /** The current utterance finished (or failed / was stopped). */
    fun onFinished(nowMs: Long) {
        lastFinishedAt = nowMs
    }

    /** Forgets the last message so it may be repeated right away (e.g. after the user paused the camera). */
    fun reset() {
        lastText = null
        lastStartedAt = NEVER
        lastFinishedAt = NEVER
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE
    }
}
