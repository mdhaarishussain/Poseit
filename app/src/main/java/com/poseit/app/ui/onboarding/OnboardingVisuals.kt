package com.poseit.app.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.theme.PoseitColors
import kotlin.math.roundToInt

/* ------------------------------------------------------------------------------------------------
 * Page 1: a mannequin cross-fading through a few poses.
 * ---------------------------------------------------------------------------------------------- */

@Composable
internal fun MannequinCarousel(modifier: Modifier = Modifier) {
    val poses = remember { DemoPoses.carousel() }
    val transition = rememberInfiniteTransition(label = "carousel")
    // Infinite transitions (not delay loops) so tests and previews stay idle.
    val cycle = transition.animateFloat(
        initialValue = 0f,
        targetValue = poses.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = poses.size * 2_000, easing = LinearEasing)),
        label = "cycle",
    )
    val index by remember(poses) { derivedStateOf { cycle.value.toInt().coerceIn(0, poses.lastIndex) } }
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    BoxWithConstraints(modifier.semantics { contentDescription = "A mannequin showing different poses" }, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight)
        Box(
            Modifier
                .size(side * 0.86f)
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(PoseitColors.Violet.copy(alpha = 0.45f), PoseitColors.Violet.copy(alpha = 0.08f), Color.Transparent),
                    ),
                ),
        )
        Crossfade(targetState = index, animationSpec = tween(550), label = "carouselPose") { i ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                PoseThumbnail(
                    template = poses[i],
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    background = null,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    poses[i].name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------------
 * Page 2: a mock camera frame where a live skeleton moves into the target outline, which turns green.
 * ---------------------------------------------------------------------------------------------- */

@Composable
internal fun MatchDemo(modifier: Modifier = Modifier) {
    val target = remember { DemoPoses.matchTarget }
    // The DSL centres each pose's bounding box, so line the bodies up before blending.
    val start = remember { DemoPoses.alignHips(DemoPoses.matchStart.figures.first(), target.figures.first()) }
    val transition = rememberInfiniteTransition(label = "match")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 5_000
                0f at 0
                0f at 500
                1f at 2_900 using FastOutSlowInEasing
                1f at 4_300
                0f at 5_000 using FastOutSlowInEasing
            },
        ),
        label = "matchProgress",
    )
    val score by remember { derivedStateOf { 0.31f + 0.67f * progress.value } }
    val percent by remember { derivedStateOf { (score * 100).roundToInt() } }
    val matched by remember { derivedStateOf { score >= 0.9f } }
    val badgeColor by animateColorAsState(PoseitColors.forScore(score), label = "badge")

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val frameWidth = min(maxWidth, maxHeight * 0.75f)
        Box(
            Modifier
                .width(frameWidth)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(28.dp))
                .background(SceneGradient)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .semantics { contentDescription = "Camera preview: a live skeleton matching the pose outline" },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawThirds(Color.White.copy(alpha = 0.10f))
                drawViewfinderCorners(Color.White.copy(alpha = 0.55f))
                val aspect = target.frameAspect
                val rect = fitFrameRect(size, aspect)
                val color = PoseitColors.forScore(score)
                drawPoseFigure(
                    target.figures.first(), aspect, rect,
                    FigurePaint(style = FigureStyle.OUTLINE, fill = color, alpha = 0.95f, strokeWidthPx = 2.5.dp.toPx()),
                )
                val live = DemoPoses.blend(start, target.figures.first(), progress.value)
                drawPoseFigure(live, aspect, rect, FigurePaint(style = FigureStyle.STICK, fill = Color.White, alpha = 0.9f))
            }
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(badgeColor)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (matched) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF0E2A1D), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text("$percent%", style = MaterialTheme.typography.titleSmall, color = Color(0xFF15161A))
            }
            Text(
                if (matched) "Perfect! Hold it…" else "Raise your right arm",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

private val SceneGradient = Brush.verticalGradient(listOf(Color(0xFF3B3561), Color(0xFF2A2C45), Color(0xFF1A2A2A)))

private fun DrawScope.drawThirds(color: Color) {
    val w = size.width
    val h = size.height
    for (k in 1..2) {
        drawLine(color, Offset(w * k / 3f, 0f), Offset(w * k / 3f, h), strokeWidth = 1.dp.toPx())
        drawLine(color, Offset(0f, h * k / 3f), Offset(w, h * k / 3f), strokeWidth = 1.dp.toPx())
    }
}

private fun DrawScope.drawViewfinderCorners(color: Color) {
    val inset = 14.dp.toPx()
    val len = 22.dp.toPx()
    val stroke = 3.dp.toPx()
    val r = Rect(Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset))
    val corners = listOf(
        r.topLeft to Offset(1f, 1f),
        r.topRight to Offset(-1f, 1f),
        r.bottomLeft to Offset(1f, -1f),
        r.bottomRight to Offset(-1f, -1f),
    )
    for ((p, d) in corners) {
        drawLine(color, p, p + Offset(len * d.x, 0f), stroke, StrokeCap.Round)
        drawLine(color, p, p + Offset(0f, len * d.y), stroke, StrokeCap.Round)
    }
}

