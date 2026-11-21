package com.poseit.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Colour tokens of the "Studio" design system (docs/DESIGN.md).
 *
 * The app screens are a cool grey seamless-paper backdrop ([Paper]); poses are white lay figures ([Mannequin])
 * standing on a patch of that paper ([Set]); raised surfaces are lighter paper ([Sheet]); chrome is [Ink] and
 * [Graphite]. The camera is the true-black [LiveView].
 *
 * Signal colours ([Miss], [Near], [Match] and their `Bright` variants for the dark camera) are functional only:
 * the live match score, per-limb feedback and destructive actions. Never use them as decoration, and never as the
 * only carrier of meaning (pair them with text or a shape). There is no brand accent colour.
 */
object Studio {
    /** App background: the seamless backdrop. */
    val Paper = Color(0xFFE4E5E1)

    /** The patch of paper a figure stands on: pose tiles, previews, the editor canvas. */
    val Set = Color(0xFFD2D3CF)

    /** Raised surfaces: bottom sheets, dialogs, menus, text fields, the camera cue card. */
    val Sheet = Color(0xFFF3F3F0)

    /** Text, icons, primary buttons, selected states. */
    val Ink = Color(0xFF17181A)

    /** Secondary text, inactive icons, helper text. 5.3:1 on [Paper]; keep it off [Set] for small text. */
    val Graphite = Color(0xFF5A5C5B)

    /** Rules that encode grouping: row dividers, the tab track, outlines. Ink at 12%. */
    val Hairline = Ink.copy(alpha = 0.12f)

    /** Figure fill. */
    val Mannequin = Color(0xFFFBFBF9)

    /** Figure contour (Ink at 55%). The face dot is plain [Ink]. */
    val MannequinContour = Ink.copy(alpha = 0.55f)

    /** Camera surround. True black, because it is a viewfinder. */
    val LiveView = Color(0xFF000000)

    /** Round camera controls sit on black at 45% (an active toggle inverts to a [Sheet] disc with an [Ink] icon). */
    val ControlScrim = LiveView.copy(alpha = 0.45f)

    /** Signal: no match / destructive. 4.6:1 on [Paper]. */
    val Miss = Color(0xFFC8372D)

    /** Signal: almost there. */
    val Near = Color(0xFFD9930D)

    /** Signal: matched. */
    val Match = Color(0xFF24935A)

    /** [Miss] for use on the dark camera. */
    val MissBright = Color(0xFFFF6B5E)

    /** [Near] for use on the dark camera. */
    val NearBright = Color(0xFFFFC24A)

    /** [Match] for use on the dark camera. */
    val MatchBright = Color(0xFF4FD18B)

    /**
     * Fill tones for the people in a multi-person pose, in order. They differ by tone, not hue, and all take the
     * same [MannequinContour]. Index with `i % FigureTones.size`.
     */
    val FigureTones: List<Color> = listOf(Mannequin, Color(0xFFC9CBC6), Color(0xFFA6A8A3), Color(0xFF83857F))

    /**
     * Miss -> Near -> Match for a 0..1 match score: [Miss] below 0.35, blending to [Near] at 0.6 and to [Match]
     * at 0.8 and above.
     * @param onDark true (the default) for the camera's black surround, which uses the `Bright` variants
     */
    fun forScore(score: Float, onDark: Boolean = true): Color {
        val miss = if (onDark) MissBright else Miss
        val near = if (onDark) NearBright else Near
        val match = if (onDark) MatchBright else Match
        return when {
            score >= 0.8f -> match
            score >= 0.6f -> lerp(near, match, ((score - 0.6f) / 0.2f).coerceIn(0f, 1f))
            score >= 0.35f -> lerp(miss, near, ((score - 0.35f) / 0.25f).coerceIn(0f, 1f))
            else -> miss
        }
    }
}

/**
 * TEMPORARY shim that maps the pre-Studio colour names onto [Studio] tokens so unconverted screens still compile.
 * Every usage is being replaced; this object will be deleted.
 */
@Deprecated("Use the Studio tokens (ui/theme/Color.kt). Temporary shim, to be deleted.")
object PoseitColors {
    val Violet = Studio.Ink
    val VioletDark = Studio.Ink
    val Coral = Studio.Ink
    val Mint = Studio.Match
    val Amber = Studio.Near
    val Red = Studio.Miss
    val Background = Studio.Paper
    val Surface = Studio.Sheet
    val SurfaceHigh = Studio.Set
    val OnSurfaceMuted = Studio.Graphite
    val FigureFill = Studio.Mannequin
    val FigureOutline = Studio.Ink
    val FigureTints = Studio.FigureTones

    fun forScore(score: Float): Color = Studio.forScore(score)
}
