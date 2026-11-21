package com.poseit.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.data.GridType
import com.poseit.app.data.OverlayStyle
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.StudioIconSize
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType

/*
 * Pieces of the settings screen that the shared Studio rows don't cover: the group frame, the picture tiles for
 * the guide style and the grid.
 */

/** A settings group: a heading on the gutter, then its rows. The list puts 32dp between groups. */
@Composable
internal fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        SectionHeading(title, Modifier.padding(horizontal = Space.gutter))
        Spacer(Modifier.height(Space.xs))
        content()
    }
}

/** A Hairline between two rows of one group, on the gutters. */
@Composable
internal fun RowDivider() = HairlineDivider(Modifier.padding(horizontal = Space.gutter))

/** The end of a row that opens something else (a file picker, a dialog, the intro). */
@Composable
internal fun RowChevron() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = Studio.Graphite,
        // The glyph has ~8dp of air on its right; move it out so it ends on the gutter, in line with the switches.
        modifier = Modifier
            .offset(x = 7.dp)
            .size(StudioIconSize),
    )
}

/** Equally wide picture tiles under a row title, on the gutters, as one radio group. */
@Composable
internal fun TileRow(content: @Composable (Modifier) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(start = Space.gutter, end = Space.gutter, bottom = Space.m),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) { content(Modifier.weight(1f)) }
}

/**
 * A choice drawn as a small pose-tile-like picture: a Set patch with the Medium radius and the label below it.
 * The selected tile takes a 2dp Ink outline and an Ink label; the others have Graphite labels.
 * @param description the radio button's accessible name
 */
@Composable
internal fun ChoiceTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = label,
    preview: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val outline by animateColorAsState(if (selected) Studio.Ink else Color.Transparent, tween(150), label = "tileOutline")
    Column(
        modifier
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(StudioShapes.Medium)
                .background(Studio.Set)
                .indication(source, ripple())
                .border(2.dp, outline, StudioShapes.Medium),
            content = preview,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            label,
            style = StudioType.label,
            color = if (selected) Studio.Ink else Studio.Graphite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The guide as each style draws it. On the light tile the outline and stick styles are drawn in Ink (the camera
 * draws them in Mannequin white, which would vanish on Set) and the silhouette is the lay figure. The tiles follow
 * the opacity slider, but never fade below 45% so the three styles stay legible.
 */
@Composable
internal fun OverlayPreview(template: PoseTemplate, style: OverlayStyle, opacity: Float) {
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(Space.s),
    ) {
        val rect = fitFrameRect(size, template.frameAspect)
        template.figures.forEachIndexed { i, figure ->
            val paint = when (style) {
                OverlayStyle.SILHOUETTE -> FigurePaint(fill = Studio.FigureTones[i % Studio.FigureTones.size])
                OverlayStyle.OUTLINE -> FigurePaint(style = FigureStyle.OUTLINE, fill = Studio.Ink)
                OverlayStyle.STICK -> FigurePaint(style = FigureStyle.STICK, fill = Studio.Ink, showFaceMarker = false)
            }
            drawPoseFigure(figure, template.frameAspect, rect, paint.copy(alpha = 0.35f + 0.65f * opacity.coerceIn(0f, 1f)))
        }
    }
}

/** The lines of a composition grid across the tile. "Off" is the bare tile. */
@Composable
internal fun GridPreview(grid: GridType) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 1.dp.toPx()
        val color = Studio.Ink.copy(alpha = 0.5f)
        val w = size.width
        val h = size.height
        fun verticalAt(f: Float) = drawLine(color, Offset(w * f, 0f), Offset(w * f, h), stroke)
        fun horizontalAt(f: Float) = drawLine(color, Offset(0f, h * f), Offset(w, h * f), stroke)
        when (grid) {
            GridType.NONE -> Unit
            GridType.THIRDS -> {
                verticalAt(1f / 3f); verticalAt(2f / 3f); horizontalAt(1f / 3f); horizontalAt(2f / 3f)
            }
            GridType.GOLDEN -> {
                verticalAt(0.382f); verticalAt(0.618f); horizontalAt(0.382f); horizontalAt(0.618f)
            }
            GridType.CENTER -> {
                val arm = w * 0.16f
                drawLine(color, Offset(w / 2 - arm, h / 2), Offset(w / 2 + arm, h / 2), stroke)
                drawLine(color, Offset(w / 2, h / 2 - arm), Offset(w / 2, h / 2 + arm), stroke)
            }
        }
    }
}

internal val GridType.shortLabel: String
    get() = when (this) {
        GridType.NONE -> "Off"
        GridType.THIRDS -> "Thirds"
        GridType.GOLDEN -> "Golden"
        GridType.CENTER -> "Centre"
    }
