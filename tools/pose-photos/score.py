#!/usr/bin/env python3
"""
score.py: rank every candidate image against every library pose with the app's own matcher (posematch.py, a port of
pose-core PoseMatcher, checked against the Kotlin scores in promo/public/footage/chorus.json).

    python3 tools/pose-photos/score.py [--top 40] [--workers 1]

Inputs:  WORK/landmarks/*.jsonl (detect_images.py), WORK/mixkit/clips/*.json, WORK/commons/files.json, poses.json
Outputs: WORK/candidates/<poseId>.json  the pose and its best candidates, best first
         WORK/candidates/index.json      per pose: id, name, category, people, best score, candidates over 0.6 / 0.7

How a candidate is scored (per image, per pose, for the pose as drawn and flipped, i.e. the app's flip toggle):
  1. the image's people: MediaPipe landmarks -> the app's 21 joints, visibility = min(visibility, presence)
     (LandmarkMapping.toScene with the scene = the whole image). "People" counts persons whose visible-joint box is at
     least 20 % of the largest one's (smaller ones are background).
  2. the guide is placed on the people as a user would drag and pinch it (PromoChorusMatch.solveTransform): the
     template is fitted into the image (SceneMapping.fitFrame), then scaled by person torso / figure torso and moved
     so the figures' mid-hips (mid-shoulders when a hip is not visible) sit on the people's; for several figures the
     means over figures and over the largest people are used. The scale is clamped to 0.15-4 (the app clamps a pinch
     to 0.4-2.5; a photo will be cropped to the pose's framing, so a small person is fine here).
  3. score = the app's PoseMatcher score (BACK camera, not mirrored, strictness irrelevant to the score).
  4. fit = how much of what the person shows the template also requires (PromoChorusMatch.fitOf: 1 = a full-body pose
     on a full-body photo or a portrait pose on a waist-up photo; low = e.g. a portrait pose on a full-body photo).
  5. sit = mean distance from the person's joints to the placed figure's, in torso lengths (info only).
  6. crop_px = the pose's own frame placed with the same transform: the crop that frames the person as the pose is
     drawn (a portrait pose crops a full-body photo to waist up); crop_inside = its share inside the photo.
  7. tcov = share (by weight) of the segments the pose needs in its own frame that the photo shows and the matcher
     scored (the app's coverage only counts segments inside the scene, so a guide dragged mostly off a waist-up photo
     would still score 1 on the few segments left).
  rank = score - 0.3 * max(0, 0.85 - fit) - 0.25 * |people - figures| - 0.2 * max(0, 0.85 - crop_inside)
         - max(0, 0.95 - tcov).
  Plus 0.08 each when the candidate's title or description has a word of the pose's category (CATEGORY_WORDS) or of
  its name ("text_match"): the matcher alone cannot tell an arm held out of frame for a selfie from an arm cut off.
  Eligible (counted in index.json): tcov >= 0.9, people ==
  figures (groups: figures..figures+1) and fit >= 0.6. "mirror_photo": true means the best match was the flipped
  pose, so the photo must be mirrored horizontally to show the pose as drawn.
At most 2 frames per Mixkit clip per pose are kept in a pose's list (the best two).
"""
from __future__ import annotations

import argparse
import json
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import posematch as pm  # noqa: E402
from common import WORK, load_poses, mp_to_app, read_json, write_json  # noqa: E402

SIZE_REF = ("TORSO", "LEFT_UPPER_ARM", "LEFT_FOREARM", "RIGHT_UPPER_ARM", "RIGHT_FOREARM", "LEFT_THIGH",
            "LEFT_SHIN", "RIGHT_THIGH", "RIGHT_SHIN")


def visible_only(s):
    return {"points": {j: p for j, p in s["points"].items() if s["vis"].get(j, 1) >= 0.5},
            "vis": {j: v for j, v in s["vis"].items() if v >= 0.5}, "unit": 0}


def bbox(s):
    pts = [p for j, p in s["points"].items() if s["vis"].get(j, 1) >= 0.5]
    if len(pts) < 2:
        return None
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return (min(xs), min(ys), max(xs), max(ys))


def area(b):
    return (b[2] - b[0]) * (b[3] - b[1]) if b else 0.0


def mid(s, a, b):
    if a in s["points"] and b in s["points"]:
        pa, pb = s["points"][a], s["points"][b]
        return ((pa[0] + pb[0]) / 2, (pa[1] + pb[1]) / 2)
    return None


