package com.poseit.app.ui.camera

import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton

/**
 * Hands-free trigger: fires once when somebody keeps a wrist above their nose for [holdMillis].
 * After firing, the hand has to come down again before the next trigger (holding the hand up does not
 * start timer after timer).
 *
 * Not thread-safe: call from one thread (the camera pipeline).
 */
class HandRaiseDetector(
    private val holdMillis: Long = 800L,
    private val minVisibility: Float = 0.5f,
) {
    private var raisedSince = NOT_RAISED
    private var armed = true

    /**
     * @param people detections in scene space
     * @param sceneAspect scene width / height
     * @param allowTrigger false while the trigger must not fire (e.g. a timer is already running); the
     *   detector then only watches for the hand coming down, and a new full hold is needed afterwards
     * @return true exactly once per completed hold
     */
    fun update(people: List<Skeleton>, sceneAspect: Float, nowMillis: Long, allowTrigger: Boolean = true): Boolean {
        val raised = people.any { isHandRaised(it, sceneAspect, minVisibility) }
        if (!raised) {
            raisedSince = NOT_RAISED
            armed = true
            return false
        }
        if (!allowTrigger || !armed) {
            raisedSince = NOT_RAISED
            return false
        }
        if (raisedSince == NOT_RAISED) raisedSince = nowMillis
        if (nowMillis - raisedSince >= holdMillis) {
            armed = false
            raisedSince = NOT_RAISED
            return true
        }
        return false
    }

    fun reset() {
        raisedSince = NOT_RAISED
        armed = true
    }

    companion object {
        private const val NOT_RAISED = Long.MIN_VALUE

        /** Fraction of the body unit (torso length) the wrist must be above the nose, to ignore jitter. */
        private const val MARGIN_UNITS = 0.1f

        /** True when a visible wrist is clearly above the visible nose. */
        fun isHandRaised(person: Skeleton, aspect: Float, minVisibility: Float = 0.5f, marginUnits: Float = MARGIN_UNITS): Boolean {
            if (!person.isVisible(Joint.NOSE, minVisibility)) return false
            val nose = person[Joint.NOSE] ?: return false
            val margin = person.estimatedUnit(aspect) * marginUnits
            return listOf(Joint.LEFT_WRIST, Joint.RIGHT_WRIST).any { wrist ->
                val p = person[wrist]
                p != null && person.isVisible(wrist, minVisibility) && p.y < nose.y - margin
            }
        }

        /**
         * Template wrists up to this many body units BELOW the nose still count as raised: a person reproducing a
         * hand-at-head-height pose easily ends up [MARGIN_UNITS] above the nose and would trigger the timer.
         */
        private const val TEMPLATE_MARGIN_UNITS = -0.25f

        /** True when the pose itself has a hand above or near the head (the trigger would fire while posing). */
        fun templateRaisesHand(template: PoseTemplate): Boolean =
            template.figures.any { isHandRaised(it, template.frameAspect, minVisibility = 0f, marginUnits = TEMPLATE_MARGIN_UNITS) }
    }
}
