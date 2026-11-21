# Poseit promo: storyboard

Vertical 1080x1920, 30 fps, 664 frames (22.13 s), with sound. Music-led now; the voice-over in `vo.txt` drops in
later without re-cutting (the picture is locked to the music grid, see `sound-plan.md`). Built in Remotion.

Frame numbers are at 30 fps. Positions are fractions of the frame (x right, y down); W = 1080 px, H = 1920 px.
Type sizes are em as a fraction of H.

## 0. PAGE

- **Problem:** someone points a phone at you and says "do something natural", and you stand there like a plank.
  You don't know what to do with your hands, so the photo comes out stiff.
- **Audience:** friends and family, on a phone, in a WhatsApp chat or an Instagram feed. Often muted on Instagram
  and with sound on WhatsApp. They give it about 20 s and they are not technical.
- **Goal:** they install it (Android, free). They should be able to repeat one sentence: "it shows you a pose, tells
  you what to fix and takes the photo itself". The link travels in the message that carries the video, so the film
  doesn't show a URL (assumption, see section 12).
- **Emotion:** from "ugh, that's me" (awkward, a little embarrassed) to amused confidence ("oh, it just coaches you
  and snaps when you get it").

**The one true thing:** the success moment on the camera screen. A body lines up with the see-through mannequin,
each limb of the guide turns from red to green, the score ring closes, "Perfect. Hold it", the countdown runs out
and the photo is taken with nobody touching the phone. Everything else in the film sets that moment up or signs it.

## 1. Brand inventory

| Look at | What Poseit has | What the film does with it |
|---|---|---|
| Logo shape | The launcher icon is a white lay figure in the "Hand on hip" pose on a studio-grey disc, generated from the app's own pose DSL and mannequin geometry (`ic_launcher_foreground.xml`). The logo is one of the poses. | The logo arrives by cause: the matched figure drops its arm after the photo and is, at that moment, the launcher figure. |
| Core control | The see-through mannequin guide drawn over the live camera; you move until you match it. Second: the score ring. Third: the shutter you don't have to press. | The film's one cause and effect: a body moves, the guide's limbs change colour, the ring closes, the camera fires itself. |
| Signature material | White lay figures (Mannequin #FBFBF9, Ink contour) standing on studio-grey seamless paper (Paper #E4E5E1, Set #D2D3CF), each with a soft oval contact shadow. The camera is a true-black live view. | Two grounds only: the lit paper studio and the black viewfinder. Every figure on screen is generated from the real pose data. |
| Key verb | Pose, match, hold. The onboarding titles are "Pick a pose to copy", "Match the guide", "Hold the pose". | The film follows those three verbs in order, performed rather than written. |
| Success moment | Every limb green, a score in the 90s, the cue card "Perfect. Hold it", the countdown, the capture flash. | The strongest frame (f440) and the loudest sound (f560). |
| Voice | Plain, kind, exact. Hints are the reason, a full stop, then what to do: "Your feet are cut off. Step back". Sentence case, British spelling, no exclamation marks. | All on-screen UI words are the app's real strings; the VO uses the README's and onboarding's own phrasing. |
| Colour logic | Greys and ink. Colour only for match feedback: Miss #C8372D, Near #D9930D, Match #24935A; brighter on the camera (#FF6B5E, #FFC24A, #4FD18B). No brand accent. | The film is grey until a body starts matching. Red, amber and green appear only in the viewfinder act and only mean off, near and matched. |
| Type | Archivo only. Expanded SemiBold for the wordmark and pose names, Expanded Bold with tabular figures for the score and countdown, normal widths for everything else. | Same roles: the wide cut is spent on "Disco point", the numerals and "Poseit". |
| What they'd never do | Purple or any accent colour, gradients, glows, radial washes, tinted near-black, drop shadows (only the contact shadow), ALL CAPS, eyebrow labels, exclamation marks, emoji, decorative staggered entrances, looping decoration. | None of these appear. No vignette, no colour grade, no glow on the lock. |

## 2. Ideas

**First idea, set aside (the category costume):** a phone mock-up floats over a soft gradient, a dotted outline
appears on a stock photo of a person, tiles fly in ("99 poses", "Offline", "Free") and an end card shows a download
button. Cover the logo and any pose or AR-outline app could run it; it also breaks Studio (gradient, glow) and lands
on a stat slide and a web-footer ending.

Three ideas of different kinds:

| | Kind | The idea in one sentence |
|---|---|---|
| **A. The studio answers** | Montage of real output that becomes one real task | Someone freezes in the viewfinder; on the next downbeat a studio of all 99 lay figures answers by snapping through every kind of pose on the beat, they all land the same pose in unison, that one figure steps into the camera as the see-through guide, and the frozen body is coached onto it limb by limb, red to green, until the camera takes the photo by itself and the figure relaxes into the app icon. |
| **B. One held shot** | UI documentary in a single take | One unbroken, screen-true shot of the live view: a stiff skeleton, the pose strip slides, one tile is tapped, the guide lands, the hints coach it to green, countdown, flash. |
| **C. The coach speaks** | Typographic essay in the brand's voice | Poseit's real coaching sentences are the picture: each lands centred on studio paper and one lay figure acts it out ("Step back", "Raise your left arm", "Lower your chin") until "Perfect. Hold it" and the shutter. |
| **D. The gallery** | Stillness that breaks | A silent dolly through a gallery of 99 frozen lay figures on plinths; one shutter click and they all change pose at once; still again; the name. |

**Tests.**
- Cover the logo: A, B and D still say Poseit (the lay figures from the app's own geometry, the per-limb red to
  green, the cue-card voice, the Archivo numerals). C half-passes: the sentences are Poseit's, but the picture is
  type on paper.
- Swap in a competitor: a pose app with photo references or a neon AR outline cannot run A, because A depends on a
  library of drawn, posable figures and on the logo being one of those poses. C works for any coaching app with its
  own lines. B works only for Poseit but is a small UI on a black void for 20 s. D shows no case: nothing happens to
  anyone, so it never proves that the app coaches or takes the photo.
- Honesty: C puts the lay figure in the person's role, which misdescribes the product (the person moves, the figure
  is the guide). A keeps the roles right: the skeleton is you as the camera sees you, the lay figures are the poses.

**Pick: A.** It is the only idea that puts the breadth (all 99 poses, couples, groups) and the one success moment
in a single chain of causes, every link of which is literally how the app works (pick, match, hold), built from the
app's own data, and it ends on the logo by cause instead of by default. It spends the "crazy" in one place, the
chorus of figures, and keeps the proof legible.

## 3. Film shape and system

| Decision | Choice and reason |
|---|---|
| Shape | Joke, then one real case: question (hook), the answer as a chorus, the pick, the match, the hold, the name. Not claim, cut, proof, cta: the only typed claim is the viewer's own question. |
| Length | 664 f, 22.13 s. Eight beats. |
| Clock | Split, by meaning: **posed things step, live things are smooth.** The lay figures are hand-posed, so they change pose in one frame on the music grid (and the final relax runs on twos). The live skeleton, the HUD and the film camera are smooth 30 fps. |
| Motion grammar | Crisp: no smear, no shutter blur, no motion-blur filter. Grain only as a surface texture, one level for the whole film. |
| Music grid | 112.5 BPM: beat 16 f, eighth 8 f, bar 64 f, bar lines at f16 + 64n (downbeats f80, 144, 208, 272, 336, 400, 464, 528, 592, 656). Every cut and snap lands on a multiple of 8 f. Chosen because 32 f is about 1 s, so the app's 3 s countdown runs at almost real speed on the half notes. |
| Type | Archivo, the app's static cuts from `app/src/main/res/font/` (OFL). Hook question: Archivo SemiBold 0.05 H (96 px, 8.9% W), white. Pose name: Archivo Expanded SemiBold 0.045 H, Ink. Cue cards: Archivo Regular 0.04 H, Ink. Score: Archivo Expanded Bold, tabular, 0.042 H, white. Countdown: Archivo Expanded Bold, outlined, about 0.26 H. Wordmark: Archivo Expanded SemiBold, ink width 0.50 W. End line: Archivo Regular 0.034 H, Graphite. Sentence case everywhere. |
| Colour | Studio greys, Ink, Mannequin white, LiveView black. Signal colours only in act III with one meaning each: red off, amber near, green matched. No accent. |
| Carrier | The figure. It changes role and size at every seam and is never pinned: stiff skeleton (B1) -> stiff lay figure (B2) -> the same lay figure through every pose (B3) -> Disco point, framed (B4) -> the see-through guide (B5-B7) -> a solid lay figure in the photo (B8) -> the launcher figure over the name. |
| Cursor | None. It's a phone and the point is that nobody touches it. The pick is shown with the app's own frame marks. |
| Overshoot | Spent once: the live arm's reach in B6 overshoots about 6 degrees and settles in 8 f, because a real arm does. Nothing else bounces. |
| Orientation | Front camera, so the viewfinder shows the guide and skeleton flipped for display (ARCHITECTURE.md: "front-camera preview is mirrored"); the raised arm of Disco point is on screen left, matching "Raise your left arm". The studio acts use the same display orientation so the carried figure never flips. The flipped Disco point's hip hand sits where the launcher figure's hip hand sits (screen right), so the B8 relax only drops the raised arm and lands on the real, unflipped icon pose. |
| Remotion rules | One theme object (Studio tokens, signal colours, grid, eases), timing derived from fps and BPM, every interpolation eased and clamped (drifts use a sine in-out over their whole span instead of linear), render-extract-look loop. The skill's "background mesh, grade, vignette" stack is replaced by the world in section 4: Studio forbids radial washes, so there is no vignette and no grade; the paper's light and grain do that job. |

## 4. World per act

| Act | Frames | Ground | Light | Depth | Texture | Sound |
|---|---|---|---|---|---|---|
| I. Viewfinder, cold | 0-80 | True-black live view (LiveView #000000) with the camera's thirds grid, white at about 20%. | None in the scene: the white skeleton lines are the only bright thing. | Grid behind, skeleton, question in front; slow push 1.00 -> 1.04. | Fine luma sensor noise, as a dark live view has. | Room tone only, then a 6 f whoosh into the cut at f80. The silence is the joke. |
| II. The studio | 80-336 | Seamless studio paper: wall Paper #E4E5E1 curving through a soft cove into floor Set #D2D3CF. | One large softbox key from upper left, out of frame: the paper is about 4% lighter top left and 4% darker bottom right (tone, not colour). Every figure has the Studio contact shadow, a soft Ink oval at 16-20%, nudged right, away from the key. | 11 rows receding in true perspective. Row 0 cropped by the bottom edge and defocused about 6 px; row 1 (the hero row) sharp; rows 9-10 defocused 2-3 px and hazed 10% toward Paper. Real parallax on the dolly. | Paper tooth (static, about 2% luma) plus the film's temporal grain. | The groove: four-on-the-floor, hats made of shutter clicks; every pose change is a click. A drop of 0.8 s before the unison hit. |
| III. Viewfinder, live | 336-560 | True-black live view, thirds grid. | The see-through guide (Mannequin at 55%) is the light in the frame; signal colours light its limbs. | Guide behind, skeleton on top, HUD frontmost; film camera creeps 1.00 -> 1.03, then steps 2% on each countdown digit. | Sensor noise. | The groove thins to bass and soft hats, 8 dB down, while the hints are read. A chime on the lock, ticks on the count, a riser, half a beat of silence, the shutter. |
| IV. The photo, then the name | 560-664 | The same studio paper, lit by the capture flash; empty except the one figure. | Same key as act II. | The figure and its contact shadow; the cove appears as the camera pulls back. | Paper tooth, grain. | Shutter tail, one final chord on the downbeat, room tone to the last frame. |

**Picture sources.** Acts II and IV: built in code from the Studio tokens and the real mannequin geometry
(`FigureShapes.build`) of the real poses (`BuiltInPoses.all`). Acts I and III: the camera HUD rebuilt from the app's
tokens and strings (DESIGN.md camera section), using `docs/images/screens/3-camera.png`,
`app/build/screenshots/camera-hud.png` and `app/build/repo-assets/screen-3-camera.png` (the RepoAssetsTest still,
score 93 from the real PoseMatcher) as the references for the ring, card, skeleton and guide colouring. The
screenshots are references, not footage: at 15 sp their text is 0.017 H and cannot pass G1, and the viewfinder act
needs a body that moves.

## 5. The hero moment

- **The screenshot frame, f272-287 (B4):** all 99 white lay figures on grey seamless paper land Disco point in
  perfect unison, arms pointing at the sky, the camera punching in one frame; a moment later thin ink frame marks
  close around one figure in the second row. It gets the hardest work: all 99 real poses exported from pose-core and
  drawn with the app's own geometry, an 11 x 9 lattice in true perspective, a contact shadow per figure, three depth
  planes with defocus and haze, one key light, paper grain, and the snap on a hit that comes out of 0.8 s of silence.
- **The strongest frame of the energy curve, f440 (B6, 66% of the film):** the same figure, now the see-through
  guide on black, every limb green, the skeleton locked into it, the ring closed at the matcher's score, "Perfect.
  Hold it". This is the product's success moment.

Energy curve: it rises through the chorus (snaps every 16 f, then every 8 f, then the crowd doubles and triples),
drops for 0.8 s, hits at the unison (41%), goes quieter for reading in the coaching act, rises to the lock (66%),
builds through the countdown, stops for half a beat, fires the shutter (84%, the loudest sound), then gets smaller:
the pull-back, the name and a 1.13 s hold. Two peaks on purpose: the joke's answer has to come on the very next
downbeat after the question, and the match has to be the strongest frame because it is the thing being sold.

## 6. Importance budget

| Beat | Importance | Why |
|---|---|---|
| B1 Hook | 2 | Supporting: sets the problem, small and quiet. |
| B2 Everyone freezes | 1 | Connective: a one-second joke. |
| B3 Chorus | 2 | Supporting: fast and dense, but it builds toward B4. |
| B4 Unison and pick | **3** | The screenshot frame, its own hit, the biggest camera move. |
| B5 Step back | 2 | Supporting proof. |
| B6 Raise your left arm, lock | **3** | The strongest frame, the slowest read, its own sound. |
| B7 Hold, shutter | 2 | The release; loud, but its picture is a countdown. |
| B8 The name | 1 | Small, quiet, short. |

Two of eight beats (25%) are importance 3.

## 7. Beat table

Overview (frames are start-end, end exclusive):

| # | Frames | Length | Role | Buys |
|---|---|---|---|---|
| B1 | 0-80 | 80 f, 2.67 s | hook | Buys recognition: the viewer sees their own freeze. |
| B2 | 80-112 | 32 f, 1.07 s | turn | Buys the joke: the viewer sees that everyone freezes exactly as they do. |
| B3 | 112-272 | 160 f, 5.33 s | proof | Buys abundance: the viewer counts a pose for every kind of photo without being told a number. |
| B4 | 272-336 | 64 f, 2.13 s | turn | Buys choice: the viewer sees one pose picked out of all of them. |
| B5 | 336-400 | 64 f, 2.13 s | proof | Buys trust: the viewer watches the camera catch that the feet are out of frame. |
| B6 | 400-464 | 64 f, 2.13 s | proof | Buys the match: the viewer watches one instruction turn an arm from red to green. |
| B7 | 464-560 | 96 f, 3.20 s | proof | Buys hands-free: the viewer sees the photo taken with nobody touching the phone. |
| B8 | 560-664 | 104 f, 3.47 s | cta | Buys the name: the viewer connects the figure they just watched with Poseit, free on Android. |

### B1. Hook: the freeze (f0-80)

- **Role / importance:** hook, 2.
- **Buys recognition:** the viewer sees their own freeze.
- **On-screen copy:** "What do I do with my hands?" in two lines ("What do I do" / "with my hands?"), centred,
  baselines at y 0.205 and 0.262, Archivo SemiBold 0.05 H, white, tracking -1.5%.
- **VO:** none. Room-tone silence.
- **What moves:** a live white stick skeleton (head top y 0.33, feet y 0.93, centred) stands stiff, arms dead
  straight, and fidgets: weight shifts, head turns left then right. Words rise into place, whole, 0.03 H, softLand
  10 f with a 4 f opacity helper: What f2, do f6, I f10, do f14, with f20, my f24, hands? f33 (the longer gap before
  the key word). On "hands?" the wrists flick up to the waist (f33-42) and flop back (f42-50): the body acts out the
  question. f50-80 is the awkward pause, a designed breath before the answer, with the head turning. Camera push
  1.00 -> 1.04.
- **Entry:** frame 0 already moving (mid weight-shift, first word half-risen).
- **Exit:** hard cut at f80 on the downbeat, mid head-turn.
- **Handoff:** the skeleton's stiff silhouette becomes a lay figure in the identical stance at the same place and
  size; the ground flips black to paper on the cut (the punchline); the camera reverses into a fast pull-back.
- **Carried object:** the stiff figure.
- **Assets:** rebuilt viewfinder (LiveView black, thirds grid); skeleton drawn the way the app draws MediaPipe's
  skeleton (white lines, joint dots, head ring; ref `docs/images/screens/3-camera.png`). The stiff stance is built
  with the pose DSL and is illustrative (declared in NOTE.md), not a library pose.
- **Picture with the text covered:** a white stick skeleton standing stiff in a black viewfinder with faint thirds
  lines, glancing around.
- **Precedent:** PL0214 (pair a hook question with the object that implies the answer, not a big type treatment):
  the body is the object.

### B2. Everyone freezes (f80-112)

- **Role / importance:** turn, 1.
- **Buys the joke:** the viewer sees that everyone freezes exactly as they do.
- **On-screen copy:** none. **VO:** none.
- **What moves:** the camera, pushed in at 2.50 so the hero figure (row 1, column 4) is exactly where and as big as
  the skeleton was, pulls back: a one-frame snap at f80-81 (2.50 -> 1.75, ratio 0.7) then a softLand tail to 1.00 by
  f100. It reveals the whole
  studio: 99 lay figures in 11 rows, all in the same stiff stance. From f100 a slow dolly forward begins. The figures
  don't move: they are frozen like you.
- **Entry:** cut in on the downbeat, already pulling back.
- **Exit:** no cut; at f112 the army breaks into poses.
- **Handoff:** continuous shot into B3.
- **Carried object:** the hero lay figure.
- **Assets:** the studio (section 4), 99 figures from `FigureShapes` in the illustrative stiff stance.
- **Picture with the text covered:** a high-angle studio of 99 white lay figures in rows on grey seamless paper, all
  standing stiff, the camera pulling back.

### B3. The chorus (f112-272)

- **Role / importance:** proof, 2.
- **Buys abundance:** the viewer counts a pose for every kind of photo without being told a number.
- **On-screen copy:** none. No category names, no counter: the 99 is the lattice itself.
- **VO:** "Poseit has ninety-nine poses to copy." (window f116-188).
- **What moves:** all 99 slots change pose in one frame on the grid, one category at a time, each slot showing a
  different pose of that category (slot pose = (column + 2 x row) mod n, so neighbours differ and every pose of the
  category appears):
  - on beats: Standing f112, Sitting f128, Leaning f144, Walking f160;
  - on eighths, as a ripple from the front row back (3 rows per frame): On the ground f176, Portrait f184,
    Selfie f192, Professional f200, Fun f208 (the bar-4 downbeat);
  - on beats again, the crowd doubling: Couple f224 (two figures per slot), Group f240 (three or four per slot), in
    the person tones Mannequin, #C9CBC6, #A6A8A3, #83857F.
  Each figure lands 1% H above its mark and settles in 6 f, like being set down. f248-272: the group crowd holds
  while the music drops out (a designed breath before the unison; the camera keeps drifting). Camera: a dolly
  forward in two pushes, 1.00 -> 1.08, with row parallax.
- **Entry / exit:** continuous; ends on the unison snap.
- **Handoff:** in-shot, at f272.
- **Carried object:** the hero figure, always near the centre of row 1.
- **Assets:** all 99 `BuiltInPoses.all` ids, by category (section 12); `FigureShapes`; DESIGN.md person tones.
  Figures are scaled by their body unit and stood on the floor; lying poses stay long and low.
- **Picture with the text covered:** the studio, the whole army snapping into a new kind of pose on every beat
  (all sitting, all leaning, all walking, all on the floor), then pairing up and crowding into groups.
- **Precedent:** PR-C03 (a wave across a grid, origin first, items on a pose clock while the camera stays smooth);
  PR-F23 (the viewer reads "many" without reading each).

### B4. Unison and pick (f272-336)

- **Role / importance:** turn, **3** (the screenshot frame).
- **Buys choice:** the viewer sees one pose picked out of all of them.
- **On-screen copy:** "Disco point", the pose name, set the way the pose detail screen sets a name under its framed
  figure: Archivo Expanded SemiBold 0.045 H, Ink, centred under the frame marks, baseline about y 0.79.
- **VO:** "Pick one." (window f292-310).
- **What moves:** f272, after 0.8 s of silence, every slot snaps to `fun_disco_point` in unison; the camera makes a
  one-frame scale cut 1.08 -> 1.18 and a softLand tail to 1.20 by f284. The framing has tilted so the hero figure's
  feet are at y 0.72, with row 0 cropped and soft below it. f288 thin Ink frame marks snap on around the hero figure
  (one frame, the detail screen's viewfinder brackets) and "Disco point" resolves under them (blur 8 -> 0 px, 5 f).
  f304-336 the camera pushes into the hero figure, accelerating (gentleIn, 1.20 -> about 2.35, the scale at which the
  hero is 0.56 H tall); the rest of the army sweeps out past the frame edges, foreground row first; the name leaves
  under the bottom edge; the frame marks reach the frame edges as the cut lands and go with the paper.
- **Entry:** the unison hit.
- **Exit:** hard cut at f336, at peak push velocity.
- **Handoff:** carried-object match cut. At f335 the hero figure is exactly the size and place of the guide in B5
  (head top y 0.38, feet y 0.94, centred); at f336 the paper turns to the black viewfinder, the figure turns
  see-through, and the push carries on for 4 f and settles.
- **Carried object:** the Disco point figure.
- **Assets:** `fun_disco_point` (all slots, flipped for display), DESIGN.md frame marks and Display role.
- **Picture with the text covered:** 99 lay figures pointing at the sky together on grey paper; one in the second row
  is framed by thin ink corner marks; the camera rushes at it.

### B5. Step back (f336-400)

- **Role / importance:** proof, 2.
- **Buys trust:** the viewer watches the camera catch that the feet are out of frame.
- **On-screen copy:** cue card "Your feet are cut off. Step back" (a real PoseMatcher hint, two lines, red dot),
  and the score numerals in the ring.
- **VO:** "Line up with it." (window f340-392).
- **What moves:** f336 the viewfinder: the see-through Disco point guide; the stiff skeleton from B1 is back (the
  callback), standing too close (1.3x, feet below the bottom edge, arms down). The guide's limbs take the matcher's
  per-limb colours (legs red, arms red or amber); the ring shows a red arc and the score. f352 the cue card drops
  in 0.02 H over 6 f (the app's own 150-220 ms standard easing). f360-392 the skeleton takes two steps back (scale
  1.30 -> 1.00 with a step bob); the feet enter at about f368; the guide's legs turn red, amber, green, frame by
  frame; the score steps up (about 21 -> 52, whatever the matcher returns). Film camera creeps 1.00 -> 1.03.
- **Entry:** cut in while B4's push decelerates.
- **Exit:** no cut; at f400 the cue card is replaced.
- **Handoff:** in-shot.
- **Carried object:** the guide (fixed) and the skeleton.
- **Assets:** `fun_disco_point` shapes flipped for display; the hint string from
  `pose-core/.../match/MatchHints.kt`; ring and card per DESIGN.md "Camera screen" and `camera-hud.png`; scores,
  limb states and hints from running the real PoseMatcher on the keyframed skeleton (section 12).
- **Picture with the text covered:** a big see-through white mannequin pointing up in a black viewfinder; a white
  stick skeleton, too close, steps back into it while the legs turn from red to green.

### B6. Raise your left arm, lock (f400-464)

- **Role / importance:** proof, **3** (the strongest frame, f440).
- **Buys the match:** the viewer watches one instruction turn an arm from red to green.
- **On-screen copy:** cue card "Raise your left arm" (amber dot), then at f440 "Perfect. Hold it" (green dot), both
  real PoseMatcher strings; the score numerals.
- **VO:** "It tells you what to change." (window f402-460).
- **What moves:** f400 the card text is replaced in one frame. f404-428 the skeleton's left arm (screen left)
  sweeps up in an arc (softLand 24 f, about 6 degrees past, settled by f434: the film's one visible spring);
  f412-430 the right hand comes to the hip. As the arm passes, the guide's left upper arm and then forearm turn red,
  amber, green. The score steps once per frame in shrinking steps and lands at f436 on the matcher's value for the
  final skeleton (target about 93, like the RepoAssetsTest still; the number shown is whatever PoseMatcher returns).
  f440 "Perfect. Hold it"; every limb green; the ring's arc closes to the score. f440-463 the lock frame holds, alive
  with the skeleton's breathing sway (about 2 px) and the camera creep.
- **Entry / exit:** continuous; into the countdown at f464.
- **Handoff:** in-shot.
- **Carried object:** the guide and the skeleton.
- **Assets:** as B5; the lock frame is matched against `app/build/repo-assets/screen-3-camera.png`.
- **Picture with the text covered:** the skeleton's arm sweeps up into the guide's raised arm, the arm turns red,
  amber, green, and the ring closes in green.
- **Precedent:** PL0319 (bring the problem-act instrument back in the solution colour): the stiff body from the hook
  returns and is matched in green. PR-B17 (a counter decelerates into its final value).

### B7. Hold, shutter (f464-560)

- **Role / importance:** proof, 2.
- **Buys hands-free:** the viewer sees the photo taken with nobody touching the phone.
- **On-screen copy:** countdown "3", "2", "1" (Archivo Expanded Bold, white with an Ink outline, about 0.26 H,
  centred at y 0.60, as the camera's countdown is drawn in `camera-hud.png`).
- **VO:** "Hold the pose, and it takes the photo." (window f466-550).
- **What moves:** f464 the cue card leaves upward, accelerating over 6 f, as "3" arrives from 1.5x (snapSettle
  10 f) and creeps 1.00 -> 0.96. f496 "2" and f528 "1" replace it in one frame with the same arrival; the film
  camera steps in 2% on each digit (a one-frame step and an 8 f tail). The skeleton holds with its breathing sway;
  the ring holds green, its score moving by a point as a live match does. f552-559 nothing moves but the sway (half
  a beat of silence). f560 the capture flash: full white for 2 frames.
- **Entry / exit:** continuous; leaves through the flash.
- **Handoff:** the flash becomes the next ground. Its white decays over 10 f into the lit studio paper instead of
  back to black; under it the figure is the same Disco point at the same size and place, now a solid lay figure with
  its contour and contact shadow: the photo has been taken.
- **Carried object:** the figure.
- **Assets:** the countdown is the app's real behaviour with the 3 s setting (chips Off, 2s, 3s, 5s, 10s); the white
  flash is the app's capture flash.
- **Picture with the text covered:** big outlined countdown numerals over the all-green guide and the still
  skeleton, then a white flash.

### B8. The photo, then the name (f560-664)

- **Role / importance:** cta, 1.
- **Buys the name:** the viewer connects the figure they just watched with Poseit, free on Android.
- **On-screen copy:** the wordmark "Poseit" (Archivo Expanded SemiBold, Ink, ink width 0.50 W, baseline y 0.62);
  one line in two rows, "Free for Android." / "Works offline." (Archivo Regular 0.034 H, Graphite, baselines y 0.685
  and 0.725), the repo's own line from `docs/images/social-preview.png`.
- **VO:** "No account and no ads." (window f596-656).
- **What moves:** f560-562 white; f562-572 the flash decays into the paper. f576-588 the figure relaxes on twos:
  the pointing arm drops to its side, the hip hand stays, the feet settle, and it lands in `stand_hand_on_hip`, the
  launcher icon's pose. f592-616 the camera pulls back (1.00 -> 0.42, softLand 24 f) to reveal the cove; the figure
  stands about 0.24 H tall with its feet at y 0.50. f608 the wordmark lands below it from 1.3x and 6 px blur
  (snapSettle 10 f). f616-630 the line resolves a word at a time (3 f apart, blur 8 -> 0). f630-664 resolved hold of
  34 f (1.13 s), kept alive by a 0.5% camera creep.
- **Entry:** out of the flash.
- **Exit:** the film ends on the living hold. No fade to black.
- **Handoff:** none; the end.
- **Carried object:** the figure, now the icon's figure.
- **Assets:** `stand_hand_on_hip` (the source of `app/src/main/res/drawable/ic_launcher_foreground.xml`); wordmark per
  DESIGN.md. No disc around the figure: a disc closing in would read as an iris wipe, and the figure on the paper
  is already the icon's content.
- **Picture with the text covered:** a lone lay figure on grey seamless paper drops its pointing arm into a hand on
  the hip; the camera pulls back and the figure stands small above the name.
- **Precedent:** PL0455 (when the character is also the logo, turn it into its icon pose and shrink it before the
  wordmark appears).

## 8. Seams

| Seam | Frame | Last frame of A | First frame of B | Carried | Device |
|---|---|---|---|---|---|
| B1 -> B2 | 80 | Stiff skeleton, head mid-turn, head top y 0.33, feet y 0.93; black; camera 1.04, pushing in | Lay figure in the same stance, same place and size, on paper; camera 2.50, snapping back | The stiff silhouette | Hard cut on the downbeat (the punchline), carried object, ground flip, reversed camera (a scale cut: a different distance) |
| B2 -> B3 | 112 | Frozen army, dolly just started | Same shot, everyone in Standing poses | The army | In-shot snap |
| B3 -> B4 | 272 | Group crowd holding in the drop, camera drifting | Everyone in Disco point, camera cut 1.08 -> 1.18 | The army, the hero figure | In-shot unison snap plus a one-frame scale cut |
| B4 -> B5 | 336 | Hero figure at guide size, mid-push at peak speed, on paper; frame marks at the frame edges | Same figure, pose, size and place, see-through on black; skeleton too close; push decelerating | The Disco point figure | Carried-object match cut, ground flip |
| B5 -> B6 | 400 | Card "Your feet are cut off. Step back" | Card "Raise your left arm" | The card, the guide | Hard text replacement in the same card |
| B6 -> B7 | 464 | Card "Perfect. Hold it" | Card leaving, "3" arriving | Guide and skeleton | In-shot |
| B7 -> B8 | 560 | All-green guide, "1" creeping, skeleton still | White, decaying into paper with the same figure, solid | The figure | The capture flash becomes the next ground |

## 9. Layout of the two worlds

**Studio camera (acts II and IV).** An 11 x 9 lattice of slots on the floor, seen from a high angle (about 20 degrees
down). At the wide framing (f100-248): row 1 (the hero row) figures about 0.24 H tall with feet at y 0.76; row 10 about
0.07 H at y 0.35; the cove at about y 0.29 with the wall above. Row 0 is cropped by the bottom edge. Row 1 shows about
five figures across, cropped at both sides; rows 6 and up show all nine. Roughly 70-80 of the 99 are on screen at once,
so the lattice reads as bigger than the frame.

**Viewfinder HUD (acts I and III).** Rebuilt for a vertical feed at about 2.3x its phone size so it reads (G1), with
the app's layout logic (ring at the right above shoulder height, cue card above the figure, countdown centred):

| Element | Position and size |
|---|---|
| Guide | head top y 0.38, feet y 0.94, centred; the raised hand reaches about (0.20 W, 0.26 H) |
| Score ring | centre (0.83 W, 0.33 H), diameter 0.19 W, stroke 12 px; white at 60% until a body is found, then the signal colour, the arc proportional to the score; numerals Archivo Expanded Bold tabular, 0.042 H, white |
| Cue card | Sheet #F3F3F0, Medium radius (26 px), centred; one line y 0.165-0.232, two lines y 0.135-0.250; Archivo Regular 0.04 H, Ink; status dot 22 px in the signal colour |
| Countdown | centred at (0.50, 0.60), about 0.26 H, outlined |
| Thirds grid | white about 20%, 2 px |
| Safe zone | all text between y 0.13 and 0.80 |

## 10. Gates planned here

- **G1 proof readable:** every UI text that must be read is at least 0.04 H: cue cards 0.04 H (77 px) on cards of
  0.68-0.80 W ("Perfect. Hold it" about 0.68 W, "Raise your left arm" about 0.78 W, the two-line feet card about
  0.78 W); score numerals 0.042 H; countdown 0.26 H; the pose name 0.045 H at about 0.55 W. The viewfinder fragment
  is the whole frame and sits in its world (black live view, grid, grain). Judged by eye on `look/sheet.jpg` and
  stills after rendering.
- **G3 end card short:** the lockup is resolved at f630 and the film ends at f664: a 34 f (1.13 s) hold, inside
  0.6-1.4 s. The last cut is hidden in the flash at f562, so the final shot is 102 f (15% of the film); the cta beat
  is 104 f (16%). Both under 25%.
- **G4 one hero per frame:** B1 skeleton plus question; B2-B3 the army as one mass with the hero figure as its focal
  point; B4 the framed figure plus its name; B5-B7 guide and skeleton as one unit with at most two supporting
  elements (card and ring; during the countdown the card leaves, so it is numeral, guide and ring); B8 the figure,
  then figure, wordmark and line. No side-by-side panels anywhere.
- G0, G2 and G5 are checked on the render (`look.py`); the flash is 2 white frames, inside G2's 3-frame allowance.

## 11. Plan checks

- **Chrome audit.** Every text on screen: the hook question (the film's one line in the viewer's voice), "Disco
  point" (the product's pose name), three cue cards, the score and the countdown (product UI), "Poseit" (the logo,
  once) and "Free for Android. Works offline." (the one line under it). No labels, counters, category names,
  corner text, URL or button.
- **Picture audit.** Every beat's "picture with the text covered" is a world with an object in it; none is a flat
  colour or a card on a colour.
- **Carrier audit.** The figure changes role, size or ground at every seam (section 8).
- **Decided or reached for.**
  - Shape: a joke and one case, because the audience's own question is the strongest hook and the case is the app's
    real three-step flow.
  - Ground: paper and black, because those are the app's only two surfaces.
  - Type: Archivo in the app's roles, because it is the app's only family.
  - Ending: the matched figure relaxes into the launcher pose, because the icon is a pose.
  - Sound: a dry studio-disco groove whose hats are shutter clicks, because the film is about a camera and the
    hero pose is Disco point.
  - Transitions: two carried-figure cuts and one capture flash, because every ground change has a cause inside the
    product (choosing a pose opens the camera, taking the photo ends it).
- **Transformation chain?** The figure passes through several roles, the most over-used shape. It is kept because
  each link is literally true of the app (the pose you pick becomes the guide, the photo is taken when you match it,
  the icon is a pose), and nothing is a dot turning into a logo.
- **Could it be another brand's film?** No: without drawn, posable figures and an icon that is one of them, the
  chorus and the ending don't exist.
- **Slide test.** No frame is a centred headline on a field; the only typed line shares the frame with the body it
  describes.

## 12. Data and assets for the build

- **Pose data (real):** a pose-core JVM test or task writes `promo/data/poses.json`: for each of the 99
  `BuiltInPoses.all` templates its id, name, category, `frameAspect` and, per figure, the joints and the
  `FigureShapes.build` primitives (capsules, circles, polygon) in isotropic units. The geometry is exported, not
  re-implemented, so the film's figures are the app's figures.
- **Live match (real if it runs):** the keyframed skeleton for B5-B7 (front-camera scene space) goes through the real
  `PoseMatcher` frame by frame, writing `promo/data/match.json` (score, per-limb state, top hint). The film shows those
  numbers, colours and hints. If the matcher can't be driven offline, the numbers are illustrative and NOTE.md says
  so.
- **Hero poses:** `fun_disco_point` (B4-B8, flipped for display), `stand_hand_on_hip` (B8, the launcher pose).
- **Chorus poses, all 99, by category:**
  - Standing: stand_hand_on_hip, stand_hands_in_pockets, stand_crossed_ankles, stand_collar_touch, stand_look_away,
    stand_arms_crossed, stand_hand_in_hair, stand_over_shoulder, stand_power_pose, stand_sunglasses.
  - Sitting: sit_crossed_legs, sit_elbows_on_knees, sit_one_knee_up, sit_side_saddle, sit_steps_sideways,
    sit_hug_knees, sit_armchair_lounge, sit_chair_backwards, sit_ankle_on_knee, sit_high_perch.
  - Leaning: lean_shoulder_wall, lean_foot_on_wall, lean_hand_on_wall, lean_railing_forward, lean_crossed_legs,
    lean_door_frame, lean_pole_swing, lean_table_edge, lean_railing_back.
  - Walking: walk_toward_camera, walk_across, walk_look_back, walk_kerb_step, walk_skip, walk_hat_hold, walk_pockets,
    walk_twirl, walk_coffee_run, walk_jacket_shoulder.
  - On the ground: floor_street_squat, floor_low_crouch, floor_one_knee, floor_cross_legged, floor_side_lie,
    floor_feet_up, floor_lean_back, floor_overhead_lie, floor_sit_on_heels.
  - Portrait: port_chin_rest, port_face_frame, port_hand_in_hair, port_over_shoulder, port_arms_crossed_smile,
    port_chin_on_hand, port_dreamy_gaze, port_collar_adjust, port_shoulder_touch.
  - Selfie: selfie_classic, selfie_peace, selfie_high_angle, selfie_hand_on_cheek, selfie_mirror,
    selfie_mirror_two_hands, selfie_look_up, selfie_lean_in.
  - Professional: pro_headshot, pro_arms_crossed, pro_blazer_pocket, pro_desk_edge, pro_open_palm, pro_table_lean,
    pro_cuff_adjust, pro_folder_hold.
  - Fun: fun_star_jump, fun_tuck_jump, fun_twirl, fun_superhero, fun_disco_point, fun_laugh_bend, fun_high_kick,
    fun_peace_lean, fun_victory.
  - Couple: couple_side_hug, couple_forehead_touch, couple_back_hug, couple_walk_hands, couple_piggyback, couple_dip,
    couple_hold_hands_facing, couple_sit_lean, couple_walk_toward, couple_cheek_kiss.
  - Group: group_arms_around, group_v_formation, group_jump, group_walk_line, group_sitting_row, group_levels,
    group_back_to_back.
- **Illustrative, declared in NOTE.md:** the stiff stance (B1-B2), the person's skeleton keyframes (B1, B5-B7), the
  studio and its lattice, the HUD's 2.3x scale and vertical arrangement.
- **Real strings:** "Your feet are cut off. Step back", "Raise your left arm", "Perfect. Hold it" (MatchHints.kt);
  "Disco point" (FunPoses.kt); "Free for Android. Works offline." (social preview); VO phrasing from README.md and
  OnboardingScreen.kt.
- **Fonts:** the six static Archivo cuts in `app/src/main/res/font/`, copied with `app/src/main/assets/licenses/OFL-Archivo.txt`.
- **Assumption to confirm with the user:** the link (github.com/mdhaarishussain/Poseit, releases/latest) travels in
  the message with the video. If the film has to stand alone (an Instagram post), the end line can become "Download
  it from GitHub", still one line and still no URL on screen.

## 13. Left out on purpose

The library screen and its filters and search, the pose editor, "Pose from a photo", photoshoot mode, the
volume-key shutter, the raise-a-hand timer, grid, level and aspect options, lighting hints, the back-camera hints for
the photographer, export and import, and the spoken guidance (the app speaks through Android TextToSpeech; we don't
fake that voice). Each would be another beat of a feature tour. The film follows one person, one pose and one photo.
