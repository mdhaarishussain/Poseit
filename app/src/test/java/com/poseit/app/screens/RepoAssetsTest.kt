package com.poseit.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.R
import com.poseit.app.appContainer
import com.poseit.app.camera.FlashMode
import com.poseit.app.camera.SceneAspect
import com.poseit.app.data.AppSettings
import com.poseit.app.data.GridType
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.library.BuiltInPoses
import com.poseit.app.pose.match.HintKind
import com.poseit.app.pose.match.MatchResult
import com.poseit.app.pose.match.PoseMatcher
import com.poseit.app.pose.scene.OverlayTransform
import com.poseit.app.pose.scene.SceneMapping
import com.poseit.app.ui.camera.CameraDock
import com.poseit.app.ui.camera.CameraTopBar
import com.poseit.app.ui.camera.CameraUiState
import com.poseit.app.ui.camera.HintBanner
import com.poseit.app.ui.camera.PoseStrip
import com.poseit.app.ui.camera.ScoreBadgeSize
import com.poseit.app.ui.camera.ScoreRing
import com.poseit.app.ui.camera.ShutterRow
import com.poseit.app.ui.camera.TimerOption
import com.poseit.app.ui.camera.ToolRail
import com.poseit.app.ui.camera.buildStripPoses
import com.poseit.app.ui.camera.toFigureStyle
import com.poseit.app.ui.components.Badge
import com.poseit.app.ui.components.ConnectedAxis
import com.poseit.app.ui.components.ConnectedCardDefaults
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.FillingCardStack
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.connectedCardsPath
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.create.CreateFromPhotoScreen
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.editor.PoseEditorScreen
import com.poseit.app.ui.library.LibraryScreen
import com.poseit.app.ui.library.LibraryViewModel
import com.poseit.app.ui.settings.SettingsScreen
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitTheme
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders the repository's images (README banner, social preview, phone screenshots, icon) from the app's own
 * composables into app/build/repo-assets/. scripts/repo_assets.py then scales, rounds and compresses them into
 * docs/images/. Run: ./gradlew :app:testDebugUnitTest --tests 'com.poseit.app.screens.RepoAssetsTest'
 *
 * Pixel sizes come from the qualifiers: w800dp-h280dp at xhdpi is exactly 1600x560.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xxhdpi")
class RepoAssetsTest {

    @get:Rule
    val compose = createComposeRule()

    /** Sample photos decode inside the frame, so captures show them (not their placeholders). */
    @get:Rule
    val photos = InlinePosePhotos()

    /**
     * Renders [content] into build/repo-assets/[name].png. With [ready], waits (up to 15 s) until that text is on
     * screen first, for screens whose view model loads the pose in the background.
     */
    private fun render(name: String, ready: String? = null, content: @Composable () -> Unit) {
        compose.setContent { PoseitTheme { content() } }
        compose.waitForIdle()
        if (ready != null) {
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText(ready, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
        }
        compose.onRoot().captureRoboImage("build/repo-assets/$name.png")
    }

    private fun screen(name: String, ready: String, content: @Composable () -> Unit) = render("screen-$name", ready) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
    }

    // ------------------------------------------------------------------ banner and social preview

    @Test
    @Config(qualifiers = "w800dp-h280dp-xhdpi")
    fun bannerLight() = render("banner-light") { Banner(dark = false) }

    @Test
    @Config(qualifiers = "w800dp-h280dp-xhdpi")
    fun bannerDark() = render("banner-dark") { Banner(dark = true) }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun socialPreview() = render("social-preview") { Social() }

    // ------------------------------------------------------------------ icon

