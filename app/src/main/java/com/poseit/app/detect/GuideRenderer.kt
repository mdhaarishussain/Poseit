package com.poseit.app.detect

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Turns a binary person mask into the pixels of a SOVS / Superpose-style guide: a translucent white fill inside
 * the person, a crisp anti-aliased white outline along the silhouette and a thin dark halo just outside it, so the
 * outline stays visible on bright scenes. Everything else is transparent.
 *
 * Pure Kotlin on flat arrays (no Android APIs, no per-pixel Bitmap calls) so it is fast and unit-testable.
 */
internal object GuideRenderer {

    /** Masks covering less than this fraction of the image count as "no person". */
    const val MIN_PERSON_FRACTION = 0.01f

    /** Outline width as a fraction of the image height. */
    const val STROKE_FRACTION = 0.006f
    const val MIN_STROKE_PX = 2f

    /** Halo width relative to the outline width. */
    const val HALO_FRACTION = 0.4f

    const val FILL_ALPHA = 0.25f
    const val HALO_ALPHA = 0.40f

    /** Blobs smaller than this fraction of the image are segmentation noise and are dropped. */
    const val MIN_BLOB_FRACTION = 0.001f

    /** Colour of the fill far from the outline (non-premultiplied ARGB). */
    val FILL_COLOR: Int = argb(FILL_ALPHA, 1f)

    /**
     * @param mask row-major person mask, `width * height` entries
     * @return non-premultiplied ARGB pixels (`width * height`), or null if the person covers < 1 % of the image
     */
    fun render(mask: BooleanArray, width: Int, height: Int): IntArray? {
        require(width > 0 && height > 0) { "Empty mask" }
        val n = width * height
        require(mask.size >= n) { "Mask has ${mask.size} entries, expected $n" }
        if (count(mask, n) < n * MIN_PERSON_FRACTION) return null

        val smooth = majorityFilter(mask, width, height, smoothingRadius(width, height))
        removeSmallBlobs(smooth, width, height, max(1, (n * MIN_BLOB_FRACTION).roundToInt()))
        if (count(smooth, n) == 0) return null

        val stroke = max(MIN_STROKE_PX, height * STROKE_FRACTION)
        val halo = max(1f, stroke * HALO_FRACTION)
        val half = stroke / 2f
        val reach = ceil(half + halo + 2f).toInt()
        val dist2 = squaredDistanceToEdge(smooth, width, height, reach)
        return compose(smooth, dist2, n, half, halo)
    }

    /** Radius of the majority filter: ~1 px per 400 px of the short side (DeepLab masks are upsampled ~4x). */
    fun smoothingRadius(width: Int, height: Int): Int = max(1, (min(width, height) / 400f).roundToInt())

    /**
     * Binary majority filter over a (2r+1)² box (clipped at the borders): a pixel is "person" when more than half
     * of the box is. Two running-sum passes, O(n) regardless of [radius].
     */
    fun majorityFilter(mask: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        val n = width * height
        val r = radius.coerceAtLeast(0)
        // Horizontal pass: rowSums[i] = person pixels in [x - r, x + r] of the same row.
        val rowSums = IntArray(n)
        for (y in 0 until height) {
            val row = y * width
            var sum = 0
            for (x in 0..min(r, width - 1)) if (mask[row + x]) sum++
            for (x in 0 until width) {
                rowSums[row + x] = sum
                val add = x + r + 1
                if (add < width && mask[row + add]) sum++
                val remove = x - r
                if (remove >= 0 && mask[row + remove]) sum--
            }
        }
        val countX = IntArray(width) { x -> min(x + r, width - 1) - max(x - r, 0) + 1 }
        // Vertical pass with a sliding column sum (row-major, cache friendly).
        val colSums = IntArray(width)
        for (y in 0..min(r, height - 1)) {
            val row = y * width
            for (x in 0 until width) colSums[x] += rowSums[row + x]
        }
        val out = BooleanArray(n)
        for (y in 0 until height) {
            val row = y * width
            val countY = min(y + r, height - 1) - max(y - r, 0) + 1
            for (x in 0 until width) out[row + x] = colSums[x] * 2 > countX[x] * countY
            val add = y + r + 1
            if (add < height) {
                val addRow = add * width
                for (x in 0 until width) colSums[x] += rowSums[addRow + x]
            }
            val remove = y - r
            if (remove >= 0) {
                val removeRow = remove * width
                for (x in 0 until width) colSums[x] -= rowSums[removeRow + x]
            }
        }
        return out
    }

