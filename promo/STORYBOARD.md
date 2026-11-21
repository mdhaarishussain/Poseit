# Poseit promo: storyboard v2 (real people, the app's graphics on top)

Vertical 1080x1920, 30 fps, 664 frames (22.13 s), with sound. Built in Remotion on the v1 project. Frame numbers are
global frames at 30 fps, end exclusive. Positions are fractions of the frame (x right, y down); W = 1080 px,
H = 1920 px. Type sizes are em as a fraction of H. The footage, crops, in-points and poses are in
`footage/manifest.json`; credits in `CREDITS.md`.

**What changed from v1, in one paragraph.** v1 was all drawn figures. The user asked for real people: "Right now its
just stickmen. How about we add overlays of real humans. Stock videos etc". In v2 real people are the subject and the
app's graphics are what they always are in the app, overlays on a camera picture: the live skeleton, the white
see-through lay figure, the per-limb colours, the score ring, the cue card, the countdown and the flash. Kept from v1:
the hook (the stick figure in the black viewfinder), the studio of 99 lay figures for the pick (now one beat, not
three), the Studio palette and Archivo, the HUD and its motion, the sound engine and its 112.5 BPM grid, the end card
and its timing. New: a montage of real people, each fitted with the library pose the app's matcher finds for them;
act III on a real person, every verdict computed by the app's code on the footage; the footage printed in Studio grey
until the shutter, so the captured photo is the film's first colour frame; and the pose the film picks is "Hands in
pockets", which answers the hook's question.

## 0. PAGE

- **Problem:** someone points a phone at you and says "do something natural", and you stand there like a plank. You
  don't know what to do with your hands, so the photo comes out stiff.
- **Audience:** friends and family, on a phone, in a WhatsApp chat or an Instagram feed, often muted. About 20 s, not
  technical. They have seen a hundred "AI pose" stick-figure demos; real people with the app's figure on them is what
  makes this one look like a product.
- **Goal:** they install it (Android, free). The sentence they can repeat: "it shows you a pose, tells you what to fix
  and takes the photo itself". The link travels in the message with the video, so the film shows no URL.
- **Emotion:** from "ugh, that's me" to amused confidence: "oh, put your hands in your pockets, and it just takes it".

**The one true thing (unchanged):** the success moment on the camera screen. A body lines up with the see-through lay
figure, its limbs turn from red to green, the ring closes, "Perfect. Hold it", the countdown runs out and the photo is
taken with nobody touching the phone. In v2 that body is a real person and every number is the app's.

## 1. Brand inventory (v2 additions to v1's table)

v1's inventory still holds (logo = a lay figure in "Hand on hip"; core control = the see-through guide; material =
white lay figures on grey seamless paper and a black live view; verbs = pose, match, hold; colour only for match
feedback; Archivo only; never purple, gradients, glows, caps, exclamation marks, emoji). Two rows change:

| Look at | v2 |
|---|---|
| Material | The camera picture is now real footage. The app's own rule (greys and ink; colour only for match feedback) decides how it looks: the footage is printed in the Studio ramp, Ink #17181A to Sheet #F3F3F0, so a magenta wall, a pink studio and a desert become one grey world and the guide's red, amber and green are the only colour until the photo. |
| Voice | The app's real hint strings, in the phrasing the app uses on the back camera, where a friend holds the phone: "Ask them to ..." (MatchHints.kt, PHOTOGRAPHER audience). And one pose name, "Hands in pockets", which is the app's answer to "What do I do with my hands?" |

## 2. Ideas considered, and the pick

**First idea, set aside (the category costume):** stock clips of happy people in a phone mock-up, a dotted AR outline
on each, tiles flying in ("99 poses", "Offline", "Free"), a download button. Any pose or AR app could run it, the
stock would sit in four looks, and it ends on a web footer.

| | Kind | The idea in one sentence |
|---|---|---|
| **A. The figure finds a body** | Joke, then a montage of real output, then one real case | The hook's frozen stick figure gets pushed into and becomes a real person; then, on the beat, the app's white lay figure lands on person after person in exactly their pose; all 99 library figures answer the hook with one pose, "Hands in pockets"; that figure drops over a real woman in a real photo studio, the app coaches her into it, counts down and takes the photo, the first colour frame; the figure steps off the photo and relaxes into the icon. |
| **B. One photo shoot, one take** | UI documentary of one task | One continuous live view of a real model, the whole app flow from the found skeleton to the flash, no montage. |
| **C. Grey until the shot** | Stillness that breaks | A calm, slow montage of grey people holding poses with their guides, nothing happening, until one shutter turns the world to colour. |
| **D. Library meets street** | Split comparison | Each beat a library tile beside a real person in the same pose, side by side. |

**Tests.**
- Cover the logo: A and C still say Poseit (white lay figures from the app's geometry on real bodies, the per-limb red
  to green, the Studio grey, the cue-card voice). B is any pose-camera demo once the figure is hidden. D is two
  panels per frame (G4) and a deck layout.
- Competitor swap: a pose app with photo references or a neon outline cannot run A, which depends on a library of
  drawn, posable figures, on the matcher scoring real bodies against them, and on an icon that is one of the poses.
