package com.poseit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.theme.PoseitColors
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

/**
 * The picture card of an empty screen, the filling card of a [FillingCardStack] under the card that says what is
 * missing (the mockups' tall drawing card under "Train your coordination."): an empty frame, a mannequin drawn in a
 * mist outline standing in cream viewfinder frame marks, with [action] (usually full width) at the card's foot.
 * Decorative: the picture is hidden from screen readers. When the card is too short for the picture to read (under
 * about 120dp), only the action shows.
 */
@Composable
fun EmptyPictureCard(
    modifier: Modifier = Modifier,
    pose: PoseTemplate = BuiltInPoses.all.first(),
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(Space.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (maxHeight >= 120.dp) {
                val aspect = pose.frameAspect.takeIf { it.isFinite() && it > 0f } ?: 0.75f
                // The frame at 82% of the space, at the pose's photo aspect.
                val h = minOf(maxHeight * 0.82f, maxWidth * 0.82f / aspect)
                Box(
                    Modifier
                        .size(width = h * aspect, height = h)
                        .frameMarks(),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        pose.figures.forEach { figure ->
                            drawPoseFigure(
                                figure, aspect, Rect(Offset.Zero, size),
                                FigurePaint(style = FigureStyle.OUTLINE, fill = PoseitColors.textSecondaryInverted, showFaceMarker = false),
                            )
                        }
                    }
                }
            }
        }
        if (action != null) {
            Spacer(Modifier.height(Space.l))
            Box(Modifier.fillMaxWidth()) { action() }
        }
    }
}
