package com.poseit.app.ui.camera

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.GridGoldenratio
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.poseit.app.camera.FlashMode
import com.poseit.app.camera.SceneAspect
import com.poseit.app.data.GridType
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PoseTileSize
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.components.StudioMenu
import com.poseit.app.ui.components.StudioMenuDivider
import com.poseit.app.ui.components.StudioMenuItem
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

private fun FlashMode.icon(): ImageVector = when (this) {
    FlashMode.OFF -> Icons.Outlined.FlashOff
    FlashMode.AUTO -> Icons.Outlined.FlashAuto
    FlashMode.ON -> Icons.Outlined.FlashOn
}

private fun FlashMode.label(): String = when (this) {
    FlashMode.OFF -> "off"
    FlashMode.AUTO -> "auto"
    FlashMode.ON -> "on"
}

private fun GridType.icon(): ImageVector = when (this) {
    GridType.NONE -> Icons.Outlined.GridOff
    GridType.THIRDS -> Icons.Outlined.Grid3x3
    GridType.GOLDEN -> Icons.Outlined.GridGoldenratio
    GridType.CENTER -> Icons.Outlined.CenterFocusStrong
}

internal fun SceneAspect.next(): SceneAspect = SceneAspect.entries[(ordinal + 1) % SceneAspect.entries.size]

/** Width of a tile in the pose strip (the tile is 3:4). */
private val StripTileWidth = 56.dp

/**
 * Back, flash, grid, aspect ratio, timer menu and quick settings, as round controls on the black surround. Flash and
 * grid cycle through their states and show the current one in their glyph; the timer inverts while one is set.
 */
@Composable
internal fun CameraTopBar(
    flash: FlashMode,
    hasFlash: Boolean,
    grid: GridType,
    aspect: SceneAspect,
    timer: TimerOption,
    onBack: () -> Unit,
    onFlash: () -> Unit,
    onGrid: () -> Unit,
    onAspect: () -> Unit,
    onTimer: (TimerOption) -> Unit,
    onPhotoshoot: () -> Unit,
    onQuickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(56.dp)
            // Discs land 14dp from the edge, in line with the tool rail over the preview.
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        RoundControl(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onClick = onBack)
        Spacer(Modifier.weight(1f))
        if (hasFlash) {
            RoundControl(flash.icon(), "Flash ${flash.label()}", onClick = onFlash)
        }
        RoundControl(grid.icon(), "Grid: ${grid.label}", onClick = onGrid)
        AspectToggle(aspect, onAspect)
        TimerMenuButton(timer, onTimer, onPhotoshoot)
        RoundControl(Icons.Outlined.Tune, "Quick settings", onClick = onQuickSettings)
    }
}

/** The aspect ratio as an outlined label ("3:4"), engraved-numeral style. Tapping cycles the ratios. */
@Composable
private fun AspectToggle(aspect: SceneAspect, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(StudioShapes.Small)
            .clickable(role = Role.Button, onClickLabel = "Change aspect ratio", onClick = onClick)
            .semantics { contentDescription = "Aspect ratio ${aspect.label}" }
            .padding(horizontal = Space.xs),
        contentAlignment = Alignment.Center,
    ) {
        // A fixed graphic: sized in dp so it keeps its proportions in the bar at any font scale.
        val size = with(LocalDensity.current) { 13.dp.toSp() }
        Text(
            aspect.label,
            style = StudioType.numeral(size),
            color = Studio.Mannequin,
            maxLines = 1,
            modifier = Modifier
                .border(1.5.dp, Studio.Mannequin, StudioShapes.Small)
                .padding(horizontal = Space.s, vertical = Space.xs),
        )
    }
}

@Composable
private fun TimerMenuButton(timer: TimerOption, onTimer: (TimerOption) -> Unit, onPhotoshoot: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val description = when (timer) {
        TimerOption.Off -> "Timer off"
        is TimerOption.Delay -> "Timer ${timer.seconds} seconds"
        is TimerOption.Photoshoot -> "Photoshoot"
    }
    val icon = when (timer) {
        TimerOption.Off -> Icons.Outlined.TimerOff
        is TimerOption.Delay -> Icons.Outlined.Timer
        is TimerOption.Photoshoot -> Icons.Outlined.BurstMode
    }
    Box {
        // The delay itself is shown on the shutter; the inverted disc says a timer is set.
        RoundControl(icon, description, onClick = { open = true }, active = timer != TimerOption.Off)
        StudioMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun item(text: String, option: TimerOption, icon: ImageVector) =
                StudioMenuItem(text, leadingIcon = icon, selected = timer == option) {
                    open = false
                    onTimer(option)
                }
            item("Off", TimerOption.Off, Icons.Outlined.TimerOff)
            TimerOption.DELAYS.forEach { s -> item("$s seconds", TimerOption.Delay(s), Icons.Outlined.Timer) }
            StudioMenuDivider()
            StudioMenuItem("Photoshoot…", leadingIcon = Icons.Outlined.BurstMode, selected = timer is TimerOption.Photoshoot) {
                open = false
                onPhotoshoot()
            }
        }
    }
}

