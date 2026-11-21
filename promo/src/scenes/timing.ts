/**
 * timing.ts: the hand-set frames of each beat (global frames, 30 fps), in one pure module so the scenes and
 * events.ts share them. The act III frames are NOT here: they come from the app's own verdicts in match.json
 * (viewfinder/match.ts MATCH_EVENTS).
 *
 *   HOOK_TIMING   B1: the word onsets live in beats.ts HOOK_WORDS; the wrist flick and flop
 *   STUDIO_TIMING B2-B4: the snap back, the dolly, the unison, the marks and the rack focus, the push
 *   NAME_TIMING   B8: the flash, the relax (on twos), the pull back, the wordmark, the end line
 */
import { HOOK_WORDS } from "../beats";

export const HOOK_TIMING = {
  hands: HOOK_WORDS[1][2].at, // "hands?"
  flickUp: [29, 38] as [number, number], // the wrists flick up to the waist (softLand)
  flop: [38, 46] as [number, number], // and flop back (falling: gentleIn)
  pause: [46, 80] as [number, number], // the awkward pause before the answer
};

export const STUDIO_TIMING = {
  cut: 80, // the hard cut: the hero lay figure takes the skeleton's frame
  snapBack: 81, // one-frame snap to 0.7 of the match zoom
  settled: 100, // softLand tail to the wide framing; the dolly begins
  dollyStop: 176, // the dolly's two pushes meet here
  drop: 248, // the group crowd holds: the planned breath before the unison
  unison: 272, // all 99 snap to Disco point, one-frame scale cut
  scaleSettled: 284,
  marks: 288, // frame marks snap on; the name resolves (5 f); the focus racks onto the hero's row (8 f)
  rackEnd: 296,
  push: [304, 335] as [number, number], // accelerating push into the hero (gentleIn), cut at peak speed
};

export const NAME_TIMING = {
  flash: 554, // the app's capture flash (85% white, 280 ms decay) over the new ground
  flashEnd: 563,
  relax: [576, 588] as [number, number], // on twos: the pointing arm drops, the launcher pose
  pull: [600, 624] as [number, number], // the camera pulls back (softLand 24 f)
  wordmark: 620, // lands from 1.3x and 6 px blur (snapSettle 10 f)
  wordmarkLanded: 630,
  line: 628, // "Free for Android." / "Works offline.": a word every 3 f, each resolving in 5 f
  resolved: 645,
  end: 664,
};
