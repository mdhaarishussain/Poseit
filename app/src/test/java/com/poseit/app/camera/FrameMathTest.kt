package com.poseit.app.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameMathTest {

    @Test
    fun rotationIsSnappedToQuarterTurns() {
        assertEquals(0, FrameConverter.normalizeRotation(0))
        assertEquals(90, FrameConverter.normalizeRotation(90))
        assertEquals(270, FrameConverter.normalizeRotation(-90))
        assertEquals(90, FrameConverter.normalizeRotation(450))
        assertEquals(90, FrameConverter.normalizeRotation(89))
        assertEquals(0, FrameConverter.normalizeRotation(359))
    }

    @Test
    fun uprightSizeSwapsForQuarterTurns() {
        assertEquals(480 to 640, FrameConverter.uprightSize(640, 480, 90))
        assertEquals(480 to 640, FrameConverter.uprightSize(640, 480, 270))
        assertEquals(640 to 480, FrameConverter.uprightSize(640, 480, 180))
        assertEquals(640 to 480, FrameConverter.uprightSize(640, 480, 0))
    }

    @Test
    fun increasingTimestampsPassThrough() {
        val clock = MonotonicTimestamps()
        assertEquals(1_000L, clock.next(1_000L))
        assertEquals(1_033L, clock.next(1_033L))
        assertEquals(1_066L, clock.next(1_066L))
    }

    @Test
    fun repeatedTimestampIsBumped() {
        val clock = MonotonicTimestamps()
        assertEquals(500L, clock.next(500L))
        assertEquals(501L, clock.next(500L))
        assertEquals(534L, clock.next(533L))
    }

    @Test
    fun backwardsJumpKeepsRealSpacing() {
        val clock = MonotonicTimestamps()
        clock.next(10_000L)
        // The camera clock restarts near zero (e.g. after a rebind with another time base).
        assertEquals(10_001L, clock.next(50L))
        assertEquals(10_034L, clock.next(83L))
        assertEquals(10_067L, clock.next(116L))
    }
}
