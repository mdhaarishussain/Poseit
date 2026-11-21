package com.poseit.app.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.CameraSetup
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.CardVariant
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.IconToggle
import com.poseit.app.ui.components.PillTabs
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseSteps
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseitAlertDialog
import com.poseit.app.ui.components.PoseitCard
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.SegmentedProgress
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.library.Difficulty
import com.poseit.app.ui.library.isCustom
import com.poseit.app.ui.library.tileDetail
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
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
    val pose = (state as? PoseDetailUiState.Ready)?.details?.pose
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

    when (state) {
        PoseDetailUiState.Loading -> PlainScreen(onBack, snackbar) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PoseitSpinner(color = PoseitColors.textPrimary) }
        }

        PoseDetailUiState.NotFound -> PlainScreen(onBack, snackbar) {
            EmptyState(
                title = "Pose not found",
                message = "It may have been deleted, or it isn't on this device. Pick another one in the library.",
                action = { PrimaryButton("Back to the library", onClick = onBack) },
            )
        }

        is PoseDetailUiState.Ready -> ReadyContent(
            state = state,
            mirrored = mirrored,
            onMirroredChange = { mirrored = it },
            snackbar = snackbar,
            onBack = onBack,
            onTry = { onTryPose(state.details.pose.id, mirrored) },
            onEdit = { onEdit(state.details.pose.id) },
            onToggleFavorite = onToggleFavorite,
            onRequestDelete = { confirmDelete = true },
        )
    }

    if (confirmDelete && pose != null) {
        PoseitAlertDialog(
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

/** Loading and not found: the bare top bar card with Back, and [content] on the canvas. */
@Composable
private fun PlainScreen(onBack: () -> Unit, snackbar: SnackbarHostState, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = PoseitColors.backgroundBody,
        topBar = { TopBarCard(title = "", onBack = onBack) },
        snackbarHost = { PoseitSnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) { content() }
    }
}

/**
 * The pose, as the mockups' stack: the top bar card (Back, the category badge, the favourite heart) fused to the
 * name card (the name in Display Light with the facts as label / value pairs) and to the preview card (the figure
 * on the dark stage inside cream frame marks, the Mirror toggle in its corner). Below, on the canvas: the
 * description, the steps in a dark card, the camera setup in an outlined card, and what the pose is good for.
 * "Customise" and "Try this pose" stay pinned at the foot.
 */
@Composable
private fun ReadyContent(
    state: PoseDetailUiState.Ready,
    mirrored: Boolean,
    onMirroredChange: (Boolean) -> Unit,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onTry: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val details = state.details
    val pose = details.pose
    val favourite: @Composable RowScope.() -> Unit = {
        PlainIconButton(
            icon = if (state.isFavorite) PoseitIcons.favouriteOn else PoseitIcons.favouriteOff,
            contentDescription = if (state.isFavorite) "Remove ${pose.name} from favourites" else "Add ${pose.name} to favourites",
            onClick = onToggleFavorite,
        )
    }
    ScrollingStackScreen(
        pinnedTitle = pose.name,
        onBack = onBack,
        badge = { Badge(pose.tileDetail) },
        actions = favourite,
        snackbarHostState = snackbar,
        bottomBar = { DetailActions(isCustom = pose.isCustom, onEdit = onEdit, onTry = onTry) },
        joined = {
            NameCard(pose)
            PreviewCard(details, height = previewHeight(), mirrored = mirrored, onMirroredChange = onMirroredChange)
        },
    ) {
        if (pose.description.isNotBlank()) {
            Text(
                pose.description,
                style = PoseitType.body,
                color = PoseitColors.textPrimary,
                modifier = Modifier.padding(start = Space.textInset, end = Space.textInset, top = Space.section),
            )
        }
        Spacer(Modifier.height(Space.section))
        if (pose.tips.isNotEmpty()) {
            PoseitCard(
                modifier = Modifier
                    .padding(horizontal = Space.edge)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = Space.cardPadding, end = Space.cardPadding, top = Space.l + Space.xs, bottom = Space.l),
            ) { PoseSteps(pose.tips) }
            Spacer(Modifier.height(Space.edge))
        }
        PoseitCard(
            modifier = Modifier
                .padding(horizontal = Space.edge)
                .fillMaxWidth(),
            variant = CardVariant.Outlined,
            contentPadding = PaddingValues(start = Space.cardPadding, end = Space.cardPadding, top = Space.l + Space.xs, bottom = Space.s),
        ) { CameraSetup(pose.camera) }
        if (pose.tags.isNotEmpty()) {
            Spacer(Modifier.height(Space.section))
            GoodFor(pose, Modifier.padding(horizontal = Space.textInset))
        }
        if (pose.isCustom) {
            Spacer(Modifier.height(Space.l))
            // Pulled in by the text button's 12dp inner padding so the label starts on the text inset.
            DestructiveButton("Delete pose", onClick = onRequestDelete, modifier = Modifier.padding(start = Space.textInset - Space.m))
        }
        Spacer(Modifier.height(Space.xl))
    }
}