- Energy: the brief is a crazy, confident motion piece, not a slideshow of stock. A is cut on the grid and accelerates
  (16 f, then 8 f shots, then a drop and a unison hit); C is the calm film; B has one speed.
- Honesty: A keeps the roles right. The skeleton is the camera seeing a person; the lay figure is the pose; the people
  are never made to look awkward (the stick figure carries the freeze) and are never captioned.

**Pick: A, with C's colour logic.** It is the only idea in which the montage, the pick and the proof are one chain of
true causes (the library's poses are poses real people strike; you pick one; the app coaches a real body into it and
shoots), it reuses v1's strongest pieces (the hook, the unison, the HUD, the end), and the grey-until-the-shutter rule
turns six unrelated stock looks into one world while making colour mean what it means in the app.

## 3. Film shape and system

| Decision | Choice and reason |
|---|---|
| Shape | Joke, montage, answer, case: the question (hook), the figure gets a body, the chorus of real people, the unison and the name that answers the question, the live coaching, the photo, the name. |
| Length | 664 f, 22.13 s (unchanged: the sound engine, the grid and the end card keep their frames). Eight beats. Act III's inner frames come from the app's verdicts; the capture must land in f505-531. |
| Clock | Split by meaning, as v1: **posed things step, live things are smooth.** Library figures and chorus guides change pose in one frame on the grid. Footage plays at its own 24 fps cadence, repeated to 30 (never interpolated in real time); the live skeleton follows the detection per film frame; the HUD and the film camera are smooth. |
| Motion grammar | Crisp. No smear, no shutter blur. One grain level for the whole film. |
| Music grid | 112.5 BPM, beat 16 f, bar 64 f, bar lines at f16 + 64n (f80, 144, 208, 272, 336 ...). Every cut in acts I-III lands on it. Act IV runs on the app's clock (30 f per digit). |
| Type | Archivo, v1 roles unchanged: hook 0.05 H SemiBold; pose name Archivo Expanded SemiBold 0.045 H; cue card Medium 0.04 H; score Expanded Bold 0.042 H; countdown 0.26 H; wordmark ink 0.50 W; end line 0.034 H. |
| Colour | Three states, one per stretch: **grey** (black live view, Studio paper, footage printed Ink to Sheet) until f336; **signal** (the guide's per-limb Miss, Near, Match bright variants and the ring) only in act IV; **full colour** only in the captured photo (B7). Then grey paper again for the name. |
| Carrier | The figure, changing role at every seam: stiff stick skeleton (B1) -> the live skeleton on a real body (B2) -> the white guide on each person (B3) -> the library figure among 99 (B4) -> the see-through guide over her, coloured by the matcher (B5-B6) -> gone from the photo, which is clean (B7) -> a solid lay figure where she stood, relaxing into the launcher pose (B8). Never at the same coordinates across three shots: each chorus guide sits where that person is. |
| Camera (phone) | Back camera, not mirrored, everywhere in v2 (v1 used the front camera). Act IV runs the matcher with mirroredView = false and the app's AUTO audience for the back camera, PHOTOGRAPHER, so cue cards read "Ask them to ...". The studio figures are drawn unflipped to match. |
| Cursor | None. |
| Overshoot | None spent. The real bodies supply the organic motion; every graphic decelerates without bounce. |
| Remotion rules | One theme (new `theme.footage` tokens for the grade), every curve eased and clamped, OffthreadVideo for every plate, a slow push on every still or locked-off shot, render and look at frames. No vignette and no colour grade beyond the Studio ramp (Studio forbids radial washes). |

## 4. Worlds per act

| Act | Frames | Ground | Light | Depth | Texture | Sound |
|---|---|---|---|---|---|---|
| I. The freeze | 0-80 | True-black live view, thirds grid at 20% white (v1). | Only the white skeleton lines. | Grid behind, skeleton, question in front; push 1.00 -> 1.03, then a rush into the skeleton (f70-80). | Sensor noise. | Room tone; the push's whoosh peaks on the cut. |
| II. People | 80-272 | Real footage, full bleed, printed in the Studio ramp: a desert under a pale sky (B2), then a magenta wall, a warm set, a balcony, two pink studios, a green screen (B3), all reading as grey paper and ink. | The footage's own light (dusk sky, softboxes, low-key wall), unified by the ramp. | Real lenses: the footage's own depth of field. The white guide sits in front of each body; the film camera pushes 1.00 -> 1.08 across the whole chorus. | Footage grain plus the film's one grain. | The groove enters on the f80 cut; a shutter-click per guide landing; the drop at f248. |
| III. The library | 272-336 | v1 studio: lit seamless paper, 99 lay figures in true perspective. | One softbox key from upper left, contact shadows. | Three depth planes, defocus and haze, rack focus to the hero. | Paper tooth, grain. | The unison hit after the drop; the music dips under the name; a whoosh into the cut. |
| IV. The live view | 336-capture | A real photo studio (paper backdrop, softbox, stands), full bleed, printed grey: the app's camera picture. | The studio's softbox (in frame at the right edge); the guide is the brightest graphic. | Guide behind, live skeleton on top, HUD frontmost; film camera creeps 1.00 -> 1.04, steps 2% per digit. | Grain. | The coaching bed 6 dB down, a card tick, the lock chime, the countdown beeps, the build, 8 f of stillness, the shutter. |
| V. The photo | capture-555 | The same frame in full colour, frozen: pink paper, her clothes, the softbox. | The flash decays into it. | A still photo; a slow push 1.00 -> 1.03. | Grain. | The shutter's tail and the final chord blooming with the colour. |
| VI. The name | 555-664 | v1 studio paper, empty except one figure; the cove appears on the pull back. | Same key as act III. | The figure and its contact shadow. | Paper tooth, grain. | The chord rings out; a soft low re-strike under the pull back; room tone to the end. |

**Picture sources.** Acts II, IV, V: Mixkit stock (manifest), detected with the app's own lite model. Acts I, III, VI:
built in code from the Studio tokens and the 99 real poses (v1).

## 5. The hero moment

- **The screenshot frame: the lock, about f402 (B6, 61% of the film).** A real woman in a real photo studio, printed
  in Studio grey, hands in her pockets, inside the white see-through lay figure whose every limb has just turned green;
  the ring closed in green at the app's score; "Perfect. Hold it". It gets the hardest work: the app's lite model on the
  full-size plate, the app's PoseMatcher, HintStabilizer and CaptureCoach frame by frame, the guide placed with the
  app's own fit and overlay transform, the Studio ramp grade, the HUD at the app's proportions.
- **The second screenshot, f272-296 (B4):** all 99 lay figures in "Hands in pockets", the frame marks closing on one,
  the name under it: the answer to the question asked 8 seconds earlier.
- **The emotional peak, about f521 (B7):** the first colour in the film, the photo itself.

**Energy curve.** Quiet hook; a rush into the first real person on the f80 downbeat; the chorus accelerates (16 f
shots, then 8 f, then two figures at once); 0.8 s of drop; the unison hit (41%); the name and a breath; the push and
match cut; quieter for the coaching; the lock (61%, the strongest frame); the countdown builds while time slows; half
a beat of silence; the shutter (77%, the loudest sound) and the colour; then smaller: the figure, the icon pose, the
name, a 1.3 s hold.

## 6. Importance budget

| Beat | Importance | Why |
|---|---|---|
| B1 Hook | 2 | Sets the problem; small and quiet. |
| B2 The figure gets a body | 2 | The first real person; supporting. |
| B3 Chorus | 2 | Fast and dense, building to B4. |
| B4 Unison and pick | **3** | The answer to the hook, its own hit, the biggest camera move. |
| B5 Coaching | 2 | Supporting proof. |
| B6 Match, hold, count | **3** | The strongest frame and the product's success moment. |
| B7 The photo | 2 | The colour payoff, short. |
| B8 The name | 1 | Small, quiet. |

Two of eight beats (25%) are importance 3.

## 7. Beat table

| # | Frames | Length | Role | Buys | Copy on screen | VO (`vo.txt`) |
|---|---|---|---|---|---|---|
| B1 | 0-80 | 80 f, 2.67 s | hook | Buys recognition: the viewer sees their own freeze. | "What do I do / with my hands?" | none |
| B2 | 80-144 | 64 f, 2.13 s | turn | Buys the reveal: the viewer sees the app's skeleton land on a real person who knows what to do with her hands. | none | none |
| B3 | 144-272 | 128 f, 4.27 s | proof | Buys abundance on real bodies: the viewer sees a pose from the library sit exactly on person after person. | none | 1, f150-222 |
| B4 | 272-336 | 64 f, 2.13 s | turn | Buys the answer: the viewer sees all 99 poses become one, and its name answers the question. | "Hands in pockets" | 2, f292-310 |
| B5 | 336-~402 | ~66 f, 2.2 s | proof | Buys trust: the viewer watches the app find a real person and say what to change. | the stabiliser's hint card(s); score | 3, f344-400 |
| B6 | ~402-~513 | ~111 f, 3.7 s | proof | Buys the match and hands-free: the viewer sees every limb turn green and the countdown run with nobody touching the phone. | "Perfect. Hold it"; 3, 2, 1; score | 4, f426-505 |
| B7 | ~513-555 | ~42 f, 1.4 s | proof | Buys the payoff: the viewer sees the photo it took, the first colour in the film. | none | none |
| B8 | 555-664 | 109 f, 3.63 s | cta | Buys the name: the viewer connects the figure that guided the photo with Poseit, free on Android. | "Poseit"; "Free for Android." / "Works offline." | 5, f590-650 |

`~` frames are planned; the real ones are the app's verdicts on the footage (the hold frame ends B5, the capture frame
ends B6). Acceptance window: capture in f505-531.

