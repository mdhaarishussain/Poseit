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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.poseit.app.ui.components.ConnectedAxis
import com.poseit.app.ui.components.ConnectedCardDefaults
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.components.TopBarHeight
import com.poseit.app.ui.components.TopBarInsets
import com.poseit.app.ui.components.connectedCardsPath
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone
import kotlin.math.max

/*
 * Two screen frames built on the connected-card motif, shared by the pose detail, the pose editor and "pose from a
 * photo". The foundation's TopBarCard(joined = ...) is a fixed bar; these add what those screens need on top:
 * a fused stack that scrolls with the page (ScrollingStackScreen) and one that fills the screen with a flexible
 * middle card (FillingCardStack).
 */

/**
 * A page whose top bar card is fused to the first cards of its content ([joined]) and scrolls away with them, like
 * the mockups' stacks. Once the bar has scrolled out under the status bar, a plain [TopBarCard] titled
 * [pinnedTitle] fades in at the top, so Back is never more than one scroll away. Only one of the two bars is in
 * the semantics tree at a time.
 *
 * @param title the in-stack bar's title ("" when the first joined card carries the screen's name)
 * @param bottomBar pinned below the scrolling content on the canvas (e.g. [PinnedActions]); it pads the navigation
 *   bar itself
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
    bottomBar: @Composable () -> Unit = {},
    joined: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val barBottomPx = with(LocalDensity.current) { (Space.edge + TopBarHeight).toPx() }
    val pinned by remember(barBottomPx) { derivedStateOf { scrollState.value >= barBottomPx } }
    Scaffold(
        modifier = modifier,
        containerColor = PoseitColors.backgroundBody,
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
        bottomBar = bottomBar,
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
 * navigation bar and the keyboard. No rule above it: the page scrolls under the band.
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

/**
 * Up to three dark cards fused into one body that fills the height it is given: [top] and [bottom] take their own
 * height and [fill] gets the rest (the editor's canvas, the photo being traced). Content gets the dark surface tone.
 * Unlike ConnectedCardStack, which stacks children at their natural height, this one is for screens that don't
 * scroll.
 */
@Composable
internal fun FillingCardStack(
    top: @Composable () -> Unit,
    fill: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottom: (@Composable () -> Unit)? = null,
    color: Color = PoseitColors.backgroundCard,
    gap: Dp = ConnectedCardDefaults.Gap,
) {
    var segments by remember { mutableStateOf(emptyList<Rect>()) }
    val contents = buildList<@Composable () -> Unit> {
        add(top)
        add(fill)
        if (bottom != null) add(bottom)
    }
    ProvideSurfaceTone(SurfaceTone.Dark) {
        Layout(
            contents = contents,
            modifier = modifier.drawWithCache {
                val path = connectedCardsPath(
                    segments,
                    ConnectedAxis.Vertical,
                    ConnectedCardDefaults.Radius.toPx(),
                    ConnectedCardDefaults.JoinRadius.toPx(),
                    ConnectedCardDefaults.NotchLength.toPx(),
                )
                onDrawBehind { drawPath(path, color) }
            },
        ) { slots, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val gapPx = gap.roundToPx()
            val loose = Constraints(minWidth = width, maxWidth = width)
            val topPlaceables = slots[0].map { it.measure(loose) }
            val bottomPlaceables = slots.getOrNull(2)?.map { it.measure(loose) }.orEmpty()
            val topH = topPlaceables.maxOfOrNull { it.height } ?: 0
            val bottomH = bottomPlaceables.maxOfOrNull { it.height } ?: 0
            val gaps = gapPx * (if (bottomPlaceables.isEmpty()) 1 else 2)
            val fillH = max(0, height - topH - bottomH - gaps)
            val fillPlaceables = slots[1].map { it.measure(Constraints.fixed(width, fillH)) }
            layout(width, height) {
                topPlaceables.forEach { it.place(0, 0) }
                val fillY = topH + gapPx
                fillPlaceables.forEach { it.place(0, fillY) }
                val rects = mutableListOf(
                    Rect(0f, 0f, width.toFloat(), topH.toFloat()),
                    Rect(0f, fillY.toFloat(), width.toFloat(), (fillY + fillH).toFloat()),
                )
                if (bottomPlaceables.isNotEmpty()) {
                    val bottomY = fillY + fillH + gapPx
                    bottomPlaceables.forEach { it.place(0, bottomY) }
                    rects += Rect(0f, bottomY.toFloat(), width.toFloat(), (bottomY + bottomH).toFloat())
                }
                if (rects != segments) segments = rects
            }
        }
    }
}
