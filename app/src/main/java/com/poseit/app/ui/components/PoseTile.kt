package com.poseit.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/** [PoseTile] sizes. The caller sets the width; the tile itself is always 3:4, the shape of a pose's photo frame. */
enum class PoseTileSize {
    /** Library grid: tile plus a caption (name, and a secondary line) below it on the background. */
    Regular,

    /** Camera pose strip: the tile alone, or with one short line in the Label style. */
    Compact,
}

/**
 * A pose tile (docs/DESIGN.md): a Set-coloured Medium-radius patch of paper holding the pose's lay figures and their
 * contact shadows, with the caption below it on the background. No container around the caption, no shadow, no
 * gradient.
 *
 * Interaction: pass [onClick] for a plain tap. For long-press menus or custom accessibility actions, leave
 * [onClick] null and put `combinedClickable(interactionSource = source, indication = null, ...)` and any
 * `semantics { customActions = ... }` in [modifier] (it applies to the whole tile and caption, which merge into one
 * node), passing the same [interactionSource] so the ripple is drawn on the tile only. For a selectable tile (the
 * camera strip), put `selectable(selected, ...)` in [modifier] the same way and pass [selected] for the outline.
 *
 * @param name first caption line (Body strong); in [PoseTileSize.Compact] the only line (Label), null by default
 * @param detail second caption line (Label, Graphite), e.g. the category; [PoseTileSize.Regular] only
 * @param onToggleFavorite shows the favourite toggle in the tile's top-right corner (outlined heart, filled when
 *   [isFavorite]); [PoseTileSize.Regular] only, a compact tile is too small for its 48dp target
 * @param selected draws a 2dp outline around the tile (Ink, or Mannequin when [onDark])
 * @param onDark caption and outline colours for the camera's black surround; the tile stays Set
 * @param nameMinLines reserve this many lines for the name (up to two), so the captions of a dense grid line up
 * @param overlay extra content drawn over the tile, e.g. a small tag in a corner
 */
@Composable
fun PoseTile(
    pose: PoseTemplate,
    modifier: Modifier = Modifier,
    size: PoseTileSize = PoseTileSize.Regular,
    name: String? = if (size == PoseTileSize.Regular) pose.name else null,
    detail: String? = if (size == PoseTileSize.Regular) pose.category.label else null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    selected: Boolean = false,
    onDark: Boolean = false,
    mirrored: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    nameMinLines: Int = 1,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val regular = size == PoseTileSize.Regular
    val click = if (onClick != null) {
        Modifier.clickable(interactionSource = source, indication = null, onClickLabel = onClickLabel, onClick = onClick)
    } else {
        Modifier
    }
    Column(modifier.then(click)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(StudioShapes.Medium)
                .background(Studio.Set)
                .indication(source, ripple())
                .then(
                    if (selected) Modifier.border(2.dp, if (onDark) Studio.Mannequin else Studio.Ink, StudioShapes.Medium) else Modifier,
                ),
        ) {
            PoseThumbnail(
                template = pose,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (regular) Space.s else Space.xs),
                mirrored = mirrored,
                background = null,
            )
            overlay()
            if (regular && onToggleFavorite != null) {
                val label = name ?: pose.name
                StudioIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove $label from favourites" else "Add $label to favourites",
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        val nameColor = if (onDark) Studio.Mannequin else Studio.Ink
        if (regular) {
            if (name != null || detail != null) Spacer(Modifier.height(Space.s))
            if (name != null) {
                // Two lines, so a long name wraps instead of being cut off in a narrow column.
                Text(
                    name,
                    style = StudioType.bodyStrong,
                    color = nameColor,
                    minLines = nameMinLines.coerceIn(1, 2),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (detail != null) {
                Text(
                    detail,
                    style = StudioType.label,
                    color = if (onDark) Studio.Mannequin.copy(alpha = 0.7f) else Studio.Graphite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (name != null) {
            Spacer(Modifier.height(Space.xs))
            Text(name, style = StudioType.label, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
