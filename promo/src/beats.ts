/**
 * beats.ts: the beat tables (frames at 30 fps, global, end exclusive). Scenes read their windows from here, never
 * from magic numbers.
 *
 * TWO PLANS LIVE HERE UNTIL THE v3 BUILD LANDS.
 *   v2 (the top half, unchanged): what the current scenes read, so the committed 22 s cut still renders and
 *   `npm run check` passes. The v3 builders delete these exports as they retire the v2 scenes.
 *   v3 (the bottom half, STORYBOARD.md v3, "clarity first"): 816 f, 27.2 s. Captions carry the story; a drawn
 *   phone shows the app's real screens; the live act is v2's real verdicts moved by LIVE_OFFSET.
 *
 * v2:
 *   BEATS          B1-B8: role, world, importance, the frames (planned where `fromApp` says so)
 *   ACTS           one world each: I hook, II people (B2-B3), III the library (B4), IV live view + photo (B5-B7),
 *                  V the name (B8). Promo.tsx switches scenes on these.
 *   PEOPLE_SHOTS   B2 and the chorus: which manifest shot is on screen when (footage/manifest.json)
 *   HOOK_WORDS     the hook's word onsets (unchanged)
 *   COPY           the real strings
 *
 * v3 (see the section header further down for the list):
 *   FILM_V3, BEATS_V3, ACTS_V3, LIVE_OFFSET, LIVE_V3, CAPTURE_WINDOW_V3, CAPTIONS, CAPTION_SYSTEM, PHONE,
 *   PHONE_FRAMING, ROOMS_V3, CHORUS_V3, COPY_V3
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

// =====================================================================================================================
// v3 (STORYBOARD.md v3, clarity first). The director's plan as data. Builders read it; changes go through the board.
//
//   FILM_V3            length and clock
//   BEATS_V3           B1-B8: role, importance, owner, frames (`fromApp` = the edge is an app verdict, see LIVE_V3)
//   ACTS_V3            the scene that renders each window (Promo.tsx switches on these once v3 is assembled)
//   LIVE_OFFSET        v3 frame = v2 frame + LIVE_OFFSET for b5-live and everything read from its match/landmarks
//   LIVE_V3            the app's verdict frames in v3 (= MATCH_EVENTS + LIVE_OFFSET; APP asserts this in check-live)
//   CAPTURE_WINDOW_V3  the acceptance window for the capture in v3 frames
//   CAPTIONS           the caption script: text, line breaks, frames, band. vo.txt is the same words, one per line
//   CAPTION_SYSTEM     size, card, bands and motion of every caption (STORY builds it; numbers are fractions)
//   PHONE              the generic device: geometry shared by both builders
//   PHONE_FRAMING      where the phone's screen is, per window (named framings and the moves between them)
//   ROOMS_V3           the real scene behind the phone, per window
//   CHORUS_V3          the five people after the photo (v2 plates and chorus.json picks, moved by their offsets)
//   COPY_V3            the real strings v3 shows
// =====================================================================================================================

export const FILM_V3 = { frames: 816, fps: 30, width: 1080, height: 1920 } as const;

export type OwnerV3 = "STORY" | "APP";

/**
 * The v3 beat table (STORYBOARD.md v3 section 7). Importance 3 = the point (2 of 8 beats). Each beat says what the
 * viewer must be able to say after it, sound off.
 */
