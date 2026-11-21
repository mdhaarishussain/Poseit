package com.poseit.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/*
 * Poseit design system v2 colour (docs/DESIGN.md, "Colour").
 *
 * The system follows Astryx's token model (github.com/meta/astryx, MIT): components read SEMANTIC roles
 * ([PoseitColors]), never raw palette values, and a theme is a set of values assigned to those roles. The roles are
 * named with Astryx's colour grammar in camelCase: `background<Surface>`, `text<Emphasis>`, `border`, `accent` and
 * `onAccent`, `track`, `overlay`, and the `Inverted` suffix for content on an inverted surface. Poseit's one theme
 * takes its values from the design mockups: a warm off-white canvas, deep green-black cards with cream text, and ONE
 * lavender accent that always carries dark text.
 */

/**
 * The raw palette: the values of Poseit's one theme. Components and screens never read these directly; they read
 * the semantic roles in [PoseitColors], which point at them (as an Astryx theme maps its palette refs onto tokens).
 */
object PoseitPalette {
    /** Raised cream: sheets, dialogs, menus, outlined rows, fields, the figure fill. */
    val Cream50 = Color(0xFFFBFAF7)

    /** The canvas every light screen sits on. */
    val Cream100 = Color(0xFFF4F3EF)

    /** Pale fill on the canvas: placeholders, inactive segments, pressed rows. */
    val Cream200 = Color(0xFFE7E7E2)

    /** The huge pale numerals on cream (display only; see [PoseitColors.textNumeralPale]). */
    val Fog = Color(0xFFD6D9D6)

    /** Deep green-black: dark cards, the top bar, text and icons on cream. */
    val Teal950 = Color(0xFF0A2125)

    /** One step up from [Teal950]: a tile or pressed area inside a dark card. */
    val Teal900 = Color(0xFF132D31)

    /** Tracks and empty progress segments on a dark card. */
    val Teal800 = Color(0xFF1E383D)

    /** Secondary text on cream. */
    val Slate600 = Color(0xFF586563)

    /** Secondary text on a dark card. */
    val Mist300 = Color(0xFFAEB7B5)

    /** The one accent. Always with [Teal950] content on it. */
    val Lavender200 = Color(0xFFCFC9F8)

    /** Pale accent: a selected row or option tint on cream. */
    val Lavender100 = Color(0xFFE6E2FC)

    /** Signal red, readable as text on cream. */
    val Red600 = Color(0xFFB8382A)

    /** Signal amber, readable as text on cream. */
    val Amber700 = Color(0xFF9A6408)

    /** Signal green, readable as text on cream. */
    val Green700 = Color(0xFF1F7A50)

    /** Bright signal red for dark surfaces (camera, dark cards). */
    val Red300 = Color(0xFFFF7B6B)

    /** Bright signal amber for dark surfaces. */
    val Amber300 = Color(0xFFFFC857)

    /** Bright signal green for dark surfaces. */
    val Green300 = Color(0xFF63DB9E)

    /** Second, third and fourth figure tones (teal-tinted greys). */
    val FigureGrey1 = Color(0xFFCFD6D4)
    val FigureGrey2 = Color(0xFFA3AFAD)
    val FigureGrey3 = Color(0xFF7D8B89)

    /** The camera surround: true black, because it is a viewfinder. */
    val Black = Color(0xFF000000)
}

/**
 * Semantic colour roles (Astryx grammar). Contrast ratios are WCAG 2.x, measured against the surface each role is
 * meant for; docs/DESIGN.md lists every text pairing.
 *
 * Which content roles to use depends on the surface underneath: on the canvas and cream surfaces use `text*`, on a
 * dark card or the top bar use `text*Inverted`. Components pick these up automatically from [LocalSurfaceTone]
 * (cards provide it); [toneColors] returns the right set for custom drawing.
 */
object PoseitColors {
    // ---- Backgrounds (Astryx --color-background-*) ----

    /** The canvas behind every light screen. */
    val backgroundBody = PoseitPalette.Cream100

    /** Raised cream: bottom sheets, dialogs, menus, outlined rows and row groups, text fields. */
    val backgroundSurface = PoseitPalette.Cream50

    /** The dark card (this theme's cards are inverted): content cards, connected stacks, the top bar, pose tiles. */
    val backgroundCard = PoseitPalette.Teal950

