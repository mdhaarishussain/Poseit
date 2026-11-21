package com.poseit.app.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.data.SavedPhoto
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.StudioButtonHeight
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Chrome colour on the viewer's black: Mannequin white, like the camera's controls. */
private val OnPhoto = Studio.Mannequin

/**
 * Full-screen photo viewer: swipe between photos, pinch / double-tap to zoom, share, delete, open in another app,
 * plus a quick review bar (Delete / Keep) to cull a burst of shots fast.
 *
 * True black around the photo, like the camera's live view. The bars are flat black at 45% (no gradient scrims)
 * with Mannequin-white outlined icons; a tap on the photo hides them.
 */
@Composable
internal fun PhotoViewer(
    photos: List<SavedPhoto>,
    initialPhoto: SavedPhoto?,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onCurrentPhotoChanged: (SavedPhoto) -> Unit,
    onShare: (SavedPhoto) -> Unit,
    onOpenExternally: (SavedPhoto) -> Unit,
    onDeleteConfirmed: (SavedPhoto) -> Unit,
    onQuickDelete: (SavedPhoto) -> Unit,
    onReviewFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialIndex = remember { photos.indexOfFirst { it.uri == initialPhoto?.uri }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentPhotos by rememberUpdatedState(photos)
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<SavedPhoto?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var lastUri by remember { mutableStateOf(initialPhoto?.uri) }

    val current = photos.getOrNull(pagerState.currentPage.coerceAtMost(photos.lastIndex))

    // Remember which photo is on screen so a refresh that reorders the list keeps it in place.
    LaunchedEffect(pagerState) {
        snapshotFlow { currentPhotos.getOrNull(pagerState.currentPage) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect {
                lastUri = it.uri
                onCurrentPhotoChanged(it)
            }
    }
    // A zoomed photo blocks paging; never leave that lock behind when the page changes (Keep / delete).
    LaunchedEffect(pagerState.currentPage) { zoomed = false }
    LaunchedEffect(photos) {
        val idx = photos.indexOfFirst { it.uri == lastUri }
        if (idx >= 0 && idx != pagerState.currentPage) pagerState.scrollToPage(idx)
    }

    Box(modifier.fillMaxSize().background(Studio.LiveView)) {
        HorizontalPager(
            state = pagerState,
            key = { photos.getOrNull(it)?.uri?.toString() ?: "page_$it" },
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val photo = photos.getOrNull(page) ?: return@HorizontalPager
            val isCurrent = page == pagerState.currentPage
            ZoomableImage(
                model = photo.uri,
                contentDescription = "Photo ${page + 1} of ${photos.size}",
                resetKey = isCurrent,
                onZoomChanged = { if (isCurrent) zoomed = it },
                onTap = { chromeVisible = !chromeVisible },
            )
        }

        // ---- top bar -------------------------------------------------------------------------------------
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Studio.ControlScrim)
                    .statusBarsPadding()
                    .height(64.dp)
                    // Glyphs land on the 20dp gutter (48dp targets around 22dp icons).
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudioIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Close viewer", onClose, tint = OnPhoto)
                Column(Modifier.weight(1f).padding(start = Space.xs)) {
                    Text(
                        text = if (photos.isEmpty()) "" else "${(pagerState.currentPage + 1).coerceAtMost(photos.size)} / ${photos.size}",
                        style = StudioType.numeral(15.sp),
                        color = OnPhoto,
                    )
                    if (current != null) {
                        Text(
                            text = formatPhotoDateTime(current.dateTakenMillis, context),
                            style = StudioType.label,
                            color = OnPhoto.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                StudioIconButton(Icons.Outlined.Share, "Share", { current?.let(onShare) }, enabled = current != null, tint = OnPhoto)
                StudioIconButton(Icons.Outlined.Delete, "Delete", { confirmDelete = current }, enabled = current != null, tint = OnPhoto)
                Box {
                    StudioIconButton(Icons.Outlined.MoreVert, "More options", { menuOpen = true }, tint = OnPhoto)
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = StudioShapes.Small,
                        containerColor = Studio.Sheet,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, Studio.Hairline),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open in gallery app", style = StudioType.body, color = Studio.Ink) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = Studio.Ink, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                menuOpen = false
                                current?.let(onOpenExternally)
                            },
                        )
                    }
                }
            }
        }

        // ---- review bar + snackbar ---------------------------------------------------------------------------
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // On black, the snackbar inverts to Sheet with Ink text so it stays visible.
            SnackbarHost(
                snackbarHostState,
                Modifier.navigationBarsPadding().padding(bottom = if (chromeVisible) 0.dp else Space.s),
            ) { data ->
                Snackbar(
                    data,
                    shape = StudioShapes.Small,
                    containerColor = Studio.Sheet,
                    contentColor = Studio.Ink,
                    actionColor = Studio.Ink,
                    actionContentColor = Studio.Ink,
                )
            }
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
            ) {
                ReviewBar(
                    enabled = current != null,
                    isLast = pagerState.currentPage >= photos.lastIndex,
                    onDelete = { current?.let(onQuickDelete) },
                    onKeep = {
                        if (pagerState.currentPage < photos.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onReviewFinished()
                        }
                    },
                )
            }
        }
    }

    confirmDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            shape = StudioShapes.Medium,
            containerColor = Studio.Sheet,
            tonalElevation = 0.dp,
            title = { Text("Delete this photo?", style = StudioType.title, color = Studio.Ink) },
            text = { Text("It's removed from your phone's gallery. This can't be undone.", style = StudioType.body, color = Studio.Graphite) },
            confirmButton = {
                DestructiveButton(
                    text = "Delete",
                    filled = true,
                    onClick = {
                        confirmDelete = null
                        onDeleteConfirmed(photo)
                    },
                )
            },
            dismissButton = { TertiaryButton("Cancel", onClick = { confirmDelete = null }) },
        )
    }
}

/**
 * Delete / Keep, side by side on the flat scrim. Delete is outlined with the bright Miss text (the dark-surround
 * variant of the signal); Keep is the primary action, inverted for black: a Sheet fill with an Ink check and label.
 */
@Composable
private fun ReviewBar(enabled: Boolean, isLast: Boolean, onDelete: () -> Unit, onKeep: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Studio.ControlScrim)
            .navigationBarsPadding()
            .padding(horizontal = Space.gutter, vertical = Space.l),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = StudioButtonHeight),
                shape = StudioShapes.Medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Studio.MissBright,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = OnPhoto.copy(alpha = 0.38f),
                ),
                border = BorderStroke(1.dp, OnPhoto.copy(alpha = 0.32f)),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.s))
                Text("Delete", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onKeep,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = StudioButtonHeight),
                shape = StudioShapes.Medium,
                elevation = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Studio.Sheet,
                    contentColor = Studio.Ink,
                    disabledContainerColor = OnPhoto.copy(alpha = 0.12f),
                    disabledContentColor = OnPhoto.copy(alpha = 0.38f),
                ),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.s))
                Text(if (isLast) "Keep and finish" else "Keep", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
