# Paused: promo film v3 (clarity cut)

`v3-paused.patch` holds the unfinished v3 build (captions, phone device, app screens in the phone, the new opening
and end), paused on purpose while the app's UI is redesigned. The v3 storyboard is committed in `../STORYBOARD.md`.
The phone scenes show the app's screens, so they need re-rendering after the redesign anyway.

To resume: `git apply promo/wip/v3-paused.patch` from the repo root, then `cd promo && npm run check`.