    @Test
    @Config(qualifiers = "w128dp-h128dp-xhdpi")
    fun icon() = render("icon") {
        // The adaptive icon flat: the dark card background layer (ic_launcher_background), then the 108dp foreground
        // layer, of which the launcher shows the central 72dp. scripts/repo_assets.py cuts it to the disc the app
        // ships for launchers without adaptive icons (res/mipmap/ic_launcher.xml).
        Box(Modifier.fillMaxSize().background(PoseitColors.backgroundCard), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.requiredSize(128.dp * 108f / 72f),
            )
        }
    }

    // ------------------------------------------------------------------ screenshots

    /**
     * The library on a pinned day, so Today's pose (picked from the date) is the same in every render: the screen's
     * view model is created ahead with a fixed clock, and LibraryScreen's own `viewModel()` call then finds it.
     */
    @Test
    fun screenLibrary() = screen("1-library", ready = "99 poses") {
        val context = LocalContext.current.applicationContext
        val owner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }.also { owner ->
                val factory = viewModelFactory { initializer { LibraryViewModel(context.appContainer.poseRepository, clock = { LIBRARY_DAY_MILLIS }) } }
                ViewModelProvider(owner.viewModelStore, factory)[LibraryViewModel::class.java]
            }
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { LibraryScreen({}, {}, {}, {}, {}, {}) }
    }

    @Test
    fun screenDetail() = screen("2-detail", ready = "How to do it") {
        PoseDetailScreen(poseId = DETAIL_POSE, onBack = {}, onTryPose = { _, _ -> }, onEdit = {})
    }

    @Test
    fun screenCamera() = screen("3-camera", ready = "Perfect") { CameraStill(BuiltInPoses.byId(CAMERA_POSE)!!) }

    @Test
    fun screenEditor() = screen("4-editor", ready = "Based on") {
        PoseEditorScreen(basePoseId = EDITOR_POSE, onBack = {}, onSaved = { _, _ -> })
    }

    @Test
    fun screenCreate() = screen("5-create", ready = "Choose a photo") { CreateFromPhotoScreen(onBack = {}, onSaved = {}) }

    @Test
    fun screenSettings() = screen("6-settings", ready = "Match strictness") { SettingsScreen(onBack = {}, onShowIntro = {}, onOpenPhotoCredits = {}) }

    /** The matcher's verdict for the camera still is a real one: the PERFECT hint at a score in the 90s. */
    @Test
    fun cameraStillIsAGenuineMatch() {
        val pose = BuiltInPoses.byId(CAMERA_POSE)!!
        val match = cameraScene(pose).match
        println("camera still score ${match.score} parts ${match.figures.map { it.partScores }}")
        assertEquals(HintKind.PERFECT, match.hints.first().kind)
        assertTrue(match.score in 0.88f..0.96f)
    }

    private companion object {
        /**
         * A pose with a sample photo, so the README shows the photo card and its credit line: a Mixkit still of a real
         * street, whose "Photo: Mixkit" is the whole credit.
         */
        const val DETAIL_POSE = "walk_across"
        const val CAMERA_POSE = "fun_superhero"
        const val EDITOR_POSE = "fun_disco_point"

        /** Noon UTC on 2026-11-22, the 1.2.0 release day: Today's pose is Peace sign, a pose with a sample photo. */
        const val LIBRARY_DAY_MILLIS = 1_795_348_800_000L
    }
}

// ====================================================================== the banner and the social preview

/*
 * The README banner and the social preview in the v2 identity (docs/DESIGN.md), built from the app's own pieces: on
 * the cream canvas, a connected stack (the top bar card with the wordmark and the lavender count badge, fused to a
 * card with the tagline) beside a connected row of dark cards, one white lay figure standing in each. Figures only:
 * no sample photos (they would need their credits, and could read as the people endorsing the app).
 */

/** Poses on the banner's figure cards, left to right. */
private val BannerCast = listOf("stand_hand_on_hip", "couple_walk_hands", "fun_disco_point")

/** Poses on the social preview's figure cards. */
private val SocialCast = listOf("walk_across", "fun_disco_point")

/** The canvas of the dark banner (GitHub's dark theme): the camera's black, where the dark cards sit in the app too. */
private fun field(dark: Boolean) = if (dark) PoseitColors.backgroundCamera else PoseitColors.backgroundBody

