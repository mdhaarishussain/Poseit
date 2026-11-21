# Poseit promo: build notes

The plan is in `STORYBOARD.md`, the sound in `sound-plan.md` and the voice-over script in `vo.txt`. This file says
what is built, what is real and what is illustrative, and records what was checked. It grows with the film; the newest
state is at the top and v1's record is at the end.

## Status v2: integrated and reviewed once (real people, the app's graphics on top; picture and sound built, the voice waits for a key)

The whole v2 film is built and assembled in `Promo` (664 f, 22.13 s, 1080 x 1920, 30 fps): real people from Mixkit
stock footage are the subject, and the app's graphics are overlays on them, as on a phone. The two builders' parts
(sections "Act IV as built" and "Acts I-III and V as built" below) are integrated: every seam is solved in
`src/scenes/shots.ts` and checked on the render, `src/events.ts` follows the v2 cuts and the app's real act IV frames,
and `npm run audio` places the sound on them. The voice-over (`vo.txt`, five lines) has its slot
(`src/audio/voiceover.ts`) and waits for an ElevenLabs key; no placeholder voice is in the film.

| Beat | Frames | On screen | Source |
|---|---|---|---|
| B1 hook | 0-79 | The stiff stick skeleton in the black viewfinder, "What do I do / with my hands?"; the words leave f61-69; the rush f70-79 lands the skeleton on B2's body | drawn (illustrative stance) |
| B2 | 80-143 | A woman on a desert, printed grey; the hook's skeleton becomes hers (f80-86), then the app's live skeleton follows her hands behind her head | Mixkit footage; the app's model and smoothing |
| B3 chorus | 144-271 | Nine cuts on the grid (f144 ... f240), each with the library lay figure the app's matcher fits to that person; f248-271 the drop holds on c9 (the man of the couple clip in "Cuff adjust", re-cropped in review 1 so his partner's face is out of frame) | Mixkit footage; the app's PoseMatcher scores (`chorus.json`) |
| B4 the pick | 272-335 | All 99 lay figures in "Hands in pockets" on studio paper, frame marks, the name, the push onto act IV's guide | drawn from the 99 real poses |
| B5 coaching | 336-400 | A woman in a photo studio, printed grey; the app's see-through guide coloured per limb, her live skeleton, the ring, "Ask them to bend their right elbow more" (f348) | Mixkit footage; every number and card is the app's verdict |
| B6 lock and countdown | 401-511 | The lock (f401, hit 2), every limb green from f404, "Perfect. Hold it" f413-421, 3 / 2 / 1 at f422 / 452 / 482 in the headroom (glyph 0.15-0.28 H), stillness f504-511 | the app's HintStabilizer and CaptureCoach |
| B7 the photo | 512-554 | The app's capture flash (f512, hit 3), then the same frame in full colour, the film's first colour, pushing 1.00 -> 1.06 | the plate's frame at the app's capture frame |
| B8 the name | 555-663 | A solid "Hands in pockets" figure where she stood, the photo's push carried across the cut and settling (2%, f555-600), relaxing into the icon pose (f576-588), the pull back, "Poseit", "Free for Android. / Works offline." | drawn from the real poses |

