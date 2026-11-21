package com.poseit.app.ui.create

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poseit.app.appContainer
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.dsl.pose
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.NumberedSteps
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.StudioInlineIconSize
import com.poseit.app.ui.components.StudioSnackbarHost
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch

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

    Scaffold(
        topBar = {
            StudioTopBar(
                title = if (state.step == CreateStep.REVIEW) "Your new pose" else "Pose from a photo",
                onBack = { if (inFlow && !state.saving) viewModel.reset() else if (!state.saving) onBack() },
            )
        },
        bottomBar = {
            if (state.step == CreateStep.INTRO) {
                PinnedActionBar { PrimaryButton("Choose a photo", onClick = ::pickPhoto, modifier = Modifier.fillMaxWidth()) }
            } else if (state.step == CreateStep.REVIEW) {
                ReviewBottomBar(
                    saving = state.saving,
                    canSave = viewModel.name.isNotBlank() && state.selected.isNotEmpty(),
                    onSave = viewModel::save,
                )
            }
        },
        snackbarHost = { StudioSnackbarHost(snackbar) },
        containerColor = Studio.Paper,
    ) { padding ->
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            contentKey = { step -> if (step == CreateStep.LOADING) CreateStep.DETECTING else step },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "createStep",
        ) { step ->
            when (step) {
                CreateStep.INTRO -> IntroContent()
                CreateStep.LOADING, CreateStep.DETECTING -> WorkingContent(state)
                CreateStep.NO_PERSON -> ProblemContent(
                    photo = state.photo,
                    aspect = state.imageAspect,
                    title = "No one found in this photo",
                    message = "Try a photo where the person is clearly visible and not too small, ideally head to toe in good light.",
                    onPick = ::pickPhoto,
                )
                CreateStep.ERROR -> ProblemContent(
                    photo = state.photo,
                    aspect = state.imageAspect,
                    title = "Couldn't trace a pose",
                    message = state.errorMessage ?: "Try another photo.",
                    onPick = ::pickPhoto,
                )
                CreateStep.REVIEW -> ReviewContent(viewModel, onPickAnother = ::pickPhoto)
            }
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

@Composable
internal fun IntroContent() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = Space.s, bottom = Space.xl),
    ) {
        Column(Modifier.widthIn(max = 560.dp)) {
            IntroDiagram(Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.section))
            Text(
                "Recreate any photo",
                style = StudioType.display,
                color = Studio.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Space.s))
            Text(
                "Pick a picture you like and Poseit traces the pose, so you can match it with the camera.",
                style = StudioType.body,
                color = Studio.Graphite,
            )
            Spacer(Modifier.height(Space.l))
            NumberedSteps(
                listOf(
                    "Choose a photo where the whole body is in view",
                    "Choose who to keep, up to four people",
                    "Save it, then line up with it in the camera",
                ),
            )
            Spacer(Modifier.height(Space.xl))
            // The main button is pinned below; the privacy note stays with the explanation.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Studio.Graphite, modifier = Modifier.size(StudioInlineIconSize))
                Spacer(Modifier.width(Space.s))
                Text(
                    "Everything happens on your phone. Your photo is never uploaded.",
                    style = StudioType.label,
                    color = Studio.Graphite,
                )
            }
        }
    }
}

/**
 * Two Set tiles side by side: a photo (a Graphite frame with a faint figure in it), then the traced mannequin
 * standing in the same frame, marked with viewfinder frame marks.
 */
@Composable
private fun IntroDiagram(modifier: Modifier = Modifier) {
    val aspect = demoPose.frameAspect
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        DiagramTile(Modifier.weight(1f)) {
            Box(
                Modifier
                    .aspectRatio(aspect)
                    .border(1.5.dp, Studio.Graphite),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawPoseFigure(
                        demoPose.figures.first(), aspect, Rect(Offset.Zero, size),
                        FigurePaint(fill = Studio.Graphite, outline = Color.Transparent, alpha = 0.3f, showFaceMarker = false),
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
            .clip(StudioShapes.Medium)
            .background(Studio.Set)
            .padding(horizontal = Space.l, vertical = Space.gutter),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ---- working / problems -----------------------------------------------------------------------------------------

/**
 * The picked photo on a Set-coloured stage that fills the content width, fitted and centred, at most [heightLimit]
 * tall. [overlay] is drawn over the photo itself (same bounds), e.g. the traced skeletons.
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
                .height(stageHeight)
                .clip(StudioShapes.Medium)
                .background(Studio.Set),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.aspectRatio(aspect, matchHeightConstraintsFirst = width / stageHeight > aspect)) {
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

@Composable
internal fun WorkingContent(state: CreateUiState) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Space.gutter)
            .padding(top = Space.s, bottom = Space.xl)
            .navigationBarsPadding(),
    ) {
        val photo = state.photo
        if (photo != null) {
            PhotoStage(photo, state.imageAspect, Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(Space.xl))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Studio.Ink, strokeWidth = 2.dp)
            Spacer(Modifier.width(Space.m))
            Text(
                if (state.step == CreateStep.LOADING) "Opening your photo…" else "Tracing the pose…",
                style = StudioType.bodyStrong,
                color = Studio.Ink,
            )
        }
        Text(
            "This only takes a moment",
            style = StudioType.label,
            color = Studio.Graphite,
            // In line with the status text, past the spinner.
            modifier = Modifier.padding(start = 18.dp + Space.m),
        )
    }
}

@Composable
internal fun ProblemContent(
    photo: Bitmap?,
    aspect: Float,
    title: String,
    message: String,
    onPick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = Space.s, bottom = Space.xl)
            .navigationBarsPadding(),
    ) {
        if (photo != null) {
            PhotoStage(photo, aspect, heightLimit = 280.dp)
            Spacer(Modifier.height(Space.xl))
        }
        Text(title, style = StudioType.title, color = Studio.Ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(message, style = StudioType.body, color = Studio.Graphite, modifier = Modifier.widthIn(max = 420.dp))
        Spacer(Modifier.height(Space.xl))
        PrimaryButton("Choose another photo", onClick = onPick, modifier = Modifier.fillMaxWidth())
    }
}
