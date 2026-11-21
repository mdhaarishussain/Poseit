package com.poseit.app.ui.gallery

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.poseit.app.appContainer
import com.poseit.app.data.SavedPhoto
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.Banner
import com.poseit.app.ui.components.ButtonForm
import com.poseit.app.ui.components.ButtonSize
import com.poseit.app.ui.components.EmptyPictureCard
import com.poseit.app.ui.components.FillingCardStack
import com.poseit.app.ui.components.PinnedTopBar
import com.poseit.app.ui.components.rememberTopBarScrolledAway
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import kotlinx.coroutines.launch

/*
 * Your photos (docs/DESIGN.md, "Screen map"): the top bar card with the photo count, fused to a dark card that starts
 * a review, then the photos under a heading per day as 3:4 tiles with 20dp corners, 8dp apart. Empty, the top bar is
 * fused to a dark card that says what to do. The full-screen viewer opens over it.
 */

/** Photos taken with Poseit, with a full-screen viewer, share and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onBack: () -> Unit, onOpenCamera: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { context.appContainer.photoRepository }
    val viewModel: GalleryViewModel = viewModel { GalleryViewModel(repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The viewer is part of this screen; remember the photo it shows across configuration changes.
    var viewerUri by rememberSaveable { mutableStateOf<String?>(null) }
    val viewerPhoto = viewerUri?.let { uri -> state.photos.firstOrNull { it.uri.toString() == uri } }
    var viewerOpen by rememberSaveable { mutableStateOf(false) }

    // ---- storage permission (Android 12 and older) ------------------------------------------------------------
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    var permissionMissing by remember { mutableStateOf(repository.needsReadPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionMissing = !granted
        viewModel.refresh()
    }
    LaunchedEffect(Unit) {
        val permission = repository.readPermission
        if (permission != null && repository.needsReadPermission() && !askedPermission) {
            askedPermission = true
            permissionLauncher.launch(permission)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionMissing = repository.needsReadPermission()
        viewModel.refresh()
    }

    // ---- delete consent (files from a previous install) ----------------------------------------------------------
    var consentFor by remember { mutableStateOf<SavedPhoto?>(null) }
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        consentFor?.let { viewModel.onDeleteConsentResult(it, granted = result.resultCode == Activity.RESULT_OK) }
        consentFor = null
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Own coroutine: a visible message must not hold up a consent request behind it.
                is GalleryEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
                is GalleryEvent.RequestDeleteConsent -> {
                    consentFor = event.photo
                    try {
                        consentLauncher.launch(IntentSenderRequest.Builder(event.request.intentSender).build())
                    } catch (e: ActivityNotFoundException) {
                        consentFor = null
                        viewModel.onDeleteConsentResult(event.photo, granted = false)
                    }
                }
            }
        }
    }

    fun startSafely(intent: Intent, failure: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(failure) }
        }
    }

    fun requestPermission() {
        val permission = repository.readPermission ?: return
        val activity = context.findActivity()
        if (askedPermission && activity != null && !activity.shouldShowRequestPermissionRationale(permission)) {
            // "Don't ask again": the system dialog won't show any more, so open the app settings instead.
            startSafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                "Couldn't open settings. Allow photo access in Android Settings, Apps, Poseit.",
            )
        } else {
            askedPermission = true
            permissionLauncher.launch(permission)
        }
    }

    fun quickDelete(photo: SavedPhoto) {
        viewModel.deleteDeferred(photo)
        scope.launch {
            // Only the latest delete can be undone: finishing the previous snackbar commits that one.
            snackbar.currentSnackbarData?.dismiss()
            var undo = false
            try {
                undo = snackbar.showSnackbar("Photo deleted", actionLabel = "Undo", duration = SnackbarDuration.Short) ==
                    SnackbarResult.ActionPerformed
            } finally {
                if (undo) viewModel.undoDelete(photo.uri) else viewModel.commitPendingDelete(photo.uri)
            }
        }
    }

    // Close the viewer once the last photo is gone.
    LaunchedEffect(viewerOpen, state.photos.isEmpty(), state.loading) {
        if (viewerOpen && !state.loading && state.photos.isEmpty()) viewerOpen = false
    }
    BackHandler(enabled = viewerOpen) { viewerOpen = false }
    fun openViewer(photo: SavedPhoto) {
        viewerUri = photo.uri.toString()
        viewerOpen = true
    }
    val gridState = rememberLazyGridState()
    val barAway = rememberTopBarScrolledAway(gridState) && state.photos.isNotEmpty() && !state.loading
    Box(Modifier.fillMaxSize().background(PoseitColors.backgroundBody)) {
        Scaffold(
            snackbarHost = { if (!viewerOpen) PoseitSnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
            containerColor = PoseitColors.backgroundBody,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        ) { padding ->
            val refreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh(userInitiated = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
                state = refreshState,
                // Below the status bar: the canvas band drawn over it would otherwise hide the spinner for most of a pull.
                indicator = {
                    RefreshIndicator(
                        refreshState,
                        state.refreshing,
                        Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars),
                    )
                },
            ) {
                when {
                    state.loading -> Column(Modifier.fillMaxSize().padding(top = topInset(), start = Space.edge, end = Space.edge)) {
                        GalleryHeader(photoCount = 0, onBack = onBack)
                        Box(Modifier.fillMaxWidth().padding(vertical = Space.xxl), contentAlignment = Alignment.Center) {
                            PoseitSpinner(color = PoseitColors.textPrimary)
                        }
                    }
                    state.photos.isEmpty() -> EmptyGallery(
                        permissionMissing = permissionMissing,
                        onGrantPermission = ::requestPermission,
                        onOpenCamera = onOpenCamera,
                        onBack = onBack,
                    )
                    else -> PhotoGrid(
                        state = gridState,
                        photos = state.photos,
                        permissionMissing = permissionMissing,
                        onGrantPermission = ::requestPermission,
                        onOpen = ::openViewer,
                        header = {
                            GalleryHeader(photoCount = state.photos.size, onBack = onBack, hideBarSemantics = barAway) {
                                ReviewCard(onReview = { openViewer(state.photos.first()) })
                            }
                        },
                    )
                }
            }
        }
        // The grid scrolls under the status bar onto this canvas band, never through it.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(PoseitColors.backgroundBody),
        )
        // Back and Review stay one tap away once the header has scrolled up under the status bar.
        PinnedTopBar(
            visible = barAway && !viewerOpen,
            title = "Your photos",
            onBack = onBack,
            actions = {
                AccentButton("Review", onClick = { state.photos.firstOrNull()?.let(::openViewer) }, size = ButtonSize.Small)
            },
        )

        AnimatedVisibility(
            visible = viewerOpen && state.photos.isNotEmpty(),
            enter = fadeIn(PoseitMotion.standard(PoseitMotion.MEDIUM)) +
                scaleIn(PoseitMotion.standard(PoseitMotion.MEDIUM), initialScale = 0.96f),
            exit = fadeOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit)) +
                scaleOut(tween(PoseitMotion.FAST, easing = PoseitMotion.EaseExit), targetScale = 0.96f),
        ) {
            PhotoViewer(
                photos = state.photos,
                initialPhoto = viewerPhoto,
                snackbarHostState = snackbar,
                onClose = { viewerOpen = false },
                onCurrentPhotoChanged = { viewerUri = it.uri.toString() },
                onShare = { startSafely(repository.shareIntent(it.uri), "No app available to share with") },
                onOpenExternally = { startSafely(repository.viewIntent(it.uri), "No gallery app found") },
                onDeleteConfirmed = { viewModel.deleteNow(it) },
                onQuickDelete = ::quickDelete,
                onReviewFinished = {
                    viewerOpen = false
                    scope.launch { snackbar.showSnackbar("That was the last photo") }
                },
            )
        }
    }
}

/** Space above the first card: the status bar plus the edge gap. */
@Composable
private fun topInset(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Space.edge

/** "12 photos", "1 photo". */
internal fun photoCountLabel(count: Int): String = if (count == 1) "1 photo" else "$count photos"

/**
 * The top bar card ("Your photos", back, the count as a lavender badge) with the cards in [joined] fused under it.
 * Place it [Space.edge] in from the screen sides.
 */
@Composable
internal fun GalleryHeader(
    photoCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hideBarSemantics: Boolean = false,
    joined: (@Composable () -> Unit)? = null,
) {
    ConnectedCardStack(modifier.fillMaxWidth()) {
        TopBarContent(
            title = "Your photos",
            // While the pinned bar stands in for it, keep a single Back in the tree.
            modifier = if (hideBarSemantics) Modifier.clearAndSetSemantics {} else Modifier,
            onBack = onBack,
            badge = if (photoCount > 0) {
                { Badge(photoCountLabel(photoCount)) }
            } else {
                null
            },
        )
        joined?.invoke()
    }
}

/**
 * The card under the top bar, like the mockups' "New challenge ... START": what Review does, and the block button that
 * starts it at the newest photo. The button sits beside the words while the title still fits on one line there, and
 * moves under them otherwise (large font sizes on a small phone).
 */
@Composable
internal fun ReviewCard(onReview: () -> Unit) {
    CardSegment {
        Layout(
            content = {
                Text("Keep the good ones", style = PoseitType.bodyLarge, color = PoseitColors.textPrimaryInverted)
                Text(
                    "Go through your photos one at a time and delete the rest",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondaryInverted,
                )
                AccentButton("Review", onClick = onReview, form = ButtonForm.Block)
            },
        ) { measurables, constraints ->
            val (title, subtitle, button) = measurables
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val b = button.measure(loose)
            val width = constraints.maxWidth
            val gap = Space.l.roundToPx()
            val besideWidth = width - b.width - gap
            val beside = besideWidth >= title.maxIntrinsicWidth(Constraints.Infinity)
            val textWidth = if (beside) besideWidth else width
            val t = title.measure(loose.copy(maxWidth = textWidth))
            val s = subtitle.measure(loose.copy(maxWidth = textWidth))
            val words = t.height + s.height
            if (beside) {
                val height = maxOf(words, b.height)
                val top = (height - words) / 2
                layout(width, height) {
                    t.place(0, top)
                    s.place(0, top + t.height)
                    b.place(width - b.width, (height - b.height) / 2)
                }
            } else {
                layout(width, words + gap + b.height) {
                    t.place(0, 0)
                    s.place(0, t.height)
                    b.place(0, words + gap)
                }
            }
        }
    }
}

/**
 * The photos, newest first, under a heading per day: 3:4 tiles (most photos are portraits, so the whole person shows)
 * with the tile radius, [Space.edge] apart and from the screen sides. [header] is the first, full-width item.
 */
@Composable
internal fun PhotoGrid(
    photos: List<SavedPhoto>,
    permissionMissing: Boolean,
    onGrantPermission: () -> Unit,
    onOpen: (SavedPhoto) -> Unit,
    header: (@Composable () -> Unit)? = null,
    state: LazyGridState = rememberLazyGridState(),
) {
    val days = remember(photos) { groupByDay(photos) }
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Space.edge,
            end = Space.edge,
            top = topInset(),
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Space.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(Space.edge),
        verticalArrangement = Arrangement.spacedBy(Space.edge),
    ) {
        if (header != null) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") { header() }
        }
        if (permissionMissing) {
            item(key = "permission", span = { GridItemSpan(maxLineSpan) }) {
                Banner(
                    message = "Some older photos are hidden. Allow photo access to see them all.",
                    // Pulled left by the text button's own padding, so its words line up with the message.
                    action = { TertiaryButton("Allow access", onClick = onGrantPermission, modifier = Modifier.offset(x = -Space.m)) },
                    modifier = Modifier.padding(top = if (header != null) Space.l else 0.dp),
                )
            }
        }
        days.forEachIndexed { index, day ->
            item(key = "day_${day.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "day") {
                SectionHeading(
                    day.label,
                    Modifier.padding(
                        start = Space.cardPadding,
                        end = Space.cardPadding,
                        top = if (index == 0) Space.l else Space.xl,
                    ),
                )
            }
            items(day.photos, key = { it.uri.toString() }, contentType = { "photo" }) { photo ->
                val request = remember(photo.uri) {
                    ImageRequest.Builder(context).data(photo.uri).crossfade(true).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = "Photo from ${formatPhotoDateTime(photo.dateTakenMillis, context)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(3f / 4f)
                        .clip(PoseitShapes.Tile)
                        .background(PoseitColors.backgroundMuted)
                        .clickable(onClickLabel = "View") { onOpen(photo) },
                )
            }
        }
    }
}

