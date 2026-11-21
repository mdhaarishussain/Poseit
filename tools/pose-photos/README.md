# pose-photos: real sample photos for the 99 library poses

Finds photos of adult stock models demonstrating each library pose, from sources whose terms allow use inside a free
app (Mixkit Free License videos, Wikimedia Commons CC0 / PD / CC BY / CC BY-SA), and ranks them with the app's own
matcher. Licences, rules and open questions: [docs/POSE_PHOTOS.md](../../docs/POSE_PHOTOS.md).

Everything downloaded or generated goes to a work folder outside git, `/home/user/poseit-photos-work` (override with
`POSE_PHOTOS_WORK`). The pose models are read from `app/src/main/assets/models/` (or the main checkout's, or
`POSE_MODELS`). Python: the promo venv (`promo/.venv`: mediapipe, opencv-python-headless, numpy, Pillow).

```sh
PY=promo/.venv/bin/python
$PY tools/pose-photos/mixkit_harvest.py crawl            # grid pages of 56 people tags -> items.json
$PY tools/pose-photos/mixkit_harvest.py scan --max-clips 420   # licence check, 360p preview, lite model, frames
$PY tools/pose-photos/commons_harvest.py search          # per-pose phrases + broad phrases -> files.json (slow: rate limits)
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

`posematch.py` reproduces the Kotlin matcher: on the promo film's nine chorus shots (`promo/public/footage/chorus.json`,
scores written by pose-core `PromoChorusMatch`) it gives the same score to 4 decimals (largest difference 0.0001) and
the same scene-space guide joints (largest difference 0.0001). Re-check after any change to pose-core `match/`.

## Outputs (work folder)

- `mixkit/items.json` every crawled card; `mixkit/clips/<id>.json` licence evidence and kept frames per scanned clip;
  `mixkit/frames/<id>_<ms>.jpg` the frames (360p previews; the release frame must come from the licensed download).
- `commons/files.json` every file found, with licence metadata and the rules verdict; `commons/img/<pageid>.jpg`.
- `landmarks/mixkit.jsonl`, `landmarks/commons.jsonl` MediaPipe landmarks per image.
- `candidates/<poseId>.json` the pose and its top 40 candidates (score, rank, fit, people, mirror flag, crop box,
  transform, source, page and file URLs, author, licence, attribution, flags); `candidates/index.json` per pose: best
  score and the number of eligible candidates over 0.6 / 0.7 / 0.8.
- `sheets/<poseId>.jpg`.
