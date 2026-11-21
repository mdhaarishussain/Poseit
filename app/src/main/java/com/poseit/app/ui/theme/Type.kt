package com.poseit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.poseit.app.R

/** Archivo at normal width (OFL, bundled in res/font): body text, labels, row titles. */
val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
)

/**
 * The expanded cut of Archivo, which recalls the lettering engraved on camera bodies: display, titles, numerals.
 * Medium, SemiBold and Bold only.
 */
val ArchivoExpanded = FontFamily(
    Font(R.font.archivo_expanded_medium, FontWeight.Medium),
    Font(R.font.archivo_expanded_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_expanded_bold, FontWeight.Bold),
)

private val Base = TextStyle(
    fontFamily = Archivo,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/**
 * The Studio type roles (docs/DESIGN.md). Prefer these names in new code; the M3 [Typography] slots are mapped onto
 * the same styles (see [StudioTypography]) so stock components pick them up.
 *
 * Rules: sentence case everywhere (no caps, so no tracking either); never accent one word of a headline; keep
 * lines under about 60 characters.
 */
object StudioType {
    /** Expanded SemiBold 28/32: pose name on the detail screen, onboarding headline. */
    val display = Base.copy(
        fontFamily = ArchivoExpanded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    )

    /** Expanded SemiBold 22/28: the "Poseit" wordmark in the library's top bar. */
    val wordmark = display.copy(fontSize = 22.sp, lineHeight = 28.sp)

    /** Expanded Medium 20/26: section headings, sheet and dialog titles. */
    val title = Base.copy(fontFamily = ArchivoExpanded, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp)

    /** Expanded Medium 18/24: screen titles in top bars. */
    val barTitle = title.copy(fontSize = 18.sp, lineHeight = 24.sp)

    /** Regular 15/22: paragraphs, descriptions, tips. */
    val body = Base.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)

    /** Medium 15/22: row titles, the name under a pose tile. */
    val bodyStrong = body.copy(fontWeight = FontWeight.Medium)

    /** Medium 13/18: chips, captions, helper text, row subtitles. Buttons and tabs use labelLarge (14/20). */
    val label = Base.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)

    /**
     * Expanded Bold with tabular figures, for the match score, countdown and counts. Not monospace.
     * @param size 15.sp to 96.sp; must be in sp
     */
    fun numeral(size: TextUnit): TextStyle = Base.copy(
        fontFamily = ArchivoExpanded,
        fontWeight = FontWeight.Bold,
        fontSize = size,
        lineHeight = size * 1.1f,
        fontFeatureSettings = "tnum",
    )
}

/** M3 slots mapped onto the Studio roles. */
internal val StudioTypography = Typography(
    displayLarge = StudioType.display.copy(fontSize = 44.sp, lineHeight = 48.sp),
    displayMedium = StudioType.display.copy(fontSize = 36.sp, lineHeight = 40.sp),
    displaySmall = StudioType.display,
    headlineLarge = StudioType.display.copy(fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = StudioType.display,
    headlineSmall = StudioType.title,
    titleLarge = StudioType.title,
    titleMedium = StudioType.barTitle,
    titleSmall = StudioType.bodyStrong,
    bodyLarge = StudioType.body,
    bodyMedium = StudioType.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = StudioType.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = StudioType.label.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = StudioType.label,
    labelSmall = StudioType.label.copy(fontSize = 12.sp, lineHeight = 16.sp),
)
