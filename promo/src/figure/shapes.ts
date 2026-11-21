/**
 * shapes.ts: TypeScript port of pose-core's FigureShapes (com.poseit.app.pose.render.FigureShapes) and the bits of
 * Skeleton it needs (toIsotropic, estimatedUnit). Same constants, same draw order, so a blended or keyframed
 * skeleton gets exactly the app's mannequin geometry. `npm run check` compares this port against the Kotlin output
 * exported in public/poses.json for all 99 poses.
 *
 *   buildShapes(skeleton, aspect): Shape[]           torso polygon + spine + shoulder/hip/flank capsules, legs, arms,
 *                                                     neck, head circle; isotropic height units
 *   noseMarker(skeleton, aspect): {c, r} | null       the face dot (only when the nose is visible >= 0.5)
 *   headCenter(iso, unit, midShoulder): Vec | null
 *   estimatedUnit(skeleton, aspect): number            body unit in frame heights
 *   toIsotropic(skeleton, aspect): Joints              x * aspect
 *   shapeBounds(shapes): {minX, minY, maxX, maxY}      ink bounds (radii included), isotropic units
 *   BONES                                              [name, from, to, part] in pose-core's Bone order
 *
 * Pure module: no React, no Remotion (so Node can run the parity check with --experimental-strip-types).
 */
import type { BodyPart, BoneName, JointName, Joints, Shape, Skeleton, Vec } from "./types";

export const PROPORTIONS = {
  TORSO: 1.0,
  NECK: 0.14,
  HEAD_RADIUS: 0.23,
  SHOULDER_HALF: 0.38,
  HIP_HALF: 0.19,
  UPPER_ARM: 0.6,
  FOREARM: 0.52,
  HAND: 0.2,
  THIGH: 0.88,
  SHIN: 0.84,
  FOOT: 0.26,
  /** Feet to top of head, body units. */
  STANDING_HEIGHT: 0.88 + 0.84 + 0.06 + 1.0 + 0.14 + 2 * 0.23 + 0.04,
};

export const BONES: [BoneName, JointName, JointName, BodyPart][] = [
  ["SHOULDERS", "LEFT_SHOULDER", "RIGHT_SHOULDER", "TORSO"],
  ["HIPS", "LEFT_HIP", "RIGHT_HIP", "TORSO"],
  ["LEFT_FLANK", "LEFT_SHOULDER", "LEFT_HIP", "TORSO"],
  ["RIGHT_FLANK", "RIGHT_SHOULDER", "RIGHT_HIP", "TORSO"],
  ["LEFT_UPPER_ARM", "LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_ARM"],
  ["LEFT_FOREARM", "LEFT_ELBOW", "LEFT_WRIST", "LEFT_ARM"],
  ["LEFT_HAND", "LEFT_WRIST", "LEFT_HAND", "LEFT_ARM"],
  ["RIGHT_UPPER_ARM", "RIGHT_SHOULDER", "RIGHT_ELBOW", "RIGHT_ARM"],
  ["RIGHT_FOREARM", "RIGHT_ELBOW", "RIGHT_WRIST", "RIGHT_ARM"],
  ["RIGHT_HAND", "RIGHT_WRIST", "RIGHT_HAND", "RIGHT_ARM"],
  ["LEFT_THIGH", "LEFT_HIP", "LEFT_KNEE", "LEFT_LEG"],
  ["LEFT_SHIN", "LEFT_KNEE", "LEFT_ANKLE", "LEFT_LEG"],
  ["LEFT_FOOT", "LEFT_ANKLE", "LEFT_FOOT", "LEFT_LEG"],
  ["RIGHT_THIGH", "RIGHT_HIP", "RIGHT_KNEE", "RIGHT_LEG"],
  ["RIGHT_SHIN", "RIGHT_KNEE", "RIGHT_ANKLE", "RIGHT_LEG"],
  ["RIGHT_FOOT", "RIGHT_ANKLE", "RIGHT_FOOT", "RIGHT_LEG"],
];
const BONE = Object.fromEntries(BONES.map((b) => [b[0], b])) as Record<BoneName, (typeof BONES)[number]>;

// ---- vector helpers -----------------------------------------------------------------------------------------
const add = (a: Vec, b: Vec): Vec => [a[0] + b[0], a[1] + b[1]];
const sub = (a: Vec, b: Vec): Vec => [a[0] - b[0], a[1] - b[1]];
const mul = (a: Vec, s: number): Vec => [a[0] * s, a[1] * s];
const mid = (a: Vec, b: Vec): Vec => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
const dist = (a: Vec, b: Vec) => Math.hypot(a[0] - b[0], a[1] - b[1]);

export const toIsotropic = (s: Skeleton, aspect: number): Joints => {
  const out: Joints = {};
  for (const [j, p] of Object.entries(s.joints) as [JointName, Vec][]) out[j] = [p[0] * aspect, p[1]];
  return out;
};

export const estimatedUnit = (s: Skeleton, aspect: number): number => {
  if (s.unit > 0) return s.unit;
  const iso = toIsotropic(s, aspect);
  const c: number[] = [];
  const pair = (a: JointName, b: JointName) => (iso[a] && iso[b] ? ([iso[a]!, iso[b]!] as const) : null);
  const sh = pair("LEFT_SHOULDER", "RIGHT_SHOULDER");
  const hp = pair("LEFT_HIP", "RIGHT_HIP");
  if (sh && hp) c.push(dist(mid(sh[0], sh[1]), mid(hp[0], hp[1])));
  if (sh) c.push(dist(sh[0], sh[1]) / 0.76);
  const ears = pair("LEFT_EAR", "RIGHT_EAR");
  if (ears) c.push(dist(ears[0], ears[1]) / 0.42);
  const la = pair("LEFT_SHOULDER", "LEFT_ELBOW");
  if (la) c.push(dist(la[0], la[1]) / PROPORTIONS.UPPER_ARM);
  const ra = pair("RIGHT_SHOULDER", "RIGHT_ELBOW");
  if (ra) c.push(dist(ra[0], ra[1]) / PROPORTIONS.UPPER_ARM);
  const m = c.length ? Math.max(...c) : 0;
  return m > 1e-4 ? m : 0.15;
};

