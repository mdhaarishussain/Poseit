#!/usr/bin/env python3
"""
mixkit_harvest.py: still frames of stock models holding a pose, from Mixkit stock videos (Free License only).

    python3 tools/pose-photos/mixkit_harvest.py crawl  [--pages 4] [--tags a,b]
    python3 tools/pose-photos/mixkit_harvest.py scan   [--max-clips 400] [--fps 3]
    python3 tools/pose-photos/mixkit_harvest.py report

crawl   reads the grid pages of many people tags (TAGS below; a tag without its own page falls back to Mixkit's search
        page /free-stock-video/discover/<tag>/) and records every item card: id, page, title, description and the
        grid's hover-preview video (360p). -> WORK/mixkit/items.json
scan    ranks the items by how likely their title and description show a person holding a library pose (RELEVANT /
        UNLIKELY words; the people rules in rules.py reject minors and sensitive contexts), then, best first, until
        --max-clips clips are scanned:
          1. reads the clip page and keeps the clip only if its download panel says "Mixkit Stock Video Free License"
             (data-license="videoFree") and its schema.org license is https://mixkit.co/license/#videoFree;
          2. downloads the 360p hover preview (the file a browser plays on the grid) to WORK/mixkit/video/<id>.mp4;
          3. runs the app's lite pose model (MediaPipe Pose Landmarker, VIDEO mode, confidences 0.5, up to 4 people)
             on --fps frames a second, follows the largest person, and measures how fast the pose changes;
          4. keeps up to 8 frames per clip whose main person shows at least head, shoulders and hips: frames where the
             pose holds still (both neighbouring samples < HOLD_SPEED deg/s) first, then clearly different poses
             (tagged "moving"); each kept frame is saved as WORK/mixkit/frames/<id>_<ms>.jpg;
          5. deletes the video. Per clip: WORK/mixkit/clips/<id>.json (licence evidence, tags, frames kept).
        --focus "couple|group|sitting" limits a run to clips whose text has one of those words (to fill weak
        categories). Requests are serial with a gap (mixkit.co 2 s, assets 1 s); re-runs skip clips already scanned.
report  prints counts.

Mixkit User Terms (https://mixkit.co/terms/, clause 9) forbid using "scripts or bots to mass download Items". This tool
reads the public grid and clip pages and fetches only the small hover previews of clips pre-selected by their text,
one at a time; see docs/POSE_PHOTOS.md for the open question this leaves.
"""
from __future__ import annotations

import argparse
import html
import json
import math
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import MODELS, WORK, Polite, read_json, write_json  # noqa: E402
import rules  # noqa: E402

BASE = "https://mixkit.co"
OUT = WORK / "mixkit"
UA = "Mozilla/5.0 (X11; Linux x86_64) PoseitPhotoHarvest/0.1"

TAGS = ["posing", "model", "fashion", "portrait", "sitting", "walking", "couple", "friends", "group", "selfie",
        "business", "office", "lifestyle", "street", "park", "beach", "studio", "dancing", "laughing", "leaning",
        "lying", "jumping", "pose", "modeling", "photoshoot", "full-body", "woman", "man", "people", "urban", "city",
        "summer", "happy", "hug", "romance", "love", "businessman", "businesswoman", "standing", "smiling", "relaxing",
        "fashion-model", "vertical-shot", "elegant", "stairs", "wall", "bench", "sunglasses", "jacket", "spinning",
        "celebration", "fun", "dance", "crouching", "kneeling", "skipping"]

