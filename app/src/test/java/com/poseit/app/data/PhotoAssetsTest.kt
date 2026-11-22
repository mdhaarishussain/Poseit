package com.poseit.app.data

import com.poseit.app.pose.library.BuiltInPoses
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plain JVM checks of the shipped sample photos, their credits and the licence texts (docs/POSE_PHOTOS.md): every
 * licence file is indexed and every index entry exists, every credit has its photo and every photo its credit, every
 * credit names a licence text that ships and has a credit line to show. Fails the build before a photo can appear
 * without its credit.
 */
class PhotoAssetsTest {

    private val assets = File("src/main/assets")
    private val licenses = File(assets, "licenses")
    private val photos = File(assets, "pose_photos")

    @Serializable
    private data class LicenceIndex(val licenses: List<Entry>) {
        @Serializable
        data class Entry(val file: String, val name: String, val url: String? = null, val covers: String)
    }

    private val index by lazy {
        Json { ignoreUnknownKeys = true }.decodeFromString<LicenceIndex>(File(licenses, "index.json").readText())
    }
    private val credits by lazy { PhotoCredits.parse(File(photos, "credits.json").readText()) }
    private val webps by lazy { photos.listFiles { f -> f.extension == "webp" }.orEmpty().sortedBy { it.name } }

    @Test
    fun everyIndexEntryHasItsFile() {
        val missing = index.licenses.map { it.file }.filterNot { File(licenses, it).isFile }
        assertTrue("licenses/index.json lists files that are not in the folder: $missing", missing.isEmpty())
        val dupes = index.licenses.groupBy { it.file }.filterValues { it.size > 1 }.keys
        assertTrue("licenses/index.json lists files twice: $dupes", dupes.isEmpty())
        index.licenses.forEach { assertTrue("index entry ${it.file} has no name or covers", it.name.isNotBlank() && it.covers.isNotBlank()) }
    }

    @Test
    fun everyLicenceFileIsIndexed() {
        val files = licenses.listFiles().orEmpty().map { it.name }.filter { it != "index.json" }.toSet()
        val notIndexed = files - index.licenses.map { it.file }.toSet()
        assertTrue("licence files missing from licenses/index.json: $notIndexed", notIndexed.isEmpty())
    }

    @Test
    fun everyCreditHasItsPhoto() {
        val missing = credits.filter { (_, c) -> !File(photos, c.photo).isFile }.keys
        assertTrue("credits without a photo file: $missing", missing.isEmpty())
        credits.forEach { (id, c) -> assertEquals("credit $id names another pose's photo", "$id.webp", c.photo) }
    }

    @Test
    fun everyPhotoHasACredit() {
        val uncredited = webps.map { it.nameWithoutExtension }.filter { it !in credits }
        assertTrue("photos without a credit in credits.json: $uncredited", uncredited.isEmpty())
    }

    @Test
    fun everyCreditsLicenceTextShips() {
        val missing = credits.filter { (_, c) -> !File(assets, c.licenceText).isFile }.mapValues { it.value.licenceText }
        assertTrue("credits whose licence text is missing: $missing", missing.isEmpty())
        val indexed = index.licenses.map { "licenses/${it.file}" }.toSet()
        val notIndexed = credits.values.map { it.licenceText }.filter { it !in indexed }.toSet()
        assertTrue("licence texts used by credits but not indexed: $notIndexed", notIndexed.isEmpty())
    }

    @Test
    fun everyCreditHasACreditLine() {
        val blank = credits.filter { (_, c) -> c.attribution.isBlank() }.keys
        assertTrue("credits with an empty attribution: $blank", blank.isEmpty())
        credits.forEach { (id, c) ->
            assertTrue("$id: the credit line starts with \"Photo: \"", c.attribution.startsWith("Photo: "))
            assertTrue("$id: no author", c.author.isNotBlank())
            assertTrue("$id: no licence", c.licence.isNotBlank())
            assertTrue("$id: source page is not https", c.pageUrl.startsWith("https://"))
            assertTrue("$id: licence link is not https", c.licenceUrl.startsWith("https://"))
        }
    }

    @Test
    fun creditsBelongToBuiltInPosesAndRecordTheRealSize() {
        credits.forEach { (id, c) ->
            assertTrue("$id is not a built-in pose", BuiltInPoses.byId(id) != null)
            val recorded = PhotoCredits.recordedSize(c)
            val actual = webpSize(File(photos, c.photo).readBytes())
            assertEquals("$id: the size in the credit's changes", actual, recorded)
        }
    }

