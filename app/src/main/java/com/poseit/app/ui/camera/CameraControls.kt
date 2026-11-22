package com.poseit.app.ui.camera

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Grid3x3
import androidx.compose.material.icons.outlined.GridGoldenratio
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.poseit.app.camera.FlashMode
import com.poseit.app.camera.SceneAspect
import com.poseit.app.data.GridType
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.CapsLabel
import com.poseit.app.ui.components.CircleIconButton
import com.poseit.app.ui.components.CircleVariant
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.IconGlyphInset
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PillBarItemSize
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PoseTileSize
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitMenu
import com.poseit.app.ui.components.PoseitMenuDivider
import com.poseit.app.ui.components.PoseitMenuItem
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.components.VerticalHairline
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/*
 * The camera's chrome cards (docs/DESIGN.md, "Screen map", Camera): the top bar card and, at the bottom, the dock,
 * a connected pair of cards (the pose strip fused to the shutter row by the concave necks). On the black surround
 * the cards take the raised card colour, one step up from black, so they read as cards; the pose tiles and the
 * round side controls inside them take the stage colour and sit in them like inset tiles.
 */

/** The camera's card colour: one step up from the black surround (1.4:1), so the cards separate from it. */
internal val CameraCardColor = PoseitColors.backgroundCardRaised

/** Height of the camera's top bar card: shorter than the app's 64dp bar, the viewfinder needs the room. */
internal val CameraBarHeight = 56.dp

/** The dock never stretches wider than this on a tablet; it centres under the preview. */
internal val DockMaxWidth = 600.dp

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

/** Width of a strip tile in the compact dock of a short screen. */
private val CompactStripTileWidth = 48.dp

/**
 * The camera's top bar card: back, then flash, grid, aspect ratio, the timer menu and quick settings as cream glyphs
 * on a dark card 8dp below the status bar and from the sides. Flash and grid cycle through their states and show the
 * current one in their glyph; the timer becomes a cream disc while one is set.
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
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = Space.edge, end = Space.edge, top = Space.edge),
    ) {
        ProvideSurfaceTone(SurfaceTone.Dark) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(CameraBarHeight)
                    .background(CameraCardColor, PoseitShapes.Card)
                    // The first and last glyphs land on the card padding, as in the app's top bar card.
                    .padding(horizontal = Space.cardPadding - IconGlyphInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainIconButton(PoseitIcons.back, "Back", onClick = onBack)
                Spacer(Modifier.weight(1f))
                if (hasFlash) {
                    PlainIconButton(flash.icon(), "Flash ${flash.label()}", onClick = onFlash)
                }
                PlainIconButton(grid.icon(), "Grid: ${grid.label}", onClick = onGrid)
                AspectToggle(aspect, onAspect)
                TimerMenuButton(timer, onTimer, onPhotoshoot)
                PlainIconButton(Icons.Outlined.Tune, "Quick settings", onClick = onQuickSettings)
            }
        }
    }
}

/** The aspect ratio as a small outlined pill ("3:4") in numerals. Tapping cycles the ratios. */
@Composable
private fun AspectToggle(aspect: SceneAspect, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(PoseitShapes.Pill)
            .clickable(role = Role.Button, onClickLabel = "Change aspect ratio", onClick = onClick)
            .semantics { contentDescription = "Aspect ratio ${aspect.label}" }
            .padding(horizontal = Space.xs),
        contentAlignment = Alignment.Center,
    ) {
        // In sp, so it grows with the user's text size like every other label.
        Text(
            aspect.label,
            style = PoseitType.numeral(13.sp, FontWeight.Medium),
            color = PoseitColors.textPrimaryInverted,
            maxLines = 1,
            modifier = Modifier
                .border(1.dp, PoseitColors.borderEmphasizedInverted, PoseitShapes.Pill)
                .padding(horizontal = Space.s + Space.xs / 2, vertical = Space.xs),
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
        // The delay itself is shown on the shutter; the cream disc says a timer is set.
        if (timer == TimerOption.Off) {
            PlainIconButton(icon, description, onClick = { open = true })
        } else {
            CircleIconButton(icon, description, onClick = { open = true }, variant = CircleVariant.Light, size = 40.dp)
        }
        PoseitMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun item(text: String, option: TimerOption, icon: ImageVector) =
                PoseitMenuItem(text, leadingIcon = icon, selected = timer == option) {
                    open = false
                    onTimer(option)
                }
            item("Off", TimerOption.Off, Icons.Outlined.TimerOff)
            TimerOption.DELAYS.forEach { s -> item("$s seconds", TimerOption.Delay(s), Icons.Outlined.Timer) }
            PoseitMenuDivider()
            PoseitMenuItem("Photoshoot…", leadingIcon = Icons.Outlined.BurstMode, selected = timer is TimerOption.Photoshoot) {
                open = false
                onPhotoshoot()
            }
        }
    }
}

