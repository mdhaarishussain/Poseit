package com.poseit.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.HairlineDivider
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PoseTileSize
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.SettingRow
import com.poseit.app.ui.components.SliderRow
import com.poseit.app.ui.components.StudioChip
import com.poseit.app.ui.components.StudioIconButton
import com.poseit.app.ui.components.StudioLargeTopBar
import com.poseit.app.ui.components.StudioSlider
import com.poseit.app.ui.components.StudioTabs
import com.poseit.app.ui.components.StudioTopBar
import com.poseit.app.ui.components.SwitchRow
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.Studio
import com.poseit.app.ui.theme.StudioShapes
import com.poseit.app.ui.theme.StudioType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every shared Studio component, the type scale and the colour tokens into
 * app/build/screenshots/components.png, two phone-width columns side by side, for design review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w860dp-h1800dp-xhdpi")
class ComponentGalleryScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun components() {
        compose.setContent {
            PoseitTheme {
                Row(
                    Modifier
                        .fillMaxSize()
                        .background(Studio.Paper)
                        .padding(vertical = Space.xl),
                    horizontalArrangement = Arrangement.spacedBy(Space.l),
                ) {
                    Column(Modifier.width(420.dp)) { LeftColumn() }
                    Column(Modifier.width(420.dp)) { RightColumn() }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/components.png")
    }
}

private val poses get() = BuiltInPoses.all
private val NoInsets = WindowInsets(0)

@Composable
private fun ColumnScope.LeftColumn() {
    StudioLargeTopBar("Poseit", windowInsets = NoInsets) {
        StudioIconButton(Icons.Outlined.PhotoLibrary, "Your photos", {})
        StudioIconButton(Icons.Outlined.Settings, "Settings", {})
    }
    Gallery("Colour") {
        SwatchGroup("Surfaces", listOf("Paper" to Studio.Paper, "Set" to Studio.Set, "Sheet" to Studio.Sheet))
        SwatchGroup("Ink", listOf("Ink" to Studio.Ink, "Graphite" to Studio.Graphite, "Hairline" to Studio.Hairline))
        SwatchGroup(
            "Figure tones",
            Studio.FigureTones.mapIndexed { i, c -> (if (i == 0) "Mannequin" else "Tone ${i + 1}") to c },
        )
        SwatchGroup("Signal", listOf("Miss" to Studio.Miss, "Near" to Studio.Near, "Match" to Studio.Match))
        SwatchGroup(
            "Signal on the camera",
            listOf("Miss" to Studio.MissBright, "Near" to Studio.NearBright, "Match" to Studio.MatchBright),
            ground = Studio.LiveView,
        )
    }
    Gallery("Type") {
        TypeSample("Display 28/32", StudioType.display, "Hand on hip")
        TypeSample("Title 20/26", StudioType.title, "Quick settings")
        TypeSample("Bar title 18/24", StudioType.barTitle, "Your photos")
        TypeSample("Heading 17/24", StudioType.heading, "Camera setup")
        TypeSample("Body 15/22", StudioType.body, "Turn your shoulders slightly away from the camera and drop your chin.")
        TypeSample("Body strong 15/22", StudioType.bodyStrong, "Voice coaching")
        TypeSample("Label large 14/20", StudioType.labelLarge, "Try this pose")
        TypeSample("Label 13/18", StudioType.label, "Standing, easy")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.xl)) {
            Text("87", style = StudioType.numeral(64.sp), color = Studio.Ink)
            Text("3", style = StudioType.numeral(40.sp), color = Studio.Ink)
            Text("12 poses", style = StudioType.numeral(15.sp), color = Studio.Ink, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
    Gallery("Sliders") {
        var light by remember { mutableFloatStateOf(0.3f) }
        StudioSlider(light, { light = it })
        var countdown by remember { mutableFloatStateOf(3f) }
        StudioSlider(countdown, { countdown = it }, valueRange = 0f..10f, steps = 9)
        var dark by remember { mutableFloatStateOf(0.7f) }
        Box(
            Modifier
                .fillMaxWidth()
                .background(Studio.LiveView, StudioShapes.Medium)
                .padding(horizontal = Space.m),
        ) { StudioSlider(dark, { dark = it }, onDark = true) }
    }
}

@Composable
private fun ColumnScope.RightColumn() {
    StudioTopBar("Settings", onBack = {}, windowInsets = NoInsets) {
        StudioIconButton(Icons.Outlined.FavoriteBorder, "Add to favourites", {})
    }
    Gallery("Buttons") {
        PrimaryButton("Try this pose", {}, Modifier.fillMaxWidth(), icon = Icons.Outlined.CameraAlt)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Edit pose", {})
            TertiaryButton("Cancel", {})
            DestructiveButton("Delete pose", {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            DestructiveButton("Delete pose", {}, filled = true)
            PrimaryButton("Save pose", {}, enabled = false)
        }
    }
    Gallery("Tabs and chips", gap = Space.l) {
        var tab by remember { mutableIntStateOf(1) }
        StudioTabs(listOf("All", "Favourites", "Recent", "My poses"), tab, { tab = it })
        var people by remember { mutableIntStateOf(0) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf("Solo", "Couple", "Group").forEachIndexed { i, label ->
                StudioChip(label, selected = people == i, onClick = { people = if (people == i) -1 else i }, leadingIcon = Icons.Outlined.Person)
            }
            StudioChip("Beach", selected = false, onClick = {})
            StudioChip("City street", selected = false, onClick = {})
            StudioChip("Elegant", selected = false, onClick = {}, enabled = false)
        }
    }
    Section {
        var voice by remember { mutableStateOf(true) }
        var level by remember { mutableStateOf(false) }
        var opacity by remember { mutableFloatStateOf(0.6f) }
        SectionHeading("Guide", Modifier.padding(horizontal = Space.gutter))
        Spacer(Modifier.height(Space.xs))
        SwitchRow("Voice guidance", voice, { voice = it }, subtitle = "Spoken hints and countdown")
        HairlineDivider(Modifier.padding(horizontal = Space.gutter))
        SwitchRow("Level indicator", level, { level = it })
        HairlineDivider(Modifier.padding(horizontal = Space.gutter))
        SliderRow("Guide opacity", "${(opacity * 100).toInt()}%", opacity, { opacity = it })
        HairlineDivider(Modifier.padding(horizontal = Space.gutter))
        SettingRow("Back up your poses", subtitle = "Save them to a file", onClick = {}) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Studio.Graphite)
        }
    }
    Gallery("Pose tiles") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            PoseTile(poses[0], Modifier.weight(1f), isFavorite = true, onToggleFavorite = {}, onClick = {})
            val couple = poses.first { it.personCount == 2 }
            PoseTile(couple, Modifier.weight(1f), detail = "Couple", onToggleFavorite = {}, onClick = {})
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Studio.LiveView, StudioShapes.Medium)
                .padding(Space.m),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            poses.drop(4).take(5).forEachIndexed { i, p ->
                PoseTile(p, Modifier.width(60.dp), size = PoseTileSize.Compact, selected = i == 1, onDark = true, onClick = {})
            }
        }
    }
    Gallery("Frame marks and empty state") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .width(132.dp)
                    .background(Studio.Set, StudioShapes.Medium)
                    .padding(Space.m),
            ) {
                PoseThumbnail(
                    poses[2],
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(poses[2].frameAspect)
                        .frameMarks(),
                    background = null,
                )
            }
            EmptyState(
                "No favourites yet",
                "Tap the heart on a pose to keep it here.",
                Modifier.weight(1f),
                action = { SecondaryButton("Browse poses", {}) },
            )
        }
    }
}

