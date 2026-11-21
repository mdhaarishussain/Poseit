package com.poseit.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.view.Window
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The v2 roles as a light M3 [ColorScheme], so stock components come out right without overrides.
 *
 * - primary is the ink (dark pill buttons, focused fields, progress); primaryContainer and secondaryContainer are
 *   the lavender accent with ink content (M3's selected states).
 * - Every container a sheet, menu or dialog reads is [PoseitColors.backgroundSurface] (cream raised); Highest
 *   (filled fields) is [PoseitColors.backgroundMuted]. There is no elevation tint.
 * - inverseSurface is the dark card, so M3 snackbars and tooltips come out as dark cards.
 */
internal val PoseitColorScheme: ColorScheme = lightColorScheme(
    primary = PoseitColors.textPrimary,
    onPrimary = PoseitColors.textPrimaryInverted,
    primaryContainer = PoseitColors.accent,
    onPrimaryContainer = PoseitColors.onAccent,
    inversePrimary = PoseitColors.accent,
    secondary = PoseitColors.textPrimary,
    onSecondary = PoseitColors.textPrimaryInverted,
    secondaryContainer = PoseitColors.accent,
    onSecondaryContainer = PoseitColors.onAccent,
    tertiary = PoseitColors.textSecondary,
    onTertiary = PoseitColors.textPrimaryInverted,
    tertiaryContainer = PoseitColors.accentMuted,
    onTertiaryContainer = PoseitColors.textPrimary,
    background = PoseitColors.backgroundBody,
    onBackground = PoseitColors.textPrimary,
    surface = PoseitColors.backgroundBody,
    onSurface = PoseitColors.textPrimary,
    surfaceVariant = PoseitColors.backgroundMuted,
    onSurfaceVariant = PoseitColors.textSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = PoseitColors.backgroundCard,
    inverseOnSurface = PoseitColors.textPrimaryInverted,
    error = PoseitColors.signalMiss,
    onError = PoseitColors.onSignal,
    errorContainer = PoseitColors.signalMiss.copy(alpha = 0.12f).compositeOver(PoseitColors.backgroundSurface),
    onErrorContainer = PoseitColors.signalMiss,
    outline = PoseitColors.borderEmphasized,
    outlineVariant = PoseitColors.border.compositeOver(PoseitColors.backgroundBody),
    scrim = PoseitColors.overlay,
    surfaceBright = PoseitColors.backgroundSurface,
    surfaceDim = PoseitColors.backgroundMuted,
    surfaceContainerLowest = PoseitColors.backgroundSurface,
    surfaceContainerLow = PoseitColors.backgroundSurface,
    surfaceContainer = PoseitColors.backgroundSurface,
    surfaceContainerHigh = PoseitColors.backgroundSurface,
    surfaceContainerHighest = PoseitColors.backgroundMuted,
)

/**
 * The app theme (Poseit design system v2): mockup palette on Astryx-style roles, Hanken Grotesk, v2 shapes. The app
 * is always light; dark cards and the camera are surfaces inside it, not a dark mode.
 *
 * It also reports "not night" to everything below it, whatever the system setting, because M3's bottom-sheet
 * dialogs pick their system-bar icon colour from `isSystemInDarkTheme()`; with the phone in dark mode they would
 * otherwise put white icons on the cream sheets.
 */
@Composable
fun PoseitTheme(content: @Composable () -> Unit) {
    // The outermost theme owns the window's system-bar icon colour; nested themes (dialogs, sheets) share its counter.
    val outer = LocalDarkScreens.current
    val darkScreens = outer ?: remember { DarkScreenCounter() }
    if (outer == null) SystemBarIconsOwner(darkScreens)
    ReportNightMode(night = false) {
        MaterialTheme(
            colorScheme = PoseitColorScheme,
            typography = PoseitTypography,
            shapes = PoseitMaterialShapes,
        ) {
            CompositionLocalProvider(
                LocalDarkScreens provides darkScreens,
                LocalSurfaceTone provides SurfaceTone.Light,
                LocalContentColor provides PoseitColors.textPrimary,
                content = content,
            )
        }
    }
}

/**
 * For content over the black camera surround: reports night mode below this point so M3 bottom sheets and dialogs
 * opened from the camera give their windows light (white) system-bar icons. Colours are unchanged; sheets keep the
 * cream surface colours.
 */
@Composable
fun LightSystemBarIcons(content: @Composable () -> Unit) = ReportNightMode(night = true, content = content)

/**
 * While in composition, gives the activity window light (white) status- and navigation-bar icons, for a screen whose
 * top and bottom edges are dark (the camera, the photo viewer). Cream screens need nothing: the app's default is dark
 * icons.
 *
 * Screens don't save and restore the window's appearance themselves: during a cross-fade two of them are composed at
 * once and would restore in the wrong order. Each one only counts itself in and out, and the outermost [PoseitTheme]
 * is the single owner that sets light icons while the count is above zero.
 */
@Composable
fun LightSystemBarIconsEffect() {
    val counter = LocalDarkScreens.current ?: return
    DisposableEffect(counter) {
        counter.count++
        onDispose { counter.count-- }
    }
}

/** How many screens that want light system-bar icons are in composition ([LightSystemBarIconsEffect]). */
@Stable
internal class DarkScreenCounter {
    var count by mutableIntStateOf(0)
}

private val LocalDarkScreens = staticCompositionLocalOf<DarkScreenCounter?> { null }

/** Sets the window's bar icons from [counter]: light while any dark screen is composed, the app's dark icons otherwise. */
@Composable
private fun SystemBarIconsOwner(counter: DarkScreenCounter) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivityWindow() }
    // API 26-28 draw the app's own translucent canvas-coloured nav-bar scrim (API 29+ use the system's contrast
    // scrim, which follows the icon colour): behind white icons it is swapped for a dark one.
    val defaultNavColor = remember(window) { if (Build.VERSION.SDK_INT in 26..28) window?.navigationBarColorCompat else null }
    val dark = counter.count > 0
    DisposableEffect(window, dark) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !dark
        controller?.isAppearanceLightNavigationBars = !dark
        if (defaultNavColor != null) {
            window?.navigationBarColorCompat = if (dark) PoseitColors.backgroundCamera.copy(alpha = 0.5f).toArgb() else defaultNavColor
        }
        onDispose {}
    }
}

private tailrec fun Context.findActivityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findActivityWindow()
    else -> null
}

/** Window.navigationBarColor, which is deprecated (and ignored) on API 35+; only used on API 26-28 here. */
@Suppress("DEPRECATION")
private var Window.navigationBarColorCompat: Int
    get() = navigationBarColor
    set(value) {
        navigationBarColor = value
    }

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
