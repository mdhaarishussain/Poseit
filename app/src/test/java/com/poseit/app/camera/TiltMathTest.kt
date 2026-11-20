package com.poseit.app.camera

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TiltMathTest {

    private val g = 9.81f
    private fun rad(deg: Float) = Math.toRadians(deg.toDouble()).toFloat()

    @Test
    fun uprightPhoneIsLevel() {
        val tilt = TiltMath.fromGravity(0f, g, 0f)!!
        assertEquals(0f, tilt.rollDegrees, 0.01f)
        assertEquals(0f, tilt.pitchDegrees, 0.01f)
    }

    @Test
    fun clockwiseRotationIsPositiveRoll() {
        // Turned clockwise by 30°: the right edge goes down, the reading turns counter-clockwise in device axes.
        val cw = TiltMath.fromGravity(-sin(rad(30f)) * g, cos(rad(30f)) * g, 0f)!!
        assertEquals(30f, cw.rollDegrees, 0.01f)
        val ccw = TiltMath.fromGravity(sin(rad(30f)) * g, cos(rad(30f)) * g, 0f)!!
        assertEquals(-30f, ccw.rollDegrees, 0.01f)
    }

    @Test
    fun topTiltedAwayIsPositivePitch() {
        val away = TiltMath.fromGravity(0f, cos(rad(20f)) * g, sin(rad(20f)) * g)!!
        assertEquals(20f, away.pitchDegrees, 0.01f)
        assertEquals(0f, away.rollDegrees, 0.01f)
        val toward = TiltMath.fromGravity(0f, cos(rad(20f)) * g, -sin(rad(20f)) * g)!!
        assertEquals(-20f, toward.pitchDegrees, 0.01f)
    }

    @Test
    fun pitchIsIndependentOfRoll() {
        val roll = rad(25f)
        val pitch = rad(15f)
        val horizontal = cos(pitch) * g
        val tilt = TiltMath.fromGravity(-sin(roll) * horizontal, cos(roll) * horizontal, sin(pitch) * g)!!
        assertEquals(25f, tilt.rollDegrees, 0.01f)
        assertEquals(15f, tilt.pitchDegrees, 0.01f)
    }

    @Test
    fun zeroVectorIsIgnored() {
        assertNull(TiltMath.fromGravity(0f, 0f, 0f))
    }

    @Test
    fun displayAxesFollowTheDisplayRotation() {
        assertEquals(1f to 2f, TiltMath.toDisplayAxes(1f, 2f, Surface.ROTATION_0))
        assertEquals(-2f to 1f, TiltMath.toDisplayAxes(1f, 2f, Surface.ROTATION_90))
        assertEquals(-1f to -2f, TiltMath.toDisplayAxes(1f, 2f, Surface.ROTATION_180))
        assertEquals(2f to -1f, TiltMath.toDisplayAxes(1f, 2f, Surface.ROTATION_270))
    }
}
