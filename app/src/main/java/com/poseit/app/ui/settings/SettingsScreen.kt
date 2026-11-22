package com.poseit.app.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.BuildConfig
import com.poseit.app.appContainer
import com.poseit.app.data.AppSettings
import com.poseit.app.data.GridType
import com.poseit.app.data.OverlayStyle
import com.poseit.app.data.PosePhoto
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.OutlinedRow
import com.poseit.app.ui.components.PoseitAlertDialog
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.RowChevron
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.SliderRow
import com.poseit.app.ui.components.SwitchRow
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.components.PinnedTopBar
import com.poseit.app.ui.components.photoSourcesPhrase
import com.poseit.app.ui.components.rememberPosePhotos
import com.poseit.app.ui.components.rememberTopBarScrolledAway
import com.poseit.app.ui.components.ValueSlider
import com.poseit.app.ui.onboarding.DemoPoses
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * Settings (docs/DESIGN.md, "Screen map"): the top bar card fused to two dark cards that decide when the photo is
 * taken (match strictness, countdown), then the other settings as outlined cream row groups under section
 * headings, places to go as outlined rows, and an About stack of dark cards at the foot. Everything scrolls under a
 * canvas band that covers the status bar.
 */

@Composable
fun SettingsScreen(onBack: () -> Unit, onShowIntro: () -> Unit, onOpenPhotoCredits: () -> Unit) {
    val appContext = LocalContext.current.applicationContext
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val container = appContext.appContainer
                SettingsViewModel(container.settingsRepository, container.poseRepository, appContext.contentResolver)
            }
        },
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    val customCount by vm.customPoseCount.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val posePhotos = rememberPosePhotos()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PoseBackup.MIME_TYPE)) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }
    fun launchSafely(block: () -> Unit) {
        try {
            block()
        } catch (e: ActivityNotFoundException) {
            vm.showMessage("No file manager found on this phone")
        }
    }

    Scaffold(
        containerColor = PoseitColors.backgroundBody,
        snackbarHost = { PoseitSnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        contentWindowInsets = WindowInsets(0),
    ) { _ ->
        val current = settings
        val layoutDirection = LocalLayoutDirection.current
        val sides = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listState = rememberLazyListState()
        val barAway = rememberTopBarScrolledAway(listState)
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = sides.calculateStartPadding(layoutDirection) + Space.edge,
                    end = sides.calculateEndPadding(layoutDirection) + Space.edge,
                    top = top + Space.edge,
                    bottom = bottom + Space.section,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.section),
            ) {
                item(key = "header") {
                    ConnectedCardStack(Modifier.fillMaxWidth()) {
                        TopBarContent(
                            "Settings",
                            onBack = onBack,
                            // While the pinned bar stands in for it, keep a single Back in the tree.
                            modifier = if (barAway) Modifier.clearAndSetSemantics {} else Modifier,
                        )
                        if (current != null) {
                            StrictnessCard(current, vm::update)
                            CountdownCard(current, vm::update)
                        }
                    }
                }
                if (current == null) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = Space.xxl), contentAlignment = Alignment.Center) {
                            PoseitSpinner(color = PoseitColors.textPrimary)
                        }
                    }
                } else {
                    item(key = "coaching") { CoachingGroup(current, vm::update) }
                    item(key = "guide") { GuideGroup(current, vm::update) }
                    item(key = "camera") { CameraGroup(current, vm::update) }
                    item(key = "poses") {
                        YourPosesGroup(
                            customCount = customCount,
                            busy = busy,
                            onExport = {
                                if (customCount == 0) {
                                    scope.launch { snackbar.showSnackbar("You haven't made any poses yet") }
                                } else {
                                    launchSafely { exportLauncher.launch(PoseBackup.FILE_NAME) }
                                }
                            },
                            onImport = { launchSafely { importLauncher.launch(PoseBackup.IMPORT_MIME_TYPES) } },
                        )
                    }
                    item(key = "about") {
                        AboutStack(
                            onShowLicenses = { showLicenses = true },
                            onOpenPhotoCredits = onOpenPhotoCredits,
                            // Plays it right away; the stored "intro seen" flag stays set (the intro sets it again on finish).
                            onShowIntro = onShowIntro,
                        )
                    }
                }
            }
            // The list scrolls under the status bar onto this canvas band, never through it.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(PoseitColors.backgroundBody),
            )
            PinnedTopBar(visible = barAway, title = "Settings", onBack = onBack)
        }
    }

    if (showLicenses) {
        val shown = posePhotos.all
        LicensesDialog(
            photoNotices = remember(shown) { photoNotices(shown) },
            mixkit = shown.any { it.source == PosePhoto.SOURCE_MIXKIT },
            onDismiss = { showLicenses = false },
        )
    }
}

