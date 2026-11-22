package com.poseit.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.theme.toneColors
import java.util.Locale

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

/**
 * Outlined person glyph for a people count: one, two or a group (Material Symbols person / group / groups; the only
 * icons a filter pill may carry).
 */
internal fun peopleIcon(count: Int): ImageVector = when {
    count <= 1 -> PoseitIcons.person
    count == 2 -> PoseitIcons.couple
    else -> PoseitIcons.group
}

/**
 * Three short segments, the first [level] of them in the surface's text colour and the rest in its track colour
 * (so it reads on cream and inside a dark card). Decorative on its own: put it next to the difficulty in words,
 * which carries the meaning.
 */
@Composable
internal fun DifficultyMeter(level: Int, modifier: Modifier = Modifier) {
    val clamped = level.coerceIn(1, 3)
    val colors = toneColors()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(width = 12.dp, height = 3.dp)
                    .background(if (i < clamped) colors.textPrimary else colors.track),
            )
        }
    }
}

/** The category's label, e.g. "Standing". */
internal val PoseTemplate.categoryText: String get() = category.label

/**
 * The pose's facts as one short phrase in sentence case: "Standing, easy", "Couple, medium", "Group, easy, 4 people"
 * (the number of people is left out when the category already says it).
 */
internal fun PoseTemplate.factsPhrase(): String = buildList {
    add(if (category == PoseCategory.CUSTOM) sourceText ?: categoryText else categoryText)
    add(Difficulty.label(difficulty).lowercase(Locale.getDefault()))
    if (personCount > 1 && category != PoseCategory.COUPLE) add("$personCount people")
}.joinToString(", ")

/** Where a user-made pose came from, or null for a built-in pose. */
internal val PoseTemplate.sourceText: String?
    get() = when (source) {
        PoseSource.FROM_PHOTO -> "From your photo"
        PoseSource.DRAWN -> "Drawn by you"
        PoseSource.BUILT_IN -> null
    }

/** The quiet line under a pose tile's name: the category, or for one of your own poses, where it came from. */
internal val PoseTemplate.tileDetail: String get() = sourceText ?: categoryText
