package com.poseit.app.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.IconSize
import com.poseit.app.ui.components.PhotoBadge
import com.poseit.app.ui.components.PhotoBadgeSize
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitMenu
import com.poseit.app.ui.components.PoseitMenuItem
import com.poseit.app.ui.components.rememberPosePhoto
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

/**
 * One tile of the library grid: a dark pose tile. Tap opens the pose; long-press opens a small menu (try this pose /
 * customise / favourite), which TalkBack offers as custom actions. A built-in pose with a sample photo carries it as
 * a small print in the card's bottom corner, beside the caption ([PhotoBadge]), which opens the photo's credit.
 *
 * @param nameLines lines reserved for the name (1 or 2): the grid passes the most any tile of the row needs, so the
 *   dark cards of one row end on the same line
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PoseCard(
    pose: PoseTemplate,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTry: () -> Unit,
    onCustomise: () -> Unit,
    modifier: Modifier = Modifier,
    nameLines: Int = 1,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val customizeLabel = if (pose.isCustom) "Edit" else "Customise"
    val favouriteLabel = if (isFavorite) "Remove from favourites" else "Add to favourites"
    val photo = rememberPosePhoto(pose)
    Box(modifier) {
        PoseTile(
            pose = pose,
            detail = pose.tileDetail,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            interactionSource = source,
            nameMinLines = nameLines,
            // A pose with a sample photo shows it as a small print beside the caption, in the corner no figure
            // reaches: spotted at a glance, without changing the tile's size or covering the figure. The print is
            // also the photo's credit button.
            badge = if (photo != null) {
                { PhotoBadge(photo, Modifier.align(Alignment.BottomEnd).padding(end = Space.m, bottom = Space.m)) }
            } else {
                null
            },
            badgeReserve = if (photo != null) PhotoBadgeSize else 0.dp,
            modifier = Modifier
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClickLabel = "Open pose",
                    onLongClickLabel = "More options",
                    onClick = onOpen,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Try this pose") { onTry(); true },
                        CustomAccessibilityAction(customizeLabel) { onCustomise(); true },
                        CustomAccessibilityAction(favouriteLabel) { onToggleFavorite(); true },
                    )
                },
        )
        PoseitMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            PoseitMenuItem("Try this pose") { menuOpen = false; onTry() }
            PoseitMenuItem(customizeLabel) { menuOpen = false; onCustomise() }
            PoseitMenuItem(favouriteLabel) { menuOpen = false; onToggleFavorite() }
        }
    }
}

/** The caption of [CreatePoseCard]. */
internal const val CreatePoseCardName = "Create a pose"

/**
 * The "Create a pose" tile at the start of the "My poses" tab: the footprint of a pose tile, drawn as an outlined
 * cream card (the quiet counterpart of the dark tiles) with a dark disc holding the plus where the figure would
 * stand, and a caption that says what it makes.
 */
@Composable
internal fun CreatePoseCard(onClick: () -> Unit, modifier: Modifier = Modifier, nameLines: Int = 1) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .clip(PoseitShapes.Tile)
            .background(PoseitColors.backgroundSurface)
            .border(1.dp, PoseitColors.borderEmphasized, PoseitShapes.Tile)
            .clickable(interactionSource = source, indication = null, onClickLabel = "Create a pose", role = Role.Button, onClick = onClick)
            .indication(source, ripple()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(PoseitColors.backgroundCard, PoseitShapes.Circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PoseitIcons.add, contentDescription = null, tint = PoseitColors.textPrimaryInverted, modifier = Modifier.size(IconSize.standalone))
            }
        }
        // The same caption block as a pose tile, so the row's cards end on one line.
        Column(Modifier.padding(start = Space.l, end = Space.m, bottom = Space.l)) {
            Text(
                CreatePoseCardName,
                style = PoseitType.bodyStrong,
                color = PoseitColors.textPrimary,
                minLines = nameLines.coerceIn(1, 2),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Photo or drawing",
                style = PoseitType.supporting,
                color = PoseitColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
