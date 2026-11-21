#!/usr/bin/env python3
"""
sitting-floor_finalize.py: turn the chosen candidates of the SITTING, FLOOR and WALKING poses into the app's sample
photos, landmarks and credits.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/sitting-floor_finalize.py frame CLIP T1[,T2] [--box x0,y0,x1,y1]
    $PY -I tools/pose-photos/picks/sitting-floor_finalize.py build [--poses id,id]

frame  fetches a Mixkit clip's licensed download (the clip page's 1080p download link, 720p as fallback) into
       WORK/picks_dl, extracts every frame within 0.3 s of each hold time and keeps the sharpest by Laplacian variance
       (measured on the person box, fractions of the frame), as WORK/picks_dl/<clip>_<ms>.png plus a .json note
       (download page, video URL, exact frame time); the video is deleted afterwards.
build  for every pick in PICKS: detects the person on the full-resolution source (the app's full model, IMAGE mode,
       up to 4 people, as detect_images.py), places the pose's guide with score.py's drag-and-pinch solve and takes the
       pose's own 3:4 frame (score.py crop_px) as the crop, grown to keep the whole person with a small margin and
       moved/shrunk to stay inside the photo (or a manual crop); mirrors when the photo matches the flipped pose;
       resizes to 900 px on the long side; saves WebP quality 78 without metadata to
       app/src/main/assets/pose_photos/<poseId>.webp; runs the same detection on the saved file
       (tools/pose-photos/landmarks/<poseId>.json) and scores it with posematch (the Python port of PoseMatcher);
       writes the credits to tools/pose-photos/picks/sitting-floor.json.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parent
sys.path.insert(0, str(TOOLS))
import score as sc  # noqa: E402
from common import MODELS, WORK, load_poses  # noqa: E402

ROOT = TOOLS.parent.parent
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "pose_photos"
LM_DIR = TOOLS / "landmarks"
CREDITS = HERE / "sitting-floor.json"
DL = WORK / "picks_dl"
UA = "Mozilla/5.0 (X11; Linux x86_64) pose-photos-review"
LONG_SIDE, QUALITY = 900, 78
CHECKED = "2026-11-21"

# The picks, chosen by looking at the contact sheets and the full-resolution frames. "src" is a Mixkit frame made by
# `frame` (picks_dl/<clip>_<ms>.png) or a Commons rendition (picks_dl/commons/<pageid>.jpg, a standard 1920 px
# thumbnail). "crop" (optional) overrides the automatic crop: [x0, y0, x1, y1] as fractions of the source.
PICKS = [
    {"pose": "walk_toward_camera", "src": "mixkit", "clip": 4856, "hold": 13.347,
     "reason": "Full-body woman walking straight at the camera on a park path mid-stride, arms swinging softly; the "
               "clearest 'walk toward the camera' among the candidates."},
    {"pose": "walk_across", "src": "mixkit", "clip": 4855, "hold": 8.342,
     "reason": "Side-on full stride across the frame, arms swinging opposite to the legs, sharp and evenly lit: "
               "exactly the drawn walk."},
    {"pose": "walk_twirl", "src": "mixkit", "clip": 41017, "hold": 2.002,
     "reason": "Slow-motion spin with both arms out wide and one foot kicked up behind, the shape of the drawn "
               "arms-out spin."},
    {"pose": "sit_crossed_legs", "src": "mixkit", "clip": 34407, "hold": 8.0,
     "reason": "Seated upright on a chair facing the camera with the legs crossed at the knee and angled to one side; "
               "the hands are behind the head rather than on the knee (the clip's other hold, 5.0 s, has the hands at "
               "the lap but she is looking down adjusting her dress)."},
    {"pose": "sit_elbows_on_knees", "src": "commons", "pageid": 77903052,
     "reason": "Sitting on steps, knees apart, forearms resting on the knees with the hands loosely clasped: the pose "
               "seen from the front (the figure is drawn side-on, hence the low matcher score)."},
    {"pose": "floor_cross_legged", "src": "mixkit", "clip": 4892, "hold": 8.342,
     "reason": "Sitting cross-legged on grass with the knees low and wide and the hands resting on the knees; "
               "three-quarter view, eyes closed (a meditation clip), whole body in frame."},
    {"pose": "floor_side_lie", "src": "mixkit", "clip": 101457, "hold": 3.337,
     "reason": "Lying on her side on floor cushions propped on one elbow with the head in that hand and the knees "
               "bent, head to the right as drawn; the matcher scores lying poses low."},
    {"pose": "floor_lean_back", "src": "commons", "pageid": 92090560,
     "reason": "Sitting on a jetty with the legs stretched out, leaning back on straight arms with the face tipped "
               "up to the light: the drawn lean-back almost exactly (mirrored to match the figure)."},
]


# Poses left without a photo: no candidate a person would recognise as the pose, or only ones that break a rule.
GAPS = {
    "walk_look_back": "No one walking away and glancing back: the best-ranked frame (Mixkit 4856 at 9.3 s) is a dancing "
                      "spin with the back to the camera, the rest stand still or walk away without looking back.",
    "walk_kerb_step": "No step down from a kerb or step in any candidate; the high scorers are people standing or "
                      "walking on flat ground.",
    "walk_skip": "No skip with a knee lifted high; top candidates are a ballet pose and dancers.",
    "walk_hat_hold": "No one holding a hat on the head while walking; the two Commons garden photos show a hat held "
                     "out or waved, not the drawn hand-on-hat.",
    "walk_pockets": "No walking with hands in pockets; the only hands-in-pockets photo (Commons 'Young man standing on "
                    "a rusty bridge') is a standing pose whose description names the person, so it is excluded.",
    "walk_coffee_run": "No one walking with a cup; the coffee clips are people seated in cafes or at home.",
    "walk_jacket_shoulder": "No jacket slung over the shoulder in any candidate.",
    "sit_one_knee_up": "No one sitting on steps with one foot a step higher; the step-sitters have both feet level.",
    "sit_side_saddle": "No knees-together, legs-to-one-side sit on a bench or chair; the closest (Mixkit 34407) is "
                       "used for crossed legs, others are a bathroom counter (excluded context) or candid seniors.",
    "sit_steps_sideways": "No one sitting along a step with the back to the wall.",
    "sit_hug_knees": "Near miss only: Mixkit 51062 (8.3 s) hugs one knee, not both, with the photographer's blurred "
                     "shoulder in the foreground and the feet cut off; Commons 'Blond woman in a red dress seated on "
                     "stairs' holds a wine glass (alcohol). Left empty.",
    "sit_armchair_lounge": "No one draped sideways over an armchair; the top Commons hit is a swimwear photo (excluded).",
    "sit_chair_backwards": "The only backwards-chair photo is a named public figure (Commons 'Leonard Bernstein in his "
                           "studio'), excluded.",
    "sit_ankle_on_knee": "The one clear ankle-on-knee (Mixkit 2146, man on a stool) has a large projected countdown "
                         "digit behind him and a light grid over his body; not a clean demonstration.",
    "sit_high_perch": "Only Mixkit 2146 (same stool clip as above: large projected digit, grid light over the body, and "
                      "one leg crossed rather than on a rung).",
    "floor_street_squat": "No one squatting; the top scores are people sitting on steps (seen from the front they "
                          "match the shape, but they are not squats).",
    "floor_low_crouch": "No crouch on the balls of the feet; Commons crouching photos are candid street scenes with "
                        "other people, side-on and small.",
    "floor_one_knee": "No one kneeling on one knee.",
    "floor_feet_up": "No one lying on the stomach with feet up.",
    "floor_overhead_lie": "No overhead shot of someone lying on their back (the one overhead clip, Mixkit 41737, is a "
                          "dark head-and-shoulders close-up among candles).",
    "floor_sit_on_heels": "No one kneeling back on the heels; the nearest is a sideways floor sit (Mixkit 101459), "
                          "which reads as a different pose.",
}


# ------------------------------------------------------------------------------------------------ Mixkit frames
def http_get(url, dest=None):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=180) as r:
        if dest is None:
            return r.read().decode("utf-8", "replace")
        with open(dest, "wb") as f:
            shutil.copyfileobj(r, f, 1 << 20)
    return dest


def mixkit_video_url(clip):
    for q in ("1080p", "720p"):
        page = f"https://mixkit.co/free-stock-video/download/{clip}/?context=sidebar&type={q}"
        html = http_get(page)
        m = re.findall(r'download--modal-url-value="(https://assets\.mixkit\.co/[^"]+)"', html) or \
            re.findall(r"https://assets\.mixkit\.co/videos/\d+/[\w-]+\.mp4", html)
        if m:
            return page, m[0], q
        time.sleep(2)
    raise SystemExit(f"no download link for Mixkit clip {clip}")


def cmd_frame(clip, times, box):
    import cv2
    DL.mkdir(parents=True, exist_ok=True)
    mp4 = DL / f"{clip}.mp4"
    page, url, q = mixkit_video_url(clip)
    time.sleep(2)
    http_get(url, mp4)
    try:
        probe = json.loads(subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height,r_frame_rate",
             "-of", "json", str(mp4)], capture_output=True, text=True, check=True).stdout)["streams"][0]
        num, den = probe["r_frame_rate"].split("/")
        fps, W, H = float(num) / float(den), probe["width"], probe["height"]
        for t in times:
            tmp = DL / f"tmp_{clip}"
            shutil.rmtree(tmp, ignore_errors=True)
            tmp.mkdir()
            start = max(0.0, t - 0.3)
            subprocess.run(["ffmpeg", "-v", "error", "-ss", f"{start:.3f}", "-i", str(mp4), "-t", "0.6", "-vsync", "0",
                            str(tmp / "f_%04d.png")], check=True)
            frames = sorted(tmp.glob("f_*.png"))
            best = None
            for k, f in enumerate(frames):
                g = cv2.cvtColor(cv2.imread(str(f)), cv2.COLOR_BGR2GRAY)
                if box:
                    g = g[int(box[1] * H):int(box[3] * H), int(box[0] * W):int(box[2] * W)]
                v = float(cv2.Laplacian(g, cv2.CV_64F).var())
                if best is None or v > best[0]:
                    best = (v, k, f)
            out = DL / f"{clip}_{round(t * 1000):06d}.png"
            shutil.copy(best[2], out)
            note = {"clip": int(clip), "hold_t": t, "frame_t": round(start + best[1] / fps, 3),
                    "lapvar": round(best[0], 1), "n_frames": len(frames), "width": W, "height": H,
                    "download_page": page, "video_url": url, "quality": q}
            out.with_suffix(".json").write_text(json.dumps(note, indent=1))
            print(out, note, flush=True)
            shutil.rmtree(tmp)
    finally:
        mp4.unlink(missing_ok=True)


# ------------------------------------------------------------------------------------------------ detection
class Detector:
    """detect_images.py's settings: pose_landmarker_full.task, IMAGE mode, 4 people, confidences 0.5, max side 1280."""

    def __init__(self):
        from mediapipe.tasks.python import BaseOptions
        from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode
        opts = PoseLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=str(MODELS / "pose_landmarker_full.task")),
            running_mode=RunningMode.IMAGE, num_poses=4, min_pose_detection_confidence=0.5,
            min_pose_presence_confidence=0.5, min_tracking_confidence=0.5, output_segmentation_masks=False)
        self.lm = PoseLandmarker.create_from_options(opts)

    def detect(self, pil_img, rec_id, image_ref):
        import numpy as np
        from mediapipe import Image, ImageFormat
        im = pil_img.convert("RGB")
        w, h = im.size
        s = 1280 / max(w, h)
        if s < 1:
            im = im.resize((round(w * s), round(h * s)), resample=3)
        res = self.lm.detect(Image(image_format=ImageFormat.SRGB, data=np.ascontiguousarray(np.asarray(im))))
        people = [[[round(l.x, 5), round(l.y, 5), round(l.z, 5),
                    round(l.visibility if l.visibility is not None else 1.0, 4),
                    round(l.presence if l.presence is not None else 1.0, 4)] for l in person]
                  for person in res.pose_landmarks]

        def area(q):
            pts = [(l[0], l[1]) for l in q if l[3] >= 0.5]
            if len(pts) < 2:
                return 0.0
            xs, ys = zip(*pts)
            return (max(xs) - min(xs)) * (max(ys) - min(ys))
        people.sort(key=lambda q: -area(q))
        return {"id": rec_id, "source": "pose_photo", "image": image_ref, "w": w, "h": h, "people": people}


