/**
 * performer.ts: the live person in act III (B5-B7), keyframed in the app's scene space (rig.ts). ILLUSTRATIVE: a
 * hand-animated body, not a recording. It is fed frame by frame to the app's real PoseMatcher, HintStabilizer and
 * CaptureCoach (app/src/test/.../promo/PromoMatchExport.kt via scripts/export-live.ts), and the film shows what
 * they return (public/match.json): the score, the limb colours, the hint card, the hold arc and the countdown.
 *
 *   PERFORMER                  the timeline (frames): found, arm, hip hand, steps, settle
 *   performerAt(frame, poses)  scene skeleton with MediaPipe-like visibility, or null before the body is found
 *   bodyFinal(poses)           the matched stance: the guide's pose as a real body would hold it (turned 1 deg,
 *                              a little off to the side, arms and legs a few degrees off)
 *
 * The story (STORYBOARD B5-B7, reordered to what the real matcher says first, see NOTE.md):
 *   f343  the camera finds the body: the stiff stance from the hook, standing too close (1.3x, feet below the frame)
 *   f364  the left arm sweeps up to the guide's raised arm (the film's one spring: about 6 degrees past, settles)
 *   f370  the right hand comes to the hip
 *   f418  two steps back (scale about the horizon, a bob per step), crossing the legs into the pose
 *   f459  settled; breathing sway from here on (the score moves by a point, as a live match does)
 */
import { spring } from "remotion";
import { theme } from "../../theme";
import { blendSkeletons } from "../../figure/interpolate";
import type { JointName, Pose, Skeleton, Vec } from "../../figure/types";
import { keys, sway, tween } from "../../motion";
import {
  SCENE_ASPECT, fitBody, fromPx, guideScene, lerpAngle, limbAngles, midOf, rotateAbout, scaleAbout, setLimb, shift, toPx,
  withVisibility, type Limb, type PxJoints,
} from "./rig";

const { H, fps } = theme.size;
const E = theme.ease;

export type PerformerPoses = { disco: Pose; stiff: Pose };

export const PERFORMER = {
  found: 343, // first frame the detector reports a person (the hint card follows 400 ms later, f355)
  armFrom: 364,
  armFrames: 30, // spring stretched to settle in 30 f
  hipFrom: 370,
  hipTo: 389,
  step1: [418, 439] as [number, number],
  step2: [440, 459] as [number, number],
  near: 1.3, // too close: 1.3x the matched size
  mid: 1.21, // after the first step the feet are still just below the frame
  horizonY: 0.47, // the phone at chest height: a step back scales about this line
};

const FACE: JointName[] = ["NOSE", "LEFT_EYE", "RIGHT_EYE", "LEFT_EAR", "RIGHT_EAR"];
const UPPER: JointName[] = [
  ...FACE, "LEFT_SHOULDER", "RIGHT_SHOULDER", "LEFT_ELBOW", "RIGHT_ELBOW", "LEFT_WRIST", "RIGHT_WRIST", "LEFT_HAND", "RIGHT_HAND",
];

type Rig = { guide: PxJoints; hip: Vec; unitPx: number; stiff: PxJoints; final: PxJoints };
const cache = new WeakMap<Pose, Rig>();

/** The matched stance: the guide as a person holds it (turned, shifted, arms and a knee a few degrees off). */
const makeFinal = (guide: PxJoints, hip: Vec): PxJoints => {
  let j = rotateAbout(guide, 1.0, hip);
  j = shift(j, [0.016 * theme.size.W, 0.004 * H]);
  const tweak = (limb: Limb, d: number[]) => {
    const { angles, lengths } = limbAngles(j, limb);
    j = setLimb(j, limb, angles.map((a, i) => a + (d[i] * Math.PI) / 180), lengths);
  };
  tweak("LEFT_ARM", [11, 10, 4]); // the raised arm a little lower and softer than the drawing
  tweak("RIGHT_ARM", [-8, -22, 0]); // the hip hand sits higher on the waist than drawn (the main error)
  tweak("LEFT_LEG", [3, -5, 0]);
  tweak("RIGHT_LEG", [-3, 5, 0]);
  return j;
};

const rigFor = (poses: PerformerPoses): Rig => {
  const hit = cache.get(poses.disco);
  if (hit) return hit;
  const g = guideScene(poses.disco);
  const guide = toPx(g);
  const hip = midOf(guide, "LEFT_HIP", "RIGHT_HIP");
  const unitPx = g.unit * H;
  const stiff = fitBody(poses.stiff, hip, unitPx);
  const rig = { guide, hip, unitPx, stiff, final: makeFinal(guide, hip) };
  cache.set(poses.disco, rig);
  return rig;
};

export const bodyFinal = (poses: PerformerPoses): Skeleton => {
  const r = rigFor(poses);
  return fromPx(r.final, r.unitPx);
};

