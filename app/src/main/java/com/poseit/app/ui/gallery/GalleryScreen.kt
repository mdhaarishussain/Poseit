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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.StudioInlineIconSize
import com.poseit.app.ui.components.StudioSnackbarHost
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.launch

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
    Box(Modifier.fillMaxSize().background(Studio.Paper)) {
        Scaffold(
            topBar = {
                StudioTopBar(title = "Your photos", onBack = onBack) {
                    if (state.photos.isNotEmpty()) {
                        TertiaryButton(
                            text = "Review",
                            onClick = {
                                viewerUri = state.photos.first().uri.toString()
                                viewerOpen = true
                            },
                        )
                    }
                }
            },
            snackbarHost = { if (!viewerOpen) StudioSnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
            containerColor = Studio.Paper,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        ) { padding ->
            val refreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh(userInitiated = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
                state = refreshState,
                // A bare Ink spinner, like every other one in the app: no disc, so no shadow.
                indicator = {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .pullToRefreshIndicator(
                                state = refreshState,
                                isRefreshing = state.refreshing,
                                containerColor = Color.Transparent,
                                elevation = 0.dp,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.refreshing) {
                            CircularProgressIndicator(Modifier.size(28.dp), color = Studio.Ink, strokeWidth = 2.dp)
                        } else {
                            CircularProgressIndicator(
                                progress = { refreshState.distanceFraction.coerceIn(0f, 1f) },
                                modifier = Modifier.size(28.dp),
                                color = Studio.Ink,
                                strokeWidth = 2.dp,
                                trackColor = Color.Transparent,
                            )
                        }
                    }
                },
            ) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Studio.Ink, strokeWidth = 2.dp)
                    }
                    state.photos.isEmpty() -> EmptyGallery(
                        permissionMissing = permissionMissing,
                        onGrantPermission = ::requestPermission,
                        onOpenCamera = onOpenCamera,
                    )
                    else -> PhotoGrid(
                        photos = state.photos,
                        permissionMissing = permissionMissing,
                        onGrantPermission = ::requestPermission,
                        onOpen = { photo ->
                            viewerUri = photo.uri.toString()
                            viewerOpen = true
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = viewerOpen && state.photos.isNotEmpty(),
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.96f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
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

/** Gap between photos in the grid: a contact sheet, so the squares nearly touch. */
private val GridGap = 2.dp

/** The photos, newest first, under a heading per day: square thumbnails 2dp apart, like a contact sheet. */
@Composable
internal fun PhotoGrid(
    photos: List<SavedPhoto>,
    permissionMissing: Boolean,
    onGrantPermission: () -> Unit,
    onOpen: (SavedPhoto) -> Unit,
) {
    val days = remember(photos) { groupByDay(photos) }
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
        verticalArrangement = Arrangement.spacedBy(GridGap),
    ) {
        if (permissionMissing) {
            item(key = "permission", span = { GridItemSpan(maxLineSpan) }) {
                PermissionNotice(onGrantPermission, Modifier.padding(top = Space.s))
            }
        }
        days.forEachIndexed { index, day ->
            item(key = "day_${day.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                Text(
                    text = day.label,
                    style = StudioType.bodyStrong,
                    color = Studio.Ink,
                    modifier = Modifier
                        .padding(top = if (index == 0 && !permissionMissing) Space.s else Space.xl, bottom = Space.s)
                        .semantics { heading() },
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
                        .aspectRatio(1f)
                        .background(Studio.Set)
                        .clickable { onOpen(photo) },
                )
            }
        }
        item(key = "bottom_inset", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun EmptyGallery(permissionMissing: Boolean, onGrantPermission: () -> Unit, onOpenCamera: () -> Unit) {
    // Scrollable so pull-to-refresh works and the message fits on small screens.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            if (permissionMissing) {
                EmptyState(
                    title = "Let Poseit see your photos",
                    message = "Allow access to photos to see the shots you took with Poseit. They stay on this phone.",
                    action = { PrimaryButton("Allow access", onClick = onGrantPermission) },
                )
            } else {
                EmptyState(
                    title = "No photos yet",
                    message = "Photos you take with the pose camera show up here, and in Pictures/Poseit on this phone.",
                    action = { PrimaryButton("Open the pose camera", onClick = onOpenCamera, icon = Icons.Outlined.CameraAlt) },
                )
            }
        }
    }
}

/** A quiet Sheet row above the grid: some photos stay hidden until photo access is allowed. */
@Composable
private fun PermissionNotice(onGrantPermission: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(StudioShapes.Medium)
            .background(Studio.Sheet)
            .padding(start = Space.l, end = Space.xs, top = Space.xs, bottom = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Studio.Graphite, modifier = Modifier.size(StudioInlineIconSize))
        Spacer(Modifier.width(Space.m))
        Text(
            "Some older photos are hidden. Allow photo access to see them all.",
            style = StudioType.label,
            color = Studio.Ink,
            modifier = Modifier.weight(1f),
        )
        TertiaryButton("Allow", onClick = onGrantPermission)
    }
}
