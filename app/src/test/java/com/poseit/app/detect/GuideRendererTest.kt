package com.poseit.app.detect

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideRendererTest {

    private val width = 200
    private val height = 300

    private fun mask(fill: (x: Int, y: Int) -> Boolean) = BooleanArray(width * height) { i -> fill(i % width, i / width) }

    private fun alpha(c: Int) = c ushr 24
    private fun gray(c: Int) = c and 0xFF

    @Test
    fun emptyMaskGivesNull() {
        assertNull(GuideRenderer.render(BooleanArray(width * height), width, height))
    }

    @Test
    fun personBelowOnePercentGivesNull() {
        // 20 x 20 = 400 px = 0.67 % of 60 000.
        val m = mask { x, y -> x in 90 until 110 && y in 140 until 160 }
        assertNull(GuideRenderer.render(m, width, height))
    }

    @Test
    fun rectangleGetsFillOutlineAndHalo() {
        val m = mask { x, y -> x in 60 until 140 && y in 60 until 240 }
        val px = GuideRenderer.render(m, width, height)
        assertNotNull(px)
        px!!
        assertEquals(width * height, px.size)
        val y = 150
        fun at(x: Int) = px[y * width + x]
        // Deep inside: translucent white fill.
        assertEquals(GuideRenderer.FILL_COLOR, at(100))
        assertEquals(64, alpha(GuideRenderer.FILL_COLOR))
        // Stroke (2 px for a 300 px tall image) straddles the contour between x = 59 and x = 60.
        assertEquals(0xFFFFFFFF.toInt(), at(60))
        assertEquals(0xFFFFFFFF.toInt(), at(59))
        // Just inside the stroke: fill again.
        assertEquals(GuideRenderer.FILL_COLOR, at(61))
        // Just outside the stroke: dark translucent halo.
        assertEquals(102, alpha(at(58)))
        assertEquals(0, gray(at(58)))
        // Beyond the halo and far away: transparent.
        assertEquals(0, at(57))
        assertEquals(0, px[0])
        assertEquals(0, px[width * height - 1])
    }

    @Test
    fun noOutlineAlongTheImageBorder() {
        // A person cut off by the bottom of the frame.
        val m = mask { x, y -> x in 60 until 140 && y >= 200 }
        val px = GuideRenderer.render(m, width, height)!!
        assertEquals(GuideRenderer.FILL_COLOR, px[(height - 1) * width + 100])
        // The top edge of the shape is still outlined.
        assertEquals(0xFFFFFFFF.toInt(), px[200 * width + 100])
    }

    @Test
    fun majorityFilterRemovesSpecksAndFillsPinholes() {
        val m = mask { x, y -> x in 50 until 150 && y in 50 until 250 }
        m[10 * width + 10] = true // isolated speck outside
        m[150 * width + 100] = false // pinhole inside
        val smooth = GuideRenderer.majorityFilter(m, width, height, radius = 1)
        assertFalse(smooth[10 * width + 10])
        assertTrue(smooth[150 * width + 100])
        // Straight edges survive.
        assertTrue(smooth[150 * width + 50])
        assertFalse(smooth[150 * width + 49])
    }

    @Test
    fun majorityFilterWithZeroRadiusIsIdentity() {
        val m = mask { x, y -> (x * 7 + y * 3) % 5 == 0 }
        assertArrayEquals(m, GuideRenderer.majorityFilter(m, width, height, radius = 0))
    }

    @Test
    fun smallBlobsAreRemovedLargeOnesKept() {
        val m = mask { x, y -> (x in 20 until 120 && y in 20 until 220) || (x in 180 until 184 && y in 280 until 284) }
        GuideRenderer.removeSmallBlobs(m, width, height, minSize = 60)
        assertTrue(m[100 * width + 50])
        assertFalse(m[281 * width + 181])
    }

    @Test
    fun edgeDistanceIsZeroOnEdgesAndBoundedByReach() {
        val m = mask { x, y -> x in 60 until 140 && y in 60 until 240 }
        val d2 = GuideRenderer.squaredDistanceToEdge(m, width, height, reach = 3)
        assertEquals(0, d2[150 * width + 60])
        assertEquals(1, d2[150 * width + 59])
        assertEquals(9, d2[150 * width + 57])
        assertEquals(Int.MAX_VALUE, d2[150 * width + 56])
        assertEquals(Int.MAX_VALUE, d2[150 * width + 100])
    }

    @Test
    fun smoothingRadiusScalesWithImageSize() {
        assertEquals(1, GuideRenderer.smoothingRadius(200, 300))
        assertEquals(2, GuideRenderer.smoothingRadius(768, 1024))
    }
}
