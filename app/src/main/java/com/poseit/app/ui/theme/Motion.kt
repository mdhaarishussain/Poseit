package com.poseit.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Motion tokens (docs/DESIGN.md, "Motion"). Motion answers the user's action only, in 150 to 250ms. The capture
 * countdown and flash are the one orchestrated moment and keep their own timing.
 *
 * The easing curves are Astryx's `--ease-standard` (decelerate: arrivals and state changes) and `--ease-exit`
 * (accelerate: things leaving).
 */
object PoseitMotion {
    /** 150ms: colour and selection changes (a pill option, a switch, a pressed row). */
    const val FAST = 150

    /** 200ms: small moves (the segmented pill's selection sliding, a badge appearing). */
    const val MEDIUM = 200

    /** 250ms: larger moves (a card expanding, a sheet's content changing). */
    const val SLOW = 250

    /** Astryx --ease-standard: cubic-bezier(0.24, 1, 0.4, 1). */
    val EaseStandard: Easing = CubicBezierEasing(0.24f, 1f, 0.4f, 1f)

    /** Astryx --ease-exit: cubic-bezier(0.3, 0, 0.6, 0.6). */
    val EaseExit: Easing = CubicBezierEasing(0.3f, 0f, 0.6f, 0.6f)

    /** A tween with the standard easing. */
    fun <T> standard(durationMillis: Int = MEDIUM): FiniteAnimationSpec<T> = tween(durationMillis, easing = EaseStandard)
}
