package com.poseit.app.pose.library

import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseTemplate

/**
 * The built-in pose library. Every pose is ORIGINAL, authored with the angle DSL in
 * [com.poseit.app.pose.dsl.pose]. Each category lives in its own file.
 */
object BuiltInPoses {
    val all: List<PoseTemplate> by lazy {
        val list = StandingPoses.all + SittingPoses.all + LeaningPoses.all + WalkingPoses.all + FloorPoses.all +
            PortraitPoses.all + SelfiePoses.all + ProfessionalPoses.all + FunPoses.all +
            CouplePoses.all + GroupPoses.all
        val dupes = list.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupes.isEmpty()) { "Duplicate pose ids: $dupes" }
        list
    }

    fun byId(id: String): PoseTemplate? = all.firstOrNull { it.id == id }

    fun byCategory(category: PoseCategory): List<PoseTemplate> = all.filter { it.category == category }
}
