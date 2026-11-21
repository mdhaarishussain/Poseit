package com.poseit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.SurfaceTone

/**
 * The slider (docs/DESIGN.md, "Sliders"): a 6dp rounded track, dark up to the thumb and the surface's track colour
 * after it, with a 22dp lavender thumb ringed in the dark ink. On a dark surface (inside a dark card, over the
 * camera, or with [onDark]) the filled track is cream. Steps show as small dots. Same parameters and semantics as
 * M3's Slider; add `semantics { stateDescription = ... }` in [modifier] for a readable value.
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
    val base = if (onDark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val active = if (enabled) base else base.copy(alpha = 0.38f)
    val inactive = if (onDark) PoseitColors.textPrimaryInverted.copy(alpha = 0.22f) else PoseitColors.track
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = poseitSliderColors(),
        thumb = {
            Box(
                Modifier
                    .size(22.dp)
                    .background(if (enabled) PoseitColors.accent else PoseitColors.backgroundMuted, PoseitShapes.Circle)
                    .border(1.5.dp, if (onDark) PoseitColors.backgroundCard else PoseitColors.textPrimary, PoseitShapes.Circle),
            )
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            ) {
                val h = size.height
                val radius = CornerRadius(h / 2f)
                val x = size.width * fraction
                fun segment(from: Float, to: Float, color: Color) {
                    if (to - from > 0.5f) drawRoundRect(color, Offset(from, 0f), Size(to - from, h), radius)
                }
                segment(0f, size.width, inactive)
                segment(0f, x, active)
                if (state.steps > 0) {
                    val dot = 1.25.dp.toPx()
                    for (i in 0..state.steps + 1) {
                        val tx = size.width * i / (state.steps + 1)
                        val tick = if (tx <= x) (if (onDark) PoseitColors.backgroundCard else PoseitColors.textPrimaryInverted) else active.copy(alpha = 0.38f)
                        drawCircle(tick, dot, Offset(tx.coerceIn(h / 2f, size.width - h / 2f), h / 2f))
                    }
                }
            }
        },
    )
}

/** v1 name of [ValueSlider]. */
@Deprecated("Use ValueSlider", ReplaceWith("ValueSlider(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished, onDark)"))
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
) = ValueSlider(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished, onDark)
