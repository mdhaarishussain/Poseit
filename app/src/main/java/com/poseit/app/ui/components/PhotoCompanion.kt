package com.poseit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.poseit.app.data.PosePhoto
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space

/*
 * Figure first, photo as companion (docs/DESIGN.md, "Sample photos"). The white figure stays the hero of every pose
 * view, because it is what the camera matches; a pose's sample photo stands beside it as a smaller companion that
 * shows the same pose on a real person, always with its credit. Poses without a photo simply don't get these:
 * their figure-only layout is complete on its own.
 *
 * - PhotoCompanionCard: "See it on a person", a dark card fused under the figure's card (the detail screen, the
 *   camera's pose tips).
 * - PhotoBadge: the photo as a small print with the credit glyph, which is also the photo's credit button (beside a
 *   pose tile's caption; on Today's pose when its credit is too long to sit beside the print).
 */

/**
 * The companion card's heading, by the number of people the pose (and so its photo) has: "See it on a person", "See
 * it on two people", "See it on a group". It says who is in the picture, never anything about them.
 */
fun photoCompanionTitle(people: Int): String = when {
    people <= 1 -> "See it on a person"
    people == 2 -> "See it on two people"
    else -> "See it on a group"
}

/** Every heading [photoCompanionTitle] gives, for checking that a pose without a photo shows none of them. */
val PhotoCompanionTitles: List<String> = listOf(1, 2, 3).map(::photoCompanionTitle)

/**
 * "See it on a person" ([photoCompanionTitle]): the pose's sample photo with its credit line, as one card of a
 * [ConnectedCardStack], fused directly under the card that shows the pose's figure. A portrait photo stands at the
 * right of the words, smaller than the figure above it; the landscape group photo runs under them, and so does a
 * portrait photo once the heading no longer fits on one line beside it (large font sizes, narrow phones). Never
 * cropped at runtime: the photo keeps its own aspect.
 *
 * The photo is never mirrored with the figure: it is a real photograph and its credit lists every change made to
 * it. While the figure is mirrored ([figureMirrored]) the words say that the photo shows the original side.
 *
 * TalkBack reads the heading, the line under it, the photo ("Photo of a person in the Hand on hip pose") and then
 * the credit button, in that order.
 *
 * The photo's size comes from the card's width and, through [maxPhotoHeight], from the figure above it: pass about
 * half the figure card's height, so on a short phone (where the figure's card shrinks) the photo shrinks with it and
 * the figure stays the larger picture.
 *
 * @param compact a smaller photo and a shorter line, for a card whose figure is drawn small (the camera's pose tips)
 * @param maxPhotoHeight the tallest the photo may be (portrait or landscape); unspecified, only the width decides
 */
