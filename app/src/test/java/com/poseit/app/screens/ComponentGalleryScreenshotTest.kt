package com.poseit.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.ui.components.AccentButton
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.BadgeTone
import com.poseit.app.ui.components.Banner
import com.poseit.app.ui.components.BannerStatus
import com.poseit.app.ui.components.ButtonForm
import com.poseit.app.ui.components.CapsLabel
import com.poseit.app.ui.components.CardSegment
import com.poseit.app.ui.components.CircleIconButton
import com.poseit.app.ui.components.CircleIconToggle
import com.poseit.app.ui.components.CircleVariant
import com.poseit.app.ui.components.CompactStat
import com.poseit.app.ui.components.ConnectedCardRow
import com.poseit.app.ui.components.ConnectedCardStack
import com.poseit.app.ui.components.DestructiveButton
import com.poseit.app.ui.components.DialogCard
import com.poseit.app.ui.components.EmptyState
import com.poseit.app.ui.components.FilterPill
import com.poseit.app.ui.components.FloatingPillBar
import com.poseit.app.ui.components.IconToggle
import com.poseit.app.ui.components.OutlinedRow
import com.poseit.app.ui.components.PillBarItemSize
import com.poseit.app.ui.components.PillBarSegments
import com.poseit.app.ui.components.PillTabs
import com.poseit.app.ui.components.PlainIconButton
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.PoseTile
import com.poseit.app.ui.components.PoseTileSize
import com.poseit.app.ui.components.PoseitCard
import com.poseit.app.ui.components.PoseitIcons
import com.poseit.app.ui.components.PoseitSnackbarHost
import com.poseit.app.ui.components.PoseitSpinner
import com.poseit.app.ui.components.PrimaryButton
import com.poseit.app.ui.components.ProvideSurfaceTone
import com.poseit.app.ui.components.RadioRow
import com.poseit.app.ui.components.RowGroup
import com.poseit.app.ui.components.SecondaryButton
import com.poseit.app.ui.components.SectionHeading
import com.poseit.app.ui.components.SegmentedPill
import com.poseit.app.ui.components.SegmentedProgress
import com.poseit.app.ui.components.SettingRow
import com.poseit.app.ui.components.SheetHandle
import com.poseit.app.ui.components.SliderRow
import com.poseit.app.ui.components.Stat
import com.poseit.app.ui.components.StatRow
import com.poseit.app.ui.components.SwitchRow
import com.poseit.app.ui.components.TertiaryButton
import com.poseit.app.ui.components.TopBarCard
import com.poseit.app.ui.components.ValueSlider
import com.poseit.app.ui.components.RowChevron
import com.poseit.app.ui.components.frameMarks
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/**
 * Renders design system v2 into app/build/screenshots/components.png for design review: every colour role with its
 * contrast on the surface it is meant for, the type scale, every shared component in its states (on cream and on a
 * dark card), and a mock home screen built only from the shared components. Three phone-width columns.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1300dp-h2600dp-xhdpi")
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
                        .background(PoseitColors.backgroundBody)
                        .padding(vertical = Space.xl, horizontal = Space.l),
                    horizontalArrangement = Arrangement.spacedBy(Space.xl),
                ) {
                    Column(Modifier.width(400.dp)) { TokenColumn() }
                    Column(Modifier.width(400.dp)) { ComponentColumn() }
                    Column(Modifier.width(400.dp)) { CompositionColumn() }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/components.png")
    }
}

private val poses get() = BuiltInPoses.all
private val NoInsets = WindowInsets(0)

// ---------------------------------------------------------------------------------------------------- tokens

@Composable
private fun ColumnScope.TokenColumn() {
    TopBarCard("Design system v2", windowInsets = NoInsets, containerColor = Color.Transparent, badge = { Badge("Tokens") })
    Gallery("Colour roles") {
        val body = PoseitColors.backgroundBody
        val card = PoseitColors.backgroundCard
        Swatches(
            "Backgrounds",
            listOf(
                Sw("backgroundBody", body),
                Sw("backgroundSurface", PoseitColors.backgroundSurface),
                Sw("backgroundMuted", PoseitColors.backgroundMuted),
                Sw("backgroundCard", card),
                Sw("backgroundCardRaised", PoseitColors.backgroundCardRaised),
                Sw("backgroundCamera", PoseitColors.backgroundCamera),
            ),
        )
        Swatches(
            "Content on cream (contrast on the canvas)",
            listOf(
                Sw("textPrimary", PoseitColors.textPrimary, body),
                Sw("textSecondary", PoseitColors.textSecondary, body),
                Sw("textNumeralPale", PoseitColors.textNumeralPale, body),
                Sw("borderEmphasized", PoseitColors.borderEmphasized, body),
                Sw("border", PoseitColors.border, body),
                Sw("track", PoseitColors.track, body),
            ),
        )
        Swatches(
            "Content on a dark card (contrast on the card)",
            listOf(
                Sw("textPrimaryInverted", PoseitColors.textPrimaryInverted, card),
                Sw("textSecondaryInverted", PoseitColors.textSecondaryInverted, card),
                Sw("borderEmphasizedInverted", PoseitColors.borderEmphasizedInverted, card),
                Sw("trackInverted", PoseitColors.trackInverted, card),
            ),
            ground = card,
        )
        Swatches(
            "Accent (contrast of its content)",
            listOf(
                Sw("accent / onAccent", PoseitColors.accent, PoseitColors.onAccent, contrastOf = PoseitColors.onAccent),
                Sw("accentMuted", PoseitColors.accentMuted, PoseitColors.textPrimary, contrastOf = PoseitColors.textPrimary),
                Sw("accent on card", PoseitColors.accent, card),
            ),
        )
        Swatches(
            "Signals on cream (contrast on the canvas)",
            listOf(
                Sw("signalMiss", PoseitColors.signalMiss, body),
                Sw("signalNear", PoseitColors.signalNear, body),
                Sw("signalMatch", PoseitColors.signalMatch, body),
            ),
        )
        Swatches(
            "Signals on the camera (contrast on black)",
            listOf(
                Sw("signalMissBright", PoseitColors.signalMissBright, PoseitColors.backgroundCamera),
                Sw("signalNearBright", PoseitColors.signalNearBright, PoseitColors.backgroundCamera),
                Sw("signalMatchBright", PoseitColors.signalMatchBright, PoseitColors.backgroundCamera),
            ),
            ground = PoseitColors.backgroundCamera,
        )
        Swatches(
            "Figure tones (contrast on the stage)",
            PoseitColors.figureTones.mapIndexed { i, c -> Sw(if (i == 0) "figureFill" else "tone ${i + 1}", c, card) },
            ground = card,
        )
    }
    Gallery("Type: Hanken Grotesk") {
        TypeSample("display  Light 34/38", PoseitType.display, "Pick a pose to copy.")
        TypeSample("headline  Regular 26/32", PoseitType.headline, "Jacket over shoulder")
        TypeSample("title  Regular 20/26", PoseitType.title, "Quick settings")
        TypeSample("wordmark  Regular 22/28", PoseitType.wordmark, "Poseit")
        TypeSample("heading  Medium 16/22", PoseitType.heading, "Camera setup")
        TypeSample("bodyLarge  Regular 17/24", PoseitType.bodyLarge, "Today's pose")
        TypeSample("body  Regular 15/22", PoseitType.body, "Turn your shoulders slightly away from the camera and drop your chin.")
        TypeSample("bodyStrong  Medium 15/22", PoseitType.bodyStrong, "Voice coaching")
        TypeSample("supporting  Regular 13/18", PoseitType.supporting, "Standing, easy")
        TypeSample("label  Medium 14/20", PoseitType.label, "Show all")
        Column {
            Text("caps  SemiBold 12/16 +0.8", style = PoseitType.supporting.copy(fontSize = 11.sp), color = PoseitColors.textSecondary)
            CapsLabel("Try this pose", color = PoseitColors.textPrimary)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            Text("0:03", style = PoseitType.numeralDisplay, color = PoseitColors.textNumeralPale)
            Text("87%", style = PoseitType.numeralLarge, color = PoseitColors.textPrimary)
            Text("12", style = PoseitType.numeralMedium, color = PoseitColors.textPrimary, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------------- components

@Composable
private fun ColumnScope.ComponentColumn() {
    Gallery("Buttons on cream") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            PrimaryButton("Restart", {}, Modifier.weight(1f))
            SecondaryButton("Pause", {}, Modifier.weight(1f))
        }
        AccentButton("Try this pose", {}, Modifier.fillMaxWidth(), icon = PoseitIcons.camera)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Start", {}, form = ButtonForm.Block)
            TertiaryButton("Cancel", {})
            DestructiveButton("Delete pose", {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            DestructiveButton("Delete pose", {}, filled = true)
            PrimaryButton("Save pose", {}, enabled = false)
            PrimaryButton({}) { PoseitSpinner(size = 18.dp) }
        }
    }
    Spacer(Modifier.height(Space.m))
    PoseitCard(Modifier.fillMaxWidth().padding(horizontal = Space.edge)) {
        Text("Buttons in a dark card", style = PoseitType.bodyLarge)
        Spacer(Modifier.height(Space.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            AccentButton("Start game", {}, Modifier.weight(1f), form = ButtonForm.Block)
            SecondaryButton("View leaders", {}, Modifier.weight(1f), form = ButtonForm.Block)
        }
        Spacer(Modifier.height(Space.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Details", {})
            TertiaryButton("Show all", {})
            DestructiveButton("Delete", {})
        }
    }
    Gallery("Icon buttons and toggles") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(PoseitIcons.camera, "Pose camera", {})
            CircleIconButton(PoseitIcons.add, "Create a pose", {}, variant = CircleVariant.Outlined)
            CircleIconButton(PoseitIcons.add, "Create a pose", {}, variant = CircleVariant.Accent)
            PlainIconButton(PoseitIcons.settings, "Settings", {})
            var mirror by remember { mutableStateOf(true) }
            IconToggle(PoseitIcons.edit, "Mirror", mirror, { mirror = it })
            IconToggle(PoseitIcons.edit, "Mirror", false, {})
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(PoseitColors.backgroundCamera, PoseitShapes.Control)
                .padding(Space.s),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(PoseitIcons.close, "Close", {}, variant = CircleVariant.Camera)
            var grid by remember { mutableStateOf(true) }
            CircleIconToggle(PoseitIcons.settings, "Grid", grid, { grid = it })
            CircleIconToggle(PoseitIcons.photos, "Flash", false, {})
            Spacer(Modifier.weight(1f))
            Text("Camera controls", style = PoseitType.supporting, color = PoseitColors.cameraContent)
        }
    }
    Gallery("Segmented pill, tabs, badges, filters") {
        var seg by remember { mutableIntStateOf(0) }
        SegmentedPill(listOf("Off", "3 s", "10 s"), seg, { seg = it })
        var tab by remember { mutableIntStateOf(1) }
        PillTabs(listOf("All", "Favourites", "Recent", "My poses"), tab, { tab = it })
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            Badge("99 poses")
            Badge("3h 41m left", tone = BadgeTone.Dark)
            ProvideSurfaceTone(SurfaceTone.Dark) {
                Box(Modifier.background(PoseitColors.backgroundCard, PoseitShapes.Inner).padding(6.dp)) { Badge("New", tone = BadgeTone.Light) }
            }
        }
        var people by remember { mutableIntStateOf(0) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf("Solo", "Couple", "Group").forEachIndexed { i, label ->
                FilterPill(label, selected = people == i, onClick = { people = if (people == i) -1 else i }, leadingIcon = PoseitIcons.person)
            }
            FilterPill("Beach", selected = false, onClick = {})
            FilterPill("Elegant", selected = false, onClick = {}, enabled = false)
        }
    }
    Gallery("Rows") {
        OutlinedRow("Your photos", onClick = {})
        OutlinedRow("Make a pose from a photo", onClick = {}, subtitle = "Pick a photo of someone posing")
        var voice by remember { mutableStateOf(true) }
        var level by remember { mutableStateOf(false) }
        var opacity by remember { mutableFloatStateOf(0.6f) }
        var shot by remember { mutableIntStateOf(0) }
        RowGroup {
            SwitchRow("Voice guidance", voice, { voice = it }, subtitle = "Spoken hints and countdown")
            SwitchRow("Level indicator", level, { level = it })
            SliderRow("Guide opacity", "${(opacity * 100).toInt()}%", opacity, { opacity = it })
            SettingRow("Back up your poses", subtitle = "Save them to a file", onClick = {}) { RowChevron() }
            RadioRow("Full body", selected = shot == 0, onClick = { shot = 0 })
        }
        var dark by remember { mutableFloatStateOf(0.35f) }
        Box(Modifier.fillMaxWidth().background(PoseitColors.backgroundCard, PoseitShapes.Control).padding(horizontal = Space.m)) {
            ProvideSurfaceTone(SurfaceTone.Dark) { ValueSlider(dark, { dark = it }, steps = 4) }
        }
    }
    Gallery("Feedback") {
        Banner("That photo has no one in it. Try one with a whole person in view.", status = BannerStatus.Error)
        DialogCard(
            title = "Delete pose?",
            text = "Hand on hip goes from your poses. Photos you took with it stay.",
            confirmButton = { DestructiveButton("Delete pose", {}, filled = true) },
            dismissButton = { TertiaryButton("Cancel", {}) },
        )
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { snackbar.showSnackbar("Pose saved", actionLabel = "Undo", duration = SnackbarDuration.Indefinite) }
        Box(Modifier.fillMaxWidth().height(72.dp)) { PoseitSnackbarHost(snackbar, Modifier.align(Alignment.Center)) }
        Column(
            Modifier
                .fillMaxWidth()
                .background(PoseitColors.backgroundSurface, PoseitShapes.SheetTop)
                .border(1.dp, PoseitColors.border, PoseitShapes.SheetTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetHandle()
            Text(
                "Quick settings",
                style = PoseitType.title,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = Space.s),
            )
            var countdown by remember { mutableIntStateOf(1) }
            SegmentedPill(listOf("Off", "3 s", "10 s"), countdown, { countdown = it }, Modifier.fillMaxWidth().padding(horizontal = Space.xl))
            Spacer(Modifier.height(Space.l))
        }
    }
}

// ---------------------------------------------------------------------------------------------------- compositions

@Composable
private fun ColumnScope.CompositionColumn() {
    Text("Mock home, shared components only", style = PoseitType.supporting, color = PoseitColors.textSecondary, modifier = Modifier.padding(start = Space.textInset, bottom = Space.s))
    MockHome()
    Gallery("Connected cards") {
        ConnectedCardStack(Modifier.fillMaxWidth()) {
            CardSegment {
                Text("Best match", style = PoseitType.bodyLarge)
                Text("Hold still while the guide turns green.", style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                Spacer(Modifier.height(Space.l))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("87%", style = PoseitType.numeralLarge)
                    Spacer(Modifier.width(Space.l))
                    SegmentedProgress(0.87f, Modifier.weight(1f), segments = 18, contentDescription = "Match")
                }
            }
            CardSegment {
                StatRow {
                    CompactStat("Poses", "99")
                    CompactStat("Favourites", "12")
                    CompactStat("Photos", "248")
                }
            }
        }
        ConnectedCardRow(Modifier.fillMaxWidth()) {
            CardSegment { Stat("Countdown", "3 s", caption = "Before the photo") }
            CardSegment { Stat("Guide", "60%", caption = "Opacity") }
        }
    }
    Gallery("Pose tiles, frame marks, empty state") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.edge)) {
            val couple = poses.first { it.personCount == 2 }
            PoseTile(poses[0], Modifier.weight(1f), isFavorite = true, onToggleFavorite = {}, onClick = {}, nameMinLines = 2)
            PoseTile(couple, Modifier.weight(1f), detail = "Couple", onToggleFavorite = {}, onClick = {}, nameMinLines = 2)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(PoseitColors.backgroundCamera, PoseitShapes.Control)
                .padding(Space.m),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            poses.drop(4).take(5).forEachIndexed { i, p ->
                PoseTile(p, Modifier.width(60.dp), size = PoseTileSize.Compact, selected = i == 1, onDark = true, onClick = {})
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .width(132.dp)
                    .background(PoseitColors.backgroundStage, PoseitShapes.Tile)
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
            Box(Modifier.weight(1f).border(1.dp, PoseitColors.border, PoseitShapes.Row)) {
                EmptyState(
                    "No favourites yet",
                    "Tap the heart on a pose to keep it here.",
                    horizontalPadding = Space.l,
                    action = { SecondaryButton("Browse poses", {}) },
                )
            }
        }
    }
}

/** The home screen as the screen map describes it, built only from shared components. */
@Composable
private fun MockHome() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(860.dp)
            .clip(PoseitShapes.Sheet)
            .background(PoseitColors.backgroundBody)
            .border(1.dp, PoseitColors.border, PoseitShapes.Sheet),
    ) {
        Column(Modifier.fillMaxSize()) {
            val today = poses[7]
            TopBarCard(
                "Poseit",
                wordmark = true,
                windowInsets = NoInsets,
                badge = { Badge("99 poses") },
                actions = { PlainIconButton(PoseitIcons.settings, "Settings", {}) },
                joined = {
                    CardSegment(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Row(Modifier.height(212.dp)) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = Space.cardPadding, top = Space.cardPadding - 4.dp, bottom = Space.cardPadding),
                            ) {
                                Text("Today's pose", style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                                Spacer(Modifier.height(Space.xs))
                                Text(today.name, style = PoseitType.headline, maxLines = 2)
                                Text(today.category.label, style = PoseitType.supporting, color = PoseitColors.textSecondaryInverted)
                                Spacer(Modifier.weight(1f))
                                AccentButton("Try this pose", {})
                            }
                            PoseThumbnail(today, Modifier.width(144.dp).fillMaxHeight().padding(top = Space.l, end = Space.l, bottom = Space.s), background = null)
                        }
                    }
                },
            )
            Spacer(Modifier.height(Space.l))
            var tab by remember { mutableIntStateOf(0) }
            PillTabs(listOf("All", "Favourites", "Recent", "My poses"), tab, { tab = it }, edgePadding = Space.edge)
            Spacer(Modifier.height(Space.s))
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space.edge),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                FilterPill("Solo", selected = true, onClick = {}, leadingIcon = PoseitIcons.person)
                FilterPill("Couple", selected = false, onClick = {}, leadingIcon = PoseitIcons.person)
                FilterPill("Standing", selected = false, onClick = {})
                FilterPill("Sitting", selected = false, onClick = {})
            }
            Text(
                "99 poses",
                style = PoseitType.supporting,
                color = PoseitColors.textSecondary,
                modifier = Modifier.padding(start = Space.textInset, top = Space.m, bottom = Space.s),
            )
            Row(Modifier.padding(horizontal = Space.edge), horizontalArrangement = Arrangement.spacedBy(Space.edge)) {
                PoseTile(poses[0], Modifier.weight(1f), isFavorite = true, onToggleFavorite = {}, onClick = {})
                PoseTile(poses[1], Modifier.weight(1f), onToggleFavorite = {}, onClick = {})
            }
        }
        FloatingPillBar(Modifier.align(Alignment.BottomCenter), windowInsets = NoInsets) {
            CircleIconButton(PoseitIcons.camera, "Pose camera", {}, size = PillBarItemSize)
            var view by remember { mutableIntStateOf(0) }
            PillBarSegments(listOf("Poses", "Photos"), view, { view = it })
            CircleIconButton(PoseitIcons.add, "Create a pose", {}, size = PillBarItemSize)
        }
    }
}

