package com.poseit.app.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.CameraSetup
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.PoseSteps
import com.poseit.app.ui.components.StudioAlertDialog
import com.poseit.app.ui.components.StudioIconToggle
import com.poseit.app.ui.components.StudioSnackbarHost
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioTabs
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.library.Difficulty
import com.poseit.app.ui.library.DifficultyMeter
import com.poseit.app.ui.library.categoryText
import com.poseit.app.ui.library.isCustom
import com.poseit.app.ui.library.peopleLabel
import com.poseit.app.ui.library.sourceText
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import java.io.File
import kotlinx.coroutines.launch

/**
 * Pose details: big preview, tips, camera advice, favourite, "Try this pose".
 * @param onTryPose opens the camera with the pose id, mirrored when the preview's Mirror toggle is on
 */
@Composable
fun PoseDetailScreen(
    poseId: String,
    onBack: () -> Unit,
    onTryPose: (id: String, mirrored: Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit = onBack,
    message: String? = null,
    onMessageShown: () -> Unit = {},
) {
    val appContext = LocalContext.current.applicationContext
    val vm: PoseDetailViewModel = viewModel(
        key = "pose_detail_$poseId",
        factory = viewModelFactory { initializer { PoseDetailViewModel(poseId, appContext.appContainer.poseRepository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onDeleted() }

    PoseDetailContent(
        state = state,
        onBack = onBack,
        onTryPose = onTryPose,
        onEdit = onEdit,
        onToggleFavorite = vm::toggleFavorite,
        onDelete = vm::delete,
        message = message,
        onMessageShown = onMessageShown,
    )
}

/** The same names as the camera's guide menu and the create review. */
private enum class PreviewMode(val label: String) { MANNEQUIN("Mannequin"), PHOTO("Photo"), OUTLINE("Photo outline") }

@Composable
private fun PoseDetailContent(
    state: PoseDetailUiState,
    onBack: () -> Unit,
    onTryPose: (id: String, mirrored: Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
) {
    // Here, not in the preview, so "Try this pose" opens the camera with the orientation the user picked.
    var mirrored by rememberSaveable { mutableStateOf(false) }
    val ready = state as? PoseDetailUiState.Ready
    val pose = ready?.details?.pose
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val previewHeight = previewHeight()
    // The name moves into the top bar once the big one has scrolled away under it.
    val showTitle by remember(previewHeight) {
        derivedStateOf { scroll.value > with(density) { previewHeight.toPx() } }
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val messageScope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message != null) {
            // Shown in its own scope: clearing the message restarts this effect, which must not cut the snackbar off.
            onMessageShown()
            messageScope.launch { snackbar.showSnackbar(message) }
        }
    }

    Scaffold(
        containerColor = Studio.Paper,
        snackbarHost = { StudioSnackbarHost(snackbar) },
        topBar = {
            StudioTopBar(title = if (showTitle && pose != null) pose.name else "", onBack = onBack) {
                if (ready != null && pose != null) {
                    // Mirrors the preview, and "Try this pose" opens the camera the same way round.
                    StudioIconToggle(
                        icon = Icons.Outlined.Flip,
                        contentDescription = "Mirror",
                        checked = mirrored,
                        onCheckedChange = { mirrored = it },
                        modifier = Modifier.semantics { stateDescription = if (mirrored) "Mirrored" else "Original" },
                    )
                    StudioIconButton(
                        icon = if (ready.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (ready.isFavorite) "Remove ${pose.name} from favourites" else "Add ${pose.name} to favourites",
                        onClick = onToggleFavorite,
                    )
                }
            }
        },
        bottomBar = {
            if (pose != null) {
                DetailBottomBar(
                    isCustom = pose.isCustom,
                    onEdit = { onEdit(pose.id) },
                    onTry = { onTryPose(pose.id, mirrored) },
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
            ) { CircularProgressIndicator(color = Studio.Ink, strokeWidth = 2.dp) }

            PoseDetailUiState.NotFound -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EmptyState(
                    title = "Pose not found",
                    message = "It may have been deleted, or it isn't on this device. Pick another one in the library.",
                    action = { PrimaryButton("Back to the library", onClick = onBack) },
                )
            }

            is PoseDetailUiState.Ready -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scroll)
                    .padding(horizontal = Space.gutter),
            ) {
                val details = state.details
                PosePreview(
                    details = details,
                    height = previewHeight,
                    mirrored = mirrored,
                )
                Spacer(Modifier.height(Space.xl))
                DetailHeader(details.pose)
                if (details.pose.tips.isNotEmpty()) {
                    Spacer(Modifier.height(Space.section))
                    PoseSteps(details.pose.tips)
                }
                Spacer(Modifier.height(Space.section))
                CameraSetup(details.pose.camera)
                if (details.pose.tags.isNotEmpty()) {
                    Spacer(Modifier.height(Space.section))
                    GoodFor(details.pose)
                }
                if (details.pose.isCustom) {
                    Spacer(Modifier.height(Space.xl))
                    // Pulled left by the text button's 12dp inner padding so the label starts on the gutter.
                    DestructiveButton(
                        "Delete pose",
                        onClick = { confirmDelete = true },
                        modifier = Modifier.offset(x = (-12).dp),
                    )
                }
                Spacer(Modifier.height(Space.xl))
            }
        }
    }

    if (confirmDelete && pose != null) {
        StudioAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = "Delete this pose?",
            text = "“${pose.name}” will be removed from My poses. This can't be undone.",
            confirmButton = {
                DestructiveButton(
                    "Delete pose",
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    filled = true,
                )
            },
            dismissButton = { TertiaryButton("Cancel", onClick = { confirmDelete = false }) },
        )
    }
}

@Composable
private fun previewHeight() = LocalConfiguration.current.screenHeightDp.let { h -> (h * 0.48f).coerceIn(260f, 540f).dp }

/** "Customise" (or "Edit") and the screen's one primary action, on the backdrop below the content. */
@Composable
private fun DetailBottomBar(isCustom: Boolean, onEdit: () -> Unit, onTry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Studio.Paper)
            .navigationBarsPadding()
            .padding(horizontal = Space.gutter, vertical = Space.m),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryButton(if (isCustom) "Edit" else "Customise", onClick = onEdit)
        PrimaryButton("Try this pose", onClick = onTry, modifier = Modifier.weight(1f), icon = Icons.Outlined.CameraAlt)
    }
}

