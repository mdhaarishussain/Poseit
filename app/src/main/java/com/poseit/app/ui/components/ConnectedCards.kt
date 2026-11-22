package com.poseit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitRadius
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone
import kotlin.math.max
import kotlin.math.min

/*
 * The signature motif of v2 (docs/DESIGN.md, "Connected cards"): dark cards stacked a slot apart and fused into one
 * body, so the canvas only shows as a soft groove cut in from each side of every join. Each slot enters
 * through the flared corners of the two cards and ends in a round tip; between the two tips the cards are one piece.
 *
 *   ╭──────────────────────────╮
 *   │  top card                │
 *   ╰──╮                    ╭──╯   <- join corners (JoinRadius 20dp), then the slot runs NotchLength (24dp) in
 *  ════○                    ○════  <- slot: Gap (10dp) tall, round tip of radius Gap / 2
 *   ╭──╯                    ╰──╮
 *   │  next card               │
 *   ╰──────────────────────────╯
 *
 * Connect cards that belong to one subject (the top bar and the card it introduces, a pose's preview and its
 * facts, a stat pair). Cards about different things stand apart with a plain [Space.edge] gap. Outlined cream
 * cards never connect.
 */

/** Geometry of the connected-card motif. */
object ConnectedCardDefaults {
    /** Outer corners of the stack. */
    val Radius: Dp = PoseitRadius.card

    /** The corners on either side of a join, which flare into the slot. */
    val JoinRadius: Dp = 20.dp

    /**
     * Space between two joined cards: the slot's thickness. About 2.5% of a phone-width card, as in the mockups, so
     * a join reads as a soft liquid groove rather than a hairline crack.
     */
    val Gap: Dp = 10.dp

    /** How far the slot runs straight past the join corners before its round tip. */
    val NotchLength: Dp = 24.dp
}

/** Direction in which connected cards follow each other. */
enum class ConnectedAxis {
    /** Cards stacked top to bottom, slots cut in from the left and right edges. */
    Vertical,

    /** Cards side by side, slots cut in from the top and bottom edges. */
    Horizontal,
}

/**
 * The outline of connected cards as one [Path]: [segments] are the cards' rectangles (pixels) in order along
 * [axis], separated by gaps; along a vertical stack they must share left and right edges (along a horizontal row,
 * top and bottom edges). One segment gives a plain rounded rectangle.
 *
 * @param radiusPx outer corners; [joinRadiusPx] corners beside a join; [notchLengthPx] the straight run of each
 *   slot past those corners. The slot's tip radius is half the gap. When a card is too short or narrow for the
 *   geometry, the radii shrink to fit.
 */
fun connectedCardsPath(
    segments: List<Rect>,
    axis: ConnectedAxis = ConnectedAxis.Vertical,
    radiusPx: Float,
    joinRadiusPx: Float,
    notchLengthPx: Float,
): Path {
    if (segments.isEmpty()) return Path()
    if (axis == ConnectedAxis.Horizontal) {
        // Build the vertical version of the transposed layout, then transpose back (swap x and y).
        val transposed = segments.map { Rect(it.top, it.left, it.bottom, it.right) }
        val path = connectedCardsPath(transposed, ConnectedAxis.Vertical, radiusPx, joinRadiusPx, notchLengthPx)
        val swap = Matrix().apply {
            values[Matrix.ScaleX] = 0f
            values[Matrix.SkewX] = 1f
            values[Matrix.SkewY] = 1f
            values[Matrix.ScaleY] = 0f
        }
        path.transform(swap)
        return path
    }
    var result = Path()
    val last = segments.lastIndex
    segments.forEachIndexed { i, r ->
        val limit = min(r.width, r.height) / 2f
        val top = (if (i == 0) radiusPx else joinRadiusPx).coerceAtMost(limit)
        val bottom = (if (i == last) radiusPx else joinRadiusPx).coerceAtMost(limit)
        val card = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = r,
                    topLeft = CornerRadius(top),
                    topRight = CornerRadius(top),
                    bottomRight = CornerRadius(bottom),
                    bottomLeft = CornerRadius(bottom),
                ),
            )
        }
        result = if (i == 0) card else Path.combine(PathOperation.Union, result, card)
    }
    for (i in 0 until last) {
        val a = segments[i]
        val b = segments[i + 1]
        val gap = b.top - a.bottom
        if (gap <= 0f) continue
        val left = max(a.left, b.left)
        val right = min(a.right, b.right)
        val tip = gap / 2f
        val joinR = min(joinRadiusPx, min(min(a.width, a.height), min(b.width, b.height)) / 2f)
        // Inset of the slot's tip centre from each side; if the cards are too narrow, close the slot entirely.
        val inset = (joinR + notchLengthPx).coerceAtMost((right - left) / 2f - tip - 1f).coerceAtLeast(joinR)
        val eps = 1f // overlap into both cards so no hairline seam shows between the pieces
        val midY = a.bottom + tip
        val bridge = Path().apply {
            moveTo(left + inset, a.bottom - eps)
            lineTo(right - inset, a.bottom - eps)
            lineTo(right - inset, a.bottom)
            // Concave end: around the right slot's round tip, through its leftmost point.
            arcTo(Rect(Offset(right - inset, midY), tip), startAngleDegrees = -90f, sweepAngleDegrees = -180f, forceMoveTo = false)
            lineTo(right - inset, b.top + eps)
            lineTo(left + inset, b.top + eps)
            lineTo(left + inset, b.top)
            arcTo(Rect(Offset(left + inset, midY), tip), startAngleDegrees = 90f, sweepAngleDegrees = -180f, forceMoveTo = false)
            close()
        }
        result = Path.combine(PathOperation.Union, result, bridge)
    }
    return result
}