**Checked on the review-1 draft** (`npm run draft`, half size, `out/draft.mp4`; `look.py --expect 22.13
--expect-audio --events out/cuts.events.json`; the integration draft's numbers are in "Review 1" below):
- G0 PASS: 22.14 s, 664 frames, audio present. G2 PASS: no run of more than 3 flat frames. G3 PASS: end hold 1.30 s;
  final shot 3.64 s, 16% of the film.
- Audio: -14.0 LUFS integrated, LRA 6.9 LU, true peak -1.1 dBFS after the AAC encode (mix.wav -14.02 LUFS, -1.50 dBTP,
  sha256 9021657e9026, unchanged by the review); audible 0.07-22.13 s; 11 of 14 cuts within 80 ms of a sound onset
  (the misses: the eighth-note cuts f216 and f232, whose clicks sit under the groove, and f555, where the colour
  chord rings on through the cut to paper by design); no flags.
- Motion: mean frame-to-frame luma change 3.3/255, below the human band (median 6.8). The only still run look.py
  finds is 20.93-21.63 s (f628-649: the end line resolving over the resolved hold); the B8 breath (0.77 s still on
  the integration draft) is gone. The stillest designed stretches: the hook's awkward pause, the countdown on the 0.5x
  hold (0.55/255 at 96 px wide), the photo (0.52), the B8 breath (0.11 for f555-576) and the end hold.
- Seams, by blending full-size stills on each side of each cut: f79 / f80 the stiff skeleton sits on her body (nose,
  ankles, mid-hip x); f335 / f336 the studio hero lies exactly under the app's guide (`out/int/blends.png`, not
  touched by the review); f554 / f555 B8's figure sits on her head, shoulders and hips in the photo at the new 6% push
  (`out/rv1/blend-554-555.png`; her feet are crossed, the figure's are not).
- By eye (review 1, `out/rv1sheet0-5.png`: every 10th frame plus every cut, hit and landing, 84 frames; then
  `out/rv1bsheet0-2.png` and full-size f380, f485, f554, f555 after the fixes): G1 the cue text is 0.04 H, the score
  0.042 H, the countdown glyph 0.13 H (0.68 of CameraHud's 96 sp), the pose name 0.045 H; G4 one person with their
  overlay per frame (the army with one framed hero in B4); G5 no corner labels, counters, timecodes or captions on
  people. No text sits in the outer 12.5% at the top or bottom (the cue card's top edge is at 0.131 H). Nothing typed
  ever shares a frame with a real person except the app's own cards, score and digits.
- `npm run check` passes (typecheck, mannequin parity against pose-core, act IV's plate, landmarks and verdicts in
  step and the acceptance list).

**Known issues left** (none blocks the cut; each is the app's real output or a documented choice):
- The lock (f401, hit 2) lands while she is still sliding into the guide: at f401 her right arm scores 0.73 (amber
  green) and she is about 0.09 W left of the figure; every limb is green from f404 and she is inside the figure by
  about f450. The strongest still is f420 ("Perfect. Hold it", hold arc 0.91, score 82), 19 frames after the hit.
  What changes on f401 is the ring: it turns green and its hold arc starts, and the film camera steps 1.5%.
- "Perfect. Hold it" is up 9 frames (f413-421): the app's own timing (400 ms stabiliser delay; at the countdown
  `CameraScreen.kt` drops `HintBanner` from composition, `if (ui.hasPose && countdown == null)`, with no exit
  animation, and the 700 ms hold is not a setting). It reads with the lock chime behind it.
- Before the lock the guide is mostly green while she stands beside it (f336-400): the app's matcher compares limb
  directions, not position; the guide stands where the solved placement put it (where she settles).
- c2's hold is 0.57 (her arms leave the cross-legged figure's arms after the landing). In the chorus look (veil and
  edge) arms that cross the torso read only by their elbows in the outline (c9's "Cuff adjust", c1, c4).
- c9's figure head sits about a third of a head left of his face (sit 0.11 torso lengths overall, head part 0.96).
- Mean motion is below the human band (above).
- The dev compositions `Dev-StudioDemo`, `Dev-BlendSheet` and `Dev-Viewfinder` still show v1 content (Disco point,
  the v1 chorus, the front-camera viewfinder). They compile and are not part of the film.
- Only half-size drafts were rendered; the full-size `npm run render` has not been run.

## Review 1 (look and fix)

Rendered the integration commit (11dc257) as a half-size draft, ran look.py, and looked at every 10th frame plus
every cut, hit and landing (84 frames, `out/rv1sheet0-5.png`), the lock frame by frame (f396-416,
`out/rv1lock0.png`), the B4 name's exit every 2nd frame (`out/rv1name0.png`) and full-size f250, f380 and f440.
look.py on that draft: G0 PASS (22.14 s, audio), G2 PASS, G3 PASS (end hold 1.30 s, final shot 3.64 s = 16%);
-14.0 LUFS, LRA 6.9, true peak -1.1 dBFS; 11 of 14 cuts on an onset; motion 3.3/255; still runs 18.50-19.27 s
(0.77 s, the B8 breath) and 20.93-21.63 s (0.70 s).

**Findings, worst first, and what was done** (G = the motionmaxxing gate it touches; "zone" = the outer 12.5% at the
top and bottom of a 9:16 frame, which a feed's UI covers):

1. **The countdown sat in the top zone (G1, zone).** Its glyph spanned 0.09-0.27 H settled and reached 0.075 H in
   the 115% landing, for all 90 countdown frames (f422-511). Fixed in `Viewfinder.tsx` `HUD_LAYOUT`: 0.68 of
   CameraHud's 96 sp (font 0.17 H, glyph about 0.13 H) centred at 0.215 H; glyph and contour now 0.15-0.28 H settled
   and 0.14-0.29 H at the landing, still clear of her hair (0.31 H at f485 under the push, full-size still).
2. **c9 cut a real woman's face in half at the right edge for 32 frames, the drop hold (stock audit, crop).** The
   couple clip's window (x 1183) held the man and half of his partner's face; no 9:16 window holds both, and the app's
   model finds only him. Re-cropped in the manifest (x 946, `recrop` note): the frame now ends just before her face
   (her shoulder and their linked hands stay at the right edge; her hair first enters at plate frame 72, after the
   last frame the shot can show, 61). Rebuilt with the pipeline (`node scripts/fetch-footage.mjs --only c9`,
   `node scripts/landmarks.mjs --only c9`, `PromoChorusMatch` with `--rerun`). The app's matcher now picks
   "Cuff adjust" (flipped) for him: score 0.99, fit 1.00, sit 0.11, hold 0.99 / sit 0.12, offset +15 (was
   "Thoughtful chin rest" 0.89, hold 0.91, whose chin-rest arm read inside the torso). The other eight picks are
   unchanged; `chorus.json`, `c9.landmarks.json` and `plates.json` change; `c9-alt1` / `c9-alt2` came out identical.
3. **The colour photo read as a still (motion; G2 passes).** The 3% push measured as static-ish for its last 1.2 s,
   on the film's emotional peak. `theme.footage.photoPush` is now 1.06 (shoes at 0.95 H on f554); `PHOTO_FIGURE_AT_CUT`
   follows it, so B8's figure still lands on her (blend checked).
4. **The B8 breath was a dead stop after a moving photo (handoff, motion).** A 0.4% creep from rest (0.77 s still in
   look.py) after a cut at peak push speed. `TheName.tsx` now carries the push across the cut: a softLand of 2% about
   the same screen point (her mid-hip) starting near the photo's speed (0.16% against 0.21% a frame), settled by the
   pull back, which is re-solved from the settled camera so the lockup framing is unchanged. 2.6% matched the speed
   but put the feet on the bottom edge; at 2% they end at about 0.98 H.
5. **The pick's name crossed the bottom zone readable (zone).** "Hands in pockets" rode the marks down and was legible
   below 0.875 H for about 8 frames (f319-327). `StudioShot.tsx` now takes it out (opacity and blur, accelExit) as its
   top goes from 0.80 to 0.875 H: it reads f288-318 and is gone before the zone.
6. **Run it had a silent no-op.** `./gradlew :pose-core:test --tests ...PromoChorusMatch` is up to date after a plate
   changes (the JSON inputs are not declared), so it writes nothing; `--rerun` added to the command (the landmark and
   footage-match scripts already pass it).

**Looked at and kept** (with the reason): the lock's timing, the 9-frame "Perfect. Hold it" and the guide green
beside her (the app's real output, under "Known issues"); the cue card's 0.94 W, whose right edge meets the white
softbox (the text at 0.04 H needs it for the app's two lines, and the app's card has no border either); the hook's
first word sitting left of centre on f0 (words are laid out at their final positions); the 1.25x veil whiting out
most of the frame on each landing's first frame (one or two frames, the landing itself); the cut-sync misses f216 and
f232 (79% of cuts already on an onset; raising the clicks would make the eighths metronomic); signal colours appear
only in act IV and the cue dots (match feedback), full colour only in the photo; no dead air (the longest stretch
without a new element is the countdown's 30 f per digit); every entrance moves on at least two properties.

## Integration (v2): what this stage changed

- **Shared files resolved.** `src/events.ts` (WORLD's v2 rewrite) keeps CUES' shape and now also says what the app
  says at the lock: the hit-2 label is "score 75 reaches the match threshold, the hold arc starts" (WORLD's label said
  every limb was green at f401, which the verdicts contradict), a new event marks every limb green (f404, computed from
  the verdicts), and a new `CUES.colour` (capture + 8 = f520, the flash decayed) is where the final chord blooms.
  `shots.ts` sections are as each builder left them; its header now documents `HOOK.cam` and `B2_BODY_AT_CUT`.
- **Sound follows the picture** (`scripts/gen-audio.mjs`, `sound-plan.md` "v2 as built"): the final chord moved from
  the pull back to the colour (f520) with the music un-muting there, a soft low A re-strike at f600 keeps the name off
  a dying tail, the bass climb and the octave A figure no longer overlap on f208-240 (the eighths now start on bar 3),
  and the v1 labels are gone. Hits: unison f272, lock f401, shutter f512.
- **Voice-over slot in step** (`src/audio/voiceover.ts`): lines 3 and 4 are derived from the app's verdicts (line 3
  f344-401, from 4 f before the first card to the lock; line 4 f426-504, from 4 f after the "3" to the stillness).
- **The v1 live path is removed:** `scripts/export-live.ts`, `src/scenes/viewfinder/performer.ts`,
  `app/src/test/java/com/poseit/app/promo/PromoMatchExport.kt`, `public/live.json`, `public/match.json` and the
  `live` / `match` npm scripts. Nothing in the film read them since act IV moved to the footage. `viewfinder/rig.ts`
  keeps only `templateFrameRect` (the app's fitFrame + OverlayTransform), which `shots.ts` uses.
- **One theme:** the chorus veil values moved into `theme.footage.chorusVeil` (land 0.70, settled 0.34, edge 0.95),
  replacing the unused `chorusGuideAlpha` / `guideLandAlpha`; `people/data.ts` `CHORUS_VEIL` reads it.
- `README.md`: v2 description, `npm run footage` before the first draft, five VO lines, line 4 silent by f504.


## Run it

```bash
cd promo
npm install
npm run poses        # re-export public/poses.json from pose-core (only after the pose library changes)
npm run assets       # copy the app's screenshots into public/screens/ (gitignored; only the v1 UiFragment uses them)
npm run check        # typecheck + mannequin parity against pose-core + act IV's plate, landmarks and verdicts in step
npm run events       # print the picture events (src/events.ts) to out/events.json for the sound stage
npm run audio        # events + scripts/gen-audio.mjs: the SFX kit, the score and public/audio/mix.wav (draft/render/studio run it first)
npm run studio       # Remotion Studio
npx remotion still src/index.ts Promo out/f.png --frame 40
npm run draft        # half-size draft to out/draft.mp4
python3 <motionmaxxing>/scripts/look.py out/draft.mp4 --out look --expect 22.13 --expect-audio
# a fresh clone needs the plates first: npm run footage (the landmark and verdict JSON are committed)

# v2 footage (landmarks and footage-match need promo/.venv: python3 -m venv .venv && .venv/bin/pip install mediapipe opencv-python-headless numpy,
# and on a bare Linux image the system libraries libegl1 and libgles2)
npm run footage        # download the manifest's clips to public/footage/src/ and build the film-time plates
                       # public/footage/<plate>.mp4 (both gitignored) + public/footage/plates.json; skips what is up to date
npm run landmarks      # the app's lite model on every plate (scripts/detect.py -> out/footage/), then the app's own
                       # LandmarkMapping + LandmarkSmoother (pose-core PromoFootageLandmarks) -> public/footage/<plate>.landmarks.json
npm run footage-match  # the app's PoseMatcher + HintStabilizer + CaptureCoach on the footage (app PromoFootageMatch)
                       # -> public/footage/nearest.json and <plate>.match.json; sets b5-live's time remap from the app's
                       # countdown start (manifest timeRemap.measured), rebuilds that plate and matches it again
npx remotion render src/index.ts Dev-FootageSheet out/sheet --sequence --image-format=png --concurrency=2
                       # the alignment proof: one page per plate, four frames each (one with a 1.35x push)
(cd .. && ./gradlew --console=plain -q :pose-core:test --tests 'com.poseit.app.tools.PromoChorusMatch' --rerun)
                       # the chorus poses -> public/footage/chorus.json (committed) and out/chorus-report.txt
```

`remotion.config.ts` points Remotion at the Playwright headless shell in `/opt/pw-browsers` when it exists (set
`REMOTION_BROWSER` to use another one). Renders go to `out/` and review sheets to `look*/`; both are gitignored.

## Real, rebuilt, illustrative (v2)

Said plainly: the people are real people in real stock footage, and every skeleton, every chorus pose choice and every
act IV number, colour, card, hold, countdown and capture frame drawn on them is real output of the app's own code run
on that footage. The people never used the app: the overlays show what the app would draw on those frames.

- **Real app output on real footage:**
  - The footage: Mixkit stock clips under the Mixkit Stock Video Free License (`CREDITS.md`), stock models who
    endorse nothing; muted, never committed.
  - Every skeleton on a person (B2 from f86, act IV): the app's own pose model (`pose_landmarker_lite.task`, the app's
    live settings) on the film's plate frames, then the app's own Kotlin `LandmarkMapping` and `LandmarkSmoother`
    (`*.landmarks.json`), drawn with the app's paint (stick style, Mannequin at 70%, visible joints only).
  - Every chorus pose (B3): the app's `PoseMatcher` (back camera, PHOTOGRAPHER, strictness 0.80) scored every
    single-person library pose, drawn and flipped, on every plate frame (`chorus.json`, pose-core
    `PromoChorusMatch.kt`). The scores are the app's; the selection rule on top (score, fit, sit, hold, no pose twice)
    is the film's.
  - Every act IV verdict (B5-B7): the score, each limb's colour (`Studio.forScore`), which card is shown and when, the
    hold, the countdown seconds and the capture frame are what the app's `PoseMatcher`, `HintStabilizer` and
    `CaptureCoach` return frame by frame on the plate the film shows (`b5-live.match.json`, app
    `PromoFootageMatch.kt`): back camera, not mirrored, PHOTOGRAPHER audience, strictness 0.75 ("Balanced", a real
    setting; the default 0.80 never locks on this footage), 700 ms hold, 3 s countdown.
  - The 99 poses and the lay-figure geometry (the TS port matches Kotlin on all 126 figures, worst error 1.3e-5
    frame heights), the paint rules, the `forScore` ramp, the strings ("Hands in pockets", "Hand on hip", "Ask them
    to bend their right elbow more", "Perfect. Hold it", "Free for Android. Works offline."), the Archivo fonts.
- **Rebuilt (declared):** the camera HUD (ring with its hold arc, cue card, countdown, capture flash) is drawn from
  `CameraHud.kt`'s tokens and sizes, not screen-captured, magnified by `theme.hud.k` = 1.95. Its arrangement is the
  film's (card and countdown in the headroom above her, ring at the right); one card and one digit at a time (the
  app crossfades); the thirds grid set to Off (a real setting).
- **Illustrative:**
  - The hook's stiff stick figure and its wrist flick (built with the app's pose DSL, not library poses) and the
    f80-86 blend from it into her detected skeleton.
  - The studio of 99 figures (B4, B8): its lattice, paper and light.
  - The grade: the footage printed in the Studio ramp (Ink to Sheet) until the shutter.
  - The guide placements: an `OverlayTransform` solved to each person (a drag and pinch any user could make).
  - The chorus guides' look: a veil and a white edge (the app draws its guide as a 55% silhouette, which act IV uses).
  - Act IV's 0.5x stretch of her hold (frame repetition of real frames; the verdicts are computed on the stretched
    frames), and the film camera's zoom on the live view.
  - The colour photo is the plate's frame at the app's capture frame, shown full frame without overlays (as the app
    saves it), not a file the app wrote.


## Footage prep (v2): plates, landmarks and the app's verdicts on real footage

Built by the footage stage for both builders: every manifest clip is downloaded (`npm run footage`, nothing
committed), cut into film-time plates, detected with the app's lite model, smoothed with the app's own Kotlin
`LandmarkSmoother`, and matched with the app's own PoseMatcher, HintStabilizer and CaptureCoach. `Dev-FootageSheet`
proves the overlay sits on the person (15 pages, 4 frames each, one through a 1.35x push): skeleton and raw dots land
on the body on every page; the smoothed joints trail the raw dots by a few px only on fast moves (the One Euro filter,
as on a phone).

| Plate | Clip | Frames (film) | Crop scale | Detected | Note |
|---|---|---|---|---|---|
| b2-alive | mk-34404 | 94 (f65-158) | 1.00 | 1.00 | full body every frame |
| c1 | mk-1236 | 46 (f129-174) | 1.35 | 1.00 | |
| c2 | mk-101459 | 51 (f145-195) | 1.00 | 1.00 | |
| c3 | mk-3191 | 48 (f161-208) | 1.00 | 1.00 | half body; the nearer woman (the selfie-taker) is the one found |
| c4 | mk-50649 | 49 (f177-225) | 1.78 | 1.00 | half body |
| c5 | mk-41665 | 39 (f193-231) | 1.38 | 1.00 | a photographer's camera at the right edge |
| c6 | mk-1236 | 45 (f201-245) | 1.56 | 1.00 | |
| c7 | mk-41664 | 39 (f209-247) | 1.39 | 1.00 | |
| c8 | mk-1236 | 47 (f217-263) | 1.31 | 1.00 | |
| c9 | mk-100743 | 96 (f225-320) | 0.92 | 1.00 | one person only (see below) |
| c9-alt1 | mk-100766 | 62 (f225-286) | 1.00 | 1.00 | one merged skeleton over both people |
| c9-alt2 | mk-50649 | 62 (f225-286) | 1.78 | 0.92 (1.00 in the shot) | half body |
| c1-alt1 | mk-1236 | 46 (f129-174) | 1.35 | 1.00 | same take as c8 |
| b5-live | mk-39676 | 249 (f321-569) | 1.25 | 1.00 | time-remapped |
| b5-live.1x | mk-39676 | 249 (f321-569) | 1.25 | 1.00 | real time, for the hold measurement |

**Act III (the app's verdicts, `public/footage/b5-live.match.json`).** The guide is `stand_hands_in_pockets` fitted at
source 4.0 s (scale 0.906, offset -0.012, +0.015 on the first crop; +0.145 after BUILDER LIVE's re-crop, see "Act IV as
built": the verdicts below did not change). At the default strictness 0.80 it never locks: on the 1x plate the
score is >= 0.80 from f403 to f421 while she arrives in the pose (peak 91 at f408), the 700 ms hold needs one more
frame, and her settled hold scores 0.70-0.78 against the figure ("Ask them to bend their left elbow more"). The
manifest's fallbacks in order: `walk_pockets` at 0.80 behaves the same (>= 0.80 f404-421); the planned guide at 0.75
("Balanced", the next real strictness setting, declared here) locks. With it, on the remapped plate:
- the score crosses into match at f401 and stays there to f473, then holds 0.69-0.73 (above the 0.63 abort line) to
  the capture;
- cards: f348 "Ask them to bend their right elbow more" (POSE), f413 "Perfect. Hold it", f486 "Ask them to bend
  their left elbow more" (during the countdown, when the app removes the banner);
- hold f401, countdown 3 / 2 / 1 at f422 / f452 / f482, capture f512 (inside f505-531: acceptance PASS).
The remap the rule gives from that countdown start is `[[336,0],[422,2.8667],[432,3.1167],[512,4.45]]` (planned:
423 / 433 / 513), now `timeRemap.measured` in the manifest; the capture frame shows source 4.45 s, inside her hold.
For comparison, a guide made by the app's pose-from-a-photo path from her own 4.0 s frame locks at 0.80 (capture
f498); it is not the film's guide (the punchline is the library pose's name). By the matcher, the library pose
nearest her hold at 4.0 s is `lean_foot_on_wall` (0.95); "Hands in pockets" scores about 0.77 there.

**Chorus snaps (`public/footage/nearest.json`).** Every template with the right person count, drawn and flipped,
fitted to the person at the key time. The snap rule now also needs `fit` >= 0.8 (the share of what the person shows
in frame that the fitted template also requires): without it a template pushed mostly out of a half-body frame wins on
the two or three segments left (`walk_twirl` scored 1.00 on c3 and c4).

| Shot | Key time | Pick (score) | Planned (score, rank) | Best in the in-point window |
|---|---|---|---|---|
| c1 | 10.59 s | none: best usable `stand_hands_in_pockets` flipped 0.77 (excluded), `fun_superhero` 0.72 | fun_superhero 0.72, 4 | 10.89 s `port_arms_crossed_smile` 0.89 |
| c2 | 0.50 s | none: `floor_cross_legged` 0.68 | floor_cross_legged 0.68, 1 | 0.58 s `floor_cross_legged` 0.81 |
| c3 | 3.71 s | none: `stand_sunglasses` flipped 0.79 | selfie_classic 0.48, 20 | 4.00 s `stand_sunglasses` flipped 0.87 |
| c4 | 0.50 s | `stand_arms_crossed` 0.99 | port_arms_crossed_smile flipped 0.99, 4 | 0.54 s `stand_arms_crossed` 1.00 |
| c5 | 6.51 s | `port_face_frame` flipped 0.96 | fun_peace_lean 0.92, 3 | 6.46 s `port_face_frame` flipped 0.97 |
| c6 | 6.09 s | `port_arms_crossed_smile` flipped 0.92 | pro_arms_crossed flipped 0.91, 2 | 6.17 s `pro_arms_crossed` flipped 0.96 |
| c7 | 2.79 s | `stand_hand_on_hip` 0.93 | same, 1 | 2.54 s `walk_toward_camera` flipped 0.99 |
| c8 | 0.58 s | `port_face_frame` 1.00 | same, 1 | same |
| c9 | 4.00 s | none: one person detected of two; best couple template 0.47 | couple, not reached | |
| c9-alt1 | 3.00 s | none: one person; 0.49 | couple, not reached | |
| c9-alt2 | 4.21 s | `stand_crossed_ankles` 0.991 | stand_hand_in_hair 0.991, 6 | |

- c5 and c8 both pick `port_face_frame`; `distinct` in nearest.json gives each snap its best unused pose (c8 then
  takes `selfie_mirror_two_hands`, 0.98).
- The couple fails the manifest's condition: MediaPipe finds one person in every frame of c9 and c9-alt1, with the lite
  and the full model, with the manifest crop and a re-centred one. The couple's 2160 file is 3840x2160 (the manifest
  said 4096x2160; corrected) and no 9:16 window holds both of them. c9-alt2 is the manifest's single-person fallback;
  its legs are out of frame, so legs cannot separate `stand_crossed_ankles` from `stand_hand_in_hair` (both 0.991:
  pick by the arms), and its mid-hip fit draws the frontal figure left of her three-quarter body.

**Real and illustrative here.** Real: the footage, every detection (the app's model and settings; VIDEO mode, since a
file has no live clock), the smoothing (the app's Kotlin), every verdict (the app's Kotlin; the coach is ticked once
per 30 fps frame, the app ticks it every 33 ms). The guide placement is a solved drag and pinch any user could make.
The 0.5x stretch is frame repetition of her real hold; the verdicts are computed on the stretched frames. The grade is
an interpretation of the manifest's words: the shot's `trim` is a midtone lift that keeps black and white
(`l + trim 4 l (1 - l)`), the S is 30% of a smoothstep for `contrast` 1.06 (`src/footage/grade.tsx`).

**Remotion note.** `<Freeze frame={n}>` clamps n to the composition's length, so a short composition shows the wrong
plate frame; `Footage` freezes at 0 and trims the video to n instead.

## Act IV as built (v2, BUILDER LIVE): the app's camera screen over a real woman, then the photo

B5-B7 (f336-554) are built on the real footage. `src/scenes/Viewfinder.tsx` draws the `b5-live` plate through
`src/footage` `Footage` (OffthreadVideo on the film's clock, Studio ramp, trim -0.1) and, in plate pixels through the
same transform, what the app's `CameraOverlay.kt` draws with its default settings: the "Hands in pockets" guide in
the silhouette style at the default 55% overlay opacity, each part in `Studio.forScore`'s bright camera colour (the
app assigns her on the first frame), and the live skeleton from `b5-live.landmarks.json` (the app's own One Euro
smoothing, stick style, Mannequin at 70%, 2.5 dp x `hud.k`, visible joints only). Screen-fixed: the score ring with
its hold arc, the hint card and the countdown (`src/ui/Hud.tsx`, unchanged), the capture flash and the film grain.
`viewfinder/match.ts` now reads `public/footage/b5-live.match.json` (bundled, so `MATCH_EVENTS` stay module-level for
`shots.ts` and `events.ts`) and also exports `MATCH_GUIDE` (the guide as the app placed it) and
`MATCH_EVENTS.digits` / `countdown`. (The v1 path, `viewfinder/performer.ts`, `scripts/export-live.ts`,
`PromoMatchExport.kt`, `public/live.json` and `public/match.json`, was kept by this stage and removed at integration.)
`npm run check-live` now checks act IV instead: the plate has the manifest's crop and remap, every landmark
frame and verdict sits on the plate frame and source frame the plate shows, the verdicts are for the back camera,
not mirrored, PHOTOGRAPHER, the picked pose, strictness >= 0.75, and the acceptance list holds.

**The re-crop.** On the prep crop (y 230) her head top was at 0.21 H, so the two-line cue card (0.13-0.31 H at
0.04 H, the PHOTOGRAPHER phrasing) covered her face and the countdown would sit on her hands at the preview's centre.
The manifest's b5-live crop is now y 30 (same 1.25x, same x): head top about 0.34 H, shoes about 0.94 H, and the HUD
lives in the headroom. Re-run: `node scripts/fetch-footage.mjs --only b5-live`, `node scripts/landmarks.mjs --only
b5-live`, `npm run footage-match`. The app's verdicts are the same as on the first crop: found f336; cards f348 "Ask
them to bend their right elbow more", f413 "Perfect. Hold it", f486 "Ask them to bend their left elbow more" (hidden:
the countdown is on); hold f401; countdown f422 / f452 / f482; capture f512; acceptance PASS; the time remap is
unchanged. Only the guide's solved offset changed (scale 0.9084, offset -0.0119, +0.1454), and the crossings (into
match f401, a one-frame dip at f427, out at f474, above the abort line to the capture). Every other landmark and match
file came out byte-identical; `nearest.json` changed only in its `liveGuide` entry.

**Timeline (all the app's verdicts):**

| Frames | What is on screen |
|---|---|
| f336 | Match cut. She stands left of centre, looking down; the guide stands where she will stand, coloured on the first frame (right arm amber 0.65, head 0.69), the skeleton on her, ring 65. The film camera lands 1.00 -> 1.025 by f342. |
| f348-412 | "Ask them to bend their right elbow more" (the guide's right arm is the amber one); f366-396 she flicks her hair (real time) and the guide's left arm dips amber (57 at f378); she slides right toward the guide and puts her hands in her pockets. |
| f401 | The lock: the score reaches 75 (strictness 0.75), the hold arc starts, the film camera steps 1.5%. Her right arm is 0.73 here; every limb is green from f404 (score peak 91 at f408). |
| f413-421 | "Perfect. Hold it", 9 frames: the stabiliser shows it 400 ms after it tops the list and the countdown removes the banner 700 ms after the lock. That is the app's timing for any body that holds still; it is now opaque for all 9 frames (one-frame swap, no crossfade) and the "3" takes its place on f422. |
| f422-511 | 3 / 2 / 1 in the headroom, each landing from 115% (200 ms) and replacing the last on one frame; the plate is at 0.5x (her real hold, frame repetition); she finishes sliding into the figure (her hip meets the guide's at about f450); score 79 -> 69; the film camera creeps to 1.045 and steps 1.5% on each digit (1.11 at most); f504-511 nothing moves but the footage. |
| f512 | The app's capture flash (85% white, 280 ms decay). Under it the frozen capture frame in full colour, the whole frame (the saved photo is the full scene, so the film camera's zoom lets go), no overlays. |
| f512-554 | The photo pushes 1.00 -> 1.03 about her mid-hip (a sine over twice the span, so it is still moving on the f555 cut). Review 1: 1.00 -> 1.06. |

**Seams exported in `shots.ts` (act IV section):** `GUIDE_FRAME` = the guide's template frame where the app put it
(`rig.ts templateFrameRect`: `SceneMapping.fitFrame` + the solved `OverlayTransform`; it reproduces the app's mapped
joints to 3e-5): x 36.6, y 585.1, 981.1 x 1308.1 px. `VF.origin` (the zoom centre) is the guide's mid-hip x, 0.06 H
above its mid-hip; `HERO_AT_CUT` (f335, 0.975 of the guide about that point): x 49.1, y 598.2, 956.5 x 1275.4.
`PHOTO_FIGURE_AT_CUT` (the guide's frame on the photo on f554, push 1.029 about her mid-hip): x 22.2, y 566.7,
1009.4 x 1345.9; drawn over the f554 render, its shoulders, hips and head sit on hers (her feet are crossed, the
figure's are not). `guideScreenFrame(f)` is the guide on the live view under the film camera.

**Film choices on top of the app (declared):** the HUD is CameraHud.kt's magnified by `hud.k`; its arrangement is
the film's: the card (top 0.13 H, max 0.94 W so the PHOTOGRAPHER strings fit the app's two lines; at 0.84 W "Ask them
to bend their right elbow more" was cut to an ellipsis) and then the countdown (centre 0.185 H, review 1: 0.215 H at 0.68 size, out of the top zone; the app centres it in
the preview) in the headroom, the ring at (0.845 W, 0.58 H) right of her body, below the softbox, which prints near
white. One card and one digit at a time (the app crossfades both; magnified, the overlap reads as a double exposure).
The grid is set to Off (a real setting; the default thirds grid put a line through her head and the countdown).

**Changed from the storyboard, because the real output does otherwise:**
1. The guide is coloured from the cut (f336), not white first: the app finds her on the plate's first frame.
2. At the lock (f401) she is still about 0.09 W left of the guide and her right arm is amber-green (0.73); the guide
   is the app's placement solved at source 4.0 s, where she settles. She slides into it during the hold and the first
   second of the countdown. The strongest still is therefore f420 (the last "Perfect. Hold it" frame: hold arc at
   0.91, every limb green, score 82, 0.03 W off), not f402.
3. "Perfect. Hold it" stays 9 frames (above). The countdown's hidden card (f486) is not drawn, as in the app.
4. The 0.80 default never locks on this footage; 0.75 ("Balanced") is the strictness used (footage prep, above).

**Checked.** Stills of every 15th frame plus each cut and hit, half size: `out/live-f336.png` ... `out/live-f554.png`
(30 files) and full-size f416, f470, f554, looked at. The skeleton and guide stay on her through the push; the card,
digits and ring never touch her face or hands; the countdown's glyph bottom (0.28 H) stays above her hair (0.30 H) at
the largest push and her shoes stay in frame. `look.py` on the act IV clip alone (f336-554, half size, still the v1
sound): G0 PASS (7.32 s), G2 PASS (no run of > 3 flat frames), G3 n/a for a clip (the photo is its last 1.45 s).
Frame-to-frame luma change (96 px wide): B5 1.27, lock to countdown 2.13, countdown 0.74, the 8 f before the capture
0.24, the photo 0.83 (the flash, then a 3% push: look.py calls f513-554 "static-ish"; review 1 made it 6%). The countdown is designed
slow (0.5x footage, the sound carries the build); what moves is her last slide into the figure, the score, the digit
landings and the camera's steps.

**Sheet, one sentence a frame (act IV clip, every 0.47 s):** grey studio, she stands left looking down, the green
see-through figure beside her with an amber arm, ring 65; the card arrives; she flicks her hair, the figure's left arm
turns amber, ring 56; still flicking, 58; hair settling, 69, the figure's right arm amber; hands in pockets, ring 88
with the hold arc; "3" over her head, inside the figure, 77; "3" landed; "2" landing; "2"; "2", 73; "1", 70; "1", 69;
the colour photo under the decaying flash; the colour photo; the colour photo pushing in.

**For the sound stage (WORLD):** the act IV cues moved to the app's real frames: `CUES.found` 336, `cards` [348, 413,
413], `lock` 401, `digits` [422, 452, 482], `still` 504, `capture` 512 (v1: 343 / 355 / 443 / 464 ... / 554).
(Integration ran `npm run audio` on these frames; the hit-2 label now says what the verdicts say: the score reaches
the threshold at f401 and every limb is green from f404.)

**What I would still improve:** the lock happens while she is still sliding (true, but the strongest frame comes
19 frames after the hit); "Perfect. Hold it" at 9 frames reads only with the lock chime and voice line 4 behind it;
the colour photo's 3% push measures as nearly still for its last 1.2 s.

## Acts I-III and V as built (v2, BUILDER WORLD): the hook, real people, the pick, the name

| Beat | Frames | What is on screen |
|---|---|---|
| B1 hook | 0-79 | v1's black live view, stiff stick skeleton and question. The question leaves before any person appears: line 1 f61-67, line 2 f63-69 (accelExit up 0.05 H, 3 f opacity helper); nothing is typed from f70. The push is the v1 drift 1.00 -> 1.03 (f0-70), then the rush f70-79 (gentleIn, peak speed on f79): a zoom about a fixed point solved so the skeleton's nose, lower ankle and mid-hip x land on B2's real body as it is on f80 (`shots.ts` HOOK.cam, `people/data.ts` B2_BODY_AT_CUT: scale 1.03 -> 1.38). The stick lines keep their on-screen width through the rush. |
| B2 the figure gets a body | 80-143 | Plate `b2-alive` in the Studio ramp, landing from 1.06 by f88 (softLand) and creeping to 1.02 (about her mid-hip). On f80 the hook's skeleton stands exactly on her; f80-86 it becomes HER skeleton (a chain blend from the stiff stance to the app's smoothed detection, softLand), then it is the app's live skeleton (useLandmarks: the lite model and the app's LandmarkSmoother) as she lifts both hands behind her head, landing on the f144 downbeat. |
| B3 chorus | 144-271 | Hard cuts on the grid (beats f144-192, eighths f208-232, f240 holds through the drop f248-272). On each cut the lay figure the app's matcher found for that person lands on them: the veil from 1.25x (softLand 5 f on beats, 4 f on eighths) while it settles 0.70 -> 0.34, and the white edge a frame later from 1.32x, closing onto the body (a lock-on). It then stays put while the person moves, as the app's guide does. The film camera pushes 1.00 -> 1.08 across all shots about each person's mid-hip (one curve), then drifts 0.3% in the drop. No names, numbers or colour. |
| B4 the pick | 272-335 | Hard cut on the unison: all 99 slots in "Hands in pockets", unflipped, set down at once (2% H, 6 f); the scale cut's softLand tail; f288 marks and the name (blur, a 0.012 H rise and opacity, 5-8 f), rack focus; f304-335 the accelerating push solved every frame onto LIVE's GUIDE_FRAME (HERO_AT_CUT = GUIDE_FRAME x 0.975): on f335 the hero frame equals HERO_AT_CUT to 0.1 px, and a blend of f335 and f336 shows the hero under act IV's guide. |
| B8 the name | 555-663 | Hard cut from the photo to paper: a solid "Hands in pockets" figure, unflipped, at LIVE's PHOTO_FIGURE_AT_CUT; a 0.4% creep from f555 (review 1: the photo's push carried across the cut, 2% settling by f600); f576-588 one hand leaves its pocket for the hip on twos (stand_hand_on_hip, the icon pose; both unflipped, so no label crosses); then v1's pull back, wordmark and end line, unchanged. |