@Composable
private fun previewHeight() = LocalConfiguration.current.screenHeightDp.let { h -> (h * 0.44f).coerceIn(240f, 480f).dp }

/** "Customise" (or "Edit") beside the screen's one accent action, on the canvas band at the foot. */
@Composable
private fun DetailActions(isCustom: Boolean, onEdit: () -> Unit, onTry: () -> Unit) {
    PinnedActions {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.edge), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton(if (isCustom) "Edit" else "Customise", onClick = onEdit)
            AccentButton("Try this pose", onClick = onTry, modifier = Modifier.weight(1f), icon = PoseitIcons.camera)
        }
    }
}

/** The name in Display Light, then three facts as the mockups' small label / value pairs. */
@Composable
private fun NameCard(pose: PoseTemplate) {
    CardSegment(contentPadding = PaddingValues(start = Space.cardPadding, end = Space.cardPadding, top = Space.l + Space.xs, bottom = Space.cardPadding)) {
        Text(
            pose.name,
            style = PoseitType.display,
            color = PoseitColors.textPrimaryInverted,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Space.l + Space.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            Fact("Level", Difficulty.label(pose.difficulty), Modifier.weight(1f)) {
                Spacer(Modifier.height(Space.s))
                // One segment lit per level; decorative next to the word.
                SegmentedProgress(
                    progress = pose.difficulty.coerceIn(1, 3) / 3f,
                    segments = 3,
                    height = 6.dp,
                    emptyColor = PoseitColors.borderInverted,
                    modifier = Modifier
                        .width(56.dp)
                        .clearAndSetSemantics {},
                )
            }
            Fact("People", "${pose.personCount}", Modifier.weight(1f))
            Fact("Shot", pose.camera.shot.label, Modifier.weight(1f))
        }
    }
}

/** A label in Supporting mist over its value in Body large cream, read as one node ("Level, Easy"). */
@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier, extra: @Composable () -> Unit = {}) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted, maxLines = 1)
        Spacer(Modifier.height(Space.xs))
        Text(value, style = PoseitType.bodyLarge, color = PoseitColors.textPrimaryInverted, maxLines = 2)
        extra()
    }
}

/**
 * The pose on the dark stage, inside cream viewfinder frame marks at the corners of its photo frame, with the
 * Mirror toggle in the card's corner. For a pose made from a photo, the Mannequin / Photo / Photo outline tabs sit
 * at the card's foot.
 */
@Composable
private fun PreviewCard(details: PoseDetails, height: Dp, mirrored: Boolean, onMirroredChange: (Boolean) -> Unit) {
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
        animationSpec = PoseitMotion.standard(PoseitMotion.SLOW),
        label = "mirror",
    )

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            Crossfade(targetState = mode, animationSpec = PoseitMotion.standard(PoseitMotion.MEDIUM), label = "previewMode") { m ->
                Box(
                    Modifier
                        .fillMaxSize()
                        // The top leaves the Mirror toggle clear of the frame's corners.
                        .padding(start = Space.xl, end = Space.xl, top = Space.xxl + Space.s, bottom = if (modes.size > 1) Space.m else Space.xl)
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
            // Mirrors the preview, and "Try this pose" opens the camera the same way round.
            IconToggle(
                icon = Icons.Outlined.Flip,
                contentDescription = "Mirror",
                checked = mirrored,
                onCheckedChange = onMirroredChange,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Space.xs, end = Space.xs)
                    .semantics { stateDescription = if (mirrored) "Mirrored" else "Original" },
            )
        }
        if (modes.size > 1) {
            PillTabs(
                labels = modes.map { it.label },
                selectedIndex = modes.indexOf(mode),
                onSelect = { selected = modes[it] },
                edgePadding = Space.l,
                modifier = Modifier.padding(bottom = Space.l),
            )
        }
    }
}

/** Letterboxes [content] into a box of the template's frame [aspect], with cream frame marks at its corners. */
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

/** Places first, then moods, as one plain phrase: "Beach, city street, casual". */
@Composable
private fun GoodFor(pose: PoseTemplate, modifier: Modifier = Modifier) {
    val labels = pose.tags.sortedWith(compareBy({ !it.isLocation }, { it.ordinal })).map { it.label }
    val phrase = labels.mapIndexed { i, label -> if (i == 0) label else label.lowercase() }.joinToString(", ")
    Column(modifier) {
        SectionHeading("Good for")
        Text(phrase, style = PoseitType.body, color = PoseitColors.textPrimary)
    }
}

