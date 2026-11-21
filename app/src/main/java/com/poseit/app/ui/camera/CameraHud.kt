package com.poseit.app.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.coach.CaptureState
import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind
import com.poseit.app.ui.components.CircleIconButton
import com.poseit.app.ui.components.CircleIconToggle
import com.poseit.app.ui.components.CircleVariant
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitMenu
import com.poseit.app.ui.components.PoseitMenuItem
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/*
 * The camera HUD (docs/DESIGN.md, "Screen map", Camera). Over the live view sit three kinds of chrome:
 *  - circle controls: cream outlined glyphs on black-at-45% discs (CircleIconButton, variant Camera); a toggle that
 *    is on inverts to a cream disc with an ink glyph (CircleIconToggle);
 *  - cue cards: anything with words in it is ink on a cream card with a 1dp ink outline (the mockups' outlined cream
 *    cards), so it stays legible over a black room and a pale wall alike;
 *  - the score: Light numerals in a ring on a solid dark card disc.
 * The only signal colours are the score ring, its hold arc and the small severity dot of the hint card. The words
 * always say the same thing, so colour is never alone.
 */

/** Diameter of the score badge. */
internal val ScoreBadgeSize = 60.dp

/** Cream of the camera chrome: the same cream as the lay figures. */
private val ChromeContent = PoseitColors.cameraContent

/** Cream at 60%: the score ring while nobody is detected. */
private val IdleRing = PoseitColors.cameraContent.copy(alpha = 0.6f)

/**
 * The live match score: Light numerals inside a ring, on a solid dark card disc (so the thin numerals read over any
 * scene). The ring is cream at 60% until somebody is detected, then takes the bright signal colour of the score.
 * While the coach holds before auto-capture, a heavier arc in the same colour runs once around the ring. Shows a
 * spinner while the detector loads and a warning glyph when detection is unavailable.
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
    val ringColor = if (p != null && detectorError == null) PoseitColors.forScore(p / 100f, onDark = true) else IdleRing
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
            val ring = 1.5.dp.toPx()
            drawCircle(PoseitColors.backgroundCard)
            val inset = hold / 2f + 3.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            drawArc(ringColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(ring))
            holdProgress()?.let { progress ->
                drawArc(
                    ringColor, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize,
                    style = Stroke(hold, cap = StrokeCap.Round),
                )
            }
        }
        when {
            detectorError != null -> Icon(PoseitIcons.warning, contentDescription = null, tint = ChromeContent, modifier = Modifier.size(IconSize.standalone))
            !detectorReady -> PoseitSpinner(size = 20.dp, color = ChromeContent)
            p == null -> Icon(Icons.Outlined.PersonSearch, contentDescription = null, tint = IdleRing, modifier = Modifier.size(IconSize.standalone))
            else -> ScoreNumeral(p)
        }
    }
}

/**
 * "87%" in the mockups' voice: Light numerals tracked tight, the percent sign a size smaller. Sized in dp: it has to
 * fit inside the fixed ring at any font scale (the badge's content description carries the value).
 */
@Composable
private fun ScoreNumeral(p: Int) {
    val density = LocalDensity.current
    // Three digits ("100") need a smaller size to clear the ring.
    val digits = with(density) { (if (p >= 100) 17.dp else 21.dp).toSp() }
    val sign = with(density) { 11.dp.toSp() }
    Text(
        buildAnnotatedString {
            append("$p")
            withStyle(SpanStyle(fontSize = sign, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)) { append("%") }
        },
        color = ChromeContent,
        style = PoseitType.numeral(digits, FontWeight.Light),
        maxLines = 1,
    )
}

/**
 * Severity of a hint as a small signal-colour dot on the cue card (the text always says the same thing, so colour
 * is never the only carrier). None while nobody is in the picture.
 */
private fun HintKind.signal(): Color? = when (this) {
    HintKind.PERFECT -> PoseitColors.signalMatch
    HintKind.POSE, HintKind.FRAMING -> PoseitColors.signalNear
    HintKind.VISIBILITY -> PoseitColors.signalMiss
    HintKind.NO_PERSON, HintKind.MISSING_PERSON -> null
}

