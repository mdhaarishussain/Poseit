package com.poseit.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.theme.Studio

/** Human wording for a 1..3 difficulty. Shared by the library and the detail screen. */
internal object Difficulty {
    fun label(level: Int): String = when (level.coerceIn(1, 3)) {
        1 -> "Easy"
        2 -> "Medium"
        else -> "Advanced"
    }
}

/** "Solo" / "Couple" / "Group of 4". */
internal fun peopleLabel(count: Int): String = when {
    count <= 1 -> "Solo"
    count == 2 -> "Couple"
    else -> "Group of $count"
}

/** Outlined person icon for a people count: one, two or a group (the only icons a filter chip may carry). */
internal fun peopleIcon(count: Int): ImageVector = when {
    count <= 1 -> Icons.Outlined.Person
    count == 2 -> Icons.Outlined.People
    else -> Icons.Outlined.Groups
}

/**
 * Three short Ink segments, the first [level] of them solid and the rest Hairline. Decorative on its own: put it
 * next to the difficulty in words, which carries the meaning.
 */
@Composable
internal fun DifficultyMeter(level: Int, modifier: Modifier = Modifier) {
    val clamped = level.coerceIn(1, 3)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(width = 10.dp, height = 3.dp)
                    .background(if (i < clamped) Studio.Ink else Studio.Hairline),
            )
        }
    }
}

/** A category's name in sentence case ("My poses" rather than the enum's "My Poses"). */
internal val PoseCategory.displayLabel: String get() = if (this == PoseCategory.CUSTOM) "My poses" else label

/** The category's label, e.g. "Standing". */
internal val PoseTemplate.categoryText: String get() = category.displayLabel

/** Where a user-made pose came from, or null for a built-in pose. */
internal val PoseTemplate.sourceText: String?
    get() = when (source) {
        PoseSource.FROM_PHOTO -> "From your photo"
        PoseSource.DRAWN -> "Drawn by you"
        PoseSource.BUILT_IN -> null
    }

/** The quiet line under a pose tile's name: the category, or for one of your own poses, where it came from. */
internal val PoseTemplate.tileDetail: String get() = sourceText ?: categoryText
