package com.poseit.app.ui.gallery

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * A photo that can be pinch-zoomed, panned and double-tap-zoomed. While not zoomed, single-finger drags are left
 * unconsumed so a surrounding pager can swipe between photos.
 *
 * @param resetKey zoom is reset whenever this changes (e.g. when the page stops being the current one)
 * @param onZoomChanged true while the image is zoomed in
 */
@Composable
internal fun ZoomableImage(
    model: Any,
    contentDescription: String?,
    resetKey: Any?,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var intrinsicSize by remember { mutableStateOf(Size.Unspecified) }
    var loadState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    /** Size of the image as laid out with ContentScale.Fit at scale 1. */
    fun fittedSize(): Size {
        val cw = containerSize.width.toFloat()
        val ch = containerSize.height.toFloat()
        if (cw <= 0f || ch <= 0f) return Size.Zero
        val iw = intrinsicSize.takeIf { it.isSpecified && it.width > 0f && it.height > 0f } ?: return Size(cw, ch)
        val s = min(cw / iw.width, ch / iw.height)
        return Size(iw.width * s, iw.height * s)
    }

    fun clamp(o: Offset, s: Float): Offset {
        val fitted = fittedSize()
        val maxX = max(0f, (fitted.width * s - containerSize.width) / 2f)
        val maxY = max(0f, (fitted.height * s - containerSize.height) / 2f)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun animateTo(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        scope.launch {
            animate(0f, 1f, animationSpec = tween(durationMillis = 260)) { t, _ ->
                scale = startScale + (targetScale - startScale) * t
                offset = startOffset + (targetOffset - startOffset) * t
            }
        }
    }

    LaunchedEffect(resetKey) {
        scale = 1f
        offset = Offset.Zero
    }
    LaunchedEffect(Unit) {
        snapshotFlow { scale > 1.01f }.distinctUntilChanged().collect { currentOnZoomChanged(it) }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2 || scale > 1.01f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (centroid.isSpecified) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val c = centroid - center
                                val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                                // Keep the content point under the fingers in place while zooming, then pan.
                                val zoomed = c - (c - offset) * (newScale / scale) + pan
                                scale = newScale
                                offset = clamp(zoomed, newScale)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (scale < 1.02f) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = { tap ->
                        if (scale > 1.01f) {
                            animateTo(1f, Offset.Zero)
                        } else {
                            val c = tap - Offset(size.width / 2f, size.height / 2f)
                            animateTo(DOUBLE_TAP_SCALE, clamp(c - c * DOUBLE_TAP_SCALE, DOUBLE_TAP_SCALE))
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        // Decode a bit above screen size so zoomed-in photos stay reasonably sharp, capped to keep memory in check
        // (2560 px ~ 20 MB per decoded photo).
        val requestPx = with(density) { (max(maxWidth.toPx(), maxHeight.toPx()) * 1.5f).roundToInt() }.coerceIn(512, 2560)
        val context = LocalContext.current
        val request = remember(model, requestPx) {
            ImageRequest.Builder(context)
                .data(model)
                .size(requestPx, requestPx)
                .precision(Precision.INEXACT)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            onState = { state ->
                loadState = state
                if (state is AsyncImagePainter.State.Success) intrinsicSize = state.painter.intrinsicSize
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
        when (loadState) {
            is AsyncImagePainter.State.Loading ->
                CircularProgressIndicator(Modifier.size(28.dp), color = Studio.Mannequin.copy(alpha = 0.72f), strokeWidth = 2.dp)
            is AsyncImagePainter.State.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.BrokenImage, contentDescription = null, tint = Studio.Mannequin.copy(alpha = 0.72f), modifier = Modifier.size(StudioIconSize))
                Text("Couldn't open this photo", style = StudioType.body, color = Studio.Mannequin.copy(alpha = 0.72f))
            }
            else -> Unit
        }
    }
}
