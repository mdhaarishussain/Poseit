package com.poseit.app.pose.scene

import com.poseit.app.pose.Box
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton

/*
 * COORDINATE SPACES used across Poseit
 * ------------------------------------
 *  FRAME space : a PoseTemplate's own normalized frame (x,y in 0..1, aspect = template.frameAspect).
 *  SCENE space : normalized coordinates over the VISIBLE camera preview, which is exactly the area of the
 *                photo that will be saved (CameraX ViewPort). x,y in 0..1, y down, aspect = sceneAspect
 *                (width/height, 3:4 by default). Scene space is in PHOTO orientation, i.e. NOT mirrored, even
 *                for the front camera. Matching, framing analysis and hints all happen in scene space.
 *  VIEW space  : pixels on screen. For the front camera the preview is mirrored, so scene x must be
 *                flipped (x -> 1 - x) when drawing.
 */

/**
 * User adjustment of the pose overlay (drag / pinch).
 * @param offsetX horizontal shift in SCENE-normalized units (fraction of scene width)
 * @param offsetY vertical shift in SCENE-normalized units (fraction of scene height)
 * @param scale uniform scale about the centre of the fitted frame
 */
data class OverlayTransform(val offsetX: Float = 0f, val offsetY: Float = 0f, val scale: Float = 1f) {
    companion object {
        val IDENTITY = OverlayTransform()
        const val MIN_SCALE = 0.4f
        const val MAX_SCALE = 2.5f
    }
}

object SceneMapping {

    /**
     * The rectangle (in SCENE-normalized units) that a template frame of [frameAspect] occupies when fitted
     * ("contain", centred) into a scene of [sceneAspect].
     */
    fun fitFrame(frameAspect: Float, sceneAspect: Float): Box {
        TODO("implemented by the engine agent")
    }

    /** Maps one point from template FRAME space to SCENE space. */
    fun frameToScene(x: Float, y: Float, frameAspect: Float, sceneAspect: Float, transform: OverlayTransform): Pair<Float, Float> {
        TODO("implemented by the engine agent")
    }

    /**
     * Maps all template figures into SCENE space.
     * @param mirrored when true the template is first mirrored anatomically ([Skeleton.mirrored]) – this is the
     *   user's "flip pose" toggle, NOT front-camera display mirroring.
     */
    fun templateToScene(
        template: PoseTemplate,
        sceneAspect: Float,
        transform: OverlayTransform = OverlayTransform.IDENTITY,
        mirrored: Boolean = false,
    ): List<Skeleton> {
        TODO("implemented by the engine agent")
    }
}
