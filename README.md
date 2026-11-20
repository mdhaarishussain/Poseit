# Poseit

A free, open-source Android pose-guide camera (in the spirit of Poseful / SuperPose / LensBuddy).

Work in progress – see below for build instructions.

## Build

Requirements: JDK 17+, Android SDK (platform 36). Then:

```
./gradlew :app:assembleDebug
```

The MediaPipe pose models are downloaded automatically on first build into `app/src/main/assets/models/`.
