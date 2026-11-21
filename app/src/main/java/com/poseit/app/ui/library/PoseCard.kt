package com.poseit.app.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.components.StudioMenu
import com.poseit.app.ui.components.StudioMenuItem
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/**
 * One tile of the library grid. Tap opens the pose; long-press opens a small menu (try this pose / customise /
 * favourite), which TalkBack offers as custom actions.
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
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val customizeLabel = if (pose.isCustom) "Edit" else "Customise"
    val favouriteLabel = if (isFavorite) "Remove from favourites" else "Add to favourites"
    Box(modifier) {
        PoseTile(
            pose = pose,
            detail = pose.tileDetail,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            interactionSource = source,
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
        StudioMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            StudioMenuItem("Try this pose") { menuOpen = false; onTry() }
            StudioMenuItem(customizeLabel) { menuOpen = false; onCustomise() }
            StudioMenuItem(favouriteLabel) { menuOpen = false; onToggleFavorite() }
        }
    }
}

/**
 * The "Create a pose" tile at the start of the "My poses" tab: the same footprint as a pose tile, an empty patch
 * of studio paper marked out with a Hairline, and a caption that says what it makes.
 */
@Composable
internal fun CreatePoseCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    Column(modifier.clickable(interactionSource = source, indication = null, onClickLabel = "Create a pose", onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(StudioShapes.Medium)
                .border(1.dp, Studio.Hairline, StudioShapes.Medium)
                .indication(source, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Studio.Ink, modifier = Modifier.size(StudioIconSize))
        }
        Spacer(Modifier.height(Space.s))
        Text("Create a pose", style = StudioType.bodyStrong, color = Studio.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "Photo or drawing",
            style = StudioType.label,
            color = Studio.Graphite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