RELEVANT = {
    "posing": 3, "poses": 3, "pose": 3, "poised": 1, "model": 2, "models": 2, "modeling": 2, "photoshoot": 2,
    "full body": 3, "full length": 3, "full-length": 3, "whole body": 3, "head to toe": 3, "full frame": 1,
    "standing": 2, "stands": 2, "sitting": 2, "sits": 2, "seated": 2, "leaning": 2, "leans": 2, "walking": 1,
    "walks": 1, "strolling": 1, "jumping": 2, "jumps": 2, "jump": 2, "dancing": 1, "dances": 1, "spinning": 2,
    "spins": 2, "twirling": 2, "twirls": 2, "lying": 2, "lies": 2, "crouching": 2, "crouches": 2, "squatting": 2,
    "kneeling": 2, "kneels": 2, "selfie": 2, "hug": 1, "hugs": 1, "hugging": 1, "embrace": 1, "piggyback": 3,
    "couple": 1, "friends": 1, "group": 1, "camera": 1, "looks at the camera": 1, "hands on hips": 3,
    "hand on hip": 3, "arms crossed": 3, "crossed arms": 3, "hands in pockets": 3, "pocket": 1, "wall": 1,
    "stairs": 1, "steps": 1, "bench": 1, "railing": 2, "chair": 1, "sofa": 1, "couch": 1, "stool": 1,
    "skipping": 2, "laughing": 1, "smiling": 1, "fashion": 1, "outfit": 1, "elegant": 1, "confident": 1,
    "sunglasses": 1, "hat": 1, "jacket": 1, "suit": 1, "blazer": 1, "businessman": 1, "businesswoman": 1,
}
UNLIKELY = {
    "close up": -2, "close-up": -2, "closeup": -2, "detail": -2, "hands of": -2, "feet": -1, "eyes": -1, "lips": -2,
    "face of": -1, "drone": -3, "aerial": -3, "timelapse": -3, "time lapse": -3, "crowd": -2, "traffic": -3,
    "landscape": -1, "animal": -1, "dog": -1, "cat": -1, "horse": -1, "car": -2, "driving": -2, "laptop": -1,
    "computer": -1, "typing": -2, "keyboard": -2, "cooking": -2, "kitchen": -1, "underwater": -3, "swimming": -3,
    "pool": -2, "silhouette": -2, "shadow": -1, "blurred": -2, "out of focus": -2, "bokeh": -1, "night": -1,
    "neon": -1, "smoke": -1, "rain": -1, "virtual background": -3, "green screen": -3, "animation": -3,
    "abstract": -3, "3d": -3, "text": -1, "logo": -3, "rotating": -1, "slow motion": 0, "from behind": -1,
    "back view": -1, "screen": -1, "phone screen": -2, "video call": -2, "meeting": -1, "presentation": -1,
    "yoga": -1, "workout": -1, "exercise": -1, "gym": -1, "running": -1, "bicycle": -2, "skateboard": -1,
    "surfing": -2, "eating": -1, "drinking": -1, "glass of": -1, "wine": -2, "champagne": -2, "beer": -2,
    "cocktail": -2, "party": -1, "club": -1, "concert": -2, "festival": -2,
}

HOLD_SPEED = 25.0  # deg/s, mean angular speed of the 10 pose segments; detect.py: ~3-8 on a still body, 40+ on a move
MAX_FRAMES = 8


def http() -> Polite:
    return Polite(gaps={"mixkit.co": 2.0, "assets.mixkit.co": 1.0}, ua=UA)


# ------------------------------------------------------------------------------------------------------------- crawl

CARD = re.compile(r'<div class="item-grid-card[^"]*"\s+data-algolia-analytics-object-id="video-(\d+)"')


def parse_grid(text: str) -> list[dict]:
    """Item cards of a tag grid page or a search (discover) page."""
    items = []
    starts = list(CARD.finditer(text))
    for k, m in enumerate(starts):
        vid = m.group(1)
        body = text[m.end(): starts[k + 1].start() if k + 1 < len(starts) else m.end() + 20000]
        video = re.search(r'<video src="([^"]+)"', body)
        thumb = re.search(r'<img src="([^"]+)"', body)
        title = re.search(r'item-grid-video-player__overlay-video-title">\s*(.*?)\s*</span>', body, re.S) or \
            re.search(r'item-grid-video-player__overlay-link">\s*(.*?)\s*</a>', body, re.S)
        desc = re.search(r'item-grid-video-player__overlay-video-description">\s*(.*?)\s*</span>', body, re.S) or \
            re.search(r'item-grid-card__description">\s*(.*?)\s*</p>', body, re.S)
        href = re.search(r'href="(/free-stock-video/[a-z0-9-]+-%s/)"' % vid, body)
        vertical = "item-grid-video-player--vertical" in body
        if not (video and href):
            continue
        items.append({
            "id": int(vid),
            "page": BASE + href.group(1),
            "title": html.unescape(title.group(1)).strip() if title else "",
            "description": html.unescape(desc.group(1)).strip() if desc else "",
            "preview": video.group(1),
            "thumb": thumb.group(1) if thumb else None,
            "vertical": vertical,
        })
    return items


