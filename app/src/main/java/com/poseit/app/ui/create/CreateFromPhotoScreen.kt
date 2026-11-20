package com.poseit.app.ui.create

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.poseit.app.appContainer
import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.dsl.pose
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.theme.PoseitColors

/** Pick a reference photo, detect the pose(s) in it and save as a custom pose. */
@OptIn(ExperimentalMaterial3Api::class)
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
    BackHandler(enabled = inFlow && !state.saving) { viewModel.reset() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.step == CreateStep.REVIEW) "Your new pose" else "Pose from photo") },
                navigationIcon = {
                    IconButton(onClick = { if (inFlow && !state.saving) viewModel.reset() else if (!state.saving) onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (state.step == CreateStep.REVIEW) {
                ReviewBottomBar(
                    saving = state.saving,
                    canSave = viewModel.name.isNotBlank() && state.selected.isNotEmpty(),
                    onPickAnother = ::pickPhoto,
                    onSave = viewModel::save,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
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
                CreateStep.INTRO -> IntroContent(onPick = ::pickPhoto)
                CreateStep.LOADING, CreateStep.DETECTING -> WorkingContent(state)
                CreateStep.NO_PERSON -> ProblemContent(
                    photo = state.photo,
                    icon = { Icon(Icons.Rounded.PersonSearch, contentDescription = null, modifier = Modifier.size(40.dp), tint = PoseitColors.Amber) },
                    title = "We couldn't spot anyone",
                    message = "Try a photo where the person is clearly visible and not too small – ideally head to toe, with good light.",
                    onPick = ::pickPhoto,
                )
                CreateStep.ERROR -> ProblemContent(
                    photo = state.photo,
                    icon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(40.dp), tint = PoseitColors.Red) },
                    title = "Something went wrong",
                    message = state.errorMessage ?: "Please try another photo.",
                    onPick = ::pickPhoto,
                )
                CreateStep.REVIEW -> ReviewContent(viewModel)
            }
        }
    }
}

// ---- intro -------------------------------------------------------------------------------------------------------

/** An original demo figure (waving, hand on hip) for the explainer illustration. */
private val demoFigure: Skeleton by lazy {
    pose("intro_demo", "Demo", PoseCategory.CUSTOM) {
        // Anchored at the top so the raised hand stays inside the tile.
        figure(cx = 0.5f, top = 0.05f, bodyHeight = 0.7f) {
            facing = 12f
            headTilt = 6f
            leftArm(upper = 145f, fore = 172f)
            rightArm(upper = -38f, fore = 58f)
            leftLeg(thigh = 6f, shin = 1f)
            rightLeg(thigh = -14f, shin = -5f)
        }
    }.figures.first()
}

@Composable
private fun IntroContent(onPick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(PoseitColors.VioletDark.copy(alpha = 0.55f), PoseitColors.Surface, PoseitColors.Coral.copy(alpha = 0.22f)),
                        ),
                    )
                    .padding(24.dp),
            ) {
                IntroIllustration(Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                Text("Recreate any photo", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pick a picture you like – we'll trace the pose so you can match it with the camera.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(20.dp))
                IntroStep(1, "Choose a photo where the whole body is in view")
                IntroStep(2, "We trace up to four people in it")
                IntroStep(3, "Save it, then line up with it live in the camera")
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onPick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Choose a photo", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                "Everything happens on your phone. Your photo is never uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
private fun IntroStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PoseitColors.Violet.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** "Photo" tile -> arrow -> "traced" tile, drawn from the demo figure. */
@Composable
private fun IntroIllustration(modifier: Modifier = Modifier) {
    val aspect = 3f / 4f
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .weight(1f, fill = false)
                .widthIn(max = 120.dp)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFB38A), Color(0xFFFF7A59), Color(0xFF8E4FD8)))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // A "sun" and a person-shaped silhouette: an abstract photo.
                drawCircle(Color.White.copy(alpha = 0.55f), radius = size.width * 0.12f, center = Offset(size.width * 0.78f, size.height * 0.2f))
                drawPoseFigure(
                    demoFigure, aspect, Rect(Offset.Zero, size),
                    FigurePaint(style = FigureStyle.SILHOUETTE, fill = Color(0xFF2A1840), outline = Color.Transparent, showFaceMarker = false),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 14.dp).size(28.dp),
        )
        Box(
            Modifier
                .weight(1f, fill = false)
                .widthIn(max = 120.dp)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2B2E3A), Color(0xFF1C1E25)))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawPoseFigure(
                    demoFigure, aspect, Rect(Offset.Zero, size),
                    FigurePaint(
                        style = FigureStyle.STICK,
                        fill = Color.White,
                        partColors = mapOf(
                            BodyPart.LEFT_ARM to PoseitColors.Coral, BodyPart.LEFT_LEG to PoseitColors.Coral,
                            BodyPart.RIGHT_ARM to PoseitColors.Violet, BodyPart.RIGHT_LEG to PoseitColors.Violet,
                            BodyPart.TORSO to PoseitColors.Mint, BodyPart.HEAD to PoseitColors.Mint,
                        ),
                    ),
                )
            }
        }
    }
}

// ---- working / problems -----------------------------------------------------------------------------------------

@Composable
private fun WorkingContent(state: CreateUiState) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val photo = state.photo
        if (photo != null) {
            ScanningPhoto(photo, state.imageAspect, Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(24.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                if (state.step == CreateStep.LOADING) "Opening your photo…" else "Tracing the pose…",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "This only takes a moment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The photo with a soft scanning band sweeping over it. */
@Composable
private fun ScanningPhoto(photo: android.graphics.Bitmap, aspect: Float, modifier: Modifier = Modifier) {
    val image = remember(photo) { photo.asImageBitmap() }
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanProgress",
    )
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(aspect, matchHeightConstraintsFirst = maxWidth / maxHeight > aspect)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            Image(image, contentDescription = "Selected photo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.Black.copy(alpha = 0.25f))
                val y = size.height * progress
                val band = size.height * 0.12f
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, PoseitColors.Violet.copy(alpha = 0.45f), Color.Transparent),
                        startY = y - band,
                        endY = y + band,
                    ),
                    topLeft = Offset(0f, y - band),
                    size = Size(size.width, band * 2f),
                )
                drawLine(PoseitColors.Violet, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

@Composable
private fun ProblemContent(
    photo: android.graphics.Bitmap?,
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    onPick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (photo != null) {
            val image = remember(photo) { photo.asImageBitmap() }
            Image(
                image,
                contentDescription = "Selected photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(20.dp)),
                alpha = 0.6f,
            )
            Spacer(Modifier.height(24.dp))
        }
        icon()
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 380.dp),
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onPick) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pick another photo")
        }
    }
}
