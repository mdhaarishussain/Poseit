package com.poseit.app.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.poseit.app.data.AppSettings
import com.poseit.app.data.COUNTDOWN_CHOICES
import com.poseit.app.data.OverlayStyle
import com.poseit.app.data.countdownChoiceFor
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.StudioChip
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.components.StudioSlider
import com.poseit.app.ui.components.StudioTabs
import com.poseit.app.ui.components.SwitchRow
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Sheets and the dialog opened from the camera. They use the light Studio colours (Sheet background, Ink text):
 * the camera is dark, but everything you read and set is on paper.
 */

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Studio.Sheet,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Space.xl)
                .navigationBarsPadding(),
        ) {
            SheetHeader("Quick settings") { DoneButton(sheetState, onDismiss) }

            GroupHeading("Coaching", first = true)
            SwitchRow(
                title = "Voice guidance",
                subtitle = "Spoken corrections and countdown",
                checked = settings.voiceGuidance,
                onCheckedChange = { v -> onUpdateSettings { it.copy(voiceGuidance = v) } },
            )
            RowDivider()
            SwitchRow(
                title = "Auto-capture",
                subtitle = "Takes the photo when you match the pose",
                checked = settings.autoCapture,
                onCheckedChange = { v -> onUpdateSettings { it.copy(autoCapture = v) } },
            )
            RowDivider()
            var strictness by remember(settings.matchThreshold) {
                mutableFloatStateOf(settings.matchThreshold.coerceIn(STRICT_MIN, STRICT_MAX))
            }
            LabelledSliderRow(
                title = "Match strictness",
                subtitle = "Higher means the pose has to be copied more exactly",
                valueLabel = "${(strictness * 100).roundToInt()}%",
                sliderDescription = "Match strictness",
                value = strictness,
                onValueChange = { strictness = it },
                onValueChangeFinished = {
                    val v = (strictness * 20f).roundToInt() / 20f
                    onUpdateSettings { it.copy(matchThreshold = v) }
                },
                valueRange = STRICT_MIN..STRICT_MAX,
                steps = 6,
            )
            RowDivider()
            Column(Modifier.padding(horizontal = Space.gutter, vertical = Space.m)) {
                Text("Countdown before auto-capture", style = StudioType.bodyStrong, color = Studio.Ink)
                Spacer(Modifier.height(Space.s))
                ChipRow {
                    val selectedCountdown = countdownChoiceFor(settings.countdownSeconds)
                    COUNTDOWN_CHOICES.forEach { s ->
                        CameraChip(if (s == 0) "None" else "${s}s", selectedCountdown == s, onClick = {
                            onUpdateSettings { it.copy(countdownSeconds = s) }
                        })
                    }
                }
            }
            RowDivider()
            SwitchRow(
                title = "Hand-raise trigger",
                subtitle = if (ui.templateRaisesHand) {
                    "Paused for this pose because a hand is at head height"
                } else {
                    "Hold a hand above your head for a moment to start the timer"
                },
                checked = ui.handRaiseEnabled,
                onCheckedChange = onHandRaise,
            )
            RowDivider()
            Text(
                "Hints are for",
                style = StudioType.bodyStrong,
                color = Studio.Ink,
                modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.m, bottom = Space.xs),
            )
            Column(Modifier.selectableGroup()) {
                AudienceMode.entries.forEach { mode ->
                    ChoiceRow(
                        title = mode.label,
                        subtitle = when (mode) {
                            AudienceMode.AUTO -> "Front camera: you're posing. Back camera: you're taking the photo."
                            AudienceMode.SUBJECT -> "“Step left”, “move closer”"
                            AudienceMode.PHOTOGRAPHER -> "“Move the phone left”, “zoom in”"
                        },
                        selected = ui.audienceMode == mode,
                        onClick = { onAudience(mode) },
                    )
                }
            }

            GroupHeading("Pose guide")
            ChipRow(Modifier.padding(horizontal = Space.gutter, vertical = Space.s)) {
                OverlayStyle.entries.forEach { style ->
                    CameraChip(style.label, settings.overlayStyle == style, onClick = { onUpdateSettings { it.copy(overlayStyle = style) } })
                }
            }
            RowDivider()
            var opacity by remember(settings.overlayOpacity) { mutableFloatStateOf(settings.overlayOpacity.coerceIn(0.15f, 1f)) }
            LabelledSliderRow(
                title = "Guide opacity",
                valueLabel = "${(opacity * 100).roundToInt()}%",
                sliderDescription = "Guide opacity",
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = { onUpdateSettings { it.copy(overlayOpacity = opacity) } },
                valueRange = 0.15f..1f,
            )
            RowDivider()
            SwitchRow(
                title = "Live skeleton",
                subtitle = "Shows the body the camera detects as thin lines",
                checked = settings.showLiveSkeleton,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showLiveSkeleton = v) } },
            )
            RowDivider()
            SwitchRow(
                title = "Level",
                subtitle = "Helps you keep the horizon straight",
                checked = settings.showLevel,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showLevel = v) } },
            )

            val hasExposure = !exposureRange.isEmpty() && exposureRange.first < exposureRange.last
            val hasZoom = maxZoom > 1.05f
            if (hasExposure || hasZoom) GroupHeading("Camera")
            if (hasExposure) {
                var exposure by remember(ui.exposureIndex) { mutableFloatStateOf(ui.exposureIndex.toFloat()) }
                val ev = exposure.roundToInt() * exposureStepEv
                LabelledSliderRow(
                    title = "Brightness",
                    valueLabel = String.format(Locale.US, "%+.1f EV", ev),
                    sliderDescription = "Exposure",
                    value = exposure.coerceIn(exposureRange.first.toFloat(), exposureRange.last.toFloat()),
                    onValueChange = {
                        exposure = it
                        onExposure(it.roundToInt())
                    },
                    valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                    steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0),
                )
            }
            if (hasExposure && hasZoom) RowDivider()
            if (hasZoom) {
                var zoom by remember(ui.zoom) { mutableFloatStateOf(ui.zoom) }
                LabelledSliderRow(
                    title = "Zoom",
                    valueLabel = String.format(Locale.US, "%.1fx", zoom),
                    sliderDescription = "Zoom",
                    value = zoom.coerceIn(1f, maxZoom),
                    onValueChange = {
                        zoom = it
                        onZoom(it)
                    },
                    valueRange = 1f..maxZoom,
                    above = {
                        ChipRow {
                            ZOOM_PRESETS.filter { it <= maxZoom + 0.01f }.forEach { preset ->
                                val label = if (preset % 1f == 0f) "${preset.toInt()}x" else "${preset}x"
                                CameraChip(label, abs(zoom - preset) < 0.05f, onClick = {
                                    zoom = preset
                                    onZoom(preset)
                                })
                            }
                        }
                    },
                )
            }
        }
    }
}

