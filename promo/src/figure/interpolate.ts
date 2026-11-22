/**
 * interpolate.ts: blend skeletons joint by joint without limbs shrinking mid-blend.
 *
 *   blendSkeletons(a, b, t, opts?)   t 0..1 (already eased, or pass opts.ease). opts.aspect = frame width/height
 *                                    (both skeletons in the same frame). opts.mode:
 *                                      "chain" (default): the hip centre moves in a straight line; the torso, the
 *                                      shoulder and hip offsets, every limb bone and the face offsets rotate along
 *                                      the kinematic chain (angle + length lerp), so a limb sweeps an arc at full
 *                                      length instead of folding through its middle;
 *                                      "linear": raw joint positions (for tiny corrections only).
 *                                    opts.spin = { LEFT_UPPER_ARM: "cw" | "ccw" | "short" ... } forces the direction
 *                                    of a segment's rotation (screen clockwise, y down); default the short way.
 *   skeletonAt(frame, keys, opts?)   keyframed skeleton: keys [{ f, skeleton, ease? }] sorted by f; the ease of a
 *                                    key shapes the segment ENDING at it (theme.ease.*; default softLand). Clamped
 *                                    before the first and after the last key. opts.twos quantises to even frames
 *                                    (the posed clock); other blend opts pass through.
 *   flipForDisplay(s)                x -> 1 - x, labels kept (front-camera preview; the app's flippedForDisplay)
 *   mirrored(s)                      x -> 1 - x AND left/right swapped (an anatomically valid mirror pose)
 *   mapJoints(s, fn)                 transform every joint position (frame coordinates)
 *   withJoints(s, overrides)         replace some joints
 *   reframe(s, from, to)             move a skeleton between frames: from/to = { x, y, w, h } rectangles in a
 *                                    shared space; returns joints normalised to `to` (unit rescaled)
 */
import { theme } from "../theme";
import type { JointName, Joints, Skeleton, Vec } from "./types";

type Spin = "short" | "cw" | "ccw";
export type Segment =
  | "TORSO" | "LEFT_SHOULDER" | "RIGHT_SHOULDER" | "LEFT_HIP" | "RIGHT_HIP"
  | "LEFT_UPPER_ARM" | "LEFT_FOREARM" | "LEFT_HAND" | "RIGHT_UPPER_ARM" | "RIGHT_FOREARM" | "RIGHT_HAND"
  | "LEFT_THIGH" | "LEFT_SHIN" | "LEFT_FOOT" | "RIGHT_THIGH" | "RIGHT_SHIN" | "RIGHT_FOOT"
  | "NOSE" | "LEFT_EYE" | "RIGHT_EYE" | "LEFT_EAR" | "RIGHT_EAR";

export type BlendOptions = {
  aspect?: number;
  mode?: "chain" | "linear";
  spin?: Partial<Record<Segment, Spin>>;
  ease?: (t: number) => number;
};