/** The outline of a cue card: the mockups' 1dp ink edge, which also holds the card's shape against a pale wall. */
private val CueBorder = BorderStroke(1.dp, PoseitColors.borderEmphasized)

/** A cream "cue card": ink text with an ink outline, legible over any scene. */
@Composable
private fun CueCard(
    modifier: Modifier = Modifier,
    shape: Shape = PoseitShapes.Control,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ProvideSurfaceTone(SurfaceTone.Light) {
        if (onClick != null) {
            Surface(
                onClick = onClick,
                color = PoseitColors.backgroundSurface,
                contentColor = PoseitColors.textPrimary,
                shape = shape,
                border = CueBorder,
                modifier = modifier,
                content = content,
            )
        } else {
            Surface(
                color = PoseitColors.backgroundSurface,
                contentColor = PoseitColors.textPrimary,
                shape = shape,
                border = CueBorder,
                modifier = modifier,
                content = content,
            )
        }
    }
}

/** The current (stabilised) coaching hint, on a cue card, in Body large so it reads at arm's length. */
@Composable
internal fun HintBanner(hint: CoachHint?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = hint,
        contentKey = { it?.text to it?.kind },
        transitionSpec = {
            fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)) togetherWith fadeOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit))
        },
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
                Modifier
                    .heightIn(min = 48.dp)
                    .padding(horizontal = Space.l, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                h.kind.signal()?.let { dot ->
                    Box(Modifier.size(10.dp).background(dot, PoseitShapes.Circle))
                    Spacer(Modifier.width(Space.m))
                }
                Text(h.text, style = PoseitType.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A lighting problem (backlit, face too dark, ...): a smaller cue card. */
@Composable
internal fun LightingChip(hint: CoachHint?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = hint != null,
        enter = fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)),
        exit = fadeOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit)),
        modifier = modifier,
    ) {
        val text = hint?.text.orEmpty()
        CueCard {
            Row(Modifier.padding(start = Space.m, end = Space.m, top = Space.s, bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, modifier = Modifier.size(IconSize.inline))
                Spacer(Modifier.width(Space.s))
                Text(text, style = PoseitType.supporting, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The pose name and where to hold the camera, for a few seconds after a pose change. Tapping opens the tips. */
@Composable
internal fun PosePill(pose: PoseTemplate?, visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && pose != null,
        enter = fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)),
        exit = fadeOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit)),
        modifier = modifier,
    ) {
        if (pose == null) return@AnimatedVisibility
        CueCard(onClick = onClick) {
            Row(
                Modifier.padding(start = Space.l, end = Space.m, top = Space.s + Space.xs, bottom = Space.s + Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(pose.name, style = PoseitType.bodyStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        pose.camera.height.advice,
                        color = PoseitColors.textSecondary,
                        style = PoseitType.supporting,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Space.m))
                Icon(PoseitIcons.info, contentDescription = "Pose tips", modifier = Modifier.size(IconSize.standalone))
            }
        }
    }
}