/** Nothing to show: the top bar fused to a dark card that says why and offers the one thing to do. */
@Composable
private fun EmptyGallery(
    permissionMissing: Boolean,
    onGrantPermission: () -> Unit,
    onOpenCamera: () -> Unit,
    onBack: () -> Unit,
) {
    // Scrollable so pull-to-refresh works and the message fits on small screens. The stack fills the screen: the top
    // bar, the card that says why it's empty, then a picture card that takes the rest, with the one action at its foot.
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val top = topInset()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fill = maxHeight - top - navBottom - Space.edge
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(top = top, start = Space.edge, end = Space.edge, bottom = navBottom + Space.edge),
        ) {
            val title = if (permissionMissing) "Let Poseit see your photos" else "No photos yet"
            val message = if (permissionMissing) {
                "Allow access to photos to see the shots you took with Poseit. They stay on this phone."
            } else {
                "Photos you take with the pose camera show up here, and in Pictures/Poseit on this phone."
            }
            FillingCardStack(
                modifier = Modifier.heightIn(min = fill),
                minFillHeight = 96.dp,
                cards = listOf<@Composable () -> Unit>(
                    { TopBarContent(title = "Your photos", onBack = onBack) },
                    {
                        CardSegment(contentPadding = PaddingValues(horizontal = Space.cardPadding)) {
                            EmptyState(title = title, message = message, horizontalPadding = 0.dp)
                        }
                    },
                    {
                        EmptyPictureCard(
                            action = {
                                if (permissionMissing) {
                                    AccentButton("Allow access", onClick = onGrantPermission, modifier = Modifier.fillMaxWidth())
                                } else {
                                    AccentButton("Open the pose camera", onClick = onOpenCamera, modifier = Modifier.fillMaxWidth())
                                }
                            },
                        )
                    },
                ),
            )
        }
    }
}

/**
 * The pull-to-refresh spinner on a small cream disc without a shadow, so it reads over the dark top bar card as well
 * as over the canvas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshIndicator(state: PullToRefreshState, refreshing: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.pullToRefreshIndicator(
            state = state,
            isRefreshing = refreshing,
            containerColor = PoseitColors.backgroundSurface,
            elevation = 0.dp,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (refreshing) {
            PoseitSpinner(color = PoseitColors.textPrimary)
        } else {
            CircularProgressIndicator(
                progress = { state.distanceFraction.coerceIn(0f, 1f) },
                modifier = Modifier.size(24.dp),
                color = PoseitColors.textPrimary,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
        }
    }
}
