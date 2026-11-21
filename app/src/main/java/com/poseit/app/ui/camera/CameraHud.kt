package com.poseit.app.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.coach.CaptureState
import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioInlineIconSize
import com.poseit.app.ui.components.StudioMenu
import com.poseit.app.ui.components.StudioMenuItem
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/*
 * The camera HUD (docs/DESIGN.md, "Camera screen"). Two kinds of chrome sit over the live view:
 *  - round controls: Mannequin-white outlined icons on black-at-45% discs; an active toggle inverts to a Sheet disc
 *    with an Ink (filled) icon;
 *  - cue cards: anything with words in it is set in Ink on a Sheet card, so it stays legible over any scene.
 * The only signal colours here are the score ring and the small severity dot of the hint card.
 */

/** Diameter of a round camera control (inside its 48dp touch target). */
internal val RoundControlSize = 44.dp

/** Diameter of the score badge. */
internal val ScoreBadgeSize = 56.dp

/** The white of the camera chrome: the same paper white as the lay figures. */
private val ChromeWhite = Studio.Mannequin

/** White at 60%: the score ring while nobody is detected. */
private val IdleRing = Studio.Mannequin.copy(alpha = 0.6f)

/**
 * A round camera control: a Mannequin outlined icon on a [Studio.ControlScrim] disc. With [active] it inverts
 * (Sheet disc, Ink icon) to show a mode that is switched on, e.g. a running self-timer or a photo guide style.
 */
@Composable
internal fun RoundControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = RoundControlSize,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (active) Studio.Sheet else Studio.ControlScrim,
            contentColor = if (active) Studio.Ink else ChromeWhite,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(StudioIconSize))
    }
}

/**
 * A round on/off control (a checkable node): outlined icon on the dark disc when off; Sheet disc with the
 * [activeIcon] (the filled form) in Ink when on.
 */
@Composable
internal fun RoundToggle(
    icon: ImageVector,
    activeIcon: ImageVector,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.size(RoundControlSize),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Studio.ControlScrim,
            contentColor = ChromeWhite,
            checkedContainerColor = Studio.Sheet,
            checkedContentColor = Studio.Ink,
        ),
    ) {
        Icon(if (checked) activeIcon else icon, contentDescription = contentDescription, modifier = Modifier.size(StudioIconSize))
    }
}

/**
 * The live match score: a numeral inside a ring on a dark disc. The ring is white at 60% until somebody is
 * detected, then takes the bright signal colour of the score. While the coach holds before auto-capture, a heavier
 * arc in the same colour runs once around the ring. Shows a spinner while the detector loads and a warning glyph
 * when detection is unavailable.
 */
@Composable
internal fun ScoreBadge(vm: CameraViewModel, detectorReady: Boolean, detectorError: String?, modifier: Modifier = Modifier) {
    val percent by vm.scorePercent.collectAsState()
    val capture = vm.capture.collectAsState()
    ScoreRing(
        percent = percent,
        detectorReady = detectorReady,
        detectorError = detectorError,
        // Read in the draw phase: updates ~30x/s without recomposition.
        holdProgress = { (capture.value as? CaptureState.Holding)?.progress },
        modifier = modifier,
    )
}

/**
 * The stateless [ScoreBadge].
 * @param percent the match score, or null while nobody is detected
 * @param holdProgress 0..1 while the coach holds before auto-capture, else null; read only while drawing
 */
