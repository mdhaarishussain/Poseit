#!/usr/bin/env python3
"""
detect.py: run the app's own pose model over a video and write every frame's landmarks, plus a summary of where a full
body holds a clear pose and where it moves between poses.

The model and settings follow the app's live analyzer (app/src/main/java/com/poseit/app/detect/PoseDetector.kt and
CameraViewModel.kt): MediaPipe Pose Landmarker, the "lite" model by default (Settings > accurate model off, the
default), min detection / presence / tracking confidence 0.5, frames fed upright and NOT mirrored so LEFT/RIGHT stay
anatomical. The app runs LIVE_STREAM on camera frames; a file has no live clock, so this uses VIDEO mode, which keeps
the same tracking between frames, with each frame's own timestamp.

    promo/.venv/bin/python promo/scripts/detect.py IN.mp4 -o OUT.json [--summary SUM.json]
        [--model lite|full] [--num-poses N] [--crop x,y,w,h] [--every K] [--max-side PX]

  -o           per-frame JSON (see "Output" below)
  --summary    also write the summary (detection rate, full-body visibility, people, motion, holds, transitions);
               printed to stdout either way
  --model      lite (app default) or full (the app's "accurate model" setting); or a path to a .task file
  --num-poses  people to look for (the app uses the guide's person count, 1-6); default 1. Use 3-4 to count people.
  --crop       analyse only this part of the frame, normalized to the source (e.g. a 9:16 window of a landscape clip:
               --crop 0.34,0,0.32,1). Landmarks are then normalized to the crop, as the app's portrait camera would see.
  --every      analyse every K-th frame (default 1)
  --max-side   downscale frames so the longest side is at most PX before detection (default 1920, as the app's still
               path; MediaPipe resizes to its 256 px input anyway, so this only saves decode time)

Output (-o):
  {
    "source": "IN.mp4", "width": W, "height": H, "fps": 23.976, "frames": N, "duration": s,
    "crop": [x, y, w, h] | null, "image_width": w px, "image_height": h px (the analysed image, i.e. the crop),
    "model": "pose_landmarker_lite.task", "running_mode": "VIDEO", "num_poses": 1,
    "landmark_names": [33 names, MediaPipe order],
    "frames_data": [ { "i": frame index, "t": seconds, "people": [ [[x, y, z, visibility, presence] x 33], ... ] }, ... ]
  }
  x, y are normalized to the analysed image (0..1, origin top left, not mirrored); z is MediaPipe's relative depth
  (hip-centred, same scale as x). People are sorted largest first (bounding box of landmarks with visibility >= 0.5).

Summary (--summary): see summarize(). In short: detection rate, mean visibility of hips/knees/ankles (is the whole body
in frame), how many people are found, a pose-change score per frame (mean angular speed of 10 segments, the 8 limb
bones plus the shoulder and hip lines, over a 0.25 s lag, deg/s: about 3-8 on a still body, 40+ on a clear move), and
the time ranges where a full body holds a clear pose for >= 0.5 s and where it moves between poses. Every threshold is
a flag (--full-body-vis, --hold-speed, --move-speed, --min-hold); --subject fullest follows the most fully visible
person instead of the largest (over-the-shoulder shots).

    promo/.venv/bin/python promo/scripts/detect.py --from-json OUT.json [--summary SUM.json] [--subject fullest]
        re-summarizes a saved -o file without detecting again.

Needs: python3 -m venv promo/.venv && promo/.venv/bin/pip install mediapipe opencv-python-headless numpy
(on a bare Linux image MediaPipe also needs the system libraries libegl1 and libgles2).
"""
from __future__ import annotations

import argparse
import json
import math
import sys
import warnings
from pathlib import Path

import cv2
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "app" / "src" / "main" / "assets" / "models"
MIN_CONFIDENCE = 0.5  # PoseDetector.MIN_CONFIDENCE