export const isVisible = (s: Skeleton, j: JointName, min = 0.5) =>
  s.joints[j] !== undefined && (s.visibility?.[j] ?? 1) >= min;

export const headCenter = (p: Joints, u: number, midShoulder: Vec | null): Vec | null => {
  const le = p.LEFT_EAR, re = p.RIGHT_EAR, nose = p.NOSE, leye = p.LEFT_EYE, reye = p.RIGHT_EYE;
  if (le && re) return add(mid(le, re), [0, -0.02 * u]); // ears sit slightly below the skull's centre
  if (leye && reye && nose) return add(mul(mid(leye, reye), 0.5), mul(nose, 0.5));
  if (nose) return nose;
  if (midShoulder) return add(midShoulder, [0, -(PROPORTIONS.NECK + PROPORTIONS.HEAD_RADIUS) * u]);
  return null;
};

/** FigureShapes.build: torso first, then legs, arms, head (back limbs first is not knowable in 2D). */
export const buildShapes = (s: Skeleton, aspect: number): Shape[] => {
  const p = toIsotropic(s, aspect);
  const u = estimatedUnit(s, aspect);
  const out: Shape[] = [];
  const ls = p.LEFT_SHOULDER, rs = p.RIGHT_SHOULDER, lh = p.LEFT_HIP, rh = p.RIGHT_HIP;
  const midShoulder = ls && rs ? mid(ls, rs) : (ls ?? rs ?? null);
  const midHip = lh && rh ? mid(lh, rh) : (lh ?? rh ?? null);
  const capsule = (a: Vec, b: Vec, wa: number, wb: number, part: BodyPart, bone: BoneName | null): Shape =>
    ({ t: "capsule", a, b, wa: wa * u, wb: wb * u, part, bone });

  // Torso: quad + a thick spine so profile views still have depth.
  if (ls && rs && lh && rh) out.push({ t: "polygon", pts: [ls, rs, rh, lh], part: "TORSO", bone: null });
  if (midShoulder && midHip) out.push(capsule(midShoulder, midHip, 0.44, 0.4, "TORSO", null));
  if (ls && rs) out.push(capsule(ls, rs, 0.2, 0.2, "TORSO", "SHOULDERS"));
  if (lh && rh) out.push(capsule(lh, rh, 0.26, 0.26, "TORSO", "HIPS"));
  if (ls && lh) out.push(capsule(ls, lh, 0.2, 0.24, "TORSO", "LEFT_FLANK"));
  if (rs && rh) out.push(capsule(rs, rh, 0.2, 0.24, "TORSO", "RIGHT_FLANK"));

  const cap = (bone: BoneName, wa: number, wb: number) => {
    const [, from, to, part] = BONE[bone];
    const a = p[from], b = p[to];
    if (a && b) out.push(capsule(a, b, wa, wb, part, bone));
  };
  cap("LEFT_THIGH", 0.27, 0.19); cap("LEFT_SHIN", 0.18, 0.12); cap("LEFT_FOOT", 0.12, 0.09);
  cap("RIGHT_THIGH", 0.27, 0.19); cap("RIGHT_SHIN", 0.18, 0.12); cap("RIGHT_FOOT", 0.12, 0.09);
  cap("LEFT_UPPER_ARM", 0.17, 0.14); cap("LEFT_FOREARM", 0.14, 0.11); cap("LEFT_HAND", 0.11, 0.08);
  cap("RIGHT_UPPER_ARM", 0.17, 0.14); cap("RIGHT_FOREARM", 0.14, 0.11); cap("RIGHT_HAND", 0.11, 0.08);

  const head = headCenter(p, u, midShoulder);
  if (head) {
    if (midShoulder) {
      const neckEnd = add(midShoulder, mul(sub(head, midShoulder), 0.55));
      out.push(capsule(midShoulder, neckEnd, 0.15, 0.13, "HEAD", null));
    }
    out.push({ t: "circle", c: head, r: PROPORTIONS.HEAD_RADIUS * u, part: "HEAD", bone: null });
  }
  return out;
};

/** The face dot: which way the face points (drawn on top of the head), or null. */
export const noseMarker = (s: Skeleton, aspect: number): { c: Vec; r: number } | null => {
  if (!isVisible(s, "NOSE")) return null;
  const nose = toIsotropic(s, aspect).NOSE;
  if (!nose) return null;
  return { c: nose, r: 0.045 * estimatedUnit(s, aspect) };
};

/** Ink bounds of shapes (isotropic units), radii included. */
export const shapeBounds = (shapes: Shape[]) => {
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  const grow = (x: number, y: number, r: number) => {
    minX = Math.min(minX, x - r); maxX = Math.max(maxX, x + r);
    minY = Math.min(minY, y - r); maxY = Math.max(maxY, y + r);
  };
  for (const s of shapes) {
    if (s.t === "circle") grow(s.c[0], s.c[1], s.r);
    else if (s.t === "capsule") { grow(s.a[0], s.a[1], s.wa / 2); grow(s.b[0], s.b[1], s.wb / 2); }
    else for (const q of s.pts) grow(q[0], q[1], 0);
  }
  return { minX, minY, maxX, maxY };
};
