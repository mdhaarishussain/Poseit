/**
 * transform.ts: the one transform between a plate's scene space and the composition. Pure, no React.
 *
 * Scene space is the app's: x, y in 0..1 over the 9:16 plate (photo orientation, not mirrored). The plate fills the
 * 1080x1920 composition, then the film camera may push in: a uniform `scale` about an `anchor` (scene coordinates),
 * then a shift (`dx`, `dy`, composition px). The Footage component draws the video AND its children through the same
 * CSS matrix, so anything drawn in plate pixels inside it (a skeleton, a guide) sticks to the person under any push;
 * `sceneToComp` gives the same point for things drawn outside it (crisp HUD text, a hit point).
 *
 *   IDENTITY_TRANSFORM
 *   footageMatrix(t, W, H)     [s, tx, ty]: comp = s * (x * W, y * H) + (tx, ty)
 *   cssMatrix(t, W, H)         the CSS transform string of that matrix (transform-origin 0 0)
 *   sceneToComp(p, t, W, H)    a scene point -> composition px
 *   compToScene(p, t, W, H)    the inverse
 *   pushAt(frame, push)        an eased push: { range: [f0, f1], scale: [s0, s1], anchor, ease? } -> transform
 */
import { theme } from "../theme";
import type { Vec } from "../figure/types";

export type FootageTransform = {
  /** uniform zoom about the anchor (1 = the plate fills the frame) */
  scale: number;
  /** zoom centre in scene coordinates */
  anchor: Vec;
  /** shift after the zoom, composition px */
  dx: number;
  dy: number;
};

export const IDENTITY_TRANSFORM: FootageTransform = { scale: 1, anchor: [0.5, 0.5], dx: 0, dy: 0 };

export const footageMatrix = (t: FootageTransform, W = theme.size.W, H = theme.size.H): [number, number, number] => {
  const ax = t.anchor[0] * W;
  const ay = t.anchor[1] * H;
  return [t.scale, ax * (1 - t.scale) + t.dx, ay * (1 - t.scale) + t.dy];
};

export const cssMatrix = (t: FootageTransform, W = theme.size.W, H = theme.size.H): string => {
  const [s, tx, ty] = footageMatrix(t, W, H);
  return `matrix(${s}, 0, 0, ${s}, ${tx}, ${ty})`;
};

export const sceneToComp = (p: Vec, t: FootageTransform, W = theme.size.W, H = theme.size.H): Vec => {
  const [s, tx, ty] = footageMatrix(t, W, H);
  return [s * p[0] * W + tx, s * p[1] * H + ty];
};

export const compToScene = (p: Vec, t: FootageTransform, W = theme.size.W, H = theme.size.H): Vec => {
  const [s, tx, ty] = footageMatrix(t, W, H);
  return [(p[0] - tx) / (s * W), (p[1] - ty) / (s * H)];
};

export type Push = {
  /** global frames: the push runs over [f0, f1] and holds outside */
  range: [number, number];
  scale: [number, number];
  /** scene point that stays put (for a person: their mid-hip) */
  anchor: Vec;
  /** default theme.ease.drift (a creep over the whole span) */
  ease?: (t: number) => number;
};

export const pushAt = (frame: number, push: Push): FootageTransform => {
  const [f0, f1] = push.range;
  const u = f1 > f0 ? Math.min(1, Math.max(0, (frame - f0) / (f1 - f0))) : 1;
  const e = (push.ease ?? theme.ease.drift)(u);
  return { scale: push.scale[0] + (push.scale[1] - push.scale[0]) * e, anchor: push.anchor, dx: 0, dy: 0 };
};
