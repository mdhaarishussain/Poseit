package com.poseit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.SurfaceTone
import kotlin.math.roundToInt

/** Height of a [ValueSlider]'s bar track. */
private val SliderTrackHeight = 20.dp

/** Size of the slider's handle: a narrow upright tick standing in the gap at the end of the filled bars. */
private val TickWidth = 4.dp
private val TickHeight = 28.dp

/**
 * The slider (docs/DESIGN.md, "Sliders"): the mockups' segmented bar ("79% ||||||||") made draggable. A row of
 * narrow 20dp bars, filled up to the value, with a 4dp x 28dp tick standing at the end of the fill as the handle.
 * Inside a dark card (or with [onDark]) the fill is lavender on the dark track and the tick cream; on cream the fill
 * is ink, because lavender and the pale track are too close in lightness to tell apart there.
 *
 * With [steps], the bar count is a multiple of the intervals so every step lands on a whole bar. Same parameters and
 * semantics as M3's Slider (it is one underneath: drag, tap and the accessibility value actions are M3's); add
 * `semantics { stateDescription = ... }` in [modifier] for a readable value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onDark: Boolean = LocalSurfaceTone.current == SurfaceTone.Dark,
) {
    val fill = if (onDark) PoseitColors.accent else PoseitColors.textPrimary
    val empty = if (onDark) PoseitColors.trackInverted else PoseitColors.track
    val tick = if (onDark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val alpha = if (enabled) 1f else 0.38f
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = poseitSliderColors(),
        // The visible handle is drawn by the track, in the gap at the end of the fill; this keeps M3's layout (the
        // track spans the slider less the thumb's width) with an invisible thumb as wide as the tick.
        thumb = { Spacer(Modifier.size(TickWidth, TickHeight)) },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(SliderTrackHeight),
            ) {
                val gap = SegmentGap.toPx()
                val n = sliderBarCount(size.width, state.steps + 1, 11.dp.toPx(), 6.dp.toPx())
                val filled = (fraction * n).roundToInt()
                val w = (size.width - gap * (n - 1)) / n
                val r = CornerRadius(minOf(w, SegmentCorner.toPx()) / 2f)
                for (i in 0 until n) {
                    val c = if (i < filled) fill else empty
                    drawRoundRect(
                        c.copy(alpha = c.alpha * alpha),
                        Offset(i * (w + gap), 0f),
                        Size(w, size.height),
                        r,
                    )
                }
                // The tick stands centred in the gap after the last filled bar (at the very ends, on the edge).
                val tw = TickWidth.toPx()
                val th = TickHeight.toPx()
                val boundary = (filled * (w + gap) - gap / 2f).coerceIn(tw / 2f, size.width - tw / 2f)
                drawRoundRect(
                    tick.copy(alpha = tick.alpha * alpha),
                    Offset(boundary - tw / 2f, (size.height - th) / 2f),
                    Size(tw, th),
                    CornerRadius(tw / 2f),
                )
            }
        },
    )
}

/**
 * How many bars a slider of [width] px shows: about one per [targetPx], rounded to a multiple of [intervals] (the
 * slider's steps + 1) so each step boundary falls on a whole bar, and never thinner than [minPx].
 */
internal fun sliderBarCount(width: Float, intervals: Int, targetPx: Float, minPx: Float): Int {
    val free = (width / targetPx).roundToInt().coerceAtLeast(1)
    if (intervals <= 1) return free
    if (intervals * minPx > width) return free
    val perStep = (width / (targetPx * intervals)).roundToInt().coerceAtLeast(1)
    return intervals * perStep
}
