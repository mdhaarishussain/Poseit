package com.poseit.app.voice

import android.content.Context
import java.io.Closeable

/**
 * Spoken guidance (Android TextToSpeech) + short audio/haptic cues.
 *
 * - [say] is rate-limited: it ignores a message identical to one spoken in the last [repeatGapMs], and
 *   never interrupts an utterance in progress unless [urgent].
 * - Silently does nothing if TTS is unavailable or [enabled] is false.
 */
class VoiceCoach(private val context: Context) : Closeable {

    var enabled: Boolean = true

    /** Minimum gap between two different non-urgent hints. */
    var minGapMs: Long = 2500L

    /** Minimum gap before repeating the same hint. */
    var repeatGapMs: Long = 6000L

    fun say(text: String, urgent: Boolean = false) {
        TODO("implemented by the camera-infra agent")
    }

    /** Short beep for countdown ticks. */
    fun tick() {
        TODO("implemented by the camera-infra agent")
    }

    /** Camera shutter sound (respects the device's shutter-sound policy via MediaActionSound). */
    fun shutter() {
        TODO("implemented by the camera-infra agent")
    }

    /** Short vibration. */
    fun haptic() {
        TODO("implemented by the camera-infra agent")
    }

    fun stop() {
        TODO("implemented by the camera-infra agent")
    }

    override fun close() {
        TODO("implemented by the camera-infra agent")
    }
}