    /** A tile or pressed area inside a dark card, and the inactive track of the segmented pill. */
    val backgroundCardRaised = PoseitPalette.Teal900

    /** Pale fill on the canvas: image placeholders, inactive progress segments, pressed outlined rows. */
    val backgroundMuted = PoseitPalette.Cream200

    /** Where lay figures stand: pose tiles, the detail preview, the editor canvas. The dark card colour. */
    val backgroundStage = PoseitPalette.Teal950

    /** The camera surround and the full-screen photo viewer. */
    val backgroundCamera = PoseitPalette.Black

    /** Modal scrim behind sheets and dialogs. */
    val overlay = PoseitPalette.Teal950.copy(alpha = 0.40f)

    // ---- Content on the canvas and cream surfaces ----

    /** Text and icons on cream. 15.0:1 on the canvas, 16.0:1 on surface. */
    val textPrimary = PoseitPalette.Teal950

    /** Secondary text and inactive icons on cream. 5.5:1 on the canvas, 5.8:1 on surface. */
    val textSecondary = PoseitPalette.Slate600

    /** Disabled text and icons on cream (exempt from contrast minimums). */
    val textDisabled = PoseitPalette.Teal950.copy(alpha = 0.38f)

    /**
     * Huge pale numerals on cream, like a timer on a training screen (1.3:1). DISPLAY ONLY: use it only where the
     * same number is also given as readable text or in semantics, never as the only carrier of a value.
     */
    val textNumeralPale = PoseitPalette.Fog

    // ---- Content on a dark card (Astryx -inverted suffix) ----

    /** Text and icons on a dark card. 15.0:1. */
    val textPrimaryInverted = PoseitPalette.Cream100

    /** Secondary text on a dark card. 8.1:1 (7.1:1 on [backgroundCardRaised]). */
    val textSecondaryInverted = PoseitPalette.Mist300

    /** Disabled text on a dark card. */
    val textDisabledInverted = PoseitPalette.Cream100.copy(alpha = 0.38f)

    // ---- Accent (Astryx --color-accent / --color-on-accent) ----

    /**
     * The one accent: badges, filled progress segments, the selected option of a segmented pill, the main call to
     * action, a switch's thumb when on. Never as text on cream (1.4:1). Always with [onAccent] content.
     */
    val accent = PoseitPalette.Lavender200

    /** Content on [accent]. 10.6:1. */
    val onAccent = PoseitPalette.Teal950

    /** Pale accent tint on cream, e.g. a selected option row. [textPrimary] on it is 13.2:1. */
    val accentMuted = PoseitPalette.Lavender100

    // ---- Borders and tracks ----

    /** The 1dp outline of outlined rows, outlined pill buttons and unselected filter pills on cream. */
    val borderEmphasized = PoseitPalette.Teal950

    /** Hairline dividers between rows of one group, and quiet edges (menus, snackbars over the camera). */
    val border = PoseitPalette.Teal950.copy(alpha = 0.12f)

    /** The 1dp outline of an outlined button on a dark card. 8.1:1 against the card. */
    val borderEmphasizedInverted = PoseitPalette.Mist300

    /** Hairlines on a dark card. */
    val borderInverted = PoseitPalette.Cream100.copy(alpha = 0.16f)

    /** Slider and progress tracks, empty segments on cream. */
    val track = PoseitPalette.Teal950.copy(alpha = 0.12f)

    /** Slider and progress tracks, empty segments on a dark card. */
    val trackInverted = PoseitPalette.Teal800

    // ---- Signals (match feedback and destructive actions; never decoration, never colour alone) ----

    /** Signal: no match; destructive actions. 5.2:1 on the canvas, so it also works as text. */
    val signalMiss = PoseitPalette.Red600

    /** Signal: almost there. 4.5:1 on the canvas. */
    val signalNear = PoseitPalette.Amber700

    /** Signal: matched. 4.8:1 on the canvas. */
    val signalMatch = PoseitPalette.Green700

    /** [signalMiss] on dark surfaces (camera, dark cards). 6.6:1 on a dark card, 8.3:1 on black. */
    val signalMissBright = PoseitPalette.Red300

    /** [signalNear] on dark surfaces. 10.9:1 on a dark card. */
    val signalNearBright = PoseitPalette.Amber300

    /** [signalMatch] on dark surfaces. 9.7:1 on a dark card. */
    val signalMatchBright = PoseitPalette.Green300

