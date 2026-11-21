/**
 * beats.ts: the beat table of STORYBOARD.md section 7 (frames at 30 fps, end exclusive). Scenes read their
 * windows from here, never from magic numbers; every cut and snap sits on the 8 f music grid (theme.grid).
 */
export const BEATS = {
  B1: { from: 0, to: 80, role: "hook", world: "liveView" },
  B2: { from: 80, to: 112, role: "turn", world: "studio" },
  B3: { from: 112, to: 272, role: "proof", world: "studio" },
  B4: { from: 272, to: 336, role: "turn", world: "studio" },
  B5: { from: 336, to: 400, role: "proof", world: "liveView" },
  B6: { from: 400, to: 464, role: "proof", world: "liveView" },
  B7: { from: 464, to: 560, role: "proof", world: "liveView" },
  B8: { from: 560, to: 664, role: "cta", world: "studio" },
} as const;

export type BeatId = keyof typeof BEATS;

/** Acts (one world each): the two cuts between worlds are at f80 and f336, the flash at f560. */
export const ACTS = {
  I: { from: 0, to: 80 },
  II: { from: 80, to: 336 },
  III: { from: 336, to: 560 },
  IV: { from: 560, to: 664 },
} as const;

/** B3 chorus snaps (storyboard): category -> frame. */
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

/** The hook's word onsets (B1). */
export const HOOK_WORDS = [
  [{ text: "What", at: 2 }, { text: "do", at: 6 }, { text: "I", at: 10 }, { text: "do", at: 14 }],
  [{ text: "with", at: 20 }, { text: "my", at: 24 }, { text: "hands?", at: 33 }],
];

/** Real strings (pose-core MatchHints.kt, FunPoses.kt, the social preview). */
export const COPY = {
  hook: "What do I do with my hands?",
  poseName: "Disco point",
  hintFeet: "Your feet are cut off. Step back",
  hintArm: "Raise your left arm",
  hintPerfect: "Perfect. Hold it",
  wordmark: "Poseit",
  endLine: ["Free for Android.", "Works offline."],
} as const;
