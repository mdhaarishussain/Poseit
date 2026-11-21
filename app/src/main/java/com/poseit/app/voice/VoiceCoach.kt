package com.poseit.app.voice

import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.Closeable
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Spoken guidance (Android TextToSpeech) + short audio/haptic cues.
 *
 * - [say] is rate-limited: it ignores a message identical to one spoken in the last [repeatGapMs], and
 *   never interrupts an utterance in progress unless [urgent].
 * - Silently does nothing if TTS is unavailable or [enabled] is false.
 *
 * [tick], [shutter] and [haptic] are independent of [enabled] (the caller gates them by its own settings).
 * All methods are thread-safe and never throw; after [close] they do nothing.
 */
class VoiceCoach(private val context: Context) : Closeable {

    @Volatile
    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) stop()
        }

    /** Minimum gap between two different non-urgent hints. */
    @Volatile
    var minGapMs: Long = 2500L

    /** Minimum gap before repeating the same hint. */
    @Volatile
    var repeatGapMs: Long = 6000L

    private val appContext: Context = context.applicationContext ?: context
    private val lock = Any()
    private val closed = AtomicBoolean(false)
    private val utteranceIds = AtomicLong()

    // Guarded by lock.
    private val gate = SpeechGate()
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var initStatus: Int? = null
    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorFailed = false
    private var shutterSound: MediaActionSound? = null
    private var vibrator: Vibrator? = null
    private var vibratorResolved = false

    /** Declared before init: the TTS engine may be configured while the constructor is still running. */
    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finished()

        // Abstract (so it must be implemented) but deprecated since API 21 in favour of the overload below.
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) = finished()

        override fun onError(utteranceId: String?, errorCode: Int) = finished()

        override fun onStop(utteranceId: String?, interrupted: Boolean) = finished()

        private fun finished() {
            synchronized(lock) { gate.onFinished(SystemClock.elapsedRealtime()) }
        }
    }

    init {
        // The init callback may run before the constructor returns (e.g. no TTS engine installed), so the engine
        // is configured by whichever of the two (callback / assignment below) happens last.
        val engine = try {
            TextToSpeech(appContext) { status -> onTtsInit(status) }
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech unavailable", e)
            null
        }
        synchronized(lock) {
            tts = engine
            configureTtsLocked()
        }
        // Preload the shutter sound so the first photo doesn't wait for it.
        synchronized(lock) { shutterSoundLocked() }
    }

    fun say(text: String, urgent: Boolean = false) {
        if (!enabled || closed.get()) return
        val message = text.trim()
        if (message.isEmpty()) return
        synchronized(lock) {
            val engine = tts?.takeIf { ttsReady } ?: return
            val speaking = try {
                engine.isSpeaking
            } catch (e: Exception) {
                false
            }
            val now = SystemClock.elapsedRealtime()
            if (!gate.tryAcquire(message, urgent, now, speaking, minGapMs, repeatGapMs)) return
            val queueMode = if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "poseit-${utteranceIds.incrementAndGet()}"
            val result = try {
                engine.speak(message, queueMode, Bundle(), utteranceId)
            } catch (e: Exception) {
                Log.w(TAG, "TextToSpeech.speak failed", e)
                TextToSpeech.ERROR
            }
            if (result != TextToSpeech.SUCCESS) gate.onFinished(now)
        }
    }

    /** Short beep for countdown ticks. */
    fun tick() {
        if (closed.get()) return
        synchronized(lock) {
            val generator = toneGeneratorLocked() ?: return
            try {
                generator.startTone(ToneGenerator.TONE_PROP_BEEP, TICK_DURATION_MS)
            } catch (e: Exception) {
                Log.w(TAG, "Beep failed", e)
            }
        }
    }

    /** Camera shutter sound (respects the device's shutter-sound policy via MediaActionSound). */
    fun shutter() {
        if (closed.get()) return
        synchronized(lock) {
            val sound = shutterSoundLocked() ?: return
            try {
                sound.play(MediaActionSound.SHUTTER_CLICK)
            } catch (e: Exception) {
                Log.w(TAG, "Shutter sound failed", e)
            }
        }
    }

    /** Short vibration. */
    fun haptic() {
        if (closed.get()) return
        val vib = synchronized(lock) { vibratorLocked() } ?: return
        try {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(HAPTIC_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(HAPTIC_DURATION_MS)
            }
        } catch (e: Exception) {
            // SecurityException without the VIBRATE permission, or a vendor quirk: haptics are optional.
            Log.w(TAG, "Vibration failed", e)
        }
    }

    fun stop() {
        synchronized(lock) {
            gate.reset()
            try {
                tts?.takeIf { ttsReady }?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "TextToSpeech.stop failed", e)
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(lock) {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.w(TAG, "TextToSpeech shutdown failed", e)
            }
            tts = null
            ttsReady = false
            toneGenerator?.release()
            toneGenerator = null
            shutterSound?.release()
            shutterSound = null
            vibrator = null
        }
    }

    // ------------------------------------------------------------------ TTS setup

    private fun onTtsInit(status: Int) {
        synchronized(lock) {
            initStatus = status
            configureTtsLocked()
        }
    }

    private fun configureTtsLocked() {
        val engine = tts ?: return
        val status = initStatus ?: return
        if (ttsReady) return
        if (closed.get() || status != TextToSpeech.SUCCESS) {
            if (status != TextToSpeech.SUCCESS) Log.w(TAG, "TextToSpeech init failed ($status)")
            runCatching { engine.shutdown() }
            tts = null
            return
        }
        try {
            val locale = pickLocale(engine)
            if (locale != null) engine.setLanguage(locale)
            engine.setSpeechRate(SPEECH_RATE)
            engine.setOnUtteranceProgressListener(progressListener)
            ttsReady = true
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech configuration failed", e)
            runCatching { engine.shutdown() }
            tts = null
        }
    }

    /**
     * The device locale if the engine supports it and it speaks the hints' language ([SPEECH_LANGUAGE]; keeps
     * regional accents such as en-IN or en-GB), else US English; null keeps the engine's default voice.
     * The coaching texts are English, and an English sentence read by e.g. a German voice is hard to follow.
     */
    private fun pickLocale(engine: TextToSpeech): Locale? {
        fun supported(locale: Locale) = try {
            engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
        } catch (e: Exception) {
            false
        }
        val preferred = Locale.getDefault()
        return when {
            preferred.language == SPEECH_LANGUAGE && supported(preferred) -> preferred
            supported(Locale.US) -> Locale.US
            supported(preferred) -> preferred
            else -> null
        }
    }

    // ------------------------------------------------------------------ lazily created cue resources

    private fun toneGeneratorLocked(): ToneGenerator? {
        if (toneGenerator == null && !toneGeneratorFailed) {
            toneGenerator = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
            } catch (e: RuntimeException) {
                // Thrown when the audio system has no free tone slots; skip beeps rather than crash.
                Log.w(TAG, "ToneGenerator unavailable", e)
                toneGeneratorFailed = true
                null
            }
        }
        return toneGenerator
    }

    private fun shutterSoundLocked(): MediaActionSound? {
        if (shutterSound == null && !closed.get()) {
            shutterSound = try {
                MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }
            } catch (e: Exception) {
                Log.w(TAG, "MediaActionSound unavailable", e)
                null
            }
        }
        return shutterSound
    }

    private fun vibratorLocked(): Vibrator? {
        if (!vibratorResolved) {
            vibratorResolved = true
            vibrator = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (e: Exception) {
                Log.w(TAG, "Vibrator unavailable", e)
                null
            }
        }
        return vibrator
    }

    private companion object {
        const val TAG = "VoiceCoach"
        const val SPEECH_RATE = 1.05f

        /** ISO 639 language of the texts passed to [say]. */
        const val SPEECH_LANGUAGE = "en"
        const val TICK_DURATION_MS = 120
        const val TONE_VOLUME = 80
        const val HAPTIC_DURATION_MS = 30L
    }
}
