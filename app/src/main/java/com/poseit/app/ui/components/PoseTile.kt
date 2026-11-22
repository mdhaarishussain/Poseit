package com.poseit.app.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitRadius
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/** [PoseTile] sizes. The caller sets the width. */
enum class PoseTileSize {
    /** Library grid: a dark card holding the figure (3:4, the shape of a pose's photo frame) and its caption. */
    Regular,

    /** Camera pose strip: the 3:4 figure tile alone, or with one short line below it. */
    Compact,
}

/**
 * A pose tile (docs/DESIGN.md, "Pose tile"): a dark card, like the mockups' cards, with the pose's white lay
 * figures standing on a flat floor spot and, in [PoseTileSize.Regular], the caption inside the card below the
 * figure: the name in Body strong cream and the category in Supporting mist. 20dp corners (12dp when compact), no
 * shadow, no gradient. The favourite toggle is a cream outlined heart in the top-right corner.
 *
 * Interaction: pass [onClick] for a plain tap. For long-press menus or custom accessibility actions, leave
 * [onClick] null and put `combinedClickable(interactionSource = source, indication = null, ...)` and any
 * `semantics { customActions = ... }` in [modifier] (it applies to the whole tile, which merges into one node),
 * passing the same [interactionSource] so the ripple is drawn on the card. For a selectable tile (the camera strip),
 * put `selectable(selected, ...)` in [modifier] the same way and pass [selected] for the lavender outline.
 *
 * @param name first caption line (Body strong); in [PoseTileSize.Compact] the only line (Supporting), below the
 *   tile, null by default
 * @param detail second caption line (Supporting, mist), e.g. the category; [PoseTileSize.Regular] only
 * @param onToggleFavorite shows the favourite toggle (outlined heart, filled when [isFavorite]); Regular only, a
 *   compact tile is too small for its 48dp target
 * @param selected draws a 2dp lavender outline around the tile
 * @param onDark the compact caption's colour for the camera's black surround (cream instead of ink)
 * @param nameMinLines reserve this many lines for the name (up to two), so the cards of a grid row line up
 * @param overlay extra content drawn over the figure area, e.g. a [Badge] in a corner
 * @param badge drawn in the card's bottom-end corner beside the caption, where no figure ever stands (a pose's
 *   photo print, [PhotoBadge]: align it with `Modifier.align(Alignment.BottomEnd)`); [PoseTileSize.Regular] only
 * @param badgeReserve the width the caption's text leaves free for [badge] at its end
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
    badge: (@Composable BoxScope.() -> Unit)? = null,
    badgeReserve: Dp = 0.dp,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val regular = size == PoseTileSize.Regular
    val shape = if (regular) PoseitShapes.Tile else PoseitShapes.Control
    val click = if (onClick != null) {
        Modifier.clickable(interactionSource = source, indication = null, onClickLabel = onClickLabel, onClick = onClick)
    } else {
        Modifier
    }
    val cornerBadge = badge.takeIf { regular }
    Column(modifier.then(click)) {
        ProvideSurfaceTone(SurfaceTone.Dark) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(PoseitColors.backgroundStage)
                        .indication(source, ripple())
                        .selectedRing(selected, if (regular) PoseitRadius.tile else PoseitRadius.control),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                    ) {
                        PoseThumbnail(
                            template = pose,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = if (regular) Space.m else Space.xs,
                                    end = if (regular) Space.m else Space.xs,
                                    top = if (regular) Space.l else Space.xs,
                                    bottom = if (regular) Space.xs else Space.xs,
                                ),
                            mirrored = mirrored,
                            background = null,
                        )
                        overlay()
                        if (regular && onToggleFavorite != null) {
                            val label = name ?: pose.name
                            PlainIconButton(
                                icon = if (isFavorite) PoseitIcons.favouriteOn else PoseitIcons.favouriteOff,
                                contentDescription = if (isFavorite) "Remove $label from favourites" else "Add $label to favourites",
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(Space.xs),
                                tint = PoseitColors.textPrimaryInverted,
                            )
                        }
                    }
                    if (regular && (name != null || detail != null)) {
                        val badgeRoom = if (cornerBadge != null) badgeReserve else 0.dp
                        Column(Modifier.padding(start = Space.l, end = Space.m + badgeRoom, bottom = Space.l)) {
                            if (name != null) {
                                // Two lines, so a long name wraps instead of being cut off in a narrow column.
                                Text(
                                    name,
                                    style = PoseitType.bodyStrong,
                                    color = PoseitColors.textPrimaryInverted,
                                    minLines = nameMinLines.coerceIn(1, 2),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (detail != null) {
                                Text(
                                    detail,
                                    style = PoseitType.supporting,
                                    color = PoseitColors.textSecondaryInverted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                // Beside the caption, in the corner no figure reaches (a pose's photo print).
                cornerBadge?.invoke(this)
            }
        }
        if (!regular && name != null) {
            Spacer(Modifier.height(Space.xs))
            Text(
                name,
                style = PoseitType.supporting,
                color = if (onDark) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
