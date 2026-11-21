package com.poseit.app.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.theme.PoseitColors

/** Human wording + colour for a 1..3 difficulty. Shared by the library and the detail screen. */
internal object Difficulty {
    fun label(level: Int): String = when (level.coerceIn(1, 3)) {
        1 -> "Easy"
        2 -> "Medium"
        else -> "Advanced"
    }

    fun color(level: Int): Color = when (level.coerceIn(1, 3)) {
        1 -> PoseitColors.Mint
        2 -> PoseitColors.Amber
        else -> PoseitColors.Coral
    }
}

/** "Solo" / "Couple" / "Group of 4" plus a matching icon. */
internal fun peopleLabel(count: Int): String = when {
    count <= 1 -> "Solo"
    count == 2 -> "Couple"
    else -> "Group of $count"
}

internal fun peopleIcon(count: Int): ImageVector = when {
    count <= 1 -> Icons.Filled.Person
    count == 2 -> Icons.Filled.People
    else -> Icons.Filled.Groups
}

/** Three dots, the first [level] of them filled. */
@Composable
internal fun DifficultyDots(level: Int, modifier: Modifier = Modifier, dotSize: Dp = 6.dp) {
    val clamped = level.coerceIn(1, 3)
    val color = Difficulty.color(clamped)
    Row(
        modifier.semantics { contentDescription = "Difficulty: ${Difficulty.label(clamped)}" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (i < clamped) color else MaterialTheme.colorScheme.outline),
            )
        }
    }
}

/** Small pill on a thumbnail showing how many people the pose is for. */
@Composable
internal fun PeopleBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = peopleLabel(count) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(peopleIcon(count), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** Small pill marking a user-made pose. */
@Composable
internal fun MineBadge(modifier: Modifier = Modifier) {
    Text(
        "Mine",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PoseitColors.Violet.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
    )
}

/**
 * Heart toggle with a little "pop" when a pose becomes a favourite.
 * @param onImage draws a translucent disc behind the heart so it stays legible on top of artwork
 */
@Composable
internal fun FavoriteToggle(
    isFavorite: Boolean,
    poseName: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onImage: Boolean = true,
) {
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite && !previous) {
            scale.animateTo(1.35f, tween(durationMillis = 110))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        previous = isFavorite
    }
    val tint by animateColorAsState(
        if (isFavorite) PoseitColors.Coral else if (onImage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "heartTint",
    )
    IconButton(onClick = onToggle, modifier = modifier) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (onImage) Color.Black.copy(alpha = 0.38f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove $poseName from favourites" else "Add $poseName to favourites",
                tint = tint,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
        }
    }
}

/** The category's label, e.g. "Standing". */
internal val PoseTemplate.categoryText: String get() = category.label
