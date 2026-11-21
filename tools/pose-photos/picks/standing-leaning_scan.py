"""Targeted dense scan of chosen Mixkit clips for the standing/leaning/professional poses.
usage: standing-leaning_scan.py fps id [id ...]
Licence: clip page must pass mixkit_harvest.check_licence (Free License). Uses the existing harvest clip record when
present (already checked today), else fetches the page. Downloads the 360p preview to WORK/sl/video, samples at fps,
runs the full model (IMAGE mode, 4 people), scores every sample against the 27 poses (score.score_image), keeps the
best 3 samples per pose per clip as frames, deletes the preview."""
import sys, json, time
from pathlib import Path
sys.path.insert(0, "/home/user/poseit-photos/tools/pose-photos")
import mixkit_harvest as mh
import score as sc
from common import MODELS, load_poses, read_json, write_json
import cv2, numpy as np
from mediapipe import Image, ImageFormat
from mediapipe.tasks.python import BaseOptions
from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode

W = Path("/home/user/poseit-photos-work"); OUT = W / "sl"
CATS = {"STANDING", "LEANING", "PROFESSIONAL"}
poses = [p for p in load_poses() if p["category"] in CATS]
fps = float(sys.argv[1]); ids = [int(x) for x in sys.argv[2:]]
items = {**(read_json(W / "mixkit" / "items.json", {}) or {}), **(read_json(OUT / "items.json", {}) or {})}
client = mh.http()
opts = PoseLandmarkerOptions(base_options=BaseOptions(model_asset_path=str(MODELS / "pose_landmarker_full.task")),
    running_mode=RunningMode.IMAGE, num_poses=4, min_pose_detection_confidence=0.5,
    min_pose_presence_confidence=0.5, min_tracking_confidence=0.5)
results = read_json(OUT / "scan.json", {}) or {}
with PoseLandmarker.create_from_options(opts) as lm:
    for cid in ids:
        if str(cid) in results: print(cid, "done"); continue
        rec = read_json(W / "mixkit" / "clips" / f"{cid}.json")
        it = items.get(str(cid))
        if rec and rec.get("licence", {}).get("ok"):
            lic, page, title, desc, preview = rec["licence"], rec["page"], rec["title"], rec.get("description", ""), rec["preview"]
        elif it:
            page, title, desc, preview = it["page"], it["title"], it.get("description", ""), it["preview"]
            html_ = client.get(page).decode("utf-8", "replace")
            lic = mh.check_licence(html_)
        else:
            print(cid, "unknown"); continue
        verdict = mh.rules.people_check(title, desc, [])
        info = {"id": cid, "page": page, "title": title, "description": desc, "preview": preview, "licence": lic,
                "checked": time.strftime("%Y-%m-%d"), "rules": verdict}
        if not lic.get("ok"):
            info["skip"] = "not Free License"; results[str(cid)] = info; print(cid, "licence", lic.get("panel_key")); continue
        vp = OUT / "video" / f"{cid}.mp4"
        try:
            client.download(preview, vp, timeout=120)
        except Exception as e:
            print(cid, "download error", e); continue
        cap = cv2.VideoCapture(str(vp)); vfps = cap.get(cv2.CAP_PROP_FPS) or 25; n = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        step = max(1, round(vfps / fps)); best = {}; k = 0; ns = 0
        while True:
            ok, fr = cap.read()
            if not ok: break
            if k % step == 0:
                h, w = fr.shape[:2]
                rgb = np.ascontiguousarray(cv2.cvtColor(fr, cv2.COLOR_BGR2RGB))
                res = lm.detect(Image(image_format=ImageFormat.SRGB, data=rgb))
                people = [[[round(l.x, 5), round(l.y, 5), round(l.z, 5), round(l.visibility or 0, 4), round(l.presence or 0, 4)]
                           for l in p] for p in res.pose_landmarks]
                ns += 1
                if people:
                    t = k / vfps
                    r = {"id": f"sl:{cid}_{int(round(t*1000)):06d}", "w": w, "h": h, "people": people}
                    for s in sc.score_image(r, poses):
                        lst = best.setdefault(s["pose"], [])
                        lst.append((s["rank"], t, s, r, fr.copy() if len(lst) < 400 else None))
                        lst.sort(key=lambda x: -x[0]); del lst[3:]
            k += 1
        cap.release(); vp.unlink(missing_ok=True)
        info["video"] = {"w": w, "h": h, "fps": vfps, "frames": n, "samples": ns}
        kept = {}
        for pid, lst in best.items():
            kept[pid] = []
            for rank, t, s, r, fr in lst:
                fn = OUT / "frames" / f"{cid}_{int(round(t*1000)):06d}.jpg"
                if fr is not None and not fn.exists(): cv2.imwrite(str(fn), fr, [cv2.IMWRITE_JPEG_QUALITY, 92])
                kept[pid].append({"t": round(t, 3), "file": fn.name, "score": s["score"], "rank": s["rank"], "eligible": s["eligible"],
                                  "mirror": s["mirror_photo"], "people": s["people"], "tcov": s["tcov"], "crop_px": s["crop_px"]})
                with open(OUT / "landmarks.jsonl", "a") as f:
                    f.write(json.dumps({**r, "image": str(fn)}) + "\n")
        info["best"] = kept; results[str(cid)] = info
        write_json(OUT / "scan.json", results, indent=1)
        top = sorted(((v[0]["rank"], p) for p, v in kept.items()), reverse=True)[:4]
        print(cid, title[:50], ns, "samples; top:", [(p, round(r, 2)) for r, p in top], flush=True)
write_json(OUT / "scan.json", results, indent=1)