### B1. Hook: the freeze (f0-80)

- **Role / importance:** hook, 2. **Buys recognition.**
- **As v1** (words rise f-3 to f29, wrists flick f29-38 and flop f38-46, head turns, room tone), with two changes:
  - **The question leaves before any person appears:** f62-70 the words exit upward, accelerating (accelExit 8 f,
    2 f apart, a 3 f opacity helper at the end). From f70 no text is on screen. Rule: the hook question never shares a
    frame with a real person, so no words are put in anyone's mouth.
  - **The rush:** f70-80 the film camera pushes into the stiff skeleton (gentleIn, peak speed on f79), solved so that on
    f79 the skeleton's frame is exactly B2's body at f80: head top about 0.12 H, feet about 0.96 H, mid-hip x about
    0.35 W (scale about 1.4 about a point near the feet, with a leftward drift). The push from f0 to f70 is the v1
    drift, 1.00 -> 1.03.
- **Exit:** hard cut at f80 on the downbeat, at peak push speed.
- **Picture with the text covered:** a white stick skeleton standing stiff in a black viewfinder, glancing around; the
  camera rushes at it.

### B2. The figure gets a body (f80-144)

- **Role / importance:** turn, 2. **Buys the reveal.**
- **Shot `b2-alive`:** clip `desert` (Mixkit 34404), source 3.30-5.43 s at 1x, full frame, Studio ramp.
- **What moves:** f80 the plate opens at 1.06 and lands at 1.00 by f88 (softLand), continuing the hook's push. The
  app's live skeleton (stick style, Mannequin lines at 70%, 2.5 dp, visible joints only) is drawn from detect.py on the
  plate, frame by frame: on f80 it sits where the hook's skeleton was. f88-144 she lifts both hands behind her head
  (her move runs 3.58-5.42 s) and the skeleton follows. Film camera drift 1.00 -> 1.02.
