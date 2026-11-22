package com.poseit.app.ui.camera

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import com.poseit.app.ui.components.SegmentedPill
import androidx.compose.ui.text.font.FontWeight
import com.poseit.app.ui.theme.PoseitRadius
import com.poseit.app.ui.components.selectedRing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poseit.app.data.AppSettings
import com.poseit.app.data.OverlayStyle
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.CameraSetup
import com.poseit.app.ui.components.FilterPill
import com.poseit.app.ui.components.IconGlyphInset
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.PhotoCompanionCard
import com.poseit.app.ui.components.PillTabs
import com.poseit.app.ui.components.rememberPosePhoto
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseSteps
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PoseitAlertDialog
import com.poseit.app.ui.components.PoseitBottomSheet
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.RadioRow
import com.poseit.app.ui.components.RowGroup
import com.poseit.app.ui.components.RowPadding
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.SliderRow
import com.poseit.app.ui.components.SwitchRow
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.library.factsPhrase
import com.poseit.app.ui.settings.CountdownChips
import com.poseit.app.ui.settings.SettingCopy
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Sheets and the dialog opened from the camera. They are cream (PoseitBottomSheet, PoseitAlertDialog): the camera is
 * dark, but everything you read and set is on the light cards. Settings sit in outlined row groups 8dp from the
 * sheet's sides with their headings on the 32dp text inset, the same layout as the Settings screen.
 */

/** Poses the user made (from a photo or in the editor). */
internal val PoseTemplate.isCustom: Boolean get() = source != PoseSource.BUILT_IN || category == PoseCategory.CUSTOM

/** Strip category of a pose: custom poses are grouped under "My Poses". */
internal val PoseTemplate.stripCategory: PoseCategory get() = if (isCustom) PoseCategory.CUSTOM else category

// ===================================================================== quick settings