/** The left arm's sweep parameter: Remotion spring, theme.spring.theOne (overshoots ~4%, i.e. ~6 deg on this arc). */
export const armT = (frame: number) =>
  frame < PERFORMER.armFrom ? 0 : spring({ frame: frame - PERFORMER.armFrom, fps, config: theme.spring.theOne, durationInFrames: PERFORMER.armFrames });

const bump = (frame: number, [a, b]: [number, number]) => (frame <= a || frame >= b ? 0 : Math.sin((Math.PI * (frame - a)) / (b - a)));

/** Body pose in body space (mid-hip at the guide's hip, matched size), before distance and sway. */
const bodyAt = (frame: number, r: Rig): PxJoints => {
  const P = PERFORMER;
  // torso + head: stiff -> final while the legs cross (second step)
  const tBody = tween(frame, P.step2, [0, 1], E.softInOut);
  const blend = blendSkeletons(fromPx(r.stiff, r.unitPx), fromPx(r.final, r.unitPx), tBody, { aspect: SCENE_ASPECT });
  let j = toPx(blend);

  // left arm: the spring sweep, out and up (short way through the horizontal)
  {
    const a = limbAngles(r.stiff, "LEFT_ARM"), b = limbAngles(r.final, "LEFT_ARM");
    const t = armT(frame);
    // the forearm and hand trail the upper arm a little (follow-through), then catch up
    const lag = (k: number) => Math.max(0, t - k * (1 - Math.min(1, t)));
    j = setLimb(j, "LEFT_ARM", [lerpAngle(a.angles[0], b.angles[0], t), lerpAngle(a.angles[1], b.angles[1], lag(0.18)), lerpAngle(a.angles[2], b.angles[2], lag(0.3))], b.lengths);
  }
  // right arm: hand to hip (upper arm swings out, elbow bends in)
  {
    const a = limbAngles(r.stiff, "RIGHT_ARM"), b = limbAngles(r.final, "RIGHT_ARM");
    const t = tween(frame, [P.hipFrom, P.hipTo], [0, 1], E.softLand);
    j = setLimb(j, "RIGHT_ARM", a.angles.map((x, i) => lerpAngle(x, b.angles[i], t)), b.lengths);
  }
  // legs: straight -> crossed during the second step; each step lifts one foot (foreshortened shin)
  for (const [limb, step] of [["RIGHT_LEG", P.step1], ["LEFT_LEG", P.step2]] as [Limb, [number, number]][]) {
    const a = limbAngles(r.stiff, limb), b = limbAngles(r.final, limb);
    const t = tween(frame, P.step2, [0, 1], E.softInOut);
    const lift = bump(frame, step);
    const lengths = b.lengths.map((l, i) => l * (1 - (i === 1 ? 0.16 : i === 0 ? 0.05 : 0) * lift));
    j = setLimb(j, limb, a.angles.map((x, i) => lerpAngle(x, b.angles[i], t)), lengths);
  }
  return j;
};

export const performerAt = (frame: number, poses: PerformerPoses): Skeleton | null => {
  const P = PERFORMER;
  if (frame < P.found) return null;
  const r = rigFor(poses);
  let j = bodyAt(frame, r);

  // fidget while standing stiff (the hook's callback): head turns, weight shifts
  const stiffness = 1 - tween(frame, [P.armFrom, P.armFrom + 10], [0, 1], E.softInOut);
  if (stiffness > 0) {
    const turn = keys(frame, [[343, -0.1], [352, 0.12, E.softInOut], [362, 0.0, E.softInOut]]) * r.unitPx * stiffness;
    j = shift(j, [turn, 0], FACE);
  }
  const weight = keys(frame, [[343, 0.6], [356, -0.5, E.softInOut], [380, 0.3, E.softInOut], [404, -0.2, E.softInOut], [418, 0, E.softInOut]]);
  const ankle = midOf(j, "LEFT_ANKLE", "RIGHT_ANKLE");
  j = rotateAbout(j, weight, ankle);

  // the raised arm drifts a few degrees once it is up (a held arm is never still)
  if (frame > P.armFrom + P.armFrames) {
    const { angles, lengths } = limbAngles(j, "LEFT_ARM");
    const d = (sway(frame, 3.2, 58, 7) * Math.PI) / 180;
    j = setLimb(j, "LEFT_ARM", angles.map((a, i) => a + d * (i === 0 ? 1 : 1.3)), lengths);
  }
  // breathing (alive from the start)
  const breath = sway(frame, 0.012 * r.unitPx, 52);
  j = shift(j, [0, breath], UPPER);
  j = shift(j, [sway(frame, 0.008 * r.unitPx, 76, 11), 0]);

  // distance: too close, then two steps back (scale about the horizon point, a bob per step)
  const s = keys(frame, [[P.step1[0], P.near], [P.step1[1], P.mid, E.softInOut], [P.step2[1], 1, E.softInOut]]);
  const bob = -(bump(frame, P.step1) * 0.045 + bump(frame, P.step2) * 0.04) * r.unitPx;
  j = shift(scaleAbout(j, s, [r.hip[0], P.horizonY * H]), [0, bob]);

  return withVisibility(fromPx(j, r.unitPx * s));
};
