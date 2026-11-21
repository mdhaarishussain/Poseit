package com.poseit.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The only corner radii in Studio. Circles are allowed only where the camera vernacular calls for them (shutter,
 * round camera controls, the score ring).
 */
object StudioShapes {
    /** The radius of [Small], for drawing code (a Canvas round rect) that needs the number. */
    val SmallRadius: Dp = 6.dp

    /** 6dp: chips, text fields, small buttons, tags, menus. */
    val Small = RoundedCornerShape(SmallRadius)

    /** 12dp: pose tiles, buttons, dialogs, cards, the camera cue card. */
    val Medium = RoundedCornerShape(12.dp)

    /** 20dp on all corners. Rarely right; bottom sheets use [SheetTop]. */
    val Large = RoundedCornerShape(20.dp)

    /** 20dp top corners: bottom sheets. */
    val SheetTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

/**
 * M3 shape slots. Note that M3 dialogs read `extraLarge` (20dp): pass `shape = StudioShapes.Medium` to an
 * AlertDialog. Bottom sheets read the top of `extraLarge`, which is [StudioShapes.SheetTop].
 */
internal val StudioMaterialShapes = Shapes(
    extraSmall = StudioShapes.Small,
    small = StudioShapes.Small,
    medium = StudioShapes.Medium,
    large = StudioShapes.Large,
    extraLarge = StudioShapes.Large,
)

/** The 4dp spacing grid. The screen gutter is [gutter] and the gap between sections is [section]. */
object Space {
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp

    /** Left and right screen padding. */
    val gutter: Dp = 20.dp
    val xl: Dp = 24.dp

    /** Between sections and setting groups. */
    val section: Dp = 32.dp
    val xxl: Dp = 48.dp
}