export const BEATS_V3 = {
  B1: { from: 0, to: 96, role: "hook", importance: 2, owner: "STORY", says: "This is about not knowing how to pose for photos." },
  B2: { from: 96, to: 160, role: "turn", importance: 2, owner: "STORY", says: "There is a phone app called Poseit that shows you how." },
  B3: { from: 160, to: 272, role: "proof", importance: 2, owner: "APP", says: "Step 1: you pick a pose in the app (Hands in pockets)." },
  /** ends on the app's "3" (LIVE_V3.digit3) */
  B4: { from: 272, to: 382, role: "proof", importance: 3, owner: "APP", says: "Step 2: you match the outline on the camera and it tells you what to fix.", fromApp: "to" },
  /** the "3" to the capture (LIVE_V3.capture) */
  B5: { from: 382, to: 472, role: "proof", importance: 3, owner: "APP", says: "Step 3: hold still and it takes the photo by itself.", fromApp: "both" },
  B6: { from: 472, to: 560, role: "proof", importance: 2, owner: "APP", says: "That is the photo it took, saved on the phone.", fromApp: "from" },
  B7: { from: 560, to: 656, role: "proof", importance: 1, owner: "STORY", says: "There are 99 poses, for all kinds of people and photos." },
  B8: { from: 656, to: 816, role: "cta", importance: 1, owner: "STORY", says: "Poseit, free for Android, works offline, no account, no ads." },
} as const satisfies Record<string, { from: number; to: number; role: string; importance: number; owner: OwnerV3; says: string; fromApp?: string }>;

/** One scene component per window (file names are the build split's, STORYBOARD.md v3 section 13). */
export const ACTS_V3 = {
  opening: { from: 0, to: 160, scene: "Opening", owner: "STORY", beats: ["B1", "B2"] },
  demo: { from: 160, to: 560, scene: "PhoneDemo", owner: "APP", beats: ["B3", "B4", "B5", "B6"] },
  chorus: { from: 560, to: 656, scene: "People", owner: "STORY", beats: ["B7"] },
  end: { from: 656, to: 816, scene: "TheName", owner: "STORY", beats: ["B8"] },
} as const;

/**
 * The live act reuses v2's b5-live plate, landmarks and verdicts, 40 frames earlier. Either play them at
 * (v3 frame - LIVE_OFFSET) or rebuild b5-live at its v3 window (manifest v3.shots); the verdicts must not change.
 */
export const LIVE_OFFSET = -40;

/**
 * The app's verdicts in v3 frames: v2's MATCH_EVENTS (found 336, card 348, hold 401, perfect 413, digits 422 / 452 /
 * 482, still 504, capture 512) + LIVE_OFFSET. After the SUBJECT-audience rerun the card text is "Bend your right elbow
 * more"; if any frame moves, these and the CAPTIONS windows move with it, never the verdicts.
 */
export const LIVE_V3 = {
  cameraOpen: 272, // the "Try this pose" tap has opened the camera; plate frame 0 (source 0.0) held until found
  found: 296, // the app finds her: the guide takes its per-limb colours, the skeleton and the ring appear
  card1: 308, // "Bend your right elbow more" (SUBJECT phrasing; v2 showed the PHOTOGRAPHER "Ask them to ...")
  flick: [326, 356] as [number, number], // her hair flick (real time), then hands into pockets, sliding into the guide
  hold: 361, // the lock: score reaches the threshold, the ring turns green and its hold arc runs
  allGreen: 364,
  perfect: [373, 381] as [number, number], // "Perfect. Hold it", the app's 9 frames
  digit3: 382,
  digit2: 412,
  digit1: 442,
  still: [464, 472] as [number, number], // nothing moves but the footage
  capture: 472, // the app's capture flash
} as const;

/** v2's f505-531 moved by LIVE_OFFSET: the capture must land here (check-live switches to it with the rerun). */
export const CAPTURE_WINDOW_V3 = [465, 491] as const;

export type CaptionBand = "top" | "bottom" | "end";

export type Caption = {
  id: string;
  /** the sentence as vo.txt has it */
  text: string;
  /** the line breaks on screen (at CAPTION_SYSTEM's size every line fits 0.83 W) */
  lines: string[];
  /** first and last frame + 1 on screen (entry starts at `from`, exit ends at `to`) */
  from: number;
  to: number;
  band: CaptionBand;
  beat: keyof typeof BEATS_V3;
  /** line index set in the wordmark face (Archivo Expanded SemiBold) instead of Archivo SemiBold */
  wordmarkLine?: number;
  note?: string;
};