// ---------------------------------------------------------------------------------------------------- helpers

/** A gallery section: heading on the text inset, then its content inset by the card edge. */
@Composable
private fun Gallery(title: String, gap: Dp = Space.m, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(Space.section))
    SectionHeading(title, Modifier.padding(horizontal = Space.textInset))
    Spacer(Modifier.height(Space.s))
    Column(Modifier.padding(horizontal = Space.edge), verticalArrangement = Arrangement.spacedBy(gap), content = content)
}

/** A swatch: a role [name] and colour, and optionally the [on] surface its contrast is measured against. */
private data class Sw(val name: String, val color: Color, val on: Color? = null, val contrastOf: Color? = null)

@Composable
private fun Swatches(title: String, items: List<Sw>, ground: Color = PoseitColors.backgroundBody) {
    val onGround = if (ground.luminance() < 0.5f) PoseitColors.textPrimaryInverted else PoseitColors.textPrimary
    val quiet = if (ground.luminance() < 0.5f) PoseitColors.textSecondaryInverted else PoseitColors.textSecondary
    Column(
        Modifier
            .fillMaxWidth()
            .background(ground, PoseitShapes.Row)
            .then(if (ground == PoseitColors.backgroundBody) Modifier.border(1.dp, PoseitColors.border, PoseitShapes.Row) else Modifier)
            .padding(Space.m),
    ) {
        Text(title, style = PoseitType.supporting, color = quiet)
        Spacer(Modifier.height(Space.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s), maxItemsInEachRow = 3) {
            items.forEach { sw ->
                Column(Modifier.width(110.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(sw.color, PoseitShapes.Inner)
                            .border(1.dp, onGround.copy(alpha = 0.14f), PoseitShapes.Inner),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sw.contrastOf != null) Text("Aa", style = PoseitType.bodyStrong, color = sw.contrastOf)
                    }
                    Text(sw.name, style = PoseitType.supporting.copy(fontSize = 11.sp, lineHeight = 14.sp), color = onGround, modifier = Modifier.padding(top = 3.dp))
                    val ratio = when {
                        sw.contrastOf != null -> contrast(sw.contrastOf, sw.color)
                        sw.on != null -> contrast(sw.color, sw.on)
                        else -> null
                    }
                    Text(
                        hex(sw.color) + (ratio?.let { "  " + String.format(Locale.ROOT, "%.1f:1", it) } ?: ""),
                        style = PoseitType.supporting.copy(fontSize = 11.sp, lineHeight = 14.sp),
                        color = quiet,
                    )
                }
            }
        }
    }
}

/** WCAG contrast of [fg] (composited over [bg] if translucent) on [bg]. */
private fun contrast(fg: Color, bg: Color): Float {
    val solid = if (fg.alpha < 1f) fg.compositeOver(bg) else fg
    val a = solid.luminance()
    val b = bg.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

private fun hex(c: Color): String {
    val argb = (c.red * 255).toInt() shl 16 or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
    val base = "#%06X".format(argb and 0xFFFFFF)
    return if (c.alpha < 1f) "$base ${(c.alpha * 100).toInt()}%" else base
}

@Composable
private fun TypeSample(role: String, style: TextStyle, sample: String) {
    Column {
        Text(role, style = PoseitType.supporting.copy(fontSize = 11.sp), color = PoseitColors.textSecondary)
        Text(sample, style = style, color = PoseitColors.textPrimary)
    }
}
