/**
 * events.ts: the film's picture events with their exact frames (30 fps, global), for the sound stage. Everything
 * is derived from the sources the picture uses, so a re-timed scene moves its sound with it:
 *   beats.ts (hook words, chorus), scenes/timing.ts (hand-set frames), scenes/studio/chorus.ts (every snap frame,
 *   including the ripple), scenes/viewfinder/match.ts (act III: the app's PoseMatcher, HintStabilizer and
 *   CaptureCoach verdicts from match.json), scenes/viewfinder/performer.ts (the live body's moves).
 *
 *   EVENTS     [{ f, t, kind, label, to?, hit? }] sorted by frame; t = f / 30 s; `to` = end frame of a span (exclusive)
 *   HITS       the three hand-placed hero hits: unison (f272), lock (score reaches the match threshold, every limb
 *              green, the hold arc starts), shutter (the capture flash)
 *   CUTS       the three ground changes: f80 (hard cut, black -> paper), f336 (match cut, paper -> black), the capture
 *   SILENCES   the planned breaths the sound should leave empty: the drop before the unison, the half beat before
 *              the shutter
 *   CUES       the same frames by name for scripts/gen-audio.mjs (the sound is placed from these, never by hand)
 * Print as JSON: npm run events   (writes out/events.json)
 */
import { CHORUS, HOOK_WORDS } from "./beats";
import { CHORUS_SNAPS } from "./scenes/studio/chorus";
import { CATEGORY_LABEL } from "./figure/poses";
import type { CategoryId } from "./figure/types";
import { HOOK_TIMING, NAME_TIMING, STUDIO_TIMING } from "./scenes/timing";
import { theme } from "./theme";
import { MATCH_EVENTS, matchAt } from "./scenes/viewfinder/match";
import { PERFORMER, armT } from "./scenes/viewfinder/performer";

export type EventKind =
  | "cut" | "word" | "body" | "camera" | "snap" | "unison" | "ui" | "match" | "count" | "shutter" | "type" | "breath";

export type PictureEvent = { f: number; t: number; kind: EventKind; label: string; to?: number; hit?: 1 | 2 | 3 };

const FPS = 30;
const ev = (f: number, kind: EventKind, label: string, extra: Partial<PictureEvent> = {}): PictureEvent => ({
  f, t: Math.round((f / FPS) * 1000) / 1000, kind, label, ...extra,
});

/** First frame in [a, b) where pred holds on the match record. */
const firstMatch = (a: number, b: number, pred: (f: number) => boolean) => {
  for (let f = a; f < b; f++) if (pred(f)) return f;
  return -1;
};

const armPeak = (() => {
  let best = PERFORMER.armFrom, v = 0;
  for (let f = PERFORMER.armFrom; f < PERFORMER.armFrom + PERFORMER.armFrames; f++) if (armT(f) > v) { v = armT(f); best = f; }
  return best;
})();

export const HITS = { unison: STUDIO_TIMING.unison, lock: MATCH_EVENTS.hold, shutter: MATCH_EVENTS.capture };
export const CUTS = [STUDIO_TIMING.cut, STUDIO_TIMING.push[1] + 1, MATCH_EVENTS.capture];
export const SILENCES = [
  { from: STUDIO_TIMING.drop, to: STUDIO_TIMING.unison, label: "the drop: the group crowd holds before the unison" },
  { from: MATCH_EVENTS.capture - 8, to: MATCH_EVENTS.capture, label: "half a beat of stillness before the shutter" },
];

