# Poseit promo: build notes

The plan is in `STORYBOARD.md`, the sound in `sound-plan.md` and the voice-over script in `vo.txt`. This file says
what is built, what is real and what is illustrative, and records what was checked. It grows with the film.

## Status: picture and sound built; the voice waits for a key

Every beat of the storyboard is built in `src/scenes/` and assembled in `Promo` (664 f, 22.13 s, 1080 x 1920). The act
III numbers, colours, cards, hold and countdown are the app's own verdicts: the app's PoseMatcher, HintStabilizer and
CaptureCoach run frame by frame over the keyframed body in a JVM test. The picture events are exported for the
sound stage in `src/events.ts`. The sound is synthesized from those events by `scripts/gen-audio.mjs` (the SFX kit, the
score and the mix, -14 LUFS), and the composition plays it (`src/audio/Soundtrack.tsx`). The voice-over (`vo.txt`) has a
slot (`src/audio/voiceover.ts`) and waits for an ElevenLabs key; see `README.md`, "Voice-over".

## Run it

```bash
cd promo
npm install
npm run poses        # re-export public/poses.json from pose-core (only after the pose library changes)
npm run assets       # copy the app's screenshots into public/screens/ (gitignored, used by UiFragment)
npm run check        # typecheck + mannequin parity against pose-core + live.json is in step with the performer
npm run match        # after editing the performer: export public/live.json, run the app's matcher -> public/match.json
npm run events       # print the picture events (src/events.ts) to out/events.json for the sound stage
npm run audio        # events + scripts/gen-audio.mjs: the SFX kit, the score and public/audio/mix.wav (draft/render/studio run it first)
npm run studio       # Remotion Studio
npx remotion still src/index.ts Promo out/f.png --frame 40
npm run draft        # half-size draft to out/draft.mp4
python3 <motionmaxxing>/scripts/look.py out/draft.mp4 --out look --expect 22.13 --expect-audio
```

`remotion.config.ts` points Remotion at the Playwright headless shell in `/opt/pw-browsers` when it exists (set
`REMOTION_BROWSER` to use another one). Renders go to `out/` and review sheets to `look*/`; both are gitignored.

## Map