def cmd_crawl(args):
    client = http()
    items = read_json(OUT / "items.json", {}) or {}
    tags = args.tags.split(",") if args.tags else TAGS
    for tag in tags:
        base_url = f"{BASE}/free-stock-video/{tag}/"
        for page in range(1, args.pages + 1):
            url = base_url + (f"?page={page}" if page > 1 else "")
            try:
                text = client.get(url).decode("utf-8", "replace")
            except Exception as e:
                if page == 1 and "/discover/" not in base_url:
                    base_url = f"{BASE}/free-stock-video/discover/{tag}/"
                    try:
                        text = client.get(base_url).decode("utf-8", "replace")
                    except Exception as e2:
                        print(f"[{tag}] no page: {e2}", flush=True)
                        break
                else:
                    print(f"[{tag}] page {page}: {e}", flush=True)
                    break
            found = parse_grid(text)
            if not found and page == 1 and "/discover/" not in base_url:
                # a tag without its own grid: use Mixkit's search page for the same words
                base_url = f"{BASE}/free-stock-video/discover/{tag}/"
                text = client.get(base_url).decode("utf-8", "replace")
                found = parse_grid(text)
            new = 0
            for it in found:
                key = str(it["id"])
                if key not in items:
                    it["tags_seen"] = [tag]
                    items[key] = it
                    new += 1
                elif tag not in items[key]["tags_seen"]:
                    items[key]["tags_seen"].append(tag)
            print(f"[{tag}] page {page}: {len(found)} cards, {new} new (total {len(items)})", flush=True)
            if not found or f"page={page + 1}" not in text:
                break
        write_json(OUT / "items.json", items)
    write_json(OUT / "items.json", items)
    print(f"{len(items)} items", flush=True)


# -------------------------------------------------------------------------------------------------------------- rank

def relevance(it: dict) -> tuple[float, dict]:
    text = " " + re.sub(r"[^a-z0-9 -]+", " ", (it["title"] + " " + it["description"]).lower()) + " "
    text = re.sub(r"\s+", " ", text)
    score = 0.0
    for w, s in RELEVANT.items():
        if f" {w} " in text:
            score += s
    for w, s in UNLIKELY.items():
        if f" {w} " in text:
            score += s
    if it.get("vertical"):
        score += 1.0  # vertical framing suits a phone pose guide (3:4 / 9:16 crops keep the whole body)
    verdict = rules.people_check(it["title"], it["description"], [])
    # Mixkit clips are stock footage of models: "public/event" words (runway, catwalk...) and art words do not apply
    verdict["reject"] = [r for r in verdict["reject"] if not r.startswith(("public/event", "not a photo"))]
    return score, verdict


# -------------------------------------------------------------------------------------------------------------- scan

