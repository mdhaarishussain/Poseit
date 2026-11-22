package com.poseit.app.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poseit.app.appContainer
import com.poseit.app.data.PosePhoto
import com.poseit.app.data.PosePhotos
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.ui.theme.LocalSurfaceTone
import com.poseit.app.ui.theme.PoseitColors
import com.poseit.app.ui.theme.PoseitShapes
import com.poseit.app.ui.theme.PoseitType
import com.poseit.app.ui.theme.Space
import com.poseit.app.ui.theme.SurfaceTone

/*
 * Sample photos of the built-in poses (docs/DESIGN.md, "Sample photos"; docs/POSE_PHOTOS.md). A photo appears only
 * in the pose guide, beside or with the pose's figure, and always with its credit line ([PhotoCredit]) or, for a tiny
 * thumbnail, a credit one tap away on the same screen. The white figure stays the guide; the photo shows a real
 * person in the pose. The app never offers a photo to share, save, export or open elsewhere, never names or
 * captions the people, and never implies they use or endorse Poseit.
 */

/** The app's [PosePhotos] (credits and decoded pixels). */
@Composable
fun rememberPosePhotos(): PosePhotos {
    val context = LocalContext.current.applicationContext
    return remember(context) { context.appContainer.posePhotos }
}

/** The sample photo of the built-in pose [poseId], or null when it has none (decide your layout from this). */
@Composable
fun rememberPosePhoto(poseId: String?): PosePhoto? {
    val photos = rememberPosePhotos()
    return remember(photos, poseId) { poseId?.let(photos::photoFor) }
}

/** The sample photo of [pose], or null: poses people make never have one. */
@Composable
fun rememberPosePhoto(pose: PoseTemplate?): PosePhoto? {
    val photos = rememberPosePhotos()
    return remember(photos, pose) { pose?.let(photos::photoFor) }
}

/**
 * The sample photo of [pose], filled and cropped into [modifier]'s bounds and clipped to [shape]; draws nothing at
 * all when the pose has no photo, so check [rememberPosePhoto] first and give the pose its figure-only layout.
 */
@Composable
fun PosePhoto(
    pose: PoseTemplate,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    alignment: Alignment = Alignment.Center,
    placeholderColor: Color = Color.Unspecified,
) {
    val photo = rememberPosePhoto(pose) ?: return
    PosePhoto(photo, modifier, shape, alignment, placeholderColor)
}

/**
 * A pose's sample photo: decoded off the main thread at the size it is drawn, filled and cropped into [modifier]'s
 * bounds (give it a size, or `fillMaxWidth().aspectRatio(photo.aspect)`), clipped to [shape] (the concentric radius
 * of where it sits: `PoseitShapes.Inner` in a card, `Tile` in a grid). A quiet placeholder colour shows while it
 * decodes ([placeholderColor]; by default the raised card colour on a dark surface, the muted fill on cream).
 *
 * @param contentDescription what the photo shows, by default "Photo of a person in the Hand on hip pose"; pass null
 *   when the photo sits inside a control whose label already names the pose
 */
@Composable
fun PosePhoto(
    photo: PosePhoto,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    alignment: Alignment = Alignment.Center,
    placeholderColor: Color = Color.Unspecified,
    contentDescription: String? = photo.contentDescription,
) {
    val photos = rememberPosePhotos()
    val placeholder = placeholderColor.takeOrElse {
        if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.backgroundCardRaised else PoseitColors.backgroundMuted
    }
    BoxWithConstraints(
        modifier
            .clip(shape)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Image
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else photo.width
        val h = if (constraints.hasBoundedHeight) constraints.maxHeight else photo.height
        // Re-decode only when the box needs another power-of-two sample, not on every pixel of an animation.
        val sample = PosePhotos.sampleSizeFor(photo.width, photo.height, w, h)
        key(photo.poseId) {
            val bitmap by produceState(photos.cachedBitmap(photo, w, h), sample) {
                value = photos.bitmap(photo, w, h)
            }
            Box(Modifier.fillMaxSize().background(placeholder)) {
                bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = alignment,
                    )
                }
            }
        }
    }
}

/**
 * A photo's credit line ([PosePhoto.attribution], "Photo: Mixkit") in the Supporting style after a small info glyph:
 * cream on a dark card, the secondary ink on cream ([color] overrides). The whole line is one button (48dp tall,
 * Role.Button, spoken as "Photo credit: Mixkit") that opens [PhotoCreditSheet] with the full credit. Put it under or
 * beside every photo you show.
 *
 * @param maxLines Commons credits are long ("Photo: <author>, CC BY 2.0, Wikimedia Commons (cropped)"); let them
 *   wrap where you can, the full credit is always one tap away
 */
@Composable
fun PhotoCredit(
    photo: PosePhoto,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
) {
    var open by rememberSaveable(photo.poseId) { mutableStateOf(false) }
    val fg = color.takeOrElse {
        if (LocalSurfaceTone.current == SurfaceTone.Dark) PoseitColors.textPrimaryInverted else PoseitColors.textSecondary
    }
    val spoken = photoCreditLabel(photo)
    Row(
        modifier
            .heightIn(min = ButtonHeight)
            .clip(PoseitShapes.Inner)
            .clickable(onClickLabel = SHOW_CREDIT, role = Role.Button) { open = true }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = SHOW_CREDIT) {
                    open = true
                    true
                }
            }
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The glyph sits on the first line when a long credit wraps, and grows with the text at large font sizes:
        // 16dp in an 18dp line at 100%, centred on that line at any size.
        val line = with(LocalDensity.current) { PoseitType.supporting.lineHeight.toDp() }
        val glyph = line * (CreditGlyph / CreditLine)
        Row(verticalAlignment = Alignment.Top) {
            Icon(PoseitIcons.info, contentDescription = null, tint = fg, modifier = Modifier.padding(top = (line - glyph) / 2).size(glyph))
            Spacer(Modifier.width(6.dp))
            Text(photo.attribution, style = PoseitType.supporting, color = fg, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
    }
    if (open) PhotoCreditSheet(photo, onDismissRequest = { open = false })
}

