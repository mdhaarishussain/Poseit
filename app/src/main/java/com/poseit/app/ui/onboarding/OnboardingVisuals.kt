package com.poseit.app.ui.onboarding

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Joint
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

/*
 * The intro's pictures. Each stands on the page's dark picture card (the stage): the lay figure alone (the
 * library), then the same figure inside cream viewfinder frame marks with one added idea per page: limb colours and
 * a hint (the camera), the countdown, joint handles (the editor). They are still diagrams; the design system has no
 * looping decoration.
 */

/**
 * Page 1: the lay figure on the stage, as in a pose tile, at about two thirds of the card's height so it stands in
 * open space like the mockups' drawings rather than filling the card.
 */
@Composable
internal fun FigurePicture(modifier: Modifier = Modifier) {
    PoseThumbnail(
        template = DemoPoses.handOnHip,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.62f)
            .clearAndSetSemantics { contentDescription = "A mannequin standing with one hand on its hip" },
        background = null,
    )
}

/**
 * Page 2: the camera's guide with the raised arm still red (off) and the rest green (matched), and the hint that
 * fixes it above the frame, across the picture card, as the camera puts hints at the top. The hint wraps rather
 * than shrinking or being cut (it carries the meaning; the red and green don't on their own), and when the card is
 * too short for both, only the hint shows.
 */
@Composable
internal fun MatchPicture(modifier: Modifier = Modifier) {
    val target = remember { DemoPoses.raisedArm }
    // The camera's bright signal colours, as they read on the dark stage; the hint's words carry the meaning too.
    val partColors = remember { BodyPart.entries.associateWith { if (it == BodyPart.RIGHT_ARM) PoseitColors.signalMissBright else PoseitColors.signalMatchBright } }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics {
                contentDescription = "The camera view. The guide's raised arm is red and the rest of it is green. " +
                    "A hint says: Raise your right arm."
            },
    ) {
        val showFrame = maxHeight >= 160.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            CueCard(
                "Raise your right arm",
                Modifier
                    .fillMaxWidth()
                    .padding(start = Space.xl, end = Space.xl, top = Space.xl),
            )
            if (showFrame) {
                Viewfinder(aspect = target.frameAspect, modifier = Modifier.weight(1f)) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .padding(Space.m),
                    ) {
                        val rect = fitFrameRect(size, target.frameAspect)
                        drawPoseFigure(target.figures.first(), target.frameAspect, rect, FigurePaint(partColors = partColors, alpha = 0.7f))
                    }
                }
            }
        }
    }
}

/** Page 3: the same pose, now held, with a hand raised above the head while the countdown runs. */
@Composable
internal fun CountdownPicture(modifier: Modifier = Modifier) {
    val pose = remember { DemoPoses.raisedArm }
    Viewfinder(
        aspect = pose.frameAspect,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "The camera view. A mannequin raises a hand and a countdown shows 3."
        },
    ) {
        PoseThumbnail(
            template = pose,
            modifier = Modifier
                .fillMaxSize()
                .padding(Space.m),
            background = null,
        )
        Text(
            "3",
            style = PoseitType.numeralLarge,
            color = PoseitColors.textPrimaryInverted,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Space.m, end = Space.l),
        )
    }
}

private val HandleJoints = listOf(
    Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_ELBOW, Joint.RIGHT_ELBOW, Joint.LEFT_WRIST, Joint.RIGHT_WRIST,
    Joint.LEFT_HIP, Joint.RIGHT_HIP, Joint.LEFT_KNEE, Joint.RIGHT_KNEE, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE,
)

/** The handle shown as being dragged in [PosingPicture]. */
private val SelectedJoint = Joint.LEFT_WRIST

/**
 * Page 4: the figure as the pose editor shows it, with a handle on each joint you can drag: dark discs ringed in
 * cream, and the one being dragged (the raised hand) in lavender, as the editor marks its selected joint.
 */
@Composable
internal fun PosingPicture(modifier: Modifier = Modifier) {
    val pose = remember { DemoPoses.star }
    Viewfinder(
        aspect = pose.frameAspect,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "A mannequin in a star jump with a round handle on each joint"
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(Space.m),
        ) {
            PoseThumbnail(template = pose, modifier = Modifier.fillMaxSize(), background = null)
            Canvas(Modifier.fillMaxSize()) {
                val rect = fitFrameRect(size, pose.frameAspect)
                val skeleton = pose.figures.first()
                // Handles scale with the figure, so a small picture (large font, small phone) isn't all rings.
                val radius = (rect.height * 0.018f).coerceIn(3.dp.toPx(), 5.dp.toPx())
                val ring = Stroke(1.5.dp.toPx())
                for (joint in HandleJoints) {
                    val p = skeleton[joint] ?: continue
                    val center = Offset(rect.left + p.x * rect.width, rect.top + p.y * rect.height)
                    val selected = joint == SelectedJoint
                    drawCircle(if (selected) PoseitColors.accent else PoseitColors.backgroundCard, if (selected) radius * 1.4f else radius, center)
                    drawCircle(
                        if (selected) PoseitColors.onAccent else PoseitColors.textPrimaryInverted,
                        if (selected) radius * 1.4f else radius,
                        center,
                        style = ring,
                    )
                }
            }
        }
    }
}

/** A frame of [aspect], as large as fits with a 24dp margin, centred, with viewfinder frame marks at its corners. */
@Composable
private fun Viewfinder(aspect: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .padding(Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        val width = min(maxWidth, maxHeight * aspect)
        Box(
            Modifier
                .size(width, width / aspect)
                .frameMarks(),
            content = content,
        )
    }
}

/**
 * The camera's hint banner in miniature: the cream cue card with ink text in the label size. It wraps to up to three
 * lines rather than shrinking (large font on a small phone), because the words are what tell you which limb is off.
 */
@Composable
private fun CueCard(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = PoseitType.label,
        color = PoseitColors.textPrimary,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(PoseitColors.backgroundSurface, PoseitShapes.Control)
            .padding(horizontal = Space.m, vertical = Space.s),
    )
}