/**
 * Horizontal strip: "All poses", a "None" tile (plain camera), then favourites and the current category, as compact
 * pose tiles. The selected tile gets a white outline and is scrolled into view.
 */
@Composable
internal fun PoseStrip(
    poses: List<PoseTemplate>,
    selectedId: String?,
    favorites: Set<String>,
    onSelect: (PoseTemplate?) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, poses) {
        val index = if (selectedId == null) 0 else poses.indexOfFirst { it.id == selectedId } + 1
        if (index >= 0) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StripActionTile(
            icon = Icons.Outlined.GridView,
            label = "All",
            description = "All poses",
            selected = false,
            onClick = onOpenAll,
            modifier = Modifier.padding(start = Space.m),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = Space.s),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "none") {
                StripActionTile(
                    icon = Icons.Outlined.Block,
                    label = "None",
                    description = "No pose, plain camera",
                    selected = selectedId == null,
                    onClick = { onSelect(null) },
                )
            }
            items(poses, key = { it.id }) { pose ->
                val selected = pose.id == selectedId
                val source = remember { MutableInteractionSource() }
                PoseTile(
                    pose = pose,
                    size = PoseTileSize.Compact,
                    selected = selected,
                    onDark = true,
                    interactionSource = source,
                    modifier = Modifier
                        .width(StripTileWidth)
                        .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Button) { onSelect(pose) }
                        .semantics { contentDescription = pose.name },
                )
            }
        }
    }
}

/** A strip tile that isn't a pose ("All", "None"): the same Set tile, with an outlined icon and a short label. */
@Composable
private fun StripActionTile(
    icon: ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(width = StripTileWidth, height = StripTileWidth * 4 / 3)
            .clip(StudioShapes.Medium)
            .background(Studio.Set)
            .then(if (selected) Modifier.border(2.dp, Studio.Mannequin, StudioShapes.Medium) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Studio.Ink, modifier = Modifier.size(StudioIconSize))
            Spacer(Modifier.height(Space.xs))
            Text(label, color = Studio.Ink, style = StudioType.label, maxLines = 1)
        }
    }
}

/** Last photo (opens the gallery), the shutter and the lens switch. */
@Composable
internal fun ShutterRow(
    lastPhoto: Uri?,
    sessionActive: Boolean,
    timer: TimerOption,
    capturing: Boolean,
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.section, vertical = Space.m),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryThumb(lastPhoto, onGallery)
        ShutterButton(sessionActive, timer, capturing, onShutter)
        FlipLensButton(onFlip)
    }
}

/** Diameter of the round controls beside the shutter. */
private val ShutterSideSize = 52.dp

@Composable
private fun GalleryThumb(lastPhoto: Uri?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(ShutterSideSize)
            .clip(CircleShape)
            .background(Studio.ControlScrim)
            .then(if (lastPhoto != null) Modifier.border(1.5.dp, Studio.Mannequin, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClickLabel = "Open gallery", onClick = onClick)
            .semantics { contentDescription = "Open gallery" },
        contentAlignment = Alignment.Center,
    ) {
        if (lastPhoto != null) {
            AsyncImage(
                model = lastPhoto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.5.dp)
                    .clip(CircleShape),
            )
        } else {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Studio.Mannequin, modifier = Modifier.size(StudioIconSize))
        }
    }
}

/**
 * The shutter: a white ring around a white disc. It shows the self-timer delay (or the photoshoot count) as a
 * numeral, and an Ink stop square while a timer or photoshoot runs.
 */
@Composable
private fun ShutterButton(sessionActive: Boolean, timer: TimerOption, capturing: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed || capturing) 0.88f else 1f,
        tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "shutterScale",
    )
    val description = when {
        sessionActive -> if (timer is TimerOption.Photoshoot) "Stop photoshoot" else "Stop timer"
        timer != TimerOption.Off -> "Start timer"
        else -> "Take photo"
    }
    Box(
        Modifier
            .size(80.dp)
            .border(4.dp, Studio.Mannequin, CircleShape)
            .padding(Space.s)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Studio.Mannequin)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Studio.Ink),
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            sessionActive -> Box(
                Modifier
                    .size(24.dp)
                    .background(Studio.Ink, StudioShapes.Small),
            )
            timer.shortLabel != null -> {
                // Inside the fixed shutter graphic: sized in dp so it always fits the disc.
                val size = with(LocalDensity.current) { 18.dp.toSp() }
                Text(
                    timer.shortLabel.orEmpty(),
                    color = Studio.Ink,
                    style = StudioType.numeral(size),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun FlipLensButton(onClick: () -> Unit) {
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(
        turns * 180f,
        tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "flipRotation",
    )
    Box(
        Modifier
            .size(ShutterSideSize)
            .clip(CircleShape)
            .background(Studio.ControlScrim)
            .clickable(role = Role.Button, onClickLabel = "Switch camera") {
                turns += 1f
                onClick()
            }
            .semantics { contentDescription = "Switch camera" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Cameraswitch,
            contentDescription = null,
            tint = Studio.Mannequin,
            modifier = Modifier
                .size(StudioIconSize)
                .graphicsLayer { rotationY = rotation },
        )
    }
}