| Path | What it is |
|---|---|
| `src/theme.ts` | The one theme object: Studio tokens, signal colours with the app's `forScore` ramp, Archivo roles, the 112.5 BPM grid, measured eases, the two springs, the camera HUD's real dp sizes and its magnification. |
| `src/fonts.ts` | Loads the app's six static Archivo cuts as "Archivo" (400, 500, 600) and "Archivo Expanded" (500, 600, 700). Rendering waits until they load. Also has `fitTextToWidth` (the wordmark is 0.50 W of ink). |
| `src/beats.ts` | The beat and act windows, the chorus snap frames, the hook's word timings and the real strings. |
| `src/figure/` | `types.ts` is the pose model. `poses.ts` reads the 99 real poses. `shapes.ts` is FigureShapes ported to TS. `Mannequin.tsx` draws the silhouette, outline and stick styles with the app's paint rules. `interpolate.ts` blends skeletons along the kinematic chain and handles keyframes, flipping and mirroring. |
| `src/world/` | `camera.ts` is the studio camera: perspective, the lattice, placing poses, zooming about a point, lens shift. `StudioPaper.tsx` is the per-pixel lit seamless paper. `StudioFigures.tsx` draws figures depth-sorted, with contact shadows and depth blur and haze. `ContactShadow.tsx`, `LiveView.tsx` (black live view, thirds grid, sensor noise) and `Grain.tsx` complete the worlds. |
| `src/ui/` | `Hud.tsx` holds the cue card, score ring, countdown numeral, frame marks and capture flash, rebuilt from `CameraHud.kt`. `UiFragment.tsx` shows a real screenshot magnified by the camera. |
| `src/motion/` | `index.ts` has tween, keys, land, leave, arrive, focusIn, snapTail, drift, stepCounter, onTwos and sway. Every curve is eased and clamped. `WordReveal.tsx` reveals whole words at speech rhythm. `CameraGroup.tsx` puts the film camera on one transform. |
| `src/scenes/` | The film. `Hook.tsx` (B1), `StudioShot.tsx` (B2-B4, one continuous shot), `Viewfinder.tsx` (B5-B7), `TheName.tsx` (B8). `shots.ts` solves every seam from the shot on the other side (the hook frame, the guide frame, the act III film camera). `timing.ts` holds the hand-set frames. `studio/camera.ts` is the act II camera (the match zoom, the dolly and truck, the scale cut, the push solved per frame onto the guide's frame, the depth of field that follows the hero). `studio/chorus.ts` is what each of the 99 slots shows at each frame. `viewfinder/rig.ts` is the app's 9:16 scene space and forward kinematics, `viewfinder/performer.ts` the keyframed live body, `viewfinder/match.ts` the app's verdicts read from `public/match.json`. |
| `src/events.ts` | Every picture event with its frame (cuts, snaps, the three hits, the two silences), derived from the sources above, for the sound stage; `CUES` names the same frames for the sound script. |
| `scripts/gen-audio.mjs` | The sound: the SFX kit, the score and the mix, synthesized and placed from `out/events.json` (deterministic). Writes `public/audio/` (gitignored, rebuilt by `npm run audio`). |
| `src/audio/` | `Soundtrack.tsx` plays `public/audio/mix.wav` from frame 0. `voiceover.ts` is the voice-over slot: the frame and window of each line of `vo.txt`. |
| `scripts/export-live.ts`, `../app/src/test/java/com/poseit/app/promo/PromoMatchExport.kt` | The live-match pipeline: the performer and the guide in scene space -> `public/live.json` -> the app's PoseMatcher + HintStabilizer + CaptureCoach at 30 fps (app defaults: strictness 0.80, 700 ms hold, 3 s countdown, front camera) -> `public/match.json`. |
| `src/dev/` | Check compositions (`Dev-*`), rendered as stills: the figure sheet, blends, the studio, the viewfinder and the end frame. They are not part of the film. |
| `scripts/check-shapes.ts` | Checks the TS geometry against Kotlin for every figure. |
| `../pose-core/src/test/kotlin/com/poseit/app/tools/PromoPoseExport.kt` | Writes `public/poses.json`. |

## Real, rebuilt, illustrative

- **Real:**
  - All 99 poses. `BuiltInPoses.all` is exported by a pose-core test, with joints, visibility, body unit and
    `FigureShapes.build` primitives.
  - The mannequin geometry. The TS port matches Kotlin on all 126 figures, with a worst error of 1.3e-5 frame
    heights.
  - The paint rules (contour, face dot, per-limb colours, stick skeleton) and the `forScore` colour ramp
    (Oklab lerp, like Compose).
  - The Archivo font files.
  - The strings ("Your feet are cut off. Step back", "Raise your left arm", "Perfect. Hold it" from
    MatchHints.kt; "Disco point"; "Free for Android. Works offline.").
  - The HUD sizes from CameraHud.kt: the 56 dp ring with a 2 dp ring and a 4 dp hold arc, cue card padding
    16/12 dp with an 8 dp dot, and the 96 sp countdown with a 6 dp contour.
  - **Every act III verdict.** The score and badge percent, each limb's colour, which card is shown and when, the
    hold arc, the countdown seconds and the capture frame are what the app's own PoseMatcher, HintStabilizer and
    CaptureCoach return, frame by frame at 30 fps, for the keyframed body against the Disco point guide in the
    app's 9:16 scene space with the app's defaults (`public/match.json`, written by `PromoMatchExport.kt`). The
    matched score is 95-96 and moves by a point as the raised arm drifts.
  - The overlay paint (CameraOverlay.kt): the guide at the default 55% overlay opacity, neutral white until a person
    is assigned, then `forScore` per limb; the live skeleton in the stick style at 70%, 2.5 dp, visible joints only.
  - The HUD's motion (CameraHud.kt, CameraScreen.kt): hint cards crossfade (180 ms in, 120 ms out); the banner is
    removed when the countdown starts; each digit lands from 115% over 200 ms with a 150 ms fade while the last one
    fades in 100 ms; the countdown sits at the preview's centre; the capture flash snaps to 85% white and decays over
    280 ms (LinearOutSlowIn).
- **Rebuilt (declare in the film's notes):** the camera HUD. It is drawn from the app's tokens and code, not
  screen-captured, and magnified by `theme.hud.k` = 1.95, so cue text is 0.04 H. Its vertical arrangement (card on
  top, ring at the right below it) is the film's; the app stacks the ring above the card at the top right.
- **Illustrative:**
  - The stiff stance and its wrist flick (`promo_stiff`, `promo_stiff_flick`). They are built with the app's pose
    DSL but are not library poses.
  - The studio, its 11 x 9 lattice (even rows staggered half a column) and its light.
  - The live body: `src/scenes/viewfinder/performer.ts` is hand-keyframed (the hook's stiff stance too close, the
    left arm's sweep, the hip hand, two steps back that cross the legs, breathing), not a recording. Its joint
    visibility is MediaPipe-like (high inside the image, low past the edge). The app's verdicts above are real for
    this body.

## The film as built

| Beat | Frames | Buys | What is on screen |
|---|---|---|---|
| B1 hook | 0-79 | Recognition: the viewer sees their own freeze. | The live stick skeleton stands stiff in the black viewfinder and fidgets; "What do I do / with my hands?" rises a word at a time (What f-3 ... hands? f29); the wrists flick up (f29-38) and flop back (f38-46); the awkward pause; film camera 1.00 -> 1.04. |
| B2 turn | 80-111 | The joke: everyone freezes like you. | Hard cut on the downbeat: the hero lay figure takes the skeleton's exact frame (a lens zoom solved from the hook's frame); a one-frame snap to 0.7 of that zoom at f81 and a softLand to the wide framing by f100: 99 stiff lay figures on lit seamless paper. |
| B3 proof | 112-271 | Abundance, counted without a number. | Eleven category snaps (on beats, then an eighth-note ripple three rows per frame, then couples and groups), every slot a different pose of the category, each set down from about 2% H; a two-push dolly (0.85 m) and a 0.45 m truck; f248-271 the group crowd holds (the drop). |
| B4 turn | 272-335 | Choice: one pose picked out of all of them. | All 99 in Disco point with a one-frame scale cut; f288 the detail screen's frame marks snap around the hero, "Disco point" resolves under them and the focus racks onto its row; f304-335 an accelerating push (2.5 m dolly, lens solved every frame onto the guide's frame); the marks open out past the frame edges and the name leaves under the bottom edge. |
| B5 proof | 336-399 | The camera sees you and says what to change. | Match cut: the picked figure becomes the see-through guide (within 2.6% of its size and place), the push lands by f342; f343 the body is found (ring 23, red); f355 "Raise your left arm"; the arm sweeps up (the film's one spring, overshoot peak f383), the right hand comes to the hip; the guide's left arm turns green at f376, the ring amber at f377. |
| B6 proof | 400-463 | The match: it catches the feet and locks when you fit. | f400 "Your feet are cut off. Step back"; two steps back (f418, f440) that cross the legs; f443 the feet are in, the score reaches the threshold (90), every limb green, the ring turns green and its hold arc runs round; f455 "Perfect. Hold it". |
| B7 proof | 464-553 | Hands-free: nobody touches the phone. | The card is removed and the countdown runs at the app's speed: 3 (f464), 2 (f494), 1 (f524); the score holds at 95-96; the film camera creeps 1.03 -> 1.07 and steps 2% on each digit; f546-553 half a beat of stillness. |
| B8 cta | 554-663 | The name, free on Android. | The app's capture flash over the new ground: the same figure, solid, at the guide's frame on paper (the photo); f576-588 it relaxes on twos into the launcher pose; f600-624 the camera pulls back to the cove; f620 "Poseit" lands; f628-645 "Free for Android. / Works offline." resolves; resolved hold to the end. |

### Changed from the storyboard, and why

1. **Act III order follows the app.** The storyboard had "Step back" before "Raise your left arm". Given the stiff body
   standing too close, the app's matcher says "Raise your left arm" first: a pose hint at severity 0.90 outranks the
   cut-off feet at 0.62. So the film goes arm first, then the step back, then the lock. B6 (importance 3) is now the
   step back into the outline and the lock.
2. **Cut-off legs stay green.** The matcher scores only the segments it sees, so the guide's legs do not turn red
   while the feet are out of frame. The cut-off shows in the score (about 60) and the amber ring. The storyboard's
   "legs turn red, amber, green" is not what the app does, so it is not in the film.
3. **Act III runs on the app's clock.** Cards appear when the HintStabilizer shows them (400 ms on top, then at least
   1500 ms up), the hold is 700 ms and the countdown is 1 s (30 f) per digit, not the 32 f half note, so B7 ends at
   f554, not f560. The body's motion was tuned so the arm card lands at f355, the feet card on the f400 downbeat and
   the countdown on the f464 downbeat. As a result "Perfect. Hold it" is up for 9 frames before the countdown
   removes it, as in the app; the lock reads from the green guide, the ring and its hold arc.
4. **The HUD moves like the app:** crossfading cards, digits from 115%, the banner removed at the countdown, the
   85% / 280 ms flash. The storyboard had a drop-in card, digits from 1.5x, a sliding card exit and 2 white frames.
5. **The countdown is centred at 0.5 H**, where CameraScreen aligns it (the storyboard had 0.60 H).
6. **The lattice's even rows are staggered half a column**, so nobody stands directly in front of the hero: it is
   whole on the f80 match cut and at the pick.
7. **The frame marks leave before the cut.** They open out past the frame edges instead of parking at the frame
   corners, which would read as the corner-bracket chrome motionmaxxing forbids.
8. **A rack focus at the pick** (f288-296) isolates the hero's row; the army behind softens further on the push.
9. **The B8 lockup is 8-12 f later** (wordmark f620, end line f628-645) so the end hold measures 1.30 s, inside G3.
10. **The hook's first word starts at f-3**, so frame 0 opens already moving.
11. **The chorus moves more:** a 2% H set-down (not 1%), a 0.85 m dolly and a 0.45 m truck for row parallax (the
    storyboard's 1.00 -> 1.08 measured too little motion between snaps).

## Review (look.py on the half-scale draft)

```bash
npx remotion render src/index.ts Promo out/draft.mp4 --scale 0.5 --codec h264 --crf 20
python3 <motionmaxxing>/scripts/look.py out/draft.mp4 --out out/look-draft4 --expect 22.13
```

Four drafts. Draft 1: the guide's arm and the matched body disagreed by about 20 degrees at a score of 93; the end
hold was 1.67 s; row 0 hid the hero's legs on the match cut; the first frame of every fade was blank. Draft 2:
spread the matched body's error so it lines up at 95-96; staggered the rows; fades start one frame in. Draft 3:
re-timed the lockup (G3 passed). Draft 4: more camera travel in the chorus, the countdown creep, the marks leaving
before the cut.

**Computed gates (draft 4):**
- G0 render exists: PASS. 22.13 s, 664 frames, matches `--expect 22.13`. No audio yet (not planned at this stage).
- G2 no empty frame: PASS. No run of more than 3 flat frames.
- G3 end card: PASS. End hold 1.30 s (0.6-1.4). The final shot, 18.47-22.13 s, is 3.67 s, 17% of the film
  (limit 25%).

**Gates judged by eye** (sheet, mid, strips and full-size stills of f79, f80, f272, f296, f460, f663):
- G1 proof readable: cue text 0.04 H on cards 0.5-0.75 W, the score numeral 0.048 H, the countdown 0.26 H, the pose
  name 0.045 H. The viewfinder is the whole frame, in its own world (black live view, grid, sensor noise).
- G4 one hero per frame: the skeleton with its question; the army as one mass; the framed figure with its name; the
  guide and skeleton as one unit with the card and ring; the figure, then the lockup. There are no side-by-side
  panels.
- G5 no page chrome: there are no corner labels, counters, timecodes, progress bars, tracked caps or kickers.
  `lint.mjs` reads HTML films, so it does not apply to this Remotion film. The ring and the cards are the product's
  UI. The frame marks belong to the figure and leave before the cut.

**Motion:** the mean frame-to-frame luma change is 2.8/255, below the human band (median 6.8, IQR 4.4-9.8).

| Stretch | Luma change (96 px wide) | Why |
|---|---|---|
| B1 | 1.1 | The awkward silence, by design. |
| B2 | 7.1 | |
| B3 | 3.2 | Median 1.1 between snaps. |
| B4 hold | 1.1 | The read. |
| B4 push | 6.6 | |
| B5 | 1.5 | |
| B6 | 1.8 | |
| B7 | 0.7 | The held pose. |
| B8 | 0.6-1.1 | Ending small. |

The film is a quiet joke around one loud chorus.

**The sheet, one sentence a frame (draft 4):**
1. 0.67 s: the black viewfinder, the stiff stick skeleton, "What do I do / with my" rising.
2. 2.07 s: the whole question over the stiff body.
3. 3.43 s: 99 stiff lay figures in staggered rows on lit seamless paper.
4. 4.83 s: the army snapped into leaning and walking poses.
5. 6.20 s: another category, a dense crowd.
6. 7.60 s: couples, two figures per slot in white and grey tones.
7. 8.97 s: groups; the darker crowd holds in the drop.
8. 10.37 s: the framed hero against a racked-soft army, "Disco point" under it.
9. 11.73 s: the stiff body too close; the guide's arms red, the rest green; ring 25.
10. 13.13 s: "Raise your left arm"; the arm is up, the hand on the hip; ring 60, amber.
11. 14.50 s: "Your feet are cut off. Step back"; stepping back; ring 70.
12. 15.90 s: "3" over the matched body; ring 95, green.
13. 17.27 s: "2"; ring 96.
14. 18.67 s: the photo: the same Disco point as a solid lay figure on paper.
15. 20.03 s: relaxed into hand-on-hip as the camera pulls back.
16. 21.43 s: the lockup resolving: the figure, "Poseit", the end line.

**What I would still improve:**
- The group crowd (f240-271), in the app's darker person tones, reads as a heavy grey mass.
- "Perfect. Hold it" is up for only 9 frames (true to the app), too short to read on its own.
- The countdown numeral covers the guide's head (true to the app's centred placement).
- Cards and digits cross-fade with 3-4 frames of overlap (the app's AnimatedContent), which looks busy in stills.
- B7 is the stillest stretch; the sound has to carry the build.
- Figures are camera-facing cards, so a figure very near the camera stands taller than true perspective. This shows
  only on the push, out of focus.
- The f272 unison frame has the soft front row across its lower third; a higher unison framing might make a
  stronger screenshot.
- Not done in this step: the full-size render and the voice (the sound is now built; see "Sound").

## Sound (as built)

`npm run audio` reads `out/events.json` and synthesizes everything in plain Node at 48 kHz in about 3 s. Every noise
source is a named, seeded generator, so two runs give byte-identical files (21 files checked by sha256). The plan is
`sound-plan.md`, and this section records what was built and measured.

**The score** is a dry studio-disco groove at 112.5 BPM (16 f per beat), in A minor with the dorian F#. The hats are
shutter-click transients, and the keys are a two-operator FM electric piano. The score enters with the first kick on the
f80 cut. The clap joins on bar 3, a 16th tick layer joins on the eighth-note snaps, and the bass climbs D-E-F#-G# with
them. The keys only tease with offbeat chops. At f248 everything stops dead, and the hit chord's own room tail plays
backwards into the unison. At f272 the low hit, the bright Am9 stab, an air crash and the unison snap land together.
The last beat before the match cut lifts (no kick, 16th hats) so the push's whoosh owns the cut. Under the coaching
cards there is no kick and no clap, just a sparse bass figure, soft hats and two warm chords, 6 dB down. The countdown
switches to the app's own clock: a kick every 15 f, an A pedal with its filter opening, and the shutter-click hats
speeding up like a motor drive winding up (eighths, then 16ths). At f546 everything stops, and only the room is left
until the shutter. At f600, when the camera starts to pull back, one Am(add9) chord sounds and rings out under the name
to the last frame. The bass is sidechained to the kick (7 dB, 90 ms) so the two never pile up.

**The SFX kit** has four families: clicks (the snaps, the pop, the wooden joint), the shutter, the tones (card tick,
countdown beep, lock chime) and air (two whooshes, the swell). Fabric and the low hit are one-offs.
- The shutter is modelled on a camera's mechanical click, because the app plays `MediaActionSound.SHUTTER_CLICK`. The
  mirror and first curtain make a hard double strike through the body's modes. 68 ms later come the second curtain
  and the mirror's slap, then a low thump, a faint spring ting and a small room.
- The countdown beep is the app's own: `VoiceCoach.tick()` plays `ToneGenerator.TONE_PROP_BEEP`, a 1200 Hz tone, for
  120 ms. It is tuned 37 cents down to D6 so it sits in the key as a suspended fourth over the A pedal. All three
  digits beep at the same pitch, as in the app.
- The lock is a latch seating (two ticks 16 ms apart) and a soft E5-B5 confirmation.

**Placement.** 50 cues, each placed from a named frame in `CUES`, with the transient on the frame's first sample.
- Three hand-placed hits: the unison (f272), the lock (f443) and the shutter (f554). Each has something cleared before
  it: the 0.8 s drop, the coaching bed 6 dB down, and the half beat of silence.
- One click per chorus snap (one family). The eighth-note snaps rise a semitone each and ripple 1-3 f after, couples
  click twice and groups three times.
- The card ticks at f355 and f400. "Perfect. Hold it" (f455) gets no tick, because it comes 12 f after the lock and is
  up for 9 f; a lock, a tick and a beep inside 21 frames would be wall-to-wall.
- The hook's fidget gets fabric, and the f576-588 relax gets a quiet wooden joint tick on each "on twos" step.
- The wordmark and the end line get no sound: the chord is already ringing.

**Mix.** Music bus automation:
- "Disco point" dips the music 4 dB from f288, and it comes back for the push.
- The coaching section is 6 dB down.
- The countdown builds from -7 dB to -3 dB, so it stays under the chorus and under the shutter.
- The music is muted from f546 until just before the final chord.

Room tone (filtered pink noise, about -47 dBFS rms) runs the whole film, so no silence is dead air. The master is
normalised to -14 LUFS (our own BS.1770-4 measurement) through a look-ahead true-peak limiter at -1.5 dBTP (4x
oversampled), which leaves 0.5 dB for the AAC encode. The limiter works 1-2.7 dB on chorus kick transients and 3.8 dB
for about 150 ms on the unison downbeat.

| Section (gen-audio report) | Frames | Mean loudness |
|---|---|---|
| Act I hook (room, fidget) | 0-80 | -38.5 LUFS |
| Groove A + chorus | 80-248 | -11.3 |
| The drop | 248-272 | -25.7 |
| Unison + groove B | 272-336 | -13.4 |
| Coaching (music -6 dB) | 336-464 | -19.2 |
| Countdown build | 464-546 | -14.8 |
| Stillness | 546-554 | -41.3 (room only) |
| Shutter + relax | 554-600 | -23.1 |
| Final chord + name | 600-664 | -18.4 |

Momentary loudness (400 ms) and peak at the hits:
- The unison: -10.2 LUFS, at the ceiling, equal to the loudest chorus snap (-9.9), with 0.8 s of drop before it.
- The lock: -18.3 LUFS, over a bed of -19.
- The shutter: -17.4 LUFS, -1.5 dBFS. It is the loudest SFX and arrives after 0.27 s of room tone.

**Review of the half-scale draft with sound** (`look.py out/draft.mp4 --expect 22.13 --expect-audio --events
out/cuts.events.json`):
- AUDIO: integrated -14.0 LUFS, LRA 7.2 LU, true peak -1.1 dBTP (after the AAC encode). Audible 0.02-22.14 s, with
  no silent tail and no dropouts. 34 onsets; all 3 cuts land within 80 ms of an onset. On f80 the groove enters on
  the cut. On f336 the coaching section's first bass note and chord change the arrangement on the cut, after the
  push's whoosh. On f554 the shutter is the cut. No flags.
- G0 render exists: PASS, with an audio stream. G2 no empty frame: PASS. G3 end card: PASS (end hold 1.30 s). The
  picture is unchanged.
- First draft, fixed: look.py flagged "silence for the first 0.98 s", because the room tone at -55 dB rms read as
  nothing. It now sits at -47 dB.

**Listen-proxy.** No ears in this session, so the sound was checked by measurement:
- The rendered film's audio against `mix.wav` (cross-correlation): offset 0 samples, and the AAC residual is
  -49 dB rms. Remotion plays the mix sample-aligned.
- Transient onsets on the isolated SFX stem (1 kHz high-pass, 0.25 ms envelope): 39 of 43 transient cues start
  0.1-0.9 ms after their frame, and none is early. The four not resolved are masked by design: the first ripple tick
  under its own snap, the unison jump over the swell (+11 dB) and the f455 tick, which was then dropped.
- look.py's onsets report -17 ms at f80 and f554. That is its 10 ms window granularity (it flags the window before
  a jump), not an early sound.
- Waveform, level lane and log spectrogram with the cuts, hits and silences marked (drawn from `mix.wav`) show
  the arc: silence, the chorus wall, the swell into the unison, a thin bed with the lock's chime, a build that stays
  under the chorus, silence, the shutter, one chord ringing out.
- Fixes this loop made:
  - The unison stack clipped into the limiter by 6.8 dB. The five keys voices all started at phase 0 on one sample,
    so they now get seeded random phases. The low hit carries the bottom of the unison, and the bass is sidechained.
  - The shutter was 5 dB under the ceiling. It is now the loudest SFX.
  - The coaching bed measured -25 LUFS at -8 dB: close to silence on a phone. It is now -19 at -6 dB.
  - The countdown build was as loud as the chorus.
  - The ending chord was too big for "end small" (-16 to -18 LUFS).
- The voice-over path was exercised end to end with synthetic test clips in a scratch folder, not in the film.
  Placement on the frame (clip start = at - lead), the 6/9 dB duck, re-normalisation to -14.03 LUFS and the
  overrun warning all work.

**Sound column (motionmaxxing sound-sync.md 6a):**
- First big sound: the groove's first kick, tied to the f80 hard cut, after 2.67 s of room tone (the joke's silence).
- Reading: the music drops 6 dB under the cards and 4 dB for "Disco point"; the hook's words are read in silence.
- Cleared before the biggest moves: the music is out for 0.8 s before the unison and off for 0.27 s before the
  shutter.
