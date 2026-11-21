/**
 * events.ts: the film's picture events with their exact frames (30 fps, global), for the sound stage. Everything
 * is derived from the sources the picture uses, so a re-timed scene moves its sound with it:
 *   beats.ts (hook words, the people shots), scenes/timing.ts (hand-set frames), scenes/people/data.ts (the chorus
 *   shots and poses from chorus.json), scenes/viewfinder/match.ts (act IV: the app's PoseMatcher, HintStabilizer and
 *   CaptureCoach verdicts on the real footage).
 *
 *   EVENTS     [{ f, t, kind, label, to?, hit? }] sorted by frame; t = f / 30 s; `to` = end frame of a span (exclusive)
 *   HITS       the three hand-placed hero hits: unison (f272), lock (Holding starts: the score reaches the match
 *              threshold and the hold arc starts; every limb is green a few frames later), shutter (the capture flash)
 *   CUTS       every hard cut: f80 (black live view -> real footage), each chorus cut, f272 (footage -> the studio),
 *              f336 (match cut onto the live view), the capture (flash -> the colour photo), f555 (photo -> paper)
 *   SILENCES   the planned breaths the sound should leave empty: the drop before the unison, the half beat before
 *              the shutter
 *   CUES       the same frames by name for scripts/gen-audio.mjs (the sound is placed from these, never by hand); the
 *              shape is v1's, so the sound script runs unchanged: `chorus` is now the chorus cuts (one click per guide
 *              landing, `category` = the landed pose's library category), `ripple` is empty
 * Print as JSON: npm run events   (writes out/events.json)
 */
import { HOOK_WORDS } from "./beats";
import { poseById } from "./figure/poses";
import { HOOK_TIMING, LIVE_PLAN, NAME_TIMING, PEOPLE_TIMING, STUDIO_TIMING } from "./scenes/timing";
import { theme } from "./theme";
import { MATCH_EVENTS, matchAt } from "./scenes/viewfinder/match";
import { B2, CHORUS_SHOTS } from "./scenes/people/data";

export type EventKind =
  | "cut" | "word" | "body" | "camera" | "snap" | "unison" | "ui" | "match" | "count" | "shutter" | "type" | "breath";

export type PictureEvent = { f: number; t: number; kind: EventKind; label: string; to?: number; hit?: 1 | 2 | 3 };

const FPS = 30;
const ev = (f: number, kind: EventKind, label: string, extra: Partial<PictureEvent> = {}): PictureEvent => ({
  f, t: Math.round((f / FPS) * 1000) / 1000, kind, label, ...extra,
});
const isFrame = (f: number | undefined): f is number => typeof f === "number" && Number.isFinite(f);

const CUT_TO_STUDIO = STUDIO_TIMING.unison;
const CUT_TO_LIVE = STUDIO_TIMING.push[1] + 1;
const PHOTO_CUT = NAME_TIMING.cut;
const FLASH_END = MATCH_EVENTS.capture + Math.round((280 * FPS) / 1000);
const cards = [MATCH_EVENTS.card1, MATCH_EVENTS.card2, MATCH_EVENTS.perfect].filter(isFrame).filter((f, i, a) => a.indexOf(f) === i).sort((a, b) => a - b);
/** the first frame from the lock on which every limb the app scores is green (Studio.forScore: part score >= 0.8) */
const ALL_GREEN = (() => {
  for (let f = MATCH_EVENTS.hold; f < MATCH_EVENTS.capture; f++) {
    const parts = Object.values(matchAt(f).parts) as number[];
    if (parts.length && parts.every((v) => v >= 0.8)) return f;
  }
  return MATCH_EVENTS.hold;
})();
/** the first colour frame: the capture flash (280 ms) has decayed off the colour photo; the final chord blooms here */
const COLOUR = MATCH_EVENTS.capture + Math.floor((280 * FPS) / 1000);

export const HITS = { unison: STUDIO_TIMING.unison, lock: MATCH_EVENTS.hold, shutter: MATCH_EVENTS.capture };
export const CUTS = [PEOPLE_TIMING.cut, ...CHORUS_SHOTS.map((s) => s.from), CUT_TO_STUDIO, CUT_TO_LIVE, MATCH_EVENTS.capture, PHOTO_CUT];
export const SILENCES = [
  { from: PEOPLE_TIMING.drop, to: STUDIO_TIMING.unison, label: "the drop: the last chorus shot holds, people still moving, before the unison" },
  { from: MATCH_EVENTS.capture - 8, to: MATCH_EVENTS.capture, label: "half a beat of stillness before the shutter" },
];