@OptIn(ExperimentalMaterial3Api::class)
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
    PoseitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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

            GroupHeading(SettingCopy.COACHING, first = true)
            SheetGroup {
                SwitchRow(
                    title = SettingCopy.VOICE,
                    subtitle = SettingCopy.VOICE_SUBTITLE,
                    checked = settings.voiceGuidance,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(voiceGuidance = v) } },
                )
                SwitchRow(
                    title = SettingCopy.AUTO_CAPTURE,
                    subtitle = SettingCopy.AUTO_CAPTURE_SUBTITLE,
                    checked = settings.autoCapture,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(autoCapture = v) } },
                )
                var strictness by remember(settings.matchThreshold) {
                    mutableFloatStateOf(settings.matchThreshold.coerceIn(SettingCopy.STRICTNESS_MIN, SettingCopy.STRICTNESS_MAX))
                }
                SliderRow(
                    title = SettingCopy.STRICTNESS,
                    subtitle = SettingCopy.strictnessHint(strictness),
                    valueLabel = SettingCopy.strictnessValue(strictness),
                    value = strictness,
                    onValueChange = { strictness = it },
                    onValueChangeFinished = {
                        val v = SettingCopy.snapStrictness(strictness)
                        onUpdateSettings { it.copy(matchThreshold = v) }
                    },
                    valueRange = SettingCopy.STRICTNESS_MIN..SettingCopy.STRICTNESS_MAX,
                    steps = 6,
                )
                CountdownRow(
                    seconds = settings.countdownSeconds,
                    onSelect = { s -> onUpdateSettings { it.copy(countdownSeconds = s) } },
                )
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
            }

            GroupHeading("Hints are for")
            SheetGroup(Modifier.selectableGroup()) {
                AudienceMode.entries.forEach { mode ->
                    RadioRow(
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

            GroupHeading(SettingCopy.GUIDE)
            SheetGroup {
                GuideStyleRow(
                    pose = ui.pose?.takeIf { it.figures.isNotEmpty() } ?: BuiltInPoses.all.first(),
                    current = settings.overlayStyle,
                    onSelect = { style -> onUpdateSettings { it.copy(overlayStyle = style) } },
                )
                var opacity by remember(settings.overlayOpacity) {
                    mutableFloatStateOf(settings.overlayOpacity.coerceIn(SettingCopy.OPACITY_MIN, 1f))
                }
                SliderRow(
                    title = SettingCopy.GUIDE_OPACITY,
                    valueLabel = "${(opacity * 100).roundToInt()}%",
                    value = opacity,
                    onValueChange = { opacity = it },
                    onValueChangeFinished = { onUpdateSettings { it.copy(overlayOpacity = opacity) } },
                    valueRange = SettingCopy.OPACITY_MIN..1f,
                )
                SwitchRow(
                    title = SettingCopy.LIVE_SKELETON,
                    subtitle = SettingCopy.LIVE_SKELETON_SUBTITLE,
                    checked = settings.showLiveSkeleton,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showLiveSkeleton = v) } },
                )
                SwitchRow(
                    title = SettingCopy.LEVEL,
                    subtitle = SettingCopy.LEVEL_SUBTITLE,
                    checked = settings.showLevel,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(showLevel = v) } },
                )
            }

            val hasExposure = !exposureRange.isEmpty() && exposureRange.first < exposureRange.last
            val hasZoom = maxZoom > 1.05f
            if (hasExposure || hasZoom) {
                GroupHeading("Camera")
                SheetGroup {
                    if (hasExposure) {
                        var exposure by remember(ui.exposureIndex) { mutableFloatStateOf(ui.exposureIndex.toFloat()) }
                        val ev = exposure.roundToInt() * exposureStepEv
                        SliderRow(
                            title = "Brightness",
                            valueLabel = String.format(Locale.US, "%+.1f", ev),
                            value = exposure.coerceIn(exposureRange.first.toFloat(), exposureRange.last.toFloat()),
                            onValueChange = {
                                exposure = it
                                onExposure(it.roundToInt())
                            },
                            valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                            steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0),
                        )
                    }
                    if (hasZoom) {
                        var zoom by remember(ui.zoom) { mutableFloatStateOf(ui.zoom) }
                        SliderRow(
                            title = "Zoom",
                            valueLabel = String.format(Locale.US, "%.1fx", zoom),
                            value = zoom.coerceIn(1f, maxZoom),
                            onValueChange = {
                                zoom = it
                                onZoom(it)
                            },
                            valueRange = 1f..maxZoom,
                            above = {
                                PillRow {
                                    ZOOM_PRESETS.filter { it <= maxZoom + 0.01f }.forEach { preset ->
                                        val label = if (preset % 1f == 0f) "${preset.toInt()}x" else "${preset}x"
                                        CameraPill(label, abs(zoom - preset) < 0.05f, onClick = {
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
    }
}

private val ZOOM_PRESETS = listOf(1f, 2f, 3f, 5f, 10f)

/** The header of a sheet: its title in the Title style on the text inset, and an optional action at the end. */
@Composable
private fun SheetHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // An action's text lands on the inset (TertiaryButton has 12dp of its own padding).
            .padding(start = Space.textInset, end = if (action != null) Space.textInset - Space.m else Space.textInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = PoseitType.title,
            color = PoseitColors.textPrimary,
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

/** A group heading on the text inset, [Space.section] below the previous group. */
@Composable
private fun GroupHeading(text: String, first: Boolean = false) {
    SectionHeading(
        text,
        Modifier.padding(start = Space.textInset, end = Space.textInset, top = if (first) Space.s else Space.section, bottom = Space.s),
    )
}

/** An outlined row group 8dp from the sheet's sides: each direct child is one row. */
@Composable
private fun SheetGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    RowGroup(modifier.padding(horizontal = Space.edge), content = content)
}

/** Filter pills 8dp apart, wrapping onto more lines when needed (at large font sizes). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PillRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) { content() }
}

/**
 * A [FilterPill] at least 48dp wide, so one-character pills ("3", "2x") fill their own touch target instead of
 * floating in it, and a row of pills keeps even 8dp gaps.
 */
@Composable
private fun CameraPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterPill(label, selected, onClick = onClick, modifier = Modifier.widthIn(min = 48.dp))
}

/** The countdown: title, subtitle and the choices as one segmented pill. */
@Composable
private fun CountdownRow(seconds: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(RowPadding),
    ) {
        Text(SettingCopy.COUNTDOWN, style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
        Text(SettingCopy.COUNTDOWN_SUBTITLE, style = PoseitType.supporting, color = PoseitColors.textSecondary)
        Spacer(Modifier.height(Space.m))
        CountdownChips(seconds = seconds, onSelect = onSelect)
    }
}

/**
 * The guide style as three small stage tiles, each drawing the current pose in that style: the chosen one takes
 * the lavender outline and an ink label (the others are slate), and is announced as selected.
 */
@Composable
private fun GuideStyleRow(pose: PoseTemplate, current: OverlayStyle, onSelect: (OverlayStyle) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(RowPadding),
    ) {
        Text("Guide style", style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
        Spacer(Modifier.height(Space.m))
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            OverlayStyle.entries.forEach { style ->
                val selected = style == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(PoseitShapes.Control)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(style) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // The chosen style: a lavender ring inside the dark tile (on the cream sheet a ring at the edge
                    // would be a faint halo) and its name in Medium ink.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .clip(PoseitShapes.Control)
                            .background(PoseitColors.backgroundStage)
                            .selectedRing(selected, PoseitRadius.control),
                    ) {
                        PoseThumbnail(
                            template = pose,
                            style = style.toFigureStyle(),
                            background = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Space.s),
                        )
                    }
                    Text(
                        style.label,
                        style = if (selected) PoseitType.supporting.copy(fontWeight = FontWeight.Medium) else PoseitType.supporting,
                        color = if (selected) PoseitColors.textPrimary else PoseitColors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(top = Space.s, bottom = Space.xs),
                    )
                }
            }
        }
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
    PoseitBottomSheet(
        onDismissRequest = onDismiss,
        // Full-height sheets would otherwise slide under the status bar (M3 1.3 only insets the bottom).
        modifier = Modifier.statusBarsPadding(),
    ) {
        SheetHeader("All poses")
        if (tabs.isEmpty()) {
            Text(
                "No poses available",
                style = PoseitType.body,
                color = PoseitColors.textSecondary,
                modifier = Modifier.padding(horizontal = Space.textInset, vertical = Space.xl),
            )
        } else {
            val current = tabs[tabIndex.coerceIn(0, tabs.lastIndex)]
            // The same filter pills as the library's category row: one is always chosen, and the row scrolls cleanly.
            LazyRow(
                modifier = Modifier
                    .padding(top = Space.s)
                    .selectableGroup(),
                contentPadding = PaddingValues(horizontal = Space.edge),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                itemsIndexed(tabs, key = { _, tab -> tab.label }) { index, tab ->
                    FilterPill(
                        label = tab.label,
                        selected = index == tabIndex.coerceIn(0, tabs.lastIndex),
                        onClick = { selectedTab = tab },
                    )
                }
            }
            val items = remember(current, poses, favorites) { current.filter(poses, favorites) }
            LazyVerticalGrid(
                // Two columns on a small phone, so names have room; three from about 390dp.
                columns = GridCells.Adaptive(minSize = 116.dp),
                contentPadding = PaddingValues(start = Space.edge, end = Space.edge, top = Space.m, bottom = Space.section),
                horizontalArrangement = Arrangement.spacedBy(Space.edge),
                verticalArrangement = Arrangement.spacedBy(Space.edge),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                items(items, key = { it.id }) { pose ->
                    val isSelected = pose.id == selected?.id
                    PoseTile(
                        pose = pose,
                        // The tab already names the category, except on the mixed favourites tab. The pose in use says
                        // so in words as well as with the lavender outline.
                        detail = when {
                            isSelected -> "In use"
                            current == PoseTab.Favorites -> pose.stripCategory.label
                            pose.personCount > 1 -> "${pose.personCount} people"
                            else -> null
                        },
                        isFavorite = pose.id in favorites,
                        onToggleFavorite = { onToggleFavorite(pose.id) },
                        selected = isSelected,
                        // Narrow columns: most names take two lines, so every caption reserves two.
                        nameMinLines = 2,
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

/**
 * The current pose's card (its figure on the stage, the name, the facts and the favourite heart) with, for a
 * built-in pose with a sample photo, the "See it on a person" card fused under it; then its description, posing
 * steps and camera advice, and how to move the guide. The photo appears only here, in the sheet, never over the
 * viewfinder: the guide in the camera is always the figure.
 *
 * @param mirrored the guide is mirrored (the camera's flip toggle): the card's figure follows it, the photo doesn't
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PoseTipsSheet(
    pose: PoseTemplate,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    mirrored: Boolean = false,
) {
    PoseitBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Space.xl)
                .navigationBarsPadding(),
        ) {
            TipsPoseCard(pose, favorite, onToggleFavorite, mirrored)
            Column(Modifier.padding(horizontal = Space.textInset)) {
                if (pose.description.isNotBlank()) {
                    Spacer(Modifier.height(Space.section))
                    Text(pose.description, style = PoseitType.body, color = PoseitColors.textPrimary)
                }
                // The same sections as the pose's detail screen.
                if (pose.tips.isNotEmpty()) PoseSteps(pose.tips, Modifier.padding(top = Space.section))
                CameraSetup(pose.camera, Modifier.padding(top = Space.section))
                // The guide gestures are otherwise only described to screen readers.
                SectionHeading("Moving the guide", Modifier.padding(top = Space.section, bottom = Space.xs))
                Text(
                    "Drag the guide to move it, pinch to resize it and twist to rotate it. Double-tap to reset it. " +
                        "Tap the padlock to keep it in place; pinching then zooms the camera. Zoom is also in Quick settings.",
                    style = PoseitType.body,
                    color = PoseitColors.textPrimary,
                )
            }
        }
    }
}

/** Height of the tips card: the heart's target above the figure, plus the card padding. */
private val TipsCardHeight = 208.dp

/** Height of the tips card's figure box: the card less the heart's target, the top padding and the bottom inset. */
private val TipsFigureHeight = TipsCardHeight - 48.dp - Space.cardPadding + IconGlyphInset - Space.l

/**
 * The pose as a dark card, like the mockups' headline card: the name in Headline and the facts at the top left, two
 * small label and value pairs (camera height, framing) at its foot, and the figure on the stage at the right under
 * the favourite heart. A pose with a sample photo gets the "See it on a person" card fused under it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TipsPoseCard(pose: PoseTemplate, favorite: Boolean, onToggleFavorite: () -> Unit, mirrored: Boolean) {
    val photo = rememberPosePhoto(pose)
    ConnectedCardStack(
        Modifier
            .padding(horizontal = Space.edge)
            .fillMaxWidth(),
    ) {
        Row {
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = TipsCardHeight)
                    .padding(start = Space.cardPadding, top = Space.cardPadding, bottom = Space.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        pose.name,
                        style = PoseitType.headline,
                        color = PoseitColors.textPrimaryInverted,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(pose.factsPhrase(), style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                }
                FlowRow(
                    Modifier.padding(top = Space.l),
                    horizontalArrangement = Arrangement.spacedBy(Space.l),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    LabelledValue("Camera", pose.camera.height.label)
                    LabelledValue("Shot", pose.camera.shot.label)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PlainIconButton(
                    icon = if (favorite) PoseitIcons.favouriteOn else PoseitIcons.favouriteOff,
                    contentDescription = if (favorite) "Remove ${pose.name} from favourites" else "Add ${pose.name} to favourites",
                    onClick = onToggleFavorite,
                    // The glyph, not the touch target, lands on the card padding.
                    modifier = Modifier.padding(top = Space.cardPadding - IconGlyphInset, end = Space.cardPadding - IconGlyphInset),
                )
                PoseThumbnail(
                    template = pose,
                    background = null,
                    mirrored = mirrored,
                    modifier = Modifier
                        .padding(end = Space.l, bottom = Space.l)
                        .size(width = 112.dp, height = TipsFigureHeight),
                )
            }
        }
        // The compact companion: its photo at most three quarters of the figure box's height, so it never outgrows
        // the figure drawn small in this card.
        if (photo != null) PhotoCompanionCard(photo, figureMirrored = mirrored, compact = true, maxPhotoHeight = TipsFigureHeight * 0.75f)
    }
}

/** A small label over its value, in one node (the mockups' "Account / Bohdan"). */
@Composable
private fun LabelledValue(label: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
        Text(value, style = PoseitType.body, color = PoseitColors.textPrimaryInverted)
    }
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
    PoseitAlertDialog(
        onDismissRequest = onDismiss,
        title = "Photoshoot",
        content = {
            Column {
                // Both are one value out of a few, so both use the segmented pill, like the countdown in Settings.
                // The photo counts differ a lot in length ("3" and "Until I stop"), so each hugs its label.
                Text("Number of photos", style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
                Spacer(Modifier.height(Space.s))
                val shotChoices = TimerOption.SHOT_CHOICES
                PillTabs(
                    labels = shotChoices.map { if (it == 0) "Until I stop" else "$it" },
                    descriptions = shotChoices.map { if (it == 0) "Until I stop" else "$it photos" },
                    selectedIndex = shotChoices.indexOf(shots),
                    onSelect = { shots = shotChoices[it] },
                    role = Role.RadioButton,
                )
                Spacer(Modifier.height(Space.l))
                Text("Time between photos", style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
                Spacer(Modifier.height(Space.s))
                val intervalChoices = TimerOption.INTERVAL_CHOICES
                SegmentedPill(
                    options = intervalChoices.map { "$it s" },
                    descriptions = intervalChoices.map { "$it seconds" },
                    selectedIndex = intervalChoices.indexOf(interval),
                    onSelect = { interval = intervalChoices[it] },
                    modifier = Modifier.fillMaxWidth(),
                    optionPadding = 6.dp,
                )
                Spacer(Modifier.height(Space.l))
                Text(
                    "Press the shutter or a volume button to start. The first photo is taken after " +
                        "${TimerOption.PHOTOSHOOT_START_DELAY_SECONDS} seconds. Press stop to end early.",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondary,
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
