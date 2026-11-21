/**
 * rig.ts: the viewfinder's geometry (acts I and III). Pure: no React, so scripts/export-live.ts can bundle it.
 *
 * SCENE SPACE is the app's own: the camera image in photo orientation, x 0..1 across, y 0..1 down, aspect 9:16 (a
 * real SceneAspect of the app, RATIO_9_16), anatomical labels (the subject's LEFT is on the image right). The film
 * frame IS the scene; the front camera shows it flipped for display (flipForDisplay), so the subject's left arm is
 * on screen left. PoseMatcher reads scene skeletons; the film draws flipForDisplay(scene skeleton).
 *
 *   SCENE_ASPECT                      9 / 16
 *   GUIDE_FRAME                       where the Disco point template frame sits ON SCREEN (display px, flipped):
 *                                     head top y 0.38, feet y 0.94, mid-hip centred (STORYBOARD section 9)
 *   frameToScene(skel, frameAspect, frame)   template frame -> scene skeleton (front camera: frame drawn flipped)
 *   guideScene(disco)                 the guide figure in scene space (what the app passes to the matcher)
 *   toPx / fromPx                     scene <-> isotropic film px (x * W, y * H)
 *   limbAngles / setLimb / lerpAngle  forward kinematics on a limb chain (lengths kept)
 *   fitBody(pose, hip, unitPx)        a template stance placed as a body in scene px (mid-hip, torso length)
 *   scaleAbout / shift                distance and position moves (a step back is a scale about the horizon)
 *   withVisibility(joints)            MediaPipe-like visibility: inside the image ~0.95, falling off past the edge
 *   visibleOnly(s)                    the joints the app draws (visibility >= 0.5)
 */
import { theme } from "../../theme";
import { poseInkBounds } from "../../figure/poses";
import { buildShapes } from "../../figure/shapes";
import type { JointName, Joints, Pose, Skeleton, Vec } from "../../figure/types";

const { W, H } = theme.size;

export const SCENE_ASPECT = W / H;

/** Pixel frame of the guide's template frame on screen (display, flipped), from the Disco point ink. */
export const guideFrameFor = (disco: Pose) => {
  const f = disco.figures[0];
  const ink = poseInkBounds(disco);
  // head top: the top of the mannequin's head circle (FigureShapes); feet: the lowest ink
  const headTop = headTopOf(f, disco.frameAspect);
  const fh = ((0.94 - 0.38) * H) / (ink.maxY - headTop);
  const fw = fh * disco.frameAspect;
  const fy = 0.38 * H - headTop * fh;
  const hipX = (f.joints.LEFT_HIP![0] + f.joints.RIGHT_HIP![0]) / 2;
  // flipped: display x = fx + fw * (1 - x); put the mid-hip on the centre line
  const fx = W / 2 - fw * (1 - hipX);
  return { x: fx, y: fy, height: fh, width: fw };
};

/** Top of the head circle, frame y (the HEAD circle of FigureShapes, isotropic y = frame y). */
export const headTopOf = (s: Skeleton, aspect: number) => {
  const head = buildShapes(s, aspect).find((x) => x.t === "circle" && x.part === "HEAD");
  if (!head || head.t !== "circle") throw new Error("headTopOf: no head circle");
  return head.c[1] - head.r;
};

/** Template frame joints -> scene (front camera: the frame is drawn flipped at `frame`, scene = un-flipped). */
export const frameToScene = (s: Skeleton, frameAspect: number, frame: { x: number; y: number; height: number }): Skeleton => {
  const fw = frame.height * frameAspect;
  const joints: Joints = {};
  for (const [k, p] of Object.entries(s.joints) as [JointName, Vec][]) {
    const dx = frame.x + fw * (1 - p[0]);
    const dy = frame.y + frame.height * p[1];
    joints[k] = [1 - dx / W, dy / H];
  }
  return { joints, visibility: s.visibility, unit: (s.unit * frame.height) / H };
};

export const guideScene = (disco: Pose): Skeleton => {
  const f = disco.figures[0];
  return frameToScene({ joints: f.joints, visibility: f.visibility, unit: f.unit }, disco.frameAspect, guideFrameFor(disco));
};

// ---- isotropic px helpers -------------------------------------------------------------------------------------
export type PxJoints = Partial<Record<JointName, Vec>>;

export const toPx = (s: Skeleton): PxJoints => {
  const o: PxJoints = {};
  for (const [k, p] of Object.entries(s.joints) as [JointName, Vec][]) o[k] = [p[0] * W, p[1] * H];
  return o;
};
export const fromPx = (j: PxJoints, unitPx: number): Skeleton => {
  const joints: Joints = {};
  for (const [k, p] of Object.entries(j) as [JointName, Vec][]) joints[k] = [p[0] / W, p[1] / H];
  return { joints, unit: unitPx / H };
};

export const midOf = (j: PxJoints, a: JointName, b: JointName): Vec => [(j[a]![0] + j[b]![0]) / 2, (j[a]![1] + j[b]![1]) / 2];

// ---- forward kinematics ---------------------------------------------------------------------------------------
export type Limb = "LEFT_ARM" | "RIGHT_ARM" | "LEFT_LEG" | "RIGHT_LEG";
export const LIMB_CHAIN: Record<Limb, [JointName, JointName, JointName, JointName]> = {
  LEFT_ARM: ["LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_WRIST", "LEFT_HAND"],
  RIGHT_ARM: ["RIGHT_SHOULDER", "RIGHT_ELBOW", "RIGHT_WRIST", "RIGHT_HAND"],
  LEFT_LEG: ["LEFT_HIP", "LEFT_KNEE", "LEFT_ANKLE", "LEFT_FOOT"],
  RIGHT_LEG: ["RIGHT_HIP", "RIGHT_KNEE", "RIGHT_ANKLE", "RIGHT_FOOT"],
};