@Composable
private fun Banner(dark: Boolean) = Identity(
    dark = dark,
    cast = BannerCast,
    margin = 16.dp,
    textWidth = 352.dp,
    wordmarkSize = 60.sp,
    badgeScale = 1.5f,
    padding = 28.dp,
) {
    Text(TAGLINE, style = Tagline.copy(fontSize = 24.sp, lineHeight = 31.sp), color = PoseitColors.textPrimaryInverted)
}

@Composable
private fun Social() = Identity(
    dark = false,
    cast = SocialCast,
    margin = 16.dp,
    textWidth = 330.dp,
    wordmarkSize = 50.sp,
    badgeScale = 1.25f,
    padding = 26.dp,
) {
    Text(TAGLINE, style = Tagline.copy(fontSize = 22.sp, lineHeight = 28.sp), color = PoseitColors.textPrimaryInverted)
    // The facts at the card's foot, where Today's pose has its button.
    Spacer(Modifier.weight(1f))
    Text(SOCIAL_FACTS, style = PoseitType.body.copy(fontSize = 17.sp, lineHeight = 24.sp), color = PoseitColors.textSecondaryInverted)
}

/**
 * The identity layout: on the [dark] or cream field, [margin] all round, the wordmark stack on the left, at least
 * [textWidth] wide, and the figure row on the right in the rest (its cards as wide as their figures need), [Space.edge]
 * apart.
 */
@Composable
private fun Identity(
    dark: Boolean,
    cast: List<String>,
    margin: Dp,
    textWidth: Dp,
    wordmarkSize: TextUnit,
    badgeScale: Float,
    padding: Dp,
    text: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(field(dark)).padding(margin)) {
        val cards = remember(cast, maxWidth, maxHeight) { figureCards(cast, maxHeight, maxWidth - textWidth - Space.edge) }
        val rowWidth = cards.sumOf { it.width.value.toDouble() }.dp + ConnectedCardDefaults.Gap * (cards.size - 1)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.edge)) {
            FillingCardStack(
                cards = listOf(
                    { WordmarkCard(wordmarkSize, badgeScale, padding) },
                    {
                        Column(Modifier.fillMaxSize().padding(horizontal = padding, vertical = padding)) { text() }
                    },
                ),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                fillIndex = 1,
            )
            FigureRow(cards, Modifier.width(rowWidth).fillMaxHeight())
        }
    }
}

/** The top bar card, banner size: the wordmark, and the count badge at the end as on the home screen. */
@Composable
private fun WordmarkCard(size: TextUnit, badgeScale: Float, padding: Dp) {
    Row(
        Modifier.fillMaxWidth().padding(start = padding, end = padding, top = padding * 0.75f, bottom = padding * 0.75f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Poseit",
            style = PoseitType.wordmark.copy(fontSize = size, lineHeight = size * 1.15f, letterSpacing = size * (-0.6f / 22f)),
            color = PoseitColors.textPrimaryInverted,
            modifier = Modifier.weight(1f),
        )
        // The app's Badge at a larger scale: every dimension and its type grow together.
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density * badgeScale, density.fontScale)) {
            Badge("${BuiltInPoses.all.size} poses")
        }
    }
}

/** One card of the figure row: the pose, the card's width, and where its frame sits inside the card. */
private class FigureCard(val pose: PoseTemplate, val width: Dp, val frameLeft: Dp, val frameTop: Dp, val frameHeight: Dp)

/**
 * Sizes the figure cards for a row [height] tall and at most [maxWidth] wide: one frame height for every pose, as
 * large as lets every figure keep some room above its head (or raised hand) and below its feet and the row fit
 * [maxWidth]. The figures stand on one floor line, placed so that the tallest of them is centred in the cards. Each
 * card is its figure's width plus a side padding.
 */