def torso_len(s, aspect, hips):
    ms, mh = mid(s, "LEFT_SHOULDER", "RIGHT_SHOULDER"), mid(s, "LEFT_HIP", "RIGHT_HIP")
    if hips and ms and mh:
        return math.hypot((ms[0] - mh[0]) * aspect, ms[1] - mh[1])
    return pm.estimated_unit({**s, "unit": 0}, aspect)


def solve_transform(figures, frame_aspect, aspect, mirror, people):
    """Drag and pinch the guide onto the people (PromoChorusMatch.solveTransform, generalised to several figures)."""
    figs = pm.template_to_scene(figures, frame_aspect, aspect, 1.0, (0.0, 0.0), mirror)
    l, t, r, b = pm.fit_frame(frame_aspect, aspect)
    cx, cy = (l + r) / 2, (t + b) / 2
    vis = [visible_only(p) for p in people]
    hips = all(mid(p, "LEFT_HIP", "RIGHT_HIP") for p in vis) and all(mid(f, "LEFT_HIP", "RIGHT_HIP") for f in figs)
    key = ("LEFT_HIP", "RIGHT_HIP") if hips else ("LEFT_SHOULDER", "RIGHT_SHOULDER")
    ta = pm.mean([mid(f, *key) for f in figs if mid(f, *key)])
    pa = pm.mean([mid(p, *key) for p in vis if mid(p, *key)])
    if ta is None or pa is None:
        return None
    pt = sum(torso_len(p, aspect, hips) for p in vis) / len(vis)
    ft = sum(torso_len(f, aspect, hips) for f in figs) / len(figs)
    scale = pt / ft if (ft > 0 and math.isfinite(pt / ft)) else 1.0
    scale = min(max(scale, 0.15), 4.0)
    ox = min(max(pa[0] - cx - (ta[0] - cx) * scale, -1.5), 1.5)
    oy = min(max(pa[1] - cy - (ta[1] - cy) * scale, -1.5), 1.5)
    return scale, (ox, oy)


def shows_in_scene(s, seg):
    def ok(j):
        p = s["points"].get(j)
        return p is not None and 0 <= p[0] <= 1 and 0 <= p[1] <= 1
    sh = ok("LEFT_SHOULDER") or ok("RIGHT_SHOULDER")
    hp = ok("LEFT_HIP") or ok("RIGHT_HIP")
    return {
        "TORSO": sh and hp, "HEAD": sh and ok("NOSE"),
        "SHOULDER_LINE": ok("LEFT_SHOULDER") and ok("RIGHT_SHOULDER"), "HIP_LINE": ok("LEFT_HIP") and ok("RIGHT_HIP"),
        "LEFT_UPPER_ARM": ok("LEFT_SHOULDER") and ok("LEFT_ELBOW"), "LEFT_FOREARM": ok("LEFT_ELBOW") and ok("LEFT_WRIST"),
        "RIGHT_UPPER_ARM": ok("RIGHT_SHOULDER") and ok("RIGHT_ELBOW"),
        "RIGHT_FOREARM": ok("RIGHT_ELBOW") and ok("RIGHT_WRIST"),
        "LEFT_THIGH": ok("LEFT_HIP") and ok("LEFT_KNEE"), "LEFT_SHIN": ok("LEFT_KNEE") and ok("LEFT_ANKLE"),
        "RIGHT_THIGH": ok("RIGHT_HIP") and ok("RIGHT_KNEE"), "RIGHT_SHIN": ok("RIGHT_KNEE") and ok("RIGHT_ANKLE"),
    }[seg]


def fit_of(scene_figs, figs, people, aspect):
    """PromoChorusMatch.fitOf: per figure, the share of what the person shows that the template requires (min)."""
    fits = []
    for k, (ev, det) in enumerate(figs):
        if ev is None:
            return 0.0
        person = visible_only(people[det])
        req = pm.TemplateInfo(k, scene_figs[k], aspect).required
        shown = covered = 0.0
        for name, *_r in pm.SEGMENTS:
            w = pm.SEG_BY_NAME[name][3]
            if not shows_in_scene(person, name):
                continue
            shown += w
            if name in req:
                covered += w
        fits.append(covered / shown if shown > 0 else 0.0)
    return min(fits) if fits else 0.0


def sit_of(scene_figs, figs, people, aspect):
    vals = []
    for k, (ev, det) in enumerate(figs):
        if ev is None:
            continue
        person = visible_only(people[det])
        tl = torso_len(person, aspect, mid(person, "LEFT_HIP", "RIGHT_HIP") is not None)
        if tl <= 1e-4:
            continue
        ds = [math.hypot((scene_figs[k]["points"][j][0] - p[0]) * aspect, scene_figs[k]["points"][j][1] - p[1])
              for j, p in person["points"].items() if 0 <= p[0] <= 1 and 0 <= p[1] <= 1 and j in scene_figs[k]["points"]]
        if len(ds) >= 6:
            vals.append(sum(ds) / len(ds) / tl)
    return sum(vals) / len(vals) if vals else 9.0


