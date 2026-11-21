/**
 * match.ts: the app's real verdicts for act IV, read from public/footage/b5-live.match.json (written by the app-module
 * test PromoFootageMatch.kt, `npm run footage-match`): the app's PoseMatcher + HintStabilizer + CaptureCoach run frame
 * by frame at 30 fps over the app's own smoothed detections of the real woman on the b5-live plate, against the
 * "Hands in pockets" guide placed with SceneMapping and an OverlayTransform. Back camera, mirroredView = false,
 * PHOTOGRAPHER audience, strictness 0.75 ("Balanced"; 0.80 never locks on this footage, see NOTE.md), 700 ms hold,
 * 3 s countdown. Bundled (static import), so the events are known at module scope for shots.ts and events.ts.
 *
 *   matchAt(frame)     the record for a frame of act IV (clamped to the exported range f336-569)
 *   MATCH_EVENTS       frames derived from the records: found (first person), card1/card2/perfect (the displayed
 *                      hint changes), hold (Holding starts: the lock), countdown + digits ({v, from} per second) and
 *                      digit3/digit2/digit1, capture
 *   MATCH_GUIDE        the guide as the app placed it: pose id and name, the template frame aspect, the solved
 *                      OverlayTransform, and its figures in scene space (draw with aspect 9:16 over the plate)
 *   MATCH_COACH        the coach settings and camera the verdicts were computed with
 *   partColorsAt(f)    the guide's per-part colours, Studio.forScore(partScore, onDark) as CameraOverlay does;
 *                      empty until a person is assigned (the guide is then drawn in its neutral tone)
 */
import raw from "../../../public/footage/b5-live.match.json";
import { theme } from "../../theme";
import type { BodyPart, JointName, Skeleton, Vec } from "../../figure/types";

export type HintKind = "NO_PERSON" | "MISSING_PERSON" | "FRAMING" | "VISIBILITY" | "POSE" | "PERFECT";
export type Capture =
  | { state: "Idle" }
  | { state: "Holding"; progress: number }
  | { state: "Countdown"; seconds: number; progress: number }
  | { state: "Capture" }
  | { state: "Cooldown"; progress: number };

export type MatchFrame = {
  f: number;
  /** plate frame and source frame the verdict was computed on */
  n: number;
  src: number;
  score: number;
  percent: number | null;
  assigned: boolean;
  parts: Partial<Record<BodyPart, number>>;
  segments: Record<string, number>;
  hints: { text: string; kind: HintKind; severity: number }[];
  shown: { text: string; kind: HintKind } | null;
  capture: Capture;
};

type File = {
  plate: string;
  camera: { lens: string; mirroredView: boolean; audience: string };
  coach: { threshold: number; holdMillis: number; countdownSeconds: number; abortMargin: number };
  guide: {
    poseId: string;
    name: string;
    mirrored: boolean;
    frameAspect: number;
    transform: { scale: number; offsetX: number; offsetY: number; rotation: number };
    definedAt: { plate: string; n: number; f: number; src: number };
    figures: { joints: Partial<Record<JointName, Vec>>; visibility: Partial<Record<JointName, number>>; unit: number }[];
  };
  frames: MatchFrame[];
};

const file = raw as unknown as File;
const frames = file.frames;
const first = frames[0].f;
const last = frames[frames.length - 1].f;

export const MATCH_PLATE = file.plate;
export const MATCH_COACH = { ...file.coach, camera: file.camera };
export const MATCH_GUIDE = {
  poseId: file.guide.poseId,
  name: file.guide.name,
  mirrored: file.guide.mirrored,
  frameAspect: file.guide.frameAspect,
  transform: file.guide.transform,
  definedAt: file.guide.definedAt,
  figures: file.guide.figures.map((g): Skeleton => ({ joints: g.joints, visibility: g.visibility, unit: g.unit })),
};

export const matchAt = (f: number): MatchFrame => frames[Math.min(last, Math.max(first, Math.floor(f))) - first];

const find = (pred: (m: MatchFrame, prev: MatchFrame | null) => boolean): number => {
  for (let i = 0; i < frames.length; i++) if (pred(frames[i], i ? frames[i - 1] : null)) return frames[i].f;
  throw new Error("b5-live.match.json: expected event not found");
};
const shownText = (m: MatchFrame | null) => m?.shown?.text ?? null;
const changes = frames.filter((m, i) => i > 0 && shownText(m) !== shownText(frames[i - 1]) && m.shown).map((m) => m.f);

/** One entry per countdown second shown ({v: seconds, from: first frame}), in order (the setting decides how many). */
const DIGITS: { v: number; from: number }[] = (() => {
  const out: { v: number; from: number }[] = [];
  for (const m of frames) {
    if (m.capture.state !== "Countdown") continue;
    const v = (m.capture as { seconds: number }).seconds;
    if (!out.length || out[out.length - 1].v !== v) out.push({ v, from: m.f });
  }
  if (!out.length) throw new Error("b5-live.match.json: no countdown");
  return out;
})();
const digitAt = (s: number) => DIGITS.find((d) => d.v === s)?.from ?? DIGITS[DIGITS.length - 1].from;

export const MATCH_EVENTS = {
  found: find((m) => m.percent !== null),
  card1: changes[0],
  card2: changes[1],
  perfect: find((m) => m.shown?.kind === "PERFECT"),
  hold: find((m) => m.capture.state === "Holding"),
  countdown: DIGITS[0].from,
  digits: DIGITS,
  digit3: digitAt(3),
  digit2: digitAt(2),
  digit1: digitAt(1),
  capture: find((m) => m.capture.state === "Capture"),
};

export const partColorsAt = (f: number): Partial<Record<BodyPart, string>> => {
  const m = matchAt(f);
  if (!m.assigned) return {};
  const o: Partial<Record<BodyPart, string>> = {};
  for (const [k, v] of Object.entries(m.parts) as [BodyPart, number][]) o[k] = theme.signal.forScore(v, true);
  return o;
};
