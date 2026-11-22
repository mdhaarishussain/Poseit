#!/usr/bin/env python3
"""
couple-group_extra.py: a small, targeted second look at Mixkit for the COUPLE and GROUP poses, which the harvest left
almost empty (two-person frames: 218 scored; three or more people: 18). The harvest crawled 3,146 clip cards but
scanned 510; the clips listed in CLIPS below were picked by hand from the crawled titles (WORK/mixkit/items.json)
because their titles name a couple or group pose (hug, cheek kiss, hand in hand, walking friends, sitting row...).

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/couple-group_extra.py fetch      # licence check + 360p preview + frames, per clip
    $PY -I tools/pose-photos/picks/couple-group_extra.py sheet      # one contact strip per clip to look at

fetch  per clip, exactly the harvest's checks (mixkit_harvest.check_licence on the clip page: download panel says
       "Mixkit Stock Video Free License", data-license="videoFree", schema.org licence #videoFree, copyrightNotice not
       Restricted; rules.people_check on title and description), then downloads the 360p hover preview, writes one
       frame a second (at most 24) to WORK/couple-group/frames/<id>_<ms>.jpg and deletes the preview. No pose model
       runs here: the frames are for looking at; a chosen frame is extracted again from the licensed 1080p download
       and detected/scored by couple-group_finalize.py. Records: WORK/couple-group/clips/<id>.json (same fields as the
       harvest's WORK/mixkit/clips records). Kept apart from WORK/mixkit so the shared candidate lists stay as scored.
sheet  WORK/couple-group/sheets/<id>.jpg strips.
"""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parent
sys.path.insert(0, str(TOOLS))
import mixkit_harvest as mh  # noqa: E402
import rules  # noqa: E402
from common import WORK, read_json, write_json  # noqa: E402

OUT = WORK / "couple-group"

# (clip id, the pose it might show). Chosen from WORK/mixkit/items.json titles; clips already scanned by the harvest
# are not repeated (their frames are in WORK/mixkit/frames).
CLIPS = [
    (930, "couple_cheek_kiss"), (39960, "couple_cheek_kiss"),
    (100763, "couple_back_hug"), (100764, "couple_back_hug"), (100747, "couple_back_hug"),
    (100738, "couple_side_hug"), (100739, "couple_side_hug"), (100743, "couple_side_hug"),
    (4643, "couple_side_hug"), (42237, "couple_forehead_touch"), (42975, "couple_forehead_touch"),
    (42994, "couple_side_hug"), (46525, "couple_back_hug"),
    (100733, "couple_hold_hands_facing"), (100759, "couple_side_hug"),
    (4661, "couple_walk_hands"), (4624, "couple_walk_hands"), (40596, "couple_walk_hands"),
    (42724, "couple_walk_hands"), (45857, "couple_walk_hands"), (4530, "couple_walk_hands"),
    (2396, "couple_walk_toward"), (32744, "couple_walk_toward"), (133, "couple_walk_toward"),
    (4574, "couple_sit_lean"),
    (8900, "group_walk_line"), (23212, "group_walk_line"), (51977, "group_arms_around"),
    (51975, "group_arms_around"), (42575, "group_v_formation"), (6674, "group_arms_around"),
    (25398, "group_arms_around"), (46878, "group_sitting_row"), (23582, "group_jump"), (36913, "group_jump"),
    (21237, "group_jump"),
]


