package com.poseit.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.toneColors

/**
 * An empty screen or list (docs/DESIGN.md, "Empty states"; Astryx EmptyState): left-aligned on [Space.textInset],
 * a short title in the Title style, one sentence that says what to do, and one button that does it. No emoji and no
 * illustrations. Colours follow the surface, so it also works inside a dark card.
 *
 * Copy invites one action and doesn't apologise: title "No favourites yet", message "Tap the heart on a pose to
 * keep it here.", action AccentButton("Browse poses") (a PrimaryButton or SecondaryButton when the screen already
 * has its accent).
 *
 * @param icon an optional outlined 24dp icon above the title, only when it helps recognition
 * @param horizontalPadding [Space.textInset] on the canvas; pass 0.dp inside a card, which has its own padding
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = Space.textInset,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = toneColors()
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = Space.section),
        horizontalAlignment = Alignment.Start,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(IconSize.standalone))
            Spacer(Modifier.height(Space.m))
        }
        Text(title, style = PoseitType.title, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(message, style = PoseitType.body, color = colors.textSecondary, modifier = Modifier.widthIn(max = 420.dp))
        if (action != null) {
            Spacer(Modifier.height(Space.xl))
            action()
        }
    }
}
