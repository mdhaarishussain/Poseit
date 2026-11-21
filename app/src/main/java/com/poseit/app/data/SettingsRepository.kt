package com.poseit.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

enum class OverlayStyle(val label: String) { SILHOUETTE("Silhouette"), OUTLINE("Outline"), STICK("Stick figure") }
enum class GridType(val label: String) { NONE("Off"), THIRDS("Rule of thirds"), GOLDEN("Golden ratio"), CENTER("Centre cross") }

data class AppSettings(
    val voiceGuidance: Boolean = true,
    val autoCapture: Boolean = true,
    /** 0..1 */
    val matchThreshold: Float = 0.80f,
    val countdownSeconds: Int = 3,
    val showLiveSkeleton: Boolean = true,
    val overlayStyle: OverlayStyle = OverlayStyle.SILHOUETTE,
    /** 0.15..1 */
    val overlayOpacity: Float = 0.55f,
    /** true = MediaPipe "full" model (more accurate), false = "lite" (faster). */
    val accurateModel: Boolean = false,
    /** Save front-camera photos mirrored (like the preview) instead of true orientation. */
    val mirrorFrontPhotos: Boolean = false,
    val grid: GridType = GridType.THIRDS,
    val showLevel: Boolean = true,
    val haptics: Boolean = true,
    val shutterSound: Boolean = true,
    val onboardingDone: Boolean = false,
)

/** Countdown lengths offered by both Settings and the camera's quick settings, so each shows the other's choice. */
val COUNTDOWN_CHOICES = listOf(0, 2, 3, 5, 10)

/** The offered countdown closest to [seconds] (an exact match when it is one of [COUNTDOWN_CHOICES]). */
fun countdownChoiceFor(seconds: Int): Int = COUNTDOWN_CHOICES.minBy { abs(it - seconds) }

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val voice = booleanPreferencesKey("voice")
        val auto = booleanPreferencesKey("auto_capture")
        val threshold = floatPreferencesKey("threshold")
        val countdown = intPreferencesKey("countdown")
        val skeleton = booleanPreferencesKey("live_skeleton")
        val style = stringPreferencesKey("overlay_style")
        val opacity = floatPreferencesKey("overlay_opacity")
        val accurate = booleanPreferencesKey("accurate_model")
        val mirror = booleanPreferencesKey("mirror_front")
        val grid = stringPreferencesKey("grid")
        val level = booleanPreferencesKey("level")
        val haptics = booleanPreferencesKey("haptics")
        val sound = booleanPreferencesKey("shutter_sound")
        val onboarding = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.voice] = s.voiceGuidance
            prefs[Keys.auto] = s.autoCapture
            prefs[Keys.threshold] = s.matchThreshold.coerceIn(0.5f, 0.98f)
            prefs[Keys.countdown] = s.countdownSeconds.coerceIn(0, 10)
            prefs[Keys.skeleton] = s.showLiveSkeleton
            prefs[Keys.style] = s.overlayStyle.name
            prefs[Keys.opacity] = s.overlayOpacity.coerceIn(0.15f, 1f)
            prefs[Keys.accurate] = s.accurateModel
            prefs[Keys.mirror] = s.mirrorFrontPhotos
            prefs[Keys.grid] = s.grid.name
            prefs[Keys.level] = s.showLevel
            prefs[Keys.haptics] = s.haptics
            prefs[Keys.sound] = s.shutterSound
            prefs[Keys.onboarding] = s.onboardingDone
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            voiceGuidance = this[Keys.voice] ?: d.voiceGuidance,
            autoCapture = this[Keys.auto] ?: d.autoCapture,
            matchThreshold = this[Keys.threshold] ?: d.matchThreshold,
            countdownSeconds = this[Keys.countdown] ?: d.countdownSeconds,
            showLiveSkeleton = this[Keys.skeleton] ?: d.showLiveSkeleton,
            overlayStyle = this[Keys.style]?.let { runCatching { OverlayStyle.valueOf(it) }.getOrNull() } ?: d.overlayStyle,
            overlayOpacity = this[Keys.opacity] ?: d.overlayOpacity,
            accurateModel = this[Keys.accurate] ?: d.accurateModel,
            mirrorFrontPhotos = this[Keys.mirror] ?: d.mirrorFrontPhotos,
            grid = this[Keys.grid]?.let { runCatching { GridType.valueOf(it) }.getOrNull() } ?: d.grid,
            showLevel = this[Keys.level] ?: d.showLevel,
            haptics = this[Keys.haptics] ?: d.haptics,
            shutterSound = this[Keys.sound] ?: d.shutterSound,
            onboardingDone = this[Keys.onboarding] ?: d.onboardingDone,
        )
    }
}
