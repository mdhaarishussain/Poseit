# Poseit architecture

Poseit is a free, offline Android pose-guide camera. You pick a pose (built-in or made from your own photo).
The camera overlays it on the live preview, detects your body with MediaPipe, scores how well you match it,
coaches you by voice and on screen, and can shoot hands-free.

## Modules

| Module | What | Android? |
|---|---|---|
| `:pose-core` | Pose model, angle DSL, built-in pose library, scene mapping, matching and coaching logic, JSON | No (pure Kotlin/JVM, unit-tested, renderable with Java2D) |
| `:app` | Compose UI, CameraX, MediaPipe, TTS, persistence | Yes |

### pose-core packages (`com.poseit.app.pose`)
- `Geometry.kt`, `Joint.kt` (21 MediaPipe-compatible landmarks, `Bone`, `BodyPart`), `Skeleton.kt` (+ `BodyProportions`), `PoseTemplate.kt` (template + category/tag/camera-advice enums)
- `dsl/PoseDsl.kt`: forward-kinematics authoring DSL. Read its header for the **angle convention**.
- `render/FigureShapes.kt`: platform-neutral mannequin geometry (capsules, circles, polygon) used by both renderers
- `library/*Poses.kt`: the built-in poses, one file per category; `BuiltInPoses.all`
- `scene/SceneMapping.kt`: template frame to scene mapping + user overlay transform
- `detect/LandmarkMapping.kt`: MediaPipe 33 landmarks to `Skeleton` in scene space
- `match/PoseMatcher.kt`: scoring, per-part scores, multi-person assignment, coaching hints (pose, framing, visibility)
- `match/LightingAnalyzer.kt`: face / backlight / dark-scene check from a coarse luma grid
- `coach/CaptureCoach.kt`: auto-capture, self-timer and interval-photoshoot state machine (pure, clock-driven)
- `io/PoseJson.kt`: custom-pose persistence; `CustomPoseFactory` builds templates from detections
- tests: `pose-core/src/test/...`; `tools/PosePreviewRenderer` draws templates to PNG (`pose-core/build/pose-previews/`)

### app packages (`com.poseit.app`)
- `PoseitApp` / `AppContainer` (manual DI; `context.appContainer`)
- `data/`: `SettingsRepository` (DataStore → `AppSettings`), `PoseRepository` (built-in + custom JSON + favourites/recents), `PhotoRepository` (MediaStore `Pictures/Poseit`)
- `camera/`: `PoseCamera` (CameraX Preview + ImageCapture + ImageAnalysis in one `UseCaseGroup`/`ViewPort`), `LevelSensor`
- `detect/PoseDetector`: MediaPipe Pose Landmarker (live stream + still image)
- `voice/VoiceCoach`: TextToSpeech + beeps + haptics
- `input/HardwareKeys`: volume-button shutter
- `ui/components/PoseFigure.kt`: **shared** Compose mannequin renderer (`drawPoseFigure`, `PoseThumbnail`, `fitFrameRect`)
- `ui/components/Studio*.kt`, `PoseTile`, `FrameMarks`, `EmptyState`, `Sections`: the shared Studio components (buttons, tabs, chips, rows, switch/slider, top bars, pose tile, viewfinder frame marks, empty state). `screens/ComponentGalleryScreenshotTest` renders them all to `app/build/screenshots/components.png`
- `ui/theme`: the Studio design system in code (`docs/DESIGN.md`): `Color.kt` (`Studio` tokens), `Type.kt` (Archivo, `StudioType`), `Shape.kt` (`StudioShapes`, `Space`), `Theme.kt` (`PoseitTheme`)
- `ui/nav/PoseitNavHost` (routes), and one package per screen: `library`, `detail`, `camera`, `gallery`, `create`, `editor`, `settings`, `onboarding`

## Coordinate spaces (important)
- **Frame space**: a template's normalized frame (0..1, aspect `template.frameAspect`, default 3:4). Shows what the final photo should look like: photo orientation, anatomical left/right.
- **Scene space**: normalized coordinates over the visible preview, which is also the saved photo (CameraX ViewPort, FILL_CENTER). 0..1, y down, aspect = `SceneAspect.ratio`. Never mirrored. Matching, framing and hints all work in scene space.
- **View space**: screen pixels. The front-camera preview is mirrored, so drawing must flip scene x → 1 - x (`drawPoseFigure(flipX = true)`, `Skeleton.flippedForDisplay()`).
- x and y are normalized separately. **Always multiply x by the aspect** (`Skeleton.toIsotropic`) before measuring angles or distances.
- MediaPipe gets upright, non-mirrored frames, so its LEFT/RIGHT labels are the subject's real left and right. Hints say "your left arm" in that anatomical sense.
- Limb-angle convention (DSL + matcher, `Vec2.fromLimbAngle` / `limbAngle`): 0° = down, +90° = image right, -90° = image left, ±180° = up.

## Camera pipeline
```
CameraX ImageAnalysis (RGBA, KEEP_ONLY_LATEST, viewport-cropped)
  -> upright non-mirrored Bitmap -> PoseDetector.detectLive (MediaPipe LIVE_STREAM, async)
  -> DetectionResult(people in scene space)
  -> PoseMatcher.match(SceneMapping.templateToScene(template, aspect, overlayTransform, flip), people)
  -> MatchResult (score, partScores -> overlay colours, hints -> banner + VoiceCoach)
  -> CaptureCoach.update(score, now) -> Countdown / Capture -> PoseCamera.takePhoto() (full-res ImageCapture)
```

## Feature map (what the apps we are matching do)
- Poseful: pose library, see-through reference overlay (opacity, drag, pinch, flip), import your own photo, grid + level, favourites, offline.
- SOVS2: outline figures for 1-4 people, background-match mode (translucent snapshot of the desired framing), language-free directing of a helper.
- LensBuddy: hands-free self-timer, interval photoshoot, spoken countdown, review the shots.
- Superpose: coaching for both subject and photographer, outline modes, live lighting and framing checks.
- Poseit extras: live MediaPipe pose matching with a score, per-limb colouring, spoken corrections, auto-capture when matched, hand-raise trigger, pose editor.

## Conventions
- Kotlin, Jetpack Compose + Material 3 with the light "Studio" design system (`docs/DESIGN.md`, `PoseitTheme`, `Studio` tokens; the camera alone is dark), no DI framework, no network needed at runtime (MediaPipe's built-in usage logging aside: its tasks-core dependency adds INTERNET and sends anonymous usage stats when online).
- All pose art is original and generated from the DSL. Never copy assets from other apps.
- Build: `./gradlew :app:assembleDebug` · engine tests: `./gradlew :pose-core:test`.