def check_licence(page_html: str) -> dict:
    panel = re.search(r"Download\s+this\s+free\s+stock\s+video\s+clip\s+for\s+commercial\s+or\s+personal\s+use,\s+"
                      r'under\s+the\s*<a[^<]*?data-license="([a-zA-Z]+)"[^<]*?>\s*([^<]+?)\s*</a>', page_html)
    schema = re.findall(r'"license":"([^"]+)"', page_html)
    restricted = "videoRestricted" in page_html.split("item-attribute")[0] if "item-attribute" in page_html else \
        "videoRestricted" in page_html
    key = panel.group(1) if panel else None
    name = panel.group(2).strip() if panel else None
    ok = key == "videoFree" and name == "Mixkit Stock Video Free License" and \
        any(s.endswith("/license/#videoFree") for s in schema[:1]) and not restricted
    downloads = re.findall(r'data-label="([^"]+)"\s+data-size="[^"]*"\s+value="(/free-stock-video/download/\d+/\?[^"]+)"',
                           page_html)
    tags = sorted(set(re.findall(r'class="meta-links__link"[^>]*href="/free-stock-video/([a-z0-9-]+)/"', page_html)))
    dur = re.search(r'"duration":"PT(\d+)M(\d+)S"', page_html)
    notice = re.search(r'"copyrightNotice":"([^"]*)"', page_html)
    author = re.search(r'"description":"[^"]*?created by ([^"]+?) that you can use', page_html)
    if notice and "restricted" in notice.group(1).lower():
        ok = False
    return {
        "ok": ok,
        "panel_licence": name, "panel_key": key,
        "schema_license": schema[0] if schema else None,
        "stated_on_page": "Download this free stock video clip for commercial or personal use, under the "
                          f"{name}." if name else None,
        "restricted_seen": restricted,
        "downloads": [{"label": html.unescape(l), "url": BASE + html.unescape(u)} for l, u in downloads],
        "tags": tags,
        "duration_s": int(dur.group(1)) * 60 + int(dur.group(2)) if dur else None,
        "copyright_notice": notice.group(1) if notice else None,
        "author": html.unescape(author.group(1)).strip() if author else None,
    }


SEGS = [("LEFT_SHOULDER", "LEFT_ELBOW"), ("LEFT_ELBOW", "LEFT_WRIST"), ("RIGHT_SHOULDER", "RIGHT_ELBOW"),
        ("RIGHT_ELBOW", "RIGHT_WRIST"), ("LEFT_HIP", "LEFT_KNEE"), ("LEFT_KNEE", "LEFT_ANKLE"),
        ("RIGHT_HIP", "RIGHT_KNEE"), ("RIGHT_KNEE", "RIGHT_ANKLE"), ("RIGHT_SHOULDER", "LEFT_SHOULDER"),
        ("RIGHT_HIP", "LEFT_HIP")]


def seg_angles(p: dict, aspect: float) -> dict:
    out = {}
    for a, b in SEGS:
        if p["vis"].get(a, 0) >= 0.5 and p["vis"].get(b, 0) >= 0.5:
            (ax, ay), (bx, by) = p["points"][a], p["points"][b]
            out[(a, b)] = math.degrees(math.atan2((bx - ax) * aspect, by - ay))
    return out


def pose_diff(a: dict, b: dict) -> float | None:
    common = [k for k in a if k in b]
    if len(common) < 3:
        return None
    return sum(abs((a[k] - b[k] + 180) % 360 - 180) for k in common) / len(common)


def framing(p: dict) -> str | None:
    v = lambda j: p["vis"].get(j, 0) >= 0.5 and -0.02 <= p["points"][j][0] <= 1.02 and -0.02 <= p["points"][j][1] <= 1.02
    if not (v("NOSE") and (v("LEFT_SHOULDER") or v("RIGHT_SHOULDER"))):
        return None
    hips = v("LEFT_HIP") or v("RIGHT_HIP")
    if v("LEFT_ANKLE") and v("RIGHT_ANKLE") and hips:
        return "full"
    if (v("LEFT_KNEE") or v("RIGHT_KNEE")) and hips:
        return "knees"
    if hips:
        return "half"
    return "head"


def scan_video(path: Path, fps: float, landmarker_factory):
    import cv2
    import numpy as np
    from mediapipe import Image, ImageFormat
    from common import mp_to_app

    cap = cv2.VideoCapture(str(path))
    src_fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    step = max(1, round(src_fps / fps))
    samples = []
    i = 0
    with landmarker_factory() as lm:
        while True:
            ok = cap.grab()
            if not ok:
                break
            if i % step == 0:
                ok, bgr = cap.retrieve()
                if not ok:
                    break
                rgb = np.ascontiguousarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
                res = lm.detect_for_video(Image(image_format=ImageFormat.SRGB, data=rgb), int(i * 1000 / src_fps))
                people = []
                for person in res.pose_landmarks:
                    raw = [[l.x, l.y, l.z, l.visibility if l.visibility is not None else 1.0,
                            l.presence if l.presence is not None else 1.0] for l in person]
                    people.append(mp_to_app(raw))
                people.sort(key=lambda p: -bbox_area(p))
                samples.append({"i": i, "t": i / src_fps, "people": people, "bgr": None})
                samples[-1]["_frame"] = bgr
            i += 1
    cap.release()
    return samples, (w, h, src_fps)