const poseName = (id: string) => poseById(id).name;

const list: PictureEvent[] = [
  // ---- act I: the hook
  ...HOOK_WORDS.flat().map((w) => ev(Math.max(0, w.at), "word", `hook word "${w.text}"`)),
  ev(HOOK_TIMING.flickUp[0], "body", "wrists flick up to the waist", { to: HOOK_TIMING.flickUp[1] }),
  ev(HOOK_TIMING.flop[0], "body", "hands flop back", { to: HOOK_TIMING.flop[1] }),
  ev(HOOK_TIMING.pause[0], "breath", "the awkward pause", { to: HOOK_TIMING.pause[1] }),
  ev(HOOK_TIMING.exit[0] - 1, "word", "the question leaves upward, line by line", { to: HOOK_TIMING.exit[1] }),
  ev(HOOK_TIMING.push[0], "camera", "the rush into the stiff skeleton (peak speed on the cut)", { to: HOOK_TIMING.push[1] }),
  // ---- act II: real people
  ev(PEOPLE_TIMING.cut, "cut", "hard cut: black live view -> the first real person; the skeleton is on her body"),
  ev(B2.morph[0], "body", "the stiff skeleton becomes her detected skeleton", { to: B2.morph[1] }),
  ev(PEOPLE_TIMING.cut, "camera", "the plate lands from 1.06 (the rush arrives)", { to: PEOPLE_TIMING.plateLanded }),
  ev(PEOPLE_TIMING.armsUp[0], "body", "she lifts both hands behind her head (the skeleton follows)", { to: PEOPLE_TIMING.armsUp[1] }),
  ...CHORUS_SHOTS.map((s) =>
    ev(s.from, "snap", `chorus cut ${s.id}: the guide "${poseName(s.poseId)}" lands on a real person (app score ${Math.round(s.score * 100)})`, { to: s.to }),
  ),
  ev(PEOPLE_TIMING.push[0], "camera", "the film camera pushes 1.00 -> 1.08 across the chorus", { to: PEOPLE_TIMING.push[1] }),
  ev(PEOPLE_TIMING.drop, "breath", "the drop: the last shot holds, the music stops", { to: STUDIO_TIMING.unison }),
  // ---- act III: the library
  ev(CUT_TO_STUDIO, "cut", "hard cut: footage -> the studio of 99 lay figures"),
  ev(STUDIO_TIMING.unison, "unison", "all 99 in Hands in pockets, set down at once; one-frame scale cut", { hit: 1 }),
  ev(STUDIO_TIMING.marks, "ui", "frame marks snap around one figure; \"Hands in pockets\" resolves"),
  ev(STUDIO_TIMING.marks, "camera", "focus racks onto the hero's row", { to: STUDIO_TIMING.rackEnd }),
  ev(STUDIO_TIMING.push[0], "camera", "accelerating push into the hero (peak speed at the cut)", { to: CUT_TO_LIVE }),
  // ---- act IV: the live view (the app's real verdicts on the footage)
  ev(CUT_TO_LIVE, "cut", "match cut: the picked figure becomes the see-through guide over the real woman"),
  ev(MATCH_EVENTS.found, "match", `body found: live skeleton appears (${matchAt(MATCH_EVENTS.found).percent})`),
  ...cards.map((f) => ev(f, "ui", `hint card: "${matchAt(f).shown?.text}"`)),
  ev(LIVE_PLAN.flick[0], "body", "she flicks her hair, then puts her hands in her pockets", { to: LIVE_PLAN.flick[1] }),
  ev(MATCH_EVENTS.hold, "match", `lock: score ${matchAt(MATCH_EVENTS.hold).percent} reaches the match threshold, the hold arc starts`, { hit: 2 }),
  ev(ALL_GREEN, "match", `every limb green (score ${matchAt(ALL_GREEN).percent})`),
  ev(MATCH_EVENTS.digit3, "count", "countdown 3 (hint card removed)"),
  ev(MATCH_EVENTS.digit2, "count", "countdown 2"),
  ev(MATCH_EVENTS.digit1, "count", "countdown 1"),
  ev(MATCH_EVENTS.capture - 8, "breath", "half a beat of stillness", { to: MATCH_EVENTS.capture }),
  ev(MATCH_EVENTS.capture, "shutter", "capture: the app's flash; the photo it took, the film's first colour", { hit: 3, to: FLASH_END }),
  ev(COLOUR, "camera", `the flash has decayed: the colour photo, pushing 1.00 -> ${theme.footage.photoPush[1].toFixed(2)}`, { to: PHOTO_CUT }),
  // ---- act V: the name
  ev(PHOTO_CUT, "cut", "hard cut: the colour photo -> grey paper; a solid lay figure where she stood"),
  ev(NAME_TIMING.relax[0], "body", "the figure relaxes on twos into the launcher pose", { to: NAME_TIMING.relax[1] }),
  ev(NAME_TIMING.pull[0], "camera", "camera pulls back to reveal the cove", { to: NAME_TIMING.pull[1] }),
  ev(NAME_TIMING.wordmark, "type", "wordmark \"Poseit\" lands", { to: NAME_TIMING.wordmarkLanded }),
  ev(NAME_TIMING.line, "type", "\"Free for Android. Works offline.\" resolves a word at a time", { to: NAME_TIMING.resolved }),
  ev(NAME_TIMING.resolved, "breath", "resolved hold to the last frame", { to: NAME_TIMING.end }),
];

