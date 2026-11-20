package com.poseit.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechGateTest {

    private val minGap = 2_500L
    private val repeatGap = 6_000L
    private val gate = SpeechGate()

    private fun say(text: String, at: Long, urgent: Boolean = false, speaking: Boolean = false) =
        gate.tryAcquire(text, urgent, at, speaking, minGap, repeatGap)

    @Test
    fun firstMessageIsSpoken() {
        assertTrue(say("Raise your left arm", at = 0))
    }

    @Test
    fun identicalMessageIsDroppedWithinRepeatGapEvenIfUrgent() {
        assertTrue(say("Step back", at = 0))
        assertFalse(say("Step back", at = 3_000))
        assertFalse(say("Step back", at = 5_999, urgent = true))
        assertTrue(say("Step back", at = 6_000))
    }

    @Test
    fun differentNonUrgentMessagesRespectTheMinimumGap() {
        assertTrue(say("Step back", at = 0))
        assertFalse(say("Turn left", at = 2_000))
        assertTrue(say("Turn left", at = 2_500))
    }

    @Test
    fun minimumGapIsMeasuredFromTheEndOfTheLastUtterance() {
        assertTrue(say("A long sentence", at = 0))
        gate.onFinished(2_000)
        assertFalse(say("Next hint", at = 3_000))
        assertTrue(say("Next hint", at = 4_500))
    }

    @Test
    fun nonUrgentNeverInterruptsUrgentDoes() {
        assertTrue(say("Step back", at = 0))
        assertFalse(say("Turn left", at = 10_000, speaking = true))
        assertTrue(say("3", at = 10_000, urgent = true, speaking = true))
        assertTrue(say("2", at = 11_000, urgent = true, speaking = true))
    }

    @Test
    fun resetAllowsImmediateRepeat() {
        assertTrue(say("Step back", at = 0))
        gate.reset()
        assertTrue(say("Step back", at = 100))
    }
}