/* ---------------------------------------------------------------------------------------------- */

/**
 * The first card under the top bar, like the mockups' "Your progress 79%": what the strictness means, then the value
 * in light numerals beside its slider. The numerals are hidden from screen readers; the slider announces the value.
 */
@Composable
private fun StrictnessCard(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    var strictness by remember(s.matchThreshold) {
        mutableFloatStateOf(s.matchThreshold.coerceIn(SettingCopy.STRICTNESS_MIN, SettingCopy.STRICTNESS_MAX))
    }
    CardSegment {
        Text(SettingCopy.STRICTNESS, style = PoseitType.bodyLarge, color = PoseitColors.textPrimaryInverted)
        Text(SettingCopy.strictnessHint(strictness), style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
        Spacer(Modifier.height(Space.l))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${(strictness * 100).roundToInt()}%",
                style = PoseitType.numeralLarge,
                color = PoseitColors.textPrimaryInverted,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Spacer(Modifier.width(Space.l))
            val valueLabel = SettingCopy.strictnessValue(strictness)
            ValueSlider(
                value = strictness,
                onValueChange = { strictness = it },
                onValueChangeFinished = {
                    val v = SettingCopy.snapStrictness(strictness)
                    update { it.copy(matchThreshold = v) }
                },
                valueRange = SettingCopy.STRICTNESS_MIN..SettingCopy.STRICTNESS_MAX,
                steps = 6,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = SettingCopy.STRICTNESS
                        stateDescription = valueLabel
                    },
            )
        }
    }
}

/** The countdown as a segmented pill across the card, fused under the strictness card. */
@Composable
private fun CountdownCard(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    CardSegment {
        Text(SettingCopy.COUNTDOWN, style = PoseitType.bodyLarge, color = PoseitColors.textPrimaryInverted)
        Text(SettingCopy.COUNTDOWN_SUBTITLE, style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
        Spacer(Modifier.height(Space.l))
        CountdownChips(
            seconds = s.countdownSeconds,
            onSelect = { seconds -> update { it.copy(countdownSeconds = seconds) } },
        )
    }
}

@Composable
private fun CoachingGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsGroup(SettingCopy.COACHING) {
        SwitchRow(
            title = SettingCopy.VOICE,
            subtitle = SettingCopy.VOICE_SUBTITLE,
            checked = s.voiceGuidance,
            onCheckedChange = { v -> update { it.copy(voiceGuidance = v) } },
        )
        SwitchRow(
            title = SettingCopy.AUTO_CAPTURE,
            subtitle = SettingCopy.AUTO_CAPTURE_SUBTITLE,
            checked = s.autoCapture,
            onCheckedChange = { v -> update { it.copy(autoCapture = v) } },
        )
    }
}

