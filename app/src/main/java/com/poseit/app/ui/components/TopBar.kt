package com.poseit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/** Insets a top bar pads itself by: the status bar and any side system bars (same as M3's top app bars). */
val TopBarInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

/** Height of the top bar card. */
val TopBarHeight: Dp = 64.dp

/**
 * The top bar card (docs/DESIGN.md, "Top bar card"): a 64dp dark card [Space.edge] below the status bar and from
 * the screen sides, with the title (or the "Poseit" wordmark) in cream, an optional lavender [Badge] and cream icon
 * actions on the right. With [onBack] a back arrow comes first.
 *
 * Pass [joined] to fuse the cards it introduces to its bottom edge with the connected-card necks (the home screen's
 * Today's pose, a pose's preview): every direct child of [joined] is one more card, as in a [ConnectedCardStack].
 *
 * @param title the screen's name in sentence case ("Your photos", "Settings"); a heading for accessibility
 * @param wordmark sets [title] in the wordmark style (only for "Poseit" on the home screen)
 * @param badge e.g. `{ Badge("99 poses") }`
 * @param actions [PlainIconButton]s (their glyphs land 24dp in from the card edge)
 * @param containerColor the band behind the card's margins; the canvas by default, transparent over a full-bleed view
 */
@Composable
fun TopBarCard(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    wordmark: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
    windowInsets: WindowInsets = TopBarInsets,
    containerColor: Color = PoseitColors.backgroundBody,
    actions: (@Composable RowScope.() -> Unit)? = null,
    joined: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(windowInsets)
            .padding(start = Space.edge, end = Space.edge, top = Space.edge),
    ) {
        if (joined == null) {
            ProvideSurfaceTone(SurfaceTone.Dark) {
                Box(Modifier.fillMaxWidth().background(PoseitColors.backgroundCard, PoseitShapes.Card)) {
                    TopBarContent(title, onBack = onBack, backContentDescription = backContentDescription, wordmark = wordmark, badge = badge, actions = actions)
                }
            }
        } else {
            ConnectedCardStack(Modifier.fillMaxWidth()) {
                TopBarContent(title, onBack = onBack, backContentDescription = backContentDescription, wordmark = wordmark, badge = badge, actions = actions)
                joined()
            }
        }
    }
}

/**
 * The inside of the top bar card, for placing it as the first card of your own [ConnectedCardStack]. It draws no
 * background; [TopBarCard] is the usual way in.
 */
@Composable
fun TopBarContent(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    wordmark: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            // The back arrow's glyph and the action glyphs land on the card padding (48dp targets, 24dp glyphs).
            .padding(
                start = if (onBack != null) Space.cardPadding - IconGlyphInset else Space.cardPadding,
                end = if (actions != null) Space.cardPadding - IconGlyphInset else Space.cardPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            PlainIconButton(PoseitIcons.back, backContentDescription, onBack, tint = PoseitColors.textPrimaryInverted)
            Spacer(Modifier.width(Space.xs))
        }
        Text(
            title,
            style = if (wordmark) PoseitType.wordmark else PoseitType.title,
            color = PoseitColors.textPrimaryInverted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(if (title.isNotEmpty()) Modifier.semantics { heading() } else Modifier),
        )
        if (badge != null) {
            Spacer(Modifier.width(Space.m))
            badge()
        }
        if (actions != null) {
            if (badge != null) Spacer(Modifier.width(Space.xs))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, content = actions)
        }
    }
}

/** v1 top bar, now drawn as a [TopBarCard]. */
@Deprecated("Use TopBarCard", ReplaceWith("TopBarCard(title, modifier, onBack, backContentDescription, windowInsets = windowInsets, actions = actions)"))
@Composable
fun StudioTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    containerColor: Color = PoseitColors.backgroundBody,
    windowInsets: WindowInsets = TopBarInsets,
    actions: @Composable RowScope.() -> Unit = {},
) = TopBarCard(title, modifier, onBack, backContentDescription, windowInsets = windowInsets, containerColor = containerColor, actions = actions)

/** v1 large top bar (the library's), now drawn as a [TopBarCard] with the wordmark. */
@Deprecated("Use TopBarCard(wordmark = true)", ReplaceWith("TopBarCard(title, modifier, onBack, backContentDescription, wordmark = true, windowInsets = windowInsets, actions = actions)"))
@Suppress("UNUSED_PARAMETER")
@Composable
fun StudioLargeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = PoseitType.wordmark,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    containerColor: Color = PoseitColors.backgroundBody,
    windowInsets: WindowInsets = TopBarInsets,
    actions: @Composable RowScope.() -> Unit = {},
) = TopBarCard(title, modifier, onBack, backContentDescription, wordmark = true, windowInsets = windowInsets, containerColor = containerColor, actions = actions)