def bbox_area(p: dict) -> float:
    pts = [p["points"][j] for j, v in p["vis"].items() if v >= 0.5]
    if len(pts) < 2:
        return 0.0
    xs, ys = zip(*pts)
    return (max(xs) - min(xs)) * (max(ys) - min(ys))


def pick_frames(samples: list, aspect: float) -> list[dict]:
    for s in samples:
        main = s["people"][0] if s["people"] else None
        s["framing"] = framing(main) if main else None
        s["angles"] = seg_angles(main, aspect) if main else {}
        big = [p for p in s["people"] if main and bbox_area(p) >= 0.25 * bbox_area(main)]
        s["n_people"] = len(big)
    speeds = []
    for k, s in enumerate(samples):
        sp = []
        for j in (k - 1, k + 1):
            if 0 <= j < len(samples):
                d = pose_diff(s["angles"], samples[j]["angles"])
                dt = abs(samples[j]["t"] - s["t"]) or 1.0
                sp.append(d / dt if d is not None else 999.0)
        speeds.append(max(sp) if sp else 999.0)
    cands = []
    for s, sp in zip(samples, speeds):
        if not s["framing"]:
            continue
        vis = sum(s["people"][0]["vis"].values()) / 21.0
        still = sp < HOLD_SPEED
        cands.append((0 if still else 1, -vis, sp, s))
    cands.sort(key=lambda c: (c[0], c[1]))
    kept = []
    for still_rank, _, sp, s in cands:
        if len(kept) >= MAX_FRAMES:
            break
        diffs = [pose_diff(s["angles"], k["angles"]) for k in kept]
        diffs = [d for d in diffs if d is not None]
        if diffs and min(diffs) < (15.0 if still_rank == 0 else 25.0):
            continue
        s["speed"] = sp
        s["still"] = still_rank == 0
        kept.append(s)
    return sorted(kept, key=lambda s: s["t"])


def make_factory(model: str, num_poses: int):
    from mediapipe.tasks.python import BaseOptions
    from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode

    def factory():
        opts = PoseLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=str(MODELS / f"pose_landmarker_{model}.task")),
            running_mode=RunningMode.VIDEO, num_poses=num_poses,
            min_pose_detection_confidence=0.5, min_pose_presence_confidence=0.5, min_tracking_confidence=0.5,
            output_segmentation_masks=False)
        return PoseLandmarker.create_from_options(opts)
    return factory