def person_extent(rec):
    """Box of the largest person's visible MediaPipe landmarks (all 33), in pixels."""
    if not rec["people"]:
        return None
    pts = [(l[0], l[1]) for l in rec["people"][0] if l[3] >= 0.5]
    xs, ys = zip(*pts)
    return [min(xs) * rec["w"], min(ys) * rec["h"], max(xs) * rec["w"], max(ys) * rec["h"]]


def fit_crop(crop, person, W, H, aspect=0.75):
    """Grow the pose frame to hold the person (+ margin: more above for the head), keep 3:4, then shrink/move it
    inside the photo."""
    x0, y0, x1, y1 = crop
    if person:
        px0, py0, px1, py1 = person
        ph, pw = py1 - py0, px1 - px0
        need = [px0 - 0.06 * max(pw, ph * 0.3), py0 - 0.10 * ph, px1 + 0.06 * max(pw, ph * 0.3), py1 + 0.05 * ph]
        x0, y0, x1, y1 = min(x0, need[0]), min(y0, need[1]), max(x1, need[2]), max(y1, need[3])
    if person:  # centre on the person horizontally: a sample photo, not the guide's exact placement
        half = max((x1 - x0) / 2, (person[2] - person[0]) / 2 + 0.06 * (person[3] - person[1]))
        x0, x1 = (person[0] + person[2]) / 2 - half, (person[0] + person[2]) / 2 + half
    w, h = x1 - x0, y1 - y0
    if w / h > aspect:      # too wide: grow the height around the centre
        cy, h = (y0 + y1) / 2, w / aspect
        y0, y1 = cy - h / 2, cy + h / 2
    else:
        cx, w = (x0 + x1) / 2, h * aspect
        x0, x1 = cx - w / 2, cx + w / 2
    s = min(1.0, W / w, H / h)  # shrink to fit the photo, around the person's centre
    if s < 1.0:
        cx, cy = ((person[0] + person[2]) / 2, (person[1] + person[3]) / 2) if person else ((x0 + x1) / 2, (y0 + y1) / 2)
        w, h = w * s, h * s
        x0, y0 = cx - w / 2, cy - h / 2
        x1, y1 = x0 + w, y0 + h
    dx = -x0 if x0 < 0 else (W - x1 if x1 > W else 0)
    dy = -y0 if y0 < 0 else (H - y1 if y1 > H else 0)
    return [round(x0 + dx), round(y0 + dy), round(x1 + dx), round(y1 + dy)]


