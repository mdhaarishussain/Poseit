#!/usr/bin/env python3
"""
sheets.py: one contact sheet per pose: the pose's figure (drawn from poses.json joints) next to its top candidates,
each with the placed guide drawn over the photo and its score, source and licence printed underneath.

    python3 tools/pose-photos/sheets.py [--top 8] [--poses id,id]

Reads WORK/candidates/<poseId>.json (score.py); writes WORK/sheets/<poseId>.jpg. Cells are 300 x 400 (3:4, the
template's frame); a candidate is shown cropped to the pose's own frame placed on it (score.py crop_px: the crop a
final photo would use; dark padding where that frame leaves the photo) and mirrored when
the match was with the flipped pose, so it reads the same way as the figure. Green text = eligible (people count and
framing fit the pose), orange = not. Up to 3 of the 8 places go to the best candidates whose title or description
names the pose or its category ("T" in the caption), even when the matcher ranks them lower.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import posematch as pm  # noqa: E402
from common import WORK, load_poses, read_json  # noqa: E402

CELL_W, CELL_H, TEXT_H, PAD = 300, 400, 64, 8
BONES = [("LEFT_SHOULDER", "RIGHT_SHOULDER"), ("LEFT_HIP", "RIGHT_HIP"), ("LEFT_SHOULDER", "LEFT_HIP"),
         ("RIGHT_SHOULDER", "RIGHT_HIP"), ("LEFT_SHOULDER", "LEFT_ELBOW"), ("LEFT_ELBOW", "LEFT_WRIST"),
         ("LEFT_WRIST", "LEFT_HAND"), ("RIGHT_SHOULDER", "RIGHT_ELBOW"), ("RIGHT_ELBOW", "RIGHT_WRIST"),
         ("RIGHT_WRIST", "RIGHT_HAND"), ("LEFT_HIP", "LEFT_KNEE"), ("LEFT_KNEE", "LEFT_ANKLE"),
         ("LEFT_ANKLE", "LEFT_FOOT"), ("RIGHT_HIP", "RIGHT_KNEE"), ("RIGHT_KNEE", "RIGHT_ANKLE"),
         ("RIGHT_ANKLE", "RIGHT_FOOT")]
LEFT_C, RIGHT_C, MID_C = (64, 160, 255), (255, 140, 60), (240, 240, 240)


def font(size):
    from PIL import ImageFont
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/dejavu/DejaVuSans.ttf"):
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def draw_figure(draw, pts, to_px, width=4, head=True):
    for a, b in BONES:
        if a in pts and b in pts:
            c = LEFT_C if a.startswith("LEFT") and b.startswith("LEFT") else \
                RIGHT_C if a.startswith("RIGHT") and b.startswith("RIGHT") else MID_C
            draw.line([to_px(pts[a]), to_px(pts[b])], fill=c, width=width)
    if head and "NOSE" in pts and "LEFT_SHOULDER" in pts and "RIGHT_SHOULDER" in pts:
        nx, ny = to_px(pts["NOSE"])
        sx = abs(to_px(pts["LEFT_SHOULDER"])[0] - to_px(pts["RIGHT_SHOULDER"])[0])
        r = max(6, sx * 0.3)
        draw.ellipse([nx - r, ny - r, nx + r, ny + r], outline=MID_C, width=max(2, width - 1))


def pose_cell(pose):
    from PIL import Image, ImageDraw
    im = Image.new("RGB", (CELL_W, CELL_H), (24, 26, 32))
    d = ImageDraw.Draw(im)
    fa = pose["frameAspect"]
    l, t, r, b = pm.fit_frame(fa, CELL_W / CELL_H)
    for fig in pose["figures"]:
        def to_px(p, l=l, t=t, r=r, b=b):
            return ((l + p[0] * (r - l)) * CELL_W, (t + p[1] * (b - t)) * CELL_H)
        pts = {j: tuple(v) for j, v in fig["joints"].items()}
        draw_figure(d, pts, to_px, width=6)
    d.rectangle([l * CELL_W, t * CELL_H, r * CELL_W - 1, b * CELL_H - 1], outline=(90, 90, 100))
    return im


def candidate_cell(c, pose):
    from PIL import Image, ImageDraw
    src = Image.open(c["image"]).convert("RGB")
    W, H = src.size
    # draw the placed guide (template in scene space with the solved transform) on a copy
    figures = [{"points": {j: tuple(v) for j, v in f["joints"].items()}, "vis": f.get("visibility", {}),
                "unit": f["unit"]} for f in pose["figures"]]
    tr = c["transform"]
    scene = pm.template_to_scene(figures, pose["frameAspect"], W / H, tr["scale"], (tr["offsetX"], tr["offsetY"]),
                                 c["mirror_photo"])
    over = src.copy()
    d = ImageDraw.Draw(over)
    for fig in scene:
        draw_figure(d, fig["points"], lambda p: (p[0] * W, p[1] * H), width=max(2, round(W / 180)), head=False)
    src = Image.blend(src, over, 0.55)
    # crop to the pose's own frame placed on the photo (score.py crop_px); dark padding where it leaves the photo
    x0, y0, x1, y1 = c.get("crop_px") or [0, 0, W, H]
    canvas = Image.new("RGB", (max(1, x1 - x0), max(1, y1 - y0)), (40, 40, 48))
    canvas.paste(src, (-x0, -y0))
    crop = canvas.resize((CELL_W, CELL_H))
    if c["mirror_photo"]:
        crop = crop.transpose(Image.FLIP_LEFT_RIGHT)
    return crop


def make_sheet(cfile: Path, poses_by_id: dict, top: int, out_dir: Path):
    from PIL import Image, ImageDraw
    data = read_json(cfile)
    pose = poses_by_id[data["pose"]["id"]]
    # the best by rank, but keep up to 3 places for the best candidates whose text names the pose or its category
    # (score.py text_match): for poses with few segments (selfies, portraits) the matcher alone over-rates any
    # waist-up frame with the arms down
    allc = data["candidates"]
    texty = [c for c in allc if c.get("text_match")][:3]
    cands = [c for c in allc if c not in texty][: max(0, top - len(texty))]
    cands = sorted(cands + texty, key=lambda c: -c["rank"])
    cols = 3
    n = 1 + len(cands)
    rows = (n + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (CELL_W + PAD) + PAD, 40 + rows * (CELL_H + TEXT_H + PAD) + PAD), (12, 12, 16))
    d = ImageDraw.Draw(sheet)
    f_title, f_small = font(20), font(12)
    d.text((PAD, 10), f"{pose['name']}  ({pose['id']}, {pose['category']}, {len(pose['figures'])} "
                      f"{'person' if len(pose['figures']) == 1 else 'people'})  - {data['eligible']} eligible of "
                      f"{data['scored']} scored", fill=(255, 255, 255), font=f_title)
    for k in range(n):
        x = PAD + (k % cols) * (CELL_W + PAD)
        y = 40 + (k // cols) * (CELL_H + TEXT_H + PAD)
        if k == 0:
            sheet.paste(pose_cell(pose), (x, y))
            d.text((x, y + CELL_H + 4), "the pose as drawn in the app\nblue = left side, orange = right side",
                   fill=(200, 200, 200), font=f_small)
            continue
        c = cands[k - 1]
        try:
            sheet.paste(candidate_cell(c, pose), (x, y))
        except Exception as e:  # missing file etc.
            d.text((x + 10, y + 10), f"cannot draw: {e}", fill=(255, 80, 80), font=f_small)
        col = (120, 230, 120) if c["eligible"] else (255, 170, 60)
        lic = (c.get("licence") or {}).get("name") or "?"
        src = c["source"] + (f" #{c.get('clip')} @{c.get('time_s')}s" if c["source"] == "Mixkit" else "")
        flags = "; ".join(c.get("flags") or [])
        lines = [f"#{k} score {c['score']:.3f} fit {c['fit']:.2f} cov {c.get('tcov', 0):.2f} "
                 f"ppl {c['people']}/{c['figures']} crop {c.get('crop_h_px', 0)}px",
                 (f"{src}" + ("  MIRRORED" if c["mirror_photo"] else "") + ("  T" if c.get("text_match") else ""))[:52],
                 f"{lic}"[:52], flags[:52]]
        d.multiline_text((x, y + CELL_H + 3), "\n".join(lines), fill=col, font=f_small, spacing=2)
    out_dir.mkdir(parents=True, exist_ok=True)
    sheet.save(out_dir / f"{pose['id']}.jpg", quality=85)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--top", type=int, default=8)
    ap.add_argument("--poses")
    args = ap.parse_args()
    poses_by_id = {p["id"]: p for p in load_poses()}
    want = set(args.poses.split(",")) if args.poses else None
    for cfile in sorted((WORK / "candidates").glob("*.json")):
        if cfile.name == "index.json" or (want and cfile.stem not in want):
            continue
        make_sheet(cfile, poses_by_id, args.top, WORK / "sheets")
    print("sheets written to", WORK / "sheets", flush=True)


if __name__ == "__main__":
    main()
