package com.poseit.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radii by role (Astryx: radius follows role, nested curves stay concentric). These seven are the only
 * radii in v2; a card's inner element uses a radius about [Space.cardPadding] smaller than the card's.
 */
object PoseitRadius {
    /** 8dp: badges, icon tiles and thumbnails inside a card, the selected option inside a segmented pill's 4dp inset. */
    val inner: Dp = 8.dp

    /** 12dp: block buttons inside cards, text fields, menus, snackbars, the camera cue card. */
    val control: Dp = 12.dp

    /** 16dp: outlined rows and row groups. */
    val row: Dp = 16.dp

    /** 20dp: pose tiles and photo thumbnails in a grid. */
    val tile: Dp = 20.dp

    /** 24dp: dark cards, connected card stacks, the top bar card. */
    val card: Dp = 24.dp

    /** 28dp: bottom sheet top corners, dialogs, the onboarding card. */
    val sheet: Dp = 28.dp
}

/** Shapes for the [PoseitRadius] roles, plus [Pill] and [Circle]. */
object PoseitShapes {
    val Inner = RoundedCornerShape(PoseitRadius.inner)
    val Control = RoundedCornerShape(PoseitRadius.control)
    val Row = RoundedCornerShape(PoseitRadius.row)
    val Tile = RoundedCornerShape(PoseitRadius.tile)
    val Card = RoundedCornerShape(PoseitRadius.card)
    val Sheet = RoundedCornerShape(PoseitRadius.sheet)

    /** Bottom sheets: [PoseitRadius.sheet] on the top corners only. */
    val SheetTop = RoundedCornerShape(topStart = PoseitRadius.sheet, topEnd = PoseitRadius.sheet)

    /** Pill buttons, filter pills, the segmented pill, the progress bar ends. */
    val Pill = RoundedCornerShape(percent = 50)

    /** Circle icon buttons, the shutter, the score ring. */
    val Circle = CircleShape
}

/** M3 shape slots. Dialogs read `extraLarge` (28dp) and bottom sheets read its top corners. */
internal val PoseitMaterialShapes = Shapes(
    extraSmall = PoseitShapes.Inner,
    small = PoseitShapes.Control,
    medium = PoseitShapes.Row,
    large = PoseitShapes.Card,
    extraLarge = PoseitShapes.Sheet,
)

/**
 * The 4dp spacing grid (Astryx spacing steps 1 to 12 are 4dp each).
 *
 * Layout model from the mockups: cards sit [edge] (8dp) from the screen sides and [edge] apart, with [cardPadding]
 * (24dp) inside, so card text lines up at [textInset] (32dp) from the screen edge. Text placed directly on the canvas
 * between cards uses [textInset] too, so everything shares one left line.
 */
object Space {
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 48.dp

    /** Cards to the screen sides, and the gap between separate cards (connected cards sit 10dp apart). */
    val edge: Dp = 8.dp

    /** Inside a dark card or the top bar card. */
    val cardPadding: Dp = 24.dp

    /** Where text starts on the canvas: [edge] + [cardPadding], in line with text inside cards. */
    val textInset: Dp = 32.dp

    /** Between groups of content on the canvas (a card stack and the next heading, setting groups). */
    val section: Dp = 24.dp
}
