package com.poseit.app.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.poseit.app.appContainer
import com.poseit.app.pose.CameraAdvice
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.library.Difficulty
import com.poseit.app.ui.library.DifficultyDots
import com.poseit.app.ui.library.EmptyState
import com.poseit.app.ui.library.FavoriteToggle
import com.poseit.app.ui.library.isCustom
import com.poseit.app.ui.library.moodEmoji
import com.poseit.app.ui.library.peopleIcon
import com.poseit.app.ui.library.peopleLabel
import com.poseit.app.ui.theme.PoseitColors
import java.io.File

/** Pose details: big preview, tips, camera advice, favourite, "Try this pose". */
@Composable
fun PoseDetailScreen(poseId: String, onBack: () -> Unit, onTryPose: (String) -> Unit, onEdit: (String) -> Unit) {
    val appContext = LocalContext.current.applicationContext
    val vm: PoseDetailViewModel = viewModel(
        key = "pose_detail_$poseId",
        factory = viewModelFactory { initializer { PoseDetailViewModel(poseId, appContext.appContainer.poseRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onBack() }

    PoseDetailContent(
        state = state,
        onBack = onBack,
        onTryPose = onTryPose,
        onEdit = onEdit,
        onToggleFavorite = vm::toggleFavorite,
        onDelete = vm::delete,
    )
}

private enum class PreviewMode(val label: String) { MANNEQUIN("Mannequin"), PHOTO("Photo"), OUTLINE("Outline") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoseDetailContent(
    state: PoseDetailUiState,
    onBack: () -> Unit,
    onTryPose: (String) -> Unit,
    onEdit: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val ready = state as? PoseDetailUiState.Ready
    val pose = ready?.details?.pose
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val previewHeight = previewHeight()
    val showTitle by remember(previewHeight) {
        derivedStateOf { scroll.value > with(density) { previewHeight.toPx() } }
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    AnimatedVisibility(visible = showTitle && pose != null, enter = fadeIn(), exit = fadeOut()) {
                        Text(pose?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    if (ready != null && pose != null) {
                        FavoriteToggle(
                            isFavorite = ready.isFavorite,
                            poseName = pose.name,
                            onToggle = onToggleFavorite,
                            onImage = false,
                        )
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (pose.isCustom) "Edit" else "Customize a copy") },
                                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                                    onClick = { menuOpen = false; onEdit(pose.id) },
                                )
                                if (pose.isCustom) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = { menuOpen = false; confirmDelete = true },
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showTitle) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (pose != null) {
                DetailBottomBar(
                    isCustom = pose.isCustom,
                    onEdit = { onEdit(pose.id) },
                    onTry = { onTryPose(pose.id) },
                )
            }
        },
    ) { padding ->
        when (state) {
            PoseDetailUiState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            PoseDetailUiState.NotFound -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    emoji = "🤷",
                    title = "Pose not found",
                    message = "This pose may have been deleted, or it isn't available on this device.",
                ) { Button(onClick = onBack) { Text("Back to the library") } }
            }

            is PoseDetailUiState.Ready -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp),
            ) {
                PosePreview(details = state.details, height = previewHeight)
                Spacer(Modifier.height(20.dp))
                DetailHeader(state.details.pose)
                if (state.details.pose.tips.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    TipsCard(state.details.pose.tips)
                }
                Spacer(Modifier.height(16.dp))
                CameraSetupCard(state.details.pose.camera)
                if (state.details.pose.tags.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    TagsSection(state.details.pose)
                }
                if (state.details.pose.isCustom) {
                    Spacer(Modifier.height(24.dp))
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete this pose")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete && pose != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
            title = { Text("Delete this pose?") },
            text = { Text("“${pose.name}” will be removed from My poses. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun previewHeight() = LocalConfiguration.current.screenHeightDp.let { h -> (h * 0.48f).coerceIn(260f, 540f).dp }

@Composable
private fun DetailBottomBar(isCustom: Boolean, onEdit: () -> Unit, onTry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.height(52.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                Icon(if (isCustom) Icons.Filled.Edit else Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isCustom) "Edit" else "Customize")
            }
            Button(
                onClick = onTry,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PoseitColors.Violet, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Try this pose", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosePreview(details: PoseDetails, height: Dp) {
    val pose = details.pose
    val modes = remember(details) {
        buildList {
            add(PreviewMode.MANNEQUIN)
            if (details.photoPath != null) add(PreviewMode.PHOTO)
            if (details.outlinePath != null) add(PreviewMode.OUTLINE)
        }
    }
    var selected by rememberSaveable { mutableStateOf(PreviewMode.MANNEQUIN) }
    val mode = if (selected in modes) selected else PreviewMode.MANNEQUIN
    var mirrored by rememberSaveable { mutableStateOf(false) }
    val flip by animateFloatAsState(
        targetValue = if (mirrored) -1f else 1f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "mirror",
    )

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(28.dp))
                .background(PreviewBackground),
        ) {
            Crossfade(targetState = mode, label = "previewMode") { m ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .graphicsLayer { scaleX = flip },
                    contentAlignment = Alignment.Center,
                ) {
                    when (m) {
                        PreviewMode.MANNEQUIN -> PoseThumbnail(
                            template = pose,
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics { contentDescription = "Mannequin showing the ${pose.name} pose" },
                            background = null,
                        )
                        PreviewMode.PHOTO -> FrameBox(pose.frameAspect) {
                            AsyncImage(
                                model = details.photoPath?.let(::File),
                                contentDescription = "Your reference photo for ${pose.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        PreviewMode.OUTLINE -> FrameBox(pose.frameAspect) {
                            if (details.photoPath != null) {
                                AsyncImage(
                                    model = File(details.photoPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.3f,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            AsyncImage(
                                model = details.outlinePath?.let(::File),
                                contentDescription = "Outline of ${pose.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
            MirroredBadge(
                visible = mirrored,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
            )
            Surface(
                onClick = { mirrored = !mirrored },
                shape = RoundedCornerShape(50),
                color = if (mirrored) PoseitColors.Violet else Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .semantics { stateDescription = if (mirrored) "Mirrored" else "Original" },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mirror", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (modes.size > 1) {
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { selected = m },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        icon = {},
                    ) { Text(m.label, maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun MirroredBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier,
    ) {
        Text(
            "Mirrored",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Letterboxes [content] into a rounded box of the template's frame [aspect]. */
@Composable
private fun FrameBox(aspect: Float, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val safeAspect = if (aspect.isFinite() && aspect > 0f) aspect else 0.75f
        val widerThanFrame = maxHeight.value > 0f && maxWidth / maxHeight > safeAspect
        Box(
            Modifier
                .aspectRatio(safeAspect, matchHeightConstraintsFirst = widerThanFrame)
                .clip(RoundedCornerShape(16.dp)),
        ) { content() }
    }
}

private val PreviewBackground = Brush.verticalGradient(listOf(Color(0xFF30334A), Color(0xFF1E2029), Color(0xFF17181D)))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(pose: PoseTemplate) {
    Text(pose.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoPill { Text(pose.category.emoji, fontSize = 14.sp); Text(pose.category.label) }
        InfoPill {
            DifficultyDots(pose.difficulty)
            Text(Difficulty.label(pose.difficulty))
        }
        // "Couple" is already said by the Couple category.
        if (peopleLabel(pose.personCount) != pose.category.label) {
            InfoPill {
                Icon(peopleIcon(pose.personCount), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(peopleLabel(pose.personCount))
            }
        }
        when (pose.source) {
            PoseSource.FROM_PHOTO -> InfoPill(accent = true) { Text("From your photo") }
            PoseSource.DRAWN -> InfoPill(accent = true) { Text("Drawn by you") }
            PoseSource.BUILT_IN -> Unit
        }
    }
    if (pose.description.isNotBlank()) {
        Spacer(Modifier.height(14.dp))
        Text(
            pose.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoPill(accent: Boolean = false, content: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (accent) PoseitColors.Violet.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) { content() }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun TipsCard(tips: List<String>) {
    SectionCard("How to do it") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tips.forEachIndexed { index, tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(PoseitColors.Violet.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = Color(0xFFCFC3FF))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(tip, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun CameraSetupCard(camera: CameraAdvice) {
    SectionCard("Camera setup") {
        Column {
            SetupRow(Icons.Filled.Height, PoseitColors.Mint, camera.height.label, camera.height.advice)
            SetupDivider()
            SetupRow(Icons.Filled.CropFree, PoseitColors.Amber, camera.shot.label, shotAdvice(camera.shot))
            SetupDivider()
            if (camera.landscape) {
                SetupRow(Icons.Filled.StayCurrentLandscape, PoseitColors.Coral, "Landscape", "Turn the phone sideways.")
            } else {
                SetupRow(Icons.Filled.StayCurrentPortrait, PoseitColors.Coral, "Portrait", "Hold the phone upright.")
            }
            val note = camera.note
            if (!note.isNullOrBlank()) {
                SetupDivider()
                SetupRow(Icons.Filled.Lightbulb, PoseitColors.Violet, "Photographer tip", note)
            }
        }
    }
}

private fun shotAdvice(shot: ShotType): String = when (shot) {
    ShotType.FULL_BODY -> "Head to toe, with a little space above the head and below the feet."
    ShotType.THREE_QUARTER -> "Frame from the head to just above the knees. Never crop right at a joint."
    ShotType.HALF_BODY -> "Frame from the head down to the waist."
    ShotType.CLOSE_UP -> "Head and shoulders fill the frame. Focus on the eyes."
}

@Composable
private fun SetupDivider() {
    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
}

@Composable
private fun SetupRow(icon: ImageVector, accent: Color, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(pose: PoseTemplate) {
    Text("Good for", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Places first, then moods, each in declaration order.
        pose.tags.sortedWith(compareBy({ !it.isLocation }, { it.ordinal })).forEach { tag ->
            InfoPill {
                if (tag.isLocation) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = PoseitColors.Coral)
                } else {
                    moodEmoji(tag)?.let { Text(it, fontSize = 14.sp) }
                }
                Text(tag.label)
            }
        }
    }
}
