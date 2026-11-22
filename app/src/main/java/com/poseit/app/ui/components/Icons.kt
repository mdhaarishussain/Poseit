package com.poseit.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Poseit's icon vocabulary (docs/DESIGN.md, "Icons"). The first group follows Astryx's built-in icon names and its
 * Android mapping to Material Symbols (packages/native, NATIVE_ICON_DEFAULTS); the glyphs come from the Outlined
 * Material Icons in androidx (the same outlined family, already bundled). The second group is Poseit's own.
 *
 * Use outlined glyphs; the one filled glyph is [favouriteOn], for an active toggle. Keep icons few: the mockups use
 * one icon per control and none as decoration.
 */
object PoseitIcons {
    // Astryx built-in names (Material Symbols names in comments).
    /** close */
    val close: ImageVector get() = Icons.Outlined.Close

    /** chevron_right: the end of an outlined row that opens something. */
    val chevronRight: ImageVector get() = Icons.AutoMirrored.Outlined.KeyboardArrowRight

    /** expand_more */
    val chevronDown: ImageVector get() = Icons.Outlined.ExpandMore

    /** check */
    val check: ImageVector get() = Icons.Outlined.Check

    /** check_circle (Astryx "success") */
    val success: ImageVector get() = Icons.Outlined.CheckCircle

    /** error (Astryx "error") */
    val error: ImageVector get() = Icons.Outlined.ErrorOutline

    /** warning */
    val warning: ImageVector get() = Icons.Outlined.WarningAmber

    /** info */
    val info: ImageVector get() = Icons.Outlined.Info

    /** A link that leaves the app for the browser (material symbol: open_in_new). */
    val openExternal: ImageVector get() = Icons.AutoMirrored.Outlined.OpenInNew

    /** more_horiz (Astryx "moreHorizontal"): the overflow circle of the floating pill bar. */
    val moreHorizontal: ImageVector get() = Icons.Outlined.MoreHoriz

    /** search */
    val search: ImageVector get() = Icons.Outlined.Search

    // Poseit's own.
    /** arrow_back: the top bar's back button. */
    val back: ImageVector get() = Icons.AutoMirrored.Outlined.ArrowBack

    /** photo_camera: the pose camera. */
    val camera: ImageVector get() = Icons.Outlined.CameraAlt

    /** photo_library: your photos. */
    val photos: ImageVector get() = Icons.Outlined.PhotoLibrary

    /** settings */
    val settings: ImageVector get() = Icons.Outlined.Settings

    /** add: create a pose. */
    val add: ImageVector get() = Icons.Outlined.Add

    /** favorite (outlined): add to favourites. */
    val favouriteOff: ImageVector get() = Icons.Outlined.FavoriteBorder

    /** favorite (filled): in favourites. The only filled glyph. */
    val favouriteOn: ImageVector get() = Icons.Filled.Favorite

    /** edit */
    val edit: ImageVector get() = Icons.Outlined.Edit

    /** delete */
    val delete: ImageVector get() = Icons.Outlined.Delete

    /** share */
    val share: ImageVector get() = Icons.Outlined.Share

    /** person: the people count on a filter pill (one person). */
    val person: ImageVector get() = Icons.Outlined.Person

    /**
     * group (Material Symbols): two people, for a couple. The hollow PeopleOutline glyph, drawn with the same stroke
     * as [person], so the people pills read as one family (the legacy Outlined People draws solid figures).
     */
    val couple: ImageVector get() = Icons.Outlined.PeopleOutline

    /**
     * groups (Material Symbols): three people, for a group. Drawn here with the hollow head and body of [person]
     * flanked by two people behind, because the bundled Outlined Groups draws its side figures solid and Outlined
     * Group is the same two-person glyph as [couple].
     */
    val group: ImageVector get() = GroupGlyph
}

/** The three-person glyph of [PoseitIcons.group], on the 24dp grid of the Outlined set, with its ~1.6dp walls. */
private val GroupGlyph: ImageVector by lazy {
    ImageVector.Builder(name = "Poseit.Group", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            fun circle(cx: Float, cy: Float, r: Float) {
                moveTo(cx - r, cy)
                arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = r * 2, dy1 = 0f)
                arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -r * 2, dy1 = 0f)
                close()
            }
            // The person in front: a ring for the head, a hollow shoulder shape for the body (as in Outlined Person).
            circle(12f, 8.4f, 3.4f)
            circle(12f, 8.4f, 1.6f)
            moveTo(6.6f, 19f)
            verticalLineTo(17.9f)
            curveTo(6.6f, 15.9f, 9.9f, 14.7f, 12f, 14.7f)
            curveTo(14.1f, 14.7f, 17.4f, 15.9f, 17.4f, 17.9f)
            verticalLineTo(19f)
            close()
            moveTo(8.7f, 17.3f)
            curveTo(9.4f, 16.9f, 10.7f, 16.5f, 12f, 16.5f)
            curveTo(13.3f, 16.5f, 14.6f, 16.9f, 15.3f, 17.3f)
            close()
            // The two behind: smaller head rings and the slivers of their shoulders either side.
            circle(4.9f, 10f, 2.5f)
            circle(4.9f, 10f, 1f)
            circle(19.1f, 10f, 2.5f)
            circle(19.1f, 10f, 1f)
            moveTo(1f, 19f)
            verticalLineTo(18.2f)
            curveTo(1f, 16.8f, 2.6f, 15.7f, 4.9f, 15.4f)
            curveTo(4.1f, 16.1f, 3.6f, 17f, 3.6f, 17.9f)
            verticalLineTo(19f)
            close()
            moveTo(23f, 19f)
            verticalLineTo(18.2f)
            curveTo(23f, 16.8f, 21.4f, 15.7f, 19.1f, 15.4f)
            curveTo(19.9f, 16.1f, 20.4f, 17f, 20.4f, 17.9f)
            verticalLineTo(19f)
            close()
        }
        .build()
}

/** Icon sizes: [standalone] glyphs (icon buttons, row ends) and [inline] ones next to a label. */
object IconSize {
    /** 24dp: every standalone icon, in a 48dp touch target. */
    val standalone: Dp = 24.dp

    /** 18dp: an icon beside button, pill or badge text. */
    val inline: Dp = 18.dp
}

/** The air between a 48dp touch target and its 24dp glyph: pad a bar by `x - IconGlyphInset` to put glyphs on x. */
val IconGlyphInset: Dp = (48.dp - IconSize.standalone) / 2
