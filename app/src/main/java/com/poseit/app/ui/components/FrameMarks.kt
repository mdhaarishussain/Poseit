package com.poseit.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors

/**
 * Viewfinder frame marks (docs/DESIGN.md, "Frame marks"): thin corner brackets at the four corners of this element,
 * drawn over its content. They show the framing of a pose's photo, like the frame lines in a viewfinder. Used on the
 * detail preview and the editor canvas, which are dark stages in v2, so they default to cream; pass
 * [PoseitColors.textPrimary] on cream.
 *
 * Apply it to the element whose bounds ARE the photo frame (e.g. a Box with `aspectRatio(template.frameAspect)`).
 * When the frame is only part of a canvas, call [drawFrameMarks] with the frame rect (e.g. from `fitFrameRect`).
 *
 * @param inset moves the marks inside the bounds, e.g. to keep them clear of a rounded clip
 */
fun Modifier.frameMarks(
    color: Color = PoseitColors.textPrimaryInverted,
    stroke: Dp = 1.5.dp,
    length: Dp = 12.dp,
    inset: Dp = 0.dp,
): Modifier = drawWithContent {
    drawContent()
    val i = inset.toPx()
    drawFrameMarks(Rect(i, i, size.width - i, size.height - i), color, stroke.toPx(), length.toPx())
}

/**
 * Draws the four corner brackets of [frame] (pixels). The strokes lie inside [frame], so its edges are the outer
 * edges of the marks.
 */
fun DrawScope.drawFrameMarks(
    frame: Rect,
    color: Color = PoseitColors.textPrimaryInverted,
    strokePx: Float = 1.5.dp.toPx(),
    lengthPx: Float = 12.dp.toPx(),
) {
    if (frame.width <= 0f || frame.height <= 0f) return
    val h = strokePx / 2f
    val r = frame.deflate(h)
    val len = lengthPx.coerceAtMost(minOf(r.width, r.height) / 2f)
    val path = Path().apply {
        fun corner(corner: Offset, dx: Float, dy: Float) {
            moveTo(corner.x, corner.y + dy * len)
            lineTo(corner.x, corner.y)
            lineTo(corner.x + dx * len, corner.y)
        }
        corner(r.topLeft, 1f, 1f)
        corner(r.topRight, -1f, 1f)
        corner(r.bottomRight, -1f, -1f)
        corner(r.bottomLeft, 1f, -1f)
    }
    drawPath(path, color, style = Stroke(width = strokePx, cap = StrokeCap.Square, join = StrokeJoin.Miter))
}
