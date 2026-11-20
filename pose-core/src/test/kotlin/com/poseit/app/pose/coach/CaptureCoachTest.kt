package com.poseit.app.pose.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCoachTest {

    private val settings = CoachSettings(
        autoCapture = true,
        threshold = 0.8f,
        holdMillis = 700L,
        countdownSeconds = 3,
        cooldownMillis = 2500L,
        abortMargin = 0.12f,
    )

    private fun countdown(s: CaptureState): CaptureState.Countdown {
        assertTrue("expected Countdown but was $s", s is CaptureState.Countdown)
        return s as CaptureState.Countdown
    }

    @Test
    fun autoCaptureTimeline() {
        val c = CaptureCoach(settings)
        assertEquals(CaptureState.Idle, c.state)
        assertEquals(CaptureState.Idle, c.update(0.5f, 0))
        assertEquals(CaptureState.Holding(0f), c.update(0.9f, 100))
        assertEquals(CaptureState.Holding(0.5f), c.update(0.9f, 450))
        val start = countdown(c.update(0.9f, 800))
        assertEquals(3, start.secondsLeft)
        assertEquals(0f, start.progress)
        assertFalse(start.manual)
        assertEquals(1, start.shot)
        assertEquals(1, start.totalShots)
        // The auto countdown tolerates small dips (>= threshold - abortMargin).
        val mid = countdown(c.update(0.7f, 1800))
        assertEquals(2, mid.secondsLeft)
        assertEquals(1f / 3f, mid.progress, 1e-4f)
        assertEquals(1, countdown(c.update(0.9f, 3500)).secondsLeft)
        assertEquals(CaptureState.Capture, c.update(0.9f, 3800))
        assertEquals(CaptureState.Capture, c.state)
        // No second Capture until the app confirms.
        assertEquals(CaptureState.Cooldown(0f), c.update(0.95f, 3833))
        assertEquals(CaptureState.Cooldown(0f), c.update(0.95f, 9000))
        c.onCaptured(9000)
        assertEquals(CaptureState.Cooldown(0f), c.state)
        assertEquals(CaptureState.Cooldown(0.5f), c.update(0.95f, 10250))
        // Cooldown over: holding again straight away because the score is still high.
        assertEquals(CaptureState.Holding(0f), c.update(0.95f, 11500))
    }

    @Test
    fun holdRestartsWhenTheScoreDips() {
        val c = CaptureCoach(settings)
        c.update(0.9f, 0)
        c.update(0.9f, 600)
        assertEquals(CaptureState.Idle, c.update(0.79f, 650))
        assertEquals(CaptureState.Holding(0f), c.update(0.9f, 700))
        assertTrue(c.update(0.9f, 1300) is CaptureState.Holding)
        assertTrue(c.update(0.9f, 1400) is CaptureState.Countdown)
    }

    @Test
    fun autoCountdownAbortsWhenScoreDropsTooFar() {
        val c = CaptureCoach(settings)
        c.update(0.9f, 0)
        countdown(c.update(0.9f, 700))
        countdown(c.update(0.69f, 1200)) // 0.69 >= 0.68: keep going
        assertEquals(CaptureState.Idle, c.update(0.6f, 1500))
        assertEquals(CaptureState.Idle, c.update(0.6f, 5000))
    }

    @Test
    fun zeroSecondCountdownCapturesRightAfterTheHold() {
        val c = CaptureCoach(settings.copy(countdownSeconds = 0))
        c.update(0.9f, 0)
        assertTrue(c.update(0.9f, 699) is CaptureState.Holding)
        assertEquals(CaptureState.Capture, c.update(0.9f, 700))
        assertEquals(CaptureState.Cooldown(0f), c.update(0.9f, 733))
    }

    @Test
    fun zeroHoldStartsTheCountdownImmediately() {
        val c = CaptureCoach(settings.copy(holdMillis = 0L, countdownSeconds = 0))
        assertEquals(CaptureState.Capture, c.update(0.9f, 0))
    }

    @Test
    fun autoCaptureDisabledStaysIdle() {
        val c = CaptureCoach(settings.copy(autoCapture = false))
        for (t in 0L..10_000L step 33L) assertEquals(CaptureState.Idle, c.update(1f, t))
        // Turning it off during an auto countdown cancels the countdown.
        val d = CaptureCoach(settings)
        d.update(0.9f, 0)
        countdown(d.update(0.9f, 700))
        d.settings = settings.copy(autoCapture = false)
        assertEquals(CaptureState.Idle, d.update(0.9f, 800))
    }

    @Test
    fun manualTimerIgnoresTheScore() {
        val c = CaptureCoach(settings)
        c.startTimer(3, 1000)
        assertTrue(c.isSessionActive)
        val first = countdown(c.state)
        assertTrue(first.manual)
        assertEquals(3, first.secondsLeft)
        assertEquals(1, first.totalShots)
        assertEquals(3, countdown(c.update(0f, 1500)).secondsLeft)
        assertEquals(2, countdown(c.update(0f, 2000)).secondsLeft)
        assertEquals(1, countdown(c.update(Float.NaN, 3999)).secondsLeft)
        assertEquals(CaptureState.Capture, c.update(0f, 4000))
        assertEquals(CaptureState.Cooldown(0f), c.update(1f, 4100))
        assertTrue(c.isSessionActive)
        c.onCaptured(4200)
        assertFalse(c.isSessionActive)
        assertEquals(CaptureState.Cooldown(0f), c.state)
    }

    @Test
    fun countdownProgressAndSecondsLeft() {
        val c = CaptureCoach(settings)
        c.startTimer(2, 0)
        var last = -1f
        for (t in 0L until 2000L step 100L) {
            val s = countdown(c.update(0f, t))
            assertTrue(s.progress >= last)
            assertEquals(t / 2000f, s.progress, 1e-4f)
            assertEquals(((2000 - t) + 999) / 1000, s.secondsLeft.toLong())
            last = s.progress
        }
        assertEquals(CaptureState.Capture, c.update(0f, 2000))
    }

    /** Drives [coach] at ~30 Hz for [durationMs], confirming each Capture after [confirmDelayMs]; returns capture times. */
    private fun drive(coach: CaptureCoach, from: Long, durationMs: Long, score: Float = 0f, confirmDelayMs: Long? = 100L): List<Long> {
        val captures = mutableListOf<Long>()
        var pendingConfirm: Long? = null
        var t = from
        while (t < from + durationMs) {
            if (pendingConfirm != null && t >= pendingConfirm) {
                coach.onCaptured(t)
                pendingConfirm = null
            }
            if (coach.update(score, t) == CaptureState.Capture) {
                captures += t
                if (confirmDelayMs != null) pendingConfirm = t + confirmDelayMs
            }
            t += 33L
        }
        return captures
    }

    @Test
    fun photoshootOfThreeShots() {
        val c = CaptureCoach(settings)
        c.startTimer(seconds = 2, nowMillis = 0, shots = 3, intervalSeconds = 1)
        val seen = mutableListOf<Int>()
        var t = 0L
        var captures = 0
        while (t < 8000) {
            val s = c.update(0f, t)
            if (s is CaptureState.Countdown) {
                assertTrue(s.manual)
                assertEquals(3, s.totalShots)
                if (seen.lastOrNull() != s.shot) seen += s.shot
            }
            if (s == CaptureState.Capture) {
                captures++
                c.onCaptured(t + 50)
            }
            t += 33L
        }
        assertEquals(3, captures)
        assertEquals(listOf(1, 2, 3), seen)
        assertFalse(c.isSessionActive)
    }

    @Test
    fun photoshootTimingAndShotNumbers() {
        val c = CaptureCoach(settings)
        c.startTimer(seconds = 2, nowMillis = 0, shots = 3, intervalSeconds = 1)
        assertEquals(CaptureState.Capture, c.update(0f, 2000))
        c.onCaptured(2100)
        val second = countdown(c.state)
        assertEquals(2, second.shot)
        assertEquals(1, second.secondsLeft)
        assertEquals(CaptureState.Capture, c.update(0f, 3100))
        c.onCaptured(3200)
        assertEquals(3, countdown(c.update(0f, 3300)).shot)
        assertEquals(CaptureState.Capture, c.update(0f, 4200))
        c.onCaptured(4300)
        assertFalse(c.isSessionActive)
        assertTrue(c.state is CaptureState.Cooldown)
    }

    @Test
    fun unlimitedPhotoshootUntilCancelled() {
        val c = CaptureCoach(settings)
        c.startTimer(seconds = 1, nowMillis = 0, shots = 0, intervalSeconds = 1)
        assertEquals(0, countdown(c.state).totalShots)
        val captures = drive(c, 0, 10_000)
        assertTrue("captures ${captures.size}", captures.size >= 7)
        assertTrue(c.isSessionActive)
        c.cancel()
        assertFalse(c.isSessionActive)
        assertEquals(CaptureState.Idle, c.state)
        assertTrue(drive(c, 10_000, 5000).isEmpty())
    }

    @Test
    fun neverCapturesTwiceWithoutConfirmation() {
        val auto = CaptureCoach(settings)
        assertEquals(1, drive(auto, 0, 20_000, score = 1f, confirmDelayMs = null).size)
        val timer = CaptureCoach(settings)
        timer.startTimer(1, 0, shots = 5, intervalSeconds = 1)
        assertEquals(1, drive(timer, 0, 20_000, confirmDelayMs = null).size)
    }

    @Test
    fun repeatedAutoCapturesRespectTheCooldown() {
        val c = CaptureCoach(settings)
        val captures = drive(c, 0, 20_000, score = 1f)
        assertTrue(captures.size >= 2)
        captures.zipWithNext { a, b ->
            assertTrue("gap ${b - a}", b - a >= settings.cooldownMillis + settings.holdMillis + settings.countdownSeconds * 1000L)
        }
    }

    @Test
    fun cancelAndResetReturnToIdle() {
        val c = CaptureCoach(settings)
        c.update(0.9f, 0)
        countdown(c.update(0.9f, 700))
        c.cancel()
        assertEquals(CaptureState.Idle, c.state)

        c.startTimer(5, 1000, shots = 3)
        c.reset()
        assertFalse(c.isSessionActive)
        assertEquals(CaptureState.Idle, c.state)
        assertEquals(CaptureState.Idle, c.update(0.1f, 7000))
    }

    @Test
    fun timerOverridesAPendingAutoCapture() {
        val c = CaptureCoach(settings.copy(countdownSeconds = 0))
        c.update(0.9f, 0)
        assertEquals(CaptureState.Capture, c.update(0.9f, 700))
        c.startTimer(1, 800)
        // The late confirmation of the auto shot must not count as the timer's photo.
        c.onCaptured(850)
        assertTrue(c.isSessionActive)
        assertTrue(c.update(0f, 900) is CaptureState.Countdown)
        assertEquals(CaptureState.Capture, c.update(0f, 1800))
    }

    @Test
    fun manualShutterConfirmationPausesAutoCapture() {
        val c = CaptureCoach(settings)
        c.onCaptured(0)
        assertEquals(CaptureState.Cooldown(0f), c.state)
        assertTrue(c.update(1f, 1000) is CaptureState.Cooldown)
        assertTrue(c.update(1f, 2600) is CaptureState.Holding)
    }

    @Test
    fun zeroSecondTimerShootsOnNextUpdate() {
        val c = CaptureCoach(settings)
        c.startTimer(0, 0, shots = 2, intervalSeconds = 0)
        assertEquals(CaptureState.Capture, c.update(0f, 0))
        c.onCaptured(10)
        assertEquals(CaptureState.Capture, c.update(0f, 20))
        c.onCaptured(30)
        assertFalse(c.isSessionActive)
    }
}