- Hits: 3, hand-picked. The chorus clicks are the groove's percussion, one family at -11 to -20 dB, not hits.
- Coverage: the score covers the whole cut. The final chord and the room run to f664.
- Metronome: the groove sits on the picture's 16 f grid because the picture is cut on it. The arrangement changes at
  every section: it enters on the cut, stops dead twice, thins under the cards, and switches to the app's 1 s clock
  (an accelerando from 112.5 to 120 BPM).

**What I would still improve:**
- Nobody has listened yet. The timbres were judged by spectrum and numbers, so play it once on a phone speaker and
  once on headphones.
- The kick's body is 44-130 Hz, which phone speakers drop. The tanh drive, the click, the clap and the bass harmonics
  carry the groove there.
- A generated music bed (ElevenLabs music, fitted with `voice.py music-fit --loop-bars --bpm 112.5`) could be warmer
  than synthesis. It must keep the drop, the stillness and the countdown clock, which a looped bed would not.
- Line 5 of the voice-over is tight: 8 words in 80 f is 3 words a second.

## Choices and findings for the scene builder

- **Eases:** the measured curves were re-derived from motionmaxxing's published shape table. A check printed
  softLand 30/59/85/95 and snapSettle 54/84/96/98, matching the table exactly.
  - There is no linear ease. Drifts use `ease.drift` (sine in-out over the whole span), as the storyboard asks.
  - `spring.land` measures 52% of its travel by 4 f and 93% by 10 f, with no overshoot.
  - `spring.theOne` measures 4.3% overshoot at f10. It is the B6 arm only.
