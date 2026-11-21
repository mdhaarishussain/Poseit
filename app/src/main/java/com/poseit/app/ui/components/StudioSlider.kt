package com.poseit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.Studio

/**
 * The Studio slider (docs/DESIGN.md): a thin 4dp track, Ink up to the thumb and Hairline after it, and a slim Ink
 * index-mark thumb, like the scale on a lens ring. Steps show as small dots on the track. Same parameters and
 * semantics as M3's Slider; add `semantics { stateDescription = ... }` in [modifier] for a readable value.
 *
 * Use it on light surfaces (Paper, Sheet). On the dark camera, pass [onDark] for a Mannequin thumb and track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onDark: Boolean = false,
) {
    val base = if (onDark) Studio.Mannequin else Studio.Ink
    val active = if (enabled) base else base.copy(alpha = 0.38f)
    val inactive = if (onDark) Studio.Mannequin.copy(alpha = 0.24f) else Studio.Hairline
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = studioSliderColors(),
        thumb = {
            // A square-ended index mark, like the frame marks.
            Box(Modifier.size(width = 4.dp, height = 24.dp).background(active))
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            ) {
                val h = size.height
                val radius = CornerRadius(h / 2f)
                val x = size.width * fraction
                val gap = 4.dp.toPx()
                fun segment(from: Float, to: Float, color: Color) {
                    if (to - from > 0.5f) drawRoundRect(color, Offset(from, 0f), Size(to - from, h), radius)
                }
                segment(0f, x - gap, active)
                segment(x + gap, size.width, inactive)
                if (state.steps > 0) {
                    val dot = 1.dp.toPx()
                    for (i in 0..state.steps + 1) {
                        val tx = size.width * i / (state.steps + 1)
                        if (kotlin.math.abs(tx - x) < gap + dot) continue
                        val tick = if (tx < x) (if (onDark) Studio.LiveView else Studio.Sheet) else active.copy(alpha = 0.38f)
                        drawCircle(tick, dot, Offset(tx.coerceIn(h / 2f, size.width - h / 2f), h / 2f))
                    }
                }
            }
        },
    )
}
