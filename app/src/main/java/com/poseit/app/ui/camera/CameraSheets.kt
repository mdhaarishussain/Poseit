package com.poseit.app.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.data.AppSettings
import com.poseit.app.data.COUNTDOWN_CHOICES
import com.poseit.app.data.OverlayStyle
import com.poseit.app.data.countdownChoiceFor
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.theme.PoseitColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Poses the user made (from a photo or in the editor). */
internal val PoseTemplate.isCustom: Boolean get() = source != PoseSource.BUILT_IN || category == PoseCategory.CUSTOM

/** Strip category of a pose: custom poses are grouped under "My Poses". */
internal val PoseTemplate.stripCategory: PoseCategory get() = if (isCustom) PoseCategory.CUSTOM else category

// ===================================================================== quick settings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun QuickSettingsSheet(
    settings: AppSettings,
    ui: CameraUiState,
    maxZoom: Float,
    exposureRange: IntRange,
    exposureStepEv: Float,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onAudience: (AudienceMode) -> Unit,
    onHandRaise: (Boolean) -> Unit,
    onZoom: (Float) -> Unit,
    onExposure: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PoseitColors.Surface,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text("Quick settings", style = MaterialTheme.typography.titleLarge)

            SectionTitle("Coaching")
            SwitchRow("Voice guidance", "Spoken corrections and countdown", settings.voiceGuidance) { v ->
                onUpdateSettings { it.copy(voiceGuidance = v) }
            }
            SwitchRow("Auto-capture", "Takes the photo when you match the pose", settings.autoCapture) { v ->
                onUpdateSettings { it.copy(autoCapture = v) }
            }
            var strictness by remember(settings.matchThreshold) {
                mutableFloatStateOf(settings.matchThreshold.coerceIn(STRICT_MIN, STRICT_MAX))
            }
            SliderRow(
                title = "Match strictness",
                value = "${(strictness * 100).roundToInt()}%",
                subtitle = "Higher means the pose has to be copied more exactly",
            ) {
                Slider(
                    value = strictness,
                    onValueChange = { strictness = it },
                    onValueChangeFinished = {
                        val v = (strictness * 20f).roundToInt() / 20f
                        onUpdateSettings { it.copy(matchThreshold = v) }
                    },
                    valueRange = STRICT_MIN..STRICT_MAX,
                    steps = 6,
                    modifier = Modifier.semantics { contentDescription = "Match strictness" },
                )
            }
            Text("Countdown before auto-capture", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                val selectedCountdown = countdownChoiceFor(settings.countdownSeconds)
                COUNTDOWN_CHOICES.forEach { s ->
                    ChoiceChip(if (s == 0) "None" else "${s}s", selectedCountdown == s) {
                        onUpdateSettings { it.copy(countdownSeconds = s) }
                    }
                }
            }
            SwitchRow(
                "Hand-raise trigger",
                if (ui.templateRaisesHand) {
                    "Paused for this pose because a hand is at head height"
                } else {
                    "Hold a hand above your head for a moment to start the timer"
                },
                ui.handRaiseEnabled,
                onHandRaise,
            )
            Text("Hints are for", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            Column(Modifier.selectableGroup()) {
                AudienceMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .selectable(selected = ui.audienceMode == mode, role = Role.RadioButton) { onAudience(mode) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = ui.audienceMode == mode, onClick = null, modifier = Modifier.padding(8.dp))
                        Column {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            val sub = when (mode) {
                                AudienceMode.AUTO -> "Front camera: you're posing. Back camera: you're taking the photo."
                                AudienceMode.SUBJECT -> "\"Step left\", \"move closer\""
                                AudienceMode.PHOTOGRAPHER -> "\"Move the phone left\", \"zoom in\""
                            }
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = PoseitColors.OnSurfaceMuted)
                        }
                    }
                }
            }

            SectionTitle("Pose guide")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                OverlayStyle.entries.forEachIndexed { i, style ->
                    SegmentedButton(
                        selected = settings.overlayStyle == style,
                        onClick = { onUpdateSettings { it.copy(overlayStyle = style) } },
                        shape = SegmentedButtonDefaults.itemShape(i, OverlayStyle.entries.size),
                    ) {
                        Text(style.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            var opacity by remember(settings.overlayOpacity) { mutableFloatStateOf(settings.overlayOpacity.coerceIn(0.15f, 1f)) }
            SliderRow(title = "Guide opacity", value = "${(opacity * 100).roundToInt()}%") {
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    onValueChangeFinished = { onUpdateSettings { it.copy(overlayOpacity = opacity) } },
                    valueRange = 0.15f..1f,
                    modifier = Modifier.semantics { contentDescription = "Guide opacity" },
                )
            }
            SwitchRow("Live skeleton", "Shows the body the camera detects as thin lines", settings.showLiveSkeleton) { v ->
                onUpdateSettings { it.copy(showLiveSkeleton = v) }
            }
            SwitchRow("Level", "Horizon line that turns green when the phone is straight", settings.showLevel) { v ->
                onUpdateSettings { it.copy(showLevel = v) }
            }

            val hasExposure = !exposureRange.isEmpty() && exposureRange.first < exposureRange.last
            val hasZoom = maxZoom > 1.05f
            if (hasExposure || hasZoom) SectionTitle("Camera")
            if (hasExposure) {
                var exposure by remember(ui.exposureIndex) { mutableFloatStateOf(ui.exposureIndex.toFloat()) }
                val ev = exposure.roundToInt() * exposureStepEv
                SliderRow(title = "Brightness", value = String.format(Locale.US, "%+.1f EV", ev)) {
                    Slider(
                        value = exposure.coerceIn(exposureRange.first.toFloat(), exposureRange.last.toFloat()),
                        onValueChange = {
                            exposure = it
                            onExposure(it.roundToInt())
                        },
                        valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                        steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0),
                        modifier = Modifier.semantics { contentDescription = "Exposure" },
                    )
                }
            }
            if (hasZoom) {
                var zoom by remember(ui.zoom) { mutableFloatStateOf(ui.zoom) }
                SliderRow(title = "Zoom", value = String.format(Locale.US, "%.1fx", zoom)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZOOM_PRESETS.filter { it <= maxZoom + 0.01f }.forEach { preset ->
                            val label = if (preset % 1f == 0f) "${preset.toInt()}x" else "${preset}x"
                            ChoiceChip(label, abs(zoom - preset) < 0.05f) {
                                zoom = preset
                                onZoom(preset)
                            }
                        }
                    }
                    Slider(
                        value = zoom.coerceIn(1f, maxZoom),
                        onValueChange = {
                            zoom = it
                            onZoom(it)
                        },
                        valueRange = 1f..maxZoom,
                        modifier = Modifier.semantics { contentDescription = "Zoom" },
                    )
                }
            }
        }
    }
}

