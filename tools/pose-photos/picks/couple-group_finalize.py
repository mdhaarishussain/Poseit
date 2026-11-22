#!/usr/bin/env python3
"""
couple-group_finalize.py: turn the chosen candidates of the COUPLE poses into the app's sample photos, landmarks and
credits, and record why the GROUP poses (and the couple poses without a usable photo) stay empty.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/sitting-floor_finalize.py frame CLIP T1[,T2]   # licensed 1080p, sharpest frame
    $PY -I tools/pose-photos/picks/couple-group_finalize.py fetch                  # Commons renditions
    $PY -I tools/pose-photos/picks/couple-group_finalize.py build [--poses id,id]

frame  (shared with the sitting-floor stage) downloads the clip page's 1080p file into WORK/picks_dl, keeps the frame
       with the highest Laplacian variance within 0.3 s of the given time as WORK/picks_dl/<clip>_<ms>.png plus a .json
       note (download page, video URL, exact frame time), and deletes the video.
fetch  downloads a standard 1920 px rendition of each Commons pick to WORK/picks_dl/couple-group/commons/.
build  per pick: detects people on the full-resolution source (the app's full model, IMAGE mode, up to 4 people, as
       detect_images.py); crops 3:4 portrait around all clearly detected people (union of their visible landmarks +
       margin; or a manual crop where the detector merges a hugging couple); mirrors when the matcher says the photo
       matches the flipped pose (or as set); resizes to 900 px on the long side; saves WebP quality 78 without
       metadata to app/src/main/assets/pose_photos/<poseId>.webp; runs the same detection on the saved file
       (tools/pose-photos/landmarks/<poseId>.json) and scores it with posematch (the Python port of PoseMatcher); writes
       tools/pose-photos/picks/couple-group.json (one object per COUPLE and GROUP pose: credits, or the reason it is
       empty).

The matcher needs both partners detected as separate people; MediaPipe often finds only one person in a hug, a
piggyback or a forehead touch, and then the pose cannot be scored at all (score null, said so in score_detail).
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parent
sys.path.insert(0, str(TOOLS))
import score as sc  # noqa: E402
from common import WORK, Polite, load_poses  # noqa: E402

_spec = importlib.util.spec_from_file_location("sitting_floor_finalize", HERE / "sitting-floor_finalize.py")
sff = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(sff)

ROOT = TOOLS.parent.parent
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "pose_photos"
LM_DIR = TOOLS / "landmarks"
CREDITS = HERE / "couple-group.json"
DL = WORK / "picks_dl"
CDL = DL / "couple-group" / "commons"
LONG_SIDE, QUALITY = 900, 78
CHECKED = "2026-11-21"

# "src": "mixkit" -> WORK/picks_dl/<clip>_<ms>.png made by `frame` (hold = the time passed to `frame`);
#        "commons" -> CDL/<pageid>_1920.jpg. "crop_px" (optional): manual [x0, y0, x1, y1] in source pixels, used
#        where the detector merges the couple into one person or the automatic 3:4 frame trims someone badly.
#        "mirror" (optional): force; otherwise mirrored when the matcher matched the flipped pose.
PICKS = [
    {"pose": "couple_walk_toward", "src": "commons", "pageid": 202609824,
     "reason": "A couple walking straight toward the camera hand in hand on a city pavement, mid-stride, she laughing "
               "as if mid-conversation: exactly the drawn 'walk and chat' (hand in hand, toward the camera)."},
    {"pose": "couple_walk_hands", "src": "mixkit", "clip": 4530, "hold": 17.5, "mirror": False, "crop_px": [580, 0, 1390, 1080],
     "reason": "A couple walking away hand in hand along a lavender-field path, free arms swinging, his head turned "
               "to the side; seen from behind and hip-up rather than side-on full length, but the hand-in-hand walk "
               "reads at once. Chosen over a full-length Commons back view whose Unsplash CC0 status is still "
               "'review needed' on Commons."},
    {"pose": "couple_hold_hands_facing", "src": "mixkit", "clip": 100733, "hold": 12.5, "crop_px": [542, 0, 1352, 1080],
     "reason": "Standing side-on facing each other about an arm's length apart, holding both hands at waist height, "
               "elbows soft, looking at each other and laughing: the tips word for word. Plain green studio "
               "backdrop; thigh-up (the clip never shows the feet)."},
    {"pose": "couple_forehead_touch", "src": "mixkit", "clip": 100759, "hold": 12.0, "crop_px": [40, 627, 1010, 1920],
     "reason": "Foreheads together, eyes closed, holding hands between them, the man leaning his head down: the drawn "
               "forehead touch. Plain green studio backdrop; thigh-up."},
    {"pose": "couple_back_hug", "src": "mixkit", "clip": 100758, "hold": 2.669, "crop_px": [500, 0, 1310, 1080],
     "reason": "He hugs her from behind with both arms wrapped around her front, his head beside hers, her hands "
               "resting on his forearms, both smiling at the camera: the tips exactly. Waist-up on a plain green "
               "studio backdrop (the drawn figure is full length)."},
    {"pose": "couple_piggyback", "src": "mixkit", "clip": 43817, "hold": 13.2,
     "reason": "She rides on his back with her arms wrapped around his shoulders and her head beside and above his, "
               "both laughing at the camera: the rider half of the drawn piggyback. Waist-up on a pink backdrop, so "
               "the carrier's legs and hands holding her legs are out of frame."},
    # crop_px (2026-11-22): the automatic frame started at y=480 and cut the top of his head; this one starts at
    # y=300 and keeps both heads whole.
    {"pose": "couple_sit_lean", "src": "mixkit", "clip": 100799, "hold": 4.338, "crop_px": [0, 300, 1080, 1740],
     "reason": "On a park bench she rests her head on his shoulder, eyes closed and smiling, while he smiles down: "
               "the defining gesture of 'sitting close'. Head-and-shoulders close-up (vertical clip), so the seated "
               "legs of the drawn figure are not shown."},
]

# Poses left without a photo: no candidate a person would recognise as the pose, or only ones that break a rule.
GAPS = {
    "couple_cheek_kiss": "No kiss on the cheek: the one clip that looked like it (Mixkit 4738, senior couple on a "
                         "bench, 5.0 s) is a kiss on the lips at full resolution; Mixkit 100789 at 9.6 s is her face "
                         "pressed laughing against his cheek, blurred, not a kiss; the green-screen kiss (100753) is "
                         "on the forehead; 'Couple kissing on the cheek' (930) is two people lying in bed, close-up; "
                         "'Boy hugging and kissing his girlfriend on the cheek' (39960) fails the minors rule.",
    "couple_side_hug": "No standing side by side with arms around each other in any candidate: the two-person "
                       "frames are couples walking hand in hand, facing each other or hugging front-on; the closest "
                       "(Commons 'Beautful women and handsome man': her elbow propped on his shoulder, his hands in "
                       "pockets) is a lean, not a hug, and the green-screen pairs (Mixkit 100744, 4847) stand side "
                       "by side with no arms in frame.",
    "couple_dip": "No dance dip anywhere in the harvest or the Mixkit titles crawled (3,146 cards); top-ranked "
                  "frames are people sitting on park benches.",
    "group_arms_around": "No three or more people standing shoulder to shoulder with arms around each other. The "
                         "18 group frames are party or New Year scenes with champagne, a cinema audience, an office "
                         "desk shot and fans on a sofa (beer bottles, team logos); the group hug (Mixkit 4709) is "
                         "seen from behind. Of the extra clips tried, 'Group of five friends laughing...' (51977) and "
                         "'Five female friends joking with poses' (51975) fail the people rules (underwear in the "
                         "description), 'Girls stop for a group photo' (6674) the minors rule, 'Team holding hands' "
                         "(25398) is Mixkit Restricted.",
    "group_v_formation": "No group with one person stepped forward and the others behind on either side; the top "
                         "frames are the same party/New Year/cinema scenes, and 'Three models posing...on board a bus' "
                         "(42575) fails the minors rule (boy, girls).",
    "group_jump": "No group jumping together: the jump clips crawled are concerts, pools, children or single "
                  "athletes, and the 'friends running' clips (23582, 36913, 21237) are Mixkit Restricted.",
    "group_walk_line": "No four friends walking toward the camera: no harvested frame shows it, and the two matching "
                       "titles (8900 'Group of friends walk on street with coffee', 23212 'Friends walking together "
                       "in the sunset') are Mixkit Restricted.",
    "group_sitting_row": "No friends sitting side by side with relaxed legs: the best frames are a New Year sofa "
                         "scene with champagne glasses (51538), a cinema row (100279) and football fans on a sofa "
                         "(44601: beer bottles, team-kit logos, face paint, legs out of frame); 'Group of exhausted "
                         "friends sitting during a party' (46878) fails the people rules (smoking).",
    "group_levels": "No group with heads at three heights (sitting, crouching, standing); the nearest (Mixkit 100320, "
                    "two seated at a desk with two standing behind) is an office video-call shot with two levels.",
    "group_back_to_back": "No middle person facing the camera with two others back to back against them in any "
                          "candidate.",
}


# ------------------------------------------------------------------------------------------------ sources
def commons_rendition(pageid, width=1920):
    files = json.loads((WORK / "commons" / "files.json").read_text())
    r = files[str(pageid)]
    orig = r["file_url"].split("?")[0]
    if width >= r["width"]:
        return r, orig, r["width"]
    path = orig.split("/wikipedia/commons/", 1)[1]
    name = path.rsplit("/", 1)[1]
    return r, f"https://thumb.wikimedia.org/wikipedia/commons/thumb/{path}/{width}px-{name}", width


def cmd_fetch(only):
    CDL.mkdir(parents=True, exist_ok=True)
    client = Polite(gaps={"thumb.wikimedia.org": 3.0, "upload.wikimedia.org": 3.0})
    for p in PICKS:
        if p["src"] != "commons" or (only and p["pose"] not in only):
            continue
        r, url, w = commons_rendition(p["pageid"])
        out = CDL / f"{p['pageid']}_{w}.jpg"
        if not out.exists():
            client.download(url, out, timeout=180)
        print(out, url, flush=True)


def clip_record(clip):
    for d in (WORK / "mixkit" / "clips", WORK / "couple-group" / "clips"):
        f = d / f"{clip}.json"
        if f.exists():
            rec = json.loads(f.read_text())
            if rec.get("licence", {}).get("ok"):
                return rec, str(f.relative_to(WORK))
    raise SystemExit(f"no licence-checked record for Mixkit clip {clip}")


def source_info(p):
    if p["src"] == "mixkit":
        stem = f"{p['clip']}_{round(p['hold'] * 1000):06d}"
        note = json.loads((DL / f"{stem}.json").read_text())
        clip, rec_path = clip_record(p["clip"])
        lic = clip["licence"]
        return DL / f"{stem}.png", {
            "source": "Mixkit", "title": clip["title"], "page_url": clip["page"],
            "file_url": note["download_page"], "video_file": note["video_url"], "video_quality": note["quality"],
            "frame_time_s": note["frame_t"], "hold_time_s_in_preview": note["hold_t"],
            "author": lic.get("author") or "Mixkit (no author named on the clip page)",
            "licence": {"name": lic["panel_licence"], "url": "https://mixkit.co/license/#videoFree",
                        "stated_on_page": lic["stated_on_page"], "checked": clip.get("checked", CHECKED),
                        "evidence": f"WORK/{rec_path}"},
            "attribution": f"Video still: Mixkit, Mixkit Stock Video Free License ({clip['page']})",
        }
    r, url, w = commons_rendition(p["pageid"])
    lic = r["licence"]
    return CDL / f"{p['pageid']}_{w}.jpg", {
        "source": "Wikimedia Commons", "title": r["title"], "page_url": r["page"],
        "file_url": r["file_url"].split("?")[0], "rendition_used": url,
        "author": lic.get("artist") or "unknown", "credit_line": lic.get("credit"),
        "licence": {"name": lic.get("short_name"), "url": lic.get("url"),
                    "attribution_required": lic.get("attribution_required"), "restrictions": lic.get("restrictions"),
                    "checked": CHECKED},
        "attribution": f"Photo: {lic.get('artist')}, {lic.get('short_name')} ({lic.get('url')}), via Wikimedia "
                       f"Commons ({r['page']})",
    }


# ------------------------------------------------------------------------------------------------ crop
def people_union(rec):
    """Box of all clearly detected people (area >= 20 % of the largest), visible MediaPipe landmarks, in pixels."""
    boxes = []
    for q in rec["people"]:
        pts = [(l[0], l[1]) for l in q if l[3] >= 0.5]
        if len(pts) >= 6:
            xs, ys = zip(*pts)
            boxes.append((min(xs), min(ys), max(xs), max(ys)))
    if not boxes:
        return None, 0
    areas = [(b[2] - b[0]) * (b[3] - b[1]) for b in boxes]
    keep = [b for b, a in zip(boxes, areas) if a >= 0.2 * max(areas)]
    x0, y0 = min(b[0] for b in keep), min(b[1] for b in keep)
    x1, y1 = max(b[2] for b in keep), max(b[3] for b in keep)
    return [x0 * rec["w"], y0 * rec["h"], x1 * rec["w"], y1 * rec["h"]], len(keep)


def auto_crop(box, W, H, aspect=0.75):
    """3:4 around the people: margin above for the head (landmarks stop at the eyes/ears), a little at the sides and
    below; then shrunk around the people's centre to fit inside the photo (which trims the sides when two people
    stand wider than a portrait frame)."""
    x0, y0, x1, y1 = box
    ph, pw = y1 - y0, x1 - x0
    x0, x1 = x0 - 0.05 * pw, x1 + 0.05 * pw
    y0, y1 = y0 - 0.18 * ph, y1 + 0.06 * ph
    w, h = x1 - x0, y1 - y0
    cx = (x0 + x1) / 2
    if w / h > aspect:
        h = w / aspect
    else:
        w = h * aspect
    s = min(1.0, W / w, H / h)
    w, h = w * s, h * s
    top = max(0.0, y0) if s == 1.0 else y0
    nx0, ny0 = cx - w / 2, top
    nx0 = min(max(0.0, nx0), W - w)
    ny0 = min(max(0.0, ny0), H - h)
    return [round(nx0), round(ny0), round(nx0 + w), round(ny0 + h)]


# ------------------------------------------------------------------------------------------------ build
def cmd_build(only):
    from PIL import Image
    poses = {p["id"]: p for p in load_poses()}
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    LM_DIR.mkdir(parents=True, exist_ok=True)
    det = sff.Detector()
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
        r = res[0] if res else None
        union, n_people = people_union(rec)
        if p.get("crop_px"):
            box = list(p["crop_px"])
            assert 0 <= box[0] < box[2] <= W and 0 <= box[1] < box[3] <= H, (p["pose"], box, W, H)
            assert abs((box[2] - box[0]) / (box[3] - box[1]) - 0.75) < 0.005, (p["pose"], box)
        else:
            if union is None:
                raise SystemExit(f"{p['pose']}: nobody detected in {img_path}; give a manual crop")
            box = auto_crop(union, W, H)
        mirror = p["mirror"] if p.get("mirror") is not None else bool(r and r["mirror_photo"])
        out = src.crop(tuple(box))
        if mirror:
            out = out.transpose(Image.FLIP_LEFT_RIGHT)
        s = LONG_SIDE / max(out.size)
        out = out.resize((round(out.width * s), round(out.height * s)), resample=Image.LANCZOS)
        clean = Image.new("RGB", out.size)
        clean.paste(out)
        dest = OUT_DIR / f"{p['pose']}.webp"
        clean.save(dest, "WEBP", quality=QUALITY, method=6)
        saved = Image.open(dest)
        rel = str(dest.relative_to(ROOT))
        lm = det.detect(saved, f"pose_photo:{p['pose']}", rel)
        lm.update({"model": "pose_landmarker_full.task", "mode": "IMAGE", "num_poses": 4})
        (LM_DIR / f"{p['pose']}.json").write_text(json.dumps(lm, separators=(",", ":")) + "\n")
        fin = sc.score_image(lm, [pose])
        f = fin[0] if fin else {}
        n_saved = people_union(lm)[1]
        changes = [f"still frame at {info['frame_time_s']} s of the {info['video_quality']} download"] \
            if p["src"] == "mixkit" else []
        changes.append(f"cropped to [{box[0]}, {box[1]}, {box[2]}, {box[3]}] px of the {W}x{H} source (3:4)")
        if mirror:
            changes.append("mirrored horizontally")
        changes.append(f"resized to {saved.width}x{saved.height}, WebP quality {QUALITY}, metadata stripped")
        note = None
        if not f:
            note = (f"not scorable: the detector finds {n_saved} separate person(s) in the shipped photo and the "
                    f"pose needs {len(pose['figures'])}")
        by_pose[p["pose"]] = {
            "pose_id": p["pose"], "pose_name": pose["name"], "category": pose["category"],
            "photo": rel, "bytes": dest.stat().st_size,
            **info,
            "changes": changes, "mirrored": bool(mirror), "crop_px": box, "source_size": [W, H],
            "app_matcher_score": f.get("score"),
            "score_detail": {"score_source_frame": r["score"] if r else None, "score_shipped_file": f.get("score"),
                             "shipped_matches_flipped_pose": f.get("mirror_photo"), "fit": f.get("fit"),
                             "tcov": f.get("tcov"), "people_detected_source": n_people,
                             "people_detected_shipped": n_saved, "framing": f.get("framing"),
                             **({"note": note} if note else {}),
                             "scorer": "tools/pose-photos/posematch.py (Python port of pose-core PoseMatcher)"},
            "reason": p["reason"],
        }
        print(f"{p['pose']}: {dest.stat().st_size // 1024} KB {saved.size} mirror={mirror} crop={box} people "
              f"{n_people}->{n_saved} score src {r['score'] if r else None} -> shipped {f.get('score')} "
              f"(flipped={f.get('mirror_photo')})", flush=True)
    for pid, why in GAPS.items():
        by_pose[pid] = {"pose_id": pid, "pose_name": poses[pid]["name"], "category": poses[pid]["category"],
                        "photo": None, "reason": why}
    ids = [q["id"] for q in load_poses()]
    out_list = sorted(by_pose.values(), key=lambda c: ids.index(c["pose_id"]))
    CREDITS.write_text(json.dumps(out_list, indent=1, ensure_ascii=False) + "\n")
    det.lm.close()
    print("credits:", CREDITS, flush=True)


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("fetch")
    a.add_argument("--poses")
    b = sub.add_parser("build")
    b.add_argument("--poses")
    args = ap.parse_args()
    only = set(args.poses.split(",")) if args.poses else None
    {"fetch": cmd_fetch, "build": cmd_build}[args.cmd](only)


if __name__ == "__main__":
    main()
