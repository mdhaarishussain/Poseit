package com.poseit.app.ui.camera

import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintStabilizerTest {

    private val raiseArm = CoachHint("Raise your left arm", HintKind.POSE, 0.6f)
    private val stepLeft = CoachHint("Step left", HintKind.FRAMING, 0.5f)
    private val perfect = CoachHint("Perfect!", HintKind.PERFECT, 0f)
    private val nobody = CoachHint("Step into the frame", HintKind.NO_PERSON, 1f)

    @Test
    fun `hint is shown only after being top for 400 ms`() {
        val s = HintStabilizer()
        assertNull(s.update(raiseArm, 0))
        assertNull(s.update(raiseArm, 399))
        assertEquals(raiseArm, s.update(raiseArm, 400))
    }

    @Test
    fun `flickering hints never reach the screen`() {
        val s = HintStabilizer()
        for (t in 0L..2000L step 100L) {
            val shown = s.update(if ((t / 100) % 2 == 0L) raiseArm else stepLeft, t)
            assertNull("t=$t", shown)
        }
    }

    @Test
    fun `shown hint stays at least 1500 ms`() {
        val s = HintStabilizer()
        s.update(raiseArm, 0)
        s.update(raiseArm, 400) // shown at 400
        s.update(stepLeft, 500)
        assertEquals(raiseArm, s.update(stepLeft, 1000)) // stable for 500 ms, but raiseArm shown only 600 ms
        assertEquals(raiseArm, s.update(stepLeft, 1899))
        assertEquals(stepLeft, s.update(stepLeft, 1900))
    }

    @Test
    fun `perfect and no-person replace a hint early`() {
        val s = HintStabilizer()
        s.update(raiseArm, 0)
        s.update(raiseArm, 400)
        s.update(perfect, 500)
        assertEquals(perfect, s.update(perfect, 900))

        // Leaving PERFECT is prompt too.
        s.update(nobody, 1000)
        assertEquals(nobody, s.update(nobody, 1400))
    }

    @Test
    fun `same hint with new severity keeps its timers and refreshes content`() {
        val s = HintStabilizer()
        s.update(raiseArm, 0)
        s.update(raiseArm, 400)
        val milder = raiseArm.copy(severity = 0.2f)
        assertEquals(milder, s.update(milder, 450))
        s.update(stepLeft, 1000)
        // raiseArm was shown at 400 (not re-shown at 450), so it may be replaced at 1900.
        assertEquals(stepLeft, s.update(stepLeft, 1900))
    }

    @Test
    fun `no hint clears the banner after the minimum times`() {
        val s = HintStabilizer()
        s.update(raiseArm, 0)
        s.update(raiseArm, 400)
        s.update(null, 1000)
        assertEquals(raiseArm, s.update(null, 1500))
        assertNull(s.update(null, 1900))
    }

    @Test
    fun `reset forgets everything`() {
        val s = HintStabilizer()
        s.update(raiseArm, 0)
        s.update(raiseArm, 400)
        s.reset()
        assertNull(s.displayed)
        assertNull(s.update(stepLeft, 500))
        assertEquals(stepLeft, s.update(stepLeft, 900))
    }
}