private const val STRICT_MIN = 0.60f
private const val STRICT_MAX = 0.95f
private val ZOOM_PRESETS = listOf(1f, 2f, 3f, 5f, 10f)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium,
        color = PoseitColors.Violet,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PoseitColors.OnSurfaceMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SliderRow(title: String, value: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = PoseitColors.Violet)
        }
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PoseitColors.OnSurfaceMuted)
        content()
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PoseitColors.Violet,
            selectedLabelColor = Color.White,
        ),
    )
}

// ===================================================================== all poses

private sealed interface PoseTab {
    val label: String

    data object Favorites : PoseTab {
        // A heart, as in the library (the star is the "My Poses" category's emoji).
        override val label = "♥ Favourites"
    }

    data class Category(val category: PoseCategory) : PoseTab {
        override val label = "${category.emoji} ${category.label}"
    }
}

private fun PoseTab.filter(poses: List<PoseTemplate>, favorites: Set<String>): List<PoseTemplate> = when (this) {
    PoseTab.Favorites -> poses.filter { it.id in favorites }
    is PoseTab.Category -> poses.filter { it.stripCategory == category }
}

/** Full library browser: category tabs (plus favourites and the user's own poses) and a grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AllPosesSheet(
    poses: List<PoseTemplate>,
    favorites: Set<String>,
    selected: PoseTemplate?,
    onSelect: (PoseTemplate) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tabs = remember(poses, favorites) {
        buildList {
            if (poses.any { it.id in favorites }) add(PoseTab.Favorites)
            if (poses.any { it.isCustom }) add(PoseTab.Category(PoseCategory.CUSTOM))
            PoseCategory.entries
                .filter { c -> c != PoseCategory.CUSTOM && poses.any { it.stripCategory == c } }
                .forEach { add(PoseTab.Category(it)) }
        }
    }
    // Remember the tab itself (not its index) so adding the first favourite doesn't shift the selection.
    var selectedTab by remember {
        mutableStateOf<PoseTab?>(selected?.let { PoseTab.Category(it.stripCategory) })
    }
    val tabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PoseitColors.Surface,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("Choose a pose", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
            if (tabs.isEmpty()) {
                Text(
                    "No poses available.",
                    color = PoseitColors.OnSurfaceMuted,
                    modifier = Modifier.padding(20.dp),
                )
                return@Column
            }
            val current = tabs[tabIndex.coerceIn(0, tabs.lastIndex)]
            ScrollableTabRow(
                selectedTabIndex = tabIndex.coerceIn(0, tabs.lastIndex),
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                tabs.forEachIndexed { i, tab ->
                    Tab(selected = i == tabIndex, onClick = { selectedTab = tab }, text = { Text(tab.label, maxLines = 1) })
                }
            }
            val items = remember(current, poses, favorites) { current.filter(poses, favorites) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                items(items, key = { it.id }) { pose ->
                    PoseCard(
                        pose = pose,
                        selected = pose.id == selected?.id,
                        favorite = pose.id in favorites,
                        onClick = { onSelect(pose) },
                        onToggleFavorite = { onToggleFavorite(pose.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PoseCard(
    pose: PoseTemplate,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(shape)
                .border(if (selected) BorderStroke(2.5.dp, PoseitColors.Violet) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
                .clickable(role = Role.Button, onClickLabel = "Use this pose", onClick = onClick)
                .semantics { contentDescription = pose.name },
        ) {
            PoseThumbnail(pose, Modifier.fillMaxSize())
            IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                    tint = if (favorite) PoseitColors.Coral else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp),
                )
            }
            if (pose.personCount > 1) {
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${pose.personCount}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        Text(
            pose.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp),
        )
    }
}

// ===================================================================== tips

private fun difficultyLabel(level: Int): String = when {
    level <= 1 -> "Easy"
    level == 2 -> "Medium"
    else -> "Advanced"
}

/** Description, posing tips and camera advice of the current pose. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PoseTipsSheet(
    pose: PoseTemplate,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PoseitColors.Surface, modifier = Modifier.statusBarsPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                PoseThumbnail(
                    pose,
                    Modifier
                        .size(width = 84.dp, height = 112.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(pose.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    val people = if (pose.personCount > 1) " · ${pose.personCount} people" else ""
                    Text(
                        "${pose.category.emoji} ${pose.category.label} · ${difficultyLabel(pose.difficulty)}$people",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PoseitColors.OnSurfaceMuted,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (favorite) "Remove from favourites" else "Add to favourites",
                        tint = if (favorite) PoseitColors.Coral else Color.White,
                    )
                }
            }
            if (pose.description.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(pose.description, style = MaterialTheme.typography.bodyLarge)
            }
            if (pose.tips.isNotEmpty()) {
                TipsHeader("How to pose")
                pose.tips.forEach { tip ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•", color = PoseitColors.Violet, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                        Text(tip, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            TipsHeader("Camera")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = PoseitColors.Violet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${pose.camera.height.label} · ${pose.camera.shot.label}", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(pose.camera.height.advice, style = MaterialTheme.typography.bodyMedium)
            if (pose.camera.landscape) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null, tint = PoseitColors.Amber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Wide shot: keep the phone upright and step back so everyone fits.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            pose.camera.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(10.dp))
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoseitColors.OnSurfaceMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PoseitColors.SurfaceHigh, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
            }
            // The guide gestures are otherwise only described to screen readers.
            TipsHeader("Guide")
            Text(
                "Drag to move · pinch to resize · twist to rotate · double-tap to reset. Tap the padlock to keep it " +
                    "in place (pinching then zooms the camera). Zoom is also in Quick settings.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TipsHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
}

// ===================================================================== photoshoot

/** Choose the number of photos and the interval of a hands-free photoshoot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhotoshootDialog(
    initial: TimerOption.Photoshoot?,
    onConfirm: (TimerOption.Photoshoot) -> Unit,
    onDismiss: () -> Unit,
) {
    var shots by remember { mutableIntStateOf(initial?.shots ?: 5) }
    var interval by remember { mutableIntStateOf(initial?.intervalSeconds ?: 3) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
        title = { Text("Photoshoot") },
        text = {
            Column {
                Text("Number of photos", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    TimerOption.SHOT_CHOICES.forEach { n ->
                        ChoiceChip(if (n == 0) "Until I stop" else "$n", shots == n) { shots = n }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Time between photos", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    TimerOption.INTERVAL_CHOICES.forEach { s ->
                        ChoiceChip("${s}s", interval == s) { interval = s }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Press the shutter (or a volume button) to start. The first photo is taken after " +
                        "${TimerOption.PHOTOSHOOT_START_DELAY_SECONDS} seconds; press stop to end early.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PoseitColors.OnSurfaceMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(TimerOption.Photoshoot(shots, interval)) }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
