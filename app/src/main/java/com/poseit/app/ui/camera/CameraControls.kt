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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridGoldenratio
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.poseit.app.camera.FlashMode
import com.poseit.app.camera.SceneAspect
import com.poseit.app.data.GridType
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.theme.PoseitColors

private fun FlashMode.icon(): ImageVector = when (this) {
    FlashMode.OFF -> Icons.Filled.FlashOff
    FlashMode.AUTO -> Icons.Filled.FlashAuto
    FlashMode.ON -> Icons.Filled.FlashOn
}

private fun FlashMode.label(): String = when (this) {
    FlashMode.OFF -> "off"
    FlashMode.AUTO -> "auto"
    FlashMode.ON -> "on"
}

private fun GridType.icon(): ImageVector = when (this) {
    GridType.NONE -> Icons.Filled.GridOff
    GridType.THIRDS -> Icons.Filled.Grid3x3
    GridType.GOLDEN -> Icons.Filled.GridGoldenratio
    GridType.CENTER -> Icons.Filled.CenterFocusStrong
}

internal fun SceneAspect.next(): SceneAspect = SceneAspect.entries[(ordinal + 1) % SceneAspect.entries.size]

/** Back, flash, grid, aspect ratio, timer menu and quick settings. */
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
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Row(
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            if (hasFlash) {
                IconButton(onClick = onFlash) {
                    Icon(flash.icon(), contentDescription = "Flash ${flash.label()}")
                }
            }
            IconButton(onClick = onGrid) {
                Icon(grid.icon(), contentDescription = "Grid: ${grid.label}")
            }
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = "Change aspect ratio", onClick = onAspect)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .semantics { contentDescription = "Aspect ratio ${aspect.label}" },
            ) {
                Text(aspect.label, style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            TimerMenuButton(timer, onTimer, onPhotoshoot)
            IconButton(onClick = onQuickSettings) {
                Icon(Icons.Filled.Tune, contentDescription = "Quick settings")
            }
        }
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
    Box {
        IconButton(onClick = { open = true }) {
            val icon = when (timer) {
                TimerOption.Off -> Icons.Filled.TimerOff
                is TimerOption.Delay -> Icons.Filled.Timer
                is TimerOption.Photoshoot -> Icons.Filled.BurstMode
            }
            val label = timer.shortLabel
            if (label == null) {
                Icon(icon, contentDescription = description)
            } else {
                BadgedBox(badge = { Badge(containerColor = PoseitColors.Violet, contentColor = Color.White) { Text(label) } }) {
                    Icon(icon, contentDescription = description)
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun item(text: String, option: TimerOption, icon: ImageVector) = DropdownMenuItem(
                text = { Text(text) },
                leadingIcon = { Icon(icon, contentDescription = null) },
                trailingIcon = { if (timer == option) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                onClick = {
                    open = false
                    onTimer(option)
                },
            )
            item("Off", TimerOption.Off, Icons.Filled.TimerOff)
            TimerOption.DELAYS.forEach { s -> item("$s seconds", TimerOption.Delay(s), Icons.Filled.Timer) }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Photoshoot…") },
                leadingIcon = { Icon(Icons.Filled.BurstMode, contentDescription = null) },
                trailingIcon = { if (timer is TimerOption.Photoshoot) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                onClick = {
                    open = false
                    onPhotoshoot()
                },
            )
        }
    }
}

/**
 * Horizontal strip: "All poses" button, a "None" item (plain camera), then favourites and the current
 * category. The selected pose is highlighted and scrolled into view.
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
        StripTile(
            selected = false,
            onClick = onOpenAll,
            description = "All poses",
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.GridView, contentDescription = null, tint = Color.White)
                Text("All", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "none") {
                StripTile(selected = selectedId == null, onClick = { onSelect(null) }, description = "No pose, plain camera") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = PoseitColors.OnSurfaceMuted)
                        Text("None", color = PoseitColors.OnSurfaceMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(poses, key = { it.id }) { pose ->
                StripTile(selected = pose.id == selectedId, onClick = { onSelect(pose) }, description = pose.name) {
                    PoseThumbnail(pose, Modifier.fillMaxSize(), showGround = false)
                    if (pose.id in favorites) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = PoseitColors.Amber,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StripTile(
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .size(width = 54.dp, height = 72.dp)
            .clip(shape)
            .background(PoseitColors.SurfaceHigh.copy(alpha = 0.85f))
            .border(if (selected) 2.5.dp else 1.dp, if (selected) PoseitColors.Violet else Color.White.copy(alpha = 0.12f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
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
            .padding(horizontal = 36.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryThumb(lastPhoto, onGallery)
        ShutterButton(sessionActive, timer, capturing, onShutter)
        FlipLensButton(onFlip)
    }
}

@Composable
private fun GalleryThumb(lastPhoto: Uri?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(2.dp, Color.White, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Open gallery", onClick = onClick)
            .semantics { contentDescription = "Open gallery" },
        contentAlignment = Alignment.Center,
    ) {
        if (lastPhoto != null) {
            AsyncImage(
                model = lastPhoto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ShutterButton(sessionActive: Boolean, timer: TimerOption, capturing: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed || capturing) 0.88f else 1f, label = "shutterScale")
    val description = when {
        sessionActive -> "Stop timer"
        timer != TimerOption.Off -> "Start timer"
        else -> "Take photo"
    }
    Box(
        Modifier
            .size(80.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (sessionActive) Color.White.copy(alpha = 0.9f) else Color.White)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = PoseitColors.Violet),
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
                    .background(PoseitColors.Red, RoundedCornerShape(6.dp)),
            )
            timer.shortLabel != null -> Text(
                timer.shortLabel.orEmpty(),
                color = PoseitColors.Background,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun FlipLensButton(onClick: () -> Unit) {
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 180f, label = "flipRotation")
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClickLabel = "Switch camera") {
                turns += 1f
                onClick()
            }
            .semantics { contentDescription = "Switch camera" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Cameraswitch,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer { rotationY = rotation },
        )
    }
}