- **Studio camera:**
  - `WIDE_CAM` and `LATTICE` were solved to the storyboard's section 9: row 1 has its feet at 0.76 H and is
    0.24 H tall; row 10 is at 0.35 H and 0.07 H tall. The camera is 20 degrees down with a 59-degree vertical FOV.
  - The cove spans about 0.25-0.32 H at that framing (measured on the render).
  - Use `zoomAbout` for the storyboard's scale moves (2.50 -> 1.75 -> 1.00, 1.08 -> 1.18) and `cam.z` for the
    dolly. The paper is re-shaded every frame, so a zoom stays sharp; never CSS-scale the studio.
- **Paper:** measured on `Dev-StudioDemo` f0.
  - The wall reads 227 at the centre (Paper is 228) and the floor 207 (Set is 210).
  - The key from the upper left makes the top-left wall 235 and the top-right 221.
  - The tooth is fixed to the surface and fades where it would go below a pixel. Frame-to-frame change on the wall
    during a dolly is 0.19/255, which is grain only, with no shimmer.
- **Shadows:** `placePose` gives one oval per person. It sits under the ankles when they are on the floor (the
  app's rule) and otherwise under the figure's ink (lying, crouching). It is nudged 5 cm right, away from the key.
- **Sitting poses:** the library has no furniture, so sitters sit on an invisible seat with their feet on the floor,
  exactly as in the app's tiles.
- **B8 relax:** blend `mirrored(disco)` -> `stand_hand_on_hip`, not `flipForDisplay(disco)`. Both draw the same
  figure, but only the mirrored one has its labels on the same screen side as the launcher pose, so no limb crosses
  over. See `Dev-BlendSheet` row 3.
- **Score ring:** in the app the ring is a full thin circle in the score colour, plus a thicker hold-progress arc
  from 12 o'clock that fills while the pose is held before the photo. The storyboard's "arc closes to the score" is
  the hold arc, so drive `hold` (0..1) for "the ring closes".
- **Cue card weight:** the app uses Archivo Medium (bodyStrong), not Regular. The theme follows the app.
- **Cue card size:** at the app's proportions, a two-line card at 0.04 H text is about 350 px tall. Decided: the
  card's top at 0.13 H and the ring at (0.83 W, 0.40 H), clear of the two-line card and of the raised arm.
- **Thirds grid:** the default is the storyboard's 20% white at 2 px. The app draws 42% at 1 dp with a shade line;
  pass `gridColor` to compare.
- **Render speed:** in this container, 96 studio frames (99 figures plus paper) render at full size in about 24 s,
  and a 60-frame half-size draft takes about 10 s.

## Licences

- **Remotion:** source-available, not open source. It is free for individuals and for companies of up to 3
  employees; larger companies need a company licence (remotion.dev/license). This is a personal project, so the free
  licence applies.
- **Archivo:** SIL OFL 1.1. The fonts are copied from the app with the licence (`public/fonts/OFL-Archivo.txt`).
- **The skills used:**
  - motionmaxxing (Apache-2.0, Tejas Makwana): no files were copied. The ease shapes were re-derived from its
    published measurements, credited in `theme.ts`. Its `look.py` is run from its own folder for render review.
  - remotion-motion-graphics (MIT, haidrrrry): no files were copied. Its theme template shaped `theme.ts`, and its
    rules are followed (eased and clamped interpolation, fps-derived timing, one theme, render and look).
- **Audio:** all of it is our own synthesis (`scripts/gen-audio.mjs`): no samples, no generated music, no voice. The
  remotion skill's `gen-sfx.mjs` / `gen-track.mjs` (MIT) and motionmaxxing's `sound-sync.md`, `mix.py` and `voice.py`
  (Apache-2.0) were read for technique; no code was copied. There was no ElevenLabs key in this session, so the VO
  script waits in `vo.txt` with its slot in `src/audio/voiceover.ts`.
