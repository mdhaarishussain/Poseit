package com.poseit.app.pose.detect

import com.poseit.app.pose.Joint
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import java.util.EnumMap
import kotlin.math.exp

/**
 * Removes landmark jitter from live detections with a One Euro filter per joint coordinate
 * (Casiez et al. 2012: low lag when moving fast, strong smoothing when still).
 *
 * People are tracked across frames by nearest hip/shoulder centre; a person that disappears for more than
 * ~500 ms loses its filter state. Visibility is smoothed with a simple exponential filter.
 *
 * Not thread-safe: call it from one thread (the analysis thread) only.
 *
 * @param minCutoff minimum cutoff frequency (Hz) – lower = smoother when still
 * @param beta speed coefficient – higher = less lag when moving
 * @param dCutoff cutoff for the derivative
 * @param speedScale landmark speeds (normalized units per second) are multiplied by this before [beta] is applied,
 *   i.e. [beta] is expressed per "virtual pixel" of a ~1000 px frame. With the defaults a still joint is filtered
 *   at ~1.5-2.5 Hz while a limb sweeping across the frame opens the cutoff to tens of Hz.
 */
class LandmarkSmoother(
    val minCutoff: Float = 1.2f,
    val beta: Float = 0.03f,
    val dCutoff: Float = 1.0f,
    val speedScale: Float = 1000f,
) {
    private val tracks = mutableListOf<Track>()
    private var lastTimestampMs: Long? = null

    /** @param people detections of one frame (scene space); @param timestampMs frame time; returns smoothed copies */
    fun smooth(people: List<Skeleton>, timestampMs: Long): List<Skeleton> {
        val last = lastTimestampMs
        // A clock that jumps backwards (camera rebind, new session) invalidates all filter state.
        if (last != null && timestampMs < last) reset()
        lastTimestampMs = timestampMs

        tracks.removeAll { timestampMs - it.lastSeenMs > TRACK_TIMEOUT_MS }
        if (people.isEmpty()) return emptyList()

        val centres = people.map { torsoCentre(it) }
        val assigned = arrayOfNulls<Track>(people.size)
        val free = tracks.toMutableList()

        // Greedy nearest-centre association, closest pairs first.
        val pairs = buildList {
            centres.forEachIndexed { i, c ->
                if (c != null) free.forEach { t -> add(Triple(i, t, c.distanceTo(t.centre))) }
            }
        }.filter { it.third <= MAX_MATCH_DISTANCE }.sortedBy { it.third }
        for ((i, track, _) in pairs) {
            if (assigned[i] == null && track in free) {
                assigned[i] = track
                free.remove(track)
            }
        }

        return people.mapIndexed { i, person ->
            val centre = centres[i] ?: return@mapIndexed person // nothing usable to smooth or track
            val track = assigned[i] ?: Track(centre, timestampMs).also { tracks += it }
            track.apply(person, centre, timestampMs)
        }
    }

    fun reset() {
        tracks.clear()
        lastTimestampMs = null
    }

    private inner class Track(var centre: Vec2, var lastSeenMs: Long) {
        private val joints = EnumMap<Joint, JointState>(Joint::class.java)

        fun apply(person: Skeleton, rawCentre: Vec2, nowMs: Long): Skeleton {
            val points = LinkedHashMap<Joint, Vec2>()
            val visibility = LinkedHashMap<Joint, Float>()
            for ((joint, p) in person.points) {
                if (!p.x.isFinite() || !p.y.isFinite()) continue
                val state = joints.getOrPut(joint) { JointState() }
                val smoothed = state.update(p, person.visibility[joint], nowMs)
                points[joint] = smoothed
                if (person.visibility.containsKey(joint)) visibility[joint] = state.visibility
            }
            centre = rawCentre
            lastSeenMs = nowMs
            return Skeleton(points = points, visibility = visibility, unit = person.unit)
        }
    }

    private inner class JointState {
        private val x = OneEuroFilter(minCutoff, beta, dCutoff, speedScale)
        private val y = OneEuroFilter(minCutoff, beta, dCutoff, speedScale)
        private var lastMs = Long.MIN_VALUE
        var visibility = 0f
            private set

        fun update(p: Vec2, rawVisibility: Float?, nowMs: Long): Vec2 {
            val v = rawVisibility?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f
            if (x.isInitialized && nowMs - lastMs > TRACK_TIMEOUT_MS) {
                x.reset()
                y.reset()
            }
            if (!x.isInitialized) {
                lastMs = nowMs
                visibility = v
                return Vec2(x.filter(p.x, 0f), y.filter(p.y, 0f))
            }
            val dtSeconds = (nowMs - lastMs) / 1000f
            if (dtSeconds <= 0f) return Vec2(x.value, y.value) // duplicate frame: keep the previous estimate
            lastMs = nowMs
            val a = 1f - exp(-dtSeconds / VISIBILITY_TIME_CONSTANT_S)
            visibility += (v - visibility) * a
            return Vec2(x.filter(p.x, dtSeconds), y.filter(p.y, dtSeconds))
        }
    }

    private companion object {
        const val TRACK_TIMEOUT_MS = 500L
        /** Max centre movement (normalized scene units) between frames for a detection to continue a track. */
        const val MAX_MATCH_DISTANCE = 0.3f
        const val VISIBILITY_TIME_CONSTANT_S = 0.12f

        private val TORSO = listOf(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, Joint.LEFT_HIP, Joint.RIGHT_HIP)

        /** Mean of the available shoulders/hips, else of all points; null for an empty skeleton. */
        fun torsoCentre(s: Skeleton): Vec2? {
            val torso = TORSO.mapNotNull { j -> s[j]?.takeIf { it.x.isFinite() && it.y.isFinite() } }
            val pts = torso.ifEmpty { s.points.values.filter { it.x.isFinite() && it.y.isFinite() } }
            if (pts.isEmpty()) return null
            return Vec2(pts.sumOf { it.x.toDouble() }.toFloat() / pts.size, pts.sumOf { it.y.toDouble() }.toFloat() / pts.size)
        }
    }
}
