package com.poseit.app.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import com.poseit.app.data.countdownChoiceFor
import com.poseit.app.ui.components.SettingRow
import com.poseit.app.ui.components.SliderRow
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.SwitchRow
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.onboarding.DemoPoses
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val STRICTNESS_MIN = 0.60f
private const val STRICTNESS_MAX = 0.95f
private const val OPACITY_MIN = 0.15f

/** A row title with its picture tiles or segmented control underneath (no bottom padding of its own). */
private val TitleAbovePadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.m, bottom = Space.m)

@Composable
fun SettingsScreen(onBack: () -> Unit, onShowIntro: () -> Unit) {
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
        containerColor = Studio.Paper,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                StudioTopBar("Settings", onBack = onBack)
                // Always 2dp tall, so the list doesn't jump when an import or export starts.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                ) {
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxSize(), color = Studio.Ink, trackColor = Studio.Hairline)
                    }
                }
            }
        },
    ) { padding ->
        val current = settings
        if (current == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Studio.Ink) }
            return@Scaffold
        }
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection),
                top = padding.calculateTopPadding() + Space.s,
                bottom = padding.calculateBottomPadding() + Space.section,
            ),
            verticalArrangement = Arrangement.spacedBy(Space.section),
        ) {
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
                AboutGroup(
                    onShowLicenses = { showLicenses = true },
                    // Plays it right away; the stored "intro seen" flag stays set (the intro sets it again on finish).
                    onShowIntro = onShowIntro,
                )
            }
        }
    }

    if (showLicenses) LicensesDialog(onDismiss = { showLicenses = false })
}

/* ---------------------------------------------------------------------------------------------- */

@Composable
private fun CoachingGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsGroup("Coaching") {
        SwitchRow(
            title = "Voice guidance",
            subtitle = "Spoken hints and countdown",
            checked = s.voiceGuidance,
            onCheckedChange = { v -> update { it.copy(voiceGuidance = v) } },
        )
        RowDivider()
        SwitchRow(
            title = "Auto-capture",
            subtitle = "Take the photo as soon as you hold the pose",
            checked = s.autoCapture,
            onCheckedChange = { v -> update { it.copy(autoCapture = v) } },
        )
        RowDivider()
        var strictness by remember(s.matchThreshold) {
            mutableFloatStateOf(s.matchThreshold.coerceIn(STRICTNESS_MIN, STRICTNESS_MAX))
        }
        val percent = (strictness * 100).roundToInt()
        SliderRow(
            title = "Match strictness",
            valueLabel = "$percent%, ${strictnessLabel(strictness).lowercase()}",
            subtitle = strictnessHint(strictness),
            value = strictness,
            onValueChange = { strictness = it },
            onValueChangeFinished = {
                val v = (strictness * 20).roundToInt() / 20f // snap to 5 %
                update { it.copy(matchThreshold = v) }
            },
            valueRange = STRICTNESS_MIN..STRICTNESS_MAX,
            steps = 6,
        )
        RowDivider()
        SettingRow(
            title = "Countdown",
            subtitle = "Seconds between a match and the photo",
            contentPadding = TitleAbovePadding,
        )
        CountdownControl(
            selected = countdownChoiceFor(s.countdownSeconds),
            onSelect = { seconds -> update { it.copy(countdownSeconds = seconds) } },
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.m),
        )
    }
}

internal fun strictnessLabel(threshold: Float): String = when {
    threshold < 0.725f -> "Relaxed"
    threshold < 0.875f -> "Balanced"
    else -> "Strict"
}

private fun strictnessHint(threshold: Float): String = when (strictnessLabel(threshold)) {
    "Relaxed" -> "A rough match is enough. Suits kids and groups."
    "Balanced" -> "Needs a close match before the photo"
    else -> "Needs an almost exact match"
}