@Composable
fun PhotoCompanionCard(
    photo: PosePhoto,
    modifier: Modifier = Modifier,
    figureMirrored: Boolean = false,
    compact: Boolean = false,
    maxPhotoHeight: Dp = Dp.Unspecified,
    contentPadding: PaddingValues = PaddingValues(
        start = Space.cardPadding,
        end = Space.cardPadding,
        top = Space.l + Space.xs,
        bottom = Space.cardPadding,
    ),
) {
    CardSegment(
        modifier = modifier.semantics { isTraversalGroup = true },
        contentPadding = contentPadding,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val measurer = rememberTextMeasurer()
            val title = photoCompanionTitle(photo.people)
            // The tallest the photo may be, as a width at its aspect: the figure above sets it (maxPhotoHeight).
            val heightCap = if (maxPhotoHeight.isSpecified) maxPhotoHeight * photo.aspect else Dp.Infinity
            val sideWidth = minOf(
                if (compact) CompactSidePhoto else (maxWidth * SidePhotoShare).coerceIn(SidePhotoMin, SidePhotoMax),
                heightCap,
            )
            val textWidth = maxWidth - sideWidth - Space.l
            val titleFits = remember(title, textWidth, density) {
                val titleWidth = measurer.measure(title, PoseitType.bodyLarge, softWrap = false, maxLines = 1).size.width
                titleWidth <= with(density) { textWidth.toPx() }
            }
            // Beside the words while the heading fits on one line next to it; under them for the landscape group
            // photo and at large font sizes. Under them it keeps the same modest size, smaller than the figure.
            val stacked = photo.isLandscape || density.fontScale >= 1.5f || !titleFits
            val stackedWidth = when {
                photo.isLandscape && compact -> CompactLandscapePhoto
                photo.isLandscape -> (maxWidth * 0.72f).coerceAtMost(LandscapePhotoMax)
                else -> sideWidth
            }.coerceAtMost(heightCap).coerceAtMost(maxWidth)
            if (stacked) {
                Column(Modifier.fillMaxWidth()) {
                    CompanionWords(title, companionLine(photo, figureMirrored, compact))
                    Spacer(Modifier.height(Space.l))
                    CompanionPhoto(photo, Modifier.width(stackedWidth))
                    Spacer(Modifier.height(Space.xs))
                    CompanionCredit(photo)
                }
            } else {
                val photoHeight = sideWidth / photo.aspect
                Row(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = photoHeight),
                    ) {
                        CompanionWords(title, companionLine(photo, figureMirrored, compact))
                        Spacer(Modifier.weight(1f).heightIn(min = Space.m))
                        // The credit closes the column at the foot of the photo, beside it like a side caption.
                        CompanionCredit(photo, Modifier.offset(y = Space.xs))
                    }
                    Spacer(Modifier.width(Space.l))
                    CompanionPhoto(photo, Modifier.width(sideWidth))
                }
            }
        }
    }
}

/** The heading and the one line under it. */
@Composable
private fun CompanionWords(title: String, line: String) {
    Text(
        title,
        style = PoseitType.bodyLarge,
        color = PoseitColors.textPrimaryInverted,
        modifier = Modifier.semantics {
            heading()
            traversalIndex = 0f
        },
    )
    Spacer(Modifier.height(Space.xs))
    Text(
        line,
        style = PoseitType.supporting,
        color = PoseitColors.textSecondaryInverted,
        modifier = Modifier.semantics { traversalIndex = 1f },
    )
}

/**
 * What the line under the heading says. It speaks about the pose and the figure, never about the people in the
 * photo (docs/POSE_PHOTOS.md: no captions with words or feelings). The figure is the subject: it is what the camera
 * matches. [compact] is the camera's own tips sheet, where the user is already in the camera.
 */
internal fun companionLine(photo: PosePhoto, figureMirrored: Boolean, compact: Boolean = false): String = when {
    figureMirrored -> "The figure is mirrored. The photo shows the original side."
    photo.people >= 3 -> "A real photo of the pose. It works with more people too."
    compact -> "A real photo of the pose. The guide you line up with is the figure."
    else -> "A real photo of the pose. In the camera, you line up with the figure, not the photo."
}

@Composable
private fun CompanionPhoto(photo: PosePhoto, modifier: Modifier) {
    PosePhoto(
        photo,
        modifier
            .aspectRatio(photo.aspect)
            .semantics { traversalIndex = 2f },
        shape = PoseitShapes.Inner,
    )
}

@Composable
private fun CompanionCredit(photo: PosePhoto, modifier: Modifier = Modifier) {
    PhotoCredit(photo, modifier.semantics { traversalIndex = 3f })
}

/** A portrait photo beside the words takes about this share of the card's inner width. */
private const val SidePhotoShare = 0.42f
private val SidePhotoMin = 112.dp
private val SidePhotoMax = 176.dp
private val LandscapePhotoMax = 288.dp

/** The compact card's portrait photo: a little smaller than the pose tips card's figure (about 99 x 132dp). */
private val CompactSidePhoto = 88.dp
private val CompactLandscapePhoto = 176.dp