- **Exit:** hard cut at f144 on the downbeat as her hands land (on motion).
- **Handoff:** the overlay changes from the skeleton (the camera sees you) to the guide (a pose), at a similar size and
  place in C1.
- **Picture with the text covered:** a woman in a black dress on a flat desert under a pale sky, printed in grey, a
  white stick skeleton on her body; she lifts both hands behind her head.

### B3. The chorus (f144-272)

- **Role / importance:** proof, 2. **Buys abundance on real bodies.**
- **Shots** (manifest `c1`-`c9`; each guide is the library pose the app's matcher scores highest for that person; see
  the snap rule in the manifest):

| Shot | Film | Clip | Who and what | Planned pose (approx score) |
|---|---|---|---|---|
| c1 | 144-160 | magenta (1236) | a man in a denim jacket, fists on hips, feet apart | Superhero stance (0.86) |
| c2 | 160-176 | floor (101459) | a woman in a gold shirt sitting cross-legged on the floor | Cross-legged (0.87) |
| c3 | 176-192 | balcony (3191) | two women on a balcony, one holding the phone up for a selfie | Classic arm selfie (0.98) |
| c4 | 192-208 | redsuit (50649) | a woman in a red trouser suit, arms crossed | Crossed-arm smile (1.00) |
| c5 | 208-216 | panel (41665) | a woman in a white dress, both hands up by her shoulders | Double peace lean (0.93) |
| c6 | 216-224 | magenta (1236) | the man again, arms crossed | Confident crossed arms (0.95) |
| c7 | 224-232 | backstage (41664) | behind the scenes of a shoot, tripod in front: a hand on the hip | Hand on hip (0.93) |
| c8 | 232-240 | magenta (1236) | the man again, hands framing his face | Face frame (1.00) |
| c9 | 240-272 | couple (100743) | a couple side by side, two guides; holds through the drop | best couple template (to compute) |

- **What moves on every cut:** the plate hard-cuts; in the same frame the guide (Mannequin fill at 62%, Ink contour at
  55%, the app's figure geometry) lands on the person from 1.25x about their mid-hip (softLand 5 f on beat shots, 4 f on
  eighth shots), opacity from 0.9 to 0.62 over the same frames. The guide then stays put while the person moves under
  it, as the app's guide does. One shutter-click per landing. No pose names, no numbers, no colour in the chorus: the
  figures are white.
- **The film camera:** one continuous push 1.00 -> 1.08 from f144 to f248 applied over every shot, so the velocity
  carries across the cuts; in the drop (f248-272) only a 0.3% drift.
- **The drop:** f248-272 the couple shot holds while the music stops (the designed breath before the unison: a held
  frame with living people in it).
- **Fallback:** if c9's couple does not detect as two people or scores under 0.75, use the manifest's alternates; with
  the single fallback the chorus ends on one figure.
- **Picture with the text covered:** grey-printed real people (a man with fists on his hips, a woman cross-legged on
  the floor, two women taking a selfie, a woman in a suit with crossed arms ...), each with a white see-through lay
  figure laid exactly over their body, cutting faster and faster, ending on a couple, each with their own figure.

### B4. Unison and pick (f272-336)

