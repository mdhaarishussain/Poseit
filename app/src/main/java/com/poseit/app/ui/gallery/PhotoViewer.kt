package com.poseit.app.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poseit.app.data.SavedPhoto
import com.poseit.app.ui.components.CapsLabel
import com.poseit.app.ui.components.CircleIconButton
import com.poseit.app.ui.components.CircleVariant
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PoseitAlertDialog
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitMenu
import com.poseit.app.ui.components.PoseitMenuItem
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.theme.LightSystemBarIconsEffect
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Full-screen photo viewer (Astryx Lightbox: the media, a close button and a position counter): swipe between photos,
 * pinch / double-tap to zoom, share, delete, open in another app, plus a quick review pair (Delete / Keep) to cull a
 * burst of shots fast.
 *
 * True black around the photo, like the camera's live view, with white system-bar icons. No bars or scrims across
 * the photo: each control carries its own dark disc or pill, like the camera's controls, and a tap on the photo hides
 * them.
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

    LightSystemBarIconsEffect()
    ProvideSurfaceTone(SurfaceTone.Dark) {
        Box(modifier.fillMaxSize().background(PoseitColors.backgroundCamera)) {
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

            // ---- top controls ------------------------------------------------------------------------------
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(PoseitMotion.standard(PoseitMotion.FAST)),
                exit = fadeOut(PoseitMotion.standard(PoseitMotion.FAST)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Space.edge, vertical = Space.edge),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleIconButton(PoseitIcons.back, "Close viewer", onClose, variant = CircleVariant.Camera)
                    Spacer(Modifier.width(Space.xs))
                    Box(Modifier.weight(1f)) {
                        PhotoPosition(
                            position = if (photos.isEmpty()) "" else "${(pagerState.currentPage + 1).coerceAtMost(photos.size)} / ${photos.size}",
                            date = current?.let { formatPhotoDateTime(it.dateTakenMillis, context, omitCurrentYear = true) }.orEmpty(),
                        )
                    }
                    Spacer(Modifier.width(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        CircleIconButton(PoseitIcons.share, "Share", { current?.let(onShare) }, variant = CircleVariant.Camera, enabled = current != null)
                        CircleIconButton(PoseitIcons.delete, "Delete", { confirmDelete = current }, variant = CircleVariant.Camera, enabled = current != null)
                        Box {
                            CircleIconButton(PoseitIcons.moreHorizontal, "More options", { menuOpen = true }, variant = CircleVariant.Camera)
                            PoseitMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                PoseitMenuItem("Open in gallery app", leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew) {
                                    menuOpen = false
                                    current?.let(onOpenExternally)
                                }
                            }
                        }
                    }
                }
            }

            // ---- review pair + snackbar ----------------------------------------------------------------------
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // A dark card with a cream hairline, legible on black.
                PoseitSnackbarHost(snackbarHostState, Modifier.padding(bottom = if (chromeVisible) 0.dp else Space.s))
                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = fadeIn(PoseitMotion.standard(PoseitMotion.FAST)),
                    exit = fadeOut(PoseitMotion.standard(PoseitMotion.FAST)),
                ) {
                    ReviewButtons(
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
    }

    confirmDelete?.let { photo ->
        PoseitAlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = "Delete this photo?",
            text = "It's removed from your phone's gallery. This can't be undone.",
            confirmButton = {
                DestructiveButton(
                    text = "Delete photo",
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
 * "2 / 12" and the date taken on an opaque dark-card pill, so the words keep their contrast over a white wall or a
 * bright sky (a see-through scrim would let a bright photo wash them out; the icon-only discs can stay see-through).
 */
@Composable
private fun PhotoPosition(position: String, date: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .heightIn(min = 48.dp)
            .background(PoseitColors.backgroundCard, PoseitShapes.Pill)
            .padding(horizontal = Space.l, vertical = Space.xs),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(position, style = PoseitType.numeral(15.sp), color = PoseitColors.cameraContent, maxLines = 1)
        if (date.isNotEmpty()) {
            Text(
                date,
                style = PoseitType.supporting,
                color = PoseitColors.textSecondaryInverted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Delete / Keep, the mockups' outlined-and-filled pill pair. Delete is the outlined pill on an opaque dark-card fill
 * (so its label keeps its contrast over any photo) with the bright Miss red (the signal for dark surroundings) and a
 * bin glyph; Keep is the primary action, a cream pill
 * with ink capitals and a check, taking the remaining width so "Keep and finish" fits (on two lines at most).
 */
@Composable
private fun ReviewButtons(enabled: Boolean, isLast: Boolean, onDelete: () -> Unit, onKeep: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.l, vertical = Space.l),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val red = if (enabled) PoseitColors.signalMissBright else PoseitColors.textDisabledInverted
            SecondaryButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.background(PoseitColors.backgroundCard, PoseitShapes.Pill),
            ) {
                Icon(PoseitIcons.delete, contentDescription = null, tint = red, modifier = Modifier.size(IconSize.inline))
                Spacer(Modifier.width(Space.s))
                CapsLabel("Delete", color = red, style = PoseitType.capsButton)
            }
            PrimaryButton(onClick = onKeep, enabled = enabled, modifier = Modifier.weight(1f)) {
                Icon(PoseitIcons.check, contentDescription = null, modifier = Modifier.size(IconSize.inline))
                Spacer(Modifier.width(Space.s))
                CapsLabel(if (isLast) "Keep and finish" else "Keep", maxLines = 2, style = PoseitType.capsButton)
            }
        }
    }
}