const lerp = (a: number, b: number, t: number) => a + (b - a) * t;
const lerpV = (a: Vec, b: Vec, t: number): Vec => [lerp(a[0], b[0], t), lerp(a[1], b[1], t)];
const mid = (a: Vec, b: Vec): Vec => [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
const TAU = Math.PI * 2;

/** Rotate-and-stretch blend of two vectors (screen space, y down: positive angle = clockwise on screen). */
const polar = (va: Vec, vb: Vec, t: number, spin: Spin = "short"): Vec => {
  const la = Math.hypot(va[0], va[1]);
  const lb = Math.hypot(vb[0], vb[1]);
  if (la < 1e-9 || lb < 1e-9) return lerpV(va, vb, t);
  const aa = Math.atan2(va[1], va[0]);
  let d = Math.atan2(vb[1], vb[0]) - aa;
  d = ((d % TAU) + TAU) % TAU; // 0..2pi, clockwise
  if (spin === "short") { if (d > Math.PI) d -= TAU; }
  else if (spin === "ccw") d -= TAU;
  const ang = aa + d * t;
  const len = lerp(la, lb, t);
  return [Math.cos(ang) * len, Math.sin(ang) * len];
};

const CHAINS: [Segment, JointName, JointName][] = [
  ["LEFT_UPPER_ARM", "LEFT_SHOULDER", "LEFT_ELBOW"],
  ["LEFT_FOREARM", "LEFT_ELBOW", "LEFT_WRIST"],
  ["LEFT_HAND", "LEFT_WRIST", "LEFT_HAND"],
  ["RIGHT_UPPER_ARM", "RIGHT_SHOULDER", "RIGHT_ELBOW"],
  ["RIGHT_FOREARM", "RIGHT_ELBOW", "RIGHT_WRIST"],
  ["RIGHT_HAND", "RIGHT_WRIST", "RIGHT_HAND"],
  ["LEFT_THIGH", "LEFT_HIP", "LEFT_KNEE"],
  ["LEFT_SHIN", "LEFT_KNEE", "LEFT_ANKLE"],
  ["LEFT_FOOT", "LEFT_ANKLE", "LEFT_FOOT"],
  ["RIGHT_THIGH", "RIGHT_HIP", "RIGHT_KNEE"],
  ["RIGHT_SHIN", "RIGHT_KNEE", "RIGHT_ANKLE"],
  ["RIGHT_FOOT", "RIGHT_ANKLE", "RIGHT_FOOT"],
];
const FACE: JointName[] = ["NOSE", "LEFT_EYE", "RIGHT_EYE", "LEFT_EAR", "RIGHT_EAR"];

const iso = (j: Joints, aspect: number): Joints => {
  const o: Joints = {};
  for (const [k, p] of Object.entries(j) as [JointName, Vec][]) o[k] = [p[0] * aspect, p[1]];
  return o;
};
const deIso = (j: Joints, aspect: number): Joints => {
  const o: Joints = {};
  for (const [k, p] of Object.entries(j) as [JointName, Vec][]) o[k] = [p[0] / aspect, p[1]];
  return o;
};

const blendLinear = (a: Joints, b: Joints, t: number): Joints => {
  const o: Joints = {};
  const keys = new Set([...Object.keys(a), ...Object.keys(b)] as JointName[]);
  for (const k of keys) {
    const pa = a[k], pb = b[k];
    o[k] = pa && pb ? lerpV(pa, pb, t) : (pa ?? pb)!;
  }
  return o;
};

export const blendSkeletons = (a: Skeleton, b: Skeleton, tRaw: number, opts: BlendOptions = {}): Skeleton => {
  const t = Math.min(1, Math.max(0, opts.ease ? opts.ease(tRaw) : tRaw));
  const aspect = opts.aspect ?? 0.75;
  const unit = a.unit > 0 && b.unit > 0 ? lerp(a.unit, b.unit, t) : a.unit || b.unit;
  const visibility: Partial<Record<JointName, number>> = {};
  for (const k of new Set([...Object.keys(a.visibility ?? {}), ...Object.keys(b.visibility ?? {})] as JointName[])) {
    visibility[k] = lerp(a.visibility?.[k] ?? 1, b.visibility?.[k] ?? 1, t);
  }
  if (t <= 0) return { ...a, visibility: a.visibility };
  if (t >= 1) return { ...b, visibility: b.visibility };

  const A = iso(a.joints, aspect);
  const B = iso(b.joints, aspect);
  const lin = blendLinear(A, B, t);
  if (opts.mode === "linear") return { joints: deIso(lin, aspect), visibility, unit };

  const need: JointName[] = ["LEFT_SHOULDER", "RIGHT_SHOULDER", "LEFT_HIP", "RIGHT_HIP"];
  if (!need.every((j) => A[j] && B[j])) return { joints: deIso(lin, aspect), visibility, unit };

  const spin = opts.spin ?? {};
  const out: Joints = { ...lin };
  const hipA = mid(A.LEFT_HIP!, A.RIGHT_HIP!), hipB = mid(B.LEFT_HIP!, B.RIGHT_HIP!);
  const shA = mid(A.LEFT_SHOULDER!, A.RIGHT_SHOULDER!), shB = mid(B.LEFT_SHOULDER!, B.RIGHT_SHOULDER!);
  const hip = lerpV(hipA, hipB, t);
  const torso = polar([shA[0] - hipA[0], shA[1] - hipA[1]], [shB[0] - hipB[0], shB[1] - hipB[1]], t, spin.TORSO);
  const sh: Vec = [hip[0] + torso[0], hip[1] + torso[1]];
  const offset = (j: JointName, centerA: Vec, centerB: Vec, center: Vec, seg: Segment) => {
    const pa = A[j], pb = B[j];
    if (!pa || !pb) return;
    const v = polar([pa[0] - centerA[0], pa[1] - centerA[1]], [pb[0] - centerB[0], pb[1] - centerB[1]], t, spin[seg]);
    out[j] = [center[0] + v[0], center[1] + v[1]];
  };
  offset("LEFT_SHOULDER", shA, shB, sh, "LEFT_SHOULDER");
  offset("RIGHT_SHOULDER", shA, shB, sh, "RIGHT_SHOULDER");
  offset("LEFT_HIP", hipA, hipB, hip, "LEFT_HIP");
  offset("RIGHT_HIP", hipA, hipB, hip, "RIGHT_HIP");
  for (const j of FACE) offset(j, shA, shB, sh, j as Segment);
  for (const [seg, from, to] of CHAINS) {
    const fa = A[from], fb = B[from], ta = A[to], tb = B[to], base = out[from];
    if (!fa || !fb || !ta || !tb || !base) continue;
    const v = polar([ta[0] - fa[0], ta[1] - fa[1]], [tb[0] - fb[0], tb[1] - fb[1]], t, spin[seg]);
    out[to] = [base[0] + v[0], base[1] + v[1]];
  }
  return { joints: deIso(out, aspect), visibility, unit };
};

export type SkeletonKey = { f: number; skeleton: Skeleton; ease?: (t: number) => number; spin?: BlendOptions["spin"] };

export const skeletonAt = (frame: number, keys: SkeletonKey[], opts: BlendOptions & { twos?: boolean } = {}): Skeleton => {
  if (keys.length === 0) throw new Error("skeletonAt: no keys");
  const f = opts.twos ? Math.floor(frame / 2) * 2 : frame;
  if (f <= keys[0].f) return keys[0].skeleton;
  const last = keys[keys.length - 1];
  if (f >= last.f) return last.skeleton;
  let i = 1;
  while (keys[i].f < f) i++;
  const k0 = keys[i - 1], k1 = keys[i];
  const t = (f - k0.f) / Math.max(1e-9, k1.f - k0.f);
  const e = k1.ease ?? theme.ease.softLand;
  return blendSkeletons(k0.skeleton, k1.skeleton, e(t), { ...opts, ease: undefined, spin: k1.spin ?? opts.spin });
};

export const mapJoints = (s: Skeleton, fn: (p: Vec, j: JointName) => Vec): Skeleton => {
  const joints: Joints = {};
  for (const [k, p] of Object.entries(s.joints) as [JointName, Vec][]) joints[k] = fn(p, k);
  return { ...s, joints };
};

export const flipForDisplay = (s: Skeleton): Skeleton => mapJoints(s, (p) => [1 - p[0], p[1]]);

const OPPOSITE = (j: JointName): JointName =>
  (j.startsWith("LEFT_") ? j.replace("LEFT_", "RIGHT_") : j.startsWith("RIGHT_") ? j.replace("RIGHT_", "LEFT_") : j) as JointName;

export const mirrored = (s: Skeleton): Skeleton => {
  const joints: Joints = {};
  const visibility: Partial<Record<JointName, number>> = {};
  for (const [k, p] of Object.entries(s.joints) as [JointName, Vec][]) joints[OPPOSITE(k)] = [1 - p[0], p[1]];
  for (const [k, v] of Object.entries(s.visibility ?? {}) as [JointName, number][]) visibility[OPPOSITE(k)] = v;
  return { joints, visibility, unit: s.unit };
};

export const withJoints = (s: Skeleton, overrides: Joints): Skeleton => ({ ...s, joints: { ...s.joints, ...overrides } });

type Rect = { x: number; y: number; w: number; h: number };
export const reframe = (s: Skeleton, from: Rect, to: Rect): Skeleton => ({
  ...mapJoints(s, (p) => [(from.x + p[0] * from.w - to.x) / to.w, (from.y + p[1] * from.h - to.y) / to.h]),
  unit: s.unit * (from.h / to.h),
});