def crop_info(frame_aspect, aspect, scale, off, w, h):
    """The pose's own frame (the template's 3:4 or 4:3 box) placed on the photo with the same drag and pinch: the crop
    that shows the person framed as the pose is drawn. It can reach past the photo's edges (then it needs padding or a
    tighter crop); crop_inside is the share of it inside the photo."""
    l, t, r, b = pm.fit_frame(frame_aspect, aspect)
    cx, cy = (l + r) / 2, (t + b) / 2
    x0, x1 = cx + (l - cx) * scale + off[0], cx + (r - cx) * scale + off[0]
    y0, y1 = cy + (t - cy) * scale + off[1], cy + (b - cy) * scale + off[1]
    iw = max(0.0, min(x1, 1) - max(x0, 0))
    ih = max(0.0, min(y1, 1) - max(y0, 0))
    inside = (iw * ih) / ((x1 - x0) * (y1 - y0)) if x1 > x0 and y1 > y0 else 0.0
    return {"crop_px": [round(x0 * w), round(y0 * h), round(x1 * w), round(y1 * h)],
            "crop_inside": round(inside, 3), "crop_h_px": round((y1 - y0) * h)}


def template_coverage(own_required, figs):
    """Share (by segment weight) of the segments the pose needs in its OWN frame that the photo's person shows and the
    matcher scored; min over figures. The app's coverage only counts template segments inside the scene, so a guide
    dragged mostly off a head-and-shoulders photo would still score 1; this catches that."""
    vals = []
    for req, (ev, _) in zip(own_required, figs):
        total = sum(req.values())
        if ev is None or total <= 0:
            vals.append(0.0)
            continue
        vals.append(sum(w for name, w in req.items() if name in ev.segments) / total)
    return min(vals) if vals else 0.0


def framing_of(s):
    def v(j):
        p = s["points"].get(j)
        return p is not None and s["vis"].get(j, 1) >= 0.5 and -0.02 <= p[0] <= 1.02 and -0.02 <= p[1] <= 1.02
    hips = v("LEFT_HIP") or v("RIGHT_HIP")
    if v("LEFT_ANKLE") and v("RIGHT_ANKLE") and hips:
        return "full"
    if (v("LEFT_KNEE") or v("RIGHT_KNEE")) and hips:
        return "knees"
    if hips:
        return "waist"
    if v("LEFT_SHOULDER") or v("RIGHT_SHOULDER"):
        return "head-shoulders"
    return "partial"


CATEGORY_WORDS = {
    "STANDING": r"stand|standing|stands|pos(e|es|ing)",
    "SITTING": r"sit|sits|sitting|seated|chair|bench|stairs|steps|stool|sofa|couch|armchair",
    "LEANING": r"lean|leans|leaning|wall|railing|door|doorway",
    "WALKING": r"walk|walks|walking|stroll|strolling",
    "PORTRAIT": r"portrait|smil\w*|face",
    "SELFIE": r"selfie|selfies|mirror",
    "PROFESSIONAL": r"business\w*|office|suit|blazer|executive|corporate|professional",
    "FUN": r"jump\w*|danc\w*|spin\w*|twirl\w*|laugh\w*|celebrat\w*|kick\w*",
    "FLOOR": r"floor|ground|grass|lying|lies|crouch\w*|squat\w*|kneel\w*|cross-legged",
    "COUPLE": r"couple|couples|lovers|boyfriend|girlfriend|husband|wife|romantic|hug\w*|embrac\w*",
    "GROUP": r"friends|group|three|four|team|together",
}
STOP = {"the", "on", "in", "and", "with", "your", "pose", "up", "off", "out", "to", "a", "of", "&", "classic", "candid",
        "relaxed", "happy", "high", "low", "double", "one", "two"}


def text_terms(pose):
    """Regexes for the pose's category and for its own name (words of 4+ letters), matched against a candidate's
    title and description: a prior for the reviewer, since the matcher alone cannot tell e.g. an arm held out of frame
    for a selfie from an arm simply cut off."""
    words = [w for w in pose["name"].lower().replace("-", " ").split() if w not in STOP and len(w) >= 4]
    own = "|".join(re.escape(w.rstrip("s")) + r"\w*" for w in words)
    return (re.compile(r"\b(" + CATEGORY_WORDS[pose["category"]] + r")\b", re.I),
            re.compile(r"\b(" + own + r")", re.I) if own else None)


