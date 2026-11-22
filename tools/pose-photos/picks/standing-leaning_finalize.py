"""standing-leaning_finalize.py: make one pose photo (standing/leaning/professional group).
  standing-leaning_finalize.py POSE --hires CLIP_MS [--mirror auto|yes|no] [--crop person|template|x0,y0,x1,y1]
                               [--pad 0.06] [--zoom 1.0] [--cx 0] [--cy 0] [--quality 78] [--dry]
  standing-leaning_finalize.py POSE --commons PAGEID ...
--hires reads a full-resolution frame saved by standing-leaning_extract.py (WORK/sl/hires/CLIP_MS.jpg + .json: the
licensed 1080p download, sharpest frame near the hold); --commons reads WORK/commons/img/PAGEID.jpg. Crop 3:4 around the
person (or the pose's own frame placed by the matcher with --crop template), mirror when the matcher's best match was the
flipped pose (--mirror auto), 675x900 Lanczos, WebP without metadata -> app/src/main/assets/pose_photos/POSE.webp. Then
the full model (IMAGE mode, 4 people) on the shipped file -> tools/pose-photos/landmarks/POSE.json, and the app-matcher
score of the source frame and of the shipped file (score.score_image) -> WORK/sl/final/POSE.json."""
import argparse, json, math, re, subprocess, sys, time, urllib.request
from pathlib import Path
sys.path.insert(0, "/home/user/poseit-photos/tools/pose-photos")
import score as sc
from common import MODELS, load_poses, read_json, write_json
import cv2, numpy as np
from PIL import Image as PImage
from mediapipe import Image, ImageFormat
from mediapipe.tasks.python import BaseOptions
from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode

REPO = Path("/home/user/poseit-photos"); W = Path("/home/user/poseit-photos-work"); SL = W / "sl"
UA = "Mozilla/5.0 (X11; Linux x86_64) PoseitPhotoHarvest/0.1"
ap = argparse.ArgumentParser()
ap.add_argument("pose"); ap.add_argument("--mixkit", type=int); ap.add_argument("--hires"); ap.add_argument("--commons"); ap.add_argument("--t", type=float)
ap.add_argument("--mirror", default="auto"); ap.add_argument("--crop", default="person"); ap.add_argument("--pad", type=float, default=0.06)
ap.add_argument("--person", type=int, default=0, help="index of the person (largest first) to frame")
ap.add_argument("--window", type=float, default=0.3); ap.add_argument("--dry", action="store_true")
ap.add_argument("--quality", type=int, default=78); ap.add_argument("--cx", type=float, default=0.0, help="shift crop x by this share of its width")
ap.add_argument("--cy", type=float, default=0.0); ap.add_argument("--zoom", type=float, default=1.0)
a = ap.parse_args()
pose = next(p for p in load_poses() if p["id"] == a.pose)
opts = PoseLandmarkerOptions(base_options=BaseOptions(model_asset_path=str(MODELS / "pose_landmarker_full.task")),
    running_mode=RunningMode.IMAGE, num_poses=4, min_pose_detection_confidence=0.5, min_pose_presence_confidence=0.5,
    min_tracking_confidence=0.5)
LM = PoseLandmarker.create_from_options(opts)

def detect(bgr):
    h, w = bgr.shape[:2]; s = 1280 / max(h, w)
    small = cv2.resize(bgr, (round(w * s), round(h * s)), interpolation=cv2.INTER_AREA) if s < 1 else bgr
    rgb = np.ascontiguousarray(cv2.cvtColor(small, cv2.COLOR_BGR2RGB))
    res = LM.detect(Image(image_format=ImageFormat.SRGB, data=rgb))
    ppl = [[[round(l.x, 5), round(l.y, 5), round(l.z, 5), round(l.visibility if l.visibility is not None else 1.0, 4),
             round(l.presence if l.presence is not None else 1.0, 4)] for l in p] for p in res.pose_landmarks]
    ppl.sort(key=lambda q: -bbox_area(q))
    return ppl

def bbox_area(p, vis=0.5):
    pts = [(l[0], l[1]) for l in p if l[3] >= vis]
    if len(pts) < 2: return 0.0
    xs, ys = zip(*pts); return (max(xs) - min(xs)) * (max(ys) - min(ys))

def fetch(url, dest):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=300) as r, open(dest, "wb") as f:
        while True:
            b = r.read(1 << 20)
            if not b: break
            f.write(b)

src = {}
if a.hires:
    hj = read_json(SL / "hires" / f"{a.hires}.json"); a.mixkit = hj["clip"]
    frame = cv2.imread(str(SL / "hires" / f"{a.hires}.jpg"))
    src.update({k: hj[k] for k in ("download_label", "download_page", "file_url", "video_size", "fps", "frame_index", "time_s",
                                    "requested_t", "laplacian_var", "frames_compared")})
else:
    frame = cv2.imread(str(W / "commons" / "img" / f"{a.commons}.jpg"))
    src.update({"image": f"commons/img/{a.commons}.jpg", "image_size": [frame.shape[1], frame.shape[0]]})