/**
 * The pose on its patch of studio paper, with viewfinder frame marks at the corners of its photo frame. Below it,
 * for a pose made from a photo, the Mannequin / Photo / Photo outline tabs.
 */
@Composable
private fun PosePreview(details: PoseDetails, height: Dp, mirrored: Boolean) {
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
    val flip by animateFloatAsState(
        targetValue = if (mirrored) -1f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "mirror",
    )

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(StudioShapes.Medium)
                .background(Studio.Set),
        ) {
            Crossfade(targetState = mode, animationSpec = tween(200), label = "previewMode") { m ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(Space.xl)
                        .graphicsLayer { scaleX = flip },
                    contentAlignment = Alignment.Center,
                ) {
                    FrameBox(pose.frameAspect) {
                        when (m) {
                            PreviewMode.MANNEQUIN -> PoseThumbnail(
                                template = pose,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = "Mannequin showing the ${pose.name} pose" },
                                background = null,
                            )
                            PreviewMode.PHOTO -> AsyncImage(
                                model = details.photoPath?.let(::File),
                                contentDescription = "Your reference photo for ${pose.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            PreviewMode.OUTLINE -> {
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
            }
        }
        if (modes.size > 1) {
            Spacer(Modifier.height(Space.s))
            StudioTabs(
                labels = modes.map { it.label },
                selectedIndex = modes.indexOf(mode),
                onSelect = { selected = modes[it] },
            )
        }
    }
}

/** Letterboxes [content] into a box of the template's frame [aspect], with frame marks at its corners. */
@Composable
private fun FrameBox(aspect: Float, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val safeAspect = if (aspect.isFinite() && aspect > 0f) aspect else 0.75f
        val widerThanFrame = maxHeight.value > 0f && maxWidth / maxHeight > safeAspect
        Box(
            Modifier
                .aspectRatio(safeAspect, matchHeightConstraintsFirst = widerThanFrame)
                .frameMarks(),
        ) { content() }
    }
}

/** Name in the display cut, then the facts as separate quiet words, then the description. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(pose: PoseTemplate) {
    Text(pose.name, style = StudioType.display, color = Studio.Ink, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Space.m))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // A couple or group says how many people instead; a user-made pose without a category says where it came
        // from instead.
        val peopleCategory = pose.category == PoseCategory.COUPLE || pose.category == PoseCategory.GROUP
        when {
            peopleCategory -> Fact(peopleLabel(pose.personCount))
            !(pose.isCustom && pose.category == PoseCategory.CUSTOM) -> Fact(pose.categoryText)
        }
        val difficulty = Difficulty.label(pose.difficulty)
        Row(
            Modifier.semantics(mergeDescendants = true) { contentDescription = "Difficulty: $difficulty" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Fact(difficulty)
            Spacer(Modifier.width(Space.s))
            DifficultyMeter(pose.difficulty)
        }
        if (!peopleCategory) Fact(peopleLabel(pose.personCount))
        pose.sourceText?.let { Fact(it) }
    }
    if (pose.description.isNotBlank()) {
        Spacer(Modifier.height(Space.l))
        Text(pose.description, style = StudioType.body, color = Studio.Ink)
    }
}

@Composable
private fun Fact(text: String) {
    Text(text, style = StudioType.body, color = Studio.Graphite)
}

/** Places first, then moods, as one plain phrase: "Beach, city street, casual". */
@Composable
private fun GoodFor(pose: PoseTemplate) {
    val labels = pose.tags.sortedWith(compareBy({ !it.isLocation }, { it.ordinal })).map { it.label }
    val phrase = labels.mapIndexed { i, label -> if (i == 0) label else label.lowercase() }.joinToString(", ")
    SectionHeading("Good for")
    Spacer(Modifier.height(Space.xs))
    Text(phrase, style = StudioType.body, color = Studio.Ink)
}
