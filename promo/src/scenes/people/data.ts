/**
 * people/data.ts: what acts II's real people need, read once at build time (pure, no React; events.ts bundles it).
 *
 *   B2                 the first real person (manifest shot b2-alive): plate id, the film camera on her (the plate
 *                      lands from 1.06 by f88, then drifts to 1.02), her mid-hip anchor, and her body on screen on f80
 *                      (B2_BODY_AT_CUT, the frame the hook's rush is solved onto)
 *   CHORUS_SHOTS       the chorus (B3), from public/footage/chorus.json (pose-core PromoChorusMatch.kt: the app's
 *                      PoseMatcher over every library pose on every plate frame, the snap rule, no pose twice): per
 *                      shot the plate, the in-point offset, the guide (scene space, back camera, drawn unflipped),
 *                      its mid-hip, its landing
 *   chorusShotAt(f)    the chorus shot on screen on global frame f
 *   chorusScale(f)     the film camera across the chorus: 1.00 -> 1.08 (f144-248), a 0.3% drift in the drop
 *   guideLanding(f, s) the guide's landing on a cut: { scale, alpha, edgeScale, edgeAlpha } (the veil 1.25x -> 1 and
 *                      0.70 -> 0.34, softLand; the white edge a frame later from 1.32x)
 */
import chorusRaw from "../../../public/footage/chorus.json";
import b2Raw from "../../../public/footage/b2-alive.landmarks.json";
import { theme } from "../../theme";
import { PEOPLE_SHOTS } from "../../beats";
import type { JointName, Skeleton, Vec } from "../../figure/types";
import { drift, keys, tween } from "../../motion";
import { PEOPLE_TIMING } from "../timing";

const { W, H } = theme.size;
const E = theme.ease;

// ---- B2: the figure gets a body ------------------------------------------------------------------------------
type B2File = { filmFrom: number; joints: JointName[]; frames: { n: number; f: number; people: ([number, number, number] | null)[][] }[] };
const b2 = b2Raw as unknown as B2File;

const b2Joint = (f: number, j: JointName): Vec => {
  const fr = b2.frames[f - b2.filmFrom];
  const p = fr.people[0][b2.joints.indexOf(j)]!;
  return [p[0], p[1]];
};
const CUT = PEOPLE_TIMING.cut;
const hip0: Vec = [(b2Joint(CUT, "LEFT_HIP")[0] + b2Joint(CUT, "RIGHT_HIP")[0]) / 2, (b2Joint(CUT, "LEFT_HIP")[1] + b2Joint(CUT, "RIGHT_HIP")[1]) / 2];

export const B2 = {
  plate: "b2-alive",
  /** zoom centre: her mid-hip on the cut, so the landing keeps her where the hook left the skeleton */
  anchor: hip0,
  landFrom: 1.06,
  /** the plate lands from 1.06 to 1.00 by f88 (the hook's rush arrives from oversize), then creeps to 1.02 */
  scale: (f: number) => keys(f, [[CUT, 1.06], [PEOPLE_TIMING.plateLanded, 1, E.softLand], [PEOPLE_TIMING.chorus, 1.02, E.drift]]),
  /** the skeleton the hook carried becomes hers over these frames (softLand) */
  morph: [CUT, CUT + 6] as [number, number],
};

/** Her body on screen on f80 (the plate at 1.06 about her mid-hip): nose, lower ankle, mid-hip x (px). */
export const B2_BODY_AT_CUT = (() => {
  const s = B2.landFrom;
  const toScreen = (p: Vec): Vec => [(hip0[0] + (p[0] - hip0[0]) * s) * W, (hip0[1] + (p[1] - hip0[1]) * s) * H];
  const nose = toScreen(b2Joint(CUT, "NOSE"));
  const ankle = Math.max(toScreen(b2Joint(CUT, "LEFT_ANKLE"))[1], toScreen(b2Joint(CUT, "RIGHT_ANKLE"))[1]);
  return { noseY: nose[1], ankleY: ankle, hipX: hip0[0] * W };
})();