@Composable
private fun GuideGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val demo = remember { DemoPoses.handOnHip }
    var opacity by remember(s.overlayOpacity) { mutableFloatStateOf(s.overlayOpacity.coerceIn(SettingCopy.OPACITY_MIN, 1f)) }
    SettingsGroup(SettingCopy.GUIDE) {
        TitledRow("Guide style", "How the pose is drawn over the camera") {
            TileRow { weight ->
                OverlayStyle.entries.forEach { style ->
                    ChoiceTile(
                        label = style.label,
                        selected = s.overlayStyle == style,
                        onClick = { update { it.copy(overlayStyle = style) } },
                        description = "${style.label} guide",
                        modifier = weight,
                    ) { OverlayPreview(demo, style, opacity) }
                }
            }
        }
        SliderRow(
            title = SettingCopy.GUIDE_OPACITY,
            valueLabel = "${(opacity * 100).roundToInt()}%",
            value = opacity,
            onValueChange = { opacity = it },
            onValueChangeFinished = { update { it.copy(overlayOpacity = opacity) } },
            valueRange = SettingCopy.OPACITY_MIN..1f,
        )
        SwitchRow(
            title = SettingCopy.LIVE_SKELETON,
            subtitle = SettingCopy.LIVE_SKELETON_SUBTITLE,
            checked = s.showLiveSkeleton,
            onCheckedChange = { v -> update { it.copy(showLiveSkeleton = v) } },
        )
        TitledRow("Grid", s.grid.label) {
            TileRow { weight ->
                GridType.entries.forEach { grid ->
                    ChoiceTile(
                        label = grid.shortLabel,
                        selected = s.grid == grid,
                        onClick = { update { it.copy(grid = grid) } },
                        description = "Grid: ${grid.label}",
                        modifier = weight,
                    ) { GridPreview(grid) }
                }
            }
        }
        SwitchRow(
            title = SettingCopy.LEVEL,
            subtitle = SettingCopy.LEVEL_SUBTITLE,
            checked = s.showLevel,
            onCheckedChange = { v -> update { it.copy(showLevel = v) } },
        )
    }
}

@Composable
private fun CameraGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsGroup("Camera") {
        SwitchRow(
            title = "High-accuracy model",
            subtitle = "More accurate, slightly slower",
            checked = s.accurateModel,
            onCheckedChange = { v -> update { it.copy(accurateModel = v) } },
        )
        SwitchRow(
            title = "Mirror front-camera photos",
            subtitle = "Saves selfies the way they look in the preview",
            checked = s.mirrorFrontPhotos,
            onCheckedChange = { v -> update { it.copy(mirrorFrontPhotos = v) } },
        )
        SwitchRow(
            title = "Shutter sound",
            checked = s.shutterSound,
            onCheckedChange = { v -> update { it.copy(shutterSound = v) } },
        )
        SwitchRow(
            title = "Haptics",
            subtitle = "Vibrates when a photo is taken or a raised hand starts the timer",
            checked = s.haptics,
            onCheckedChange = { v -> update { it.copy(haptics = v) } },
        )
    }
}

/** Places to go, as separate outlined rows (they open a file picker). A spinner by the heading while one runs. */
@Composable
private fun YourPosesGroup(customCount: Int, busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.edge)) {
        SectionHeading(
            "Your poses",
            Modifier.padding(horizontal = Space.cardPadding),
            action = if (busy) {
                {
                    PoseitSpinner(
                        size = 20.dp,
                        color = PoseitColors.textPrimary,
                        modifier = Modifier.semantics { contentDescription = "Working on your poses" },
                    )
                }
            } else {
                null
            },
        )
        OutlinedRow(
            title = "Export poses",
            subtitle = if (customCount == 0) {
                "Nothing to export yet"
            } else {
                "Save ${PoseBackup.poses(customCount)} to a file. Photo guides stay on this phone."
            },
            onClick = onExport,
            enabled = !busy,
        )
        OutlinedRow(
            title = "Import poses",
            subtitle = "Add poses from a Poseit file",
            onClick = onImport,
            enabled = !busy,
        )
    }
}

/**
 * About, at the foot: a dark card with the wordmark, the version and Poseit's promise in the card headline, fused to
 * dark rows that open the notices and the photo credits and replay the intro.
 */
@Composable
private fun AboutStack(onShowLicenses: () -> Unit, onOpenPhotoCredits: () -> Unit, onShowIntro: () -> Unit) {
    val photos = rememberPosePhotos()
    Column(Modifier.fillMaxWidth()) {
        SectionHeading("About", Modifier.padding(horizontal = Space.cardPadding))
        Spacer(Modifier.height(Space.s))
        ConnectedCardStack(Modifier.fillMaxWidth()) {
            CardSegment {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Poseit", style = PoseitType.wordmark, color = PoseitColors.textPrimaryInverted, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(Space.l))
                    Text("Version ${BuildConfig.VERSION_NAME}", style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                }
                Spacer(Modifier.height(Space.xl))
                Text(
                    "Works offline, with no ads and no account.",
                    style = PoseitType.headline,
                    color = PoseitColors.textPrimaryInverted,
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    "The camera feed and your photos stay on this phone.",
                    style = PoseitType.body,
                    color = PoseitColors.textSecondaryInverted,
                )
                Spacer(Modifier.height(Space.l))
                Text(
                    "Pose detection uses MediaPipe (Apache License 2.0). The typeface is Hanken Grotesk (SIL Open Font " +
                        "License 1.1). The sample photos of the poses come from ${photoSourcesPhrase(photos.all)}.",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondaryInverted,
                )
            }
            LinkSegment("Open-source notices", onShowLicenses)
            LinkSegment("Photo credits", onOpenPhotoCredits)
            LinkSegment("Show intro again", onShowIntro)
        }
    }
}

