package com.poseit.app.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.components.TopBarHeight
import com.poseit.app.ui.components.TopBarInsets
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.Space

/*
 * Screen frames built on the connected-card motif, shared by the pose detail and "pose from a photo". The
 * foundation's TopBarCard(joined = ...) is a fixed bar; ScrollingStackScreen is a fused stack that scrolls with the
 * page, with PinnedActions at its foot. (A stack that fills a screen that doesn't scroll is the shared
 * FillingCardStack.)
 */

/**
 * A page whose top bar card is fused to the first cards of its content ([joined]) and scrolls away with them, like
 * the mockups' stacks. Once the bar has scrolled out under the status bar, a plain [TopBarCard] titled
 * [pinnedTitle] fades in at the top, so Back is never more than one scroll away. Only one of the two bars is in
 * the semantics tree at a time.
 *
 * @param title the in-stack bar's title ("" when the first joined card carries the screen's name)
 * @param bottomBar pinned below the scrolling content on the canvas (e.g. [PinnedActions]); it pads the navigation
 *   bar itself. A hairline marks its top edge while there is more page under it, so a card cut off by the band
 *   reads as "scroll for more" rather than as a clipping bug.
 * @param content the rest of the page under the stack, on the canvas
 */
@Composable
internal fun ScrollingStackScreen(
    pinnedTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    badge: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    joined: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val barBottomPx = with(LocalDensity.current) { (Space.edge + TopBarHeight).toPx() }
    val pinned by remember(barBottomPx) { derivedStateOf { scrollState.value >= barBottomPx } }
    Scaffold(
        modifier = modifier,
        containerColor = PoseitColors.backgroundBody,
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
        bottomBar = {
            if (bottomBar != null) {
                Column {
                    // Always laid out (so the page's height doesn't change when it appears), drawn only while the
                    // page can scroll on.
                    HairlineDivider(Modifier.alpha(if (scrollState.canScrollForward) 1f else 0f))
                    bottomBar()
                }
            }
        },
        snackbarHost = { if (snackbarHostState != null) PoseitSnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Box(
                    Modifier
                        .windowInsetsPadding(TopBarInsets)
                        .padding(start = Space.edge, end = Space.edge, top = Space.edge),
                ) {
                    ConnectedCardStack(Modifier.fillMaxWidth()) {
                        TopBarContent(
                            title = title,
                            onBack = onBack,
                            badge = badge,
                            actions = actions,
                            // While the pinned bar is showing, this one is off screen: keep a single Back in the tree.
                            modifier = if (pinned) Modifier.clearAndSetSemantics {} else Modifier,
                        )
                        joined()
                    }
                }
                content()
            }
            // The canvas behind the status bar, so the stack scrolls under it rather than under the clock.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(PoseitColors.backgroundBody),
            )
            AnimatedVisibility(
                visible = pinned,
                enter = fadeIn(PoseitMotion.standard(PoseitMotion.FAST)),
                exit = fadeOut(PoseitMotion.standard(PoseitMotion.FAST)),
            ) {
                TopBarCard(title = pinnedTitle, onBack = onBack, actions = actions)
            }
        }
    }
}

/**
 * The band at the foot of a [ScrollingStackScreen]: the canvas, [Space.edge] around its content, above the
 * navigation bar and the keyboard. The screen draws a hairline above it while the page scrolls on under it.
 */
@Composable
internal fun PinnedActions(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(PoseitColors.backgroundBody)
            .navigationBarsPadding()
            .imePadding()
            .padding(Space.edge),
    ) { content() }
}
