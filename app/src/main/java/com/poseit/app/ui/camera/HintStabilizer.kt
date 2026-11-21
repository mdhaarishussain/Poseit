package com.poseit.app.ui.camera

import com.poseit.app.pose.match.CoachHint
import com.poseit.app.pose.match.HintKind

/**
 * Debounces coaching hints so the banner and the voice do not flicker between hints from frame to frame.
 *
 * - A hint must be the top hint for at least [minTopMillis] before it is shown.
 * - A shown hint stays for at least [minShowMillis], unless it or its replacement is urgent
 *   ([HintKind.PERFECT] or [HintKind.NO_PERSON]): reaching the pose or leaving the frame shows up promptly.
 * - Hints are identified by kind, text and figure, so an updated severity does not restart the timers.
 *
 * Not thread-safe: call from one thread (the camera pipeline).
 */
class HintStabilizer(
    private val minTopMillis: Long = 400L,
    private val minShowMillis: Long = 1500L,
) {
    private var hasCandidate = false
    private var candidateKey: Key? = null
    private var candidateSince = 0L
    private var displayedSince = 0L

    /** The hint currently shown, or null. */
    var displayed: CoachHint? = null
        private set

    /** Feeds the current top hint (null = no hint) and returns the hint to display. */
    fun update(top: CoachHint?, nowMillis: Long): CoachHint? {
        val key = top?.key()
        if (!hasCandidate || key != candidateKey) {
            hasCandidate = true
            candidateKey = key
            candidateSince = nowMillis
        }
        val shown = displayed
        if (key == shown?.key()) {
            // Same hint as on screen: keep its timers, refresh its contents (severity, speech).
            displayed = top
            return displayed
        }
        if (nowMillis - candidateSince < minTopMillis) return shown

        val canReplace = shown == null ||
            nowMillis - displayedSince >= minShowMillis ||
            shown.kind.isUrgent ||
            top?.kind?.isUrgent == true
        if (canReplace) {
            displayed = top
            displayedSince = nowMillis
        }
        return displayed
    }

    fun reset() {
        hasCandidate = false
        candidateKey = null
        candidateSince = 0L
        displayed = null
        displayedSince = 0L
    }

    private data class Key(val kind: HintKind, val text: String, val figureIndex: Int?)

    private fun CoachHint.key() = Key(kind, text, figureIndex)

    private val HintKind.isUrgent: Boolean get() = this == HintKind.PERFECT || this == HintKind.NO_PERSON
}