const list: PictureEvent[] = [
  // ---- act I: the hook
  ...HOOK_WORDS.flat().map((w) => ev(Math.max(0, w.at), "word", `hook word "${w.text}"`)),
  ev(HOOK_TIMING.flickUp[0], "body", "wrists flick up to the waist", { to: HOOK_TIMING.flickUp[1] }),
  ev(HOOK_TIMING.flop[0], "body", "hands flop back", { to: HOOK_TIMING.flop[1] }),
  ev(HOOK_TIMING.pause[0], "breath", "the awkward pause", { to: HOOK_TIMING.pause[1] }),
  // ---- act II: the studio
  ev(STUDIO_TIMING.cut, "cut", "hard cut: black viewfinder -> studio of 99 stiff lay figures (the punchline)"),
  ev(STUDIO_TIMING.snapBack, "camera", "camera snaps back, settles on the wide framing", { to: STUDIO_TIMING.settled }),
  ev(STUDIO_TIMING.settled, "camera", "dolly forward begins (two pushes)", { to: STUDIO_TIMING.unison }),
  ...CHORUS.map((c) => ev(c.at, "snap", `chorus: every figure snaps to a "${CATEGORY_LABEL[c.category as CategoryId]}" pose`)),
  ...CHORUS_SNAPS.filter((f) => !CHORUS.some((c) => c.at === f) && f !== STUDIO_TIMING.unison).map((f) => ev(f, "snap", "chorus ripple: the next rows snap (3 rows per frame)")),
  ev(STUDIO_TIMING.drop, "breath", "the drop: the group crowd holds", { to: STUDIO_TIMING.unison }),
  ev(STUDIO_TIMING.unison, "unison", "all 99 snap to Disco point in unison; one-frame scale cut", { hit: 1 }),
  ev(STUDIO_TIMING.marks, "ui", "frame marks snap around one figure; \"Disco point\" resolves"),
  ev(STUDIO_TIMING.marks, "camera", "focus racks onto the hero's row", { to: STUDIO_TIMING.rackEnd }),
  ev(STUDIO_TIMING.push[0], "camera", "accelerating push into the hero (peak speed at the cut)", { to: STUDIO_TIMING.push[1] + 1 }),
  // ---- act III: the viewfinder (the app's real verdicts)
  ev(STUDIO_TIMING.push[1] + 1, "cut", "match cut: the picked figure becomes the see-through guide on the black viewfinder"),
  ev(MATCH_EVENTS.found, "match", `body found: live skeleton appears, ring turns red (${matchAt(MATCH_EVENTS.found).percent})`),
  ev(MATCH_EVENTS.card1, "ui", `hint card: "${matchAt(MATCH_EVENTS.card1).shown?.text}"`),
  ev(PERFORMER.armFrom, "body", "the left arm sweeps up (the film's one spring)", { to: PERFORMER.armFrom + PERFORMER.armFrames }),
  ev(armPeak, "body", "the arm's overshoot peak"),
  ev(firstMatch(PERFORMER.armFrom, MATCH_EVENTS.card2, (f) => (matchAt(f).parts.LEFT_ARM ?? 0) >= 0.8), "match", "the guide's left arm turns green"),
  ev(PERFORMER.hipFrom, "body", "the right hand comes to the hip", { to: PERFORMER.hipTo }),
  ev(firstMatch(MATCH_EVENTS.found, MATCH_EVENTS.hold, (f) => (matchAt(f).percent ?? 0) >= 60), "match", "ring turns amber"),
  ev(MATCH_EVENTS.card2, "ui", `hint card: "${matchAt(MATCH_EVENTS.card2).shown?.text}"`),
  ev(PERFORMER.step1[0], "body", "first step back", { to: PERFORMER.step1[1] }),
  ev(PERFORMER.step2[0], "body", "second step back, legs cross into the pose", { to: PERFORMER.step2[1] }),
  ev(MATCH_EVENTS.hold, "match", `lock: score ${matchAt(MATCH_EVENTS.hold).percent}, every limb green, ring green, hold arc starts`, { hit: 2 }),
  ev(MATCH_EVENTS.perfect, "ui", `hint card: "${matchAt(MATCH_EVENTS.perfect).shown?.text}"`),
  ev(MATCH_EVENTS.digit3, "count", "countdown 3 (hint card removed)"),
  ev(MATCH_EVENTS.digit2, "count", "countdown 2"),
  ev(MATCH_EVENTS.digit1, "count", "countdown 1"),
  ev(MATCH_EVENTS.capture - 8, "breath", "half a beat of stillness", { to: MATCH_EVENTS.capture }),
  // ---- act IV: the photo, then the name
  ev(MATCH_EVENTS.capture, "shutter", "capture: the app's flash; the ground becomes the studio paper, the guide a solid figure", { hit: 3, to: NAME_TIMING.flashEnd }),
  ev(NAME_TIMING.relax[0], "body", "the figure relaxes on twos into the launcher pose", { to: NAME_TIMING.relax[1] }),
  ev(NAME_TIMING.pull[0], "camera", "camera pulls back to reveal the cove", { to: NAME_TIMING.pull[1] }),
  ev(NAME_TIMING.wordmark, "type", "wordmark \"Poseit\" lands", { to: NAME_TIMING.wordmarkLanded }),
  ev(NAME_TIMING.line, "type", "\"Free for Android. Works offline.\" resolves a word at a time", { to: NAME_TIMING.resolved }),
  ev(NAME_TIMING.resolved, "breath", "resolved hold to the last frame", { to: NAME_TIMING.end }),
];

