package com.poseit.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.poseit.app.ui.components.RowGroup
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitMotion
import com.poseit.app.ui.theme.PoseitRadius
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

/*
 * Pieces of the settings screen that the shared rows don't cover: the group frame, and the small dark stage tiles
 * that preview the guide style and the grid.
 */

/**
 * A settings group: a [SectionHeading] in line with the text inside cards, then its rows in one outlined cream
 * [RowGroup] (which draws the hairlines between rows). Each direct child of [content] is one row. Expects to sit
 * [Space.edge] in from the screen sides, like every card.
 */
@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SectionHeading(title, Modifier.padding(horizontal = Space.cardPadding), action = action)
        Spacer(Modifier.height(Space.s))
        RowGroup(content = content)
    }
}

/** A hairline between two rows that are not in a RowGroup (the camera's Quick settings sheet), on the row padding. */
@Composable
internal fun RowDivider() = HairlineDivider(Modifier.padding(horizontal = Space.cardPadding))

/** A row title and subtitle above content of its own (picture tiles), padded like the other rows of a group. */
@Composable
internal fun TitledRow(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(TitledRowPadding)) {
        Text(title, style = PoseitType.bodyStrong, color = PoseitColors.textPrimary)
        if (subtitle != null) Text(subtitle, style = PoseitType.supporting, color = PoseitColors.textSecondary)
        Spacer(Modifier.height(Space.m))
        content()
    }
}

private val TitledRowPadding = PaddingValues(start = Space.cardPadding, end = Space.cardPadding, top = Space.m, bottom = Space.l)

/** Equally wide picture tiles as one radio group. */
@Composable
internal fun TileRow(content: @Composable (Modifier) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) { content(Modifier.weight(1f)) }
}

private val RingInset = 4.dp
private val RingWidth = 2.dp

/**
 * A choice drawn as a small dark stage tile (3:4, 12dp corners) with its label below (two lines at most, for large
 * font sizes). The selected tile takes a 2dp lavender ring 4dp inside its edge and a primary, medium-weight label; the others have no ring and secondary
 * labels, so the choice never rests on colour alone.
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
    val ring by animateColorAsState(
        if (selected) PoseitColors.accent else Color.Transparent,
        PoseitMotion.standard(PoseitMotion.FAST),
        label = "tileRing",
    )
    Column(
        modifier
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(PoseitShapes.Control)
                .background(PoseitColors.backgroundStage)
                .indication(source, ripple(color = PoseitColors.textPrimaryInverted))
                .drawWithContent {
                    drawContent()
                    // The ring sits 4dp inside the edge, on the dark stage, where lavender has strong contrast.
                    val inset = RingInset.toPx()
                    val stroke = RingWidth.toPx()
                    val corner = (PoseitRadius.control - RingInset).toPx()
                    drawRoundRect(
                        ring,
                        topLeft = Offset(inset + stroke / 2, inset + stroke / 2),
                        size = Size(size.width - 2 * inset - stroke, size.height - 2 * inset - stroke),
                        cornerRadius = CornerRadius(corner),
                        style = Stroke(stroke),
                    )
                },
            content = preview,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            label,
            style = if (selected) PoseitType.bodyStrong else PoseitType.body,
            color = if (selected) PoseitColors.textPrimary else PoseitColors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The guide as each style draws it over the camera: cream on the dark stage, like the live view. The tiles follow
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
                OverlayStyle.SILHOUETTE -> FigurePaint(fill = PoseitColors.figureTones[i % PoseitColors.figureTones.size])
                OverlayStyle.OUTLINE -> FigurePaint(style = FigureStyle.OUTLINE, fill = PoseitColors.figureFill)
                OverlayStyle.STICK -> FigurePaint(style = FigureStyle.STICK, fill = PoseitColors.figureFill, showFaceMarker = false)
            }
            drawPoseFigure(figure, template.frameAspect, rect, paint.copy(alpha = 0.45f + 0.55f * opacity.coerceIn(0f, 1f)))
        }
    }
}

/** The lines of a composition grid across the dark tile, in viewfinder cream. "Off" is the bare stage. */
@Composable
internal fun GridPreview(grid: GridType) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 1.dp.toPx()
        val color = PoseitColors.textPrimaryInverted.copy(alpha = 0.6f)
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
