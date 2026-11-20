# Poseit 📸🧍

**A free, open-source, offline pose-guide camera for Android.**
Don't know how to pose? Pick a pose, line yourself up with the on-screen figure, and Poseit tells you what to fix.
It shoots automatically when you've got it right.

Poseit combines the best ideas of the paid pose apps, with no subscription, no ads, no account and no internet:

| Idea | From | In Poseit |
|---|---|---|
| Pose library + see-through overlay you can move, resize, rotate, flip | Poseful | ✅ 90+ original poses, drag / pinch / rotate / mirror, opacity |
| Use **your own photo** as the reference | Poseful | ✅ "Create from photo": traces the pose and makes a ghost / outline overlay |
| Outline figures for 1–4 people, background-match mode | SOVS2 | ✅ couple & group poses, "Lock scene" background match |
| Hands-free self-timer, interval photoshoot, spoken countdown | Lens Buddy | ✅ 3/5/10 s timer, photoshoot (N shots every X s), volume-key shutter |
| Coaching for subject *and* photographer, lighting & framing checks | Superpose | ✅ spoken + on-screen hints, lighting warnings, framing advice |
| **Live pose matching** | – | ✅ MediaPipe detects your body, scores the match, colours each limb red→green |
| **Auto-capture** | – | ✅ takes the photo when you hold the pose (or raise a hand to start the timer) |
| Pose editor | – | ✅ drag the mannequin's joints to design your own poses |

## Install

Download `app-release.apk` (or `app-debug.apk`) from the latest
[GitHub Actions run](../../actions/workflows/android.yml) → *Artifacts → poseit-apks*, copy it to your phone and
open it (allow "install unknown apps" when asked). Android 7.0+ required; best on a recent phone.

## Build it yourself

Requirements: JDK 17+ and the Android SDK (platform 36). Android Studio works too: just open the folder.

```bash
./gradlew :app:assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease        # minified, signed with the debug key for easy sideloading
./gradlew :pose-core:test             # engine unit tests (+ renders pose preview sheets)
```

The MediaPipe models (pose landmarker lite/full + DeepLab v3, all Apache-2.0) are downloaded automatically into
`app/src/main/assets/models/` on the first build.

## How it works

```
CameraX frame ─▶ MediaPipe Pose Landmarker (33 landmarks, on-device)
              ─▶ One-Euro smoothing ─▶ PoseMatcher (limb-angle comparison vs. the template)
              ─▶ score + per-limb colours + hints ("Raise your left arm", "Step back")
              ─▶ voice coach + auto-capture state machine ─▶ full-resolution photo in Pictures/Poseit
```

* `pose-core/`: pure Kotlin engine (pose model, the angle-based authoring DSL, built-in library, matching,
  coaching, JSON). Fully unit-tested on the JVM.
* `app/`: Jetpack Compose UI, CameraX, MediaPipe, TextToSpeech.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the details: coordinate spaces, mirroring and the contracts
between modules.

## Credits & licenses

* Pose detection & segmentation: [MediaPipe](https://github.com/google-ai-edge/mediapipe) (Apache-2.0)
* AndroidX / Jetpack Compose / CameraX, Kotlin coroutines & serialization, Coil (Apache-2.0)
* All pose figures are original. They are generated from joint angles by Poseit's own DSL, and no images were
  copied from other apps.