/** A dark card in a connected stack that opens something: the title in cream and a chevron. One button. */
@Composable
private fun LinkSegment(title: String, onClick: () -> Unit) {
    CardSegment(
        contentPadding = PaddingValues(start = Space.cardPadding, end = Space.l, top = Space.l, bottom = Space.l),
        onClick = onClick,
    ) {
        Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = PoseitType.bodyLarge, color = PoseitColors.textPrimaryInverted, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Space.l))
            RowChevron()
        }
    }
}

private data class Notice(val name: String, val owner: String, val license: String)

/** The sample photos as notices, one per source and licence: "Sample pose photos (27)", "Mixkit", its licence. */
private fun photoNotices(photos: List<PosePhoto>): List<Notice> =
    photos.groupBy { it.source to it.licence }.map { (key, list) ->
        val (source, licence) = key
        val owner = if (source == PosePhoto.SOURCE_MIXKIT) "Mixkit" else "$source contributors"
        Notice("Sample pose photos (${list.size})", owner, licence)
    }

private val notices = listOf(
    Notice("MediaPipe Tasks Vision", "Google LLC", "Apache License 2.0"),
    Notice("MediaPipe Pose Landmarker models (lite and full)", "Google LLC", "Apache License 2.0"),
    Notice("DeepLab v3 segmentation model (MediaPipe)", "Google LLC", "Apache License 2.0"),
    Notice("AndroidX, Jetpack Compose, CameraX, DataStore, Navigation", "The Android Open Source Project", "Apache License 2.0"),
    Notice("Material Icons", "Google LLC", "Apache License 2.0"),
    Notice("Coil", "Coil Contributors", "Apache License 2.0"),
    Notice("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o. and contributors", "Apache License 2.0"),
    Notice("Hanken Grotesk typeface", "The Hanken Grotesk Project Authors", "SIL Open Font License 1.1"),
    Notice("Astryx design system (token and component model)", "Meta Platforms, Inc.", "MIT License"),
)

@Composable
private fun LicensesDialog(photoNotices: List<Notice>, mixkit: Boolean, onDismiss: () -> Unit) {
    PoseitAlertDialog(
        onDismissRequest = onDismiss,
        title = "Open-source notices",
        content = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                (notices + photoNotices).forEach { n ->
                    Column(Modifier.semantics(mergeDescendants = true) {}) {
                        Text(n.name, style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
                        Text("${n.owner}, ${n.license}", style = PoseitType.supporting, color = PoseitColors.textSecondary)
                    }
                }
                Text(
                    "The Apache License 2.0 is at apache.org/licenses/LICENSE-2.0, the SIL Open Font License 1.1 " +
                        "at openfontlicense.org and the MIT License at opensource.org/license/mit. Software and fonts " +
                        "under these licences are distributed \"as is\", " +
                        "without warranties or conditions of any kind.",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondary,
                )
                Text(
                    "The sample photos are used under " +
                        (if (mixkit) "the Mixkit Stock Video Free License (mixkit.co/license), " else "") +
                        "Creative Commons licences (creativecommons.org/licenses) and the CC0 public domain dedication " +
                        "(creativecommons.org/publicdomain/zero/1.0). Each photo's author, source, licence text and " +
                        "the changes made are in Photo credits.",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondary,
                )
                Text(
                    "All pose illustrations in Poseit are original and generated by the app itself.",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondary,
                )
            }
        },
        confirmButton = { TertiaryButton("Close", onClick = onDismiss) },
    )
}