# ------------------------------------------------------------------------------------------------ build
def source_info(p):
    if p["src"] == "mixkit":
        note = json.loads((DL / f"{p['clip']}_{round(p['hold'] * 1000):06d}.json").read_text())
        clip = json.loads((WORK / "mixkit" / "clips" / f"{p['clip']}.json").read_text())
        img = DL / f"{p['clip']}_{round(p['hold'] * 1000):06d}.png"
        return img, {
            "source": "Mixkit", "title": clip["title"], "page_url": clip["page"],
            "file_url": note["download_page"], "video_file": note["video_url"], "video_quality": note["quality"],
            "frame_time_s": note["frame_t"], "hold_time_s_in_preview": note["hold_t"],
            "author": "Mixkit (no author named on the clip page)",
            "licence": {"name": clip["licence"]["panel_licence"], "url": "https://mixkit.co/license/#videoFree",
                        "stated_on_page": clip["licence"]["stated_on_page"], "checked": CHECKED},
            "attribution": f"Video still: Mixkit, Mixkit Stock Video Free License ({clip['page']})",
        }
    files = json.loads((WORK / "commons" / "files.json").read_text())
    r = files[str(p["pageid"])]
    lic = r["licence"]
    img = DL / "commons" / f"{p['pageid']}.jpg"
    thumb = re.sub(r"/\d+px-", "/1920px-", (r.get("thumb_url") or "").split("?")[0]).replace(
        "thumb.wikimedia.org", "upload.wikimedia.org")
    return img, {
        "source": "Wikimedia Commons", "title": r["title"], "page_url": r["page"],
        "file_url": r["file_url"].split("?")[0], "rendition_used": thumb,
        "author": lic.get("artist") or "unknown", "credit_line": lic.get("credit"),
        "licence": {"name": lic.get("short_name"), "url": lic.get("url"),
                    "attribution_required": lic.get("attribution_required"), "restrictions": lic.get("restrictions"),
                    "checked": CHECKED},
        "attribution": f"Photo: {lic.get('artist')}, {lic.get('short_name')} ({lic.get('url')}), via Wikimedia "
                       f"Commons ({r['page']})",
    }


