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
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioType

/**
 * An empty screen or list (docs/DESIGN.md): left-aligned, a short title, one sentence that says what to do, and
 * one button that does it. No emoji and no illustrations.
 *
 * Copy invites one action and doesn't apologise: title "No favourites yet", message "Tap the heart on a pose to
 * keep it here.", action PrimaryButton("Browse poses").
 *
 * @param icon an optional small outlined icon (22dp, Graphite) above the title, only when it helps recognition
 * @param action one button, usually a [PrimaryButton] (a [SecondaryButton] if the screen already has a primary)
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = Space.section),
        horizontalAlignment = Alignment.Start,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Studio.Graphite, modifier = Modifier.size(StudioIconSize))
            Spacer(Modifier.height(Space.m))
        }
        Text(title, style = StudioType.title, color = Studio.Ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(message, style = StudioType.body, color = Studio.Graphite, modifier = Modifier.widthIn(max = 420.dp))
        if (action != null) {
            Spacer(Modifier.height(Space.xl))
            action()
        }
    }
}
