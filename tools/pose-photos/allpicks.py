#!/usr/bin/env python3
"""
allpicks.py: contact sheets of every shipped sample photo next to its pose figure, for review.

    promo/.venv/bin/python tools/pose-photos/allpicks.py [--fragments]

Reads app/src/main/assets/pose_photos/credits.json (or, with --fragments, the selection fragments before the confirm
stage) and tools/pose-photos/app-match.json. Writes WORK/all-picks.jpg (every photo, one grid) and
WORK/all-picks-<n>.jpg (16 photos a sheet, larger). Under each pair: the pose name and id, source and licence, the app
score (shipped photo against the pose as drawn; the flipped pose's score in brackets) and the nearest library pose by
the matcher's rank when it is not the photo's own.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import credits as cr  # noqa: E402
import posematch as pm  # noqa: E402
from sheets import draw_figure, font  # noqa: E402

WORK = Path(os.environ.get("POSE_PHOTOS_WORK", "/home/user/poseit-photos-work"))
BG, CELL_BG, FG, DIM, WARN = (14, 14, 18), (30, 32, 40), (240, 240, 240), (170, 170, 180), (255, 170, 60)


def figure_cell(pose, w, h):
    from PIL import Image, ImageDraw
    im = Image.new("RGB", (w, h), CELL_BG)
    d = ImageDraw.Draw(im)
    l, t, r, b = pm.fit_frame(pose["frameAspect"], w / h)
    for fig in pose["figures"]:
        def to_px(p, l=l, t=t, r=r, b=b):
            return ((l + p[0] * (r - l)) * w, (t + p[1] * (b - t)) * h)
        draw_figure(d, {j: tuple(v) for j, v in fig["joints"].items()}, to_px, width=max(3, w // 60))
    d.rectangle([l * w, t * h, r * w - 1, b * h - 1], outline=(80, 80, 92))
    return im


def photo_cell(path, w, h):
    from PIL import Image
    src = Image.open(path).convert("RGB")
    s = min(w / src.width, h / src.height)
    src = src.resize((max(1, round(src.width * s)), max(1, round(src.height * s))), Image.LANCZOS)
    im = Image.new("RGB", (w, h), CELL_BG)
    im.paste(src, ((w - src.width) // 2, (h - src.height) // 2))
    return im


def entries(use_fragments: bool):
    poses = {p["id"]: p for p in cr.load_poses()}
    match = cr.load_match()
    order = [p["id"] for p in cr.load_poses()]
    if use_fragments:
        picks = cr.load_picks()
        rows = {k: {"photo": Path(v["photo"]).name, "source": v["source"], "licence": v["licence"]["name"],
                    "mirrored": v.get("mirrored")} for k, v in picks.items() if v.get("photo")}
    else:
        rows = json.loads((cr.PHOTOS / "credits.json").read_text())
    return [(poses[k], rows[k], match.get(k)) for k in order if k in rows]


def sheet(items, cell_w, cell_h, cols, title, out):
    from PIL import Image, ImageDraw
    gap, text_h, head = 10, 58 if cell_w >= 200 else 50, 44
    pair_w = 2 * cell_w + 4
    rows = (len(items) + cols - 1) // cols
    W = gap + cols * (pair_w + gap)
    H = head + rows * (cell_h + text_h + gap) + gap
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    f_t, f_n, f_s = font(22), font(14 if cell_w >= 200 else 12), font(12 if cell_w >= 200 else 10)
    d.text((gap, 12), title, fill=FG, font=f_t)
    for k, (pose, c, m) in enumerate(items):
        x = gap + (k % cols) * (pair_w + gap)
        y = head + (k // cols) * (cell_h + text_h + gap)
        im.paste(figure_cell(pose, cell_w, cell_h), (x, y))
        im.paste(photo_cell(cr.PHOTOS / c["photo"], cell_w, cell_h), (x + cell_w + 4, y))
        score = m.get("appScore") if m else None
        flip = ((m or {}).get("flipped") or {}).get("score")
        near = (m or {}).get("nearestPose")
        own = near == pose["id"]
        s_txt = "no score" if score is None else f"app {score:.2f}" + (f" (flip {flip:.2f})" if flip is not None else "")
        lines = [f"{pose['name']}  [{pose['id']}]",
                 f"{c['source']}, {c['licence']}" + ("  mirrored" if c.get("mirrored") else ""),
                 s_txt + ("" if own or not near else f"  nearest: {near}")]
        low = score is None or score < 0.5 or (near and not own)
        d.text((x, y + cell_h + 3), lines[0][:60], fill=FG, font=f_n)
        d.text((x, y + cell_h + 21), lines[1][:72], fill=DIM, font=f_s)
        d.text((x, y + cell_h + 36), lines[2][:72], fill=WARN if low else DIM, font=f_s)
    im.save(out, quality=86)
    print("wrote", out, im.size, flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--fragments", action="store_true")
    ap.add_argument("--per-sheet", type=int, default=16)
    args = ap.parse_args()
    items = entries(args.fragments)
    WORK.mkdir(parents=True, exist_ok=True)
    for old in WORK.glob("all-picks-*.jpg"):
        old.unlink()
    n = len(items)
    sheet(items, 150, 200, 6, f"Pose photos: all {n} shipped photos (left: the pose as drawn, right: the photo)",
          WORK / "all-picks.jpg")
    for i in range(0, n, args.per_sheet):
        part = items[i:i + args.per_sheet]
        sheet(part, 240, 320, 4, f"Pose photos {i + 1}-{i + len(part)} of {n} (left: the pose as drawn, right: the "
                                 f"shipped photo; orange = low app score or nearer to another pose)",
              WORK / f"all-picks-{i // args.per_sheet + 1}.jpg")


if __name__ == "__main__":
    main()