NAMES = [
    "nose", "left_eye_inner", "left_eye", "left_eye_outer", "right_eye_inner", "right_eye", "right_eye_outer",
    "left_ear", "right_ear", "mouth_left", "mouth_right", "left_shoulder", "right_shoulder", "left_elbow",
    "right_elbow", "left_wrist", "right_wrist", "left_pinky", "right_pinky", "left_index", "right_index",
    "left_thumb", "right_thumb", "left_hip", "right_hip", "left_knee", "right_knee", "left_ankle", "right_ankle",
    "left_heel", "right_heel", "left_foot_index", "right_foot_index",
]
IDX = {n: i for i, n in enumerate(NAMES)}
LOWER = [IDX[n] for n in ("left_hip", "right_hip", "left_knee", "right_knee", "left_ankle", "right_ankle")]
# Segments whose image-plane angle describes the pose: the 8 limb bones, the shoulder line and the hip line.
SEGMENTS = [
    ("left_shoulder", "left_elbow"), ("left_elbow", "left_wrist"),
    ("right_shoulder", "right_elbow"), ("right_elbow", "right_wrist"),
    ("left_hip", "left_knee"), ("left_knee", "left_ankle"),
    ("right_hip", "right_knee"), ("right_knee", "right_ankle"),
    ("left_shoulder", "right_shoulder"), ("left_hip", "right_hip"),
]
SEG_IDX = [(IDX[a], IDX[b]) for a, b in SEGMENTS]


# ----------------------------------------------------------------------------------------------------------- detect

def model_path(name: str) -> Path:
    if name in ("lite", "full"):
        return MODELS / f"pose_landmarker_{name}.task"
    return Path(name)


def detect(video: Path, model: str = "lite", num_poses: int = 1, crop=None, every: int = 1, max_side: int = 1920):
    from mediapipe import Image, ImageFormat
    from mediapipe.tasks.python import BaseOptions
    from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode

    mpath = model_path(model)
    options = PoseLandmarkerOptions(
        base_options=BaseOptions(model_asset_path=str(mpath)),
        running_mode=RunningMode.VIDEO,
        num_poses=num_poses,
        min_pose_detection_confidence=MIN_CONFIDENCE,
        min_pose_presence_confidence=MIN_CONFIDENCE,
        min_tracking_confidence=MIN_CONFIDENCE,
        output_segmentation_masks=False,
    )
    cap = cv2.VideoCapture(str(video))
    if not cap.isOpened():
        raise SystemExit(f"cannot open {video}")
    fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    frames = []
    i = 0
    # size of the analysed image (the crop, if any): landmark x and y are normalized to it
    img_w, img_h = width, height
    if crop:
        img_w, img_h = round(crop[2] * width), round(crop[3] * height)
    with PoseLandmarker.create_from_options(options) as landmarker:
        while True:
            ok, bgr = cap.read()
            if not ok:
                break
            if i % every == 0:
                img = bgr
                if crop:
                    x, y, w, h = crop
                    x0, y0 = int(round(x * img.shape[1])), int(round(y * img.shape[0]))
                    x1, y1 = int(round((x + w) * img.shape[1])), int(round((y + h) * img.shape[0]))
                    img = img[y0:y1, x0:x1]
                longest = max(img.shape[:2])
                if longest > max_side:
                    s = max_side / longest
                    img = cv2.resize(img, (max(1, round(img.shape[1] * s)), max(1, round(img.shape[0] * s))),
                                     interpolation=cv2.INTER_AREA)
                rgb = np.ascontiguousarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
                ts_ms = int(round(i * 1000.0 / fps))
                result = landmarker.detect_for_video(Image(image_format=ImageFormat.SRGB, data=rgb), ts_ms)
                people = []
                for person in result.pose_landmarks:
                    people.append([[round(l.x, 5), round(l.y, 5), round(l.z, 5),
                                    round(l.visibility if l.visibility is not None else 1.0, 4),
                                    round(l.presence if l.presence is not None else 1.0, 4)] for l in person])
                people.sort(key=lambda p: -bbox_area(p))
                frames.append({"i": i, "t": round(i / fps, 4), "people": people})
            i += 1
    cap.release()
    return {
        "source": str(video),
        "width": width, "height": height, "fps": round(fps, 4), "frames": i, "duration": round(i / fps, 4),
        "crop": list(crop) if crop else None,
        "image_width": img_w, "image_height": img_h,
        "model": mpath.name, "running_mode": "VIDEO", "num_poses": num_poses,
        "min_confidence": MIN_CONFIDENCE, "every": every,
        "landmark_names": NAMES,
        "frames_data": frames,
    }


def bbox_area(person, vis=0.5):
    pts = [(l[0], l[1]) for l in person if l[3] >= vis]
    if len(pts) < 2:
        return 0.0
    xs, ys = zip(*pts)
    return (max(xs) - min(xs)) * (max(ys) - min(ys))