private const val STRICT_MIN = 0.60f
private const val STRICT_MAX = 0.95f
private val ZOOM_PRESETS = listOf(1f, 2f, 3f, 5f, 10f)

/**
 * The header bar of a full-height sheet, which behaves like a screen: the title at the top-bar size (Title 18/24,
 * so the group headings below it read as its sections) and an optional action at the end.
 */
@Composable
private fun SheetHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // An action's text lands on the gutter (TertiaryButton has 12dp of its own padding).
            .padding(start = Space.gutter, end = if (action != null) Space.s else Space.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = StudioType.barTitle,
            color = Studio.Ink,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        action?.invoke()
    }
}

/** "Done": closes a sheet with its slide-down. A full-height sheet leaves almost no scrim to tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoneButton(sheetState: SheetState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    TertiaryButton("Done", onClick = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    })
}

/** A group heading on the gutter, 32dp below the previous group (16dp below the sheet header). */
@Composable
private fun GroupHeading(text: String, first: Boolean = false) {
    SectionHeading(
        text,
        Modifier.padding(start = Space.gutter, end = Space.gutter, top = if (first) Space.l else Space.section, bottom = Space.xs),
    )
}

/** Hairline between two rows of one group, inset to the gutters. */
@Composable
private fun RowDivider() = HairlineDivider(Modifier.padding(horizontal = Space.gutter))

/** Chips 8dp apart, wrapping onto more lines when needed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) { content() }
}

/**
 * A [StudioChip] at least 48dp wide, so one-character chips ("3", "2s") fill their own touch target instead of
 * floating in it, and a row of chips keeps even 8dp gaps.
 */
@Composable
private fun CameraChip(label: String, selected: Boolean, onClick: () -> Unit) {
    StudioChip(label, selected, onClick = onClick, modifier = Modifier.widthIn(min = 48.dp))
}