def cmd_build(only):
    from PIL import Image
    poses = {p["id"]: p for p in load_poses()}
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    LM_DIR.mkdir(parents=True, exist_ok=True)
    det = Detector()
    credits = json.loads(CREDITS.read_text()) if CREDITS.exists() else []
    by_pose = {c["pose_id"]: c for c in credits if isinstance(c, dict) and "pose_id" in c}
    for p in PICKS:
        if only and p["pose"] not in only:
            continue
        pose = poses[p["pose"]]
        img_path, info = source_info(p)
        src = Image.open(img_path).convert("RGB")
        W, H = src.size
        rec = det.detect(src, "source", str(img_path))
        res = sc.score_image(rec, [pose])
        if not res:
            raise SystemExit(f"{p['pose']}: no person found in {img_path}")
        r = res[0]
        mirror = r["mirror_photo"] if p.get("mirror") is None else p["mirror"]
        if p.get("crop"):
            c = p["crop"]
            box = [round(c[0] * W), round(c[1] * H), round(c[2] * W), round(c[3] * H)]
        else:
            box = fit_crop(r["crop_px"], person_extent(rec), W, H)
        out = src.crop(tuple(box))
        if mirror:
            out = out.transpose(Image.FLIP_LEFT_RIGHT)
        s = LONG_SIDE / max(out.size)
        out = out.resize((round(out.width * s), round(out.height * s)), resample=Image.LANCZOS)
        clean = Image.new("RGB", out.size)
        clean.paste(out)
        dest = OUT_DIR / f"{p['pose']}.webp"
        clean.save(dest, "WEBP", quality=QUALITY, method=6)
        # detection and score on the file as shipped
        saved = Image.open(dest)
        rel = str(dest.relative_to(ROOT))
        lm = det.detect(saved, f"pose_photo:{p['pose']}", rel)
        lm.update({"model": "pose_landmarker_full.task", "mode": "IMAGE", "num_poses": 4})
        (LM_DIR / f"{p['pose']}.json").write_text(json.dumps(lm, separators=(",", ":")) + "\n")
        fin = sc.score_image(lm, [pose])
        f = fin[0] if fin else {}
        changes = [f"still frame at {info['frame_time_s']} s of the {info['video_quality']} download"] \
            if p["src"] == "mixkit" else []
        changes.append(f"cropped to [{box[0]}, {box[1]}, {box[2]}, {box[3]}] px of the {W}x{H} source (3:4)")
        if mirror:
            changes.append("mirrored horizontally")
        changes.append(f"resized to {saved.width}x{saved.height}, WebP quality {QUALITY}, metadata stripped")
        by_pose[p["pose"]] = {
            "pose_id": p["pose"], "pose_name": pose["name"], "category": pose["category"],
            "photo": rel, "bytes": dest.stat().st_size,
            **info,
            "changes": changes, "mirrored": bool(mirror), "crop_px": box, "source_size": [W, H],
            "app_matcher_score": f.get("score"),
            "score_detail": {"score_source_frame": r["score"], "score_shipped_file": f.get("score"),
                             "shipped_matches_flipped_pose": f.get("mirror_photo"), "fit": f.get("fit"),
                             "tcov": f.get("tcov"), "people": f.get("people"), "framing": f.get("framing"),
                             "scorer": "tools/pose-photos/posematch.py (Python port of pose-core PoseMatcher)"},
            "reason": p["reason"],
        }
        print(f"{p['pose']}: {dest.stat().st_size // 1024} KB {saved.size} mirror={mirror} crop={box} "
              f"score src {r['score']} -> shipped {f.get('score')} (flipped={f.get('mirror_photo')})", flush=True)
    for pid, why in GAPS.items():
        by_pose[pid] = {"pose_id": pid, "pose_name": poses[pid]["name"], "category": poses[pid]["category"],
                        "photo": None, "reason": why}
    cat_order = {"WALKING": 0, "SITTING": 1, "FLOOR": 2}
    ids = [q["id"] for q in load_poses()]
    out_list = sorted(by_pose.values(), key=lambda c: (cat_order.get(c["category"], 9), ids.index(c["pose_id"])))
    CREDITS.write_text(json.dumps(out_list, indent=1, ensure_ascii=False) + "\n")
    det.lm.close()
    print("credits:", CREDITS, flush=True)


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("frame")
    a.add_argument("clip")
    a.add_argument("times")
    a.add_argument("--box")
    b = sub.add_parser("build")
    b.add_argument("--poses")
    args = ap.parse_args()
    if args.cmd == "frame":
        cmd_frame(args.clip, [float(t) for t in args.times.split(",")],
                  [float(v) for v in args.box.split(",")] if args.box else None)
    else:
        cmd_build(set(args.poses.split(",")) if args.poses else None)


if __name__ == "__main__":
    main()