/** How a [PhotoCredit] is announced: "Photo credit: Mixkit". */
fun photoCreditLabel(photo: PosePhoto): String = "Photo credit: " + photo.attribution.removePrefix("Photo: ")

private const val SHOW_CREDIT = "Show the full photo credit"
private val CreditGlyph = 16.dp

/** The credit text's line height at 100% (`PoseitType.supporting`), which [CreditGlyph] is drawn in. */
private val CreditLine = 18.dp

/**
 * The full credit of a sample photo in a [PoseitBottomSheet]: the credit line, the author, the source with "View
 * source" (the clip or file page in the browser), the licence with "View licence" and "Read the licence" (the
 * bundled text, in the same sheet), and the changes made in plain words.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCreditSheet(photo: PosePhoto, onDismissRequest: () -> Unit) {
    PoseitBottomSheet(onDismissRequest = onDismissRequest) {
        // Scrolls on a short phone or at a large font size; the licence page scrolls its own text inside.
        Column(Modifier.verticalScroll(rememberScrollState())) {
            PhotoCreditContent(photo, Modifier.padding(bottom = Space.l))
        }
    }
}

/**
 * The inside of a [PhotoCreditSheet], also usable inline (the component gallery, tests). Two pages: the credit, and
 * the licence text after "Read the licence" ([showLicence] opens on it).
 */
@Composable
fun PhotoCreditContent(photo: PosePhoto, modifier: Modifier = Modifier, showLicence: Boolean = false) {
    var licencePage by rememberSaveable(photo.poseId) { mutableStateOf(showLicence) }
    var linkFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    fun open(url: String) {
        linkFailed = !openInBrowser(context, url)
    }
    ProvideSurfaceTone(SurfaceTone.Light) {
        Column(modifier.fillMaxWidth()) {
            if (licencePage) {
                LicencePage(photo, onBack = { licencePage = false })
            } else {
                Text(
                    "Photo credit",
                    style = PoseitType.title,
                    color = PoseitColors.textPrimary,
                    modifier = Modifier
                        .padding(horizontal = Space.textInset)
                        .semantics { heading() },
                )
                Text(
                    "Sample photo for ${photo.poseName}",
                    style = PoseitType.supporting,
                    color = PoseitColors.textSecondary,
                    modifier = Modifier.padding(horizontal = Space.textInset),
                )
                Spacer(Modifier.height(Space.l))
                RowGroup(Modifier.padding(horizontal = Space.edge)) {
                    CreditItem("Credit", photo.attribution)
                    CreditItem("Author", photo.authorForDisplay)
                    CreditItem("Source", photo.source) {
                        SecondaryButton("View source", onClick = { open(photo.pageUrl) })
                    }
                    CreditItem("Licence", photo.licence) {
                        SecondaryButton("View licence", onClick = { open(photo.licenceUrl) })
                        SecondaryButton("Read the licence", onClick = { licencePage = true })
                    }
                    CreditItem("Changes", photo.changesSentence)
                }
                if (linkFailed) {
                    Text(
                        "No app on this phone can open web links.",
                        style = PoseitType.supporting,
                        color = PoseitColors.signalMiss,
                        modifier = Modifier.padding(horizontal = Space.textInset, vertical = Space.s),
                    )
                }
            }
        }
    }
}

/** One labelled part of the credit: the label in Supporting secondary, the value in Body, then any [actions]. */
@Composable
private fun CreditItem(label: String, value: String, actions: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(RowPadding)) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            Text(label, style = PoseitType.supporting, color = PoseitColors.textSecondary)
            Text(value, style = PoseitType.body, color = PoseitColors.textPrimary)
        }
        if (actions != null) {
            Spacer(Modifier.height(Space.s))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                actions()
            }
        }
    }
}

/** The bundled licence text, scrollable, under a back arrow and the licence's name. */
@Composable
private fun LicencePage(photo: PosePhoto, onBack: () -> Unit) {
    val photos = rememberPosePhotos()
    val text by produceState<String?>(null, photo.licenceTextPath) {
        value = runCatching { photos.licenceText(photo.licenceTextPath) }.getOrDefault("")
    }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    Row(
        Modifier.padding(start = Space.textInset - IconGlyphInset, end = Space.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIconButton(PoseitIcons.back, "Back to the photo credit", onBack)
        Spacer(Modifier.width(Space.xs))
        Text(
            photo.licence,
            style = PoseitType.title,
            color = PoseitColors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
    }
    Spacer(Modifier.height(Space.s))
    Box(
        Modifier
            .padding(horizontal = Space.edge)
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = maxHeight)
            .clip(PoseitShapes.Row)
            .background(PoseitColors.backgroundBody),
    ) {
        val body = text
        if (body == null) {
            PoseitSpinner(Modifier.align(Alignment.Center), color = PoseitColors.textPrimary)
        } else {
            Text(
                body.ifEmpty { "The licence text could not be read. It is also at ${photo.licenceUrl}" },
                style = PoseitType.supporting,
                color = PoseitColors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.l, vertical = Space.l),
            )
        }
    }
}

/** Opens [url] in the browser (ACTION_VIEW); false when no app on the phone can open it. */
private fun openInBrowser(context: Context, url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