/**
 * A setting row with its value on the right and a [StudioSlider] below, like the shared SliderRow, but the slider
 * also carries its own name ([sliderDescription]) for screen readers, and [above] can hold presets (zoom chips).
 */
@Composable
private fun LabelledSliderRow(
    title: String,
    valueLabel: String,
    sliderDescription: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    subtitle: String? = null,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    above: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = Space.m),
    ) {
        Row {
            // The value sits on the title's baseline, not centred against title and subtitle.
            Column(
                Modifier
                    .weight(1f)
                    .alignByBaseline(),
            ) {
                Text(title, style = StudioType.bodyStrong, color = Studio.Ink)
                if (subtitle != null) Text(subtitle, style = StudioType.label, color = Studio.Graphite)
            }
            Spacer(Modifier.width(Space.l))
            Text(valueLabel, style = StudioType.label, color = Studio.Ink, modifier = Modifier.alignByBaseline())
        }
        if (above != null) {
            Spacer(Modifier.height(Space.s))
            above()
        }
        StudioSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.semantics {
                contentDescription = sliderDescription
                stateDescription = valueLabel
            },
        )
    }
}

/** One option of a single-choice group: the whole row selects it; the radio sits at the end like other controls. */
@Composable
private fun ChoiceRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.gutter, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = StudioType.body, color = Studio.Ink)
            if (subtitle != null) Text(subtitle, style = StudioType.label, color = Studio.Graphite)
        }
        Spacer(Modifier.width(Space.l))
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Studio.Ink, unselectedColor = Studio.Graphite),
        )
    }
}

// ===================================================================== all poses

private sealed interface PoseTab {
    val label: String

    data object Favorites : PoseTab {
        override val label = "Favourites"
    }

    data class Category(val category: PoseCategory) : PoseTab {
        override val label = category.label
    }
}

private fun PoseTab.filter(poses: List<PoseTemplate>, favorites: Set<String>): List<PoseTemplate> = when (this) {
    PoseTab.Favorites -> poses.filter { it.id in favorites }
    is PoseTab.Category -> poses.filter { it.stripCategory == category }
}

/** Full library browser: category tabs (plus favourites and the user's own poses) and a grid of pose tiles. */
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
        containerColor = Studio.Sheet,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            SheetHeader("Choose a pose")
            if (tabs.isEmpty()) {
                Text(
                    "No poses available.",
                    style = StudioType.body,
                    color = Studio.Graphite,
                    modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.xl),
                )
                return@Column
            }
            val current = tabs[tabIndex.coerceIn(0, tabs.lastIndex)]
            StudioTabs(
                labels = tabs.map { it.label },
                selectedIndex = tabIndex.coerceIn(0, tabs.lastIndex),
                onSelect = { selectedTab = tabs[it] },
                edgePadding = Space.gutter,
                modifier = Modifier.padding(top = Space.s),
            )
            val items = remember(current, poses, favorites) { current.filter(poses, favorites) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.l, bottom = Space.section),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
                verticalArrangement = Arrangement.spacedBy(Space.l),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                items(items, key = { it.id }) { pose ->
                    val isSelected = pose.id == selected?.id
                    PoseTile(
                        pose = pose,
                        // The tab already names the category, except on the mixed favourites tab.
                        detail = when {
                            current == PoseTab.Favorites -> pose.stripCategory.label
                            pose.personCount > 1 -> "${pose.personCount} people"
                            else -> null
                        },
                        isFavorite = pose.id in favorites,
                        onToggleFavorite = { onToggleFavorite(pose.id) },
                        selected = isSelected,
                        onClick = { onSelect(pose) },
                        onClickLabel = "Use this pose",
                        modifier = Modifier.semantics {
                            role = Role.Button
                            this.selected = isSelected
                        },
                    )
                }
            }
        }
    }
}

// ===================================================================== tips

private fun difficultyLabel(level: Int): String = when {
    level <= 1 -> "Easy"
    level == 2 -> "Medium"
    else -> "Advanced"
}

/** "Standing, easy" / "Couple, medium, 2 people": the pose's facts as one short phrase. */
private fun PoseTemplate.factsLine(): String = buildList {
    add(category.label)
    add(difficultyLabel(difficulty).lowercase(Locale.getDefault()))
    if (personCount > 1) add("$personCount people")
}.joinToString(", ")

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase(Locale.getDefault()) }