**The chorus poses (`public/footage/chorus.json`, pose-core `PromoChorusMatch.kt`).** The prep's `nearest.json` scored
one key frame per shot, and three shots had no usable pose there. The new test scores every single-person library pose,
drawn and flipped, on every plate frame (the app's PoseMatcher, back camera, PHOTOGRAPHER, strictness 0.80; the guide
placed with SceneMapping and a solved OverlayTransform as in PromoFootageMatch), and then scores each candidate as a
FIXED guide on every frame the shot shows after it lands ("hold"). The plate may start anywhere in its 15-frame handles,
so the key frame is placed on the landing frame (`offset`, plate frames). Usable means: score >= 0.80 at the key frame,
fit >= 0.8 (the prep's rule), sit <= 0.25 and hold sit <= 0.30, hold mean >= 0.55, never "Hands in pockets". `sit` is
new: the matcher compares limb directions, so a figure can score 0.97 while its torso leans off the person (fun_peace_lean
on c5: the figure's head 0.11 W from hers); sit is the mean distance from each joint the person shows to the drawn
figure's joint, in torso lengths. In half-body shots (no knees in frame) sitting, floor and leaning poses are not offered
(the matcher only sees the upper body and tied floor_sit_on_heels with crossed arms on c4). A slot uses its own clip when
it has a usable pose (c9: the couple clip, not c9-alt2, which repeats c4's person). The nine picks are the assignment
with no pose twice that maximises the summed rank ((score + hold) / 2 - 0.5 (sit + hold sit) / 2); `out/chorus-report.txt`
lists every slot's candidates.

| Shot | Film | Plate | Pose | Score | Fit | Sit | Hold / hold sit | Offset | Key (source) |
|---|---|---|---|---|---|---|---|---|---|
| c1 | f144-160 | c1 | Relaxed arms crossed (flipped) | 0.92 | 1.00 | 0.18 | 0.85 / 0.19 | +11 | 10.97 s |
| c2 | f160-176 | c2 | Cross-legged | 0.81 | 1.00 | 0.17 | 0.57 / 0.22 | +4 | 0.58 s |
| c3 | f176-192 | c3 | Cheek rest selfie | 0.87 | 0.87 | 0.22 | 0.79 / 0.27 | -15 | 3.17 s |
| c4 | f192-208 | c4 | Crossed-arm smile (flipped) | 0.97 | 1.00 | 0.10 | 0.97 / 0.10 | -9 | 0.13 s |
| c5 | f208-216 | c5 | Face frame (flipped) | 0.97 | 1.00 | 0.10 | 0.97 / 0.10 | -2 | 6.46 s |
| c6 | f216-224 | c6 | Blazer pocket | 0.97 | 1.00 | 0.15 | 0.97 / 0.14 | -15 | 5.63 s |
| c7 | f224-232 | c7 | Walk toward the camera (flipped) | 0.99 | 1.00 | 0.17 | 0.99 / 0.18 | -9 | 2.50 s |
| c8 | f232-240 | c8 | Two-hand mirror | 0.98 | 1.00 | 0.15 | 0.98 / 0.15 | -2 | 0.50 s |
| c9 | f240-272 | c9 | Thoughtful chin rest | 0.89 | 1.00 | 0.15 | 0.91 / 0.15 | +14 | 4.42 s |
| c9 (review 1, re-cropped) | f240-272 | c9 | Cuff adjust (flipped) | 0.99 | 1.00 | 0.11 | 0.99 / 0.12 | +15 | 4.46 s |

c2's hold is 0.57: her arms leave the figure's arms after the landing while the cross-legged legs stay on it. c3 and c6
use the whole handle (offset -15).

**Changed from the board, and why.**
1. **The chorus guide is a veil and an edge, not a 62% fill.** White at 62% over grey footage hid the person it was
   fitted to (rendered and looked at: c1 and c3 vanished behind it). The veil settles at 0.34 under a crisp white edge
   (the figure's own outline shape, 2 dp), so the person reads through the figure. No face dot: on a real face it read
   as a mark on the person. The values are `theme.footage.chorusVeil` (moved there from `people/data.ts` at
   integration, replacing the board's `chorusGuideAlpha` and `guideLandAlpha`).
2. **The skeleton morph (f80-86)** is new: the board had her detected skeleton on f80 in place of the stiff one. Six
   frames of blend make the seam the film's sentence (the figure gets a body). Illustrative, like the stiff stance.
3. **c9 is one figure on the couple clip.** The app's model finds only the man (the prep's finding); the fallback list
   offered c9-alt2 (c4's person again, a 2.3x figure she walks out of). The couple clip keeps both people and a new
   face; the figure sits on the man (sit 0.15 through the hold). The chorus therefore ends on one figure. (Review 1:
   that window cut her face in half at the right edge for the whole drop; the clip is now framed on him, her face out
   of frame, and the matcher picks "Cuff adjust", see "Review 1".)
4. **B3 -> B4 is a plain cut on the hit.** The c9 figure is half-body and fills the frame; matching the studio hero to
   it would wreck the lattice framing (the board allows this).
5. **The studio camera's v1 B2 zoom is gone** (the studio is on screen from f272 only); `studio/chorus.ts` is now the
   unison alone. The v1 `CHORUS` table is removed from `beats.ts` and the dead v1 fields from `STUDIO_TIMING`.
6. **The hook's words leave f61-69**, one frame earlier than the board's f62-70, so the last line is gone on f70.

**Events and sound.** `src/events.ts` is rewritten for v2 (no performer, no studio chorus): every hard cut, the morph,
each chorus landing with its pose and score, the drop, the unison, act IV from MATCH_EVENTS. CUES keeps v1's shape, so
`scripts/gen-audio.mjs` runs unchanged: `chorus` is the nine landings (category = the landed pose's category), `ripple`
is empty. Run: integrated -14.0 LUFS, hits unison f272 -10.4 LUFS, lock f401 -15.3, shutter f512 -18.0. (Integration
then moved the final chord to the colour and fixed the labels: "Integration (v2)" above.)

**Checked (half-size draft of the whole film, `out/world-draft.mp4`, LIVE's act IV as it stood at 14:41).**
`look.py --expect 22.13 --expect-audio`: G0 PASS (22.14 s, audio), G2 PASS, G3 PASS (end hold 1.30 s, final shot
3.64 s = 16%). Audio -14.0 LUFS, true peak -1.3 dBFS, 10 of 14 cuts within 80 ms of an onset (the misses are the
eighth-note cuts, whose clicks sit under the kick). Motion: mean luma change 3.2/255 (below the human band; the hook's
silence and act IV's hold are designed). By eye on stills of every 2-5 frames of f0-342 and f550-663 (`out/world/`):
G1 the pose name is 0.045 H; G4 one person with their figure per frame, the army with one framed hero; G5 nothing in
corners, no labels, counters or captions on people.

## Map

| Path | What it is |
|---|---|
| `src/theme.ts` | The one theme object: Studio tokens, signal colours with the app's `forScore` ramp, Archivo roles, the 112.5 BPM grid, measured eases, the two springs, the camera HUD's real dp sizes and its magnification. |
| `src/fonts.ts` | Loads the app's six static Archivo cuts as "Archivo" (400, 500, 600) and "Archivo Expanded" (500, 600, 700). Rendering waits until they load. Also has `fitTextToWidth` (the wordmark is 0.50 W of ink). |
| `src/beats.ts` | The v2 beat and act windows (B5-B7's inner frames are planned; the real ones are `MATCH_EVENTS`), the capture window, `PEOPLE_SHOTS` (which plate is on screen when in B2-B3), the hook's word timings, the two story poses and the real strings. |
| `src/figure/` | `types.ts` is the pose model. `poses.ts` reads the 99 real poses. `shapes.ts` is FigureShapes ported to TS. `Mannequin.tsx` draws the silhouette, outline and stick styles with the app's paint rules. `interpolate.ts` blends skeletons along the kinematic chain and handles keyframes, flipping and mirroring. |
| `src/world/` | `camera.ts` is the studio camera: perspective, the lattice, placing poses, zooming about a point, lens shift. `StudioPaper.tsx` is the per-pixel lit seamless paper. `StudioFigures.tsx` draws figures depth-sorted, with contact shadows and depth blur and haze. `ContactShadow.tsx`, `LiveView.tsx` (black live view, thirds grid, sensor noise) and `Grain.tsx` complete the worlds. |
| `src/ui/` | `Hud.tsx` holds the cue card, score ring, countdown numeral, frame marks and capture flash, rebuilt from `CameraHud.kt`. `UiFragment.tsx` shows a real screenshot magnified by the camera. |
| `src/motion/` | `index.ts` has tween, keys, land, leave, arrive, focusIn, snapTail, drift, stepCounter, onTwos and sway. Every curve is eased and clamped. `WordReveal.tsx` reveals whole words at speech rhythm. `CameraGroup.tsx` puts the film camera on one transform. |
| `src/scenes/` | The film. `Hook.tsx` (B1), `People.tsx` (B2 and the chorus B3, real footage), `StudioShot.tsx` (B4, the pick), `Viewfinder.tsx` (B5-B7: the live view over real footage, the capture and the colour photo), `TheName.tsx` (B8). `shots.ts` solves every seam from the shot on the other side (`HOOK.cam` onto `B2_BODY_AT_CUT`, `GUIDE_FRAME` and `HERO_AT_CUT`, `vfScale`, `PHOTO_FIGURE_AT_CUT`). `timing.ts` holds the hand-set frames. `studio/camera.ts` is the pick's camera (the scale cut, the push solved per frame onto `HERO_AT_CUT`, the depth of field that follows the hero); `studio/chorus.ts` what the 99 slots show (all in "Hands in pockets"). `people/data.ts` is B2's camera and body on the cut and the chorus shots from `chorus.json`. `viewfinder/rig.ts` is `templateFrameRect` (SceneMapping.fitFrame + OverlayTransform); `viewfinder/match.ts` reads the app's verdicts from `public/footage/b5-live.match.json` (`MATCH_EVENTS`, `MATCH_GUIDE`, `partColorsAt`). |
| `src/events.ts` | Every picture event with its frame (the cuts, the chorus landings, the morph, the three hits, every limb green, the colour, the two silences), derived from the sources above, for the sound stage; `CUES` names the same frames for the sound script. |
| `scripts/gen-audio.mjs` | The sound: the SFX kit, the score and the mix, synthesized and placed from `out/events.json` (deterministic). Writes `public/audio/` (gitignored, rebuilt by `npm run audio`). |
| `src/audio/` | `Soundtrack.tsx` plays `public/audio/mix.wav` from frame 0. `voiceover.ts` is the voice-over slot: the frame and window of each line of `vo.txt` (lines 3 and 4 derived from `MATCH_EVENTS`). |
| `src/dev/` | Check compositions (`Dev-*`), rendered as stills; not part of the film. `Dev-FootageSheet` is v2; `Dev-FigureSheet`, `Dev-BlendSheet`, `Dev-StudioDemo`, `Dev-Viewfinder` and `Dev-End` were built for v1 and still show v1 content (Disco point, the v1 chorus, the front-camera viewfinder). |
| `scripts/check-shapes.ts`, `scripts/check-live.ts` | `npm run check`: the TS geometry against Kotlin for every figure; act IV's plate, landmarks and verdicts describe the same frames, for the film's camera and guide, and pass the acceptance list. |
| `../pose-core/src/test/kotlin/com/poseit/app/tools/PromoPoseExport.kt` | Writes `public/poses.json`. |
| `scripts/fetch-footage.mjs` (`npm run footage`) | Downloads every manifest clip to `public/footage/src/<catalogue>.mp4` and builds one plate per shot, `public/footage/<plate>.mp4`: 30 fps CFR, H.264 yuv420p (BT.709 tags), keyframe every 15 f, no audio, the manifest crop scaled to 1080x1920. Plates are in FILM time with a 15-frame handle each side: plate frame n = global frame `filmFrom + n`, showing source frame `floor(S(f) * fps)` (frame repetition, never interpolation; S = in + rate t, or the time-remap keys). Also `b5-live.1x` (the live shot at 1x, to measure the hold) and the alternates' plates (`c9-alt1`, `c9-alt2`, `c1-alt1`). Idempotent (a parameter hash per plate). |
| `public/footage/plates.json` (committed) | Every plate: shot, clip, film window, `filmFrom`, frames, crop and crop scale, source fps, and the source frame shown on each plate frame. |
| `public/footage/src/`, `public/footage/*.mp4` (gitignored) | The downloaded clips and the plates. Never committed. |
| `scripts/landmarks.mjs` (`npm run landmarks`) | `scripts/detect.py` (unchanged: lite model, confidence 0.5, VIDEO mode, un-mirrored; 2 poses for couple plates) on every plate into `out/footage/<plate>.detect.json`, the act III guide frame through the full model (`public/footage/b5-live.guide-photo.json`, the app's "pose from a photo" detection), then the pose-core test below. |
| `../pose-core/src/test/kotlin/com/poseit/app/tools/PromoFootageLandmarks.kt` | The app's live path on the detections: `LandmarkMapping.toScene` (scene 9:16 = the plate) and `LandmarkSmoother` (One Euro per joint, min cutoff 1.2 Hz, beta 0.03; visibility 120 ms; CameraViewModel.onDetection) at the plate's 30 fps timestamps. Writes `public/footage/<plate>.landmarks.json` (committed): per plate frame the 33 raw landmarks and the smoothed 21-joint skeletons the overlay draws (visibility >= 0.5) and the matcher reads. |
| `scripts/footage-match.mjs` (`npm run footage-match`) | Runs the app test below, sets b5-live's `timeRemap.measured` from the app's countdown start on the 1x plate (real time, then 1x -> 0.5x over 10 f, 0.5x to the capture), rebuilds that plate, its landmarks and its match, and prints the acceptance checks and the snaps. |
| `../app/src/test/java/com/poseit/app/promo/PromoFootageMatch.kt` | The app's PoseMatcher + HintStabilizer + CaptureCoach over the smoothed landmarks (back camera: `mirroredView=false`, PHOTOGRAPHER audience; app defaults: 700 ms hold, 3 s countdown, abort margin 0.12). Guides are library templates mapped with `SceneMapping.templateToScene` and an `OverlayTransform` solved like a drag and pinch (mid-hip and torso length; scale 0.4-2.5, offsets +-0.9). Writes `public/footage/nearest.json` (every snap: all templates of the right person count, drawn and flipped, ranked; the snap-rule pick; the best frame in the in-point window; a no-repeat `distinct` pick) and `<plate>.match.json` (v1's match.json records plus the guide in scene space, the events and, for b5-live, every acceptance attempt). `b5-live.photo-guide.match.json` is the comparison run with a guide from the app's pose-from-a-photo path (`CustomPoseFactory.fromDetection`). |
| `src/footage/` | `plates.ts` (the plates and `plateFrameFor`), `transform.ts` (the one plate -> composition transform: a push about a scene anchor, `cssMatrix`, `sceneToComp`, `pushAt`), `grade.tsx` (the Studio ramp `RampFilter`, `rampCurve`), `data.ts` (file types, `peopleAt`, `drawable`, `matchRecordAt`, `guideFigures`, `partColors`), `hooks.ts` (`useLandmarks`, `useFootageMatch`, `useFootageJson`: fetched from `public/` with delayRender, not bundled), `Footage.tsx` (a plate on the film's clock, graded, with children drawn in plate pixels through the same transform), `index.ts`. `manifest.ts` now reads `timeRemap.measured` first (`remapKeys`). |
| `src/dev/FootageSheet.tsx` (`Dev-FootageSheet`) | One page per plate: four frames (first, guide or middle, fastest move, last with a 1.35x push) with the smoothed skeleton, the raw landmarks as red dots and the guide in the app's paint. |
| `footage/catalogue.md`, `catalogue.json` | The scout's checked catalogue of 56 stock clips: licences, detection runs, holds, verdicts. |
| `footage/manifest.json` | The clips and shots the v2 film uses (source, page, file URL, licence, in/out, rate or remap, 9:16 crop, grade, pose plan, alternates). |
| `scripts/detect.py` | Runs the app's own pose model (lite by default) on a video with the app's live settings; per-frame landmarks and a summary. Needs `promo/.venv` (MediaPipe) and the system `libegl1` and `libgles2`. |
| `src/footage/manifest.ts` | Typed reader of the manifest: clips, shots, source time and plate frame for a film frame. |
| `CREDITS.md` | Every clip with its page and licence; the people are stock models who endorse nothing. |
| `src/scenes/People.tsx` | Act II: B2 (the plate, the skeleton morph, her live skeleton) and the chorus (plates, offsets, the landing guides, the push). |
| `src/scenes/people/data.ts` | B2's camera and her body on the cut (B2_BODY_AT_CUT, from `b2-alive.landmarks.json`), CHORUS_SHOTS from `chorus.json`, the chorus push, the landing, CHORUS_VEIL. |
| `../pose-core/src/test/kotlin/com/poseit/app/tools/PromoChorusMatch.kt` | The chorus poses: `./gradlew :pose-core:test --tests 'com.poseit.app.tools.PromoChorusMatch'` -> `public/footage/chorus.json` (committed) and `out/chorus-report.txt`. |

## v1 record (superseded)

The sections below describe v1 as it was built and reviewed (all drawn figures: the studio chorus, Disco point, a
keyframed body on the front camera). v2 replaced those beats; what still holds from here is the method and the
kit: the eases, the studio camera and paper findings, the HUD's sizes and motion, the sound's timbres and mix rules.

### v1: status

Every beat of the storyboard is built in `src/scenes/` and assembled in `Promo` (664 f, 22.13 s, 1080 x 1920). The act
III numbers, colours, cards, hold and countdown are the app's own verdicts: the app's PoseMatcher, HintStabilizer and
CaptureCoach run frame by frame over the keyframed body in a JVM test. The picture events are exported for the
sound stage in `src/events.ts`. The sound is synthesized from those events by `scripts/gen-audio.mjs` (the SFX kit, the
score and the mix, -14 LUFS), and the composition plays it (`src/audio/Soundtrack.tsx`). The voice-over (`vo.txt`) has a
slot (`src/audio/voiceover.ts`) and waits for an ElevenLabs key; see `README.md`, "Voice-over".

### v1: The film as built

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

#### Changed from the storyboard, and why

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

### v1: Review (look.py on the half-scale draft)

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

### v1: Sound (as built)

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

### v1: Choices and findings for the scene builder

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
- **Stock footage:** Mixkit clips under the Mixkit Stock Video Free License, each checked on its own page and listed
  in `CREDITS.md`; nothing from Coverr (its licence bars evaluating AI models). The clips and plates are gitignored
  and never committed; only the manifest, the scripts, the credits and the derived landmark and verdict data are.
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
