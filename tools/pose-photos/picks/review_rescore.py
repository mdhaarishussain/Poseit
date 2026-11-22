#!/usr/bin/env python3
"""
review_rescore.py: rank every image already detected by the earlier rounds against chosen poses, to look for
replacements after a review.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/review_rescore.py --poses selfie_classic,floor_one_knee [--top 30] [--sheets]

Pools (all in the work folder, nothing is downloaded): the first run (WORK/landmarks, metadata from mixkit/clips and
commons/files.json), gap round 1 (WORK/gaps-r1), gap round 2 (WORK/gaps-r2; Commons thumbnails whose licence metadata
was never read are marked "licence unchecked" and must be checked before use) and the standing-leaning scan
(WORK/sl). Mixkit frames are 360p previews: a pick must be re-cut from the clip's licensed download.

Writes WORK/review/candidates/<poseId>.json (the same shape as score.py's) and, with --sheets, WORK/review/sheets/.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
import score as sc  # noqa: E402
from common import WORK, load_poses, read_json, write_json  # noqa: E402

OUT = WORK / "review"


def mixkit_meta(clips_dir: Path, prefix: str, frames_dir: Path) -> dict:
    meta = {}
    for p in clips_dir.glob("*.json"):
        c = read_json(p)
        if not c or not c.get("scanned") or not (c.get("licence") or {}).get("ok"):
            continue
        for f in c.get("frames", []):
            stem = Path(f["file"]).stem
            meta[prefix + stem] = {
                "source": "Mixkit", "clip": c["id"], "time_s": f["t"], "title": c["title"], "page": c["page"],
                "description": c.get("description", ""), "image": str(frames_dir / f"{stem}.jpg"),
                "licence": {"name": c["licence"]["panel_licence"], "url": "https://mixkit.co/license/#videoFree"},
                "flags": list(c.get("flags", [])) + ([] if f.get("still", True) else ["moving (not a hold)"]),
                "author": c["licence"].get("author")}
    return meta


def commons_meta(files_json: Path, prefix: str) -> dict:
    meta = {}
    for pid, r in (read_json(files_json, {}) or {}).items():
        lic = r["licence"]
        meta[prefix + pid] = {
            "source": "Wikimedia Commons", "title": r["title"], "page": r["page"], "file_url": r["file_url"],
            "description": r.get("description", ""), "author": lic.get("artist"),
            "licence": {"name": lic.get("short_name"), "url": lic.get("url"),
                        "restrictions": lic.get("restrictions")},
            "categories": r.get("categories"),
            "flags": list(r["verdict"]["flags"]), "ok": r["ok"], "pageid": int(pid)}
    return meta


def sl_meta() -> dict:
    meta = {}
    for cid, c in (read_json(WORK / "sl" / "scan.json", {}) or {}).items():
        if not (c.get("licence") or {}).get("ok"):
            continue
        meta[f"sl:{cid}"] = {"source": "Mixkit", "clip": int(cid), "title": c["title"], "page": c["page"],
                             "description": c.get("description", ""),
                             "licence": {"name": c["licence"]["panel_licence"],
                                         "url": "https://mixkit.co/license/#videoFree"},
                             "flags": list(c.get("flags", [])), "author": c["licence"].get("author")}
    return meta


def pools():
    """(landmark files, meta lookup) per pool."""
    first = sc.load_meta()
    r1 = {**mixkit_meta(WORK / "gaps-r1" / "mixkit" / "clips", "gmixkit:", WORK / "gaps-r1" / "mixkit" / "frames"),
          **commons_meta(WORK / "gaps-r1" / "commons" / "files.json", "gcommons:")}
    r2 = {**mixkit_meta(WORK / "gaps-r2" / "mixkit" / "clips", "gmixkit:", WORK / "gaps-r2" / "mixkit" / "frames"),
          **commons_meta(WORK / "gaps-r2" / "commons" / "files.json", "r2commons:")}
    hits = read_json(WORK / "gaps-r2" / "commons" / "hits.json", {}) or {}
    slm = sl_meta()

    def r2_lookup(rid):
        if rid in r2:
            return r2[rid]
        if rid.startswith("r2commons:"):
            pid = rid.split(":", 1)[1]
            h = hits.get(pid)
            if h:
                return {"source": "Wikimedia Commons", "title": h["title"], "description": h.get("snippet", ""),
                        "page": "https://commons.wikimedia.org/wiki/" + h["title"].replace(" ", "_"),
                        "licence": {"name": "licence unchecked"}, "flags": ["licence unchecked"],
                        "pageid": int(pid)}
        return None

    def sl_lookup(rid):
        stem = rid.split(":", 1)[1]
        cid = stem.split("_")[0]
        m = slm.get(f"sl:{cid}")
        if not m:
            return None
        img = WORK / "sl" / "frames" / f"{stem}.jpg"
        return {**m, "time_s": int(stem.split("_")[1]) / 1000, "image": str(img)}

    yield sorted((WORK / "landmarks").glob("*.jsonl")), first.get
    yield sorted((WORK / "gaps-r1" / "landmarks").glob("*.jsonl")), r1.get
    yield sorted((WORK / "gaps-r2" / "landmarks").glob("*.jsonl")), r2_lookup
    yield [WORK / "sl" / "landmarks.jsonl"], sl_lookup


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--poses", required=True)
    ap.add_argument("--top", type=int, default=30)
    ap.add_argument("--per-clip", type=int, default=2)
    ap.add_argument("--sheets", action="store_true")
    args = ap.parse_args()
    want = args.poses.split(",")
    poses = [p for p in load_poses() if p["id"] in want]
    per = {p["id"]: [] for p in poses}
    metas = {}
    seen = set()
    for files, lookup in pools():
        for lf in files:
            for line in lf.read_text().splitlines():
                try:
                    rec = json.loads(line)
                except json.JSONDecodeError:
                    continue
                m = lookup(rec["id"])
                if m is None or m.get("ok") is False or not rec.get("people"):
                    continue
                img = rec.get("image") or m.get("image")
                if not img or not Path(img).exists() or rec["id"] in seen:
                    continue
                seen.add(rec["id"])
                metas[rec["id"]] = m
                for r in sc.score_image(rec, poses):
                    r.update({"id": rec["id"], "image": img, "w": rec["w"], "h": rec["h"]})
                    per[r["pose"]].append(r)
    (OUT / "candidates").mkdir(parents=True, exist_ok=True)
    for pose in poses:
        cat_re, own_re = sc.text_terms(pose)
        for r in per[pose["id"]]:
            m = metas[r["id"]]
            text = (m.get("title") or "") + " " + (m.get("description") or "")
            r["text_match"] = [k for k, rx in (("category", cat_re), ("name", own_re)) if rx and rx.search(text)]
            r["rank"] = round(r["rank"] + 0.08 * len(r["text_match"]), 4)
        lst = sorted(per[pose["id"]], key=lambda r: -r["rank"])
        kept, per_clip = [], {}
        for r in lst:
            m = metas[r["id"]]
            if m["source"] == "Mixkit":
                if per_clip.get(m["clip"], 0) >= args.per_clip:
                    continue
                per_clip[m["clip"]] = per_clip.get(m["clip"], 0) + 1
            kept.append({**r, **{k: v for k, v in m.items() if k not in ("ok", "image")}})
            if len(kept) >= args.top:
                break
        elig = [r for r in lst if r["eligible"]]
        write_json(OUT / "candidates" / f"{pose['id']}.json", {
            "pose": {"id": pose["id"], "name": pose["name"], "category": pose["category"],
                     "people": len(pose["figures"]), "frameAspect": pose["frameAspect"]},
            "scored": len(lst), "eligible": len(elig), "candidates": kept}, indent=1)
        print(f"{pose['id']}: {len(lst)} scored, {len(elig)} eligible", flush=True)
    if args.sheets:
        import sheets as sh
        by_id = {p["id"]: p for p in load_poses()}
        for pose in poses:
            sh.make_sheet(OUT / "candidates" / f"{pose['id']}.json", by_id, args.top, OUT / "sheets")
        print(OUT / "sheets", flush=True)


if __name__ == "__main__":
    main()