/* ------------------------------------------------------------------------------------------------
 * Page 3: hands-free features with a highlight travelling between them.
 * ---------------------------------------------------------------------------------------------- */

private data class Feature(val icon: ImageVector, val accent: Color, val title: String, val body: String)

private val handsFreeFeatures = listOf(
    Feature(Icons.Filled.AutoAwesome, PoseitColors.Mint, "Auto-capture", "Shoots the moment you match"),
    Feature(Icons.Filled.RecordVoiceOver, PoseitColors.Amber, "Voice coach", "“Tilt your head a little left”"),
    Feature(Icons.Filled.Timer, PoseitColors.Coral, "Timer", "3, 5 or 10 second countdown"),
    Feature(Icons.Filled.BurstMode, PoseitColors.Violet, "Photoshoot", "A shot every few seconds"),
)

@Composable
internal fun HandsFreeDemo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "handsFree")
    val cycle = transition.animateFloat(
        initialValue = 0f,
        targetValue = handsFreeFeatures.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(handsFreeFeatures.size * 1_500, easing = LinearEasing)),
        label = "highlight",
    )
    val active by remember { derivedStateOf { cycle.value.toInt().coerceIn(0, handsFreeFeatures.lastIndex) } }
    // 3-2-1 for the timer tile, synced to its highlight.
    val countdown by remember {
        derivedStateOf {
            val local = cycle.value - 2f
            if (local in 0f..1f) 3 - (local * 3f).toInt().coerceAtMost(2) else 3
        }
    }
    Column(
        modifier.widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0 until 2) {
                    val index = row * 2 + col
                    FeatureTile(
                        feature = handsFreeFeatures[index],
                        highlighted = index == active,
                        badge = if (index == 2) "$countdown" else null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureTile(feature: Feature, highlighted: Boolean, badge: String?, modifier: Modifier = Modifier) {
    val border by animateColorAsState(
        if (highlighted) feature.accent else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(400),
        label = "tileBorder",
    )
    val bg by animateColorAsState(
        if (highlighted) feature.accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(400),
        label = "tileBg",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(feature.accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(feature.icon, contentDescription = null, tint = feature.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (highlighted) feature.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(feature.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            feature.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------------------------------------------------------------------
 * Page 4: a photo turned into a guide + a hand-drawn mannequin.
 * ---------------------------------------------------------------------------------------------- */

@Composable
internal fun MakeItYoursDemo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mine")
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cardWidth = min(maxWidth * 0.44f, maxHeight * 0.62f)
        DemoCard(
            template = DemoPoses.handOnHip,
            style = FigureStyle.OUTLINE,
            background = PhotoGradient,
            icon = Icons.Filled.AddAPhoto,
            caption = "From a photo",
            modifier = Modifier
                .width(cardWidth)
                .offset(x = -cardWidth * 0.53f)
                .graphicsLayer { rotationZ = -7f + sway * 1.5f },
        )
        DemoCard(
            template = DemoPoses.star,
            style = FigureStyle.SILHOUETTE,
            background = DrawGradient,
            icon = Icons.Filled.Draw,
            caption = "Drawn by you",
            modifier = Modifier
                .width(cardWidth)
                .offset(x = cardWidth * 0.53f, y = cardWidth * 0.08f)
                .graphicsLayer { rotationZ = 6f - sway * 1.5f },
        )
    }
}

private val PhotoGradient = Brush.linearGradient(listOf(Color(0xFFFFB38A), Color(0xFFB765E8), Color(0xFF3A3170)))
private val DrawGradient = Brush.linearGradient(listOf(Color(0xFF2E3140), Color(0xFF1D1F27)))

@Composable
private fun DemoCard(
    template: PoseTemplate,
    style: FigureStyle,
    background: Brush,
    icon: ImageVector,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(background),
        ) {
            PoseThumbnail(template = template, modifier = Modifier.fillMaxSize(), style = style, background = null)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            caption,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        )
    }
}
