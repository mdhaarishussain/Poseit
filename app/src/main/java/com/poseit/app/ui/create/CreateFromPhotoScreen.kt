package com.poseit.app.ui.create

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.dsl.pose
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.Banner
import com.poseit.app.ui.components.BannerStatus
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FillingCardStack
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.NumberedSteps
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.detail.PinnedActions
import com.poseit.app.ui.detail.ScrollingStackScreen
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.launch

/** The screen's name, in the top bar card of every step but the review. */
private const val TITLE = "Pose from a photo"

/** Pick a reference photo, detect the pose(s) in it and save as a custom pose. */
@Composable
fun CreateFromPhotoScreen(onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { context.appContainer.poseRepository }
    val viewModel: CreateFromPhotoViewModel = viewModel { CreateFromPhotoViewModel(context.applicationContext, repository) }
    val state = viewModel.state
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentOnSaved by rememberUpdatedState(onSaved)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }
    fun pickPhoto() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateEvent.Saved -> currentOnSaved(event.id)
                is CreateEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
            }
        }
    }

    // Back from a result goes back to the explainer first; saving can't be interrupted.
    val inFlow = state.step != CreateStep.INTRO
    // Enabled while saving too, so system Back is swallowed instead of popping the screen mid-save (which would
    // cancel the save and delete its files).
    BackHandler(enabled = inFlow || state.saving) { if (!state.saving) viewModel.reset() }
    val back: () -> Unit = { if (inFlow && !state.saving) viewModel.reset() else if (!state.saving) onBack() }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = { fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)) togetherWith fadeOut(PoseitMotion.standard(PoseitMotion.FAST)) },
        contentKey = { step -> if (step == CreateStep.LOADING) CreateStep.DETECTING else step },
        modifier = Modifier
            .fillMaxSize()
            .background(PoseitColors.backgroundBody),
        label = "createStep",
    ) { step ->
        when (step) {
            CreateStep.INTRO -> IntroContent(onBack = back, onPick = ::pickPhoto, snackbarHostState = snackbar)
            CreateStep.LOADING, CreateStep.DETECTING -> WorkingContent(state, onBack = back, snackbarHostState = snackbar)
            CreateStep.NO_PERSON -> ProblemContent(
                photo = state.photo,
                aspect = state.imageAspect,
                title = "No one found in this photo",
                message = "Try a photo where the person is clearly visible and not too small, ideally head to toe in good light.",
                onPick = ::pickPhoto,
                onBack = back,
                snackbarHostState = snackbar,
            )
            CreateStep.ERROR -> ProblemContent(
                photo = state.photo,
                aspect = state.imageAspect,
                title = "Couldn't trace a pose",
                message = state.errorMessage ?: "Try another photo.",
                onPick = ::pickPhoto,
                onBack = back,
                snackbarHostState = snackbar,
            )
            CreateStep.REVIEW -> ReviewContent(
                viewModel,
                onPickAnother = ::pickPhoto,
                onBack = back,
                snackbarHostState = snackbar,
                bottomBar = {
                    ReviewBottomBar(
                        saving = state.saving,
                        canSave = viewModel.name.isNotBlank() && state.selected.isNotEmpty(),
                        onSave = viewModel::save,
                    )
                },
            )
        }
    }
}

// ---- intro -------------------------------------------------------------------------------------------------------

/** An original demo pose (waving, hand on hip) for the explainer diagram. */
private val demoPose: PoseTemplate by lazy {
    pose("intro_demo", "Demo", PoseCategory.CUSTOM) {
        // Anchored at the top so the raised hand stays inside the frame.
        figure(cx = 0.5f, top = 0.06f, bodyHeight = 0.72f) {
            facing = 12f
            headTilt = 6f
            leftArm(upper = 145f, fore = 172f)
            rightArm(upper = -38f, fore = 58f)
            leftLeg(thigh = 6f, shin = 1f)
            rightLeg(thigh = -14f, shin = -5f)
        }
    }
}

/**
 * The explainer, as the mockups' stack: the top bar card fused to the headline card ("Recreate any photo" in
 * Display Light) and to the diagram card (a photo, then the pose traced from it, with the privacy line at its
 * foot). The steps follow on the canvas; "Choose a photo" is pinned at the foot.
 */
@Composable
internal fun IntroContent(onBack: () -> Unit = {}, onPick: () -> Unit = {}, snackbarHostState: SnackbarHostState? = null) {
    ScrollingStackScreen(
        pinnedTitle = TITLE,
        title = TITLE,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        bottomBar = { PinnedActions { AccentButton("Choose a photo", onClick = onPick, modifier = Modifier.fillMaxWidth()) } },
        joined = {
            CardSegment(contentPadding = PaddingValues(start = Space.cardPadding, end = Space.cardPadding, top = Space.l + Space.xs, bottom = Space.cardPadding)) {
                Text(
                    "Recreate any photo",
                    style = PoseitType.display,
                    color = PoseitColors.textPrimaryInverted,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Space.s))
                Text(
                    "Pick a picture you like and Poseit traces the pose, so you can match it with the camera.",
                    style = PoseitType.body,
                    color = PoseitColors.textSecondaryInverted,
                )
            }
            CardSegment(contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.l, bottom = Space.l + Space.xs)) {
                IntroDiagram(Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.l))
                Row(Modifier.padding(horizontal = Space.s), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = PoseitColors.textSecondaryInverted, modifier = Modifier.size(IconSize.inline))
                    Spacer(Modifier.width(Space.s))
                    Text(
                        "Everything happens on your phone. Your photo is never uploaded.",
                        style = PoseitType.supporting,
                        color = PoseitColors.textSecondaryInverted,
                    )
                }
            }
        },
    ) {
        Spacer(Modifier.height(Space.l))
        NumberedSteps(
            listOf(
                "Choose a photo where the whole body is in view",
                "Choose who to keep, up to four people",
                "Save it, then line up with it in the camera",
            ),
            Modifier.padding(horizontal = Space.textInset),
        )
        Spacer(Modifier.height(Space.xl))
    }
}