private fun figureCards(ids: List<String>, height: Dp, maxWidth: Dp): List<FigureCard> {
    val poses = ids.map { BuiltInPoses.byId(it) ?: error("no pose $it") }
    val room = height * 0.1f
    // Limbs and the head are drawn wider than their joints; the head stands above its topmost joint.
    val headRoom = 0.075f
    val ankle = { p: PoseTemplate -> p.figures.maxOf { f -> maxOf(f[Joint.LEFT_ANKLE]?.y ?: 0f, f[Joint.RIGHT_ANKLE]?.y ?: 0f) } }
    val top = { p: PoseTemplate -> p.figures.flatMap { it.points.values }.minOf { it.y } - headRoom }
    val pad = height * 0.07f
    // Each figure's horizontal extent in its frame (0..1 of the frame width).
    val extents = poses.map { p ->
        val pts = p.figures.flatMap { it.points.values }
        // The floor spot under each figure is wider than a slim figure (PoseThumbnail: 0.17 of the frame width).
        val spots = p.figures.mapNotNull { f ->
            val l = f[Joint.LEFT_ANKLE] ?: return@mapNotNull null
            val r = f[Joint.RIGHT_ANKLE] ?: return@mapNotNull null
            (l.x + r.x) / 2f
        }
        val limb = 0.035f / p.frameAspect
        minOf(pts.minOf { it.x } - limb, spots.minOf { it - 0.17f }) to maxOf(pts.maxOf { it.x } + limb, spots.maxOf { it + 0.17f })
    }
    // From the top of the head to the floor spot's foot, in frame heights.
    val tallest = poses.maxOf { p -> ankle(p) + 0.02f - top(p) }
    val byHeight = (height - room * 2) / tallest
    val fixed = pad * 2 * poses.size + ConnectedCardDefaults.Gap * (poses.size - 1)
    val units = poses.indices.sumOf { i -> (poses[i].frameAspect * (extents[i].second - extents[i].first)).toDouble() }.toFloat()
    val frame = minOf(byHeight, (maxWidth - fixed) / units)
    val floorY = (height + frame * tallest) / 2 - frame * 0.006f
    return poses.mapIndexed { i, p ->
        val fw = frame * p.frameAspect
        val (minX, maxX) = extents[i]
        FigureCard(
            pose = p,
            width = fw * (maxX - minX) + pad * 2,
            frameLeft = pad - fw * minX,
            frameTop = floorY - frame * (ankle(p) + 0.014f),
            frameHeight = frame,
        )
    }
}

/** The figure cards side by side, fused by the connected-card necks (`connectedCardsPath`, as ConnectedCardRow). */
@Composable
private fun FigureRow(cards: List<FigureCard>, modifier: Modifier) {
    Row(
        modifier.drawWithCache {
            val gap = ConnectedCardDefaults.Gap.toPx()
            var x = 0f
            val segments = cards.map { c -> Rect(x, 0f, x + c.width.toPx(), size.height).also { x = it.right + gap } }
            val path = connectedCardsPath(
                segments,
                ConnectedAxis.Horizontal,
                ConnectedCardDefaults.Radius.toPx(),
                ConnectedCardDefaults.JoinRadius.toPx(),
                ConnectedCardDefaults.NotchLength.toPx(),
            )
            onDrawBehind { drawPath(path, PoseitColors.backgroundCard) }
        },
        horizontalArrangement = Arrangement.spacedBy(ConnectedCardDefaults.Gap),
    ) {
        cards.forEach { c ->
            Box(Modifier.width(c.width).fillMaxHeight().clipToBounds()) {
                PoseThumbnail(
                    template = c.pose,
                    // The frame is wider than the card (its empty sides overhang): place it from the card's corner.
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .offset(x = c.frameLeft, y = c.frameTop)
                        .requiredSize(width = c.frameHeight * c.pose.frameAspect, height = c.frameHeight),
                    background = null,
                )
            }
        }
    }
}