# -------------------------------------------------------------------------------------------------------- summarize

def ranges(mask, times, min_len):
    """Contiguous True runs of mask as [start_s, end_s] with end - start >= min_len."""
    out, start = [], None
    for k, m in enumerate(mask):
        if m and start is None:
            start = k
        if (not m or k == len(mask) - 1) and start is not None:
            end = k if m else k - 1
            t0, t1 = times[start], times[end]
            if t1 - t0 >= min_len:
                out.append((start, end))
            start = None
    return out


def smooth(x, n):
    if n <= 1 or len(x) == 0:
        return np.asarray(x, dtype=float)
    k = np.ones(n) / n
    xp = np.pad(np.asarray(x, dtype=float), (n // 2, n - 1 - n // 2), mode="edge")
    return np.convolve(xp, k, mode="valid")


def pick(people, subject="largest", seg_vis=0.5):
    """Index of the person the summary follows: the largest (default; people are sorted largest first) or the one whose
    hips, knees and ankles are most visible ("fullest": the subject of an over-the-shoulder shot, not the foreground)."""
    if subject == "fullest" and len(people) > 1:
        return max(range(len(people)), key=lambda j: (sum(people[j][i][3] for i in LOWER), bbox_area(people[j])))
    return 0


def is_full_body(person, full_body_vis=0.6, seg_vis=0.5):
    v = [l[3] for l in person]
    return (sum(v[i] for i in LOWER) / len(LOWER) >= full_body_vis and v[IDX["left_ankle"]] >= seg_vis
            and v[IDX["right_ankle"]] >= seg_vis and v[IDX["nose"]] >= seg_vis)


def summarize(data, full_body_vis=0.6, seg_vis=0.5, hold_speed=15.0, move_speed=40.0, min_hold=0.5, min_move=0.2,
              subject="largest"):
    """
    Per frame, for the followed person (pick(): the largest, or with subject="fullest" the most fully visible):
      lower_vis    mean visibility of hips, knees and ankles (6 landmarks). full_body = lower_vis >= full_body_vis
                   and both ankles >= seg_vis, and the nose too: the whole body is in frame.
      angles       image-plane angle of the 10 SEGMENTS (deg), NaN where either end is below seg_vis.
      speed        pose-change score: mean |angle(t + 0.125 s) - angle(t - 0.125 s)| / 0.25 s over the segments visible
                   at both ends (deg/s, at least 4 segments), lightly smoothed (0.1 s). The lag averages out landmark
                   jitter. Camera zoom and pan barely change it; the person moving does.
    Holds: full body, speed < hold_speed for >= min_hold s. Transitions: speed > move_speed for >= min_move s, or the
    stretch between two holds. Each hold reports its mean segment angles (for matching against library poses) and a
    few plain pose features (which wrists are above the shoulders, hands near the hips, a bent knee).
    """
    fr = data["frames_data"]
    fps = data["fps"] / max(1, data.get("every", 1))
    crop = data.get("crop")
    img_w = data.get("image_width") or (round(crop[2] * data["width"]) if crop else data["width"])
    img_h = data.get("image_height") or (round(crop[3] * data["height"]) if crop else data["height"])
    n = len(fr)
    times = [f["t"] for f in fr]
    detected = np.array([len(f["people"]) > 0 for f in fr])
    people_counts = [len(f["people"]) for f in fr]
    lower_vis = np.full(n, np.nan)
    ankle_ok = np.zeros(n, bool)
    nose_ok = np.zeros(n, bool)
    height = np.full(n, np.nan)
    angles = np.full((n, len(SEG_IDX)), np.nan)
    chosen = [pick(f["people"], subject, seg_vis) if f["people"] else None for f in fr]
    any_full = np.array([any(is_full_body(q, full_body_vis, seg_vis) for q in f["people"]) for f in fr])
    for k, f in enumerate(fr):
        if not f["people"]:
            continue
        p = np.array(f["people"][chosen[k]])
        vis = p[:, 3]
        lower_vis[k] = float(np.mean(vis[LOWER]))
        ankle_ok[k] = vis[IDX["left_ankle"]] >= seg_vis and vis[IDX["right_ankle"]] >= seg_vis
        nose_ok[k] = vis[IDX["nose"]] >= seg_vis
        ys = p[vis >= seg_vis, 1]
        if len(ys):
            height[k] = float(ys.max() - ys.min())
        for s, (a, b) in enumerate(SEG_IDX):
            if vis[a] >= seg_vis and vis[b] >= seg_vis:
                dx, dy = p[b, 0] - p[a, 0], p[b, 1] - p[a, 1]
                # image x and y are scaled differently by the aspect ratio: angle in pixels
                angles[k, s] = math.degrees(math.atan2(dy * img_h, dx * img_w))
    full_body = (lower_vis >= full_body_vis) & ankle_ok & nose_ok

    # Angular speed over a lag of about 0.25 s (centred), so landmark jitter averages out and real moves stay.
    lag = max(1, int(round(0.25 * fps)))
    speed = np.full(n, np.nan)
    for k in range(n):
        k0, k1 = k - lag // 2, k + (lag - lag // 2)
        if k0 < 0 or k1 >= n:
            continue
        a0, a1 = angles[k0], angles[k1]
        ok = ~np.isnan(a0) & ~np.isnan(a1)
        if ok.sum() >= 4:
            d = (a1[ok] - a0[ok] + 180.0) % 360.0 - 180.0
            speed[k] = float(np.mean(np.abs(d))) * fps / lag
    filled = np.where(np.isnan(speed), 0.0, speed)
    sm = smooth(filled, max(1, int(round(0.1 * fps))))
    sm[np.isnan(speed)] = np.nan

    hold_mask = full_body & (np.nan_to_num(sm, nan=1e9) < hold_speed)
    holds = ranges(list(hold_mask), times, min_hold)
    move_mask = np.nan_to_num(sm, nan=0.0) > move_speed
    moves = ranges(list(move_mask), times, min_move)

    def feats(k0, k1):
        ps = [np.array(fr[k]["people"][chosen[k]]) for k in range(k0, k1 + 1) if fr[k]["people"]]
        p = np.mean(ps, axis=0)
        y = lambda name: p[IDX[name], 1]
        x = lambda name: p[IDX[name], 0]
        torso = abs((y("left_hip") + y("right_hip")) / 2 - (y("left_shoulder") + y("right_shoulder")) / 2) or 1e-6
        out = []
        for side in ("left", "right"):
            if y(f"{side}_wrist") < y(f"{side}_shoulder"):
                out.append(f"{side} wrist above shoulder")
            elif math.hypot(x(f"{side}_wrist") - x(f"{side}_hip"), y(f"{side}_wrist") - y(f"{side}_hip")) < 0.35 * torso:
                out.append(f"{side} hand near hip")
            # knee angle
            a = np.array([x(f"{side}_hip"), y(f"{side}_hip")])
            b = np.array([x(f"{side}_knee"), y(f"{side}_knee")])
            c = np.array([x(f"{side}_ankle"), y(f"{side}_ankle")])
            v1, v2 = a - b, c - b
            cosang = float(np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2) + 1e-9))
            if math.degrees(math.acos(max(-1.0, min(1.0, cosang)))) < 150:
                out.append(f"{side} knee bent")
        return out

    def seg_means(k0, k1):
        block = angles[k0:k1 + 1]
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", RuntimeWarning)
            m = np.nanmean(block, axis=0)
        return {f"{a}-{b}": (None if np.isnan(v) else round(float(v), 1)) for (a, b), v in zip(SEGMENTS, m)}

    def r(x, nd=3):
        return None if x is None or (isinstance(x, float) and math.isnan(x)) else round(float(x), nd)

    hold_list = []
    for k0, k1 in holds:
        hold_list.append({
            "start": r(times[k0], 2), "end": r(times[k1], 2), "length": r(times[k1] - times[k0], 2),
            "mean_speed": r(np.nanmean(sm[k0:k1 + 1]), 1),
            "lower_vis": r(np.nanmean(lower_vis[k0:k1 + 1]), 2),
            "body_height": r(np.nanmean(height[k0:k1 + 1]), 2),
            "features": feats(k0, k1),
            "segment_angles": seg_means(k0, k1),
        })
    move_list = [{"start": r(times[k0], 2), "end": r(times[k1], 2), "length": r(times[k1] - times[k0], 2),
                  "peak_speed": r(np.nanmax(sm[k0:k1 + 1]), 1)} for k0, k1 in moves]
    # between consecutive holds: the transition the coaching act would show
    between = []
    for (a0, a1), (b0, b1) in zip(holds, holds[1:]):
        seg = sm[a1:b0 + 1]
        between.append({"from_hold_end": r(times[a1], 2), "to_hold_start": r(times[b0], 2),
                        "length": r(times[b0] - times[a1], 2),
                        "peak_speed": r(np.nanmax(seg) if np.any(~np.isnan(seg)) else None, 1)})

    det_people = [c for c in people_counts if c > 0]
    return {
        "source": data["source"], "model": data["model"], "num_poses": data["num_poses"], "crop": data["crop"],
        "subject": subject,
        "duration": data["duration"], "frames_analysed": n,
        "detection_rate": r(detected.mean() if n else 0.0),
        "full_body_rate": r(full_body.mean() if n else 0.0),
        "any_person_full_body_rate": r(any_full.mean() if n else 0.0),
        "lower_body_visibility_mean": r(np.nanmean(lower_vis) if detected.any() else None),
        "lower_body_visibility_by_joint": {
            NAMES[j]: r(np.mean([f["people"][chosen[k]][j][3] for k, f in enumerate(fr) if f["people"]])
                        if detected.any() else None)
            for j in LOWER
        },
        "body_height_median": r(np.nanmedian(height) if detected.any() else None),
        "people_max": int(max(people_counts) if people_counts else 0),
        "people_mode": int(max(set(det_people), key=det_people.count)) if det_people else 0,
        "pose_speed_median": r(np.nanmedian(sm) if np.any(~np.isnan(sm)) else None, 1),
        "pose_speed_p90": r(np.nanpercentile(sm, 90) if np.any(~np.isnan(sm)) else None, 1),
        "thresholds": {"full_body_vis": full_body_vis, "seg_vis": seg_vis, "hold_speed": hold_speed,
                       "move_speed": move_speed, "min_hold": min_hold, "min_move": min_move},
        "holds": hold_list,
        "moves": move_list,
        "between_holds": between,
        "speed_track": [r(v, 1) for v in sm[:: max(1, int(round(fps / 4)))]],  # 4 samples per second
    }