/**
 * Two raised tiles on the dark card: a photo (a mist frame with a faint figure in it), then the traced mannequin
 * standing in the same frame, marked with cream viewfinder frame marks.
 */
@Composable
private fun IntroDiagram(modifier: Modifier = Modifier) {
    val aspect = demoPose.frameAspect
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        DiagramTile(Modifier.weight(1f)) {
            Box(
                Modifier
                    .aspectRatio(aspect)
                    .border(1.5.dp, PoseitColors.borderEmphasizedInverted),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawPoseFigure(
                        demoPose.figures.first(), aspect, Rect(Offset.Zero, size),
                        FigurePaint(fill = PoseitColors.textSecondaryInverted, outline = Color.Transparent, alpha = 0.35f, showFaceMarker = false),
                    )
                }
            }
        }
        DiagramTile(Modifier.weight(1f)) {
            Box(
                Modifier
                    .aspectRatio(aspect)
                    .frameMarks(),
            ) {
                PoseThumbnail(demoPose, Modifier.fillMaxSize(), background = null)
            }
        }
    }
}

@Composable
private fun DiagramTile(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .aspectRatio(3f / 4f)
            .clip(PoseitShapes.Control)
            .background(PoseitColors.backgroundCardRaised)
            .padding(horizontal = Space.l, vertical = Space.xl),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ---- working / problems -----------------------------------------------------------------------------------------

/**
 * The picked photo fitted and centred in the width it is given (at most [heightLimit] tall), its corners rounded to
 * sit inside a dark card. [overlay] is drawn over the photo itself (same bounds), e.g. the traced skeletons.
 */
@Composable
internal fun PhotoStage(
    photo: Bitmap,
    aspect: Float,
    modifier: Modifier = Modifier,
    heightLimit: Dp = Dp.Unspecified,
    contentDescription: String? = "Selected photo",
    alpha: Float = 1f,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val image = remember(photo) { photo.asImageBitmap() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val limit = if (heightLimit != Dp.Unspecified) minOf(heightLimit, maxHeight) else maxHeight
        val natural = width / aspect
        val stageHeight = if (limit == Dp.Infinity) natural else minOf(natural, limit)
        Box(
            Modifier
                .fillMaxWidth()
                .height(stageHeight),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .aspectRatio(aspect, matchHeightConstraintsFirst = width / stageHeight > aspect)
                    .clip(PoseitShapes.Control),
            ) {
                Image(
                    image,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.FillBounds,
                    alpha = alpha,
                    modifier = Modifier.fillMaxSize(),
                )
                overlay()
            }
        }
    }
}

/**
 * While the photo opens and the pose is traced: one stack filling the screen, the top bar card fused to the photo
 * card and to a status card with the spinner.
 */
@Composable
internal fun WorkingContent(state: CreateUiState, onBack: () -> Unit = {}, snackbarHostState: SnackbarHostState? = null) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PoseitColors.backgroundBody)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        FillingCardStack(
            modifier = Modifier
                .fillMaxSize()
                .padding(Space.edge),
            fillIndex = 1,
            cards = listOf<@Composable () -> Unit>(
                { TopBarContent(TITLE, onBack = onBack) },
                {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(Space.m),
                        contentAlignment = Alignment.Center,
                    ) {
                        state.photo?.let { PhotoStage(it, state.imageAspect) }
                    }
                },
                {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.cardPadding, vertical = Space.l + Space.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PoseitSpinner(size = 20.dp, color = PoseitColors.accent)
                        Spacer(Modifier.width(Space.l))
                        Column {
                            Text(
                                if (state.step == CreateStep.LOADING) "Opening your photo…" else "Tracing the pose…",
                                style = PoseitType.bodyLarge,
                                color = PoseitColors.textPrimaryInverted,
                            )
                            Text("This only takes a moment", style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                        }
                    }
                },
            ),
        )
        if (snackbarHostState != null) PoseitSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * No person found, or tracing failed: the photo in the stack under the top bar, then an error banner saying what
 * happened and what to try, with "Choose another photo" pinned at the foot.
 */
@Composable
internal fun ProblemContent(
    photo: Bitmap?,
    aspect: Float,
    title: String,
    message: String,
    onPick: () -> Unit,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
) {
    ScrollingStackScreen(
        pinnedTitle = TITLE,
        title = TITLE,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        bottomBar = { PinnedActions { AccentButton("Choose another photo", onClick = onPick, modifier = Modifier.fillMaxWidth()) } },
        joined = {
            if (photo != null) {
                CardSegment(contentPadding = PaddingValues(Space.m)) { PhotoStage(photo, aspect, heightLimit = 280.dp) }
            }
        },
    ) {
        Spacer(Modifier.height(Space.section))
        Banner(
            message = message,
            title = title,
            status = BannerStatus.Error,
            modifier = Modifier
                .padding(horizontal = Space.edge)
                .semantics { heading() },
        )
        Spacer(Modifier.height(Space.xl))
    }
}
