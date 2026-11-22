package com.poseit.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.poseit.app.data.PosePhoto
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.PhotoCreditSheet
import com.poseit.app.ui.components.PinnedTopBar
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PosePhoto
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.components.RowChevron
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.TopBarContent
import com.poseit.app.ui.components.rememberPosePhotos
import com.poseit.app.ui.components.rememberTopBarScrolledAway
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/*
 * Settings > About > Photo credits (docs/DESIGN.md, "Sample photos"): every sample photo with its pose, a small
 * thumbnail beside the pose's figure, the credit line and the licence. A row opens the photo's full credit sheet.
 * Laid out like Settings: the top bar card fused to an explainer card, then a section per source.
 */

@Composable
fun PhotoCreditsScreen(onBack: () -> Unit) {
    val posePhotos = rememberPosePhotos()
    val photos = remember(posePhotos) { posePhotos.all }
    val bySource = remember(photos) { photos.groupBy { it.source }.toList().sortedBy { (source, _) -> sourceOrder(source) } }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val count = if (photos.size == 1) "1 photo" else "${photos.size} photos"

    Scaffold(containerColor = PoseitColors.backgroundBody, contentWindowInsets = WindowInsets(0)) { _ ->
        val layoutDirection = LocalLayoutDirection.current
        val sides = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listState = rememberLazyListState()
        val barAway = rememberTopBarScrolledAway(listState)
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = sides.calculateStartPadding(layoutDirection) + Space.edge,
                    end = sides.calculateEndPadding(layoutDirection) + Space.edge,
                    top = top + Space.edge,
                    bottom = bottom + Space.section,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.edge),
            ) {
                item(key = "header") {
                    ConnectedCardStack(Modifier.fillMaxWidth()) {
                        TopBarContent(
                            "Photo credits",
                            onBack = onBack,
                            badge = { Badge(count) },
                            modifier = if (barAway) Modifier.clearAndSetSemantics {} else Modifier,
                        )
                        CardSegment {
                            Text(
                                "Some poses show a photo of a real person beside the figure.",
                                style = PoseitType.bodyLarge,
                                color = PoseitColors.textPrimaryInverted,
                            )
                            Spacer(Modifier.height(Space.s))
                            Text(
                                "The photos come from Mixkit and Wikimedia Commons contributors and are used under " +
                                    "the licences below. Tap a photo for its author, source, licence and the changes made.",
                                style = PoseitType.body,
                                color = PoseitColors.textSecondaryInverted,
                            )
                        }
                    }
                }
                bySource.forEach { (source, list) ->
                    item(key = "heading-$source") {
                        SectionHeading(
                            source,
                            Modifier.padding(start = Space.cardPadding, end = Space.cardPadding, top = Space.l, bottom = Space.xs),
                        )
                    }
                    items(list, key = { it.poseId }) { photo ->
                        PhotoCreditRow(photo, onClick = { selected = photo.poseId })
                    }
                }
            }
            // The list scrolls under the status bar onto this canvas band, never through it.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(PoseitColors.backgroundBody),
            )
            PinnedTopBar(visible = barAway, title = "Photo credits", onBack = onBack, badge = { Badge(count) })
        }
    }

    selected?.let { id ->
        posePhotos.photoFor(id)?.let { PhotoCreditSheet(it, onDismissRequest = { selected = null }) }
    }
}

private fun sourceOrder(source: String) = if (source == PosePhoto.SOURCE_MIXKIT) 0 else 1

/**
 * One photo in the list: an outlined cream row (one button) with the photo and the pose's figure side by side, the
 * pose name, the credit line and the licence, and a chevron.
 */
@Composable
internal fun PhotoCreditRow(photo: PosePhoto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pose = remember(photo.poseId) { BuiltInPoses.byId(photo.poseId) }
    ProvideSurfaceTone(SurfaceTone.Light) {
        Row(
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(PoseitShapes.Row)
                .background(PoseitColors.backgroundSurface)
                .border(1.dp, PoseitColors.borderEmphasized, PoseitShapes.Row)
                .clickable(onClickLabel = "Show the full photo credit", role = Role.Button, onClick = onClick)
                .padding(start = Space.m, end = Space.l, top = Space.m, bottom = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                // The row's own words name the pose, so the thumbnail adds no second label.
                PosePhoto(photo, Modifier.size(ThumbWidth, ThumbHeight), shape = PoseitShapes.Inner, contentDescription = null)
                if (pose != null) {
                    PoseThumbnail(pose, Modifier.size(ThumbWidth, ThumbHeight).clip(PoseitShapes.Inner).background(PoseitColors.backgroundStage))
                }
            }
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(photo.poseName, style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
                Text(photo.attribution, style = PoseitType.supporting, color = PoseitColors.textSecondary)
                Text("Licence: ${photo.licence}", style = PoseitType.supporting, color = PoseitColors.textSecondary)
            }
            Spacer(Modifier.width(Space.s))
            RowChevron()
        }
    }
}

private val ThumbWidth = 42.dp
private val ThumbHeight = 56.dp