# -------------------------------------------------------------------------------------------------------------- cli

def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("video", type=Path, nargs="?")
    ap.add_argument("-o", "--out", type=Path)
    ap.add_argument("--summary", type=Path)
    ap.add_argument("--model", default="lite")
    ap.add_argument("--num-poses", type=int, default=1)
    ap.add_argument("--crop", type=lambda s: tuple(float(v) for v in s.split(",")))
    ap.add_argument("--every", type=int, default=1)
    ap.add_argument("--max-side", type=int, default=1920)
    ap.add_argument("--from-json", type=Path, help="summarize an existing -o file instead of detecting again")
    ap.add_argument("--full-body-vis", type=float, default=0.6)
    ap.add_argument("--hold-speed", type=float, default=15.0)
    ap.add_argument("--move-speed", type=float, default=40.0)
    ap.add_argument("--min-hold", type=float, default=0.5)
    ap.add_argument("--subject", choices=("largest", "fullest"), default="largest",
                    help="whom the summary follows when several people are found (fullest: the subject of an "
                         "over-the-shoulder shot)")
    a = ap.parse_args(argv)
    if not a.video and not a.from_json:
        ap.error("give a video, or --from-json with a saved -o file")

    if a.from_json:
        data = json.loads(a.from_json.read_text())
    else:
        data = detect(a.video, a.model, a.num_poses, a.crop, a.every, a.max_side)
        if a.out:
            a.out.parent.mkdir(parents=True, exist_ok=True)
            a.out.write_text(json.dumps(data, separators=(",", ":")))
    s = summarize(data, full_body_vis=a.full_body_vis, hold_speed=a.hold_speed, move_speed=a.move_speed,
                  min_hold=a.min_hold, subject=a.subject)
    text = json.dumps(s, indent=1)
    if a.summary:
        a.summary.parent.mkdir(parents=True, exist_ok=True)
        a.summary.write_text(text)
    brief = {k: s[k] for k in ("source", "subject", "detection_rate", "full_body_rate", "lower_body_visibility_mean",
                               "people_max", "pose_speed_median")}
    brief["holds"] = [(h["start"], h["end"]) for h in s["holds"]]
    print(json.dumps(brief))


if __name__ == "__main__":
    sys.exit(main())