// ---- B3: the chorus ------------------------------------------------------------------------------------------
type RawFigure = { joints: Partial<Record<JointName, Vec>>; visibility: Partial<Record<JointName, number>>; unit: number };
type RawShot = {
  id: string;
  film: [number, number];
  land: number;
  plate: string | null;
  clip?: string;
  offset?: number;
  key?: { n: number; f: number; src: number; sourceTime: number };
  pose: null | {
    poseId: string;
    name: string;
    mirrored: boolean;
    score: number;
    fit: number;
    hold: { mean: number; min: number };
    transform: { scale: number; offsetX: number; offsetY: number; rotation: number };
  };
  figures?: RawFigure[];
};

export type ChorusShot = {
  id: string;
  from: number;
  to: number;
  land: number;
  plate: string;
  /** plate frames to shift by: the plate shows global frame f + offset (inside its 15-frame handles) */
  offset: number;
  keyF: number;
  poseId: string;
  mirrored: boolean;
  score: number;
  guide: Skeleton[];
  /** the guide's mid-hip in scene space: the landing's and the push's centre */
  anchor: Vec;
};

const midHip = (s: Skeleton): Vec => {
  const l = s.joints.LEFT_HIP, r = s.joints.RIGHT_HIP;
  if (l && r) return [(l[0] + r[0]) / 2, (l[1] + r[1]) / 2];
  const a = s.joints.LEFT_SHOULDER!, b = s.joints.RIGHT_SHOULDER!;
  return [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
};

const rawShots = (chorusRaw as unknown as { shots: RawShot[] }).shots;

export const CHORUS_SHOTS: ChorusShot[] = PEOPLE_SHOTS.filter((p) => p.shot !== B2.plate).map((p) => {
  const r = rawShots.find((x) => x.id === p.shot);
  if (!r || !r.pose || !r.plate || !r.figures) throw new Error(`chorus.json: no pose for ${p.shot} (run PromoChorusMatch)`);
  const guide = r.figures.map((g) => ({ joints: g.joints, visibility: g.visibility, unit: g.unit }));
  return {
    id: p.shot,
    from: p.from,
    to: p.to,
    land: r.land,
    plate: r.plate,
    offset: r.offset ?? 0,
    keyF: r.key!.f,
    poseId: r.pose.poseId,
    mirrored: r.pose.mirrored,
    score: r.pose.score,
    guide,
    anchor: midHip(guide[0]),
  };
});

export const chorusShotAt = (f: number): ChorusShot =>
  CHORUS_SHOTS.find((s) => f >= s.from && f < s.to) ?? CHORUS_SHOTS[f < CHORUS_SHOTS[0].from ? 0 : CHORUS_SHOTS.length - 1];

const PUSH = PEOPLE_TIMING.push;
export const chorusScale = (f: number) => {
  const [s0, s1] = theme.footage.chorusPush;
  if (f < PUSH[1]) return drift(f, PUSH, [s0, s1]);
  return s1 * drift(f, [PUSH[1], PEOPLE_TIMING.unison], [1, 1.003]);
};

/**
 * The chorus guide's veil (theme.footage.chorusVeil): the board planned 0.9 -> 0.62, but a white figure at 62% over
 * grey footage hides the person it is fitted to (checked on the render); the veil settles at 0.34 under a crisp white
 * edge, so the person reads through the figure and the figure still reads as the app's lay figure.
 */
export const CHORUS_VEIL = theme.footage.chorusVeil;

/**
 * The landing on a cut, two layers staggered by a frame: the veil lands from 1.25x (softLand over `land` frames)
 * while its opacity settles; the edge follows one frame later from a little further out and closes onto the body
 * (softLand over `land` + 1 frames), so the figure reads as locking on.
 */
export const guideLanding = (f: number, s: ChorusShot) => {
  const a = s.from, b = s.from + s.land;
  return {
    scale: tween(f, [a, b], [theme.footage.guideLandFrom, 1], E.softLand),
    alpha: tween(f, [a, b], [CHORUS_VEIL.land, CHORUS_VEIL.settled], E.softLand),
    edgeScale: f <= a ? theme.footage.guideLandFrom + 0.07 : tween(f, [a + 1, b + 1], [theme.footage.guideLandFrom + 0.07, 1], E.softLand),
    edgeAlpha: tween(f, [a, a + 2], [0.35 * CHORUS_VEIL.edge, CHORUS_VEIL.edge], E.softInOut),
  };
};