    /** Clears 4-connected person blobs smaller than [minSize] pixels (in place). */
    fun removeSmallBlobs(mask: BooleanArray, width: Int, height: Int, minSize: Int) {
        if (minSize <= 1) return
        val n = width * height
        val visited = BooleanArray(n)
        // BFS queue; after a blob's search, queue[0 until tail] holds exactly that blob's pixels.
        val queue = IntArray(n)
        for (start in 0 until n) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            while (head < tail) {
                val i = queue[head++]
                val x = i % width
                if (x > 0 && mask[i - 1] && !visited[i - 1]) {
                    visited[i - 1] = true
                    queue[tail++] = i - 1
                }
                if (x < width - 1 && mask[i + 1] && !visited[i + 1]) {
                    visited[i + 1] = true
                    queue[tail++] = i + 1
                }
                if (i >= width && mask[i - width] && !visited[i - width]) {
                    visited[i - width] = true
                    queue[tail++] = i - width
                }
                if (i + width < n && mask[i + width] && !visited[i + width]) {
                    visited[i + width] = true
                    queue[tail++] = i + width
                }
            }
            if (tail < minSize) for (k in 0 until tail) mask[queue[k]] = false
        }
    }

    /**
     * Squared distance from every pixel to the nearest silhouette edge pixel (a person pixel with a 4-neighbour
     * outside the person), or [Int.MAX_VALUE] beyond [reach] px. The image border is not an edge, so people cut
     * off by the frame get no outline along it.
     */
    fun squaredDistanceToEdge(mask: BooleanArray, width: Int, height: Int, reach: Int): IntArray {
        val n = width * height
        val dist2 = IntArray(n) { Int.MAX_VALUE }
        // Disk of offsets within the reach.
        val maxD2 = reach * reach
        var count = 0
        val dxs = IntArray((2 * reach + 1) * (2 * reach + 1))
        val dys = IntArray(dxs.size)
        val d2s = IntArray(dxs.size)
        for (dy in -reach..reach) for (dx in -reach..reach) {
            val d2 = dx * dx + dy * dy
            if (d2 <= maxD2) {
                dxs[count] = dx
                dys[count] = dy
                d2s[count] = d2
                count++
            }
        }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val i = row + x
                if (!mask[i]) continue
                val edge = (x > 0 && !mask[i - 1]) || (x < width - 1 && !mask[i + 1]) ||
                    (y > 0 && !mask[i - width]) || (y < height - 1 && !mask[i + width])
                if (!edge) continue
                for (k in 0 until count) {
                    val nx = x + dxs[k]
                    val ny = y + dys[k]
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                    val j = ny * width + nx
                    if (d2s[k] < dist2[j]) dist2[j] = d2s[k]
                }
            }
        }
        return dist2
    }

    private fun compose(mask: BooleanArray, dist2: IntArray, n: Int, half: Float, halo: Float): IntArray {
        val out = IntArray(n)
        for (i in 0 until n) {
            val inside = mask[i]
            val d2 = dist2[i]
            if (d2 == Int.MAX_VALUE) {
                out[i] = if (inside) FILL_COLOR else 0
                continue
            }
            // Signed distance to the silhouette contour, which runs half a pixel outside the edge pixels.
            val d = sqrt(d2.toFloat())
            val s = if (inside) -(d + 0.5f) else d - 0.5f
            val strokeCoverage = (half + 0.5f - abs(s)).coerceIn(0f, 1f)
            val baseAlpha: Float
            val baseGray: Float
            if (inside) {
                baseAlpha = FILL_ALPHA
                baseGray = 1f
            } else {
                baseAlpha = HALO_ALPHA * (half + halo + 0.5f - s).coerceIn(0f, 1f)
                baseGray = 0f
            }
            // Opaque white stroke composited over the base (fill or halo).
            val alpha = strokeCoverage + baseAlpha * (1f - strokeCoverage)
            if (alpha < 1f / 255f) {
                out[i] = 0
                continue
            }
            val premultiplied = strokeCoverage + baseGray * baseAlpha * (1f - strokeCoverage)
            out[i] = argb(alpha, premultiplied / alpha)
        }
        return out
    }

    private fun count(mask: BooleanArray, n: Int): Int {
        var c = 0
        for (i in 0 until n) if (mask[i]) c++
        return c
    }

    /** Non-premultiplied ARGB from an alpha and a gray level (both 0..1). */
    private fun argb(alpha: Float, gray: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        val g = (gray.coerceIn(0f, 1f) * 255f).roundToInt()
        return (a shl 24) or (g shl 16) or (g shl 8) or g
    }
}
