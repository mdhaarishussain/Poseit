/**
 * timing.ts: the hand-set frames of each beat (global frames, 30 fps), in one pure module so the scenes and
 * events.ts share them. The live act's frames are NOT here: they come from the app's own verdicts in match.json
 * (viewfinder/match.ts MATCH_EVENTS); LIVE_PLAN only records the storyboard's targets for them.
 *
 *   HOOK_TIMING   B1: the word onsets live in beats.ts HOOK_WORDS; the wrist flick and flop; v2: the words leave
 *                 and the camera rushes into the stiff body (the push is solved onto B2's body at f80)
 *   PEOPLE_TIMING B2-B3 (v2): the first real person, the chorus cuts, the film camera's push, the drop
 *   STUDIO_TIMING B4: the unison, the marks and the name, the rack focus, the push into the hero
 *   LIVE_PLAN     B5-B7 targets (superseded by MATCH_EVENTS once match.json is computed on the footage)
 *   NAME_TIMING   B8: the cut from the photo, the relax (on twos), the pull back, the wordmark, the end line
 * Owners (STORYBOARD.md section 13): BUILDER WORLD owns HOOK/PEOPLE/STUDIO/NAME_TIMING, BUILDER LIVE owns LIVE_PLAN.
 */
import { HOOK_WORDS } from "../beats";

export const HOOK_TIMING = {
  hands: HOOK_WORDS[1][2].at, // "hands?"
  flickUp: [29, 38] as [number, number], // the wrists flick up to the waist (softLand)
  flop: [38, 46] as [number, number], // and flop back (falling: gentleIn)
  pause: [46, 70] as [number, number], // the awkward pause before the answer
  exit: [62, 70] as [number, number], // v2: the words leave upward (accelExit, 2 f apart) before any person is on screen
  push: [70, 80] as [number, number], // v2: the rush into the stiff body (gentleIn, peak speed on f79), solved onto B2's body
};

/** B2-B3 (v2): the shot windows themselves are beats.ts PEOPLE_SHOTS. */
export const PEOPLE_TIMING = {
  cut: 80, // the first real person: the plate lands 1.06 -> 1.00 by f88, continuing the hook's push
  plateLanded: 88,
  armsUp: [88, 144] as [number, number], // her hands go behind her head (source 3.58-5.42 s), landing on the downbeat
  chorus: 144, // the first guide lands
  /** every chorus cut is beats.ts PEOPLE_SHOTS; the guide lands from 1.25x there (softLand 5 f on beats, 4 f on eighths) */
  /** the film camera pushes 1.00 -> 1.08 across every chorus shot, then only drifts in the drop */
  push: [144, 248] as [number, number],
  drop: 248, // the music stops; the couple holds (the designed breath before the unison)
  unison: 272,
};

/** B4 (v2: the studio is on screen from the unison only; its camera keeps v1's dolly so the f272 framing holds). */
export const STUDIO_TIMING = {
  unison: 272, // hard cut after the drop: all 99 in Hands in pockets, set down at once; one-frame scale cut
  scaleSettled: 284,
  marks: 288, // frame marks snap on; the name resolves (5 f); the focus racks onto the hero's row (8 f)
  rackEnd: 296,
  push: [304, 335] as [number, number], // accelerating push into the hero (gentleIn), cut at peak speed
};

/**
 * B5-B7 targets (STORYBOARD v2). The real frames are MATCH_EVENTS (found, card1, hold, digit3..1, capture) from the
 * app's matcher on the footage; these only say where the plan expects them and what must hold.
 */
export const LIVE_PLAN = {
  cut: 336, // the match cut: the library figure becomes the guide over her; the push lands by f342
  flick: [366, 396] as [number, number], // her hair flick at 1x (source 1.0-2.0 s), then hands into pockets
  hold: 402, // the lock (Holding): every limb green, the hold arc runs 700 ms
  digit3: 423, // countdown starts; the plate eases to 0.5x over 10 f
  capture: 513, // the app's capture: flash, then the colour photo; must land in beats.ts CAPTURE_WINDOW
  photoCut: 555, // hard cut from the colour photo to the paper
};

export const NAME_TIMING = {
  cut: 555, // v2: hard cut from the colour photo to the paper; the figure stands where she stood (the capture
  // flash belongs to the live act, Viewfinder, on MATCH_EVENTS.capture)
  relax: [576, 588] as [number, number], // on twos: the pointing arm drops, the launcher pose
  pull: [600, 624] as [number, number], // the camera pulls back (softLand 24 f)
  wordmark: 620, // lands from 1.3x and 6 px blur (snapSettle 10 f)
  wordmarkLanded: 630,
  line: 628, // "Free for Android." / "Works offline.": a word every 3 f, each resolving in 5 f
  resolved: 645,
  end: 664,
};