@Composable
internal fun ScoreRing(
    percent: Int?,
    detectorReady: Boolean,
    detectorError: String?,
    holdProgress: () -> Float?,
    modifier: Modifier = Modifier,
) {
    val p = percent
    val ringColor = if (p != null && detectorError == null) Studio.forScore(p / 100f, onDark = true) else IdleRing
    val description = when {
        detectorError != null -> "Pose detection unavailable"
        !detectorReady -> "Starting pose detection"
        p == null -> "Nobody detected yet"
        else -> "Pose match $p percent"
    }
    Box(
        modifier
            .size(ScoreBadgeSize)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val hold = 4.dp.toPx()
            val ring = 2.dp.toPx()
            drawCircle(Studio.ControlScrim)
            val inset = hold / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            drawArc(ringColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(ring))
            holdProgress()?.let { progress ->
                drawArc(
                    ringColor, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize,
                    style = Stroke(hold, cap = StrokeCap.Butt),
                )
            }
        }
        when {
            detectorError != null -> Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ChromeWhite, modifier = Modifier.size(StudioIconSize))
            !detectorReady -> CircularProgressIndicator(Modifier.size(20.dp), color = ChromeWhite, strokeWidth = 2.dp, trackColor = Color.Transparent)
            p == null -> Icon(Icons.Outlined.PersonSearch, contentDescription = null, tint = IdleRing, modifier = Modifier.size(StudioIconSize))
            else -> {
                // Sized in dp: the numeral has to fit inside the fixed ring at any font scale (the badge's content
                // description carries the value for screen readers).
                // Three expanded digits ("100") need a smaller size to clear the ring.
                val numeral = with(LocalDensity.current) { (if (p >= 100) 15.dp else 18.dp).toSp() }
                Text("$p", color = ChromeWhite, style = StudioType.numeral(numeral), maxLines = 1)
            }
        }
    }
}

/**
 * Severity of a hint as a small signal-colour dot on the cue card (the text always says the same thing, so colour
 * is never the only carrier). None while nobody is in the picture.
 */
private fun HintKind.signal(): Color? = when (this) {
    HintKind.PERFECT -> Studio.Match
    HintKind.POSE, HintKind.FRAMING -> Studio.Near
    HintKind.VISIBILITY -> Studio.Miss
    HintKind.NO_PERSON, HintKind.MISSING_PERSON -> null
}

/** A Sheet "cue card": Ink text that stays legible over any scene. */
@Composable
private fun CueCard(
    modifier: Modifier = Modifier,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = if (small) StudioShapes.Small else StudioShapes.Medium
    if (onClick != null) {
        Surface(onClick = onClick, color = Studio.Sheet, contentColor = Studio.Ink, shape = shape, modifier = modifier, content = content)
    } else {
        Surface(color = Studio.Sheet, contentColor = Studio.Ink, shape = shape, modifier = modifier, content = content)
    }
}