export const EVENTS: PictureEvent[] = list.sort((a, b) => a.f - b.f);

/**
 * CUES: the same frames by name, for the sound stage (scripts/gen-audio.mjs reads them from out/events.json, so a
 * re-timed picture moves its sound with it). All global frames at 30 fps; spans are [from, to).
 */
const chorusFrames = new Set<number>(CHORUS.map((c) => c.at));
export const CUES = {
  fps: FPS,
  frames: NAME_TIMING.end,
  /** the music grid: one beat = 16 f (112.5 BPM), bar lines at f16 + 64n */
  grid: { beat: theme.grid.beat, firstBar: theme.grid.firstBar },
  hookWords: HOOK_WORDS.flat().map((w) => Math.max(0, w.at)),
  flick: HOOK_TIMING.flickUp,
  flop: HOOK_TIMING.flop,
  cut1: STUDIO_TIMING.cut,
  chorus: CHORUS.map((c) => ({ f: c.at, category: c.category as string })),
  /** the ripple frames after an eighth-note snap (the next rows snapping, 3 per frame) */
  ripple: CHORUS_SNAPS.filter((f) => !chorusFrames.has(f) && f !== STUDIO_TIMING.unison),
  drop: STUDIO_TIMING.drop,
  unison: STUDIO_TIMING.unison,
  marks: STUDIO_TIMING.marks,
  push: [STUDIO_TIMING.push[0], STUDIO_TIMING.push[1] + 1] as [number, number],
  /** "Disco point" is on screen from the marks until the push carries it out under the frame */
  nameRead: [STUDIO_TIMING.marks, STUDIO_TIMING.push[1] + 1] as [number, number],
  cut2: STUDIO_TIMING.push[1] + 1,
  found: MATCH_EVENTS.found,
  cards: [MATCH_EVENTS.card1, MATCH_EVENTS.card2, MATCH_EVENTS.perfect],
  lock: MATCH_EVENTS.hold,
  digits: [MATCH_EVENTS.digit3, MATCH_EVENTS.digit2, MATCH_EVENTS.digit1],
  still: MATCH_EVENTS.capture - 8,
  capture: MATCH_EVENTS.capture,
  flashEnd: NAME_TIMING.flashEnd,
  relax: NAME_TIMING.relax,
  /** the frames on which the relaxing figure changes pose (on twos) */
  relaxSteps: Array.from({ length: (NAME_TIMING.relax[1] - NAME_TIMING.relax[0]) / 2 }, (_, k) => NAME_TIMING.relax[0] + 2 * (k + 1)),
  pull: NAME_TIMING.pull,
  wordmark: NAME_TIMING.wordmark,
  line: NAME_TIMING.line,
  resolved: NAME_TIMING.resolved,
  end: NAME_TIMING.end,
};
