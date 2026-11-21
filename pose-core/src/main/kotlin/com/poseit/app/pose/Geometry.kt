package com.poseit.app.pose

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A 2D point / vector. Image convention everywhere in this app: x grows to the right,
 * y grows DOWN.
 */
@Serializable
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    operator fun div(s: Float) = Vec2(x / s, y / s)
    operator fun unaryMinus() = Vec2(-x, -y)

    val length: Float get() = sqrt(x * x + y * y)
    fun normalized(): Vec2 = length.let { if (it < 1e-6f) Vec2(0f, 0f) else Vec2(x / it, y / it) }
    fun dot(o: Vec2) = x * o.x + y * o.y
    fun distanceTo(o: Vec2) = (this - o).length

    /** Rotate clockwise *as seen on screen* (because y points down) by [degrees]. */
    fun rotated(degrees: Float): Vec2 {
        val r = degrees.toRad()
        val c = cos(r)
        val s = sin(r)
        return Vec2(x * c - y * s, x * s + y * c)
    }

    companion object {
        val ZERO = Vec2(0f, 0f)

        /**
         * Unit vector for a "limb angle" in the app-wide convention:
         *   0° = straight DOWN, +90° = toward image RIGHT, -90° = toward image LEFT, ±180° = straight UP.
         */
        fun fromLimbAngle(degrees: Float): Vec2 {
            val r = degrees.toRad()
            return Vec2(sin(r), cos(r))
        }

        fun lerp(a: Vec2, b: Vec2, t: Float) = Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        fun mid(a: Vec2, b: Vec2) = Vec2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    }
}

fun Float.toRad(): Float = (this * PI / 180.0).toFloat()
fun Float.toDeg(): Float = (this * 180.0 / PI).toFloat()

/** Limb angle (see [Vec2.fromLimbAngle]) of the direction from [from] to [to]. Range (-180, 180]. */
fun limbAngle(from: Vec2, to: Vec2): Float {
    val d = to - from
    return atan2(d.x, d.y).toDeg()
}

/** Smallest signed difference a - b in degrees, in (-180, 180]. */
fun angleDiff(a: Float, b: Float): Float {
    var d = (a - b) % 360f
    if (d > 180f) d -= 360f
    if (d <= -180f) d += 360f
    return d
}

fun absAngleDiff(a: Float, b: Float): Float = abs(angleDiff(a, b))

/** Interior angle at [joint] formed by [a]-joint-[b], in degrees [0, 180]. 180 = straight limb. */
fun interiorAngle(a: Vec2, joint: Vec2, b: Vec2): Float {
    val u = (a - joint).normalized()
    val v = (b - joint).normalized()
    val dot = (u.dot(v)).coerceIn(-1f, 1f)
    return kotlin.math.acos(dot).toDeg()
}

/** Axis-aligned rectangle in whatever space the caller uses. */
@Serializable
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val center get() = Vec2((left + right) / 2f, (top + bottom) / 2f)
    fun contains(p: Vec2, margin: Float = 0f) =
        p.x >= left - margin && p.x <= right + margin && p.y >= top - margin && p.y <= bottom + margin

    companion object {
        fun around(points: Iterable<Vec2>): Box? {
            var l = Float.MAX_VALUE
            var t = Float.MAX_VALUE
            var r = -Float.MAX_VALUE
            var b = -Float.MAX_VALUE
            var any = false
            for (p in points) {
                any = true
                if (p.x < l) l = p.x
                if (p.y < t) t = p.y
                if (p.x > r) r = p.x
                if (p.y > b) b = p.y
            }
            return if (any) Box(l, t, r, b) else null
        }
    }
}
