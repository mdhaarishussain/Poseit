/**
 * data.ts: the derived footage data and how to read it. Pure, no React (the hooks are in hooks.ts).
 *
 * public/footage/<plate>.landmarks.json (pose-core PromoFootageLandmarks.kt over scripts/detect.py): per plate frame
 * the 33 raw MediaPipe landmarks and the app's smoothed skeletons (LandmarkMapping.toScene + LandmarkSmoother), in
 * the plate's scene space. public/footage/<plate>.match.json (app PromoFootageMatch.kt): per film frame the app's
 * PoseMatcher / HintStabilizer / CaptureCoach verdicts against the plate's guide, the guide itself in scene space and
 * the events; the same records as v1's match.json. public/footage/nearest.json: the library poses nearest to each
 * chorus person.
 *
 *   peopleAt(lm, n)            the smoothed app skeletons on plate frame n (Skeleton, scene coordinates, unit 0)
 *   drawable(s)                the joints the app's overlay draws (visibility >= 0.5, CameraViewModel.visibleOnly)
 *   rawAt(lm, n)               the 33 raw landmarks per person [x, y, z, visibility, presence]
 *   matchRecordAt(m, f)        the verdict on global frame f (clamped to the exported range)
 *   guideFigures(m)            the guide's figures in scene space (draw with aspect 9:16 over the plate)
 *   partColors(record)         the guide's per-part colours, Studio.forScore on the dark live view, as CameraOverlay
 */
import { theme } from "../theme";
import type { BodyPart, JointName, Skeleton, Vec } from "../figure/types";

export type LandmarkFrame = {
  n: number;
  f: number;
  src: number;
  t: number;
  raw: number[][][];
  people: ([number, number, number] | null)[][];
};

export type LandmarksFile = {
  source: string;
  plate: string;
  shot: string;
  catalogue: string;
  film: [number, number];
  filmFrom: number;
  shotStart: number;
  fps: number;
  aspect: number;
  image: [number, number];
  model: string;
  runningMode: string;
  numPoses: number;
  smoother: Record<string, number>;
  joints: JointName[];
  landmarkNames: string[];
  summary: { frames: number; detectionRate: number; shotDetectionRate: number; peopleMax: number; firstDetected: number };
  frames: LandmarkFrame[];
};

export type HintKind = "NO_PERSON" | "MISSING_PERSON" | "FRAMING" | "VISIBILITY" | "POSE" | "PERFECT";
export type CaptureRecord =
  | { state: "Idle" }
  | { state: "Holding"; progress: number }
  | { state: "Countdown"; seconds: number; progress: number }
  | { state: "Capture" }
  | { state: "Cooldown"; progress: number };

export type MatchRecord = {
  f: number;
  n: number;
  src: number;
  score: number;
  percent: number | null;
  assigned: boolean;
  parts: Partial<Record<BodyPart, number>>;
  segments: Record<string, number>;
  hints: { text: string; kind: HintKind; severity: number }[];
  shown: { text: string; kind: HintKind } | null;
  capture: CaptureRecord;
};

export type OverlayTransform = { scale: number; offsetX: number; offsetY: number; rotation: number };

export type MatchEvents = {
  found: number | null;
  matchFrom: number | null;
  crossings: { f: number; match: boolean }[];
  cards: { f: number; text: string; kind: HintKind }[];
  hold: number | null;
  countdown: Partial<Record<"1" | "2" | "3", number>>;
  capture: number | null;
};

export type FootageMatchFile = {
  source: string;
  plate: string;
  shot: string;
  aspect: number;
  fps: number;
  camera: { lens: "BACK" | "FRONT"; mirroredView: boolean; audience: "SUBJECT" | "PHOTOGRAPHER" };
  coach: { threshold: number; holdMillis: number; countdownSeconds: number; abortMargin: number; clock: string };
  guide: {
    origin: "library" | "snap" | "photo";
    poseId: string;
    name: string;
    mirrored: boolean;
    frameAspect: number;
    transform: OverlayTransform;
    definedAt: { plate: string; n: number; f: number; src: number };
    figures: { joints: Partial<Record<JointName, Vec>>; visibility: Partial<Record<JointName, number>>; unit: number }[];
  };
  events: MatchEvents;
  attempts?: { poseId: string; threshold: number; plate: string; transform: OverlayTransform; chosen: boolean; events: MatchEvents }[];
  frames: MatchRecord[];
};

/** CameraViewModel.LIVE_MIN_VISIBILITY */
export const LIVE_MIN_VISIBILITY = 0.5;

const frameOf = (lm: LandmarksFile, n: number): LandmarkFrame =>
  lm.frames[Math.max(0, Math.min(lm.frames.length - 1, Math.floor(n)))];

export const peopleAt = (lm: LandmarksFile, n: number): Skeleton[] =>
  frameOf(lm, n).people.map((p) => {
    const joints: Skeleton["joints"] = {};
    const visibility: NonNullable<Skeleton["visibility"]> = {};
    p.forEach((j, k) => {
      if (!j) return;
      joints[lm.joints[k]] = [j[0], j[1]];
      visibility[lm.joints[k]] = j[2];
    });
    return { joints, visibility, unit: 0 };
  });

export const drawable = (s: Skeleton, min = LIVE_MIN_VISIBILITY): Skeleton => ({
  ...s,
  joints: Object.fromEntries(
    Object.entries(s.joints).filter(([j]) => (s.visibility?.[j as JointName] ?? 1) >= min),
  ) as Skeleton["joints"],
});

export const rawAt = (lm: LandmarksFile, n: number): number[][][] => frameOf(lm, n).raw;

export const matchRecordAt = (m: FootageMatchFile, f: number): MatchRecord => {
  const first = m.frames[0].f;
  return m.frames[Math.max(0, Math.min(m.frames.length - 1, Math.floor(f) - first))];
};

export const guideFigures = (m: FootageMatchFile): Skeleton[] =>
  m.guide.figures.map((g) => ({ joints: g.joints, visibility: g.visibility, unit: g.unit }));

export const partColors = (r: MatchRecord): Partial<Record<BodyPart, string>> => {
  if (!r.assigned) return {};
  const o: Partial<Record<BodyPart, string>> = {};
  for (const [k, v] of Object.entries(r.parts) as [BodyPart, number][]) o[k] = theme.signal.forScore(v, true);
  return o;
};