- **Role / importance:** turn, **3**. **Buys the answer.**
- **On-screen copy:** "Hands in pockets" (the pose's real name in BuiltInPoses), Archivo Expanded SemiBold 0.045 H,
  Ink, centred under the frame marks, baseline about y 0.79 (about 0.79 W wide).
- **What moves:** v1's B4 with a new pose and a new seam in. f272, after the drop, hard cut to the studio: every one of
  the 99 slots is `stand_hands_in_pockets` (unflipped), and the camera makes v1's one-frame scale cut 1.08 -> 1.18 with
  the softLand tail. The hero figure's frame on f272 is the frame of the couple's nearer guide on f271 (preferred; a
  plain cut on the hit is acceptable if the lattice framing fights it). f288 frame marks snap on, the name resolves
  (blur 8 -> 0, 5 f), the focus racks onto the hero's row. f304-335 the accelerating push, solved every frame onto
  act IV's guide frame (`shots.ts` GUIDE_FRAME); the marks open out past the frame edges; the name leaves under the
  bottom edge.
- **Exit:** hard cut at f336 at peak push speed.
- **Picture with the text covered:** 99 white lay figures with their hands in their pockets on grey paper; one is
  framed by thin ink marks; the camera rushes at it.

### B5. Coaching (f336 to the app's hold frame, planned f402)

- **Role / importance:** proof, 2. **Buys trust.**
- **Shot `b5-live`:** clip `studio` (Mixkit 39676), source from 0.00 s, crop 864x1536 at (140, 230), Studio ramp.
- **On-screen copy:** whatever the app's HintStabilizer shows for these frames (expect one card before the lock), in
  the back camera's PHOTOGRAPHER phrasing; the score in the ring.
- **What moves:** f336 match cut: the picked figure is now the app's see-through guide (Mannequin at the default 55%)
  over the live view, same size and place; the push lands by f342. The app finds her: the live skeleton appears, the
  guide's limbs take `forScore` colours (the film's first colour), the ring shows the score in its signal colour. A
  card arrives on the stabiliser's frame. f366-396 she flicks her hair (real time, source 1.0-2.0 s) and puts her
  hands in her pockets; the guide's arms go from red or amber to green as the score climbs.
- **Picture with the text covered:** a woman in a light shirt and dark trousers on a paper backdrop in a real photo
  studio, a softbox at the edge, a white see-through lay figure standing where she will stand; she flicks her hair and
  the figure's arms turn from red to green as her hands go into her pockets.

### B6. Match, hold and countdown (hold frame to capture, planned f402-513)

- **Role / importance:** proof, **3** (the strongest frame at the lock).
- **On-screen copy:** "Perfect. Hold it", then the countdown 3, 2, 1, the score (all the app's).
- **What moves (the app's behaviour, v1's HUD code):** the lock: every limb green, the ring green, its hold arc runs
  700 ms; the "Perfect. Hold it" card crossfades in. At the countdown the banner is removed, each digit lands from 115%
  (200 ms) and the last fades (100 ms). From the countdown the plate eases from 1x to 0.5x over 10 f (`timeRemap`): her
  hold stretches while the app counts, so the real 3 s countdown completes inside her real hold. Film camera creeps
  1.00 -> 1.04 and steps 2% on each digit. The 8 frames before the capture: nothing moves but the footage.
- **Exit:** the app's capture flash (85% white, 280 ms decay).
- **Picture with the text covered:** the woman, hands in her pockets, inside the all-green figure; big outlined
  numerals count down; a white flash.

### B7. The photo (capture to f555, planned f513-555)

- **Role / importance:** proof, 2. **Buys the payoff.**
- **What moves:** under the flash the plate's frame at the capture appears in **full colour**, frozen and clean (no
  guide, no skeleton, no HUD: the app saves the photo without them). A slow push 1.00 -> 1.03 about her mid-hip.
- **Exit:** hard cut at f555.
- **Picture with the text covered:** a colour photo of a woman with her hands in her pockets on a pink paper backdrop
  in a photo studio.

### B8. The name (f555-664)

- **Role / importance:** cta, 1. **Buys the name.**
- **On-screen copy:** "Poseit"; "Free for Android." / "Works offline." (v1).
- **What moves:** f555 hard cut to grey Studio paper: a solid lay figure in "Hands in pockets", unflipped, exactly
  where she stood in the photo on f554 (same size and place), with its contact shadow. f555-576 a designed breath
  (0.7 s, kept alive by a 0.4% creep): the colour has gone and the pose stands alone as a figure. f576-588 it relaxes
  on twos into "Hand on hip", the launcher pose: one hand leaves its pocket for the hip. Then v1 unchanged: f600-624
  pull back to the cove, f620 the wordmark lands, f628-645 the end line resolves a word at a time, resolved hold to
  f664 (v1 measured 1.30 s).
- **Exit:** the film ends on the living hold. No fade.
- **Picture with the text covered:** a lone lay figure on grey seamless paper, hands in its pockets, takes one hand
  out to its hip; the camera pulls back; the figure stands small above the name.

## 8. Seams

