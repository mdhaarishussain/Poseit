#!/usr/bin/env python3
"""
review_finalize.py: replacement photos chosen after the review of the shipped set (2026-11-22).

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/review_rescore.py --poses ... --sheets          # look for candidates
    $PY -I tools/pose-photos/picks/sitting-floor_finalize.py frame CLIP T1[,T2]    # licensed 1080p, sharpest frame
    $PY -I tools/pose-photos/picks/review_finalize.py build [--poses id,id]

Same steps and outputs as gaps-r1_finalize.py (crop 3:4, mirror when asked or when the photo matches the flipped
pose, 900 px long side, WebP without metadata, landmarks of the shipped file, posematch score). Credits:
tools/pose-photos/picks/review.json (the last fragment in credits.py's ORDER). Photos removed by the review are listed
in credits.py's DROPPED, not here.

Each clip page's licence panel was read again on 2026-11-22 before its file was fetched: "Download this free stock
video clip for commercial or personal use, under the Mixkit Stock Video Free License" (data-license="videoFree",
schema.org license https://mixkit.co/license/#videoFree), no "Restricted" wording on the page.
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
from common import WORK  # noqa: E402

_spec = importlib.util.spec_from_file_location("gaps_r1_finalize", HERE / "gaps-r1_finalize.py")
f1 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(f1)
_spec2 = importlib.util.spec_from_file_location("gaps_r2_finalize", HERE / "gaps-r2_finalize.py")
f2 = importlib.util.module_from_spec(_spec2)
_spec2.loader.exec_module(f2)

CREDITS = HERE / "review.json"
CHECKED = "2026-11-22"

# "src": "mixkit" -> WORK/picks_dl/<clip>_<ms>.png made by `frame` (hold = the time passed to `frame`).
# "crop_px": [x0, y0, x1, y1] in source pixels (3:4). "mirror": forced.
PICKS: list[dict] = [
    {"pose": "selfie_classic", "src": "mixkit", "clip": 40038, "hold": 0.5, "crop_px": [550, 0, 1360, 1080],
     "mirror": True,
     "reason": "Replaces the close-up from beside the phone (Mixkit 51119), which showed neither the extended arm nor "
               "the whole face. A man in the passenger seat of an open jeep holds his phone out at arm's length above "
               "eye level and smiles into it: the classic arm selfie, phone, arm and face all in frame. A smiling "
               "woman leans in beside him (a two-person selfie; the pose itself is drawn for one). Mirrored so the "
               "phone arm is on the same side as the figure's. Single-person candidates were looked at and fail the "
               "3:4 frame: in Mixkit 41181 ('Woman taking a selfie in the morning', 16:9) the phone and her face are "
               "1,450 px apart, so any 810x1080 crop loses the phone or the face."},
    {"pose": "port_arms_crossed_smile", "src": "mixkit", "clip": 43806, "hold": 1.0, "crop_px": [611, 0, 1421, 1080],
     "reason": "Replaces the veiled fashion model (Mixkit 52269), whose sequinned veil hid the smile. A man in a coral "
               "hoodie stands square to the camera with his arms folded and a relaxed closed-mouth smile, eyes on "
               "the lens, on a plain mint studio backdrop (a painted backdrop, not a chroma-key screen): the "
               "crossed-arm smile, waist-up, sharp and evenly lit."},
    {"pose": "fun_twirl", "src": "mixkit", "clip": 43219, "hold": 12.35, "crop_px": [230, 200, 1070, 1320],
     "reason": "Replaces the woman swinging a flannel shirt overhead while walking (Mixkit 276), which showed no spin. "
               "A barefoot dancer in a black top and long skirt turns on the spot, one arm thrown high and the other "
               "out to the side, the skirt swinging with the turn, in a misty empty hall: a twirl with the arms as "
               "drawn. The heel flick is hidden by the skirt."},
]

# No-photo reasons that change because of the review (a later fragment's reason wins for a pose without a photo).
GAPS: dict[str, str] = {
    "stand_arms_crossed": "The full-body crossed-arms photos found name the person ('Wena Opena, 2021', "
        "'Danielagalliano', 'Claudio-antonioli', 'Kuzzat Altay', 'MBL 35597' = 'Portrait de Marie Béatrice Levaux', a "
        "wrestler's watermarked 'BasedHRAEW2024'), come from a cosplay convention, or show two possibly teenage girls "
        "('Cuba (6959893471)'). 'Slightly Business casual Informal wear - pose 2' is excluded too: its categories name "
        "the model ('Alisa (photo model)'). Mixkit 43806 and 28302 cross their arms but are waist-up.",
}


def wire():
    f1.PICKS = PICKS
    f1.GAPS = GAPS
    f1.CREDITS = CREDITS
    f1.CHECKED = CHECKED
    f1.commons_record = f2.commons_record
    f1.clip_record = f2.clip_record


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    b = sub.add_parser("build")
    b.add_argument("--poses")
    args = ap.parse_args()
    wire()
    only = set(args.poses.split(",")) if args.poses else None
    f1.cmd_build(only)
    # the licence panels were read again today, just before the downloads
    recs = json.loads(CREDITS.read_text())
    for r in recs:
        if r.get("photo") and r.get("source") == "Mixkit":
            r["licence"]["checked"] = CHECKED
            r["licence"]["rechecked"] = ("clip page read again on 2026-11-22 before the 1080p file was fetched: "
                                         "data-license=\"videoFree\", schema.org license "
                                         "https://mixkit.co/license/#videoFree, no 'Restricted' wording")
    CREDITS.write_text(json.dumps(recs, indent=1, ensure_ascii=False) + "\n")


if __name__ == "__main__":
    main()
