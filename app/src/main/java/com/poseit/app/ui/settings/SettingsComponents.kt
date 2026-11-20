package com.poseit.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.data.GridType
import com.poseit.app.data.OverlayStyle
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.components.fitFrameRect
import com.poseit.app.ui.theme.PoseitColors

/** Section title above a group card. */
@Composable
internal fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFFB9A8FF),
        modifier = modifier
            .padding(start = 8.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/** Rounded card grouping several rows, separated by hairlines. */
@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(start = 68.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    )
}

@Composable
internal fun SettingsIcon(icon: ImageVector, accent: Color) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

/** Icon + title + optional subtitle + trailing content. */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon, accent)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** A row that toggles a boolean when tapped anywhere. */
@Composable
internal fun SwitchRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        icon = icon,
        accent = accent,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = PoseitColors.Violet, checkedThumbColor = Color.White),
        )
    }
}

/** A tappable row with a chevron. */
@Composable
internal fun ActionRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showChevron: Boolean = true,
) {
    SettingsRow(
        icon = icon,
        accent = accent,
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        trailing = if (showChevron) {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            null
        },
    )
}

/** Title row with a value label on the right and a slider underneath. */
@Composable
internal fun SliderRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    valueLabel: String,
    subtitle: String?,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    Column(Modifier.padding(bottom = 4.dp)) {
        SettingsRow(icon = icon, accent = accent, title = title, subtitle = subtitle) {
            Text(valueLabel, style = MaterialTheme.typography.titleSmall, color = accent)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            modifier = Modifier
                .padding(start = 64.dp, end = 16.dp)
                .semantics { stateDescription = valueLabel },
        )
    }
}

/** Generic selectable tile with a preview drawn by [preview]. */
@Composable
internal fun ChoiceTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = label,
    preview: @Composable () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (selected) PoseitColors.Violet else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        label = "tileBorder",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .background(if (selected) PoseitColors.Violet.copy(alpha = 0.12f) else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(PreviewScene),
        ) { preview() }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** A camera-ish backdrop so the overlay previews look like they would over the live preview. */
private val PreviewScene = Brush.verticalGradient(listOf(Color(0xFF465A7A), Color(0xFF3A4458), Color(0xFF4B4038)))

internal fun OverlayStyle.toFigureStyle(): FigureStyle = when (this) {
    OverlayStyle.SILHOUETTE -> FigureStyle.SILHOUETTE
    OverlayStyle.OUTLINE -> FigureStyle.OUTLINE
    OverlayStyle.STICK -> FigureStyle.STICK
}

/** The overlay as it would be drawn on the camera, at [opacity]. */
@Composable
internal fun OverlayPreview(template: PoseTemplate, style: OverlayStyle, opacity: Float) {
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(4.dp),
    ) {
        val rect = fitFrameRect(size, template.frameAspect)
        template.figures.forEachIndexed { i, figure ->
            drawPoseFigure(
                figure,
                template.frameAspect,
                rect,
                FigurePaint(
                    style = style.toFigureStyle(),
                    fill = PoseitColors.FigureTints[i % PoseitColors.FigureTints.size],
                    alpha = opacity.coerceIn(0f, 1f),
                ),
            )
        }
    }
}

/** Lines of a composition grid on a small frame. */
@Composable
internal fun GridPreview(grid: GridType) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 1.5.dp.toPx()
        val color = Color.White.copy(alpha = 0.85f)
        val w = size.width
        val h = size.height
        fun verticalAt(f: Float) = drawLine(color, Offset(w * f, 0f), Offset(w * f, h), stroke)
        fun horizontalAt(f: Float) = drawLine(color, Offset(0f, h * f), Offset(w, h * f), stroke)
        when (grid) {
            GridType.NONE -> {
                // A slashed frame reads as "off".
                val r = Rect(Offset(w * 0.3f, h * 0.36f), Offset(w * 0.7f, h * 0.64f))
                drawLine(color.copy(alpha = 0.6f), r.topLeft, r.bottomRight, stroke)
            }
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

/** A row of equally wide tiles. */
@Composable
internal fun TileRow(content: @Composable (Modifier) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) { content(Modifier.weight(1f)) }
}