| Seam | Frame | Last frame of A | First frame of B | Carried | Device |
|---|---|---|---|---|---|
| B1 -> B2 | 80 | Stiff skeleton at her body's frame, black, the camera at peak push speed | Her detected skeleton, same frame, on grey footage; the plate still zooming in (1.06 -> 1.00) | The figure | Carried object + same velocity; ground flip black -> footage; the groove's first kick |
| B2 -> B3 | 144 | Her hands landing behind her head, skeleton on | c1: a man, fists on hips, the guide landing from 1.25x | The overlay (skeleton becomes guide) | Hard cut on the downbeat, on motion; similar body size and centre |
| inside B3 | 160 ... 240 | Person N with their guide | Person N+1, the guide in a new pose at their body | The guide | One-frame pose change on the grid; the film camera's push carries across |
| B3 -> B4 | 272 | The couple holding in the drop | 99 figures in Hands in pockets, scale cut | The nearer guide -> the hero figure | Hard cut on the unison hit after 0.8 s of silence; same frame for the hero (preferred) |
| B4 -> B5 | 336 | Hero figure at the guide's frame, peak push speed, on paper | The same figure, see-through, over her in the live view; push decelerating | The figure | Carried-object match cut; paper -> a real paper backdrop |
| B5 -> B6 | ~402 | Arms turning green | The lock: all green, ring green, hold arc | The guide | In-shot (the app's verdict) |
| B6 -> B7 | ~513 | All-green guide, "1" fading, stillness | Flash decaying into the same frame in colour, overlays gone | Her | The app's capture flash becomes colour |
| B7 -> B8 | 555 | The colour photo, push at 1.03 | A solid lay figure in her pose, same frame, on grey paper | The pose | Carried object (her pose becomes the figure); colour -> grey |

## 9. Layout

**Chorus framing (B2-B3).** Each 9:16 crop puts the body 0.55-0.85 H tall, feet no lower than 0.97 H, centred where
the person stands (the crops are in the manifest). The guide is fitted to the body with the app's overlay transform
(mid-hip and torso length from the detection at the key time), so the guide moves to each person and is never pinned.
Upscale at most 1.78x (c4); everything else 1.0-1.56x.

**Live view HUD (B5-B6).** v1's layout and magnification (`theme.hud.k` = 1.95: cue text 0.04 H):

| Element | Position and size |
|---|---|
| Guide | the app's fitted 3:4 frame in the 9:16 scene (full width, 0.75 H, centred), adjusted by the solved overlay transform; head top about 0.21 H, feet about 0.79 H |
| Score ring | v1 (0.83 W, 0.40 H). The softbox sits at the right edge, near white in the ramp: draw the ring on the app's 45% control scrim disc so the white numerals read. LIVE may move it up if it still fights the softbox. |
| Cue card | top at 0.13 H, max width 0.84 W; PHOTOGRAPHER strings run longer ("Ask them to ..."), so plan for two lines (and check three) |
| Countdown | centred at (0.50, 0.50), about 0.26 H, outlined |
| Thirds grid | optional over footage (the app's setting); if drawn, 20% white, 2 px |
| Safe zone | all text between y 0.13 and 0.80 |

## 10. Gates planned here

- **G1 proof readable:** cue text 0.04 H on cards 0.5-0.84 W; score numerals 0.042 H; countdown 0.26 H; pose name
  0.045 H at about 0.79 W. The live view is the whole frame, in a world (a real studio). By eye on the sheet.
- **G2 no empty frame:** no flat run over 3 frames. The capture flash peaks at 85% white for 1-2 frames; the eighth
  shots are 8 frames of moving footage.
- **G3 end card short:** B8's last 89 frames are v1's frame for frame, so the end hold stays at v1's measured 1.30 s.
  Final shot (f555 to the end) 109 f = 16% of the film; the cta beat 16%.
- **G4 one hero per frame:** B2 the person and her skeleton as one unit; B3 one person and their guide (c3: the
  selfie-taker's guide; c9: two guides as one couple); B4 the army as one mass with the framed hero; B5-B6 person,
  guide and skeleton as one unit with the card and ring; B7 the photo; B8 the figure, then the lockup. No panels.
- **G0, G2, G5** on the render with `look.py` (`--expect 22.13 --expect-audio`); `lint.mjs` reads HTML films, so G5
  is checked by eye: no corner labels, counters, timecodes, progress bars, tracked caps or kickers.

## 11. Plan checks

- **Chrome audit:** the hook question (the viewer's voice, never over a person), "Hands in pockets" (a pose name),
  the app's cue cards, score and countdown (product UI), "Poseit" and its one line. Nothing else: no clip credits on
  screen, no labels, no URL.
- **Picture audit:** every beat's picture with the text covered is a world with a subject (real people in a real
  place, or the lit studio); none is a card on a colour.
- **Carrier audit:** the figure changes role, size or ground at every seam (section 8).
- **Stock audit:** no names or captions on people; nobody shown as awkward (the freeze is the stick figure's); no
  testimonial framing (no stars, quotes, smiles at the phone after the shot); the wordmark and end line never over a
  person; no face crop; no other camera app or phone brand on screen; Mixkit Free License only, every clip muted.
- **Decided or reached for:**
  - Shape: a joke and a montage before one case, because the hook's question deserves a literal answer and the
    montage is where real people show the library is real.
  - Ground: grey footage, because the app allows no colour but match feedback; black and paper as in v1.
  - Type and colour: the app's.
  - Ending: the figure from the photo relaxes into the icon pose, because the icon is a pose.
  - Sound: v1's studio-disco groove; the colour photo gets the warm chord.
  - Transitions: carried figures, one hit after a drop, one flash, because each world change has a cause.
- **Could it be another brand's film?** No: without drawn, posable figures that a matcher fits to real bodies, and an
  icon that is one of them, the chorus, the pick and the ending do not exist.
- **Slide test:** no frame is a headline on a field; the only typed lines share the frame with the figure they name.

## 12. Data and assets for the build

**The footage** (`footage/manifest.json`, `CREDITS.md`). Ten Mixkit clips (one alternate), all under the Mixkit Stock
Video Free License. Nothing from Coverr (its licence bars evaluating AI models; every shot here is detected).

**Pipeline** (all outputs gitignored unless marked committed):

1. **Fetch:** `curl -L -o public/footage/src/<clip>.mp4 <file>` for each clip the shots use (1080 files; the couple's
   4K file).
2. **Plates:** one film-ready file per shot, `public/footage/<shot>.mp4`: trimmed to the shot's source window plus
   0.5 s handles, cropped to the manifest rect, scaled to 1080x1920, 30 fps by frame repetition, audio stripped:
   `ffmpeg -ss <in-0.5> -t <dur+1> -i src/<clip>.mp4 -vf "crop=w:h:x:y,scale=1080:1920:flags=lanczos,fps=30" -an -c:v libx264 -crf 14 -pix_fmt yuv420p <shot>.mp4`.
   The live plate also carries the time remap (step 6). Plate frame n is film frame `film[0] + n` (after the handle).
3. **Detection:** `promo/.venv/bin/python scripts/detect.py public/footage/<shot>.mp4 -o public/footage/<shot>.landmarks.json`
   (the app's lite model and live settings; `--num-poses 2` for c9). Landmarks are then in scene space: the plate is
   the scene (9:16, photo orientation). The venv needs `libegl1` and `libgles2` on a fresh container. The landmark
   files are derived data and may be committed (they are what the matcher read).
4. **B2 data (WORLD):** `public/footage/b2-alive.json`, committed: `{ source, shot, from: 80, fps: 30, frames: [{ f, person: { joints, visibility } | null }] }`
   in scene space, joints named as `figure/types.ts` JointName.
5. **Chorus match (WORLD):** a pose-core JVM test (`pose-core/src/test/kotlin/com/poseit/app/tools/PromoChorusMatch.kt`)
   reads each chorus shot's key-frame detection and runs the app's `PoseMatcher.match` against every single-person
   template (couple templates for c9), each mapped into the 9:16 scene with `SceneMapping` and an `OverlayTransform`
   solved to the person (mid-hip and torso length), `mirroredView = false`, `FramingAudience.PHOTOGRAPHER`. It writes
   `public/footage/chorus.json`, committed: `{ source, shots: [{ id, film, clip, keyFrame, poseId, score, partScores, transform: { scale, offsetX, offsetY } | [per figure], ranking: [top 5 { poseId, score }] }] }`.
   WORLD applies the manifest snap rule and records any substitution in NOTE.md.
6. **Live match (LIVE):** `scripts/export-live.ts` builds `public/live.json` from the live plate's detection (no more
   keyframed performer) and the guide (`stand_hands_in_pockets` mapped with `SceneMapping` and the solved
   `OverlayTransform`); `PromoMatchExport.kt` runs PoseMatcher, HintStabilizer and CaptureCoach with
   `mirroredView = false`, `FramingAudience.PHOTOGRAPHER` and the app's coach defaults, writing `public/match.json`
   (committed; v1 schema plus `camera`, `transform`, `remap`). Order: 1x plate -> match -> set the ramp from the
   measured hold frame -> retimed plate -> detect -> match again -> check the acceptance list in the manifest.
7. **Sound:** `npm run audio` as before; `src/events.ts` derives every cue from the sources above.

**Shared code contracts.**
- `src/footage/manifest.ts` (committed by the director): typed `MANIFEST`, `clipById`, `shotById`,
  `sourceTimeAt(shot, frame)`, `plateFrameAt(shot, frame)`. Read-only for both builders; changes go through the
  manifest JSON.
- `theme.footage` (committed by the director): the ramp's two inks, the guide's chorus alpha, the landing scale, the
  photo push.
- `src/scenes/shots.ts`: LIVE owns `GUIDE_FRAME` and `guideScreenFrame(f)` (now the back-camera, unflipped guide from
  fitFrame plus the transform) and adds `PHOTO_FIGURE_AT_CUT` (her figure's screen frame on f554 with the photo push);
  WORLD owns `HOOK` (the new push) and adds `B2_BODY_AT_CUT`; `HERO_AT_CUT` follows `GUIDE_FRAME`.

**Real, rebuilt, illustrative (to record in NOTE.md).** Real: the footage (stock models), every detection (the app's
lite model), every chorus pose choice and every act IV number, colour, card, hold, countdown and capture frame (the
app's code), the poses and figure geometry, the strings, the fonts. Rebuilt: the HUD (v1). Illustrative: the stick
figure's stiff stance (hook), the studio, the grade, the 0.5x stretch of the held pose (the verdicts are computed on
the stretched frames), the guide's placement (a drag and pinch any user could make), and the people never used the
app: the overlays show what the app would draw on these frames.

**Real strings.** "Hands in pockets", "Hand on hip" (StandingPoses); "Perfect. Hold it" and whatever hint the
stabiliser shows (MatchHints.kt, PHOTOGRAPHER phrasing); "Free for Android. Works offline." (social preview); the
hook question (the viewer's voice, v1).

## 13. Build split

Two builders work in parallel in the same tree. Ownership is by file; shared files take small, local edits only,
and each section of a shared file has one owner.

**BUILDER LIVE: act IV and V (B5-B7, f336-555) and the live-match pipeline.**
- Owns: `src/scenes/Viewfinder.tsx` (the live view, HUD, flash and the colour photo up to f555),
  `src/scenes/viewfinder/*` (rig, match reader; `performer.ts` is retired), `src/ui/Hud.tsx`, `scripts/export-live.ts`,
  `scripts/check-live.ts`, `scripts/detect.py`, `app/src/test/java/com/poseit/app/promo/PromoMatchExport.kt`,
  `public/live.json`, `public/match.json`, the `b5-live` plate and its landmarks.
- Acceptance: the manifest's `b5-live.acceptance` list; capture in f505-531; the lock frame is the hero still.
- Exposes in `shots.ts`: `GUIDE_FRAME`, `guideScreenFrame(f)`, `PHOTO_FIGURE_AT_CUT`.
- `scripts/detect.py` is LIVE's to change; WORLD calls it as is (read-only) for its own plates.

**BUILDER WORLD: everything else (B1-B4, B8) and the chorus pipeline.**
- Owns: `src/scenes/Hook.tsx`, `src/scenes/StudioShot.tsx` (now the pick only, from f272), `src/scenes/studio/*`,
  new `src/scenes/People.tsx` and `src/scenes/people/*` (B2 and the chorus), `src/scenes/TheName.tsx`, `src/world/*`
  (a new footage layer with the Studio ramp goes here: `src/world/Footage.tsx`, which LIVE also uses read-only),
  `scripts/footage.mjs` (fetch and plates from the manifest, if scripted), `pose-core/src/test/kotlin/com/poseit/app/tools/PromoChorusMatch.kt`,
  `public/footage/b2-alive.json`, `public/footage/chorus.json`, the plates for `b2-alive` and `c1`-`c9`.
- Exposes in `shots.ts`: `HOOK`, `B2_BODY_AT_CUT`, `HERO_AT_CUT`.

**Shared** (small local edits, owner per section):
- `src/beats.ts`, `src/scenes/timing.ts` (director's plan; WORLD owns `HOOK_TIMING`, `STUDIO_TIMING`,
  `PEOPLE_TIMING`, `NAME_TIMING`; LIVE owns `LIVE_PLAN`, which the real `MATCH_EVENTS` supersede).
- `src/scenes/shots.ts` (sections as above).
- `src/Promo.tsx` (WORLD swaps the act II placeholder for `People` when it lands; LIVE does not edit it).
- `src/events.ts` (WORLD; act IV events already derive from `MATCH_EVENTS`, so LIVE edits only if it adds an event).
- `src/theme.ts` (no edits expected; the director added `theme.footage`).
- `src/footage/*` (director; read-only for both).
- `src/audio/voiceover.ts`, `scripts/gen-audio.mjs`, `sound-plan.md` (WORLD, after LIVE's act IV frames settle).
- `NOTE.md` (both append their own sections).

**Placeholders in the tree now:** `Promo.tsx` already assembles the five v2 acts; act II (f80-272) renders v1's studio
chorus until `People.tsx` exists; `StudioShot` still shows Disco point and `Viewfinder` the v1 performer until each
builder replaces them. `npm run check` passes on this state.

## 14. Left out on purpose

- **The v1 studio chorus and "everyone freezes" joke** (B2-B3): replaced by real people; the studio stays for the one
  beat where all 99 poses matter.
- **Pose names and scores in the chorus:** unreadable at 8-16 frames, and the one name is saved for the answer.
- **Coverr clips**, including the only clip with a phone photographer and a full body: the licence's ban on evaluating
  AI models.
- **The phone-photographer cutaways and reaction shots:** another camera app's interface, branded phones, and a
  smiling-at-the-photo shot would read as an endorsement.
- **Anyone shown as awkward** (the "unsure" clip): the stick figure carries the freeze.
- **Keying the green screens onto paper:** the ramp already turns them into a grey seamless.
- **Slow motion of real movement:** interpolating a hair flick tears; only the near-still hold is stretched.
- **A group of three or more:** no free clip frames a group in 9:16.
- **Feature tour** (library filters, editor, pose from a photo, photoshoot mode, voice coaching, settings): one
  question, one pose, one photo.
