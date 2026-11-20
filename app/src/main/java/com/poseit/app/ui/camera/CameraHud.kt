package com.poseit.app.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.coach.CaptureState
import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind
import com.poseit.app.ui.theme.PoseitColors

private val HudScrim = Color.Black.copy(alpha = 0.6f)
private val PerfectInk = Color(0xFF0B2A1C)
private val FramingAccent = Color(0xFFB9A8FF)

/**
 * Match score in a ring, plus an outer mint progress ring while the coach is holding before auto-capture.
 * Shows a spinner while the detector loads and a warning when detection is unavailable.
 */
@Composable
internal fun ScoreBadge(vm: CameraViewModel, detectorReady: Boolean, detectorError: String?, modifier: Modifier = Modifier) {
    val percent by vm.scorePercent.collectAsState()
    val capture = vm.capture.collectAsState()
    val p = percent
    val color = if (p != null) PoseitColors.forScore(p / 100f) else PoseitColors.OnSurfaceMuted
    val description = when {
        detectorError != null -> "Pose detection unavailable"
        !detectorReady -> "Starting pose detection"
        p == null -> "Nobody detected yet"
        else -> "Pose match $p percent"
    }
    Box(
        modifier
            .size(62.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val ring = 4.dp.toPx()
            val outer = 2.5.dp.toPx()
            drawCircle(HudScrim)
            val inset = outer + 2.dp.toPx() + ring / 2f
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.16f), 0f, 360f, false, topLeft, arcSize, style = Stroke(ring))
            if (p != null && p > 0) {
                drawArc(color, -90f, 360f * p / 100f, false, topLeft, arcSize, style = Stroke(ring, cap = StrokeCap.Round))
            }
            // Read in the draw phase: updates ~30x/s without recomposition.
            val state = capture.value
            if (state is CaptureState.Holding) {
                val o = outer / 2f
                drawArc(
                    PoseitColors.Mint, -90f, 360f * state.progress.coerceIn(0f, 1f), false,
                    Offset(o, o), Size(size.width - outer, size.height - outer),
                    style = Stroke(outer, cap = StrokeCap.Round),
                )
            }
        }
        when {
            detectorError != null -> Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = PoseitColors.Amber)
            !detectorReady -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            p == null -> Icon(Icons.Filled.PersonSearch, contentDescription = null, tint = PoseitColors.OnSurfaceMuted)
            else -> Text(
                "$p%",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
        }
    }
}

private fun HintKind.accent(): Color = when (this) {
    HintKind.PERFECT -> PoseitColors.Mint
    HintKind.POSE -> PoseitColors.Amber
    HintKind.FRAMING -> FramingAccent
    HintKind.VISIBILITY -> PoseitColors.Coral
    HintKind.NO_PERSON, HintKind.MISSING_PERSON -> PoseitColors.OnSurfaceMuted
}

private fun HintKind.icon(): ImageVector = when (this) {
    HintKind.PERFECT -> Icons.Filled.CheckCircle
    HintKind.POSE -> Icons.Filled.AccessibilityNew
    HintKind.FRAMING -> Icons.Filled.CropFree
    HintKind.VISIBILITY -> Icons.Filled.Visibility
    HintKind.NO_PERSON, HintKind.MISSING_PERSON -> Icons.Filled.PersonSearch
}

