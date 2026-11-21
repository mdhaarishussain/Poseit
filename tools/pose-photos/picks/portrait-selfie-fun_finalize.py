#!/usr/bin/env python3
"""
portrait-selfie-fun_finalize.py: turn the chosen candidates of the PORTRAIT, SELFIE and FUN poses into the app's sample
photos, landmarks and credits. Same pipeline as sitting-floor_finalize.py (whose Mixkit frame step and detector
settings it reuses), with three differences: its own download folder (WORK/picks_dl/psf), a Commons download step, and
an upper-body person extent for waist-up poses (so the crop keeps the pose's own framing instead of growing to the
whole body).

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/portrait-selfie-fun_finalize.py fetch [--poses id,id]
    $PY -I tools/pose-photos/picks/portrait-selfie-fun_finalize.py build [--poses id,id]

fetch  for every pick: a Mixkit pick downloads the clip's licensed 1080p file (720p as fallback; link read from the
       clip's download page), extracts every frame within 0.3 s of the hold time and keeps the sharpest by Laplacian
       variance measured on the person box (WORK/picks_dl/psf/<clip>_<ms>.png + .json note); the video is deleted
       right after. A Commons pick downloads a standard-width rendition (default 1920 px) from upload.wikimedia.org to
       WORK/picks_dl/psf/commons/<pageid>_<width>.jpg.
build  detects the person on the full-resolution source (the app's full model, IMAGE mode, up to 4 people), places
       the pose's guide with score.py's drag-and-pinch solve, takes the pose's own 3:4 frame as the crop, grows it to
       hold the person (the whole body for full-body poses, head to hips for waist-up ones) with a small margin, keeps
       it inside the photo (or uses a manual crop); mirrors when the photo matches the flipped pose (or as set);
       resizes to 900 px on the long side; saves WebP quality 78 without metadata to
       app/src/main/assets/pose_photos/<poseId>.webp; runs the same detection on the saved file
       (tools/pose-photos/landmarks/<poseId>.json), scores it with posematch (Python port of PoseMatcher) and writes
       the credits to tools/pose-photos/picks/portrait-selfie-fun.json (gaps included, with the reason).
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import time
import urllib.parse
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
CREDITS = HERE / "portrait-selfie-fun.json"
DL = WORK / "picks_dl" / "psf"
UA_WEB = "Mozilla/5.0 (X11; Linux x86_64) pose-photos-review"
UA_COMMONS = "PoseitPhotoHarvest/0.1 (pose-guide app sample-photo search; python-urllib)"
LONG_SIDE, QUALITY = 900, 78
CHECKED = "2026-11-21"
MAX_BYTES = 100 * 1024

# The picks, chosen by looking at the contact sheets, the clips' other frames and the full-resolution sources.
#   src "mixkit": clip + hold (the candidate frame's time in the 360p preview); src "commons": pageid (+ width of the
#   rendition). Optional: "crop" [x0, y0, x1, y1] as fractions of the source (overrides the automatic crop), "mirror"
#   (overrides the matcher's choice), "extent" "upper" | "full" (person extent used to grow the crop).
PICKS = [
    # ---------------------------------------------------------------- PORTRAIT
    {"pose": "port_chin_rest", "src": "mixkit", "clip": 101458, "hold": 0.0, "crop": [0.15, 0.13, 0.857, 0.66],
     "reason": "Knuckles resting under the chin, head tilted, eyes on the lens: the thoughtful chin rest read at a "
               "glance. She is seated (cropped waist-up) and the supporting arm rests low instead of folding across the "
               "waist, which is why the matcher scores it low."},
    {"pose": "port_face_frame", "src": "mixkit", "clip": 100676, "hold": 0.0, "crop": [0.0, 0.10, 1.0, 0.85],
     "reason": "Both hands raised to frame the face, smiling at the lens, waist-up: the clearest face frame among the "
               "candidates (hands sit at the temples rather than the jaw; editorial styling with a fruit garland)."},
    {"pose": "port_hand_in_hair", "src": "commons", "pageid": 91140061, "width": 3840,
     "crop": [0.44, 0.04, 0.727, 0.58],
     "reason": "One arm raised with the fingers in the hair at the crown, elbow out to the side, the other arm low on a "
               "railing, looking at the lens: the drawn pose, sharp and evenly lit."},
    {"pose": "port_over_shoulder", "src": "mixkit", "clip": 50647, "hold": 5.339, "crop": [0.339, 0.26, 0.651, 1.0],
     "reason": "Body turned away from the camera, head turned back over the shoulder to look into the lens: the "
               "shoulder glance (the frame at 6.3 s scores higher but shows only her profile)."},
    {"pose": "port_arms_crossed_smile", "src": "mixkit", "clip": 52269, "hold": 7.341, "crop": [0.445, 0.27, 0.753, 1.0],
     "reason": "Arms folded across the body, shoulders angled, a soft closed-mouth smile, plain red backdrop: the "
               "crossed-arm smile (fashion styling with a sheer beaded veil). Preferred over the higher-scoring "
               "frame of Mixkit 50649, where a lace bra shows under the open blazer."},
    {"pose": "port_dreamy_gaze", "src": "mixkit", "clip": 43391, "hold": 1.001, "extent": "upper",
     "reason": "Holding a warm mug at chest height and smiling off into the distance: the dreamy gaze as described "
               "(one hand on the mug rather than two)."},
    {"pose": "port_collar_adjust", "src": "mixkit", "clip": 435, "hold": 7.667, "crop": [0.466, 0.06, 0.745, 0.72],
     "reason": "Fingers pinching the neckerchief at the collar with the elbow out, the other hand low at the pocket, "
               "chin down and eyes on the lens: the collar adjust."},
    {"pose": "port_shoulder_touch", "src": "commons", "pageid": 63315555, "width": 3840,
     "crop": [0.34, 0.08, 0.56, 0.52],
     "reason": "Hand resting high on the chest at the opposite collarbone, head tilted, looking at the lens: the soft "
               "shoulder touch (the hand sits a little lower than drawn)."},
    # ---------------------------------------------------------------- SELFIE
    {"pose": "selfie_classic", "src": "mixkit", "clip": 51119, "hold": 2.667, "crop": [0.24, 0.0, 0.661875, 1.0],
     "reason": "Phone held out at arm's length a little above eye level, shoulders turned toward it, looking at the "
               "phone: the everyday arm selfie seen from the side. The matcher scores it low because the photo shows "
               "the selfie being taken, not the front camera's view the figure is drawn for."},
    {"pose": "selfie_peace", "src": "mixkit", "clip": 51119, "hold": 8.0, "mirror": False,
     "crop": [0.41, 0.0, 0.831875, 1.0],
     "reason": "Phone at arm's length in one hand and a V sign beside the face with the other, big smile, head tilted "
               "toward the V: the peace-sign selfie (seen from beside the phone, hence the low matcher score)."},
    {"pose": "selfie_high_angle", "src": "commons", "pageid": 196293216, "width": 1920, "extent": "upper",
     "reason": "Phone raised high above the head and angled down, looking up into it with a smile: the high-angle "
               "selfie. The focus is on the phone and hand, so the face is soft."},
    {"pose": "selfie_mirror", "src": "commons", "pageid": 174758407, "width": 1920, "extent": "full",
     "reason": "Full-length mirror selfie in a hotel corridor, phone held up at face height with the face still "
               "visible: the outfit-of-the-day mirror shot (his free hand holds a hat at his side instead of resting "
               "on the hip)."},
    {"pose": "selfie_mirror_two_hands", "src": "commons", "pageid": 145072603, "width": 1920, "extent": "full",
     "reason": "Full-length mirror selfie with the phone held in both hands in front of the chest and face, elbows "
               "close to the body: the two-hand mirror shot (standing square rather than with a popped knee)."},
    # ---------------------------------------------------------------- FUN
    {"pose": "fun_superhero", "src": "commons", "pageid": 88226302, "width": 1920, "crop": [0.12, 0.22, 0.705, 1.0],
     "reason": "Hands on hips with the elbows out, feet apart, chin up and gaze to the sky: the superhero stance "
               "(he is looking at an eclipse through viewing glasses)."},
    {"pose": "fun_disco_point", "src": "mixkit", "clip": 42306, "hold": 2.002, "extent": "full",
     "reason": "One arm pointing high to the sky, the other hand on the hip, hip popped with one knee bent in, "
               "smiling: the disco point, full length on a playful set."},
    {"pose": "fun_victory", "src": "mixkit", "clip": 51746, "hold": 9.343, "extent": "full",
     "reason": "Both arms up with bent elbows and fists raised in a goal-post shape, one knee lifted, cheering: the "
               "victory pose mid-dance."},
]

# Poses left without a photo: no candidate a person would recognise as the pose, or only ones that break a rule.
GAPS = {
    "selfie_hand_on_cheek": "No usable cheek-rest selfie. Mixkit 43187 (8.0 s) has a hand at the side of the head but "
                            "the phone sits at the far edge of the frame, so a 3:4 crop shows no selfie; Mixkit 273 "
                            "holds a phone with the hand on the chin (a thinking gesture), and phone and face do not "
                            "fit one 3:4 crop.",
    "port_chin_on_hand": "No one leaning forward over a table with the chin on the knuckles. Nearest: Mixkit 101458 "
                         "(used for the chin rest, seated on the floor, no table) and Mixkit 8744 (cheek propped on a "
                         "hand on a sofa, in a clip titled 'upset and frustrated'); neither reads as the cafe pose.",
    "selfie_look_up": "No selfie looking up past the phone to the sky; the high scorers are mirror selfies, dancers and "
                      "people looking at their phones.",
    "selfie_lean_in": "No one leaning the upper body toward a phone with a hand on the hip; the best-scoring frame "
                      "(Mixkit 43595 at 9.0 s) is two women, one taking a selfie with the other at her shoulder.",
    "fun_star_jump": "No mid-air star jump. The only jump photo (Commons 'Jumping in the air, Flash jump shot') is a "
                     "group of youths that may include minors; the rest stand with arms out.",
    "fun_tuck_jump": "No tuck jump in any candidate; best scores (0.46) are dancers and a hula-hoop performer standing.",
    "fun_twirl": "Near miss only: Mixkit 43219 (contemporary dancer spinning in a long dress, one arm up) is backlit, "
                 "small and seen from behind, without the heel flick; the ballet frames (Mixkit 40162) are an "
                 "en-pointe stage pose with heavy stage make-up. Left empty.",
    "fun_laugh_bend": "No one doubled over laughing with a hand on the knee; best score 0.46 (a seated couple).",
    "fun_high_kick": "No front kick; best score 0.44 (an arms-out standing pose and dancers).",
    "fun_peace_lean": "No double peace sign; the leaning candidates are dancers and a dark silhouette.",
}


# ------------------------------------------------------------------------------------------------ downloads
def http_get(url, dest=None, ua=UA_WEB):
    req = urllib.request.Request(url, headers={"User-Agent": ua})
    with urllib.request.urlopen(req, timeout=300) as r:
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


def preview_person_box(clip, hold):
    """Box of the largest person in the 360p preview frame the candidate came from, as fractions of the frame."""
    key = f"mixkit:{clip}_{round(hold * 1000):06d}"
    with open(WORK / "landmarks" / "mixkit.jsonl") as f:
        for line in f:
            if line.startswith('{"id": "' + key + '"'):
                rec = json.loads(line)
                if not rec["people"]:
                    return None
                pts = [(l[0], l[1]) for l in rec["people"][0] if l[3] >= 0.5]
                xs, ys = zip(*pts)
                m = 0.05
                return [max(0, min(xs) - m), max(0, min(ys) - m), min(1, max(xs) + m), min(1, max(ys) + m)]
    return None


def mixkit_frame(clip, hold):
    import cv2
    DL.mkdir(parents=True, exist_ok=True)
    out = DL / f"{clip}_{round(hold * 1000):06d}.png"
    if out.exists() and out.with_suffix(".json").exists():
        return out
    box = preview_person_box(clip, hold)
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
        tmp = DL / f"tmp_{clip}"
        shutil.rmtree(tmp, ignore_errors=True)
        tmp.mkdir()
        start = max(0.0, hold - 0.3)
        subprocess.run(["ffmpeg", "-v", "error", "-ss", f"{start:.3f}", "-i", str(mp4), "-t", "0.6", "-vsync", "0",
                        str(tmp / "f_%04d.png")], check=True)
        best = None
        for k, f in enumerate(sorted(tmp.glob("f_*.png"))):
            g = cv2.cvtColor(cv2.imread(str(f)), cv2.COLOR_BGR2GRAY)
            if box:
                g = g[int(box[1] * H):int(box[3] * H), int(box[0] * W):int(box[2] * W)]
            v = float(cv2.Laplacian(g, cv2.CV_64F).var())
            if best is None or v > best[0]:
                best = (v, k, f)
        shutil.copy(best[2], out)
        note = {"clip": int(clip), "hold_t": hold, "frame_t": round(start + best[1] / fps, 3),
                "lapvar": round(best[0], 1), "n_frames": len(list(tmp.glob('f_*.png'))), "width": W, "height": H,
                "sharpness_box": box, "download_page": page, "video_url": url, "quality": q}
        out.with_suffix(".json").write_text(json.dumps(note, indent=1))
        print(out, note, flush=True)
        shutil.rmtree(tmp)
    finally:
        mp4.unlink(missing_ok=True)
    return out


def commons_rendition(pageid, width):
    files = json.loads((WORK / "commons" / "files.json").read_text())
    r = files[str(pageid)]
    orig = r["file_url"].split("?")[0]
    if width >= r["width"]:
        url, width = orig, r["width"]
    else:
        path = orig.split("/wikipedia/commons/", 1)[1]          # e.g. 5/51/Name.jpg
        name = path.rsplit("/", 1)[1]
        url = f"https://thumb.wikimedia.org/wikipedia/commons/thumb/{path}/{width}px-{name}"
    return r, url, width


def commons_fetch(pageid, width):
    d = DL / "commons"
    d.mkdir(parents=True, exist_ok=True)
    r, url, w = commons_rendition(pageid, width)
    out = d / f"{pageid}_{w}.jpg"
    if not out.exists():
        from common import Polite  # serial client: per-host gaps, Retry-After honoured on 429/503
        Polite(gaps={"thumb.wikimedia.org": 3.0, "upload.wikimedia.org": 3.0}).download(url, out, timeout=180)
        print(out, url, flush=True)
    return out, url


def cmd_fetch(only):
    for p in PICKS:
        if only and p["pose"] not in only:
            continue
        if p["src"] == "mixkit":
            mixkit_frame(p["clip"], p["hold"])
            time.sleep(3)
        else:
            commons_fetch(p["pageid"], p.get("width", 1920))


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


UPPER = range(0, 25)   # MediaPipe face, shoulders, arms, hands and hips: head to waist


def person_extent(rec, which="full"):
    """Box of the largest person's visible MediaPipe landmarks (all 33, or head to hips), in pixels."""
    if not rec["people"]:
        return None
    ids = range(33) if which == "full" else UPPER
    pts = [(rec["people"][0][i][0], rec["people"][0][i][1]) for i in ids if rec["people"][0][i][3] >= 0.5]
    if len(pts) < 2:
        return None
    xs, ys = zip(*pts)
    return [min(xs) * rec["w"], min(ys) * rec["h"], max(xs) * rec["w"], max(ys) * rec["h"]]


