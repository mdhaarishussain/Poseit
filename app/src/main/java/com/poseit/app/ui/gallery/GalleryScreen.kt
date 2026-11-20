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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.poseit.app.ui.theme.PoseitColors
import kotlinx.coroutines.launch

/** Photos taken with Poseit, with a full-screen viewer, share and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onBack: () -> Unit) {
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
                "Couldn't open settings",
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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val topBarScroll = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Your photos")
                            if (state.photos.isNotEmpty()) {
                                Text(
                                    text = if (state.photos.size == 1) "1 photo" else "${state.photos.size} photos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (state.photos.isNotEmpty()) {
                            TextButton(onClick = {
                                viewerUri = state.photos.first().uri.toString()
                                viewerOpen = true
                            }) {
                                Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Review")
                            }
                        }
                    },
                    scrollBehavior = topBarScroll,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            snackbarHost = { if (!viewerOpen) SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh(userInitiated = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.photos.isEmpty() -> EmptyGallery(permissionMissing = permissionMissing, onGrantPermission = ::requestPermission)
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
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
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
                    scope.launch { snackbar.showSnackbar("All caught up – that was the last photo") }
                },
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<SavedPhoto>,
    permissionMissing: Boolean,
    onGrantPermission: () -> Unit,
    onOpen: (SavedPhoto) -> Unit,
) {
    val days = remember(photos) { groupByDay(photos) }
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (permissionMissing) {
            item(key = "permission", span = { GridItemSpan(maxLineSpan) }) {
                PermissionBanner(onGrantPermission, Modifier.padding(bottom = 8.dp))
            }
        }
        days.forEach { day ->
            item(key = "day_${day.key}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
private fun EmptyGallery(permissionMissing: Boolean, onGrantPermission: () -> Unit) {
    // Scrollable so pull-to-refresh works and the message fits on small screens; centred when there is room.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PoseitColors.Violet.copy(alpha = 0.35f), PoseitColors.Coral.copy(alpha = 0.25f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (permissionMissing) Icons.Rounded.Lock else Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (permissionMissing) "Let Poseit see your photos" else "No photos yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (permissionMissing) {
                    "To show the shots you took with Poseit, allow access to photos on this phone. Nothing ever leaves your device."
                } else {
                    "Pick a pose, line yourself up with the guide and tap the shutter. Every shot you take with Poseit lands right here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp),
            )
            Spacer(Modifier.height(24.dp))
            if (permissionMissing) {
                FilledTonalButton(onClick = onGrantPermission) { Text("Allow access") }
            } else {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Tip: photos are saved to Pictures, Poseit" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = PoseitColors.Mint)
                    Spacer(Modifier.size(8.dp))
                    Text("Saved to Pictures/Poseit", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(onGrantPermission: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = PoseitColors.Amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                "Some older photos may be hidden. Allow photo access to see them all.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onGrantPermission) { Text("Allow") }
        }
    }
}