    /** Content on a filled [signalMiss] (the confirming button of a delete dialog). 5.5:1. */
    val onSignal = PoseitPalette.Cream50

    /** Astryx intent alias: destructive actions and errors on cream. */
    val error = signalMiss

    /** Astryx intent alias: errors on dark surfaces. */
    val errorInverted = signalMissBright

    // ---- Lay figures ----

    /** Figure fill: reads on a dark stage by its own lightness and on cream by its [figureContour]. */
    val figureFill = PoseitPalette.Cream50

    /** Figure contour: [textPrimary] at 55%. Defines the figure on cream; melts into a dark stage. */
    val figureContour = PoseitPalette.Teal950.copy(alpha = 0.55f)

    /** The dot that shows which way a figure faces. */
    val figureFace = PoseitPalette.Teal950

    /** The flat floor spot under a figure's feet on a dark stage (no gradient, no shadow). */
    val figureFloorOnDark = PoseitPalette.Cream100.copy(alpha = 0.10f)

    /** The flat floor spot under a figure's feet on cream. */
    val figureFloorOnLight = PoseitPalette.Teal950.copy(alpha = 0.08f)

    /**
     * Fill tones for the people in a multi-person pose, in order. They differ by tone, not hue, all take the same
     * [figureContour], and all read on the dark stage (the darkest is 4.7:1 against it). Index with
     * `i % figureTones.size`.
     */
    val figureTones: List<Color> = listOf(figureFill, PoseitPalette.FigureGrey1, PoseitPalette.FigureGrey2, PoseitPalette.FigureGrey3)

    // ---- Camera ----

    /** The disc behind a camera control: black at 45%, so it reads over any scene. */
    val cameraControlScrim = PoseitPalette.Black.copy(alpha = 0.45f)

    /** An active camera toggle inverts to this disc, with [textPrimary] glyph. */
    val cameraControlActive = PoseitPalette.Cream50

    /** Camera glyphs and text over the live view. */
    val cameraContent = PoseitPalette.Cream50

    /**
     * Miss -> Near -> Match for a 0..1 match score: miss below 0.35, blending to near at 0.6 and to match at 0.8
     * and above. Always pair it with the number or a word; colour is never the only signal.
     * @param onDark true (the default) for the camera and dark cards, which use the `Bright` variants
     */
    fun forScore(score: Float, onDark: Boolean = true): Color {
        val miss = if (onDark) signalMissBright else signalMiss
        val near = if (onDark) signalNearBright else signalNear
        val match = if (onDark) signalMatchBright else signalMatch
        return when {
            score >= 0.8f -> match
            score >= 0.6f -> lerp(near, match, ((score - 0.6f) / 0.2f).coerceIn(0f, 1f))
            score >= 0.35f -> lerp(miss, near, ((score - 0.35f) / 0.25f).coerceIn(0f, 1f))
            else -> miss
        }
    }

    /** Content roles for content on cream. */
    val onLight = ToneColors(
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textDisabled = textDisabled,
        border = border,
        borderEmphasized = borderEmphasized,
        track = track,
        signalMiss = signalMiss,
    )

    /** Content roles for content on a dark card, the top bar or the camera. */
    val onDark = ToneColors(
        textPrimary = textPrimaryInverted,
        textSecondary = textSecondaryInverted,
        textDisabled = textDisabledInverted,
        border = borderInverted,
        borderEmphasized = borderEmphasizedInverted,
        track = trackInverted,
        signalMiss = signalMissBright,
    )
}

/** Whether content sits on a light (canvas, cream) or a dark (card, top bar, camera) surface. */
enum class SurfaceTone { Light, Dark }

/**
 * The tone of the surface under the current content. Dark cards, the top bar card, connected stacks and the camera
 * provide [SurfaceTone.Dark]; buttons, rows, icons and text roles read it to choose their default colours, so a
 * SecondaryButton inside a dark card gets a light outline without being told.
 */
val LocalSurfaceTone = compositionLocalOf { SurfaceTone.Light }

/** The content roles for one surface tone: [PoseitColors.onLight] or [PoseitColors.onDark]. */
@Immutable
data class ToneColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val border: Color,
    val borderEmphasized: Color,
    val track: Color,
    val signalMiss: Color,
)

/** Content roles for the surface under the current content ([LocalSurfaceTone]). */
@Composable
@ReadOnlyComposable
fun toneColors(): ToneColors = if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.onDark else PoseitColors.onLight
