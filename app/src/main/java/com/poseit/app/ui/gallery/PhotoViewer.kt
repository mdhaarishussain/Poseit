package com.poseit.app.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.data.SavedPhoto
import com.poseit.app.ui.theme.PoseitColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Full-screen photo viewer: swipe between photos, pinch / double-tap to zoom, share, delete, open in another app,
 * plus a quick review bar (Delete / Keep) to cull a burst of shots fast.
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

    Box(modifier.fillMaxSize().background(Color.Black)) {
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
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close viewer", tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = if (photos.isEmpty()) "" else "${(pagerState.currentPage + 1).coerceAtMost(photos.size)} / ${photos.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    if (current != null) {
                        Text(
                            text = formatPhotoDateTime(current.dateTakenMillis, context),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = { current?.let(onShare) }, enabled = current != null) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                }
                IconButton(onClick = { confirmDelete = current }, enabled = current != null) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Open in gallery app") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
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
            SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding().padding(bottom = if (chromeVisible) 0.dp else 8.dp))
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
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
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("Delete this photo?") },
            text = { Text("It will be removed from your phone's gallery. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = null
                        onDeleteConfirmed(photo)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PoseitColors.Red),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReviewBar(enabled: Boolean, isLast: Boolean, onDelete: () -> Unit, onKeep: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PoseitColors.Red),
                border = BorderStroke(1.5.dp, PoseitColors.Red.copy(alpha = if (enabled) 0.9f else 0.35f)),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onKeep,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PoseitColors.Mint, contentColor = Color(0xFF0B2A1D)),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isLast) "Keep · done" else "Keep", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