/**
 * A pose's sample photo as a small print in a pose tile (the library grid), in the card's bottom-end corner beside
 * the caption: the one place a figure never reaches, so the print never covers the guide, and the tile keeps its
 * size. It is the whole photo at its own aspect (never a round crop, which would cut off heads or turn it into a
 * portrait of the model), on a thin mist edge, with the small info glyph of [PhotoCredit] on its corner.
 *
 * It is too small to carry its credit line, so the print is the credit button: one tap opens [PhotoCreditSheet],
 * which starts with the photo at a readable size beside the figure. TalkBack says what the photo shows and whose
 * it is ([photoBadgeLabel]: "Photo of a person in the Hand on hip pose. Photo credit: Mixkit", click label "Show the
 * full photo credit"). Its touch target is at least [PhotoBadgeSize] square, the print aligned to its bottom-end
 * corner; the caption leaves [photoBadgeReserve] for it ([PoseTile]'s `badgeReserve`).
 *
 * @param printSize the print's longer side; the shorter one follows the photo's aspect
 * @param onShowCredit opens the credit sheet; a screen that shows many badges (the library grid) hoists the sheet
 *   and passes this. Null: the badge opens its own sheet.
 */
@Composable
fun PhotoBadge(
    photo: PosePhoto,
    modifier: Modifier = Modifier,
    printSize: Dp = PhotoBadgePrint,
    onShowCredit: (() -> Unit)? = null,
) {
    var open by rememberSaveable(photo.poseId) { mutableStateOf(false) }
    val show = onShowCredit ?: { open = true }
    val width = if (photo.isLandscape) printSize else printSize * photo.aspect
    val height = if (photo.isLandscape) printSize / photo.aspect else printSize
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(maxOf(PhotoBadgeSize, width), maxOf(PhotoBadgeSize, height))
            .clickable(interactionSource = source, indication = null, onClickLabel = ShowCredit, role = Role.Button) { show() }
            .clearAndSetSemantics {
                contentDescription = photoBadgeLabel(photo)
                role = Role.Button
                onClick(label = ShowCredit) {
                    show()
                    true
                }
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box {
            PosePhoto(
                photo,
                Modifier
                    .size(width, height)
                    .border(1.dp, PoseitColors.borderEmphasizedInverted, PoseitShapes.Inner)
                    .clip(PoseitShapes.Inner)
                    .indication(source, ripple()),
                shape = PoseitShapes.Inner,
                contentDescription = null,
            )
            // The credit glyph on the print's corner: the same info glyph as every credit line, on a stage disc.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = BadgeGlyphOverhang, y = BadgeGlyphOverhang)
                    .size(BadgeGlyph)
                    .background(PoseitColors.backgroundStage, PoseitShapes.Circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PoseitIcons.info,
                    contentDescription = null,
                    tint = PoseitColors.textPrimaryInverted,
                    modifier = Modifier.size(BadgeGlyph - 2.dp),
                )
            }
        }
    }
    if (open && onShowCredit == null) PhotoCreditSheet(photo, onDismissRequest = { open = false })
}

/**
 * How a [PhotoBadge] is announced: what the photo shows, then whose it is ("Photo of a person in the Hand on hip
 * pose. Photo credit: Mixkit").
 */
fun photoBadgeLabel(photo: PosePhoto): String = "${photo.contentDescription}. ${photoCreditLabel(photo)}"

/** The longer side of a [PhotoBadge]'s print in a library tile (36 x 48dp for a portrait photo). */
val PhotoBadgePrint: Dp = 48.dp

/** A [PhotoBadge]'s touch target, which the tile's caption leaves free: the print and the gap before it. */
val PhotoBadgeSize: Dp = 48.dp

/**
 * The width a pose tile's caption leaves free at its end for [photo]'s [PhotoBadge]: the badge's touch target, and
 * at least [Space.s] between the name and the print, so the wide print of the landscape group photo doesn't touch
 * the name. The library's name measurement uses the same width.
 */
fun photoBadgeReserve(photo: PosePhoto, printSize: Dp = PhotoBadgePrint): Dp {
    val width = if (photo.isLandscape) printSize else printSize * photo.aspect
    return maxOf(PhotoBadgeSize, width + Space.s)
}

private val BadgeGlyph = 16.dp

/** How far the glyph's disc reaches past the print's bottom-end corner. */
private val BadgeGlyphOverhang = 4.dp

private const val ShowCredit = "Show the full photo credit"
