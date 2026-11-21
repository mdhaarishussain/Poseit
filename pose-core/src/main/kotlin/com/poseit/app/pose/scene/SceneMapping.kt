package com.poseit.app.pose.scene

import com.poseit.app.pose.Box
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.Skeleton
import com.poseit.app.pose.Vec2

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
 * @param rotation rotation in degrees (clockwise on screen) about the centre of the fitted frame. Rotation must be
 *   applied in ISOTROPIC units (x scaled by the scene aspect) so the figure is not sheared.
 *
 * Order: fit frame into scene -> scale & rotate about the fitted frame's centre -> translate by offsets.
 */
data class OverlayTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    companion object {
        val IDENTITY = OverlayTransform()
        const val MIN_SCALE = 0.4f
        const val MAX_SCALE = 2.5f
    }
}

object SceneMapping {

    /** Used whenever a caller passes a non-finite or non-positive aspect ratio. */
    private const val FALLBACK_ASPECT = 3f / 4f

    /**
     * The rectangle (in SCENE-normalized units) that a template frame of [frameAspect] occupies when fitted
     * ("contain", centred) into a scene of [sceneAspect].
     *
     * A frame that is relatively wider than the scene spans the full scene width (letterboxed top/bottom);
     * otherwise it spans the full height (pillarboxed left/right). Invalid aspects are treated as 3:4.
     */
    fun fitFrame(frameAspect: Float, sceneAspect: Float): Box {
        val frame = sanitizeAspect(frameAspect)
        val scene = sanitizeAspect(sceneAspect)
        return if (frame > scene) {
            val h = scene / frame
            val top = (1f - h) / 2f
            Box(0f, top, 1f, top + h)
        } else {
            val w = frame / scene
            val left = (1f - w) / 2f
            Box(left, 0f, left + w, 1f)
        }
    }

    /** Maps one point from template FRAME space to SCENE space. */
    fun frameToScene(x: Float, y: Float, frameAspect: Float, sceneAspect: Float, transform: OverlayTransform): Pair<Float, Float> {
        val p = Mapper(frameAspect, sceneAspect, transform).map(Vec2(x, y))
        return p.x to p.y
    }

    /**
     * Maps all template figures into SCENE space.
     *
     * Visibility maps are kept; [Skeleton.unit] (frame heights) is converted to scene heights
     * (`unit * fittedFrame.height * transform.scale`); an unknown unit (0) stays 0.
     *
     * @param mirrored when true the template is first mirrored anatomically ([Skeleton.mirrored]) – this is the
     *   user's "flip pose" toggle, NOT front-camera display mirroring.
     */
    fun templateToScene(
        template: PoseTemplate,
        sceneAspect: Float,
        transform: OverlayTransform = OverlayTransform.IDENTITY,
        mirrored: Boolean = false,
    ): List<Skeleton> {
        val mapper = Mapper(template.frameAspect, sceneAspect, transform)
        val figures = if (mirrored) template.figures.map { it.mirrored() } else template.figures
        return figures.map { figure ->
            Skeleton(
                points = figure.points.mapValues { (_, p) -> mapper.map(p) },
                visibility = figure.visibility,
                unit = figure.unit * mapper.unitScale,
            )
        }
    }

    private fun sanitizeAspect(aspect: Float): Float =
        if (aspect.isFinite() && aspect > 0f) aspect else FALLBACK_ASPECT

    /** Precomputes the fit and transform so a whole template can be mapped without redoing the maths per point. */
    private class Mapper(frameAspect: Float, sceneAspect: Float, transform: OverlayTransform) {
        private val fit = fitFrame(frameAspect, sceneAspect)
        private val aspect = sanitizeAspect(sceneAspect)
        private val scale = transform.scale.takeIf { it.isFinite() && it > 0f } ?: 1f
        private val rotation = transform.rotation.takeIf { it.isFinite() } ?: 0f
        private val offsetX = transform.offsetX.takeIf { it.isFinite() } ?: 0f
        private val offsetY = transform.offsetY.takeIf { it.isFinite() } ?: 0f
        private val center = fit.center

        /** Factor converting a length in frame heights into scene heights. */
        val unitScale: Float = fit.height * scale

        fun map(p: Vec2): Vec2 {
            val fx = fit.left + p.x * fit.width
            val fy = fit.top + p.y * fit.height
            // Isotropic offset from the fitted frame's centre, so scaling/rotating does not shear the figure.
            var d = Vec2((fx - center.x) * aspect, fy - center.y) * scale
            if (rotation != 0f) d = d.rotated(rotation)
            return Vec2(center.x + d.x / aspect + offsetX, center.y + d.y + offsetY)
        }
    }
}
