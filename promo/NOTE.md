# Poseit promo: build notes

The plan is in `STORYBOARD.md`, the sound in `sound-plan.md` and the voice-over script in `vo.txt`. This file says
what is built, what is real and what is illustrative, and records what was checked. It grows with the film.

## Status: foundation

The Remotion project, the real pose data, the mannequin renderer, both worlds, the camera HUD parts and the motion
helpers are in place. `Promo` is still a scaffold: each act shows its world, and B1 shows the hook over the stiff
skeleton. The scenes come next.

## Run it

```bash
cd promo
npm install
npm run poses        # re-export public/poses.json from pose-core (only after the pose library changes)
npm run assets       # copy the app's screenshots into public/screens/ (gitignored, used by UiFragment)
npm run check        # typecheck + parity of the TS mannequin geometry against pose-core's FigureShapes
npm run studio       # Remotion Studio
npx remotion still src/index.ts Promo out/f.png --frame 40
npm run draft        # half-size draft to out/draft.mp4
python3 <motionmaxxing>/scripts/look.py out/draft.mp4 --out look --expect 22.13
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
- **Rebuilt (declare in the film's notes):** the camera HUD. It is drawn from the app's tokens and code, not
  screen-captured, and magnified by `theme.hud.k` = 1.95, so cue text is 0.04 H.
- **Illustrative:**
  - The stiff stance and its wrist flick (`promo_stiff`, `promo_stiff_flick`). They are built with the app's pose
    DSL but are not library poses.
  - The studio, its 11 x 9 lattice and its light.
  - The person's skeleton keyframes, once built.

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
- **Cue card size:** at the app's proportions, a two-line card at 0.04 H text is about 320 px tall (y 0.135 to
  0.30 H). It overlaps the ring at the storyboard's (0.83 W, 0.33 H). Either lower the ring to about 0.36 H or start
  the card higher. This needs deciding before B5.
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
- **Audio:** no ElevenLabs key in this session. There is no voice and no generated music; the VO script waits in
  `vo.txt`.