/**
 * The dock: the camera's bottom cards, fused into one body by the connected-card necks (pass the pose strip and the
 * shutter row as its two children). Over the black surround it is the camera card colour; [overPreview] makes it
 * translucent where a short screen pushes it over the bottom of the preview, so feet stay visible under it.
 */
@Composable
internal fun CameraDock(overPreview: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ConnectedCardStack(
        modifier
            .widthIn(max = DockMaxWidth)
            .fillMaxWidth(),
        color = if (overPreview) CameraCardColor.copy(alpha = 0.62f) else CameraCardColor,
        content = content,
    )
}

/**
 * Horizontal strip: "All poses", a "None" tile (plain camera), then favourites and the current category, as compact
 * pose tiles inset in the dock's first card. The selected tile gets the lavender outline and is scrolled into view.
 */
@Composable
internal fun PoseStrip(
    poses: List<PoseTemplate>,
    selectedId: String?,
    favorites: Set<String>,
    onSelect: (PoseTemplate?) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val tileWidth = if (compact) CompactStripTileWidth else StripTileWidth
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, poses) {
        val index = if (selectedId == null) 0 else poses.indexOfFirst { it.id == selectedId } + 1
        if (index >= 0) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    ProvideSurfaceTone(SurfaceTone.Dark) {
        Row(
            modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) Space.s else Space.s + Space.xs / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StripActionTile(
                icon = Icons.Outlined.GridView,
                label = "All",
                description = "All poses",
                selected = false,
                onClick = onOpenAll,
                width = tileWidth,
                modifier = Modifier.padding(start = Space.m),
            )
            Spacer(Modifier.width(Space.s))
            VerticalHairline(Modifier.height(tileWidth))
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = Space.s),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
                // A hard margin, not content padding: scrolled tiles clip just inside the card's round corners.
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Space.xs),
            ) {
                item(key = "none") {
                    StripActionTile(
                        icon = Icons.Outlined.Block,
                        label = "None",
                        description = "No pose, plain camera",
                        selected = selectedId == null,
                        onClick = { onSelect(null) },
                        width = tileWidth,
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
                            .width(tileWidth)
                            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Button) { onSelect(pose) }
                            .semantics { contentDescription = pose.name },
                    )
                }
            }
        }
    }
}

/**
 * A strip tile that isn't a pose ("All", "None"): the same inset stage tile, with a cream glyph and a tiny capitals
 * label; the lavender outline when selected.
 */