def cmd_fetch(args):
    items = read_json(WORK / "mixkit" / "items.json", {}) or {}
    client = mh.http()
    for d in ("clips", "frames", "video"):
        (OUT / d).mkdir(parents=True, exist_ok=True)
    for cid, pose in CLIPS:
        if args.only and cid not in args.only:
            continue
        dest = OUT / "clips" / f"{cid}.json"
        if dest.exists() and not args.force:
            continue
        it = items.get(str(cid)) or next((v for v in items.values() if int(v["id"]) == cid), None)
        if it is None:
            print(f"{cid}: not in items.json", flush=True)
            continue
        verdict = rules.people_check(it["title"], it["description"], [])
        verdict["reject"] = [r for r in verdict["reject"] if not r.startswith(("public/event", "not a photo"))]
        rec = {"id": cid, "page": it["page"], "title": it["title"], "description": it["description"],
               "preview": it["preview"], "vertical": bool(it.get("vertical")), "for_pose": pose,
               "flags": verdict["flags"], "checked": time.strftime("%Y-%m-%d"), "scanned": False}
        if verdict["reject"]:
            rec["skip"] = "people rules: " + "; ".join(verdict["reject"])
            write_json(dest, rec, indent=1)
            print(f"{cid}: {rec['skip']}", flush=True)
            continue
        page = client.get(it["page"]).decode("utf-8", "replace")
        lic = mh.check_licence(page)
        rec["licence"] = lic
        if not lic["ok"]:
            rec["skip"] = "not Mixkit Stock Video Free License"
            write_json(dest, rec, indent=1)
            print(f"{cid}: licence {lic['panel_key']} / {lic.get('copyright_notice')} -> skip", flush=True)
            continue
        vpath = OUT / "video" / f"{cid}.mp4"
        try:
            client.download(it["preview"], vpath, timeout=120)
            probe = json.loads(subprocess.run(
                ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
                 "-show_entries", "format=duration", "-of", "json", str(vpath)],
                capture_output=True, text=True, check=True).stdout)
            w, h = probe["streams"][0]["width"], probe["streams"][0]["height"]
            dur = float(probe["format"]["duration"])
            tmp = OUT / "video" / f"tmp_{cid}"
            shutil.rmtree(tmp, ignore_errors=True)
            tmp.mkdir()
            subprocess.run(["ffmpeg", "-v", "error", "-i", str(vpath), "-vf", "fps=1", "-frames:v", "24",
                            "-q:v", "3", str(tmp / "f_%03d.jpg")], check=True)
            frames = []
            for k, f in enumerate(sorted(tmp.glob("f_*.jpg"))):
                t = k + 0.5  # fps filter: frame k covers [k, k+1) s, sampled at its middle
                if t > dur:
                    break
                name = f"{cid}_{round(t * 1000):06d}.jpg"
                shutil.move(str(f), OUT / "frames" / name)
                frames.append({"file": name, "t": round(t, 3)})
            shutil.rmtree(tmp, ignore_errors=True)
        finally:
            vpath.unlink(missing_ok=True)
        rec.update({"scanned": True, "video": {"width": w, "height": h, "duration_s": round(dur, 3)},
                    "frames": frames})
        write_json(dest, rec, indent=1)
        print(f"{cid} {it['title'][:60]!r}: {len(frames)} frames ({w}x{h})", flush=True)
    print(f"{client.requests} requests", flush=True)


def cmd_sheet(args):
    from PIL import Image, ImageDraw
    (OUT / "sheets").mkdir(parents=True, exist_ok=True)
    for p in sorted((OUT / "clips").glob("*.json")):
        rec = read_json(p)
        if not rec.get("scanned"):
            continue
        frames = rec["frames"]
        tw, th = (120, 213) if rec["video"]["height"] > rec["video"]["width"] else (240, 135)
        cols = 8
        rows = (len(frames) + cols - 1) // cols
        im = Image.new("RGB", (cols * tw, rows * (th + 14) + 16), (20, 20, 24))
        d = ImageDraw.Draw(im)
        d.text((4, 2), f"{rec['id']} [{rec['for_pose']}] {rec['title'][:110]}", fill=(255, 255, 255))
        for k, f in enumerate(frames):
            x, y = (k % cols) * tw, 16 + (k // cols) * (th + 14)
            t = Image.open(OUT / "frames" / f["file"]).convert("RGB")
            t.thumbnail((tw, th))
            im.paste(t, (x, y + 12))
            d.text((x + 2, y), f"{f['t']:.1f}s", fill=(255, 255, 0))
        im.save(OUT / "sheets" / f"{rec['id']}.jpg", quality=85)
    print(OUT / "sheets")


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("fetch")
    a.add_argument("--only", type=lambda s: {int(x) for x in s.split(",")})
    a.add_argument("--force", action="store_true")
    sub.add_parser("sheet")
    args = ap.parse_args()
    {"fetch": cmd_fetch, "sheet": cmd_sheet}[args.cmd](args)


if __name__ == "__main__":
    main()
