#!/usr/bin/env python3
"""
detect_images.py: the app's full pose model on every candidate image, up to 4 people each.

    python3 tools/pose-photos/detect_images.py [--source mixkit|commons|all] [--model full|lite] [--num-poses 4]

Inputs:  WORK/mixkit/frames/*.jpg (frames kept by mixkit_harvest.py scan), WORK/commons/img/*.jpg (commons_harvest.py)
Output:  WORK/landmarks/<source>.jsonl, one line per image:
         {"id": "mixkit:<clip>_<ms>" | "commons:<pageid>", "source", "image": path, "w", "h",
          "people": [[[x, y, z, visibility, presence] x 33], ...]}   (largest person first; x, y normalized to the image)
Model and settings as the app's still-photo path: MediaPipe Pose Landmarker (pose_landmarker_full.task, the app's
"accurate model"), IMAGE mode, min detection / presence confidence 0.5, the image upright and not mirrored. One
process, one landmarker; images already in the output are skipped, so it can be re-run as new images arrive.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import MODELS, WORK  # noqa: E402

SOURCES = {
    "mixkit": (WORK / "mixkit" / "frames", lambda p: "mixkit:" + p.stem),
    "commons": (WORK / "commons" / "img", lambda p: "commons:" + p.stem),
}


def bbox_area(person, vis=0.5):
    pts = [(l[0], l[1]) for l in person if l[3] >= vis]
    if len(pts) < 2:
        return 0.0
    xs, ys = zip(*pts)
    return (max(xs) - min(xs)) * (max(ys) - min(ys))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", default="all", choices=["all", *SOURCES])
    ap.add_argument("--model", default="full", choices=["full", "lite"])
    ap.add_argument("--num-poses", type=int, default=4)
    ap.add_argument("--max-side", type=int, default=1280)
    args = ap.parse_args()

    import cv2
    import numpy as np
    from mediapipe import Image, ImageFormat
    from mediapipe.tasks.python import BaseOptions
    from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode

    opts = PoseLandmarkerOptions(
        base_options=BaseOptions(model_asset_path=str(MODELS / f"pose_landmarker_{args.model}.task")),
        running_mode=RunningMode.IMAGE, num_poses=args.num_poses,
        min_pose_detection_confidence=0.5, min_pose_presence_confidence=0.5, min_tracking_confidence=0.5,
        output_segmentation_masks=False)
    out_dir = WORK / "landmarks"
    out_dir.mkdir(parents=True, exist_ok=True)
    with PoseLandmarker.create_from_options(opts) as lm:
        for name, (folder, make_id) in SOURCES.items():
            if args.source not in ("all", name) or not folder.exists():
                continue
            out = out_dir / f"{name}.jsonl"
            seen = set()
            if out.exists():
                for line in out.read_text().splitlines():
                    try:
                        seen.add(json.loads(line)["id"])
                    except (json.JSONDecodeError, KeyError):
                        pass
            todo = [p for p in sorted(folder.glob("*.jpg")) if make_id(p) not in seen]
            print(f"{name}: {len(seen)} done, {len(todo)} to detect", flush=True)
            with out.open("a") as f:
                for k, p in enumerate(todo):
                    bgr = cv2.imread(str(p))
                    if bgr is None:
                        continue
                    h, w = bgr.shape[:2]
                    s = args.max_side / max(h, w)
                    if s < 1:
                        bgr = cv2.resize(bgr, (round(w * s), round(h * s)), interpolation=cv2.INTER_AREA)
                    rgb = np.ascontiguousarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
                    res = lm.detect(Image(image_format=ImageFormat.SRGB, data=rgb))
                    people = [[[round(l.x, 5), round(l.y, 5), round(l.z, 5),
                                round(l.visibility if l.visibility is not None else 1.0, 4),
                                round(l.presence if l.presence is not None else 1.0, 4)] for l in person]
                              for person in res.pose_landmarks]
                    people.sort(key=lambda q: -bbox_area(q))
                    f.write(json.dumps({"id": make_id(p), "source": name, "image": str(p), "w": w, "h": h,
                                        "people": people}) + "\n")
                    if k % 200 == 199:
                        f.flush()
                        print(f"  {k + 1}/{len(todo)}", flush=True)
            print(f"{name}: done", flush=True)


if __name__ == "__main__":
    main()