/** Description, posing tips and camera advice of the current pose. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PoseTipsSheet(
    pose: PoseTemplate,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Studio.Sheet, modifier = Modifier.statusBarsPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = Space.xl)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(pose.name, style = StudioType.display, color = Studio.Ink, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(Space.xs))
                    Text(pose.factsLine(), style = StudioType.body, color = Studio.Graphite)
                }
                Spacer(Modifier.width(Space.s))
                StudioIconButton(
                    icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (favorite) "Remove ${pose.name} from favourites" else "Add ${pose.name} to favourites",
                    onClick = onToggleFavorite,
                    // The glyph, not the touch target, lines up with the gutter (and the name's cap height).
                    modifier = Modifier.offset(x = (48.dp - StudioIconSize) / 2, y = -Space.s),
                )
            }
            if (pose.description.isNotBlank()) {
                Spacer(Modifier.height(Space.l))
                Text(pose.description, style = StudioType.body, color = Studio.Ink)
            }
            if (pose.tips.isNotEmpty()) {
                TipsHeading("How to pose")
                pose.tips.forEach { tip ->
                    Row(Modifier.padding(vertical = Space.xs)) {
                        Text("•", style = StudioType.body, color = Studio.Ink, modifier = Modifier.width(Space.l))
                        Text(tip, style = StudioType.body, color = Studio.Ink)
                    }
                }
            }
            TipsHeading("Camera")
            Text(
                "${pose.camera.height.label}, ${pose.camera.shot.label.lowercaseFirst()}",
                style = StudioType.bodyStrong,
                color = Studio.Ink,
            )
            Spacer(Modifier.height(Space.xs))
            Text(pose.camera.height.advice, style = StudioType.body, color = Studio.Ink)
            if (pose.camera.landscape) {
                Spacer(Modifier.height(Space.s))
                Text("Wide shot: keep the phone upright and step back so everyone fits.", style = StudioType.body, color = Studio.Ink)
            }
            pose.camera.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(Space.s))
                Text(note, style = StudioType.body, color = Studio.Graphite)
            }
            // The guide gestures are otherwise only described to screen readers.
            TipsHeading("Guide")
            Text(
                "Drag the guide to move it, pinch to resize it and twist to rotate it. Double-tap to reset it. " +
                    "Tap the padlock to keep it in place; pinching then zooms the camera. Zoom is also in Quick settings.",
                style = StudioType.body,
                color = Studio.Ink,
            )
        }
    }
}

@Composable
private fun TipsHeading(text: String) {
    SectionHeading(text, Modifier.padding(top = Space.section, bottom = Space.s))
}

// ===================================================================== photoshoot

/** Choose the number of photos and the interval of a hands-free photoshoot. */
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
        shape = StudioShapes.Medium,
        containerColor = Studio.Sheet,
        titleContentColor = Studio.Ink,
        textContentColor = Studio.Ink,
        tonalElevation = 0.dp,
        title = { Text("Photoshoot", style = StudioType.title) },
        text = {
            Column {
                Text("Number of photos", style = StudioType.bodyStrong)
                Spacer(Modifier.height(Space.s))
                ChipRow {
                    TimerOption.SHOT_CHOICES.forEach { n ->
                        CameraChip(if (n == 0) "Until I stop" else "$n", shots == n, onClick = { shots = n })
                    }
                }
                Spacer(Modifier.height(Space.l))
                Text("Time between photos", style = StudioType.bodyStrong)
                Spacer(Modifier.height(Space.s))
                ChipRow {
                    TimerOption.INTERVAL_CHOICES.forEach { s ->
                        CameraChip("${s}s", interval == s, onClick = { interval = s })
                    }
                }
                Spacer(Modifier.height(Space.l))
                Text(
                    "Press the shutter or a volume button to start. The first photo is taken after " +
                        "${TimerOption.PHOTOSHOOT_START_DELAY_SECONDS} seconds. Press stop to end early.",
                    style = StudioType.label,
                    color = Studio.Graphite,
                )
            }
        },
        confirmButton = {
            PrimaryButton("Set photoshoot", onClick = { onConfirm(TimerOption.Photoshoot(shots, interval)) })
        },
        dismissButton = {
            TertiaryButton("Cancel", onClick = onDismiss)
        },
    )
}
