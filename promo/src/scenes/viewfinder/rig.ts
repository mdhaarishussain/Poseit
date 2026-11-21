/**
 * rig.ts: the live view's geometry (act IV). Pure: no React, so shots.ts and the scripts can bundle it.
 *
 * SCENE SPACE is the app's own: the camera image in photo orientation, x 0..1 across, y 0..1 down, aspect 9:16 (a
 * real SceneAspect of the app, RATIO_9_16), anatomical labels (the subject's LEFT is on the image right). The film
 * frame IS the scene, and the back camera shows it unflipped. PoseMatcher reads scene skeletons.
 *
 *   SCENE_ASPECT                      9 / 16
 *   templateFrameRect(aspect, t)      where the app puts a template frame in the 9:16 scene, in film px:
 *                                     SceneMapping.fitFrame, then the OverlayTransform (scale about the fitted
 *                                     frame's centre, then the offsets; rotation 0)
 *
 * (v1's front-camera helpers for the keyframed performer, guideFrameFor, frameToScene and the forward kinematics,
 * went with the performer: act IV is real footage now.)
 */
import { theme } from "../../theme";

const { W, H } = theme.size;

export const SCENE_ASPECT = W / H;

/**
 * Where the app draws a template frame of `frameAspect` in the 9:16 scene (SceneMapping.templateToScene's Mapper:
 * fitFrame "contain", then an isotropic scale about the fitted frame's centre, then the offsets in scene units), in
 * film px. The back camera draws the scene unflipped, so this is also the frame on screen (before the film camera).
 */
export const templateFrameRect = (
  frameAspect: number,
  t: { scale: number; offsetX: number; offsetY: number; rotation?: number },
): { x: number; y: number; width: number; height: number } => {
  if (t.rotation) throw new Error("templateFrameRect: rotation is not supported");
  const wide = frameAspect > SCENE_ASPECT;
  const w = wide ? 1 : frameAspect / SCENE_ASPECT;
  const h = wide ? SCENE_ASPECT / frameAspect : 1;
  const left = (1 - w) / 2;
  const top = (1 - h) / 2;
  const cx = left + w / 2;
  const cy = top + h / 2;
  const x0 = cx + (left - cx) * t.scale + t.offsetX;
  const y0 = cy + (top - cy) * t.scale + t.offsetY;
  return { x: x0 * W, y: y0 * H, width: w * t.scale * W, height: h * t.scale * H };
};
