package com.poseit.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.toneColors
import kotlin.math.roundToInt

/**
 * The segmented progress bar (docs/DESIGN.md, "Progress"; Astryx ProgressBar): a row of narrow rounded bars, the
 * done ones lavender and the rest on the track colour of the surface (the mockups' "79%" bar). For a count of
 * steps or a 0..1 amount. Reports a progress range to accessibility services.
 *
 * @param progress 0..1; segments fill whole, rounding to the nearest
 * @param segments how many bars; about one per 12dp of width reads best
 * @param contentDescription what is progressing ("Photos analysed"); the value is announced from [progress]
 */
@Composable
fun SegmentedProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 20,
    height: Dp = 28.dp,
    contentDescription: String? = null,
    filledColor: Color = PoseitColors.accent,
    emptyColor: Color = toneColors().track,
) {
    val p = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(p, PoseitMotion.standard(PoseitMotion.SLOW), label = "segmentedProgress")
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f)
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        val n = segments.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val filled = (animated * n).roundToInt()
        val r = CornerRadius(minOf(w, 4.dp.toPx()) / 2f)
        for (i in 0 until n) {
            drawRoundRect(if (i < filled) filledColor else emptyColor, Offset(i * (w + gap), 0f), Size(w, size.height), r)
        }
    }
}

/**
 * The indeterminate spinner: 2dp stroke in the current content colour (ink on cream, cream on a dark card, the
 * label colour inside a button). Give it a content description nearby (a "Saving" label) when the wait matters.
 */
@Composable
fun PoseitSpinner(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = LocalContentColor.current) {
    CircularProgressIndicator(modifier.size(size), color = color, strokeWidth = 2.dp, trackColor = Color.Transparent)
}
