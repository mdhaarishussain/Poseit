package com.poseit.app.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poseit.app.BuildConfig
import com.poseit.app.appContainer
import com.poseit.app.data.AppSettings
import com.poseit.app.data.COUNTDOWN_CHOICES
import com.poseit.app.data.GridType
import com.poseit.app.data.OverlayStyle
import com.poseit.app.data.countdownChoiceFor
import com.poseit.app.ui.onboarding.DemoPoses
import com.poseit.app.ui.theme.PoseitColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val STRICTNESS_MIN = 0.60f
private const val STRICTNESS_MAX = 0.95f
private const val OPACITY_MIN = 0.15f

@OptIn(ExperimentalMaterial3Api::class)
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = PoseitColors.Violet)
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
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(key = "coaching") { CoachingSection(current, vm::update) }
            item(key = "overlay") { OverlaySection(current, vm::update) }
            item(key = "camera") { CameraSection(current, vm::update) }
            item(key = "poses") {
                MyPosesSection(
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
                AboutSection(
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
private fun CoachingSection(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsSectionTitle("Coaching")
    SettingsGroup {
        SwitchRow(
            icon = Icons.Filled.RecordVoiceOver,
            accent = PoseitColors.Amber,
            title = "Voice guidance",
            subtitle = "Spoken corrections and countdown",
            checked = s.voiceGuidance,
            onCheckedChange = { v -> update { it.copy(voiceGuidance = v) } },
        )
        SettingsDivider()
        SwitchRow(
            icon = Icons.Filled.AutoAwesome,
            accent = PoseitColors.Mint,
            title = "Auto-capture",
            subtitle = "Take the photo as soon as you hold the pose",
            checked = s.autoCapture,
            onCheckedChange = { v -> update { it.copy(autoCapture = v) } },
        )
        SettingsDivider()
        var strictness by remember(s.matchThreshold) {
            mutableFloatStateOf(s.matchThreshold.coerceIn(STRICTNESS_MIN, STRICTNESS_MAX))
        }
        val percent = (strictness * 100).roundToInt()
        SliderRow(
            icon = Icons.Filled.Speed,
            accent = PoseitColors.Violet,
            title = "Match strictness",
            valueLabel = "$percent% · ${strictnessLabel(strictness)}",
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
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Timer,
            accent = PoseitColors.Coral,
            title = "Countdown",
            subtitle = "Seconds between “matched” and the photo",
        )
        val selected = countdownChoiceFor(s.countdownSeconds)
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(start = 68.dp, end = 16.dp, bottom = 14.dp),
        ) {
            COUNTDOWN_CHOICES.forEachIndexed { index, seconds ->
                SegmentedButton(
                    selected = seconds == selected,
                    onClick = { update { it.copy(countdownSeconds = seconds) } },
                    shape = SegmentedButtonDefaults.itemShape(index, COUNTDOWN_CHOICES.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(activeContainerColor = PoseitColors.Violet.copy(alpha = 0.35f)),
                ) { Text(if (seconds == 0) "Off" else "${seconds}s", maxLines = 1) }
            }
        }
    }
}

internal fun strictnessLabel(threshold: Float): String = when {
    threshold < 0.725f -> "Relaxed"
    threshold < 0.875f -> "Balanced"
    else -> "Strict"
}

private fun strictnessHint(threshold: Float): String = when (strictnessLabel(threshold)) {
    "Relaxed" -> "Forgiving – great for kids, groups and quick snaps"
    "Balanced" -> "A close match before the shot is taken"
    else -> "Only a near-perfect pose counts"
}

@Composable
private fun OverlaySection(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val demo = remember { DemoPoses.handOnHip }
    var opacity by remember(s.overlayOpacity) { mutableFloatStateOf(s.overlayOpacity.coerceIn(OPACITY_MIN, 1f)) }
    SettingsSectionTitle("Overlay")
    SettingsGroup {
        SettingsRow(
            icon = Icons.Filled.Layers,
            accent = PoseitColors.Violet,
            title = "Guide style",
            subtitle = "How the pose is drawn over the camera",
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
        SettingsDivider()
        SliderRow(
            icon = Icons.Filled.Opacity,
            accent = PoseitColors.Mint,
            title = "Guide opacity",
            valueLabel = "${(opacity * 100).roundToInt()}%",
            subtitle = null,
            value = opacity,
            onValueChange = { opacity = it },
            onValueChangeFinished = { update { it.copy(overlayOpacity = opacity) } },
            valueRange = OPACITY_MIN..1f,
        )
        SettingsDivider()
        SwitchRow(
            icon = Icons.Filled.Accessibility,
            accent = PoseitColors.Coral,
            title = "Live skeleton",
            subtitle = "Show the body Poseit detects as dots and lines",
            checked = s.showLiveSkeleton,
            onCheckedChange = { v -> update { it.copy(showLiveSkeleton = v) } },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.GridOn,
            accent = PoseitColors.Amber,
            title = "Grid",
            subtitle = s.grid.label,
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
        SettingsDivider()
        SwitchRow(
            icon = Icons.Filled.Straighten,
            accent = PoseitColors.Mint,
            title = "Level indicator",
            subtitle = "Helps you keep the horizon straight",
            checked = s.showLevel,
            onCheckedChange = { v -> update { it.copy(showLevel = v) } },
        )
    }
}

@Composable
private fun CameraSection(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsSectionTitle("Camera")
    SettingsGroup {
        SwitchRow(
            icon = Icons.Filled.Memory,
            accent = PoseitColors.Violet,
            title = "High-accuracy model",
            subtitle = "More accurate, slightly slower",
            checked = s.accurateModel,
            onCheckedChange = { v -> update { it.copy(accurateModel = v) } },
        )
        SettingsDivider()
        SwitchRow(
            icon = Icons.Filled.FlipCameraAndroid,
            accent = PoseitColors.Coral,
            title = "Mirror front-camera photos",
            subtitle = "Save selfies the way they look in the preview",
            checked = s.mirrorFrontPhotos,
            onCheckedChange = { v -> update { it.copy(mirrorFrontPhotos = v) } },
        )
        SettingsDivider()
        SwitchRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            accent = PoseitColors.Amber,
            title = "Shutter sound",
            subtitle = null,
            checked = s.shutterSound,
            onCheckedChange = { v -> update { it.copy(shutterSound = v) } },
        )
        SettingsDivider()
        SwitchRow(
            icon = Icons.Filled.Vibration,
            accent = PoseitColors.Mint,
            title = "Haptics",
            subtitle = "Vibrate when you match and when a photo is taken",
            checked = s.haptics,
            onCheckedChange = { v -> update { it.copy(haptics = v) } },
        )
    }
}

@Composable
private fun MyPosesSection(customCount: Int, busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    SettingsSectionTitle("My poses")
    SettingsGroup {
        ActionRow(
            icon = Icons.Filled.FileUpload,
            accent = PoseitColors.Violet,
            title = "Export my poses",
            subtitle = if (customCount == 0) "Nothing to export yet" else "Save ${PoseBackup.poses(customCount)} to a file",
            onClick = onExport,
            enabled = !busy,
        )
        SettingsDivider()
        ActionRow(
            icon = Icons.Filled.FileDownload,
            accent = PoseitColors.Mint,
            title = "Import poses",
            subtitle = "Add poses from a Poseit file",
            onClick = onImport,
            enabled = !busy,
        )
    }
}

@Composable
private fun AboutSection(onShowLicenses: () -> Unit, onShowIntro: () -> Unit) {
    SettingsSectionTitle("About")
    SettingsGroup {
        SettingsRow(
            icon = Icons.Filled.Info,
            accent = PoseitColors.Violet,
            title = "Poseit",
            subtitle = "Version ${BuildConfig.VERSION_NAME}",
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.VerifiedUser,
            accent = PoseitColors.Mint,
            title = "Works offline · no ads · no account",
            subtitle = "Your camera feed and photos never leave your phone",
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Brush,
            accent = PoseitColors.Coral,
            title = "All poses are original illustrations",
            subtitle = "Every figure is generated by Poseit's own pose engine",
        )
        SettingsDivider()
        ActionRow(
            icon = Icons.Filled.Code,
            accent = PoseitColors.Amber,
            title = "Open-source notices",
            subtitle = "Libraries and models that make Poseit possible",
            onClick = onShowLicenses,
        )
        SettingsDivider()
        ActionRow(
            icon = Icons.Filled.Replay,
            accent = PoseitColors.Violet,
            title = "Show intro again",
            subtitle = null,
            onClick = onShowIntro,
            showChevron = false,
        )
    }
}

private data class Notice(val name: String, val owner: String, val license: String)

private val notices = listOf(
    Notice("MediaPipe Tasks Vision", "Google LLC", "Apache License 2.0"),
    Notice("MediaPipe Pose Landmarker models (lite & full)", "Google LLC", "Apache License 2.0"),
    Notice("DeepLab v3 segmentation model (MediaPipe)", "Google LLC", "Apache License 2.0"),
    Notice("AndroidX, Jetpack Compose, CameraX, DataStore, Navigation", "The Android Open Source Project", "Apache License 2.0"),
    Notice("Material Icons", "Google LLC", "Apache License 2.0"),
    Notice("Coil", "Coil Contributors", "Apache License 2.0"),
    Notice("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o. and contributors", "Apache License 2.0"),
)

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Code, contentDescription = null) },
        title = { Text("Open-source notices") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                notices.forEach { n ->
                    Column {
                        Text(n.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${n.owner} · ${n.license}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Licensed under the Apache License, Version 2.0. You may obtain a copy of the license at " +
                        "https://www.apache.org/licenses/LICENSE-2.0. Software distributed under the license is " +
                        "distributed on an \"AS IS\" basis, without warranties or conditions of any kind.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "All pose illustrations in Poseit are original and generated by the app itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