def load_meta():
    meta = {}
    for p in (WORK / "mixkit" / "clips").glob("*.json"):
        c = read_json(p)
        if not c or not c.get("scanned"):
            continue
        dl = {d["label"]: d["url"] for d in c["licence"].get("downloads", [])}
        for f in c.get("frames", []):
            key = "mixkit:" + Path(f["file"]).stem
            meta[key] = {
                "source": "Mixkit", "clip": c["id"], "time_s": f["t"], "title": c["title"], "page": c["page"],
                "description": c.get("description", ""),
                "file_url": dl.get("Full HD") or dl.get("HD Ready") or next(iter(dl.values()), None),
                "preview_url": c.get("preview"),
                "author": c["licence"].get("author") or "Mixkit (author not named on the clip page)",
                "licence": {"name": c["licence"]["panel_licence"], "url": "https://mixkit.co/license/#videoFree",
                            "stated_on_page": c["licence"]["stated_on_page"], "checked": c.get("checked")},
                "attribution": f"Video: Mixkit ({c['page']})",
                "flags": list(c.get("flags", [])) + ([] if f.get("still") else ["moving (not a hold)"]),
                "still": f.get("still"),
            }
    files = read_json(WORK / "commons" / "files.json", {}) or {}
    for pid, r in files.items():
        lic = r["licence"]
        artist = lic.get("artist") or "unknown"
        meta["commons:" + pid] = {
            "source": "Wikimedia Commons", "title": r["title"], "page": r["page"], "file_url": r["file_url"],
            "description": r.get("description", ""),
            "thumb_url": r.get("thumb_url"), "author": artist, "credit": lic.get("credit"),
            "licence": {"name": lic.get("short_name"), "key": lic.get("key"), "url": lic.get("url"),
                        "attribution_required": lic.get("attribution_required"),
                        "restrictions": lic.get("restrictions")},
            "attribution": f"Photo: {artist}, {lic.get('short_name')}, via Wikimedia Commons ({r['page']})",
            "flags": list(r["verdict"]["flags"]),
            "ok": r["ok"],
        }
    return meta