/**
 * THE CAPTION SCRIPT. One plain sentence at a time, second person, the app's voice, sentence case, British spelling,
 * no exclamation marks, no middle dots, no arrows. Each is on screen at least 0.4 s per word (12 f) and never under
 * 1.2 s (36 f). Top band in the phone's wide framings, bottom band when the phone is pushed in (the HUD then owns the
 * top) and over the chorus (faces are in the upper half there). The end lines are Ink on Paper under the wordmark.
 */
export const CAPTIONS: Caption[] = [
  { id: "C1", text: "Never know how to pose for photos?", lines: ["Never know how", "to pose for photos?"], from: 0, to: 96, band: "top", beat: "B1", note: "the first word is already rising on f0; the whole line is up by f24" },
  { id: "C2", text: "Poseit shows you how.", lines: ["Poseit", "shows you how."], from: 100, to: 156, band: "top", beat: "B2", wordmarkLine: 0, note: "with the icon landing on the phone's screen at f96" },
  { id: "C3", text: "Pick a pose to copy.", lines: ["Pick a pose to copy."], from: 164, to: 228, band: "top", beat: "B3", note: "the app's own onboarding title; over the library scroll, gone as the tap opens the detail" },
  { id: "C4", text: "Match the outline.", lines: ["Match the outline."], from: 278, to: 314, band: "top", beat: "B4", note: "the app's word for the guide ('Help them match the outline'); the outline stands beside her, she slides into it" },
  { id: "C5", text: "It tells you what to fix.", lines: ["It tells you", "what to fix."], from: 322, to: 394, band: "bottom", beat: "B4", note: "the phone is pushed in: the app's card 'Bend your right elbow more' reads above her head" },
  { id: "C6", text: "Hold still.", lines: ["Hold still."], from: 398, to: 434, band: "top", beat: "B5", note: "after the pull back at the '3'" },
  { id: "C7", text: "It takes the photo for you.", lines: ["It takes the photo", "for you."], from: 436, to: 508, band: "top", beat: "B5", note: "the capture (f472) happens under it: nobody touches the phone" },
  { id: "C8", text: "Here's your photo.", lines: ["Here's your photo."], from: 516, to: 560, band: "top", beat: "B6", note: "over the colour photo's headroom once it fills the frame" },
  { id: "C9", text: "99 poses for every kind of photo.", lines: ["99 poses for", "every kind of photo."], from: 562, to: 654, band: "bottom", beat: "B7", note: "explains the figures landing on five people" },
  { id: "C10", text: "Free for Android.", lines: ["Free for Android."], from: 724, to: 816, band: "end", beat: "B8" },
  { id: "C11", text: "Works offline.", lines: ["Works offline."], from: 740, to: 816, band: "end", beat: "B8" },
  { id: "C12", text: "No account, no ads.", lines: ["No account, no ads."], from: 752, to: 816, band: "end", beat: "B8" },
];

/**
 * How every caption looks and moves (fractions of W = 1080 and H = 1920). Archivo's cap height is 0.686 em, so
 * 0.051 H type has a 0.035 H cap height (the brief's floor) and a 0.056 H line.
 */
export const CAPTION_SYSTEM = {
  font: { family: "Archivo", weight: 600, sizeH: 0.051, lineH: 0.056, capH: 0.035, wordmarkFamily: "Archivo Expanded" },
  maxLineW: 0.83,
  /** captions over a picture sit on a Sheet cue card: hug the text, no dot, no border, no shadow (unlike the app's card, which has a status dot and lives inside the phone) */
  card: { fill: "#F3F3F0", ink: "#17181A", radiusW: 0.028, padXW: 0.045, padYH: 0.016 },
  /** top band: card top at 0.135 H (a two-line card ends at 0.279 H). bottom band: card bottom at 0.865 H (a two-line card starts at 0.721 H). Both inside the middle 75% of the frame. */
  bands: { topCardTopH: 0.135, bottomCardBottomH: 0.865 },
  /** the end lines: Ink on Paper, no card, centred under the wordmark (B8 lays them out) */
  end: { ink: "#17181A" },
  motion: {
    inFrames: 8, // card and words rise 0.012 H and resolve (softLand); words of a line 3 f apart
    riseH: 0.012,
    wordGap: 3,
    outFrames: 6, // leave accelerating (accelExit), up 0.008 H; never two captions on screen at once
    minGap: 2,
  },
  minFramesPerWord: 12,
  minFrames: 36,
} as const;