/**
 * A [Shape] of connected cards with known sizes, for clipping (e.g. a photo cut into the motif). Cards follow each
 * other along [axis] with [gap] between them; [segmentLengths] are the cards' heights (vertical) or widths
 * (horizontal), and the last card takes whatever space remains. For live content use [ConnectedCardStack], which
 * measures its children.
 */
class ConnectedCardShape(
    private val segmentLengths: List<Dp>,
    private val axis: ConnectedAxis = ConnectedAxis.Vertical,
    private val gap: Dp = ConnectedCardDefaults.Gap,
    private val radius: Dp = ConnectedCardDefaults.Radius,
    private val joinRadius: Dp = ConnectedCardDefaults.JoinRadius,
    private val notchLength: Dp = ConnectedCardDefaults.NotchLength,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val total = if (axis == ConnectedAxis.Vertical) size.height else size.width
        val gapPx = gap.toPx()
        val rects = mutableListOf<Rect>()
        var pos = 0f
        val lengths = segmentLengths.map { it.toPx() }
        for (i in 0..lengths.size) {
            val len = if (i < lengths.size) lengths[i] else total - pos
            if (len <= 0f) break
            rects += if (axis == ConnectedAxis.Vertical) Rect(0f, pos, size.width, pos + len) else Rect(pos, 0f, pos + len, size.height)
            pos += len + gapPx
            if (pos >= total) break
        }
        Outline.Generic(connectedCardsPath(rects, axis, radius.toPx(), joinRadius.toPx(), notchLength.toPx()))
    }
}

/**
 * Dark cards fused into one body (the connected-card motif). Every direct child is one card: give each its own
 * padding (or use [CardSegment], which pads by [Space.cardPadding] and can be clickable). Children are stacked
 * [gap] apart at the stack's full width and the stack draws one shape behind them.
 *
 * Also the way to join the top bar card to the card below: put a [TopBarContent] first.
 *
 * Content inside gets the dark-surface tone, so shared components pick their on-dark colours.
 *
 * @param color the cards' fill; keep it opaque
 */
@Composable
fun ConnectedCardStack(
    modifier: Modifier = Modifier,
    color: Color = PoseitColors.backgroundCard,
    gap: Dp = ConnectedCardDefaults.Gap,
    content: @Composable () -> Unit,
) = ConnectedCards(ConnectedAxis.Vertical, modifier, color, gap, content)

/**
 * A vertical [ConnectedCardStack] that fills the height it is given: every card in [cards] takes its own height
 * except the one at [fillIndex], which gets what is left (the onboarding picture, the editor's canvas, the photo
 * being traced, the picture of an empty screen). Each entry of [cards] is one card (it may emit several nodes; they
 * share the card's slot). Content gets the dark surface tone. In a vertically scrolling parent, give it
 * `Modifier.heightIn(min = ...)`: it fills that height and grows past it when the cards need more.
 *
 * @param minFillHeight the filling card never gets less than this; on a short screen with large text the stack then
 *   runs past the bottom rather than crushing the card to nothing
 */
