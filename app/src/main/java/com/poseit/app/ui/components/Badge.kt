package com.poseit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes

/** The fill of a [Badge]. */
enum class BadgeTone {
    /** Lavender with dark capitals: the default, like the mockups' level badge. */
    Accent,

    /** Dark with cream capitals: a badge on the canvas next to dark text (the mockups' time-left tag). */
    Dark,

    /** Cream with dark capitals: a badge inside a dark card that should not compete with an accent. */
    Light,
}

/**
 * A badge (Astryx Badge): a short status or count in the tiny capitals on a 24dp rounded tag, e.g. the top bar's
 * "99 poses", "New", "2 people". Not interactive. Pass sentence case; it is uppercased for display only.
 * @param icon optional 14dp leading glyph, only when it helps recognition
 */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Accent, icon: ImageVector? = null) {
    val (fill, fg) = when (tone) {
        BadgeTone.Accent -> PoseitColors.accent to PoseitColors.onAccent
        BadgeTone.Dark -> PoseitColors.backgroundCard to PoseitColors.textPrimaryInverted
        BadgeTone.Light -> PoseitColors.backgroundSurface to PoseitColors.textPrimary
    }
    Row(
        modifier
            .heightIn(min = 24.dp)
            .background(fill, PoseitShapes.Inner)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        CapsLabel(text, color = fg)
    }
}