/** Absolute screen angles (radians, y down: positive = clockwise) and lengths of a limb's three bones. */
export const limbAngles = (j: PxJoints, limb: Limb) => {
  const c = LIMB_CHAIN[limb];
  const angles: number[] = [];
  const lengths: number[] = [];
  for (let i = 0; i < 3; i++) {
    const a = j[c[i]]!, b = j[c[i + 1]]!;
    angles.push(Math.atan2(b[1] - a[1], b[0] - a[0]));
    lengths.push(Math.hypot(b[0] - a[0], b[1] - a[1]));
  }
  return { angles, lengths };
};

/** Writes a limb from its root joint (kept) with absolute angles and lengths. */
export const setLimb = (j: PxJoints, limb: Limb, angles: number[], lengths: number[]): PxJoints => {
  const c = LIMB_CHAIN[limb];
  const o = { ...j };
  let p = o[c[0]]!;
  for (let i = 0; i < 3; i++) {
    p = [p[0] + Math.cos(angles[i]) * lengths[i], p[1] + Math.sin(angles[i]) * lengths[i]];
    o[c[i + 1]] = p;
  }
  return o;
};

const TAU = Math.PI * 2;
/** Angle blend; t may leave 0..1 (a spring's overshoot carries on past the target). spin: screen direction. */
export const lerpAngle = (a0: number, a1: number, t: number, spin: "short" | "cw" | "ccw" = "short") => {
  let d = (((a1 - a0) % TAU) + TAU) % TAU; // 0..2pi clockwise
  if (spin === "short") { if (d > Math.PI) d -= TAU; }
  else if (spin === "ccw") d -= TAU;
  return a0 + d * t;
};

/** Translate every joint of a limb chain (or the whole body) rigidly. */
export const shift = (j: PxJoints, d: Vec, only?: JointName[]): PxJoints => {
  const o: PxJoints = {};
  for (const [k, p] of Object.entries(j) as [JointName, Vec][]) o[k] = !only || only.includes(k) ? [p[0] + d[0], p[1] + d[1]] : p;
  return o;
};

/** Distance move: a body stepping toward or away from a camera at chest height scales about the horizon point. */
export const scaleAbout = (j: PxJoints, s: number, c: Vec): PxJoints => {
  const o: PxJoints = {};
  for (const [k, p] of Object.entries(j) as [JointName, Vec][]) o[k] = [c[0] + (p[0] - c[0]) * s, c[1] + (p[1] - c[1]) * s];
  return o;
};

/** Rotate joints about a pivot (degrees, screen clockwise). */
export const rotateAbout = (j: PxJoints, deg: number, c: Vec, only?: JointName[]): PxJoints => {
  const r = (deg * Math.PI) / 180, cs = Math.cos(r), sn = Math.sin(r);
  const o: PxJoints = {};
  for (const [k, p] of Object.entries(j) as [JointName, Vec][]) {
    if (only && !only.includes(k)) { o[k] = p; continue; }
    const dx = p[0] - c[0], dy = p[1] - c[1];
    o[k] = [c[0] + dx * cs - dy * sn, c[1] + dx * sn + dy * cs];
  }
  return o;
};

/** A template stance as a body in scene px: mid-hip at `hip`, torso length `unitPx` (photo orientation kept). */
export const fitBody = (pose: Pose, hip: Vec, unitPx: number): PxJoints => {
  const f = pose.figures[0];
  const a = pose.frameAspect;
  const hx = ((f.joints.LEFT_HIP![0] + f.joints.RIGHT_HIP![0]) / 2) * a;
  const hy = (f.joints.LEFT_HIP![1] + f.joints.RIGHT_HIP![1]) / 2;
  const k = unitPx / f.unit;
  const o: PxJoints = {};
  for (const [name, p] of Object.entries(f.joints) as [JointName, Vec][]) o[name] = [hip[0] + (p[0] * a - hx) * k, hip[1] + (p[1] - hy) * k];
  return o;
};

/**
 * MediaPipe-like visibility from position: ~0.95 well inside the image, falling to 0.1 at the edge (a landmark
 * outside the image is reported with low visibility).
 */
export const withVisibility = (s: Skeleton): Skeleton => {
  const visibility: Partial<Record<JointName, number>> = {};
  const inner = 0.012; // H units inside the edge where visibility starts to fall
  for (const [k, p] of Object.entries(s.joints) as [JointName, Vec][]) {
    const inX = Math.min(p[0], 1 - p[0]) / SCENE_ASPECT; // distance inside, in H units (negative = outside)
    const inY = Math.min(p[1], 1 - p[1]);
    const d = Math.min(inX, inY);
    const t = Math.min(1, Math.max(0, (inner - d) / inner));
    visibility[k] = Math.round((0.95 - 0.85 * t) * 1000) / 1000;
  }
  return { ...s, visibility };
};

export const visibleOnly = (s: Skeleton, min = 0.5): Skeleton => {
  const joints: Joints = {};
  for (const [k, p] of Object.entries(s.joints) as [JointName, Vec][]) if ((s.visibility?.[k] ?? 1) >= min) joints[k] = p;
  return { ...s, joints };
};