@Composable
private fun StripActionTile(
    icon: ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = StripTileWidth,
) {
    Box(
        modifier
            .size(width = width, height = width * 4 / 3)
            .clip(PoseitShapes.Control)
            .background(PoseitColors.backgroundStage)
            .then(if (selected) Modifier.border(2.dp, PoseitColors.accent, PoseitShapes.Control) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = PoseitColors.textPrimaryInverted, modifier = Modifier.size(IconSize.standalone))
            Spacer(Modifier.height(Space.s))
            // In sp, so it grows with the user's text size; at the very largest sizes it ends in an ellipsis, and the
            // glyph and the tile's description still say what it does.
            CapsLabel(
                label,
                color = PoseitColors.textSecondaryInverted,
                style = PoseitType.caps.copy(letterSpacing = PoseitType.caps.letterSpacing * 0.5f),
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** Last photo (opens the gallery), the shutter and the lens switch: the dock's second card. */
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
    compact: Boolean = false,
) {
    ProvideSurfaceTone(SurfaceTone.Dark) {
        Row(
            modifier
                .fillMaxWidth()
                .padding(horizontal = Space.l, vertical = if (compact) Space.xs + Space.xs / 2 else Space.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val side = if (compact) 48.dp else ShutterSideSize
            GalleryThumb(lastPhoto, onGallery, side)
            ShutterButton(sessionActive, timer, capturing, onShutter, if (compact) CompactShutterSize else ShutterSize)
            FlipLensButton(onFlip, side)
        }
    }
}

/** Diameter of the round controls beside the shutter: the floating pill bar's circles. */
private val ShutterSideSize: Dp = PillBarItemSize

/** Diameter of the shutter. */
private val ShutterSize: Dp = 72.dp

/** Diameter of the shutter in the compact dock of a short screen. */
private val CompactShutterSize: Dp = 64.dp

@Composable
private fun GalleryThumb(lastPhoto: Uri?, onClick: () -> Unit, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(PoseitShapes.Circle)
            .background(PoseitColors.backgroundCard)
            .then(if (lastPhoto != null) Modifier.border(1.5.dp, PoseitColors.cameraContent, PoseitShapes.Circle) else Modifier)
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
                    .padding(3.dp)
                    .clip(PoseitShapes.Circle),
            )
        } else {
            Icon(PoseitIcons.photos, contentDescription = null, tint = PoseitColors.textPrimaryInverted, modifier = Modifier.size(IconSize.standalone))
        }
    }
}

/**
 * The shutter: a cream ring around a cream disc. It shows the self-timer delay (or the photoshoot count) as a
 * numeral, and an ink stop square while a timer or photoshoot runs.
 */
@Composable
private fun ShutterButton(sessionActive: Boolean, timer: TimerOption, capturing: Boolean, onClick: () -> Unit, size: Dp) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed || capturing) 0.88f else 1f,
        PoseitMotion.standard(PoseitMotion.FAST),
        label = "shutterScale",
    )
    val description = when {
        sessionActive -> if (timer is TimerOption.Photoshoot) "Stop photoshoot" else "Stop timer"
        timer != TimerOption.Off -> "Start timer"
        else -> "Take photo"
    }
    Box(
        Modifier
            .size(size)
            .border(3.dp, PoseitColors.cameraContent, PoseitShapes.Circle)
            .padding(6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PoseitShapes.Circle)
            .background(PoseitColors.cameraContent)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = PoseitColors.textPrimary),
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
                    .size(22.dp)
                    .background(PoseitColors.textPrimary, PoseitShapes.Inner),
            )
            timer.shortLabel != null -> {
                // Inside the fixed shutter graphic: sized in dp so it always fits the disc.
                val size = with(LocalDensity.current) { 18.dp.toSp() }
                Text(
                    timer.shortLabel.orEmpty(),
                    color = PoseitColors.textPrimary,
                    style = PoseitType.numeral(size),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun FlipLensButton(onClick: () -> Unit, size: Dp) {
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(
        turns * 180f,
        PoseitMotion.standard(PoseitMotion.SLOW),
        label = "flipRotation",
    )
    // An inset stage disc like the gallery button; only the glyph turns.
    Box(
        Modifier
            .size(size)
            .clip(PoseitShapes.Circle)
            .background(PoseitColors.backgroundCard)
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
            tint = PoseitColors.textPrimaryInverted,
            modifier = Modifier
                .size(IconSize.standalone)
                .graphicsLayer { rotationY = rotation },
        )
    }
}
