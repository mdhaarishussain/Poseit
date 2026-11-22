"""standing-leaning_extract.py CLIP T [T ...]: download the clip's licensed 1080p (else 720p) file from its Mixkit download page, save
the sharpest frame (Laplacian variance on the main person's box) within +-0.3 s of each T as a full-resolution JPEG
(quality 95) in WORK/sl/hires/<clip>_<ms>.jpg with a JSON record next to it, then delete the video."""
import json, math, re, sys, time, urllib.request
from pathlib import Path
sys.path.insert(0, "/home/user/poseit-photos/tools/pose-photos")
from common import MODELS, read_json, write_json
import cv2, numpy as np
from mediapipe import Image, ImageFormat
from mediapipe.tasks.python import BaseOptions
from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode
W = Path("/home/user/poseit-photos-work"); SL = W / "sl"; HI = SL / "hires"; HI.mkdir(exist_ok=True)
UA = "Mozilla/5.0 (X11; Linux x86_64) PoseitPhotoHarvest/0.1"
cid = int(sys.argv[1]); times = [float(t) for t in sys.argv[2:]]
clip = read_json(W / "mixkit" / "clips" / f"{cid}.json")
if not (clip and clip.get("licence", {}).get("ok")):
    clip = (read_json(SL / "scan.json", {}) or {}).get(str(cid))
assert clip and clip["licence"]["ok"] and clip["licence"]["panel_licence"] == "Mixkit Stock Video Free License", "licence"
dls = {d["label"]: d["url"] for d in clip["licence"].get("downloads", [])}
vid = mp4 = label = None
for label in ("Full HD", "HD Ready"):
    if label not in dls: continue
    page = urllib.request.urlopen(urllib.request.Request(dls[label], headers={"User-Agent": UA}), timeout=60).read().decode("utf-8", "replace")
    m = re.findall(r'https://assets\.mixkit\.co/videos/[^"\']+\.mp4', page)
    if m: mp4 = m[0]; break
assert mp4, "no file link on the download page"
time.sleep(2)
vid = SL / "dl" / f"{cid}.mp4"
with urllib.request.urlopen(urllib.request.Request(mp4, headers={"User-Agent": UA}), timeout=600) as r, open(vid, "wb") as f:
    while True:
        b = r.read(1 << 20)
        if not b: break
        f.write(b)
opts = PoseLandmarkerOptions(base_options=BaseOptions(model_asset_path=str(MODELS / "pose_landmarker_full.task")),
    running_mode=RunningMode.IMAGE, num_poses=4, min_pose_detection_confidence=0.5, min_pose_presence_confidence=0.5, min_tracking_confidence=0.5)
lm = PoseLandmarker.create_from_options(opts)
cap = cv2.VideoCapture(str(vid)); fps = cap.get(cv2.CAP_PROP_FPS); vw = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)); vh = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
nfr = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
for T in times:
    f0 = max(0, int(math.floor((T - 0.3) * fps))); f1 = min(nfr - 1, int(math.ceil((T + 0.3) * fps)))
    cap.set(cv2.CAP_PROP_POS_FRAMES, f0); frames = []
    for k in range(f0, f1 + 1):
        ok, fr = cap.read()
        if not ok: break
        frames.append((k, fr))
    mid = frames[len(frames) // 2][1]
    s = 1280 / max(vw, vh); small = cv2.resize(mid, (round(vw * s), round(vh * s)), interpolation=cv2.INTER_AREA)
    res = lm.detect(Image(image_format=ImageFormat.SRGB, data=np.ascontiguousarray(cv2.cvtColor(small, cv2.COLOR_BGR2RGB))))
    box = (0, 0, vw, vh)
    if res.pose_landmarks:
        ppl = sorted(res.pose_landmarks, key=lambda p: -((max(l.x for l in p) - min(l.x for l in p)) * (max(l.y for l in p) - min(l.y for l in p))))
        xs = [l.x * vw for l in ppl[0] if (l.visibility or 0) > 0.5]; ys = [l.y * vh for l in ppl[0] if (l.visibility or 0) > 0.5]
        if xs: box = (int(max(0, min(xs) - 30)), int(max(0, min(ys) - 80)), int(min(vw, max(xs) + 30)), int(min(vh, max(ys) + 30)))
    best = max(((cv2.Laplacian(cv2.cvtColor(fr[box[1]:box[3], box[0]:box[2]], cv2.COLOR_BGR2GRAY), cv2.CV_64F).var(), k, fr) for k, fr in frames), key=lambda x: x[0])
    var, k, fr = best
    name = f"{cid}_{int(round(T * 1000)):06d}"
    cv2.imwrite(str(HI / f"{name}.jpg"), fr, [cv2.IMWRITE_JPEG_QUALITY, 95])
    write_json(HI / f"{name}.json", {"clip": cid, "requested_t": T, "frame_index": k, "time_s": round(k / fps, 3), "fps": round(fps, 3),
        "video_size": [vw, vh], "laplacian_var": round(var, 1), "frames_compared": len(frames), "person_box": box,
        "download_label": label, "download_page": dls[label], "file_url": mp4, "page": clip["page"], "title": clip["title"]}, indent=1)
    print(name, k, round(k / fps, 3), round(var, 1), vw, vh, flush=True)
cap.release(); vid.unlink()
print("video deleted", vid.name)
