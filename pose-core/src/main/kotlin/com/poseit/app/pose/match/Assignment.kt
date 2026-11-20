package com.poseit.app.pose.match

/**
 * Assigns detected people to template figures by minimising the summed pair cost.
 *
 * Every figure either takes a distinct detection or stays unassigned at [UNASSIGNED_COST], which is larger than any
 * pair cost, so as many figures as possible get a person. Small problems (the normal case: at most a handful of
 * people) are solved exactly by exhaustive search; larger ones fall back to a greedy cheapest-pair-first pass.
 */
internal object Assignment {
    const val UNASSIGNED_COST = 10f
    private const val EXACT_LIMIT = 6

    /**
     * @param cost cost[i][j] of giving detection j to figure i (finite values below [UNASSIGNED_COST])
     * @return for each figure the assigned detection index, or -1
     */
    fun solve(cost: Array<FloatArray>, detections: Int): IntArray {
        val figures = cost.size
        if (figures == 0) return IntArray(0)
        if (detections <= 0) return IntArray(figures) { -1 }
        return if (figures <= EXACT_LIMIT && detections <= EXACT_LIMIT) exact(cost, detections) else greedy(cost, detections)
    }

    private fun exact(cost: Array<FloatArray>, detections: Int): IntArray {
        val n = cost.size
        val current = IntArray(n) { -1 }
        val used = BooleanArray(detections)
        var best = IntArray(n) { -1 }
        var bestCost = Float.POSITIVE_INFINITY

        fun search(i: Int, acc: Float) {
            if (acc >= bestCost) return
            if (i == n) {
                bestCost = acc
                best = current.copyOf()
                return
            }
            for (j in 0 until detections) {
                if (used[j]) continue
                used[j] = true
                current[i] = j
                search(i + 1, acc + safe(cost[i][j]))
                used[j] = false
            }
            current[i] = -1
            search(i + 1, acc + UNASSIGNED_COST)
        }
        search(0, 0f)
        return best
    }

    private fun greedy(cost: Array<FloatArray>, detections: Int): IntArray {
        val result = IntArray(cost.size) { -1 }
        val used = BooleanArray(detections)
        val pairs = buildList {
            for (i in cost.indices) for (j in 0 until detections) add(Triple(i, j, safe(cost[i][j])))
        }.sortedBy { it.third }
        for ((i, j, _) in pairs) {
            if (result[i] == -1 && !used[j]) {
                result[i] = j
                used[j] = true
            }
        }
        return result
    }

    private fun safe(c: Float): Float = if (c.isFinite()) c.coerceIn(0f, UNASSIGNED_COST - 1f) else UNASSIGNED_COST - 1f
}