/** The current (stabilised) coaching hint, on a cue card. */
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
        CueCard(
            Modifier
                .widthIn(max = 420.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                Modifier.padding(horizontal = Space.l, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                h.kind.signal()?.let { dot ->
                    Box(Modifier.size(8.dp).background(dot, CircleShape))
                    Spacer(Modifier.width(Space.s))
                }
                Text(h.text, style = StudioType.bodyStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A lighting problem (backlit, face too dark, ...): a smaller cue card. */
@Composable
internal fun LightingChip(hint: CoachHint?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = hint != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(120)), modifier = modifier) {
        val text = hint?.text.orEmpty()
        CueCard(small = true) {
            Row(Modifier.padding(horizontal = Space.m, vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(StudioInlineIconSize))
                Spacer(Modifier.width(Space.s))
                Text(text, style = StudioType.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The pose name and where to hold the camera, for a few seconds after a pose change. Tapping opens the tips. */
@Composable
internal fun PosePill(pose: PoseTemplate?, visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && pose != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
        modifier = modifier,
    ) {
        if (pose == null) return@AnimatedVisibility
        CueCard(onClick = onClick) {
            Row(
                Modifier.padding(start = Space.l, end = Space.m, top = Space.s, bottom = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(pose.name, style = StudioType.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        pose.camera.height.advice,
                        color = Studio.Graphite,
                        style = StudioType.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Space.s))
                Icon(Icons.Outlined.Info, contentDescription = "Pose tips", modifier = Modifier.size(StudioIconSize))
            }
        }
    }
}

/** "3 / 10" photoshoot counter with a stop button. */
@Composable
internal fun ShotCounter(session: SessionInfo, onStop: () -> Unit, modifier: Modifier = Modifier) {
    CueCard(modifier) {
        Row(Modifier.padding(start = Space.l), verticalAlignment = Alignment.CenterVertically) {
            Text(
                session.label,
                style = StudioType.numeral(15.sp),
                modifier = Modifier.semantics {
                    contentDescription = if (session.total == 0) "Photo ${session.shot}" else "Photo ${session.shot} of ${session.total}"
                },
            )
            StudioIconButton(Icons.Outlined.Stop, contentDescription = "Stop photoshoot", onClick = onStop)
        }
    }
}

/** Small persistent notice when pose detection could not start. */
@Composable
internal fun DetectorNotice(modifier: Modifier = Modifier) {
    CueCard(modifier, small = true) {
        Text("Pose matching unavailable", style = StudioType.label, modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s))
    }
}

/**
 * Why the camera isn't working (in use by another app, disabled, failed to start...), shown over the black preview
 * for as long as [com.poseit.app.camera.PoseCamera.error] reports it. Also used for the storage notice.
 */
@Composable
internal fun CameraErrorNotice(message: String, modifier: Modifier = Modifier) {
    CueCard(modifier.widthIn(max = 320.dp)) {
        Text(
            message,
            style = StudioType.body,
            modifier = Modifier
                .padding(horizontal = Space.l, vertical = Space.m)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * The self-timer countdown: huge numerals drawn like the lay figures (Mannequin white with an Ink contour), so they
 * read over a bright or a dark scene without a shadow. With the capture flash this is the camera's one
 * orchestrated motion: each second lands with a short settle from 115% and leaves with a quick fade. No bounce.
 */
@Composable
internal fun CountdownNumber(seconds: Int?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = seconds != null, enter = fadeIn(tween(120)), exit = fadeOut(tween(120)), modifier = modifier) {
        AnimatedContent(
            targetState = seconds ?: 0,
            transitionSpec = {
                (scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 1.15f) + fadeIn(tween(150))) togetherWith
                    fadeOut(tween(100))
            },
            label = "countdown",
        ) { s ->
            val text = if (s > 0) "$s" else ""
            val style = StudioType.numeral(CountdownSize)
            val contour = with(LocalDensity.current) { 6.dp.toPx() }
            Box {
                // The contour is a drawing of the same numeral: hidden from screen readers so it isn't read twice.
                Text(
                    text,
                    color = Studio.MannequinContour,
                    style = style.copy(drawStyle = Stroke(width = contour, join = StrokeJoin.Round)),
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Text(text, color = ChromeWhite, style = style, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            }
        }
    }
}

/** The largest numeral of the type scale. */
private val CountdownSize = 96.sp

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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
        if (ui.hasPose) {
            RoundControl(Icons.Outlined.Info, "Pose tips", onClick = onTips)
            RoundToggle(
                icon = Icons.Outlined.LockOpen,
                activeIcon = Icons.Filled.Lock,
                contentDescription = if (ui.overlayLocked) "Unlock pose guide" else "Lock pose guide in place",
                checked = ui.overlayLocked,
                onCheckedChange = { onToggleLock() },
            )
            RoundToggle(Icons.Outlined.Flip, Icons.Filled.Flip, "Mirror pose", checked = ui.poseMirrored, onCheckedChange = { onToggleMirror() })
            val modes = ui.availableOverlayModes
            if (modes.size > 1) {
                OverlayModeButton(ui.overlayMode, modes, onOverlayMode)
            }
        }
        RoundToggle(
            icon = Icons.Outlined.Wallpaper,
            activeIcon = Icons.Filled.Wallpaper,
            contentDescription = if (ui.sceneLock != null) "Clear locked background" else "Lock background to match the framing",
            checked = ui.sceneLock != null,
            onCheckedChange = { onToggleSceneLock() },
        )
    }
}

private fun OverlayMode.icon(): ImageVector = when (this) {
    OverlayMode.MANNEQUIN -> Icons.Outlined.AccessibilityNew
    OverlayMode.PHOTO_GHOST -> Icons.Outlined.Image
    OverlayMode.PHOTO_OUTLINE -> Icons.Outlined.PersonOutline
}

@Composable
private fun OverlayModeButton(current: OverlayMode, modes: List<OverlayMode>, onSelect: (OverlayMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundControl(
            current.icon(),
            "Guide: ${current.label}. Change guide style",
            onClick = { open = true },
            active = current != OverlayMode.MANNEQUIN,
        )
        StudioMenu(expanded = open, onDismissRequest = { open = false }) {
            modes.forEach { mode ->
                StudioMenuItem(mode.label, leadingIcon = mode.icon(), selected = mode == current) {
                    open = false
                    onSelect(mode)
                }
            }
        }
    }
}
