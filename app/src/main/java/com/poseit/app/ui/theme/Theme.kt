package com.poseit.app.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PoseitColors {
    val Violet = Color(0xFF7C5CFF)
    val VioletDark = Color(0xFF5A3FE0)
    val Coral = Color(0xFFFF7A59)
    val Mint = Color(0xFF3DDC97)
    val Amber = Color(0xFFFFC145)
    val Red = Color(0xFFFF5A5F)
    val Background = Color(0xFF121316)
    val Surface = Color(0xFF1B1D22)
    val SurfaceHigh = Color(0xFF262932)
    val OnSurfaceMuted = Color(0xFFA6A9B6)
    val FigureFill = Color(0xFFF4F4F6)
    val FigureOutline = Color(0xFF1B1D22)
    /** Tints for the 2nd/3rd person in multi-person poses. */
    val FigureTints = listOf(Color(0xFFF4F4F6), Color(0xFFFFC9A8), Color(0xFFA8D8FF))

    /** Red -> amber -> mint for a 0..1 match score. */
    fun forScore(score: Float): Color = when {
        score >= 0.8f -> Mint
        score >= 0.6f -> lerp(Amber, Mint, (score - 0.6f) / 0.2f)
        score >= 0.35f -> lerp(Red, Amber, (score - 0.35f) / 0.25f)
        else -> Red
    }

    private fun lerp(a: Color, b: Color, t: Float): Color {
        val k = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * k,
            green = a.green + (b.green - a.green) * k,
            blue = a.blue + (b.blue - a.blue) * k,
            alpha = a.alpha + (b.alpha - a.alpha) * k,
        )
    }
}

private val scheme = darkColorScheme(
    primary = PoseitColors.Violet,
    onPrimary = Color.White,
    primaryContainer = PoseitColors.VioletDark,
    onPrimaryContainer = Color.White,
    secondary = PoseitColors.Coral,
    onSecondary = Color.White,
    tertiary = PoseitColors.Mint,
    background = PoseitColors.Background,
    onBackground = Color(0xFFF2F2F5),
    surface = PoseitColors.Surface,
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = PoseitColors.SurfaceHigh,
    onSurfaceVariant = PoseitColors.OnSurfaceMuted,
    surfaceContainer = PoseitColors.Surface,
    surfaceContainerHigh = PoseitColors.SurfaceHigh,
    surfaceContainerHighest = Color(0xFF30333D),
    error = PoseitColors.Red,
    outline = Color(0xFF3A3D48),
)

private val typography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    )
}

@Composable
fun PoseitTheme(content: @Composable () -> Unit) {
    // The app is always dark. Report night mode to Compose too, so anything keyed on isSystemInDarkTheme() (the M3
    // bottom-sheet dialogs set their system-bar icon colours from it) matches the UI instead of the system setting.
    val config = LocalConfiguration.current
    val darkConfig = remember(config) {
        Configuration(config).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        }
    }
    CompositionLocalProvider(LocalConfiguration provides darkConfig) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
