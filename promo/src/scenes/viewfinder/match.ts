/**
 * match.ts: the app's real verdicts for act III, read from public/match.json (written by the app-module test
 * PromoMatchExport.kt from the performer's keyframes: PoseMatcher + HintStabilizer + CaptureCoach at 30 fps).
 *
 *   matchAt(frame)     the record for a frame of act III (clamped to the exported range)
 *   MATCH_EVENTS       frames derived from the records: found (first person), card1/card2/perfect (the displayed
 *                      hint changes), hold (Holding starts), digit3/digit2/digit1 (countdown seconds), capture
 *   partColorsAt(f)    the guide's per-part colours, Studio.forScore(partScore, onDark) as CameraOverlay does;
 *                      empty until a person is assigned (the guide is then drawn neutral white)
 */
import raw from "../../../public/match.json";
import { theme } from "../../theme";
import type { BodyPart } from "../../figure/types";

export type HintKind = "NO_PERSON" | "MISSING_PERSON" | "FRAMING" | "VISIBILITY" | "POSE" | "PERFECT";
export type Capture =
  | { state: "Idle" }
  | { state: "Holding"; progress: number }
  | { state: "Countdown"; seconds: number; progress: number }
  | { state: "Capture" }
  | { state: "Cooldown"; progress: number };

export type MatchFrame = {
  f: number;
  score: number;
  percent: number | null;
  assigned: boolean;
  parts: Partial<Record<BodyPart, number>>;
  segments: Record<string, number>;
  hints: { text: string; kind: HintKind; severity: number }[];
  shown: { text: string; kind: HintKind } | null;
  capture: Capture;
};

const frames = (raw as unknown as { frames: MatchFrame[] }).frames;
const first = frames[0].f;
const last = frames[frames.length - 1].f;

export const matchAt = (f: number): MatchFrame => frames[Math.min(last, Math.max(first, Math.floor(f))) - first];

const find = (pred: (m: MatchFrame, prev: MatchFrame | null) => boolean): number => {
  for (let i = 0; i < frames.length; i++) if (pred(frames[i], i ? frames[i - 1] : null)) return frames[i].f;
  throw new Error("match.json: expected event not found");
};
const shownText = (m: MatchFrame | null) => m?.shown?.text ?? null;
const changes = frames.filter((m, i) => i > 0 && shownText(m) !== shownText(frames[i - 1]) && m.shown).map((m) => m.f);
const digitAt = (s: number) => find((m) => m.capture.state === "Countdown" && (m.capture as { seconds: number }).seconds === s);

export const MATCH_EVENTS = {
  found: find((m) => m.percent !== null),
  card1: changes[0],
  card2: changes[1],
  perfect: find((m) => m.shown?.kind === "PERFECT"),
  hold: find((m) => m.capture.state === "Holding"),
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