/** A gallery section: heading on the gutter, then its content inset by the gutter. */
@Composable
private fun Gallery(title: String, gap: Dp = Space.m, content: @Composable ColumnScope.() -> Unit) {
    Section {
        SectionHeading(title, Modifier.padding(horizontal = Space.gutter))
        Spacer(Modifier.height(Space.m))
        Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(gap), content = content)
    }
}

/** 32dp section gap above anything but the first block. */
@Composable
private fun Section(content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(Space.section))
    Column(content = content)
}

@Composable
private fun SwatchGroup(name: String, colors: List<Pair<String, Color>>, ground: Color = Studio.Paper) {
    Column {
        Text(name, style = StudioType.label, color = Studio.Graphite)
        Spacer(Modifier.height(Space.xs))
        val inset = if (ground == Studio.Paper) 0.dp else Space.s
        Row(
            Modifier
                .fillMaxWidth()
                .background(ground, StudioShapes.Small)
                .padding(inset),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            colors.forEach { (label, color) ->
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(color, StudioShapes.Small)
                            .border(1.dp, Studio.Hairline, StudioShapes.Small),
                    )
                    val onGround = if (ground.luminance() < 0.5f) Studio.Mannequin else Studio.Ink
                    Text(
                        "$label ${hex(color)}",
                        style = StudioType.label.copy(fontSize = 12.sp),
                        color = onGround,
                        modifier = Modifier.padding(top = Space.xs),
                    )
                }
            }
        }
    }
}

private fun hex(c: Color): String {
    val argb = (c.alpha * 255).toInt() shl 24 or ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
    return if (c.alpha < 1f) "${(c.alpha * 100).toInt()}%" else "#%06X".format(argb and 0xFFFFFF)
}

@Composable
private fun TypeSample(role: String, style: TextStyle, sample: String) {
    Column {
        Text(role, style = StudioType.label.copy(fontSize = 12.sp), color = Studio.Graphite)
        Text(sample, style = style, color = MaterialTheme.colorScheme.onBackground)
    }
}
