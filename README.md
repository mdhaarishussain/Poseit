# Poseit 📸🧍

**A free, open-source, offline pose-guide camera for Android.**
Don't know how to pose? Pick a pose, line yourself up with the on-screen figure, and Poseit tells you what to fix.
It shoots automatically when you've got it right.

Poseit combines the best ideas of the paid pose apps, with no subscription, no ads and no account. It works fully
offline, and your camera feed and photos never leave your phone (the bundled MediaPipe library may send anonymous
usage statistics to Google when you're online):

| Idea | From | In Poseit |
|---|---|---|
| Pose library + see-through overlay you can move, resize, rotate, flip | Poseful | ✅ 99 original poses in 11 categories, drag / pinch / rotate / mirror, opacity, grid, level |
| Use **your own photo** as the reference | Poseful | ✅ "Create from photo": traces the pose and makes a ghost / outline overlay |
| Outline figures for 1–4 people, background-match mode | SOVS2 | ✅ couple & group poses, "Lock scene" background match |
| Hands-free self-timer, interval photoshoot, spoken countdown | Lens Buddy | ✅ 3/5/10 s timer, photoshoot (N shots every X s), volume-key shutter |
| Coaching for subject *and* photographer, lighting & framing checks | Superpose | ✅ spoken + on-screen hints, lighting warnings, framing advice |
| **Live pose matching** | – | ✅ MediaPipe detects your body, scores the match, colours each limb red→green |
| **Auto-capture** | – | ✅ takes the photo when you hold the pose (or raise a hand to start the timer) |
| Pose editor | – | ✅ drag the mannequin's joints to design your own poses |

## Install

Download the latest `Poseit-vX.Y.Z.apk` from [Releases](../../releases) on your Android phone and open it (allow
"install unknown apps" when asked). Android 7.0+ required; best on a recent phone. New versions install over old
ones: every build is signed with the same public sideload key (`app/signing/`). Builds from any branch are also
available as artifacts of the [Build APK workflow](../../actions/workflows/android.yml).

To publish a release, push a version tag: `git tag v1.2.0 && git push origin v1.2.0`. The
[Release workflow](.github/workflows/release.yml) builds the APK and attaches it to a GitHub Release, using
`docs/releases/<tag>.md` as the notes. To sign with your own key instead of the public one, add the
`POSEIT_KEYSTORE_BASE64`, `POSEIT_KEYSTORE_PASSWORD`, `POSEIT_KEY_ALIAS` and `POSEIT_KEY_PASSWORD` repository
secrets.

## Build it yourself

Requirements: JDK 17+ and the Android SDK (platform 36). Android Studio works too: just open the folder.

```bash
./gradlew :app:assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease        # minified, signed with the public sideload key
./gradlew :pose-core:test             # engine unit tests (+ renders pose preview sheets)
./gradlew :app:testDebugUnitTest      # app tests: Robolectric UI flows + screenshots in app/build/screenshots/
```

The MediaPipe models (pose landmarker lite/full + DeepLab v3, all Apache-2.0) are downloaded automatically into
`app/src/main/assets/models/` on the first build.

## Status

Version 1.1.0. It has been tested on the JVM but **not yet run on a real phone**:

* 273 automated tests pass:
  * engine tests, including a check that every built-in pose can actually be matched;
  * Robolectric UI-flow tests on Android 7.0, 9 and 15 (onboarding, library, detail, editor, settings, gallery,
    create-from-photo, camera permission and controls);
  * screenshot tests, including small phones and large fonts.
* Three independent review rounds fixed 50 issues.
* Things a JVM can't exercise still need a device check: live CameraX + MediaPipe detection, voice (TextToSpeech),
  saving to the gallery, and how well real detections score.

Known limitations:

* Lying-down and landscape (couple/group) poses are shown smaller, because the camera is portrait-only.
* Arms that go around a partner are drawn across the partner's figure. The figures have no depth or props.
* "Export my poses" saves the poses but not the photos behind photo-based poses.
* Builds are signed with a public sideload key, for easy updates. Add your own key (see Install) before
  distributing the app to other people.

Ideas for later:

* Remember the lens, aspect ratio and timer between camera visits.
* A review screen limited to the shots from the last photoshoot.
* Switch to the front camera automatically for selfie poses.
* Include reference photos in exports.

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
between modules. The visual design ("Studio": white lay figures on studio-grey paper, Archivo type, colour only for
match feedback) is specified in [docs/DESIGN.md](docs/DESIGN.md).

## Credits & licenses

* Pose detection & segmentation: [MediaPipe](https://github.com/google-ai-edge/mediapipe) (Apache-2.0)
* AndroidX / Jetpack Compose / CameraX, Kotlin coroutines & serialization, Coil (Apache-2.0)
* Typeface: [Archivo](https://github.com/Omnibus-Type/Archivo) by Omnibus-Type (SIL Open Font License 1.1, bundled)
* All pose figures are original. They are generated from joint angles by Poseit's own DSL, and no images were
  copied from other apps.