/** "3 / 10" photoshoot counter with a stop button, on a cream pill. */
@Composable
internal fun ShotCounter(session: SessionInfo, onStop: () -> Unit, modifier: Modifier = Modifier) {
    CueCard(modifier, shape = PoseitShapes.Pill) {
        Row(Modifier.padding(start = Space.l + Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(
                session.label,
                style = PoseitType.numeral(17.sp),
                modifier = Modifier.semantics {
                    contentDescription = if (session.total == 0) "Photo ${session.shot}" else "Photo ${session.shot} of ${session.total}"
                },
            )
            PlainIconButton(Icons.Outlined.Stop, contentDescription = "Stop photoshoot", onClick = onStop)
        }
    }
}

/** Small persistent notice when pose detection could not start. */
@Composable
internal fun DetectorNotice(modifier: Modifier = Modifier) {
    CueCard(modifier) {
        Text(
            "Pose matching unavailable",
            style = PoseitType.supporting,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s),
        )
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
            style = PoseitType.body,
            modifier = Modifier
                .padding(horizontal = Space.l, vertical = Space.m)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * The self-timer countdown: huge Light numerals drawn like the lay figures (cream with an ink contour), so they read
 * over a bright or a dark scene without a shadow. With the capture flash this is the camera's one orchestrated
 * motion: each second lands with a short settle from 115% and leaves with a quick fade. No bounce.
 */
@Composable
internal fun CountdownNumber(seconds: Int?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = seconds != null, enter = fadeIn(tween(120)), exit = fadeOut(tween(120)), modifier = modifier) {
        AnimatedContent(
            targetState = seconds ?: 0,
            transitionSpec = {
                (scaleIn(PoseitMotion.standard(PoseitMotion.MEDIUM), initialScale = 1.15f) + fadeIn(tween(PoseitMotion.FAST))) togetherWith
                    fadeOut(tween(100, easing = PoseitMotion.EaseExit))
            },
            label = "countdown",
        ) { s ->
            val text = if (s > 0) "$s" else ""
            val style = PoseitType.numeral(CountdownSize, FontWeight.Light)
            val contour = with(LocalDensity.current) { 5.dp.toPx() }
            Box {
                // The contour is a drawing of the same numeral: hidden from screen readers so it isn't read twice.
                Text(
                    text,
                    color = PoseitColors.figureContour,
                    style = style.copy(drawStyle = Stroke(width = contour, join = StrokeJoin.Round)),
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Text(text, color = ChromeContent, style = style, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            }
        }
    }
}

/** Larger than numeralDisplay: the countdown is read from across the room. */
private val CountdownSize = 120.sp

/** Diameter of a circle control over the live view (its touch target is 48dp too). */
internal val CameraControlSize: Dp = 48.dp

/**
 * Vertical tool rail on the right edge of the preview: pose tips, overlay lock, mirror pose, overlay mode
 * (photo poses) and background match ("lock scene"), as camera circles.
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
            CircleIconButton(PoseitIcons.info, "Pose tips", onClick = onTips, variant = CircleVariant.Camera, size = CameraControlSize)
            CircleIconToggle(
                // The padlock closes as well as the disc turning cream: the state is in the shape too.
                icon = if (ui.overlayLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                contentDescription = if (ui.overlayLocked) "Unlock pose guide" else "Lock pose guide in place",
                checked = ui.overlayLocked,
                onCheckedChange = { onToggleLock() },
                size = CameraControlSize,
            )
            CircleIconToggle(Icons.Outlined.Flip, "Mirror pose", checked = ui.poseMirrored, onCheckedChange = { onToggleMirror() }, size = CameraControlSize)
            val modes = ui.availableOverlayModes
            if (modes.size > 1) {
                OverlayModeButton(ui.overlayMode, modes, onOverlayMode)
            }
        }
        CircleIconToggle(
            icon = Icons.Outlined.Wallpaper,
            contentDescription = if (ui.sceneLock != null) "Clear locked background" else "Lock background to match the framing",
            checked = ui.sceneLock != null,
            onCheckedChange = { onToggleSceneLock() },
            size = CameraControlSize,
        )
    }
}

private fun OverlayMode.icon(): ImageVector = when (this) {
    OverlayMode.MANNEQUIN -> Icons.Outlined.AccessibilityNew
    OverlayMode.PHOTO_GHOST -> Icons.Outlined.Image
    OverlayMode.PHOTO_OUTLINE -> Icons.Outlined.PersonOutline
}

/** The guide style of a photo pose: a camera circle that turns cream while a photo guide is shown. */
@Composable
private fun OverlayModeButton(current: OverlayMode, modes: List<OverlayMode>, onSelect: (OverlayMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        CircleIconButton(
            current.icon(),
            "Guide: ${current.label}. Change guide style",
            onClick = { open = true },
            variant = if (current != OverlayMode.MANNEQUIN) CircleVariant.Light else CircleVariant.Camera,
            size = CameraControlSize,
        )
        PoseitMenu(expanded = open, onDismissRequest = { open = false }) {
            modes.forEach { mode ->
                PoseitMenuItem(mode.label, leadingIcon = mode.icon(), selected = mode == current) {
                    open = false
                    onSelect(mode)
                }
            }
        }
    }
}
