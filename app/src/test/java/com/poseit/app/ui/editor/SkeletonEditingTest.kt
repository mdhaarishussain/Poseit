package com.poseit.app.ui.editor

import com.poseit.app.pose.Bone
import com.poseit.app.pose.Box
import com.poseit.app.pose.Joint
import com.poseit.app.pose.ShotType
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SkeletonEditingTest {

    private val aspect = 3f / 4f
    private val figure = SkeletonEditing.neutralFigure(aspect)

    private fun iso(s: Skeleton, j: Joint): Vec2 = s[j]!!.let { Vec2(it.x * aspect, it.y) }

    private fun assertSame(a: Vec2, b: Vec2, eps: Float = 1e-4f) {
        assertTrue("expected $a == $b", abs(a.x - b.x) < eps && abs(a.y - b.y) < eps)
    }

    private fun boneLength(s: Skeleton, bone: Bone) = iso(s, bone.from).distanceTo(iso(s, bone.to))

    private fun dragJoint(s: Skeleton, j: Joint, by: Vec2): Skeleton {
        val start = iso(s, j)
        return SkeletonEditing.drag(s, aspect, DragTarget.JointTarget(j), start, start + by)
    }

    @Test
    fun elbowRotatesAroundShoulderAndCarriesTheForearm() {
        val edited = dragJoint(figure, Joint.LEFT_ELBOW, Vec2(0.15f, -0.1f))
        assertSame(iso(figure, Joint.LEFT_SHOULDER), iso(edited, Joint.LEFT_SHOULDER))
        for (bone in listOf(Bone.LEFT_UPPER_ARM, Bone.LEFT_FOREARM, Bone.LEFT_HAND)) {
            assertEquals(boneLength(figure, bone), boneLength(edited, bone), 1e-4f)
        }
        // The elbow really moved, the rest of the body did not.
        assertTrue(iso(figure, Joint.LEFT_ELBOW).distanceTo(iso(edited, Joint.LEFT_ELBOW)) > 0.02f)
        for (j in Joint.entries - setOf(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, Joint.LEFT_HAND)) {
            assertSame(iso(figure, j), iso(edited, j))
        }
        // Forearm is carried rigidly: the elbow angle is unchanged.
        val before = iso(figure, Joint.LEFT_SHOULDER).distanceTo(iso(figure, Joint.LEFT_WRIST))
        val after = iso(edited, Joint.LEFT_SHOULDER).distanceTo(iso(edited, Joint.LEFT_WRIST))
        assertEquals(before, after, 1e-4f)
    }

    @Test
    fun wristRotatesAroundElbowOnly() {
        val edited = dragJoint(figure, Joint.RIGHT_WRIST, Vec2(-0.1f, -0.12f))
        assertSame(iso(figure, Joint.RIGHT_ELBOW), iso(edited, Joint.RIGHT_ELBOW))
        assertSame(iso(figure, Joint.RIGHT_SHOULDER), iso(edited, Joint.RIGHT_SHOULDER))
        assertEquals(boneLength(figure, Bone.RIGHT_FOREARM), boneLength(edited, Bone.RIGHT_FOREARM), 1e-4f)
        assertEquals(boneLength(figure, Bone.RIGHT_HAND), boneLength(edited, Bone.RIGHT_HAND), 1e-4f)
    }

    @Test
    fun kneeCarriesAnkleAndFoot() {
        val edited = dragJoint(figure, Joint.LEFT_KNEE, Vec2(0.2f, -0.15f))
        assertSame(iso(figure, Joint.LEFT_HIP), iso(edited, Joint.LEFT_HIP))
        for (bone in listOf(Bone.LEFT_THIGH, Bone.LEFT_SHIN, Bone.LEFT_FOOT)) {
            assertEquals(boneLength(figure, bone), boneLength(edited, bone), 1e-4f)
        }
        assertSame(iso(figure, Joint.RIGHT_ANKLE), iso(edited, Joint.RIGHT_ANKLE))
    }

    @Test
    fun headRotatesAroundTheNeck() {
        val neck = Vec2.mid(iso(figure, Joint.LEFT_SHOULDER), iso(figure, Joint.RIGHT_SHOULDER))
        val head = SkeletonEditing.headCenter(figure, aspect)!!
        val edited = SkeletonEditing.drag(figure, aspect, DragTarget.Head, head, head + Vec2(0.08f, 0.02f))
        for (j in Joint.FACE) {
            assertEquals(iso(figure, j).distanceTo(neck), iso(edited, j).distanceTo(neck), 1e-4f)
        }
        assertTrue(iso(figure, Joint.NOSE).distanceTo(iso(edited, Joint.NOSE)) > 0.01f)
        assertSame(iso(figure, Joint.LEFT_SHOULDER), iso(edited, Joint.LEFT_SHOULDER))
        assertSame(iso(figure, Joint.LEFT_WRIST), iso(edited, Joint.LEFT_WRIST))
    }

    @Test
    fun leanRotatesTheUpperBodyAroundTheHips() {
        val hip = Vec2.mid(iso(figure, Joint.LEFT_HIP), iso(figure, Joint.RIGHT_HIP))
        val handle = SkeletonEditing.handles(figure, aspect).first { it.target == DragTarget.Lean }.position
        val edited = SkeletonEditing.drag(figure, aspect, DragTarget.Lean, handle, handle + Vec2(0.1f, 0f))
        for (j in listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP, Joint.LEFT_KNEE, Joint.RIGHT_FOOT)) {
            assertSame(iso(figure, j), iso(edited, j))
        }
        val neckBefore = Vec2.mid(iso(figure, Joint.LEFT_SHOULDER), iso(figure, Joint.RIGHT_SHOULDER))
        val neckAfter = Vec2.mid(iso(edited, Joint.LEFT_SHOULDER), iso(edited, Joint.RIGHT_SHOULDER))
        assertEquals(neckBefore.distanceTo(hip), neckAfter.distanceTo(hip), 1e-4f)
        assertTrue("upper body should lean toward the drag", neckAfter.x > neckBefore.x + 0.02f)
        for (bone in listOf(Bone.LEFT_UPPER_ARM, Bone.RIGHT_FOREARM, Bone.SHOULDERS)) {
            assertEquals(boneLength(figure, bone), boneLength(edited, bone), 1e-4f)
        }
    }

    @Test
    fun shoulderTiltsTheShoulderLineAroundTheNeck() {
        val edited = dragJoint(figure, Joint.LEFT_SHOULDER, Vec2(0f, -0.05f))
        val neckBefore = Vec2.mid(iso(figure, Joint.LEFT_SHOULDER), iso(figure, Joint.RIGHT_SHOULDER))
        val neckAfter = Vec2.mid(iso(edited, Joint.LEFT_SHOULDER), iso(edited, Joint.RIGHT_SHOULDER))
        assertSame(neckBefore, neckAfter)
        assertTrue(iso(edited, Joint.LEFT_SHOULDER).y < iso(figure, Joint.LEFT_SHOULDER).y)
        assertTrue(iso(edited, Joint.RIGHT_SHOULDER).y > iso(figure, Joint.RIGHT_SHOULDER).y)
        assertEquals(boneLength(figure, Bone.LEFT_UPPER_ARM), boneLength(edited, Bone.LEFT_UPPER_ARM), 1e-4f)
        assertSame(iso(figure, Joint.LEFT_HIP), iso(edited, Joint.LEFT_HIP))
    }

    @Test
    fun moveTranslatesEverythingAndKeepsUnit() {
        val start = iso(figure, Joint.NOSE)
        val edited = SkeletonEditing.drag(figure, aspect, DragTarget.Move, start, start + Vec2(0.03f, 0.02f))
        for (j in figure.points.keys) {
            assertEquals(figure[j]!!.x + 0.04f, edited[j]!!.x, 1e-4f) // 0.03 iso / aspect 0.75
            assertEquals(figure[j]!!.y + 0.02f, edited[j]!!.y, 1e-4f)
        }
        assertEquals(figure.unit, edited.unit, 0f)
        assertEquals(figure.visibility, edited.visibility)
    }

    @Test
    fun moveCannotLoseTheFigure() {
        val start = iso(figure, Joint.NOSE)
        val edited = SkeletonEditing.drag(figure, aspect, DragTarget.Move, start, start + Vec2(5f, 5f))
        val box = Box.around(edited.points.values)!!
        assertTrue(box.left <= 0.92f + 1e-4f && box.top <= 0.92f + 1e-4f)
    }

    @Test
    fun missingParentFallsBackToTheNextJointUpTheChain() {
        val noElbow = figure.copy(points = figure.points - Joint.LEFT_ELBOW)
        val edited = dragJoint(noElbow, Joint.LEFT_WRIST, Vec2(0.1f, -0.1f))
        val shoulder = iso(noElbow, Joint.LEFT_SHOULDER)
        assertEquals(iso(noElbow, Joint.LEFT_WRIST).distanceTo(shoulder), iso(edited, Joint.LEFT_WRIST).distanceTo(shoulder), 1e-4f)
    }

    @Test
    fun mirrorKeepsPlaceAndSwapsSides() {
        val off = SkeletonEditing.translateClamped(figure, Vec2(-0.2f, 0f))
        val mirrored = SkeletonEditing.mirrorInPlace(off)
        assertEquals(Box.around(off.points.values)!!.center.x, Box.around(mirrored.points.values)!!.center.x, 1e-4f)
        val cx = Box.around(off.points.values)!!.center.x
        assertEquals(2 * cx - off[Joint.RIGHT_WRIST]!!.x, mirrored[Joint.LEFT_WRIST]!!.x, 1e-4f)
        val twice = SkeletonEditing.mirrorInPlace(mirrored)
        for (j in off.points.keys) assertSame(off[j]!!, twice[j]!!)
    }

    @Test
    fun hitTestFindsHandlesBodyAndMisses() {
        val r = 0.03f
        assertEquals(DragTarget.JointTarget(Joint.LEFT_ELBOW), SkeletonEditing.hitTest(figure, aspect, iso(figure, Joint.LEFT_ELBOW), r))
        // Middle of the torso moves the figure.
        val torso = Vec2.lerp(
            Vec2.mid(iso(figure, Joint.LEFT_SHOULDER), iso(figure, Joint.RIGHT_SHOULDER)),
            Vec2.mid(iso(figure, Joint.LEFT_HIP), iso(figure, Joint.RIGHT_HIP)),
            0.4f,
        )
        assertEquals(DragTarget.Move, SkeletonEditing.hitTest(figure, aspect, torso, r))
        // Middle of the shin grabs the ankle (rotates the shin around the knee).
        val shin = Vec2.mid(iso(figure, Joint.RIGHT_KNEE), iso(figure, Joint.RIGHT_ANKLE))
        assertEquals(DragTarget.JointTarget(Joint.RIGHT_ANKLE), SkeletonEditing.hitTest(figure, aspect, shin, r * 0.5f))
        assertNull(SkeletonEditing.hitTest(figure, aspect, Vec2(0.02f, 0.02f), r))
    }

    @Test
    fun newPeopleMatchTheExistingScaleAndAvoidOverlap() {
        val first = SkeletonEditing.translateClamped(figure, Vec2(-0.2f, 0f))
        val added = SkeletonEditing.placementForNewFigure(listOf(first), aspect)
        assertEquals(first.estimatedUnit(aspect), added.estimatedUnit(aspect), 1e-3f)
        val a = Box.around(first.points.values)!!.center.x
        val b = Box.around(added.points.values)!!.center.x
        assertTrue("new figure should be placed apart ($a vs $b)", abs(a - b) > 0.25f)
        assertNotNull(added[Joint.LEFT_FOOT])
    }

    @Test
    fun shotTypeFollowsWhatIsInFrame() {
        assertEquals(ShotType.FULL_BODY, PoseEditorViewModel.inferShot(listOf(figure)))
        // Zoomed in on the upper body: hips stay in frame, knees and feet fall below it.
        val portrait = figure.mapPoints { Vec2(it.x, it.y * 2f - 0.2f) }
        assertEquals(ShotType.HALF_BODY, PoseEditorViewModel.inferShot(listOf(portrait)))
    }

    @Test
    fun nonSquareFramesKeepRealBoneLengths() {
        val wide = 16f / 9f
        val f = SkeletonEditing.neutralFigure(wide)
        val start = f[Joint.RIGHT_ELBOW]!!.let { Vec2(it.x * wide, it.y) }
        val edited = SkeletonEditing.drag(f, wide, DragTarget.JointTarget(Joint.RIGHT_ELBOW), start, start + Vec2(-0.2f, -0.2f))
        assertTrue(SkeletonEditing.maxBoneLengthChange(f, edited, wide) < 1e-4f)
    }
}
