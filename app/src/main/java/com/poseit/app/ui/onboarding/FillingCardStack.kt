package com.poseit.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.components.ConnectedAxis
import com.poseit.app.ui.components.ConnectedCardDefaults
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.components.connectedCardsPath
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.SurfaceTone

/**
 * A vertical connected-card stack (the shared `ConnectedCardStack` look: dark cards fused by concave necks) whose
 * LAST card takes all the height left over, so the stack fills a page. The shared stack sizes every card to its
 * content, which can't make a picture card grow to the bottom of the screen.
 *
 * Every direct child is one card, [gap] apart; the cards before the last are measured at their own height, the
 * last gets the remaining height (at least [minLastHeight], so a very large font pushes the stack past the
 * bottom rather than crushing the picture). Content gets the dark surface tone.
 */
@Composable
internal fun FillingCardStack(
    modifier: Modifier = Modifier,
    gap: Dp = ConnectedCardDefaults.Gap,
    minLastHeight: Dp = 120.dp,
    content: @Composable () -> Unit,
) {
    var segments by remember { mutableStateOf(emptyList<Rect>()) }
    val color = PoseitColors.backgroundCard
    Layout(
        content = { ProvideSurfaceTone(SurfaceTone.Dark, content) },
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
    ) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val heads = measurables.dropLast(1).map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
        val used = heads.sumOf { it.height } + gapPx * heads.size
        val available = if (constraints.hasBoundedHeight) constraints.maxHeight - used else 0
        val lastHeight = available.coerceAtLeast(minLastHeight.roundToPx())
        val last = measurables.last().measure(Constraints.fixed(width, lastHeight))
        val placeables = heads + last
        val height = (used + lastHeight).coerceAtMost(if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE)
        layout(width, height) {
            var y = 0
            val rects = ArrayList<Rect>(placeables.size)
            placeables.forEach { p ->
                p.place(0, y)
                rects += Rect(0f, y.toFloat(), width.toFloat(), (y + p.height).toFloat())
                y += p.height + gapPx
            }
            if (rects != segments) segments = rects
        }
    }
}