/**
 * The generic phone (both builders draw exactly this; APP owns src/phone/Device.tsx). Screen aspect = the app's
 * screen renders (1200 x 2580). No brand, no buttons, no tilt, no drop shadow.
 */
export const PHONE = {
  screenAspect: 1200 / 2580, // width / height
  bezel: 0.03, // of the screen width, each side
  deviceRadius: 0.13, // of the device width; the screen's radius is deviceRadius minus the bezel
  punchHole: { d: 0.03, cy: 0.022 }, // d of the screen width; centre from the screen top, of the screen height
  body: "#17181A", // Studio.Ink
  rim: { color: "#5A5C5B", alpha: 0.6, px: 1.5 }, // Studio.Graphite edge light at 1080 W
  splashIcon: { d: 0.4, cy: 0.5 }, // the Android launch screen: the launcher icon on Paper #E4E5E1, d of the screen width
  touch: { d: 0.11, fillAlpha: 0.22, ringAlpha: 0.45, park: 4, squash: 0.9, squashFrames: 2 }, // Ink disc, d of the screen width
} as const;

/** The screen's height in H for a screen width in W (1080 x 1920 frame). */
export const screenHeightH = (screenW: number) => (screenW * 1080) / PHONE.screenAspect / 1920;

export type ScreenRect = { cx: number; top: number; w: number };

/**
 * Named framings of the phone's SCREEN rectangle: cx and w in W, top in H. Moves between framings are one camera
 * group (device, screen and everything on it scale together; captions and the room do not). STORY follows the keys
 * up to f160 exactly (the seam into APP's library); APP may refine its own framings within the constraints in
 * STORYBOARD.md v3 section 9.
 */
export const PHONE_FRAMING = {
  /** B1: the phone in the room, the caption above it. Slow push with the screen top fixed. */
  W1: { from: { cx: 0.5, top: 0.3, w: 0.5 }, to: { cx: 0.5, top: 0.3, w: 0.515 }, frames: [0, 96] as [number, number], ease: "linear drift" },
  /** B2: on the screen cut the camera steps in (softLand 10 f), then drifts. */
  W2: { from: { cx: 0.5, top: 0.29, w: 0.56 }, to: { cx: 0.5, top: 0.29, w: 0.575 }, frames: [96, 160] as [number, number], ease: "softLand 96-106, then drift" },
  /** B3 library: the screen runs off the bottom; the caption above. f160 is the STORY -> APP seam. */
  M: { from: { cx: 0.5, top: 0.29, w: 0.66 }, to: { cx: 0.5, top: 0.29, w: 0.68 }, frames: [160, 226] as [number, number], ease: "softLand 160-176, then drift" },
  /** B3 detail, magnified: the title 'Hands in pockets' at em >= 0.035 H for >= 14 f; the screen covers the frame (the room swaps here). Suggested. */
  D1: { from: { cx: 0.5, top: -0.38, w: 1.45 }, to: { cx: 0.5, top: -0.4, w: 1.48 }, frames: [228, 246] as [number, number], ease: "gentleIn-out push 228-240" },
  /** B3: pull back so 'Try this pose' is in view for the tap (button at about 0.82 H). Suggested. */
  D2: { from: { cx: 0.5, top: 0.08, w: 0.66 }, to: { cx: 0.5, top: 0.08, w: 0.66 }, frames: [246, 264] as [number, number], ease: "softLand 246-258" },
  /** B4/B5 wide: the phone in her studio, the caption above; the countdown digits read at about 0.06 H. */
  W3: { from: { cx: 0.5, top: 0.3, w: 0.48 }, to: { cx: 0.5, top: 0.3, w: 0.49 }, frames: [264, 312] as [number, number], ease: "softLand 264-284, then drift; again 382-402 to 472" },
  /** B4 close: the preview 0.70 W wide with its top near 0.035 H (card top >= 0.125 H), her feet at or above 0.70 H, so a two-line bottom caption (0.721-0.865 H) sits over the app's bottom chrome, not over her. */
  C: { from: { cx: 0.5, top: -0.01, w: 0.7 }, to: { cx: 0.5, top: -0.01, w: 0.71 }, frames: [312, 382] as [number, number], ease: "softLand 312-330; a 1.5% step on the lock (f361)" },
  /** B6: push into the photo until it fills the frame, the device past the frame edges; then a slow push 1.00 -> 1.06 on the photo. */
  PHOTO: { from: { cx: 0.5, top: 0.3, w: 0.49 }, to: { cx: 0.5, top: -0.1, w: 1.08 }, frames: [490, 516] as [number, number], ease: "softLand" },
} as const satisfies Record<string, { from: ScreenRect; to: ScreenRect; frames: [number, number]; ease: string }>;