@Composable
private fun GuideGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val demo = remember { DemoPoses.handOnHip }
    var opacity by remember(s.overlayOpacity) { mutableFloatStateOf(s.overlayOpacity.coerceIn(OPACITY_MIN, 1f)) }
    SettingsGroup("Guide") {
        SettingRow(
            title = "Guide style",
            subtitle = "How the pose is drawn over the camera",
            contentPadding = TitleAbovePadding,
        )
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
        RowDivider()
        SliderRow(
            title = "Guide opacity",
            valueLabel = "${(opacity * 100).roundToInt()}%",
            value = opacity,
            onValueChange = { opacity = it },
            onValueChangeFinished = { update { it.copy(overlayOpacity = opacity) } },
            valueRange = OPACITY_MIN..1f,
        )
        RowDivider()
        SwitchRow(
            title = "Live skeleton",
            subtitle = "Show the body Poseit detects as dots and lines",
            checked = s.showLiveSkeleton,
            onCheckedChange = { v -> update { it.copy(showLiveSkeleton = v) } },
        )
        RowDivider()
        SettingRow(
            title = "Grid",
            subtitle = s.grid.label,
            contentPadding = TitleAbovePadding,
        )
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
        RowDivider()
        SwitchRow(
            title = "Level indicator",
            subtitle = "Helps you keep the horizon straight",
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
        RowDivider()
        SwitchRow(
            title = "Mirror front-camera photos",
            subtitle = "Save selfies the way they look in the preview",
            checked = s.mirrorFrontPhotos,
            onCheckedChange = { v -> update { it.copy(mirrorFrontPhotos = v) } },
        )
        RowDivider()
        SwitchRow(
            title = "Shutter sound",
            checked = s.shutterSound,
            onCheckedChange = { v -> update { it.copy(shutterSound = v) } },
        )
        RowDivider()
        SwitchRow(
            title = "Haptics",
            subtitle = "Vibrate when a photo is taken or a raised hand starts the timer",
            checked = s.haptics,
            onCheckedChange = { v -> update { it.copy(haptics = v) } },
        )
    }
}

@Composable
private fun YourPosesGroup(customCount: Int, busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    SettingsGroup("Your poses") {
        SettingRow(
            title = "Export poses",
            subtitle = if (customCount == 0) {
                "Nothing to export yet"
            } else {
                "Save ${PoseBackup.poses(customCount)} to a file. Photo guides stay on this phone."
            },
            onClick = onExport,
            enabled = !busy,
        ) { RowChevron() }
        RowDivider()
        SettingRow(
            title = "Import poses",
            subtitle = "Add poses from a Poseit file",
            onClick = onImport,
            enabled = !busy,
        ) { RowChevron() }
    }
}

@Composable
private fun AboutGroup(onShowLicenses: () -> Unit, onShowIntro: () -> Unit) {
    SettingsGroup("About") {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter, vertical = Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Column {
                Text("Poseit", style = StudioType.bodyStrong, color = Studio.Ink)
                Text("Version ${BuildConfig.VERSION_NAME}", style = StudioType.label, color = Studio.Graphite)
            }
            Text(
                "Works offline, with no ads and no account. The camera feed and your photos stay on this phone.",
                style = StudioType.body,
                color = Studio.Ink,
            )
            Text(
                "Pose detection uses MediaPipe (Apache License 2.0). The typeface is Archivo (SIL Open Font License 1.1).",
                style = StudioType.label,
                color = Studio.Graphite,
            )
        }
        RowDivider()
        SettingRow(title = "Open-source notices", onClick = onShowLicenses) { RowChevron() }
        RowDivider()
        SettingRow(title = "Show intro again", onClick = onShowIntro) { RowChevron() }
    }
}

private data class Notice(val name: String, val owner: String, val license: String)

private val notices = listOf(
    Notice("MediaPipe Tasks Vision", "Google LLC", "Apache License 2.0"),
    Notice("MediaPipe Pose Landmarker models (lite and full)", "Google LLC", "Apache License 2.0"),
    Notice("DeepLab v3 segmentation model (MediaPipe)", "Google LLC", "Apache License 2.0"),
    Notice("AndroidX, Jetpack Compose, CameraX, DataStore, Navigation", "The Android Open Source Project", "Apache License 2.0"),
    Notice("Material Icons", "Google LLC", "Apache License 2.0"),
    Notice("Coil", "Coil Contributors", "Apache License 2.0"),
    Notice("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o. and contributors", "Apache License 2.0"),
    Notice("Archivo typeface", "The Archivo Project Authors (Omnibus-Type)", "SIL Open Font License 1.1"),
)

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = StudioShapes.Medium,
        title = { Text("Open-source notices") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                notices.forEach { n ->
                    Column {
                        Text(n.name, style = StudioType.bodyStrong, color = Studio.Ink)
                        Text("${n.owner}, ${n.license}", style = MaterialTheme.typography.bodySmall, color = Studio.Graphite)
                    }
                }
                Text(
                    "The Apache License 2.0 is at apache.org/licenses/LICENSE-2.0 and the SIL Open Font License 1.1 " +
                        "at openfontlicense.org. Software and fonts under these licences are distributed \"as is\", " +
                        "without warranties or conditions of any kind.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Studio.Graphite,
                )
                Text(
                    "All pose illustrations in Poseit are original and generated by the app itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Studio.Graphite,
                )
            }
        },
        confirmButton = { TertiaryButton("Close", onClick = onDismiss) },
    )
}
