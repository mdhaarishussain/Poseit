#!/usr/bin/env bash
# Copies the app's real screenshots into public/screens/ (gitignored) for UiFragment.
# Full-resolution captures come from the app's screenshot tests (app/build/screenshots, 1200 x 2580);
# the committed README screens (docs/images/screens, 560 px) are the fallback.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p public/screens
src=../app/build/screenshots
if [ -d "$src" ]; then
  for f in library detail camera-hud onboarding editor_from_pose settings review-camera-timer-running; do
    [ -f "$src/$f.png" ] && cp "$src/$f.png" public/screens/
  done
fi
[ -f ../app/build/repo-assets/screen-3-camera.png ] && cp ../app/build/repo-assets/screen-3-camera.png public/screens/camera-matched.png
cp ../docs/images/screens/*.png public/screens/ 2>/dev/null || true
ls public/screens
