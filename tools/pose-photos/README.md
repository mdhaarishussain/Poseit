# pose-photos: real sample photos for the 99 library poses

Finds photos of adult stock models demonstrating each library pose, from sources whose terms allow use inside a free
app (Mixkit Free License videos, Wikimedia Commons CC0 / PD / CC BY / CC BY-SA), and ranks them with the app's own
matcher. Licences, rules and open questions: [docs/POSE_PHOTOS.md](../../docs/POSE_PHOTOS.md).

Shipped now: **34 of 99 poses have a photo**, 27 Mixkit stills from 27 clips and 7 Wikimedia Commons photos (5 CC0, 2
CC BY 2.0); 65 have none. 14 photos were dropped along the way (2 at the confirm stage, 10 at the review, 1 at the
second look, 1 at the photo stage review); each keeps its reason in `credits.py`'s `DROPPED`. Release builds leave
the Mixkit stills out until the owner decides (docs/POSE_PHOTOS.md, "Mixkit in release builds").

Everything downloaded or generated goes to a work folder outside git, `/home/user/poseit-photos-work` (override with
`POSE_PHOTOS_WORK`). The pose models are read from `app/src/main/assets/models/` (or the main checkout's, or
`POSE_MODELS`). Python: the promo venv (`promo/.venv`: mediapipe, opencv-python-headless, numpy, Pillow).

```sh
PY=promo/.venv/bin/python
$PY tools/pose-photos/mixkit_harvest.py crawl            # grid pages of 56 people tags -> items.json
$PY tools/pose-photos/mixkit_harvest.py scan --max-clips 420   # licence check, 360p preview, lite model, frames
$PY tools/pose-photos/mixkit_harvest.py scan --max-clips 510 --min-relevance 1 --focus "couple|friends|group|sitting|lying|kneeling|..."
$PY tools/pose-photos/commons_harvest.py search          # per-pose phrases + broad phrases -> files.json (slow: rate limits)
$PY tools/pose-photos/commons_harvest.py search --cached-only   # re-apply the rules to saved results, no requests
$PY tools/pose-photos/commons_harvest.py download        # ~1200 px renditions of the files that pass
$PY tools/pose-photos/detect_images.py                   # full model, up to 4 people -> landmarks/*.jsonl
$PY tools/pose-photos/score.py                           # every image x every pose -> candidates/<poseId>.json, index.json
$PY tools/pose-photos/sheets.py                          # sheets/<poseId>.jpg: the figure + top 8 candidates
```

Every step resumes: pages, clips, files and images already done are skipped.

