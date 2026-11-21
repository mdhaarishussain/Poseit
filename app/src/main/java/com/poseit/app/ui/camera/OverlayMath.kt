package com.poseit.app.ui.camera

import com.poseit.app.pose.scene.OverlayTransform

/** Converts on-screen gestures into [OverlayTransform] changes (scene space). */
internal object OverlayMath {

    /** How far the overlay centre may be dragged away from the scene centre (fraction of the scene). */
    const val MAX_OFFSET = 0.9f

    /**
     * Applies one transform-gesture step.
     * @param panX horizontal drag in view pixels
     * @param panY vertical drag in view pixels
     * @param zoom pinch factor of this step
     * @param rotationDeg rotation of this step in degrees, clockwise on screen
     * @param viewWidth width of the preview in pixels (= scene width)
     * @param viewHeight height of the preview in pixels (= scene height)
     * @param flipX the preview is mirrored (front lens): screen x is the opposite of scene x, and a clockwise
     *   rotation on screen is counter-clockwise in the scene
     */
    fun applyGesture(
        transform: OverlayTransform,
        panX: Float,
        panY: Float,
        zoom: Float,
        rotationDeg: Float,
        viewWidth: Float,
        viewHeight: Float,
        flipX: Boolean,
    ): OverlayTransform {
        if (viewWidth <= 0f || viewHeight <= 0f) return transform
        val sign = if (flipX) -1f else 1f
        val safeZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
        val safeRotation = if (rotationDeg.isFinite()) rotationDeg else 0f
        return transform.copy(
            offsetX = (transform.offsetX + sign * panX / viewWidth).coerceIn(-MAX_OFFSET, MAX_OFFSET),
            offsetY = (transform.offsetY + panY / viewHeight).coerceIn(-MAX_OFFSET, MAX_OFFSET),
            scale = (transform.scale * safeZoom).coerceIn(OverlayTransform.MIN_SCALE, OverlayTransform.MAX_SCALE),
            rotation = normalizeDegrees(transform.rotation + sign * safeRotation),
        )
    }

    /** Maps any angle into (-180, 180]. */
    fun normalizeDegrees(degrees: Float): Float {
        var d = degrees % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        return d
    }
}