/**
 * The real scene behind the phone: the plate the phone is filming, scaled, blurred and printed in the Studio ramp,
 * lifted toward Sheet so the device and the captions read. It moves at 0.4x the phone's scale changes (depth).
 */
export const ROOMS_V3 = [
  { plate: "o1", from: 0, to: 240, note: "her white studio; holds source 3.5 s after f105 (the clip's man enters at 4.0 s)" },
  { plate: "b5-live", from: 240, to: 516, note: "swapped while D1 covers the frame; the studio the camera act films (v2 plate at frame - LIVE_OFFSET)" },
] as const;

export const ROOM_LOOK = { scale: 1.6, blurPx: 28, liftToSheet: 0.25, parallax: 0.4 } as const;

/**
 * B7: five people, each with the library figure the app's matcher picked for them in v2 (public/footage/chorus.json),
 * now in their own colour. offset: v3 frame = v2 frame + offset. Cuts on the beats; c9 holds into the end card.
 */
export const CHORUS_V3 = [
  { shot: "c7", from: 560, to: 576, offset: 336, pose: "walk_toward_camera", flipped: true, kind: "walking" },
  { shot: "c2", from: 576, to: 592, offset: 416, pose: "floor_cross_legged", flipped: false, kind: "sitting" },
  { shot: "c5", from: 592, to: 608, offset: 384, pose: "port_face_frame", flipped: true, kind: "portrait" },
  { shot: "c6", from: 608, to: 624, offset: 392, pose: "pro_blazer_pocket", flipped: false, kind: "standing" },
  { shot: "c9", from: 624, to: 656, offset: 384, pose: "port_arms_crossed_smile", flipped: false, kind: "portrait, older man" },
] as const;

/** Real strings v3 shows (the app's UI and strings; captions are in CAPTIONS). */
export const COPY_V3 = {
  wordmark: "Poseit",
  pick: "Hands in pockets", // StandingPoses: the library tile, the detail title, the guide
  tryPose: "Try this pose", // PoseDetailScreen's button
  hint: "Bend your right elbow more", // MatchHints.kt, SUBJECT audience (after the rerun; v2's data has the PHOTOGRAPHER form)
  hintPerfect: "Perfect. Hold it",
  saved: "Saved to Pictures/Poseit", // CameraScreen.kt snackbar after a capture
  audienceSetting: "I'm posing", // CameraModels.kt AudienceMode.SUBJECT label, quick setting "Hints are for"
} as const;