    @Test
    fun licenceTextsReflowIntoParagraphsWithoutLosingAWord() {
        val raw = "Attribution 2.0\n\n" +
            "CREATIVE COMMONS CORPORATION IS NOT A LAW FIRM AND DOES NOT PROVIDE LEGAL\n" +
            "SERVICES. DISTRIBUTION OF THIS LICENSE.\n\n" +
            "a. \"Work\" means the copyrightable work of authorship offered under the\n" +
            "   terms of this License.\n" +
            "b. \"You\" means an individual.\n"
        assertEquals(
            "Attribution 2.0\n\n" +
                "CREATIVE COMMONS CORPORATION IS NOT A LAW FIRM AND DOES NOT PROVIDE LEGAL SERVICES. DISTRIBUTION OF THIS LICENSE.\n\n" +
                "a. \"Work\" means the copyrightable work of authorship offered under the terms of this License.\n" +
                "b. \"You\" means an individual.\n",
            LicenceText.reflow(raw),
        )
        val words = Regex("\\s+")
        licenses.listFiles { f -> f.extension == "txt" }.orEmpty().forEach { f ->
            val text = f.readText()
            assertEquals(f.name, text.split(words).filter { it.isNotEmpty() }, LicenceText.reflow(text).split(words).filter { it.isNotEmpty() })
        }
    }

    /**
     * CC BY and BY-SA 2.x and 3.0 require the title of the work in the credit, if the licensor supplies one: every
     * such credit carries it, and the app shows it as "Original title" in the full credit.
     */
    @Test
    fun creditsUnderLicencesThatAskForATitleHaveOne() {
        val needTitle = credits.filterValues { PhotoCredits.licenceRequiresTitle(it.licence) }
        assertTrue("no shipped photo is under CC BY 2.0? the check would test nothing", needTitle.isNotEmpty())
        needTitle.forEach { (id, c) ->
            val title = c.title?.let(PhotoCredits::displayTitle)
            assertTrue("$id (${c.licence}) needs a title", !title.isNullOrBlank())
        }
        assertEquals("Ladies Grey Loungewear Set", PhotoCredits.displayTitle("File:Ladies Grey Loungewear Set (51618413959).jpg"))
        assertEquals("Woman in a red dress", PhotoCredits.displayTitle("File:Woman in a red dress.jpg"))
        assertTrue(PhotoCredits.licenceRequiresTitle("CC BY 2.0"))
        assertTrue(PhotoCredits.licenceRequiresTitle("CC BY-SA 3.0"))
        assertTrue(PhotoCredits.licenceRequiresTitle("CC BY 2.5"))
        assertTrue(!PhotoCredits.licenceRequiresTitle("CC BY 4.0"))
        assertTrue(!PhotoCredits.licenceRequiresTitle("CC0"))
        assertTrue(!PhotoCredits.licenceRequiresTitle("Mixkit Stock Video Free License"))
    }

    /**
     * credits.json ships inside the APK, so it holds only what the app reads: the review notes (which describe the
     * people), scores and download links stay in tools/pose-photos/credits-full.json.
     */
    @Test
    fun theShippedCreditsHoldOnlyWhatTheAppReads() {
        val appFields = setOf("photo", "source", "title", "pageUrl", "author", "licence", "licenceUrl", "licenceText", "attribution", "changes", "mirrored", "frameTimeS")
        val raw = Json.parseToJsonElement(File(photos, "credits.json").readText()).jsonObject
        raw.forEach { (id, entry) ->
            val extra = entry.jsonObject.keys - appFields
            assertTrue("$id carries fields the app does not read: $extra", extra.isEmpty())
        }
        // The parser the release build runs (R8 keeps its generated serializer) reads every entry.
        assertEquals(raw.keys, credits.keys)
    }

    /** The licence texts are shown in the app: their headers speak to the reader, not to the people who prepared them. */
    @Test
    fun licenceTextsShownInTheAppHaveNoMaintainerNotes() {
        credits.values.map { it.licenceText }.toSet().forEach { path ->
            val head = File(assets, path).readText().substringBefore("\n\n")
            assertTrue("$path header names a repository path", "app/src" !in head && "docs/" !in head && "credits.json" !in head)
        }
        val mixkit = File(licenses, "Mixkit-Stock-Video-Free-License.txt").readText()
        assertTrue("could not be read" !in mixkit)
    }

    /** Width and height from a WebP header (lossy VP8, lossless VP8L or extended VP8X). */
    private fun webpSize(b: ByteArray): Pair<Int, Int> {
        fun u8(i: Int) = b[i].toInt() and 0xFF
        fun le16(i: Int) = u8(i) or (u8(i + 1) shl 8)
        fun le24(i: Int) = le16(i) or (u8(i + 2) shl 16)
        require(String(b, 0, 4) == "RIFF" && String(b, 8, 4) == "WEBP") { "not a WebP file" }
        return when (String(b, 12, 4)) {
            "VP8 " -> (le16(26) and 0x3FFF) to (le16(28) and 0x3FFF)
            "VP8L" -> {
                val bits = le16(21) or (le16(23) shl 16)
                ((bits and 0x3FFF) + 1) to (((bits shr 14) and 0x3FFF) + 1)
            }
            "VP8X" -> (le24(24) + 1) to (le24(27) + 1)
            else -> error("unknown WebP chunk ${String(b, 12, 4)}")
        }
    }
}