def fit_crop(crop, person, W, H, aspect=0.75, full=True):
    """Grow the pose frame to hold the person (+ margin: more above for the head; below the feet only for full-body
    poses), keep 3:4, centre on the person horizontally, then shrink/move it inside the photo."""
    x0, y0, x1, y1 = crop
    if person:
        px0, py0, px1, py1 = person
        ph, pw = py1 - py0, px1 - px0
        mx = 0.06 * max(pw, ph * 0.3)
        need = [px0 - mx, py0 - (0.10 if full else 0.16) * ph, px1 + mx, py1 + (0.05 if full else 0.10) * ph]
        x0, y0, x1, y1 = min(x0, need[0]), min(y0, need[1]), max(x1, need[2]), max(y1, need[3])
        half = max((x1 - x0) / 2, pw / 2 + mx)
        x0, x1 = (px0 + px1) / 2 - half, (px0 + px1) / 2 + half
    w, h = x1 - x0, y1 - y0
    if w / h > aspect:
        cy, h = (y0 + y1) / 2, w / aspect
        y0, y1 = cy - h / 2, cy + h / 2
    else:
        cx, w = (x0 + x1) / 2, h * aspect
        x0, x1 = cx - w / 2, cx + w / 2
    s = min(1.0, W / w, H / h)
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
        img = DL / f"{p['clip']}_{round(p['hold'] * 1000):06d}.png"
        note = json.loads(img.with_suffix(".json").read_text())
        clip = json.loads((WORK / "mixkit" / "clips" / f"{p['clip']}.json").read_text())
        return img, {
            "source": "Mixkit", "title": clip["title"], "page_url": clip["page"],
            "file_url": note["download_page"], "video_file": note["video_url"], "video_quality": note["quality"],
            "frame_time_s": note["frame_t"], "hold_time_s_in_preview": note["hold_t"],
            "author": "Mixkit (no author named on the clip page)",
            "licence": {"name": clip["licence"]["panel_licence"], "url": "https://mixkit.co/license/#videoFree",
                        "stated_on_page": clip["licence"]["stated_on_page"], "checked": CHECKED},
            "attribution": f"Video still: Mixkit, Mixkit Stock Video Free License ({clip['page']})",
        }
    r, url, w = commons_rendition(p["pageid"], p.get("width", 1920))
    lic = r["licence"]
    img = DL / "commons" / f"{p['pageid']}_{w}.jpg"
    artist = lic.get("artist") or "unknown"
    share_alike = "SA" in (lic.get("short_name") or "")
    return img, {
        "source": "Wikimedia Commons", "title": r["title"], "page_url": r["page"],
        "file_url": r["file_url"].split("?")[0], "rendition_used": url,
        "author": artist, "credit_line": lic.get("credit"),
        "licence": {"name": lic.get("short_name"), "url": lic.get("url"),
                    "attribution_required": lic.get("attribution_required"), "restrictions": lic.get("restrictions"),
                    "checked": CHECKED},
        "attribution": f"Photo: {artist}, {lic.get('short_name')} ({lic.get('url')}), via Wikimedia Commons "
                       f"({r['page']}); cropped" + ("" if not share_alike else
                                                     f"; this crop is shared under {lic.get('short_name')}"),
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
        full = p.get("extent", "full") == "full"
        if p.get("crop"):
            c = p["crop"]
            box = [round(c[0] * W), round(c[1] * H), round(c[2] * W), round(c[3] * H)]
        else:
            box = fit_crop(r["crop_px"], person_extent(rec, "full" if full else "upper"), W, H, full=full)
        out = src.crop(tuple(box))
        if mirror:
            out = out.transpose(Image.FLIP_LEFT_RIGHT)
        s = LONG_SIDE / max(out.size)
        out = out.resize((round(out.width * s), round(out.height * s)), resample=Image.LANCZOS)
        clean = Image.new("RGB", out.size)
        clean.paste(out)
        dest = OUT_DIR / f"{p['pose']}.webp"
        q = QUALITY
        clean.save(dest, "WEBP", quality=q, method=6)
        saved = Image.open(dest)
        rel = str(dest.relative_to(ROOT))
        lm = det.detect(saved, f"pose_photo:{p['pose']}", rel)
        lm.update({"model": "pose_landmarker_full.task", "mode": "IMAGE", "num_poses": 4})
        (LM_DIR / f"{p['pose']}.json").write_text(json.dumps(lm, separators=(",", ":")) + "\n")
        fin = sc.score_image(lm, [pose])
        f = fin[0] if fin else {}
        changes = [f"still frame at {info['frame_time_s']} s of the {info['video_quality']} download"] \
            if p["src"] == "mixkit" else [f"{W}x{H} rendition of the original"]
        changes.append(f"cropped to [{box[0]}, {box[1]}, {box[2]}, {box[3]}] px of the {W}x{H} source (3:4)")
        if mirror:
            changes.append("mirrored horizontally")
        changes.append(f"resized to {saved.width}x{saved.height}, WebP quality {q}, metadata stripped")
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
        flag = "" if dest.stat().st_size <= MAX_BYTES else "  OVER 100 KB"
        print(f"{p['pose']}: {dest.stat().st_size // 1024} KB {saved.size} mirror={mirror} crop={box} "
              f"score src {r['score']} -> shipped {f.get('score')} (flipped={f.get('mirror_photo')}){flag}", flush=True)
    for pid, why in GAPS.items():
        by_pose[pid] = {"pose_id": pid, "pose_name": poses[pid]["name"], "category": poses[pid]["category"],
                        "photo": None, "reason": why}
    picked = {p["pose"] for p in PICKS}
    for pid in list(by_pose):
        if pid not in picked and pid not in GAPS:
            del by_pose[pid]
    cat_order = {"PORTRAIT": 0, "SELFIE": 1, "FUN": 2}
    ids = [q["id"] for q in load_poses()]
    out_list = sorted(by_pose.values(), key=lambda c: (cat_order.get(c["category"], 9), ids.index(c["pose_id"])))
    CREDITS.write_text(json.dumps(out_list, indent=1, ensure_ascii=False) + "\n")
    det.lm.close()
    print("credits:", CREDITS, flush=True)


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name in ("fetch", "build"):
        a = sub.add_parser(name)
        a.add_argument("--poses")
    args = ap.parse_args()
    only = set(args.poses.split(",")) if args.poses else None
    cmd_fetch(only) if args.cmd == "fetch" else cmd_build(only)


if __name__ == "__main__":
    main()
