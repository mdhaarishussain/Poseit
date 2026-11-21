package com.poseit.app.pose.match

import com.poseit.app.pose.BodyPart
import com.poseit.app.pose.Joint
import com.poseit.app.pose.PoseCategory
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.TestFixtures.ASPECT
import com.poseit.app.pose.TestFixtures.detected
import com.poseit.app.pose.TestFixtures.figure
import com.poseit.app.pose.Vec2
import com.poseit.app.pose.dsl.pose
import com.poseit.app.pose.scaledAbout
import com.poseit.app.pose.scene.SceneMapping
import com.poseit.app.pose.shifted
import com.poseit.app.pose.torsoCentre
import com.poseit.app.pose.without
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PoseMatcherTest {

    private val subject = FramingAudience.SUBJECT
    private val photographer = FramingAudience.PHOTOGRAPHER

    /** Makes framing hints reachable even when the pose itself is perfect. */
    private val strict = MatchConfig(perfectScore = 0.99f)

    private fun match(
        template: Skeleton,
        detection: Skeleton,
        audience: FramingAudience = subject,
        config: MatchConfig = MatchConfig(),
    ) = PoseMatcher.match(listOf(template), listOf(detection), ASPECT, audience, config)

    private fun MatchResult.firstPose() = hints.firstOrNull { it.kind == HintKind.POSE }
    private fun MatchResult.firstFraming() = hints.firstOrNull { it.kind == HintKind.FRAMING }
    private fun MatchResult.texts() = hints.map { it.text }

    private fun assertHintInvariants(r: MatchResult) {
        assertTrue("hints never empty", r.hints.isNotEmpty())
        assertTrue("at most 3 hints: ${r.texts()}", r.hints.size <= 3)
        assertTrue("score in 0..1: ${r.score}", r.score in 0f..1f)
        r.hints.zipWithNext { a, b -> assertTrue("sorted by severity: ${r.texts()}", a.severity >= b.severity) }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scoring
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    fun identicalPoseScoresPerfect() {
        val t = figure {
            facing = 15f
            leftArm(upper = 40f, fore = -50f)
            rightArm(upper = -6f)
            rightLeg(thigh = -12f, shin = -4f)
        }
        val r = match(t, detected(t))
        assertTrue("score ${r.score}", r.score >= 0.95f)
        assertEquals(HintKind.PERFECT, r.hints.first().kind)
        assertEquals("Perfect. Hold it", r.hints.first().text)
        assertEquals(1, r.hints.size)
        assertEquals(1, r.peopleDetected)
        val fig = r.figures.single()
        assertEquals(0, fig.detectedIndex)
        assertEquals(1f, fig.coverage, 1e-4f)
        BodyPart.entries.forEach { part -> assertTrue("$part ${fig.partScores[part]}", (fig.partScores[part] ?: 0f) > 0.95f) }
        assertTrue(fig.segments.filter { it.required }.all { it.actualAngle != null && it.score > 0.95f })
    }

    @Test
    fun smallJitterStillMatches() {
        val t = figure { leftArm(60f, 100f); rightArm(-20f) }
        val rnd = Random(7)
        val noisy = detected(t).mapPoints { Vec2(it.x + (rnd.nextFloat() - 0.5f) * 0.008f, it.y + (rnd.nextFloat() - 0.5f) * 0.008f) }
        val r = match(t, noisy)
        assertTrue("score ${r.score}", r.score >= 0.9f)
        assertEquals(HintKind.PERFECT, r.hints.first().kind)
    }

    @Test
    fun raisedLeftArmTemplateAsksToRaiseLeftArm() {
        val t = figure { leftArm(upper = 165f, fore = 170f) }
        val d = detected(figure { })
        val r = match(t, d)
        assertHintInvariants(r)
        assertTrue(r.score < 0.8f)
        val top = r.firstPose()
        assertNotNull(top)
        assertEquals("Raise your left arm", top!!.text)
        assertEquals(BodyPart.LEFT_ARM, top.part)
        assertEquals(top, r.hints.first())
        val fig = r.figures.single()
        assertTrue(fig.partScores.getValue(BodyPart.LEFT_ARM) < 0.2f)
        assertTrue(fig.partScores.getValue(BodyPart.RIGHT_ARM) > 0.95f)

        val p = match(t, d, photographer)
        assertEquals("Ask them to raise their left arm", p.firstPose()!!.text)
    }

    @Test
    fun loweredArmAndLateralArmHints() {
        // Template arm down, person has it up -> lower.
        val lower = match(figure { }, detected(figure { rightArm(-165f, -170f) }))
        assertEquals("Lower your right arm", lower.firstPose()!!.text)
        // Arm should be out to the side; person keeps it by the body -> move it away.
        val away = match(figure { leftArm(50f) }, detected(figure { leftArm(5f) }))
        assertEquals("Move your left arm away from your body", away.firstPose()!!.text)
        val closer = match(figure { leftArm(5f) }, detected(figure { leftArm(50f) }))
        assertEquals("Bring your left arm closer to your body", closer.firstPose()!!.text)
        // Back to the camera: the left arm's outward side is the image LEFT.
        val awayBack = match(figure { facing = 180f; leftArm(-50f) }, detected(figure { facing = 180f; leftArm(-5f) }))
        assertEquals("Move your left arm away from your body", awayBack.firstPose()!!.text)
    }

    @Test
    fun elbowAndKneeHints() {
        val bend = match(figure { leftArm(upper = 20f, fore = 110f) }, detected(figure { leftArm(upper = 20f, fore = 20f) }))
        assertEquals("Bend your left elbow more", bend.firstPose()!!.text)
        val straighten = match(figure { rightArm(upper = -20f, fore = -20f) }, detected(figure { rightArm(-20f, -110f) }))
        assertEquals("Straighten your right arm", straighten.firstPose()!!.text)

        val lift = match(figure { leftLeg(thigh = 120f, shin = 10f) }, detected(figure { }))
        assertEquals("Lift your left knee", lift.firstPose()!!.text)
        val stepOut = match(figure { rightLeg(thigh = -35f, shin = -35f) }, detected(figure { }))
        assertEquals("Step your right foot out", stepOut.firstPose()!!.text)
        val bendKnee = match(figure { leftLeg(thigh = 10f, shin = -80f) }, detected(figure { leftLeg(10f, 10f) }))
        assertEquals("Bend your left knee more", bendKnee.firstPose()!!.text)
        val straightLeg = match(figure { leftLeg(10f, 10f) }, detected(figure { leftLeg(thigh = 10f, shin = -80f) }))
        assertEquals("Straighten your left leg", straightLeg.firstPose()!!.text)
    }

    @Test
    fun mirroredDetectionScoresClearlyLower() {
        val t = figure {
            leftArm(150f, 160f)
            rightArm(-10f, 30f)
            leftLeg(25f, 10f)
        }
        val same = match(t, detected(t)).score
        val mirrored = match(t, detected(t.mirrored())).score
        assertTrue("same $same", same >= 0.95f)
        assertTrue("mirrored $mirrored vs $same", mirrored < 0.75f && mirrored < same - 0.25f)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Facing & directions
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    fun backFacingSubjectFlipsLeanDirection() {
        val front = match(figure { torsoLean = 35f }, detected(figure { }))
        assertEquals("Lean to your left", front.firstPose()!!.text)
        assertEquals(BodyPart.TORSO, front.firstPose()!!.part)

        val back = match(figure { facing = 180f; torsoLean = 35f }, detected(figure { facing = 180f }))
        assertEquals("Lean to your right", back.firstPose()!!.text)

        val upright = match(figure { }, detected(figure { torsoLean = -35f }))
        assertEquals("Stand up straight", upright.firstPose()!!.text)
    }

    @Test
    fun backFacingSubjectFlipsStepDirection() {
        val front = figure { }
        val frontResult = match(front, detected(front).shifted(0.25f, 0f), config = strict)
        assertEquals("Step to your right", frontResult.firstFraming()!!.text)

        val back = figure { facing = 180f }
        val backResult = match(back, detected(back).shifted(0.25f, 0f), config = strict)
        assertEquals("Step to your left", backResult.firstFraming()!!.text)
    }

    @Test
    fun facingTheWrongWayAsksToTurn() {
        val r = match(figure { }, detected(figure { facing = 180f }))
        assertHintInvariants(r)
        assertEquals("Turn around to face the camera", r.hints.first().text)
        val p = match(figure { facing = 180f }, detected(figure { }), photographer)
        assertEquals("Ask them to turn their back to the camera", p.hints.first().text)
    }

    @Test
    fun headTiltAndShoulderHints() {
        // Head and shoulder-line errors are minor, so use a strict config to see their hints.
        val tilt = match(figure { headTilt = 30f }, detected(figure { }), config = strict)
        assertEquals("Tilt your head to your left", tilt.firstPose()!!.text)
        val tiltBackFacing = match(figure { facing = 160f; headYaw = 180f; headTilt = 30f }, detected(figure { facing = 160f; headYaw = 180f }))
        // Back-facing heads have no visible nose, so the head is not compared at all.
        assertTrue(tiltBackFacing.figures.single().segments.none { it.segment == Segment.HEAD && it.required })

        // Positive tilt = image-right shoulder lower = the LEFT shoulder of someone facing the camera.
        val drop = match(figure { shoulderTilt = 25f }, detected(figure { }), config = strict)
        assertEquals("Drop your left shoulder", drop.firstPose()!!.text)
        val dropRight = match(figure { shoulderTilt = -25f }, detected(figure { }), config = strict)
        assertEquals("Drop your right shoulder", dropRight.firstPose()!!.text)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Visibility
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    fun missingLegsGiveFeetVisibilityHint() {
        val t = figure { }
        val d = detected(t).without(
            Joint.LEFT_KNEE, Joint.RIGHT_KNEE, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE, Joint.LEFT_FOOT, Joint.RIGHT_FOOT,
        )
        val r = match(t, d)
        assertHintInvariants(r)
        assertTrue(r.figures.single().coverage < 0.7f)
        assertTrue(r.score < 0.8f)
        val vis = r.hints.first { it.kind == HintKind.VISIBILITY }
        assertEquals("Your feet are cut off. Step back", vis.text)

        val p = match(t, d, photographer)
        assertTrue(p.texts().toString(), "Their feet are cut off. Step back or tilt down" in p.texts())
    }

    @Test
    fun feetBelowTheFrameAreCutOff() {
        val t = figure { }
        // Detector extrapolates the feet below the image with low confidence.
        val d = detected(t).let { s ->
            val low = listOf(Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE, Joint.LEFT_FOOT, Joint.RIGHT_FOOT)
            Skeleton(
                points = s.points.mapValues { (j, p) -> if (j in low) Vec2(p.x, 1.08f) else p },
                visibility = s.visibility.mapValues { (j, v) -> if (j in low) 0.2f else v },
            )
        }
        val r = match(t, d)
        assertTrue(r.texts().toString(), "Your feet are cut off. Step back" in r.texts())
    }

    @Test
    fun headOutOfFrame() {
        val t = figure { }
        val d = detected(t).shifted(0f, -0.25f)
        val r = match(t, d)
        assertHintInvariants(r)
        assertEquals("Your head is out of frame", r.hints.first().text)
        assertEquals(HintKind.VISIBILITY, r.hints.first().kind)
        assertTrue("a cut-off head is never a match: ${r.score}", r.score < 0.8f)
        assertEquals("Their head is cut off. Tilt the phone up", match(t, d, photographer).hints.first().text)
        val offLeft = detected(t).shifted(-0.62f, 0f)
        assertTrue(match(t, offLeft, photographer).texts().contains("Their head is cut off. Move the phone left"))
    }

    @Test
    fun halfBodyTemplateIgnoresLegs() {
        val t = figure(top = 0.05f, bodyHeight = 1.6f) { leftArm(15f, -20f); rightArm(-10f) }
        assertTrue("template knees below the frame", t.points.getValue(Joint.LEFT_KNEE).y > 1.05f)
        assertTrue("template hips in frame", t.points.getValue(Joint.LEFT_HIP).y < 0.95f)

        val noLegs = detected(t).without(
            Joint.LEFT_KNEE, Joint.RIGHT_KNEE, Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE, Joint.LEFT_FOOT, Joint.RIGHT_FOOT,
        )
        val r = match(t, noLegs)
        assertTrue("score ${r.score}", r.score >= 0.95f)
        assertEquals(HintKind.PERFECT, r.hints.first().kind)
        val segs = r.figures.single().segments.associateBy { it.segment }
        listOf(Segment.LEFT_THIGH, Segment.RIGHT_THIGH, Segment.LEFT_SHIN, Segment.RIGHT_SHIN).forEach {
            assertFalse("$it should not be required", segs[it]?.required ?: false)
        }
        assertTrue(segs.getValue(Segment.LEFT_FOREARM).required)
        assertFalse(BodyPart.LEFT_LEG in r.figures.single().partScores)

        // Legs in odd positions below the frame don't matter either.
        val wildLegs = detected(t).let { s ->
            val moved = mapOf(
                Joint.LEFT_KNEE to Vec2(0.95f, 0.98f), Joint.LEFT_ANKLE to Vec2(1.2f, 1.1f),
                Joint.RIGHT_KNEE to Vec2(0.1f, 1.3f), Joint.RIGHT_ANKLE to Vec2(-0.2f, 1.5f),
            )
            Skeleton(s.points + moved, s.visibility + moved.mapValues { 0.9f })
        }
        val r2 = match(t, wildLegs, config = strict)
        assertTrue(r2.hints.none { it.kind == HintKind.VISIBILITY })
        assertTrue("score ${r2.score}", r2.score >= 0.95f)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Framing
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    fun translatedDetectionGivesFramingDirectionForBothAudiences() {
        val t = figure { }
        val right = detected(t).shifted(0.25f, 0f)
        assertEquals("Step to your right", match(t, right, subject, strict).firstFraming()!!.text)
        assertEquals("Move the phone right", match(t, right, photographer, strict).firstFraming()!!.text)

        val left = detected(t).shifted(-0.25f, 0f)
        assertEquals("Step to your left", match(t, left, subject, strict).firstFraming()!!.text)
        assertEquals("Move the phone left", match(t, left, photographer, strict).firstFraming()!!.text)

        val high = detected(t).shifted(0f, -0.12f)
        assertEquals("Tilt the phone up", match(t, high, photographer, strict).firstFraming()!!.text)
        // The subject can't fix a vertical offset by stepping, so they get a phone instruction too (once, unprefixed).
        val r = match(t, high, subject, strict)
        assertEquals(listOf("Raise the phone a little"), r.hints.filter { it.text.contains("phone") }.map { it.text })
        val low = detected(t).shifted(0f, 0.04f)
        assertTrue(match(t, low, subject, strict).hints.none { it.text.contains("phone") }) // within tolerance
    }

    @Test
    fun subjectVerticalHintIsEmittedOncePerMatch() {
        // Two people both too high in the picture: a single, unprefixed "Raise the phone" hint, never "Lower".
        val a = figure(cx = 0.3f, bodyHeight = 0.6f) { }
        val b = figure(cx = 0.72f, bodyHeight = 0.6f) { }
        val r = PoseMatcher.match(
            listOf(a, b),
            listOf(detected(a).shifted(0f, -0.15f), detected(b).shifted(0f, -0.15f)),
            ASPECT,
            subject,
            strict,
        )
        val phone = r.hints.filter { it.text.contains("phone") }
        assertEquals(listOf("Raise the phone a little"), phone.map { it.text })
        assertNull(phone.single().figureIndex)
    }

    @Test
    fun personNumbersFollowTheMirroredView() {
        // Person 2 (right in the scene) keeps the arm down. On a mirrored (front-camera) preview that figure is drawn
        // on the LEFT of the screen, so it is "Person 1" there.
        val (a, b) = SceneMapping.templateToScene(couple, ASPECT)
        val bWrong = detected(figure(cx = 0.72f, bodyHeight = 0.6f) { facing = -20f })
        val plain = PoseMatcher.match(listOf(a, b), listOf(detected(a), bWrong), ASPECT)
        assertEquals("Person 2: Raise your right arm", plain.firstPose()!!.text)
        val mirrored = PoseMatcher.match(listOf(a, b), listOf(detected(a), bWrong), ASPECT, mirroredView = true)
        assertEquals("Person 1: Raise your right arm", mirrored.firstPose()!!.text)
        assertEquals(1, mirrored.firstPose()!!.figureIndex)
    }

    @Test
    fun sizeHints() {
        val t = figure(bottom = 0.8f, bodyHeight = 0.6f) { }
        val centre = t.torsoCentre()
        val small = detected(t).scaledAbout(centre, 0.7f)
        assertEquals("Step closer", match(t, small, subject, strict).firstFraming()!!.text)
        assertEquals("Move closer or zoom in", match(t, small, photographer, strict).firstFraming()!!.text)

        val big = detected(t).scaledAbout(centre, 1.35f)
        assertEquals("Step back", match(t, big, subject, strict).firstFraming()!!.text)
        assertEquals("Step back or zoom out", match(t, big, photographer, strict).firstFraming()!!.text)

        val same = match(t, detected(t), subject, strict)
        assertTrue(same.hints.none { it.kind == HintKind.FRAMING })
    }

    @Test
    fun photographerIsToldNotToCropAtTheKnees() {
        val t = figure { }
        // Person framed so that the bottom edge runs through the knees.
        val d = detected(t).let { s ->
            val knee = s.points.getValue(Joint.LEFT_KNEE).y
            s.shifted(0f, 1f - knee)
        }
        val r = match(t, d, photographer)
        assertTrue(r.texts().toString(), r.texts().any { it.startsWith("Their feet are cut off") })
        // A knees-up template whose knees sit below the frame: the person is framed so the edge cuts the knees.
        val threeQuarter = figure(top = 0.03f, bodyHeight = 1.5f) { }
        assertTrue(threeQuarter.points.getValue(Joint.LEFT_KNEE).y in 1.07f..1.2f)
        val dq = detected(threeQuarter).let { s -> s.shifted(0f, 1f - s.points.getValue(Joint.LEFT_KNEE).y) }
        val rq = match(threeQuarter, dq, photographer, MatchConfig(perfectScore = 1f))
        assertTrue(rq.texts().toString(), "Don't crop at the joints. Show the feet or frame above the knees" in rq.texts())
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Multi-person
    // ---------------------------------------------------------------------------------------------------------------

    private val couple = pose("couple", "Couple", PoseCategory.COUPLE) {
        figure(cx = 0.28f, bodyHeight = 0.6f) { facing = 20f; leftArm(150f, 160f) }
        figure(cx = 0.72f, bodyHeight = 0.6f) { facing = -20f; rightArm(-160f, -170f) }
    }

    @Test
    fun twoPersonAssignmentHandlesSwappedDetectionOrder() {
        val (a, b) = SceneMapping.templateToScene(couple, ASPECT)
        val r = PoseMatcher.match(listOf(a, b), listOf(detected(b), detected(a)), ASPECT)
        assertEquals(1, r.figures[0].detectedIndex)
        assertEquals(0, r.figures[1].detectedIndex)
        assertTrue("score ${r.score}", r.score >= 0.95f)
        assertEquals(2, r.peopleDetected)
        assertEquals(HintKind.PERFECT, r.hints.first().kind)
    }

    @Test
    fun identicalFiguresAreAssignedByPosition() {
        val twins = pose("twins", "Twins", PoseCategory.GROUP) {
            figure(cx = 0.25f, bodyHeight = 0.5f) { }
            figure(cx = 0.75f, bodyHeight = 0.5f) { }
        }
        val (a, b) = SceneMapping.templateToScene(twins, ASPECT)
        val r = PoseMatcher.match(listOf(a, b), listOf(detected(b), detected(a)), ASPECT)
        assertEquals(listOf(1, 0), r.figures.map { it.detectedIndex })
    }

    @Test
    fun missingPersonAndPersonPrefix() {
        val (a, b) = SceneMapping.templateToScene(couple, ASPECT)
        val one = PoseMatcher.match(listOf(a, b), listOf(detected(a)), ASPECT)
        assertHintInvariants(one)
        assertEquals(HintKind.MISSING_PERSON, one.hints.first().kind)
        assertEquals("1 of 2 people in the frame", one.hints.first().text)
        assertEquals(0.5f, one.score, 0.05f)
        assertNull(one.figures[1].detectedIndex)
        assertEquals(0f, one.figures[1].score)
        val oneP = PoseMatcher.match(listOf(a, b), listOf(detected(a)), ASPECT, photographer)
        assertEquals("1 of 2 people in the frame. Get both in", oneP.hints.first().text)
        // Even a very lenient strictness never calls it perfect while someone is missing.
        val lenient = PoseMatcher.match(listOf(a, b), listOf(detected(a)), ASPECT, config = MatchConfig(perfectScore = 0.3f))
        assertEquals(HintKind.MISSING_PERSON, lenient.hints.first().kind)

        // Person 2 (right in the template) keeps the arm down.
        val bWrong = detected(figure(cx = 0.72f, bodyHeight = 0.6f) { facing = -20f })
        val r = PoseMatcher.match(listOf(a, b), listOf(detected(a), bWrong), ASPECT)
        val top = r.firstPose()!!
        assertEquals("Person 2: Raise your right arm", top.text)
        assertEquals(1, top.figureIndex)
        assertEquals("Person 2, Raise your right arm", top.speech)
        val rp = PoseMatcher.match(listOf(a, b), listOf(detected(a), bWrong), ASPECT, photographer)
        assertEquals("Person 2: Ask them to raise their right arm", rp.firstPose()!!.text)
    }

    @Test
    fun extraPeopleAreIgnored() {
        val t = figure(cx = 0.3f, bodyHeight = 0.6f) { leftArm(120f) }
        val stranger = detected(figure(cx = 0.8f, bodyHeight = 0.5f) { })
        val r = PoseMatcher.match(listOf(t), listOf(stranger, detected(t)), ASPECT)
        assertEquals(1, r.figures.single().detectedIndex)
        assertTrue(r.score >= 0.95f)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Robustness
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    fun nobodyDetected() {
        val t = figure { }
        val r = PoseMatcher.match(listOf(t), emptyList(), ASPECT)
        assertEquals(0f, r.score)
        assertEquals(0, r.peopleDetected)
        assertEquals(listOf("Step into the frame"), r.texts())
        assertEquals(HintKind.NO_PERSON, r.hints.single().kind)
        assertNull(r.figures.single().detectedIndex)
        assertTrue(r.figures.single().segments.isNotEmpty())

        val p = PoseMatcher.match(listOf(t), emptyList(), ASPECT, photographer)
        assertEquals(listOf("Point the camera at the person"), p.texts())

        // A "person" with nothing confidently visible counts as nobody.
        val ghost = Skeleton(t.points, t.points.keys.associateWith { 0.1f })
        val g = PoseMatcher.match(listOf(t), listOf(ghost), ASPECT)
        assertEquals(HintKind.NO_PERSON, g.hints.single().kind)
        assertEquals(0, g.peopleDetected)
    }

    @Test
    fun neverThrowsOnBadInput() {
        val t = figure { }
        val nan = Skeleton(t.points.mapValues { (j, p) -> if (j == Joint.LEFT_ELBOW) Vec2(Float.NaN, p.y) else p },
            t.points.keys.associateWith { if (it == Joint.NOSE) Float.NaN else 0.9f })
        val inf = t.mapPoints { Vec2(Float.POSITIVE_INFINITY, it.y) }
        val cases = listOf(
            PoseMatcher.match(emptyList(), emptyList(), ASPECT),
            PoseMatcher.match(emptyList(), listOf(detected(t)), ASPECT),
            PoseMatcher.match(listOf(t), listOf(nan), ASPECT),
            PoseMatcher.match(listOf(t), listOf(inf), ASPECT),
            PoseMatcher.match(listOf(t), listOf(Skeleton(emptyMap())), ASPECT),
            PoseMatcher.match(listOf(Skeleton(emptyMap())), listOf(detected(t)), ASPECT),
            PoseMatcher.match(listOf(nan), listOf(detected(t)), Float.NaN),
            PoseMatcher.match(listOf(t), listOf(detected(t)), -1f),
            PoseMatcher.match(listOf(t), listOf(detected(t)), ASPECT, config = MatchConfig(Float.NaN, 50f, 10f, Float.NaN)),
            PoseMatcher.match(List(5) { t }, List(6) { detected(t) }, ASPECT),
            PoseMatcher.match(List(8) { t }, List(7) { detected(t) }, ASPECT),
        )
        cases.forEach(::assertHintInvariants)
        assertEquals(0f, cases[0].score)
        assertEquals("Pick a pose to get started", cases[1].hints.single().text)
        assertTrue(cases[2].score.isFinite())
    }

    @Test
    fun randomPosesKeepInvariants() {
        val rnd = Random(1234)
        fun angle(range: Float) = (rnd.nextFloat() * 2f - 1f) * range
        repeat(60) {
            val t = figure(cx = 0.35f + rnd.nextFloat() * 0.3f) {
                facing = angle(180f); torsoLean = angle(30f)
                leftArm(angle(180f), angle(180f)); rightArm(angle(180f), angle(180f))
                leftLeg(angle(60f), angle(60f)); rightLeg(angle(60f), angle(60f))
            }
            val d = figure(cx = 0.35f + rnd.nextFloat() * 0.3f, bodyHeight = 0.5f + rnd.nextFloat() * 0.5f) {
                facing = angle(180f); headTilt = angle(30f)
                leftArm(angle(180f), angle(180f)); rightArm(angle(180f), angle(180f))
                leftLeg(angle(60f), angle(60f))
            }
            for (audience in FramingAudience.entries) {
                val r = PoseMatcher.match(listOf(t), listOf(detected(d)), ASPECT, audience)
                assertHintInvariants(r)
                r.figures.single().partScores.values.forEach { assertTrue(it in 0f..1f) }
            }
        }
    }

    @Test
    fun phrasingTurnsSubjectImperativesIntoPhotographerRequests() {
        assertEquals("Raise your left arm", Phrasing.instruct("Raise your left arm", subject))
        assertEquals("Ask them to raise their left arm", Phrasing.instruct("Raise your left arm", photographer))
        assertEquals("Ask them to tilt their head to their right", Phrasing.instruct("Tilt your head to your right", photographer))
        assertEquals("left", Phrasing.subjectSide(towardImageRight = true, facing = Facing.FRONT))
        assertEquals("right", Phrasing.subjectSide(towardImageRight = true, facing = Facing.BACK))
        assertEquals("right", Phrasing.subjectSide(towardImageRight = false, facing = Facing.UNKNOWN))
    }
}