/** Light text with balanced lines, so a two-line tagline doesn't leave one word alone. */
private val Tagline get() = PoseitType.display.copy(letterSpacing = (-0.4).sp, lineBreak = LineBreak.Heading)
private const val TAGLINE = "A pose guide for people who freeze in front of a camera."
private const val SOCIAL_FACTS = "Free for Android. Works offline."

// ====================================================================== the camera still

/** The guide figures and the detected people in scene space, and the matcher's verdict on them. */
private class CameraScene(val guide: List<Skeleton>, val people: List<Skeleton>, val match: MatchResult)

/**
 * The template figures in scene space and a "detected" person close to them (see [performer]). The limb colours,
 * the score and the hint all come from the real [PoseMatcher].
 */
private fun cameraScene(pose: PoseTemplate): CameraScene {
    val aspect = SceneAspect.RATIO_3_4.ratio
    val guide = SceneMapping.templateToScene(pose, aspect, GuideTransform, mirrored = false)
    val people = guide.map { performer(it, aspect) }
    return CameraScene(guide, people, PoseMatcher.match(guide, people, aspect))
}

/** The guide pinched a little smaller and dragged down (as the overlay gestures do), so the hint card clears its head. */
private val GuideTransform = OverlayTransform(offsetY = 0.1f, scale = 0.78f)

/**
 * Someone who has lined up with [sk], as the camera would see them: the body turned 1.5 degrees and a little off to
 * the side, the left arm 23 degrees out and the right arm 14. That scores in the low 90s, inside the default match
 * strictness, with every limb still in the match colour.
 */
private fun performer(sk: Skeleton, aspect: Float): Skeleton {
    val arm = 23f
    val shift = 0.025f
    val hip = sk.midHip() ?: return sk
    // Work in isotropic units so the turn doesn't shear the body.
    fun rotate(p: Vec2, about: Vec2, deg: Float): Vec2 {
        val r = Math.toRadians(deg.toDouble())
        val dx = (p.x - about.x) * aspect
        val dy = p.y - about.y
        val x = dx * cos(r) - dy * sin(r)
        val y = dx * sin(r) + dy * cos(r)
        return Vec2(about.x + x.toFloat() / aspect, about.y + y.toFloat())
    }
    val armJoints = setOf(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, Joint.LEFT_HAND)
    val otherArm = setOf(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, Joint.RIGHT_HAND)
    val ls = sk[Joint.LEFT_SHOULDER]
    val rs = sk[Joint.RIGHT_SHOULDER]
    return sk.copy(
        points = sk.points.mapValues { (j, p0) ->
            var p = rotate(p0, hip, 1.5f)
            if (j in armJoints && ls != null) p = rotate(p, rotate(ls, hip, 1.5f), -arm)
            if (j in otherArm && rs != null) p = rotate(p, rotate(rs, hip, 1.5f), arm * 0.6f)
            Vec2(p.x + shift, p.y + shift * 0.5f)
        },
        unit = 0f,
    )
}

/** Viewfinder white for the grid, with a faint black line under it (as CameraOverlay draws it). */
private fun DrawScope.thirdsGrid() {
    val line = PoseitColors.figureFill.copy(alpha = 0.42f)
    val shade = PoseitColors.backgroundCamera.copy(alpha = 0.18f)
    val w = 1.dp.toPx()
    listOf(size.width / 3f, size.width * 2f / 3f).forEach { x ->
        drawLine(shade, Offset(x + w, 0f), Offset(x + w, size.height), w)
        drawLine(line, Offset(x, 0f), Offset(x, size.height), w)
    }
    listOf(size.height / 3f, size.height * 2f / 3f).forEach { y ->
        drawLine(shade, Offset(0f, y + w), Offset(size.width, y + w), w)
        drawLine(line, Offset(0f, y), Offset(size.width, y), w)
    }
}

/**
 * The camera screen as it looks the moment a pose matches, built from the camera's own pieces (top bar, overlay
 * drawing, score ring, hint card, tool rail, pose strip, shutter row) with the layout of CameraContent. The preview
 * is the black surround: there is no photo behind the guide.
 */
