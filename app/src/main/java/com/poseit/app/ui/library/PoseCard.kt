package com.poseit.app.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.theme.PoseitColors

internal val ThumbnailBackground = Brush.verticalGradient(listOf(Color(0xFF2E3140), Color(0xFF1D1F27)))

/**
 * One tile of the library grid. Tap opens the pose; long-press opens a small menu (try in camera / customize).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PoseCard(
    pose: PoseTemplate,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTry: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box {
            Column(
                Modifier
                    .combinedClickable(
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
                            CustomAccessibilityAction("Try in camera") { onTry(); true },
                            CustomAccessibilityAction(if (pose.isCustom) "Edit" else "Customize") { onCustomize(); true },
                            CustomAccessibilityAction(if (isFavorite) "Remove from favourites" else "Add to favourites") {
                                onToggleFavorite(); true
                            },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ThumbnailBackground),
                ) {
                    PoseThumbnail(
                        template = pose,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        background = null,
                    )
                    Row(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (pose.personCount > 1) PeopleBadge(pose.personCount)
                        if (pose.isCustom) MineBadge()
                    }
                }
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp)) {
                    Text(
                        pose.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pose.categoryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        DifficultyDots(pose.difficulty)
                    }
                }
            }
            FavoriteToggle(
                isFavorite = isFavorite,
                poseName = pose.name,
                onToggle = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Try in camera") },
                    leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                    onClick = { menuOpen = false; onTry() },
                )
                DropdownMenuItem(
                    text = { Text(if (pose.isCustom) "Edit" else "Customize") },
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    onClick = { menuOpen = false; onCustomize() },
                )
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove from favourites" else "Add to favourites") },
                    leadingIcon = {
                        Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null)
                    },
                    onClick = { menuOpen = false; onToggleFavorite() },
                )
            }
        }
    }
}

/** Dashed-looking "new pose" tile shown first in the "My poses" tab. */
@Composable
internal fun CreatePoseCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .border(1.5.dp, PoseitColors.Violet.copy(alpha = 0.6f), shape)
            .background(PoseitColors.Violet.copy(alpha = 0.08f))
            .clickable(onClickLabel = "Create a pose", onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(PoseitColors.Violet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("New pose", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "From a photo or drawn by hand",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}
