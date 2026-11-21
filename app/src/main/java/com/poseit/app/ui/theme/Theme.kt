package com.poseit.app.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration

/**
 * The Studio colours as a light M3 [ColorScheme], so stock components come out right without overrides.
 *
 * Studio has three surface tones, not M3's five-step container ladder: the containers that sheets (Low), menus
 * (Container) and dialogs (High) read are all [Studio.Sheet], so they stand off the [Studio.Paper] background;
 * Highest (filled cards, filled text fields) is [Studio.Set]. There is no elevation tint.
 *
 * Selected states read `secondaryContainer`, which is [Studio.Ink]. Two stock defaults therefore need Studio
 * colours passed explicitly, or their track disappears: Slider (use `studioSliderColors()`) and determinate
 * progress indicators (pass `trackColor = Studio.Hairline`).
 */
internal val StudioColorScheme: ColorScheme = lightColorScheme(
    primary = Studio.Ink,
    onPrimary = Studio.Sheet,
    primaryContainer = Studio.Ink,
    onPrimaryContainer = Studio.Sheet,
    inversePrimary = Studio.Mannequin,
    secondary = Studio.Ink,
    onSecondary = Studio.Sheet,
    secondaryContainer = Studio.Ink,
    onSecondaryContainer = Studio.Sheet,
    tertiary = Studio.Graphite,
    onTertiary = Studio.Sheet,
    tertiaryContainer = Studio.Set,
    onTertiaryContainer = Studio.Ink,
    background = Studio.Paper,
    onBackground = Studio.Ink,
    surface = Studio.Paper,
    onSurface = Studio.Ink,
    surfaceVariant = Studio.Set,
    onSurfaceVariant = Studio.Graphite,
    surfaceTint = Color.Transparent,
    inverseSurface = Studio.Ink,
    inverseOnSurface = Studio.Sheet,
    error = Studio.Miss,
    onError = Studio.Sheet,
    errorContainer = Studio.Miss.copy(alpha = 0.12f).compositeOver(Studio.Sheet),
    onErrorContainer = Studio.Miss.copy(alpha = 0.85f).compositeOver(Studio.Ink),
    outline = Studio.Hairline.compositeOver(Studio.Paper),
    outlineVariant = Studio.Ink.copy(alpha = 0.07f).compositeOver(Studio.Paper),
    scrim = Studio.Ink.copy(alpha = 0.32f),
    surfaceBright = Studio.Sheet,
    surfaceDim = Studio.Set,
    surfaceContainerLowest = Studio.Sheet,
    surfaceContainerLow = Studio.Sheet,
    surfaceContainer = Studio.Sheet,
    surfaceContainerHigh = Studio.Sheet,
    surfaceContainerHighest = Studio.Set,
)

/**
 * The app theme: Studio colours, Archivo type and Studio shapes. The app is always light.
 *
 * It also reports "not night" to everything below it, whatever the system setting, because M3's bottom-sheet
 * dialogs pick their system-bar icon colour from `isSystemInDarkTheme()`; with the phone in dark mode they would
 * otherwise put white icons on the light sheets.
 */
@Composable
fun PoseitTheme(content: @Composable () -> Unit) {
    ReportNightMode(night = false) {
        MaterialTheme(
            colorScheme = StudioColorScheme,
            typography = StudioTypography,
            shapes = StudioMaterialShapes,
            content = content,
        )
    }
}

/**
 * For content over the black camera surround: reports night mode below this point so M3 bottom sheets and dialogs
 * opened from the camera give their windows light (white) system-bar icons. Colours are unchanged; sheets keep the
 * light Studio colours.
 */
@Composable
fun LightSystemBarIcons(content: @Composable () -> Unit) = ReportNightMode(night = true, content = content)

@Composable
private fun ReportNightMode(night: Boolean, content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val reported = remember(config, night) {
        Configuration(config).apply {
            val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
        }
    }
    CompositionLocalProvider(LocalConfiguration provides reported, content = content)
}