def cmd_scan(args):
    import cv2

    client = http()
    items = read_json(OUT / "items.json", {}) or {}
    for d in ("clips", "frames", "video"):
        (OUT / d).mkdir(parents=True, exist_ok=True)
    ranked = []
    for key, it in items.items():
        score, verdict = relevance(it)
        it["relevance"] = score
        it["verdict"] = verdict
        if verdict["reject"] or score < args.min_relevance:
            continue
        ranked.append(it)
    if args.focus:
        focus = re.compile(r"\b(" + args.focus + r")\b", re.I)
        ranked = [it for it in ranked if focus.search(it["title"] + " " + it["description"])]
    ranked.sort(key=lambda it: (-it["relevance"], -it["id"]))
    done = {int(p.stem) for p in (OUT / "clips").glob("*.json")}
    scanned = sum(1 for p in (OUT / "clips").glob("*.json") if (read_json(p) or {}).get("scanned"))
    print(f"{len(items)} items, {len(ranked)} pass text rules (relevance >= {args.min_relevance}); "
          f"{scanned} already scanned", flush=True)
    factory = make_factory(args.model, 4)
    for it in ranked:
        if scanned >= args.max_clips:
            break
        if it["id"] in done:
            continue
        rec = {"id": it["id"], "page": it["page"], "title": it["title"], "description": it["description"],
               "preview": it["preview"], "relevance": it["relevance"], "flags": it["verdict"]["flags"],
               "tags_seen": it.get("tags_seen", []), "checked": time.strftime("%Y-%m-%d"), "scanned": False}
        try:
            page = client.get(it["page"]).decode("utf-8", "replace")
            lic = check_licence(page)
        except Exception as e:
            print(f"  {it['id']}: page error {e}", flush=True)
            continue
        rec["licence"] = lic
        if not lic["ok"]:
            rec["skip"] = "not Mixkit Stock Video Free License"
            write_json(OUT / "clips" / f"{it['id']}.json", rec, indent=1)
            print(f"  {it['id']}: licence {lic['panel_key']} -> skip", flush=True)
            continue
        vpath = OUT / "video" / f"{it['id']}.mp4"
        try:
            client.download(it["preview"], vpath, timeout=120)
            samples, (w, h, sfps) = scan_video(vpath, args.fps, factory)
        except Exception as e:
            print(f"  {it['id']}: video error {e}", flush=True)
            vpath.unlink(missing_ok=True)
            continue
        aspect = w / h if h else 0.5625
        kept = pick_frames(samples, aspect)
        frames = []
        for s in kept:
            ms = int(round(s["t"] * 1000))
            fname = f"{it['id']}_{ms:06d}.jpg"
            cv2.imwrite(str(OUT / "frames" / fname), s["_frame"], [cv2.IMWRITE_JPEG_QUALITY, 92])
            frames.append({"file": fname, "t": round(s["t"], 3), "framing": s["framing"], "still": s["still"],
                           "speed": round(s["speed"], 1), "people_lite": s["n_people"]})
        rec.update({"scanned": True, "video": {"width": w, "height": h, "fps": round(sfps, 3),
                                               "samples": len(samples), "sample_fps": args.fps},
                    "frames": frames,
                    "people_max": max((s["n_people"] for s in samples if s.get("n_people") is not None), default=0)})
        write_json(OUT / "clips" / f"{it['id']}.json", rec, indent=1)
        vpath.unlink(missing_ok=True)
        scanned += 1
        print(f"  [{scanned}] {it['id']} {it['title'][:60]!r}: {len(samples)} samples, {len(frames)} frames kept "
              f"({sum(f['still'] for f in frames)} still)", flush=True)
    print(f"scanned {scanned} clips; {client.requests} requests", flush=True)


def cmd_report(args):
    clips = [read_json(p) for p in (OUT / "clips").glob("*.json")]
    scanned = [c for c in clips if c and c.get("scanned")]
    print(json.dumps({
        "items": len(read_json(OUT / "items.json", {}) or {}),
        "clip_pages_checked": len(clips),
        "licence_skipped": sum(1 for c in clips if c and c.get("skip")),
        "clips_scanned": len(scanned),
        "frames_kept": sum(len(c["frames"]) for c in scanned),
        "still_frames": sum(sum(f["still"] for f in c["frames"]) for c in scanned),
        "clips_with_frames": sum(1 for c in scanned if c["frames"]),
    }, indent=1))


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("crawl")
    c.add_argument("--pages", type=int, default=4)
    c.add_argument("--tags")
    s = sub.add_parser("scan")
    s.add_argument("--max-clips", type=int, default=400)
    s.add_argument("--fps", type=float, default=3.0)
    s.add_argument("--model", default="lite", choices=["lite", "full"])
    s.add_argument("--min-relevance", type=float, default=2.0)
    s.add_argument("--focus", help="regex alternation: only clips whose text has one of these words (e.g. couple|group)")
    sub.add_parser("report")
    args = ap.parse_args()
    {"crawl": cmd_crawl, "scan": cmd_scan, "report": cmd_report}[args.cmd](args)


if __name__ == "__main__":
    main()