def score_image(rec, poses, top_k_people=6):
    w, h = rec["w"], rec["h"]
    aspect = w / h
    people = [mp_to_app(p) for p in rec["people"]]
    people = [p for p in people if sum(1 for v in p["vis"].values() if v >= 0.5) >= 6]
    if not people:
        return []
    areas = [area(bbox(p)) for p in people]
    big = max(areas)
    order = sorted(range(len(people)), key=lambda i: -areas[i])
    people = [people[i] for i in order][:top_k_people]
    areas = [areas[i] for i in order][:top_k_people]
    n_sig = sum(1 for a in areas if a >= 0.2 * big)
    out = []
    for pose in poses:
        n = len(pose["figures"])
        if n_sig < n or n_sig > n + (1 if n >= 3 else 0) + 1:
            continue  # cannot be eligible and would only waste time
        figures = [{"points": {j: tuple(v) for j, v in f["joints"].items()}, "vis": f.get("visibility", {}),
                    "unit": f["unit"]} for f in pose["figures"]]
        # the segments the pose needs in its own frame (as drawn / flipped), for template_coverage
        own_required = {m: [{name: pm.SEG_BY_NAME[name][3] for name in
                             pm.TemplateInfo(k, f, pose["frameAspect"]).required}
                            for k, f in enumerate(pm.template_to_scene(figures, pose["frameAspect"],
                                                                       pose["frameAspect"], 1.0, (0.0, 0.0), m))]
                        for m in (False, True)}
        best = None
        for mirror in (False, True):
            tr = solve_transform(figures, pose["frameAspect"], aspect, mirror, people[:n])
            if tr is None:
                continue
            scale, off = tr
            scene = pm.template_to_scene(figures, pose["frameAspect"], aspect, scale, off, mirror)
            score, figs = pm.match(scene, people, aspect)
            if best is not None and score <= best["score"] - 0.2:
                continue
            fit = fit_of(scene, figs, people, aspect)
            tcov = template_coverage(own_required[mirror], figs)
            sit = sit_of(scene, figs, people, aspect)
            mismatch = abs(n_sig - n)
            crop = crop_info(pose["frameAspect"], aspect, scale, off, w, h)
            rank = score - 0.3 * max(0.0, 0.85 - fit) - 0.25 * mismatch - 0.2 * max(0.0, 0.85 - crop["crop_inside"]) \
                - 1.0 * max(0.0, 0.95 - tcov)
            if best is None or rank > best["rank"]:
                used = [d for _, d in figs if d is not None]
                boxes = [bbox(people[d]) for d in used]
                union = (min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes),
                         max(b[3] for b in boxes)) if boxes and all(boxes) else None
                best = {
                    "pose": pose["id"], "score": round(score, 4), "rank": round(rank, 4), "fit": round(fit, 3),
                    "sit": round(sit, 3), "tcov": round(tcov, 3), "mirror_photo": mirror, "people": n_sig,
                    "figures": n, "eligible": (n <= n_sig <= n + (1 if n >= 3 else 0)) and fit >= 0.6 and tcov >= 0.9,
                    "framing": framing_of(people[used[0]]) if used else None,
                    "people_box_px": [round(union[0] * w), round(union[1] * h), round(union[2] * w),
                                      round(union[3] * h)] if union else None,
                    "transform": {"scale": round(scale, 4), "offsetX": round(off[0], 4), "offsetY": round(off[1], 4)},
                    **crop,
                    "parts": [{k: round(v, 3) for k, v in e.part_scores.items()} if e else None for e, _ in figs],
                    "segments": [e.segments if e else None for e, _ in figs],
                    "figure_scores": [round(e.score, 4) if e else 0.0 for e, _ in figs],
                }
        if best:
            out.append(best)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=40)
    args = ap.parse_args()
    poses = load_poses()
    meta = load_meta()
    per_pose = {p["id"]: [] for p in poses}
    n_img = {"mixkit": 0, "commons": 0}
    for lf in sorted((WORK / "landmarks").glob("*.jsonl")):
        for line in lf.read_text().splitlines():
            try:
                rec = json.loads(line)
            except json.JSONDecodeError:
                continue
            m = meta.get(rec["id"])
            if m is None or m.get("ok") is False:
                continue  # frame of a clip not (or no longer) accepted / Commons file failing the rules
            n_img[rec["source"]] = n_img.get(rec["source"], 0) + 1
            for r in score_image(rec, poses):
                r.update({"id": rec["id"], "image": rec["image"], "w": rec["w"], "h": rec["h"]})
                per_pose[r["pose"]].append(r)
    cand_dir = WORK / "candidates"
    cand_dir.mkdir(parents=True, exist_ok=True)
    index = []
    for pose in poses:
        cat_re, own_re = text_terms(pose)
        for r in per_pose[pose["id"]]:
            m = meta[r["id"]]
            text = (m.get("title") or "") + " " + (m.get("description") or "")
            r["text_match"] = [k for k, rx in (("category", cat_re), ("name", own_re)) if rx and rx.search(text)]
            r["rank"] = round(r["rank"] + 0.08 * len(r["text_match"]), 4)
        lst = sorted(per_pose[pose["id"]], key=lambda r: -r["rank"])
        kept, per_clip = [], {}
        for r in lst:
            m = meta[r["id"]]
            if m["source"] == "Mixkit":
                k = per_clip.get(m["clip"], 0)
                if k >= 2:
                    continue
                per_clip[m["clip"]] = k + 1
            kept.append({**r, **{k: v for k, v in m.items() if k != "ok"}})
            if len(kept) >= args.top:
                break
        elig = [r for r in lst if r["eligible"]]
        write_json(cand_dir / f"{pose['id']}.json", {
            "pose": {"id": pose["id"], "name": pose["name"], "category": pose["category"],
                     "people": len(pose["figures"]), "frameAspect": pose["frameAspect"]},
            "scored": len(lst), "eligible": len(elig), "candidates": kept}, indent=1)
        index.append({
            "id": pose["id"], "name": pose["name"], "category": pose["category"], "people": len(pose["figures"]),
            "best_score": max((r["score"] for r in elig), default=0.0),
            "best_rank": max((r["rank"] for r in elig), default=0.0),
            "over_0_6": sum(1 for r in elig if r["score"] > 0.6),
            "over_0_7": sum(1 for r in elig if r["score"] > 0.7),
            "over_0_8": sum(1 for r in elig if r["score"] > 0.8),
            "eligible": len(elig),
        })
    write_json(cand_dir / "index.json", {"images_scored": n_img, "poses": index}, indent=1)
    print(json.dumps(n_img), "images scored;", sum(1 for i in index if i["best_score"] > 0.7), "poses > 0.7,",
          sum(1 for i in index if i["best_score"] > 0.6), "poses > 0.6", flush=True)


if __name__ == "__main__":
    main()