H, Wd = frame.shape[:2]
ppl = detect(frame)
assert ppl, "nobody detected"
rec = {"id": "src", "w": Wd, "h": H, "people": ppl}
scored = [s for s in sc.score_image(rec, [pose]) if s["pose"] == a.pose]
s0 = scored[0] if scored else None
mirror = (s0["mirror_photo"] if s0 else False) if a.mirror == "auto" else a.mirror == "yes"

def person_box(p):
    P = lambda i: (p[i][0] * Wd, p[i][1] * H, p[i][3])
    pts = [(x, y) for x, y, v in (P(i) for i in range(33)) if v >= 0.35]
    xs, ys = [q[0] for q in pts], [q[1] for q in pts]
    nose = P(0); ls, rs = P(11), P(12)
    sh_y = (ls[1] + rs[1]) / 2
    head_top = nose[1] - 0.95 * max(10, sh_y - nose[1])
    top = min(min(ys), head_top)
    feet = [P(i) for i in (27, 28, 29, 30, 31, 32)]
    bottom = max(max(ys), max((y for x, y, v in feet if v >= 0.35), default=max(ys)))
    return min(xs), top, max(xs), bottom

if re.match(r"^-?\d+,-?\d+,-?\d+,-?\d+$", a.crop):
    x0, y0, x1, y1 = [float(v) for v in a.crop.split(",")]
elif a.crop == "template" and s0:
    x0, y0, x1, y1 = s0["crop_px"]
else:
    bl, bt, br, bb = person_box(ppl[a.person])
    if a.person == -1 or len(ppl) > 1 and a.crop == "people":
        boxes = [person_box(q) for q in ppl]
        bl, bt, br, bb = min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes)
    bh = bb - bt
    hh = bh * (1 + 2 * a.pad); ww = (br - bl) + 2 * a.pad * bh
    hh = max(hh, ww / 0.75)
    cx, cy = (bl + br) / 2, (bt + bb) / 2
    x0, x1, y0, y1 = cx - 0.375 * hh, cx + 0.375 * hh, cy - hh / 2, cy + hh / 2
# zoom/shift, keep 3:4
cw, ch = (x1 - x0), (y1 - y0)
ch = ch / a.zoom; cw = 0.75 * ch
cx, cy = (x0 + x1) / 2 + a.cx * cw, (y0 + y1) / 2 + a.cy * ch
if ch > H: ch = H; cw = 0.75 * ch
if cw > Wd: cw = Wd; ch = cw / 0.75
x0 = min(max(cx - cw / 2, 0), Wd - cw); y0 = min(max(cy - ch / 2, 0), H - ch)
x0, y0 = int(round(x0)), int(round(y0)); x1, y1 = int(round(x0 + cw)), int(round(y0 + ch))
crop = cv2.cvtColor(frame[y0:y1, x0:x1], cv2.COLOR_BGR2RGB)
im = PImage.fromarray(crop)
if mirror: im = im.transpose(PImage.FLIP_LEFT_RIGHT)
im = im.resize((675, 900), PImage.LANCZOS)
out = (SL / "final" / f"{a.pose}.webp") if a.dry else (REPO / "app/src/main/assets/pose_photos" / f"{a.pose}.webp")
out.parent.mkdir(parents=True, exist_ok=True)
im.save(out, "WEBP", quality=a.quality, method=6)   # PIL writes no EXIF/ICC/XMP unless given
# detect + score the saved file
saved = cv2.imread(str(out))
ppl2 = detect(saved)
rec2 = {"id": f"photo:{a.pose}", "w": saved.shape[1], "h": saved.shape[0], "people": ppl2}
sc2 = [s for s in sc.score_image(rec2, [pose]) if s["pose"] == a.pose] if ppl2 else []
s2 = sc2[0] if sc2 else None
lmk = {"id": f"photo:{a.pose}", "image": f"app/src/main/assets/pose_photos/{a.pose}.webp", "w": saved.shape[1],
       "h": saved.shape[0], "model": "pose_landmarker_full.task, IMAGE mode, num_poses 4, confidences 0.5 (detect_images.py settings)",
       "landmark_format": "[x, y, z, visibility, presence] x 33 per person (MediaPipe order), x and y normalized to the image; largest person first",
       "people": ppl2}
if not a.dry:
    write_json(REPO / "tools/pose-photos/landmarks" / f"{a.pose}.json", lmk)
res = {"pose": a.pose, "source": "Mixkit" if a.mixkit else "Wikimedia Commons", "clip": a.mixkit, "commons": a.commons, **src,
       "crop_px": [x0, y0, x1, y1], "crop_size": [x1 - x0, y1 - y0], "mirrored": mirror, "out": str(out), "bytes": out.stat().st_size,
       "source_frame_score": s0 and {k: s0[k] for k in ("score", "rank", "mirror_photo", "tcov", "fit", "people", "eligible")},
       "photo_score": s2 and {k: s2[k] for k in ("score", "rank", "mirror_photo", "tcov", "fit", "people", "eligible", "framing")},
       "people_in_photo": len(ppl2)}
write_json(SL / "final" / f"{a.pose}{'_dry' if a.dry else ''}.json", res, indent=1)
print(json.dumps(res, indent=1))