| File | What it does |
| --- | --- |
| `common.py` | paths, poses.json loading, MediaPipe 33 -> app 21 joints (as `LandmarkMapping.toScene`), a serial HTTP client with per-host gaps and Retry-After |
| `rules.py` | licence allow-list for Commons; people rules on text (minors, sensitive contexts, public figures and events, non-photos, watermarks) |
| `mixkit_harvest.py` | Mixkit crawl and scan (Free License only, checked on each clip page) |
| `commons_harvest.py`, `commons_queries.py` | Commons search per pose and per category, metadata and licence per file, downloads |
| `detect_images.py` | the app's full pose model on every candidate frame and photo (IMAGE mode, up to 4 people) |
| `posematch.py` | Python port of pose-core `PoseMatcher` (segments, weights, coverage, critical joints, framing, assignment) and `SceneMapping.templateToScene` |
| `score.py` | ranks every candidate against every pose (pose as drawn and flipped), people count, framing fit |
| `sheets.py` | contact sheets per pose |
| `picks/gaps-r1_search.py` | gap filling, round 1: whole Commons categories (Files from Pexels, Hands in pockets, People leaning, ...) and per-pose searches in the Unsplash/Pixabay CC0 imports; more pages of pose-related Mixkit tags and searches; title-picked clips scanned (licence check, 360p preview, full model, scored against the gap poses, 1-fps strips); outputs in `WORK/gaps-r1/` |
| `picks/gaps-r1_finalize.py` | round-1 picks -> `app/src/main/assets/pose_photos/`, `landmarks/`, `picks/gaps-r1.json` (credits, or why a pose stays empty) |
| `picks/gaps-r2_search.py` | gap filling, round 2: Commons titles via `list=search` (500 a request: the API allows about one request a minute here) over the reviewed Unsplash, Pixabay, PxHere and rawpixel CC0 imports, ~30 pose categories (Men/Women with crossed arms, ... sitting on stairs, ... crouching, kneeling, jumping, Chin on hand, People sitting on desks/armchairs/stools/chairs, ...) and pose phrases; 500 px thumbnails from thumb.wikimedia.org (about one a second) for titles that pass the text rules; the app's full model and matcher on all of them; licence metadata only for the best per pose; more Mixkit tags and searches, title-picked clips scanned with round 1's scanner; outputs in `WORK/gaps-r2/` |
| `picks/gaps-r2_finalize.py` | round-2 picks (round 1's build, round 2's records) -> photos, landmarks, `picks/gaps-r2.json`; the attribution says what was changed, that a BY-SA crop keeps the licence, and adds an author's requested link |
| `picks/review_rescore.py` | review of the shipped set (2026-11-22): ranks every frame and photo already detected by the earlier rounds (first run, gap rounds 1 and 2, the standing-leaning scan; nothing is downloaded) against chosen poses -> `WORK/review/candidates/`, `WORK/review/sheets/`; Commons thumbnails whose licence was never read are marked "licence unchecked" |
| `picks/review_finalize.py` | the review's replacement photos (round 1's build, records in `picks/review.json`); the photos the review removed are in `credits.py`'s `DROPPED` |
| `picks/*.json` | the selection records per round (photo and credit, or why a pose has none) |
| pose-core `PosePhotoMatch` (test) | the app's own Kotlin PoseMatcher on every shipped photo's landmarks: score against its own pose as drawn and flipped, part scores, fit, tcov, and the nearest library pose by rank -> `app-match.json` |
| `credits.py` | merges the selection records (later rounds win; `ORDER` ends with the review's `review`) and `app-match.json` into `app/src/main/assets/pose_photos/credits.json` (only the fields the app reads), `credits-full.json` (everything) and `coverage.json`; holds the confirm stage's decisions (`DROPPED`, `NOTES`); `--table` prints the docs' coverage table; refuses photos without a credit, credits without a photo, and licences without a shipped text |
| `allpicks.py` | review sheets: every shipped photo beside its pose figure, name, source, licence, app score and nearest pose -> `WORK/all-picks.jpg` (one grid) and `WORK/all-picks-<n>.jpg` (16 a sheet) |

`posematch.py` reproduces the Kotlin matcher: on the promo film's nine chorus shots (`promo/public/footage/chorus.json`,
scores written by pose-core `PromoChorusMatch`) it gives the same score to 4 decimals (largest difference 0.0001) and
the same scene-space guide joints (largest difference 0.0001). Re-check after any change to pose-core `match/`.

## Confirm and credits (after any change to the shipped photos)

```sh
export ANDROID_HOME=/opt/android-sdk
./gradlew --console=plain -q :pose-core:test --tests 'com.poseit.app.tools.PosePhotoMatch' --rerun   # -> app-match.json
python3 tools/pose-photos/credits.py                    # -> credits.json, credits-full.json, coverage.json
$PY tools/pose-photos/allpicks.py                       # -> WORK/all-picks*.jpg
```

## Outputs in git

- `app/src/main/assets/pose_photos/<poseId>.webp` the shipped photos; `credits.json` their credits (keyed by pose id,
  only what the app reads; it ships in the APK).
- `credits-full.json` the same credits with the download links, app scores, review notes and source records.
- `app/src/main/assets/licenses/` the licence texts and `index.json`.
- `landmarks/<poseId>.json` the app's full pose model on each shipped photo (IMAGE mode, up to 4 people, MediaPipe's
  33 landmarks as `[x, y, z, visibility, presence]`, normalised to the photo).
- `app-match.json` (PosePhotoMatch), `coverage.json` (credits.py): all 99 poses, photo or not, scores, verdicts.

## Outputs (work folder)

- `mixkit/items.json` every crawled card; `mixkit/clips/<id>.json` licence evidence and kept frames per scanned clip;
  `mixkit/frames/<id>_<ms>.jpg` the frames (360p previews; the release frame must come from the licensed download).
- `commons/files.json` every file found, with licence metadata and the rules verdict; `commons/img/<pageid>.jpg`.
- `landmarks/mixkit.jsonl`, `landmarks/commons.jsonl` MediaPipe landmarks per image.
- `candidates/<poseId>.json` the pose and its top 40 candidates (score, rank, fit, people, mirror flag, crop box,
  transform, source, page and file URLs, author, licence, attribution, flags); `candidates/index.json` per pose: best
  score and the number of eligible candidates over 0.6 / 0.7 / 0.8.
- `sheets/<poseId>.jpg`.
