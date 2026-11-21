/**
 * beats.ts: the v2 beat table of STORYBOARD.md section 7 (frames at 30 fps, global, end exclusive). Scenes read their
 * windows from here, never from magic numbers. Acts I-III cut on the 8 f music grid (theme.grid); acts IV-V run on
 * the app's clock, so their inner frames are the app's verdicts (scenes/viewfinder/match.ts MATCH_EVENTS) and the
 * `planned` values below are targets, not data.
 *
 *   BEATS          B1-B8: role, world, importance, the frames (planned where `fromApp` says so)
 *   ACTS           one world each: I hook, II people (B2-B3), III the library (B4), IV live view + photo (B5-B7),
 *                  V the name (B8). Promo.tsx switches scenes on these.
 *   PEOPLE_SHOTS   B2 and the chorus: which manifest shot is on screen when (footage/manifest.json)
 *   CHORUS         v1's studio chorus snaps (still read by scenes/studio/chorus.ts and events.ts until BUILDER WORLD
 *                  retires the studio chorus; v2 uses PEOPLE_SHOTS)
 *   HOOK_WORDS     the hook's word onsets (unchanged)
 *   COPY           the real strings
 */
export type World = "liveView" | "footage" | "studio" | "photo";

export const BEATS = {
  B1: { from: 0, to: 80, role: "hook", world: "liveView", importance: 2 },
  B2: { from: 80, to: 144, role: "turn", world: "footage", importance: 2 },
  B3: { from: 144, to: 272, role: "proof", world: "footage", importance: 2 },
  B4: { from: 272, to: 336, role: "turn", world: "studio", importance: 3 },
  /** ends on the app's hold frame (MATCH_EVENTS.hold); planned f402 */
  B5: { from: 336, to: 402, role: "proof", world: "footage", importance: 2, fromApp: "to" },
  /** the lock to the capture (MATCH_EVENTS.hold -> MATCH_EVENTS.capture); planned f402-513, capture must land in f505-531 */
  B6: { from: 402, to: 513, role: "proof", world: "footage", importance: 3, fromApp: "both" },
  /** the captured photo in colour, from the capture frame to the hard cut */
  B7: { from: 513, to: 555, role: "proof", world: "photo", importance: 2, fromApp: "from" },
  B8: { from: 555, to: 664, role: "cta", world: "studio", importance: 1 },
} as const;

export type BeatId = keyof typeof BEATS;

/**
 * Acts, one world each. The two hard cuts into the library and out of it are f272 (the unison hit) and f336 (the
 * match cut); the live act ends on the f555 cut from the colour photo to the paper.
 */
export const ACTS = {
  I: { from: 0, to: 80, scene: "Hook" },
  II: { from: 80, to: 272, scene: "People" },
  III: { from: 272, to: 336, scene: "StudioShot" },
  IV: { from: 336, to: 555, scene: "Viewfinder" },
  V: { from: 555, to: 664, scene: "TheName" },
} as const;

/** The capture must land inside this window so the colour photo holds 24-50 f before the f555 cut. */
export const CAPTURE_WINDOW = [505, 531] as const;

/**
 * B2 and the chorus: the manifest shot on screen in each window (footage/manifest.json `shots`). Every cut is on the
 * grid: beats (16 f) f144-192, eighths (8 f) f208-232, the couple f240, which holds through the drop (f248-272).
 */
export const PEOPLE_SHOTS = [
  { shot: "b2-alive", from: 80, to: 144 },
  { shot: "c1", from: 144, to: 160 },
  { shot: "c2", from: 160, to: 176 },
  { shot: "c3", from: 176, to: 192 },
  { shot: "c4", from: 192, to: 208 },
  { shot: "c5", from: 208, to: 216 },
  { shot: "c6", from: 216, to: 224 },
  { shot: "c7", from: 224, to: 232 },
  { shot: "c8", from: 232, to: 240 },
  { shot: "c9", from: 240, to: 272 },
] as const;

/** v1 studio chorus snaps (category -> frame). Retired in v2 (see PEOPLE_SHOTS); kept so the v1 studio still builds. */
export const CHORUS = [
  { category: "STANDING", at: 112 },
  { category: "SITTING", at: 128 },
  { category: "LEANING", at: 144 },
  { category: "WALKING", at: 160 },
  { category: "FLOOR", at: 176 },
  { category: "PORTRAIT", at: 184 },
  { category: "SELFIE", at: 192 },
  { category: "PROFESSIONAL", at: 200 },
  { category: "FUN", at: 208 },
  { category: "COUPLE", at: 224 },
  { category: "GROUP", at: 240 },
] as const;

/**
 * The hook's word onsets (B1): 4-6 f apart (headline rhythm) with the longer gap before the key word. "What" starts
 * before frame 0 so the film opens already moving, the first word half-risen. v2: the words leave f62-70
 * (timing.ts HOOK_TIMING.exit), before any real person is on screen.
 */
export const HOOK_WORDS = [
  [{ text: "What", at: -3 }, { text: "do", at: 2 }, { text: "I", at: 6 }, { text: "do", at: 10 }],
  [{ text: "with", at: 16 }, { text: "my", at: 20 }, { text: "hands?", at: 29 }],
];

/** The two library poses the story is built on (BuiltInPoses ids). */
export const POSES = {
  /** the pick (B4), act IV's guide, and B8's figure before it relaxes */
  pick: "stand_hands_in_pockets",
  /** the launcher icon's pose: B8 relaxes into it */
  icon: "stand_hand_on_hip",
} as const;

/** Real strings (pose-core StandingPoses / MatchHints.kt, the social preview). Act IV's cards come from match.json. */
export const COPY = {
  hook: "What do I do with my hands?",
  poseName: "Hands in pockets",
  hintPerfect: "Perfect. Hold it",
  wordmark: "Poseit",
  endLine: ["Free for Android.", "Works offline."],
} as const;