@Composable
private fun CameraStill(pose: PoseTemplate) {
    val aspect = SceneAspect.RATIO_3_4
    val scene = remember(pose) { cameraScene(pose) }
    val match = scene.match
    val settings = AppSettings()
    val ui = CameraUiState(pose = pose, poseLoaded = true, aspect = aspect, detectorReady = true)
    val strip = remember(pose) { buildStripPoses(BuiltInPoses.all, emptySet(), pose.category, pose) }
    Column(Modifier.fillMaxSize().background(PoseitColors.backgroundCamera)) {
        CameraTopBar(
            flash = FlashMode.OFF, hasFlash = true, grid = GridType.THIRDS, aspect = aspect, timer = TimerOption.Off,
            onBack = {}, onFlash = {}, onGrid = {}, onAspect = {}, onTimer = {}, onPhotoshoot = {}, onQuickSettings = {},
        )
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val density = LocalDensity.current
            var controlsHeightPx by remember { mutableIntStateOf(0) }
            val controlsHeight = with(density) { controlsHeightPx.toDp() }
            val previewWidth = min(maxWidth, maxHeight * aspect.ratio)
            val previewHeight = previewWidth / aspect.ratio
            val free = maxHeight - controlsHeight
            val previewTop = if (previewHeight <= free) (free - previewHeight) / 2 else 0.dp
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = previewTop)
                    .width(previewWidth)
                    .aspectRatio(aspect.ratio)
                    .clipToBounds(),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    thirdsGrid()
                    val rect = Rect(Offset.Zero, size)
                    // As CameraOverlay draws them: the guide in the default style and opacity, each limb in the
                    // bright match colour of its score, then the detected body as thin white sticks at 70%.
                    scene.guide.forEachIndexed { i, figure ->
                        val fm = match.figures.getOrNull(i)
                        val colors = if (fm?.detectedIndex != null) fm.partScores.mapValues { (_, s) -> PoseitColors.forScore(s, onDark = true) } else emptyMap()
                        drawPoseFigure(
                            figure, aspect.ratio, rect,
                            FigurePaint(
                                style = settings.overlayStyle.toFigureStyle(),
                                fill = PoseitColors.figureTones[i % PoseitColors.figureTones.size],
                                partColors = colors,
                                alpha = settings.overlayOpacity,
                            ),
                        )
                    }
                    val stick = FigurePaint(style = FigureStyle.STICK, fill = PoseitColors.figureFill, alpha = 0.7f, strokeWidthPx = 2.5.dp.toPx(), showFaceMarker = false)
                    scene.people.forEach { drawPoseFigure(it, aspect.ratio, rect, stick) }
                }
                Box(Modifier.fillMaxSize().padding(Space.m)) {
                    ScoreRing(
                        percent = (match.score * 100).roundToInt(),
                        detectorReady = true,
                        detectorError = null,
                        // The coach holding the match before auto-capture.
                        holdProgress = { 0.55f },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                    HintBanner(
                        match.hints.first(),
                        Modifier.align(Alignment.TopCenter).padding(top = ScoreBadgeSize + Space.l),
                    )
                    ToolRail(ui, {}, {}, {}, {}, {}, Modifier.align(Alignment.CenterEnd))
                }
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { controlsHeightPx = it.height }
                    .padding(Space.edge),
            ) {
                // The dock: the pose strip fused to the shutter row, as CameraContent draws it.
                CameraDock(overPreview = false) {
                    PoseStrip(poses = strip, selectedId = pose.id, favorites = emptySet(), onSelect = {}, onOpenAll = {})
                    ShutterRow(
                        lastPhoto = null, sessionActive = false, timer = TimerOption.Off, capturing = false,
                        onGallery = {}, onShutter = {}, onFlip = {},
                    )
                }
            }
        }
    }
}