/** The current (stabilised) coaching hint. */
@Composable
internal fun HintBanner(hint: CoachHint?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = hint,
        contentKey = { it?.text to it?.kind },
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        modifier = modifier,
        label = "hint",
    ) { h ->
        if (h == null) {
            Spacer(Modifier.size(0.dp))
            return@AnimatedContent
        }
        val perfect = h.kind == HintKind.PERFECT
        val accent = h.kind.accent()
        Surface(
            color = if (perfect) accent.copy(alpha = 0.94f) else HudScrim,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(h.kind.icon(), contentDescription = null, tint = if (perfect) PerfectInk else accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    h.text,
                    color = if (perfect) PerfectInk else Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Small chip for a lighting problem (backlit, face too dark, ...). */
@Composable
internal fun LightingChip(hint: CoachHint?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = hint != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val text = hint?.text.orEmpty()
        Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = PoseitColors.Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Temporary pill with the pose name and where to hold the camera. Tapping opens the tips sheet. */
@Composable
internal fun PosePill(pose: PoseTemplate?, visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && pose != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier,
    ) {
        if (pose == null) return@AnimatedVisibility
        Surface(onClick = onClick, color = HudScrim, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        pose.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        pose.camera.height.advice,
                        color = PoseitColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Info, contentDescription = "Pose tips", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** "3/10" photoshoot counter with a stop button. */
@Composable
internal fun ShotCounter(session: SessionInfo, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = HudScrim, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BurstMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                session.label,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics {
                    contentDescription = if (session.total == 0) "Photo ${session.shot}" else "Photo ${session.shot} of ${session.total}"
                },
            )
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop photoshoot", tint = PoseitColors.Red)
            }
        }
    }
}

/** Small persistent notice when pose detection could not start. */
@Composable
internal fun DetectorNotice(modifier: Modifier = Modifier) {
    Surface(color = HudScrim, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = PoseitColors.Amber, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Matching off", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Big animated countdown numbers. */
@Composable
internal fun CountdownNumber(seconds: Int?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = seconds != null, enter = fadeIn(), exit = fadeOut(tween(150)), modifier = modifier) {
        AnimatedContent(
            targetState = seconds ?: 0,
            transitionSpec = {
                (scaleIn(initialScale = 1.6f) + fadeIn(tween(200))) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut(tween(150)))
            },
            label = "countdown",
        ) { s ->
            Text(
                text = if (s > 0) "$s" else "",
                color = Color.White,
                style = TextStyle(
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 4f), blurRadius = 18f),
                ),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
    }
}

/** A round translucent toggle used in the vertical tool rail. */
@Composable
internal fun RailToggle(
    icon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White,
            checkedContainerColor = PoseitColors.Violet,
            checkedContentColor = Color.White,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

/** A round translucent action button used in the vertical tool rail. */
@Composable
internal fun RailButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (highlighted) PoseitColors.Violet else Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

/**
 * Vertical tool rail on the right edge of the preview: pose tips, overlay lock, mirror pose, overlay mode
 * (photo poses) and background match ("lock scene").
 */
@Composable
internal fun ToolRail(
    ui: CameraUiState,
    onTips: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleMirror: () -> Unit,
    onOverlayMode: (OverlayMode) -> Unit,
    onToggleSceneLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (ui.hasPose) {
            RailButton(Icons.Filled.Info, "Pose tips", onClick = onTips)
            RailToggle(
                if (ui.overlayLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                if (ui.overlayLocked) "Unlock pose guide" else "Lock pose guide in place",
                checked = ui.overlayLocked,
                onCheckedChange = { onToggleLock() },
            )
            RailToggle(Icons.Filled.Flip, "Mirror pose", checked = ui.poseMirrored, onCheckedChange = { onToggleMirror() })
            val modes = ui.availableOverlayModes
            if (modes.size > 1) {
                OverlayModeButton(ui.overlayMode, modes, onOverlayMode)
            }
        }
        RailToggle(
            Icons.Filled.Wallpaper,
            if (ui.sceneLock != null) "Clear locked background" else "Lock background to match the framing",
            checked = ui.sceneLock != null,
            onCheckedChange = { onToggleSceneLock() },
        )
    }
}

private fun OverlayMode.icon(): ImageVector = when (this) {
    OverlayMode.MANNEQUIN -> Icons.Filled.AccessibilityNew
    OverlayMode.PHOTO_GHOST -> Icons.Filled.Image
    OverlayMode.PHOTO_OUTLINE -> Icons.Filled.PersonOutline
}

@Composable
private fun OverlayModeButton(current: OverlayMode, modes: List<OverlayMode>, onSelect: (OverlayMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        RailButton(
            current.icon(),
            "Guide: ${current.label}. Change guide style",
            onClick = { open = true },
            highlighted = current != OverlayMode.MANNEQUIN,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label, fontWeight = if (mode == current) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = { Icon(mode.icon(), contentDescription = null) },
                    onClick = {
                        open = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}
