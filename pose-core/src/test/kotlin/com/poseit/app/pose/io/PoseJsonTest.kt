package com.poseit.app.pose.io

import com.poseit.app.pose.CameraHeight
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTag
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.without
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseJsonTest {

    private val dslPose = pose("custom_1", "Hand on hip", PoseCategory.CUSTOM) {
        description = "Mine"
        tips("Relax", "Breathe")
        tags(PoseTag.CASUAL, PoseTag.CITY)
        camera(CameraHeight.WAIST, ShotType.FULL_BODY, note = "Low angle")
        figure { leftArm(40f, -50f) }
    }

    private val photoPose = PoseTemplate(
        id = "photo_2",
        name = "From photo",
        category = PoseCategory.COUPLE,
        figures = listOf(
            Skeleton(mapOf(Joint.NOSE to Vec2(0.3f, 0.2f), Joint.LEFT_SHOULDER to Vec2(0.35f, 0.3f)), mapOf(Joint.NOSE to 0.7f), 0.18f),
            Skeleton(mapOf(Joint.NOSE to Vec2(0.7f, 0.21f))),
        ),
        frameAspect = 4f / 3f,
        source = PoseSource.FROM_PHOTO,
        createdAt = 1_700_000_000_123L,
        referenceImage = "/data/user/0/com.poseit.app/files/ref.jpg",
        outlineImage = null,
        difficulty = 2,
    )

    @Test
    fun roundTrip() {
        val list = listOf(dslPose, photoPose)
        val json = PoseJson.encode(list)
        assertTrue(json.contains("\"version\":1"))
        assertTrue(json.contains("\"poses\""))
        assertTrue("explicit nulls are omitted", !json.contains("outlineImage"))
        assertEquals(list, PoseJson.decode(json))
        assertEquals(emptyList<PoseTemplate>(), PoseJson.decode(PoseJson.encode(emptyList())))
    }

    @Test
    fun garbageDecodesToEmpty() {
        listOf("", "   ", "not json", "{", "[1,2", "null", "42", "\"poses\"", "{}", "{\"poses\": 5}", "{\"version\":1}", "[[[[[[").forEach {
            assertEquals("input: $it", emptyList<PoseTemplate>(), PoseJson.decode(it))
        }
        val deep = "[".repeat(100_000) + "]".repeat(100_000)
        assertEquals(emptyList<PoseTemplate>(), PoseJson.decode(deep))
    }

    @Test
    fun corruptEntriesAreSkippedIndividually() {
        val root = Json.parseToJsonElement(PoseJson.encode(listOf(dslPose, photoPose))).jsonObject
        val poses = root.getValue("poses").jsonArray
        val badCategory = JsonObject(poses[1].jsonObject + ("category" to JsonPrimitive("NOT_A_CATEGORY")))
        val tampered = JsonObject(
            root + ("poses" to JsonArray(listOf(poses[0], JsonPrimitive(3), JsonObject(mapOf("name" to JsonPrimitive("x"))), badCategory))),
        )
        assertEquals(listOf(dslPose), PoseJson.decode(tampered.toString()))
    }

    @Test
    fun unknownKeysAndBareArraysAreAccepted() {
        val root = Json.parseToJsonElement(PoseJson.encode(listOf(photoPose))).jsonObject
        val pose = root.getValue("poses").jsonArray[0].jsonObject
        val future = JsonObject(
            mapOf(
                "version" to JsonPrimitive(7),
                "exportedBy" to JsonPrimitive("Poseit 9"),
                "poses" to JsonArray(listOf(JsonObject(pose + ("sparkles" to JsonPrimitive(true))))),
            ),
        )
        assertEquals(listOf(photoPose), PoseJson.decode(future.toString()))
        assertEquals(listOf(photoPose), PoseJson.decode(JsonArray(listOf(pose)).toString()))
    }

    @Test
    fun invalidValuesAreSanitised() {
        val broken = photoPose.copy(
            figures = listOf(
                Skeleton(
                    mapOf(Joint.NOSE to Vec2(Float.NaN, 0.2f), Joint.LEFT_HIP to Vec2(0.4f, 0.6f)),
                    mapOf(Joint.LEFT_HIP to Float.POSITIVE_INFINITY),
                    Float.NaN,
                ),
                Skeleton(emptyMap()),
            ),
            frameAspect = Float.NaN,
        )
        val decoded = PoseJson.decode(PoseJson.encode(listOf(broken))).single()
        assertEquals(1, decoded.figures.size)
        assertEquals(setOf(Joint.LEFT_HIP), decoded.figures.single().points.keys)
        assertEquals(0f, decoded.figures.single().unit)
        assertEquals(3f / 4f, decoded.frameAspect)

        val noFigures = photoPose.copy(id = "empty", figures = emptyList())
        assertEquals(listOf(photoPose), PoseJson.decode(PoseJson.encode(listOf(noFigures, photoPose, photoPose.copy(name = "dupe")))))
    }

    // ---------------------------------------------------------------------------------------------------------------
    // CustomPoseFactory
    // ---------------------------------------------------------------------------------------------------------------

    private fun detectedPerson(cx: Float, legs: Boolean = true): Skeleton {
        val pts = linkedMapOf(
            Joint.NOSE to Vec2(cx, 0.2f),
            Joint.LEFT_SHOULDER to Vec2(cx + 0.08f, 0.32f),
            Joint.RIGHT_SHOULDER to Vec2(cx - 0.08f, 0.32f),
            Joint.LEFT_ELBOW to Vec2(cx + 0.1f, 0.45f),
            Joint.LEFT_HIP to Vec2(cx + 0.04f, 0.6f),
            Joint.RIGHT_HIP to Vec2(cx - 0.04f, 0.6f),
            Joint.LEFT_KNEE to Vec2(cx + 0.05f, 0.75f),
            Joint.LEFT_ANKLE to Vec2(cx + 0.05f, 0.9f),
            Joint.RIGHT_WRIST to Vec2(cx - 0.12f, 0.55f),
        )
        val vis = pts.keys.associateWith { if (it == Joint.RIGHT_WRIST) 0.3f else 0.9f }
        val s = Skeleton(pts, vis)
        return if (legs) s else s.without(Joint.LEFT_KNEE, Joint.LEFT_ANKLE)
    }

    @Test
    fun customPoseFromDetection() {
        val t = CustomPoseFactory.fromDetection(
            id = "c1",
            name = "Beach day",
            category = PoseCategory.CUSTOM,
            people = listOf(detectedPerson(0.7f), detectedPerson(0.3f)),
            imageAspect = 4f / 3f,
            referenceImage = "/files/ref.jpg",
            createdAt = 42L,
        )
        assertEquals("c1", t.id)
        assertEquals("Beach day", t.name)
        assertEquals(PoseCategory.CUSTOM, t.category)
        assertEquals(PoseSource.FROM_PHOTO, t.source)
        assertEquals(4f / 3f, t.frameAspect)
        assertEquals("/files/ref.jpg", t.referenceImage)
        assertEquals(42L, t.createdAt)
        assertEquals(2, t.figures.size)
        // Left to right, low-visibility joints dropped, visibility cleared, unit estimated.
        assertTrue(t.figures[0].points.getValue(Joint.NOSE).x < t.figures[1].points.getValue(Joint.NOSE).x)
        t.figures.forEach { f ->
            assertTrue(Joint.RIGHT_WRIST !in f.points)
            assertTrue(f.visibility.isEmpty())
            assertTrue(f.unit > 0f)
            assertEquals(Skeleton(f.points).estimatedUnit(4f / 3f), f.unit)
        }
        assertTrue("Match the outline from your photo" in t.tips)
        assertEquals(ShotType.FULL_BODY, t.camera.shot)
        assertTrue(t.camera.landscape)
        // It survives persistence.
        assertEquals(listOf(t), PoseJson.decode(PoseJson.encode(listOf(t))))
    }

    @Test
    fun customPoseShotTypeAndEdgeCases() {
        val upper = CustomPoseFactory.fromDetection(
            "c2", "Portrait", PoseCategory.PORTRAIT, listOf(detectedPerson(0.5f, legs = false)), 0.75f, null, 0L, PoseSource.DRAWN,
        )
        assertEquals(ShotType.HALF_BODY, upper.camera.shot)
        assertEquals(PoseSource.DRAWN, upper.source)
        assertEquals(PoseCategory.PORTRAIT, upper.category)

        val nobody = CustomPoseFactory.fromDetection("c3", "Empty", PoseCategory.CUSTOM, emptyList(), Float.NaN, null, 0L)
        assertTrue(nobody.figures.isEmpty())
        assertEquals(3f / 4f, nobody.frameAspect)

        val faint = Skeleton(mapOf(Joint.NOSE to Vec2(0.5f, 0.5f), Joint.LEFT_HIP to Vec2(0.5f, 0.7f)), mapOf(Joint.NOSE to 0.1f))
        assertTrue(CustomPoseFactory.fromDetection("c4", "Faint", PoseCategory.CUSTOM, listOf(faint), 0.75f, null, 0L).figures.isEmpty())
    }
}
