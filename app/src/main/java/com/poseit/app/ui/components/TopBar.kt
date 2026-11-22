package com.poseit.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
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
 * @param hideBarSemantics removes the bar's own content (Back, title, actions) from the semantics tree, for an
 *   in-list bar while a [PinnedTopBar] stands in for it, so there is only ever one Back
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
    hideBarSemantics: Boolean = false,
    joined: (@Composable () -> Unit)? = null,
) {
    val barModifier = if (hideBarSemantics) Modifier.clearAndSetSemantics {} else Modifier
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
                    TopBarContent(title, barModifier, onBack, backContentDescription, wordmark, badge, actions)
                }
            }
        } else {
            ConnectedCardStack(Modifier.fillMaxWidth()) {
                TopBarContent(title, barModifier, onBack, backContentDescription, wordmark, badge, actions)
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

/**
 * The stand-in for a top bar card that scrolls away with its list (Settings, the photos grid, the home screen): once
 * the in-list bar has gone under the status bar ([visible], from [rememberTopBarScrolledAway]), a plain
 * [TopBarCard] fades in at the top, so Back and the bar's actions are never more than one scroll away. Hide the
 * in-list bar's semantics while it shows (TopBarCard's `hideBarSemantics`, or `clearAndSetSemantics {}` on a
 * [TopBarContent]). Place it over the list, at the top of the screen.
 */
@Composable
fun PinnedTopBar(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    wordmark: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
    windowInsets: WindowInsets = TopBarInsets,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(PoseitMotion.standard(PoseitMotion.FAST)),
        exit = fadeOut(PoseitMotion.standard(PoseitMotion.FAST)),
    ) {
        TopBarCard(title = title, onBack = onBack, wordmark = wordmark, badge = badge, windowInsets = windowInsets, actions = actions)
    }
}

/**
 * Whether a list's first item, a top bar card [Space.edge] below the list's top padding, has scrolled out of sight
 * (its bottom edge past the list's top).
 */
@Composable
fun rememberTopBarScrolledAway(state: LazyListState): Boolean {
    val barBottomPx = with(LocalDensity.current) { (Space.edge + TopBarHeight).toPx() }
    val away by remember(state, barBottomPx) {
        derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset >= barBottomPx }
    }
    return away
}

/** [rememberTopBarScrolledAway] for a lazy grid whose first item holds the top bar card. */
@Composable
fun rememberTopBarScrolledAway(state: LazyGridState): Boolean {
    val barBottomPx = with(LocalDensity.current) { (Space.edge + TopBarHeight).toPx() }
    val away by remember(state, barBottomPx) {
        derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset >= barBottomPx }
    }
    return away
}
