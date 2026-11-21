package com.poseit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/*
 * Surfaces and cards (docs/DESIGN.md, "Cards"). Astryx model: Card / ClickableCard with variants. Poseit's cards
 * are flat: no shadow, no elevation, no gradient. A card tells the content inside which tone it sits on
 * (LocalSurfaceTone), so buttons, rows, icons and text pick light-on-dark colours inside a dark card by themselves.
 */

/**
 * Sets the surface tone (and the matching default content colour) for [content]: use it for custom dark areas
 * (the camera HUD, a full-bleed dark screen) so shared components inside choose their on-dark colours.
 */
@Composable
fun ProvideSurfaceTone(tone: SurfaceTone, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSurfaceTone provides tone,
        LocalContentColor provides if (tone == SurfaceTone.Dark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary,
        content = content,
    )
}

/** The look of a [PoseitCard]. */
enum class CardVariant {
    /** Deep green-black with cream content: the default card, the one the mockups are built from. */
    Dark,

    /** Cream surface with a 1dp dark outline: list rows and quiet groups on the canvas. Never connects. */
    Outlined,

    /** The lavender accent with dark content. At most one per screen, for the thing to do next. */
    Accent,

    /** Pale cream fill without an outline: placeholders and inset areas on the canvas. */
    Muted,
}

/**
 * A card (Astryx Card; ClickableCard when [onClick] is set). Flat, [PoseitShapes.Card] radius for dark cards and
 * [PoseitShapes.Row] for outlined ones, [Space.cardPadding] inside.
 *
 * To fuse dark cards with the concave necks, put them in a [ConnectedCardStack] instead (each child there is one
 * card); a lone dark card stands apart, [Space.edge] from its neighbours.
 *
 * @param onClick makes the whole card one button (Role.Button) with [onClickLabel] as its action label; nested
 *   buttons still work on their own
 * @param shape defaults by variant: Card (24dp) for Dark, Accent and Muted, Row (16dp) for Outlined
 */
@Composable
fun PoseitCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Dark,
    shape: Shape = if (variant == CardVariant.Outlined) PoseitShapes.Row else PoseitShapes.Card,
    contentPadding: PaddingValues = PaddingValues(Space.cardPadding),
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (fill, tone) = when (variant) {
        CardVariant.Dark -> PoseitColors.backgroundCard to SurfaceTone.Dark
        CardVariant.Outlined -> PoseitColors.backgroundSurface to SurfaceTone.Light
        CardVariant.Accent -> PoseitColors.accent to SurfaceTone.Light
        CardVariant.Muted -> PoseitColors.backgroundMuted to SurfaceTone.Light
    }
    val border = if (variant == CardVariant.Outlined) BorderStroke(1.dp, PoseitColors.borderEmphasized) else null
    ProvideSurfaceTone(tone) {
        Column(
            modifier
                .clip(shape)
                .background(fill)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * The tiny uppercase label of pill buttons, badges and segmented options ([PoseitType.caps]). Pass the words in
 * sentence case: they are uppercased for display only, and screen readers and tests get the sentence-case text
 * (all-caps words are often spelled out letter by letter). Numbers and the unit right after a number keep their
 * case ("3 s", "10s", "2 people" stays "2 PEOPLE" because "people" is a word, not a unit).
 */
@Composable
fun CapsLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = PoseitType.caps,
    maxLines: Int = 1,
    textAlign: TextAlign? = null,
) {
    Box(modifier.clearAndSetSemantics { this.text = AnnotatedString(text) }) {
        Text(
            capsForDisplay(text),
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            softWrap = maxLines > 1,
            textAlign = textAlign,
        )
    }
}

/** Units that keep their case after a number in a [CapsLabel]: "3 s", "1/250 s", "50 mm". */
private val Units = setOf("s", "ms", "min", "h", "mm", "cm", "m", "px", "dp", "x")

/** Uppercases [text] for display, leaving numbers ("10s") and a unit after a number ("3 s") as written. */
internal fun capsForDisplay(text: String): String {
    val words = text.split(' ')
    return words.mapIndexed { i, w ->
        val afterNumber = i > 0 && words[i - 1].firstOrNull()?.isDigit() == true
        when {
            w.firstOrNull()?.isDigit() == true -> w
            afterNumber && w in Units -> w
            else -> w.uppercase()
        }
    }.joinToString(" ")
}

/**
 * Marks a selected dark tile (a pose tile, a guide-style preview) with a 2dp lavender ring drawn 4dp inside its
 * edge, so the ring always sits on the dark stage, where lavender has strong contrast, whatever surface the tile is
 * on (on cream, a ring at the edge would be a faint halo). Draw it over the tile's content, with the tile's corner
 * [radius]. Pair it with words or semantics (an "In use" caption, a selected state): never colour alone.
 */
fun Modifier.selectedRing(selected: Boolean, radius: Dp, color: Color = PoseitColors.accent): Modifier =
    if (!selected) {
        this
    } else {
        drawWithContent {
            drawContent()
            val inset = 4.dp.toPx()
            val stroke = 2.dp.toPx()
            drawRoundRect(
                color,
                topLeft = Offset(inset + stroke / 2, inset + stroke / 2),
                size = Size(size.width - 2 * inset - stroke, size.height - 2 * inset - stroke),
                cornerRadius = CornerRadius((radius - 4.dp).toPx().coerceAtLeast(0f)),
                style = Stroke(stroke),
            )
        }
    }
