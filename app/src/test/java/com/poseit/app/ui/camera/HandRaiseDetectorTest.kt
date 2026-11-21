package com.poseit.app.ui.camera

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandRaiseDetectorTest {

    private val aspect = 0.75f

    /** A standing person (scene space); wrist height configurable. */
    private fun person(leftWristY: Float = 0.55f, wristVisibility: Float = 0.9f): Skeleton {
        val points = mapOf(
            Joint.NOSE to Vec2(0.5f, 0.25f),
            Joint.LEFT_SHOULDER to Vec2(0.58f, 0.35f),
            Joint.RIGHT_SHOULDER to Vec2(0.42f, 0.35f),
            Joint.LEFT_HIP to Vec2(0.55f, 0.6f),
            Joint.RIGHT_HIP to Vec2(0.45f, 0.6f),
            Joint.LEFT_WRIST to Vec2(0.62f, leftWristY),
            Joint.RIGHT_WRIST to Vec2(0.38f, 0.55f),
        )
        val visibility = points.keys.associateWith { 0.95f } + (Joint.LEFT_WRIST to wristVisibility)
        return Skeleton(points, visibility)
    }

    private val raised = person(leftWristY = 0.1f)
    private val lowered = person()

    @Test
    fun `detects a wrist clearly above the nose`() {
        assertTrue(HandRaiseDetector.isHandRaised(raised, aspect))
        assertFalse(HandRaiseDetector.isHandRaised(lowered, aspect))
        // Barely above the nose is jitter, not a raised hand.
        assertFalse(HandRaiseDetector.isHandRaised(person(leftWristY = 0.245f), aspect))
        // An invisible wrist does not count.
        assertFalse(HandRaiseDetector.isHandRaised(person(leftWristY = 0.1f, wristVisibility = 0.2f), aspect))
    }

    @Test
    fun `fires once after the hold time`() {
        val d = HandRaiseDetector(holdMillis = 800)
        assertFalse(d.update(listOf(raised), aspect, 0))
        assertFalse(d.update(listOf(raised), aspect, 799))
        assertTrue(d.update(listOf(raised), aspect, 800))
        assertFalse(d.update(listOf(raised), aspect, 2000))
        assertFalse(d.update(listOf(raised), aspect, 5000))
    }

    @Test
    fun `lowering the hand before the hold time cancels`() {
        val d = HandRaiseDetector(holdMillis = 800)
        d.update(listOf(raised), aspect, 0)
        d.update(listOf(lowered), aspect, 500)
        assertFalse(d.update(listOf(raised), aspect, 900))
        assertTrue(d.update(listOf(raised), aspect, 1700))
    }

    @Test
    fun `re-arms after the hand comes down`() {
        val d = HandRaiseDetector(holdMillis = 800)
        d.update(listOf(raised), aspect, 0)
        assertTrue(d.update(listOf(raised), aspect, 800))
        d.update(listOf(lowered), aspect, 1000)
        d.update(listOf(raised), aspect, 1100)
        assertTrue(d.update(listOf(raised), aspect, 1900))
    }

    @Test
    fun `suppressed updates never fire and need a fresh hold`() {
        val d = HandRaiseDetector(holdMillis = 800)
        d.update(listOf(raised), aspect, 0, allowTrigger = false)
        assertFalse(d.update(listOf(raised), aspect, 1000, allowTrigger = false))
        assertFalse(d.update(listOf(raised), aspect, 1100, allowTrigger = true))
        assertTrue(d.update(listOf(raised), aspect, 1900, allowTrigger = true))
    }

    @Test
    fun `any of several people can trigger`() {
        val d = HandRaiseDetector(holdMillis = 100)
        d.update(listOf(lowered, raised), aspect, 0)
        assertTrue(d.update(listOf(lowered, raised), aspect, 100))
    }

    @Test
    fun `templates with a hand above the head are recognised`() {
        fun template(sk: Skeleton) = PoseTemplate("t", "T", PoseCategory.FUN, listOf(sk))
        assertTrue(HandRaiseDetector.templateRaisesHand(template(Skeleton(raised.points))))
        assertFalse(HandRaiseDetector.templateRaisesHand(template(Skeleton(lowered.points))))
        assertEquals(false, HandRaiseDetector.templateRaisesHand(PoseTemplate("e", "E", PoseCategory.FUN, emptyList())))
    }

    @Test
    fun `templates with a hand at head height pause the trigger too`() {
        fun template(sk: Skeleton) = PoseTemplate("t", "T", PoseCategory.FUN, listOf(sk))
        // Wrist just below the nose (hand behind the head / on the wall at head height): a person copying it easily
        // lifts the wrist above the nose, so the trigger must be paused for this pose.
        assertTrue(HandRaiseDetector.templateRaisesHand(template(Skeleton(person(leftWristY = 0.27f).points))))
        // A hand at chest height is far enough below.
        assertFalse(HandRaiseDetector.templateRaisesHand(template(Skeleton(person(leftWristY = 0.45f).points))))
    }
}
