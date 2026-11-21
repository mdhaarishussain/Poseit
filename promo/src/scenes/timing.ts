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

// =====================================================================================================================
// v3 (STORYBOARD.md v3). Inner frames per window, global v3 frames. The beat edges, captions, phone framings and the
// app's verdict frames are in beats.ts (BEATS_V3, CAPTIONS, PHONE_FRAMING, LIVE_V3); these are the hand-set frames
// inside each window. Owners: STORY owns OPENING_V3, CHORUS_V3_TIMING and END_V3; APP owns DEMO_V3.
// The v2 objects above stay until the v2 scenes are retired.
// =====================================================================================================================

/** B1-B2 (STORY): the problem on the phone's plain camera, then the app's launch screen. */
export const OPENING_V3 = {
  drift: [0, 96] as [number, number], // PHONE_FRAMING.W1: the film starts moving; her real footage on the screen
  screenCut: 96, // on the beat: the screen cuts from the plain camera to the Android launch screen (Paper)
  iconLand: [96, 104] as [number, number], // the launcher icon lands from 2.6x (softLand 8 f), PHONE.splashIcon
  step: [96, 106] as [number, number], // the camera steps in W1 -> W2 (softLand 10 f)
  hold: [106, 160] as [number, number], // the icon holds, settled; f160 hands the same screen to APP's library
};

/** B3-B6 (APP): pick, line up, hold, the photo. Live frames are beats.ts LIVE_V3 (the app's verdicts). */
export const DEMO_V3 = {
  launchToLibrary: [160, 172] as [number, number], // the app's launch screen gives way to the library (on the screen only)
  pushM: [160, 176] as [number, number], // W2 -> M
  scroll: [176, 214] as [number, number], // one real-feeling scroll of the library that settles with "Hands in pockets" in reach
  tapTile: { park: 216, press: 220, answer: 221 }, // the touch disc parks 4 f, squashes 2 f, the tile answers a frame later
  detailOpen: [222, 232] as [number, number], // the app's navigation to the pose detail
  pushD1: [228, 240] as [number, number], // magnified: the detail title "Hands in pockets" reads (>= 0.035 H em)
  roomSwap: 240, // o1's room -> her studio, while D1 covers the frame
  pullD2: [246, 258] as [number, number], // "Try this pose" in view
  tapTry: { park: 256, press: 260, answer: 261 },
  cameraOpen: [262, 272] as [number, number], // the app's navigation to the camera; plate frame 0 held until LIVE_V3.found
  pullW3: [264, 284] as [number, number], // D2 -> W3: the phone in her studio
  pushC: [312, 330] as [number, number], // W3 -> C: the card readable
  lockStep: 361, // the film camera steps 1.5% on the lock (v2)
  pullW: [382, 402] as [number, number], // C -> W3 on the "3": the whole phone, nobody touching it
  flash: [472, 480] as [number, number], // the app's capture flash on the preview (85% white, 280 ms decay)
  thumb: 480, // the app's gallery thumbnail takes the photo (white ring) and "Saved to Pictures/Poseit" rises
  viewerOpen: [484, 494] as [number, number], // the photo opens from the thumbnail to the full screen, in colour
  pushPhoto: [490, 516] as [number, number], // PHONE_FRAMING.PHOTO: the photo fills the frame
  photoPush: [516, 560] as [number, number], // slow push 1.00 -> 1.06 about her mid-hip, still moving on the cut
  cut: 560,
};

/** B7 (STORY): the chorus cuts are beats.ts CHORUS_V3; the film camera carries the photo's push across them. */
export const CHORUS_V3_TIMING = {
  first: 560,
  push: [560, 624] as [number, number], // 1.00 -> 1.06 across the four beat shots, about each person's mid-hip
  hold: [624, 656] as [number, number], // c9 holds (0.3% drift): the breath before the end card
  cut: 656,
};

/** B8 (STORY): c9's figure becomes the solid figure on paper, relaxes into the icon pose and becomes the icon. */
export const END_V3 = {
  cut: 656, // hard cut on the beat: the solid "Crossed-arm smile" figure exactly where c9's guide was
  breath: [656, 672] as [number, number], // the chorus push carried across the cut, settling
  relax: [672, 684] as [number, number], // on twos into "Hand on hip" (the icon's pose)
  iconForm: [684, 700] as [number, number], // the Set-grey disc grows behind it and the contour thickens: the app icon
  pull: [688, 712] as [number, number], // the camera pulls back; the icon settles above the wordmark
  wordmark: 708,
  lines: [724, 740, 752] as [number, number, number], // CAPTIONS C10-C12, a word every 3 f, each resolving in 5 f
  resolved: 768, // then a living hold (slow drift) so the static end hold stays 0.6-1.4 s
  end: 816,
};
