package com.poseit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType

/** Insets a top bar pads itself by: the status bar and any side system bars (same as M3's top app bars). */
val TopBarInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

/**
 * A screen's top bar: 56dp, Paper background with no elevation or scroll tint, an outlined back arrow, the title in
 * the bar-title style (Archivo Expanded Medium 18/24) and an actions slot. Without a back arrow the title starts on
 * the 20dp gutter, in line with the screen content. Put [StudioIconButton]s in [actions].
 *
 * @param title the screen's name in sentence case ("Your photos", "Settings"); may be empty
 * @param onBack shows the back arrow (content description [backContentDescription]) when not null
 * @param containerColor Paper by default; pass Color.Transparent over a full-bleed preview
 */
@Composable
fun StudioTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    containerColor: Color = Studio.Paper,
    windowInsets: WindowInsets = TopBarInsets,
    actions: @Composable RowScope.() -> Unit = {},
) = TopBarLayout(title, StudioType.barTitle, 56, modifier, onBack, backContentDescription, containerColor, windowInsets, actions)

/**
 * The large-title variant for a top-level screen (the library): 64dp, the title in the wordmark style (Archivo
 * Expanded SemiBold 22/28) on the gutter, actions on the right. Pass [StudioType.display] as [titleStyle] for a
 * bigger page title.
 */
@Composable
fun StudioLargeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = StudioType.wordmark,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    containerColor: Color = Studio.Paper,
    windowInsets: WindowInsets = TopBarInsets,
    actions: @Composable RowScope.() -> Unit = {},
) = TopBarLayout(title, titleStyle, 64, modifier, onBack, backContentDescription, containerColor, windowInsets, actions)

@Composable
private fun TopBarLayout(
    title: String,
    titleStyle: TextStyle,
    heightDp: Int,
    modifier: Modifier,
    onBack: (() -> Unit)?,
    backContentDescription: String,
    containerColor: Color,
    windowInsets: WindowInsets,
    actions: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides Studio.Ink) {
        Row(
            modifier
                .fillMaxWidth()
                .background(containerColor)
                .windowInsetsPadding(windowInsets)
                .height(heightDp.dp)
                // The back arrow's glyph and the action glyphs land on the 20dp gutter (48dp target, 22dp icon).
                .padding(start = if (onBack != null) Space.gutter - IconGlyphInset else Space.gutter, end = Space.gutter - IconGlyphInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                StudioIconButton(Icons.AutoMirrored.Outlined.ArrowBack, backContentDescription, onBack)
                Spacer(Modifier.width(Space.xs))
            }
            Text(
                title,
                style = titleStyle,
                color = Studio.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .then(if (title.isNotEmpty()) Modifier.semantics { heading() } else Modifier),
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