export const EVENTS: PictureEvent[] = list.filter((e) => isFrame(e.f)).sort((a, b) => a.f - b.f);

/**
 * CUES: the same frames by name, for the sound stage (scripts/gen-audio.mjs reads them from out/events.json, so a
 * re-timed picture moves its sound with it). All global frames at 30 fps; spans are [from, to).
 */
export const CUES = {
  fps: FPS,
  frames: NAME_TIMING.end,
  /** the music grid: one beat = 16 f (112.5 BPM), bar lines at f16 + 64n */
  grid: { beat: theme.grid.beat, firstBar: theme.grid.firstBar },
  hookWords: HOOK_WORDS.flat().map((w) => Math.max(0, w.at)),
  flick: HOOK_TIMING.flickUp,
  flop: HOOK_TIMING.flop,
  cut1: PEOPLE_TIMING.cut,
  /** one shutter-click per guide landing (every chorus cut); category = the landed pose's library category */
  chorus: CHORUS_SHOTS.map((s) => ({ f: s.from, category: poseById(s.poseId).category as string })),
  /** v1's studio ripple; there is none in v2 */
  ripple: [] as number[],
  drop: PEOPLE_TIMING.drop,
  unison: STUDIO_TIMING.unison,
  marks: STUDIO_TIMING.marks,
  push: [STUDIO_TIMING.push[0], CUT_TO_LIVE] as [number, number],
  /** "Hands in pockets" is on screen from the marks until the push carries it out under the frame */
  nameRead: [STUDIO_TIMING.marks, CUT_TO_LIVE] as [number, number],
  cut2: CUT_TO_LIVE,
  found: MATCH_EVENTS.found,
  cards,
  lock: MATCH_EVENTS.hold,
  digits: [MATCH_EVENTS.digit3, MATCH_EVENTS.digit2, MATCH_EVENTS.digit1],
  still: MATCH_EVENTS.capture - 8,
  capture: MATCH_EVENTS.capture,
  flashEnd: FLASH_END,
  /** the first colour frame (the flash has decayed): the final chord blooms here and rings through the f555 cut */
  colour: COLOUR,
  photoCut: PHOTO_CUT,
  relax: NAME_TIMING.relax,
  /** the frames on which the relaxing figure changes pose (on twos) */
  relaxSteps: Array.from({ length: (NAME_TIMING.relax[1] - NAME_TIMING.relax[0]) / 2 }, (_, k) => NAME_TIMING.relax[0] + 2 * (k + 1)),
  pull: NAME_TIMING.pull,
  wordmark: NAME_TIMING.wordmark,
  line: NAME_TIMING.line,
  resolved: NAME_TIMING.resolved,
  end: NAME_TIMING.end,
};
