package com.poseit.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.poseit.app.R
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
import com.poseit.app.ui.components.FigurePaint
import com.poseit.app.ui.components.FigureStyle
import com.poseit.app.ui.components.PoseThumbnail
import com.poseit.app.ui.components.drawPoseFigure
import com.poseit.app.ui.create.CreateFromPhotoScreen
import com.poseit.app.ui.detail.PoseDetailScreen
import com.poseit.app.ui.editor.PoseEditorScreen
import com.poseit.app.ui.library.LibraryScreen
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

    @Test
    fun screenLibrary() = screen("1-library", ready = "99 poses") { LibraryScreen({}, {}, {}, {}, {}, {}) }

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
        const val DETAIL_POSE = "couple_side_hug"
        const val CAMERA_POSE = "fun_superhero"
        const val EDITOR_POSE = "fun_disco_point"
    }
}

// ====================================================================== the studio set

/** Poses on the banner, left to right. */
private val BannerCast = listOf("stand_hand_on_hip", "walk_across", "couple_walk_hands", "fun_disco_point")

private class Placed(val pose: PoseTemplate, val left: Dp, val top: Dp)

/**
 * Lays [ids] out left to right from [start], each pose's frame [frameHeight] tall, so that every figure's feet (its
 * contact shadow) land on [floorY] and neighbouring figures keep [gap] between their bodies.
 */
private fun placeCast(ids: List<String>, frameHeight: Dp, floorY: Dp, start: Dp, gap: Dp): List<Placed> {
    var x = start
    return ids.map { id ->
        val pose = BuiltInPoses.byId(id) ?: error("no pose $id")
        val w = frameHeight * pose.frameAspect
        val pts = pose.figures.flatMap { it.points.values }
        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        val ankle = pose.figures.maxOf { f -> maxOf(f[Joint.LEFT_ANKLE]?.y ?: 0f, f[Joint.RIGHT_ANKLE]?.y ?: 0f) }
        // Limbs and the head are drawn wider than their joints: pad by a little over half a limb.
        val pad = frameHeight * 0.035f
        val left = x - w * minX + pad
        val top = floorY - frameHeight * (ankle + 0.014f)
        x += w * (maxX - minX) + pad * 2 + gap
        Placed(pose, left, top)
    }
}

private fun castWidth(placed: List<Placed>, frameHeight: Dp): Dp {
    val last = placed.last()
    val pts = last.pose.figures.flatMap { it.points.values }
    return last.left + frameHeight * last.pose.frameAspect * pts.maxOf { it.x } + frameHeight * 0.035f - placed.first().left
}

@Composable
private fun Cast(placed: List<Placed>, frameHeight: Dp, dx: Dp = 0.dp) {
    placed.forEach { p ->
        PoseThumbnail(
            template = p.pose,
            modifier = Modifier
                .offset(x = p.left + dx, y = p.top)
                .size(width = frameHeight * p.pose.frameAspect, height = frameHeight),
            background = null,
        )
    }
}

@Composable
private fun Banner(dark: Boolean) = SetScene(
    cast = BannerCast,
    dark = dark,
    frame = 196.dp,
    floorTop = 204.dp,
    floorY = 234.dp,
    margin = 56.dp,
    gap = 10.dp,
    textTop = 61.dp,
    textWidth = 340.dp,
) { ink, quiet ->
    Text("Poseit", style = Wordmark.copy(fontSize = 64.sp, lineHeight = 68.sp), color = ink)
    Spacer(Modifier.height(Space.m))
    Text(TAGLINE, style = Tagline.copy(fontSize = 19.sp, lineHeight = 27.sp), color = quiet)
}

@Composable
private fun Social() = SetScene(
    cast = listOf("couple_side_hug", "fun_disco_point"),
    dark = false,
    frame = 216.dp,
    floorTop = 236.dp,
    floorY = 262.dp,
    margin = 64.dp,
    gap = 16.dp,
    textTop = 66.dp,
    textWidth = 300.dp,
) { ink, quiet ->
    Text("Poseit", style = Wordmark.copy(fontSize = 52.sp, lineHeight = 56.sp), color = ink)
    Spacer(Modifier.height(Space.m))
    Text(TAGLINE, style = Tagline.copy(fontSize = 18.sp, lineHeight = 25.sp), color = ink)
    Spacer(Modifier.height(Space.s))
    Text(SOCIAL_FACTS, style = Tagline.copy(fontSize = 15.sp, lineHeight = 21.sp), color = quiet)
}

/**
 * The studio set: paper wall, the darker floor the figures stand on, the [cast] lined up on the right with their
 * feet on [floorY], and the type on the left from [textTop], which puts the wordmark's cap height level with the
 * tallest head.
 */
@Composable
private fun SetScene(
    cast: List<String>,
    dark: Boolean,
    frame: Dp,
    floorTop: Dp,
    floorY: Dp,
    margin: Dp,
    gap: Dp,
    textTop: Dp,
    textWidth: Dp,
    text: @Composable ColumnScope.(ink: Color, quiet: Color) -> Unit,
) {
    val wall = if (dark) PoseitColors.backgroundCamera else PoseitColors.backgroundBody
    val ink = if (dark) PoseitColors.figureFill else PoseitColors.textPrimary
    val quiet = if (dark) PoseitColors.figureFill.copy(alpha = 0.7f) else PoseitColors.textSecondary
    BoxWithConstraints(Modifier.fillMaxSize().background(wall).clipToBounds()) {
        Floor(dark, floorTop)
        val placed = placeCast(cast, frame, floorY = floorY, start = 0.dp, gap = gap)
        val width = castWidth(placed, frame)
        Cast(placed, frame, dx = maxWidth - margin - width - placed.first().left)
        Column(Modifier.padding(start = margin, top = textTop).width(textWidth)) { text(ink, quiet) }
    }
}

/** The floor of the set: the darker patch of paper the figures stand on (Set on Paper), or a faint line on black. */
@Composable
private fun Floor(dark: Boolean, top: Dp) {
    if (dark) {
        Box(Modifier.offset(y = top).fillMaxWidth().height(1.dp).background(PoseitColors.figureFill.copy(alpha = 0.16f)))
    } else {
        Box(Modifier.offset(y = top).fillMaxSize().background(PoseitColors.backgroundMuted))
    }
}

private val Wordmark get() = PoseitType.wordmark.copy(letterSpacing = (-0.5).sp)

/** Body text with balanced lines, so a two-line tagline doesn't leave one word alone. */
private val Tagline get() = PoseitType.body.copy(lineBreak = LineBreak.Heading)
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
