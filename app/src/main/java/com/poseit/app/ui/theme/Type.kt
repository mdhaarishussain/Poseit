package com.poseit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.poseit.app.R

/**
 * Hanken Grotesk (OFL, bundled in res/font as static Latin subsets): Light, Regular, Medium, SemiBold. A friendly
 * neo-grotesk with a real Light weight for big numbers and headlines, and tabular figures by default.
 */
val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk_light, FontWeight.Light),
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
)

private val Base = TextStyle(
    fontFamily = HankenGrotesk,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/**
 * The v2 type roles (docs/DESIGN.md, "Type"), named after Astryx's text roles (display, heading, body, label,
 * supporting) plus Poseit's numerals and the tiny uppercase [caps] label.
 *
 * The voice comes from weight and tracking, as in the mockups: big text is LIGHT or Regular and tracked tight;
 * small controls are SemiBold capitals tracked open. Sentence case everywhere except [caps], which only pill
 * buttons, badges and segmented options use (use the CapsLabel component, which keeps the sentence-case words for
 * screen readers). Headlines balance their lines ([LineBreak.Heading]).
 */
object PoseitType {
    /** Light 34/38, -0.8sp: onboarding and explainer headlines ("Pick a pose to copy."), the pose name on detail. */
    val display = Base.copy(
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
        lineBreak = LineBreak.Heading,
    )

    /** Regular 26/32, -0.5sp: the headline of a card ("Jacket over shoulder" on Today's pose), large page titles. */
    val headline = Base.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        lineBreak = LineBreak.Heading,
    )

    /** Regular 20/26, -0.3sp: screen titles in the top bar card, sheet and dialog titles, card titles. */
    val title = Base.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        lineBreak = LineBreak.Heading,
    )

    /** Regular 22/28, -0.6sp: the "Poseit" wordmark in the top bar card. */
    val wordmark = Base.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.6).sp,
    )

    /** Medium 16/22, -0.1sp: section and group headings on the canvas or in a sheet, empty-state titles. */
    val heading = Base.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
        lineBreak = LineBreak.Heading,
    )

    /** Regular 17/24: a card's label ("Today's pose", "Best match"), row titles in outlined rows. */
    val bodyLarge = Base.copy(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp)

    /** Regular 15/22: paragraphs, descriptions, tips. */
    val body = Base.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)

    /** Medium 15/22: setting row titles, the pose name on a tile. */
    val bodyStrong = body.copy(fontWeight = FontWeight.Medium)

    /** Regular 13/18: secondary lines ("Standing, easy"), helper text, row subtitles, captions. */
    val supporting = Base.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)

    /** Medium 14/20: sentence-case controls (text buttons, menu items, filter pills, tabs). */
    val label = Base.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)

    /**
     * SemiBold 12/16 capitals, +0.8sp: pill buttons ("TRY THIS POSE"), badges ("99 POSES"), segmented options.
     * Never type the capitals into strings: pass sentence case to CapsLabel, which uppercases for display only.
     */
    val caps = Base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp)

    /** Light 72/72 numerals: the capture countdown, a timer. */
    val numeralDisplay = numeral(72.sp)

    /** Light 44/48 numerals: a headline number ("87%" match). */
    val numeralLarge = numeral(44.sp)

    /** Regular 26/30 numerals: a stat value in a StatPair ("12", "3 s"). */
    val numeralMedium = numeral(26.sp, FontWeight.Normal)

    /**
     * Numerals: tabular figures (Hanken Grotesk's default; `tnum` is set for any fallback font), tracked 2% tight.
     * @param size in sp; 15.sp to 120.sp
     * @param weight Light for 28sp and up, Regular below
     */
    fun numeral(size: TextUnit, weight: FontWeight = if (size.value >= 28f) FontWeight.Light else FontWeight.Normal): TextStyle = Base.copy(
        fontWeight = weight,
        fontSize = size,
        lineHeight = size * 1.08f,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    )
}

/** M3 slots mapped onto the v2 roles, so stock components (text fields, dialogs, menus) pick them up. */
internal val PoseitTypography = Typography(
    displayLarge = PoseitType.numeralLarge,
    displayMedium = PoseitType.display.copy(fontSize = 40.sp, lineHeight = 44.sp),
    displaySmall = PoseitType.display,
    headlineLarge = PoseitType.display.copy(fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = PoseitType.headline,
    headlineSmall = PoseitType.title.copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = PoseitType.title,
    titleMedium = PoseitType.heading,
    titleSmall = PoseitType.bodyStrong,
    bodyLarge = PoseitType.body,
    bodyMedium = PoseitType.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = PoseitType.supporting,
    labelLarge = PoseitType.label,
    labelMedium = PoseitType.supporting.copy(fontWeight = FontWeight.Medium),
    labelSmall = PoseitType.supporting.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
