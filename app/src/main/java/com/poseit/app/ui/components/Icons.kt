package com.poseit.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
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

    /** person: the people count on a filter pill. */
    val person: ImageVector get() = Icons.Outlined.Person
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