@Composable
fun FillingCardStack(
    cards: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    fillIndex: Int = cards.lastIndex,
    minFillHeight: Dp = 0.dp,
    color: Color = PoseitColors.backgroundCard,
    gap: Dp = ConnectedCardDefaults.Gap,
) {
    var segments by remember { mutableStateOf(emptyList<Rect>()) }
    ProvideSurfaceTone(SurfaceTone.Dark) {
        Layout(
            contents = cards,
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
            if (slots.isEmpty()) return@Layout layout(constraints.minWidth, constraints.minHeight) {}
            val width = constraints.maxWidth
            val gapPx = gap.roundToPx()
            val loose = Constraints(minWidth = width, maxWidth = width)
            val fill = fillIndex.coerceIn(0, slots.lastIndex)
            val measured = slots.mapIndexed { i, nodes -> if (i == fill) emptyList() else nodes.map { it.measure(loose) } }
            val heights = IntArray(slots.size) { i -> measured[i].maxOfOrNull { it.height } ?: 0 }
            val used = heights.sum() + gapPx * (slots.size - 1)
            // In a scrolling column the height is unbounded: fill to the minimum height given instead (e.g.
            // `heightIn(min = viewport)`), and grow past it when the content needs more.
            val target = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
            heights[fill] = max(target - used, minFillHeight.roundToPx())
            val fillPlaceables = slots[fill].map { it.measure(Constraints.fixed(width, heights[fill])) }
            val total = used + heights[fill]
            val height = if (constraints.hasBoundedHeight) constraints.maxHeight else total
            layout(width, height) {
                var y = 0
                val rects = ArrayList<Rect>(slots.size)
                slots.indices.forEach { i ->
                    (if (i == fill) fillPlaceables else measured[i]).forEach { it.place(0, y) }
                    rects += Rect(0f, y.toFloat(), width.toFloat(), (y + heights[i]).toFloat())
                    y += heights[i] + gapPx
                }
                if (rects != segments) segments = rects
            }
        }
    }
}

/**
 * Dark cards side by side, fused with slots cut in from the top and bottom (a stat pair, two choices). Children
 * share the width equally and take the height of the tallest. Every direct child is one card.
 */
@Composable
fun ConnectedCardRow(
    modifier: Modifier = Modifier,
    color: Color = PoseitColors.backgroundCard,
    gap: Dp = ConnectedCardDefaults.Gap,
    content: @Composable () -> Unit,
) = ConnectedCards(ConnectedAxis.Horizontal, modifier, color, gap, content)

/**
 * One card inside a [ConnectedCardStack] or [ConnectedCardRow]: full width (or height), [contentPadding] inside, and
 * optionally clickable as a whole (Role.Button) with its ripple kept inside the card.
 */
@Composable
fun CardSegment(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Space.cardPadding),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(PoseitShapes.Card)
                        .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
private fun ConnectedCards(
    axis: ConnectedAxis,
    modifier: Modifier,
    color: Color,
    gap: Dp,
    content: @Composable () -> Unit,
) {
    // The children's rectangles, measured in layout and read when drawing the shape behind them.
    var segments by remember { mutableStateOf(emptyList<Rect>()) }
    Layout(
        content = { ProvideSurfaceTone(SurfaceTone.Dark, content) },
        modifier = modifier.drawWithCache {
            val path = connectedCardsPath(
                segments,
                axis,
                ConnectedCardDefaults.Radius.toPx(),
                ConnectedCardDefaults.JoinRadius.toPx(),
                ConnectedCardDefaults.NotchLength.toPx(),
            )
            onDrawBehind { drawPath(path, color) }
        },
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val n = measurables.size
        if (n == 0) return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        if (axis == ConnectedAxis.Vertical) {
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
            val placeables = measurables.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
            val height = placeables.sumOf { it.height } + gapPx * (n - 1)
            layout(width, height.coerceIn(constraints.minHeight, max(constraints.minHeight, constraints.maxHeight))) {
                var y = 0
                val rects = ArrayList<Rect>(n)
                placeables.forEach { p ->
                    p.place(0, y)
                    rects += Rect(0f, y.toFloat(), width.toFloat(), (y + p.height).toFloat())
                    y += p.height + gapPx
                }
                if (rects != segments) segments = rects
            }
        } else {
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
            val each = ((width - gapPx * (n - 1)) / n).coerceAtLeast(0)
            val tallest = measurables.maxOf { it.maxIntrinsicHeight(each) }.coerceAtLeast(constraints.minHeight)
            val placeables = measurables.map { it.measure(Constraints(minWidth = each, maxWidth = each, minHeight = tallest, maxHeight = tallest)) }
            layout(width, tallest) {
                var x = 0
                val rects = ArrayList<Rect>(n)
                placeables.forEach { p ->
                    p.place(x, 0)
                    rects += Rect(x.toFloat(), 0f, (x + p.width).toFloat(), p.height.toFloat())
                    x += p.width + gapPx
                }
                if (rects != segments) segments = rects
            }
        }
    }
}
