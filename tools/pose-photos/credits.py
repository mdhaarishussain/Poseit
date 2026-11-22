#!/usr/bin/env python3
"""
credits.py: merge the selection fragments (tools/pose-photos/picks/*.json) and the app-matcher check
(tools/pose-photos/app-match.json, written by pose-core PosePhotoMatch) into the app's photo credits.

    python3 tools/pose-photos/credits.py            # writes credits.json and coverage.json
    python3 tools/pose-photos/credits.py --check    # only report, write nothing

Outputs
  app/src/main/assets/pose_photos/credits.json   keyed by pose id, one entry per shipped photo:
      photo, source, title, pageUrl, fileUrl, author, licence, licenceUrl, attribution (the short line to show under
      the photo), changes, mirrored, appScore (PoseMatcher, the shipped photo against its own pose as drawn), note
      (when the score does not measure the photo: e.g. the detector separates only one of two people)
  tools/pose-photos/coverage.json                 all 99 poses: photo or not, source, licence, app score, nearest
      pose, the selection's reason, and the confirm stage's verdict

Fragment merge: a record with a photo wins; otherwise the latest round's reason (base fragments, then gaps-r1, then
gaps-r2). Review decisions of the confirm stage (DROPPED, NOTES below) are applied on top.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parent.parent
PHOTOS = REPO / "app/src/main/assets/pose_photos"
POSES_JSON = REPO / "promo/public/poses.json"
ORDER = ["sitting-floor", "standing-leaning", "portrait-selfie-fun", "couple-group", "gaps-r1", "gaps-r2", "review"]

# Confirm stage (pose-core PosePhotoMatch + a second look at every flagged photo, 2026-11-21). A photo is flagged when
# its app score is below LOW or the matcher ranks another library pose nearer than its own.
LOW = 0.5

# Photos removed from the app, and why.
DROPPED: dict[str, str] = {
    "couple_piggyback": "Dropped at the confirm stage: the Mixkit frame ('Portrait of a happy couple on a pink "
                        "background', waist-up) shows her arms round his neck from behind, which reads as a back hug "
                        "(already shown for couple_back_hug); nothing in the frame or the clip's text shows a "
                        "piggyback ride, and the detector separates only one person. No other candidate shows one.",
    "fun_superhero": "Dropped at the confirm stage: the only candidate with the stance is a Commons photo of a member "
                     "of the public at an event ('Man watching the total solar eclipse at Southern Illinois "
                     "University', CC BY-SA 4.0), not a stock model, head tipped back to the sky in eclipse glasses; "
                     "the people rules skip files from events. The next candidates are a performer in a leotard, a "
                     "studio shoot with the photographer in front of the model, and a near-silhouette under a pink "
                     "filter (Mixkit 1236, hi-res frames checked).",
    # Review of the shipped set (2026-11-22): every photo looked at again beside its figure and its source record.
    # Replacements are in picks/review.json (review_finalize.py); these were removed and nothing better was found
    # (review_rescore.py over all ~8,100 detected frames and photos, plus title searches of every cached record).
    "selfie_mirror_two_hands": "Removed at the review: the man wears tight black shorts that read as boxer briefs, "
                               "barefoot in a kitchen (the rules exclude underwear), the file's own title calls the "
                               "shot unflattering ('... looking strangely due to status of high concentration'), and "
                               "an Apple logo shows on the phone. No other full-length two-hand mirror selfie among "
                               "the candidates (the mirror files found are close-ups, a fisheye, a funfair mirror or "
                               "a wedding-dress fitting).",
    "couple_walk_toward": "Removed at the review: the licence cannot be verified. Uploader Shixart1985 marks it 'Own "
                          "work', CC BY 2.0, with no outside source and no licence review; the cached records hold "
                          "about 24 such uploads of many different models and places in stock-caption style, a "
                          "pattern of re-uploaded stock photos. Commons' API and upload server answered HTTP 429 to "
                          "every check of the uploader's talk page and the file's history. Replacements looked at: "
                          "the Mixkit couple clips that walk toward the camera (40584, 2402, 133, 32744) are 16:9 and "
                          "thigh-up, too wide for a 3:4 frame, and 2402 has a bustier top; 'Coney Island couple "
                          "(Unsplash)' has large brand signs and children in the background.",
    "selfie_high_angle": "Removed at the review: same uploader and licence doubt as couple_walk_toward (Shixart1985, "
                         "'Own work', CC BY 2.0, generated-sounding caption); the face was also out of focus and the "
                         "app score 0.08. The only other raised-phone selfie is a frame of a clip that fails the 3:4 "
                         "frame (Mixkit 41181); the Commons selfies found are passers-by or self-portraits.",
    "pro_blazer_pocket": "Removed at the review: no hand is in a pocket (one hand holds the other forearm), it was a "
                         "second frame of the clip used for stand_crossed_ankles, and the red-sequinned face veil "
                         "reads like blood at thumbnail size. No candidate with a hand in a blazer pocket.",
    "stand_crossed_ankles": "Removed at the review: the ankles are not crossed (the boots stand side by side, one half "
                            "a step ahead), the model also filled pro_blazer_pocket and port_arms_crossed_smile, and "
                            "the red-sequinned face veil reads like blood at thumbnail size. The other candidates "
                            "(Mixkit 34404, a sheer dress; 1236 under a pink filter) do not cross the ankles either.",
    "floor_one_knee": "Removed at the review: a marriage proposal (ring box held out, partner cropped away, mouth "
                      "open mid-speech), not a pose demonstration, and the kneeling leg was lost in the dark (score "
                      "0.15). The other kneelers are a photographer crouched in front of a model (Mixkit 34405) and a "
                      "gardener hidden by plants (Mixkit 969).",
    "pro_arms_crossed": "Removed at the review: the file's categories name the model ('Alisa (photo model)'), which "
                        "the named-person rule excludes (the same rule removed other crossed-arms photos and "
                        "'Isabela Salvador' for selfie_hand_on_cheek). The file is also in 'Full consent', so an "
                        "owner who decides to allow consenting named models could restore it (record kept in "
                        "picks/gaps-r2.json). No other full-body confident crossed arms.",
    "lean_railing_back": "Removed at the review: the clip calls her a woman, but in the full-resolution frame she could "
                         "be under 18, and the rules skip anything that could show a minor. No other elbows-on-the-"
                         "rail lean among the candidates.",
    "port_shoulder_touch": "Removed at the review: the Commons file is in the category 'Seduction (facial "
                           "expression)'; the rules exclude seductive and glamour framing (the text rules now catch "
                           "'seduction'). No other hand-to-shoulder portrait among the candidates.",
    "fun_victory": "Removed at the review: motion blur on the face and hands, and a low-cut top with visible cleavage "
                   "at the centre of the frame (the rules exclude cleavage). The other arms-up frames are a performer "
                   "on a stage ('Jump for joy', an event) and dancing clips with one arm up.",
    # Second look before the photos went into the app's screens (2026-11-22).
    "selfie_classic": "Removed at the second look: three people taking a selfie in a car (the review's replacement, "
                      "Mixkit 40038 at 0.367 s), while the pose is a solo selfie with the arm extended, so the photo "
                      "misleads. The earlier frame was a close-up from beside the phone with no extended arm. No "
                      "other solo arm's-length selfie among the candidates.",
}

# Flagged photos kept after the second look: what the photo shows and why the score does not measure it. Shown in
# coverage.json and the docs, and as "note" in credits.json. Part scores are PosePhotoMatch's (app-match.json).
NOTES: dict[str, str] = {
    "pro_headshot": "Kept: a textbook headshot (suit, smile, eyes on the lens). The photo ends at the chest (the full "
                    "height of the 16:9 source frame) while the drawn frame reaches the hips, so most segments the "
                    "figure needs are outside the photo and the matcher's coverage rule gives 0; head and torso parts "
                    "score 1.0.",
    "couple_cheek_kiss": "Kept: she kisses his cheek, both smiling (he looks down, not at the camera). A tight close-up "
                         "of the two faces: the detector separates one person of two, so the score (0.01) does not "
                         "measure the photo.",
    "stand_hand_in_hair": "Kept: standing tall with one hand in the hair and the other arm down, as drawn. The long "
                          "dress hides the legs from the detector (no leg segment scored, so coverage is low) and the "
                          "raised elbow points forward rather than out (left arm 0.0).",
    "floor_side_lie": "Kept: lying on her side propped on the elbow, head in hand, knees bent, head to the right as "
                      "drawn. With the body horizontal the head and shin directions are very sensitive (head 0.07, "
                      "lower leg 0.05); its own pose ranks second of 82.",
    "couple_forehead_touch": (
        "Kept: foreheads together, eyes closed, hands joined between them, as drawn; thigh-up. The two "
        "overlapping bodies confuse the detector (one head scores 0.0). A flat chroma-green studio "
        "background (as couple_hold_hands_facing, same couple and shoot)."),
    "sit_elbows_on_knees": (
        "Kept: sitting on steps with the forearms on the knees and the hands clasped. Seen from the front"
        " while the figure is drawn side-on, so the torso and thighs point differently in 2D (torso "
        "0.28); the nearest pose by rank is the street squat, a similar folded shape. Licence: CC0 as "
        "copied from Pexels; the Commons copy is dated 2017-04-04, when Pexels still published under CC0 "
        "(it adopted its own licence later); the earliest archived Pexels page (2019) could not be read "
        "from here (HTTP 403). Nike marks on both shoes (incidental)."),
    "lean_crossed_legs": "Kept: leaning back on a car with the ankles crossed. Dark clothes against the dark car hide "
                         "an arm and a leg from the detector (not scored, so coverage is low).",
    "couple_walk_hands": "Kept: walking hand in hand, free arms swinging; seen from behind and hip-up instead of "
                         "side-on full length.",
    "sit_crossed_legs": (
        "Kept: seated upright with the legs crossed at the knee as drawn (legs 1.0 / 0.81); both hands "
        "are behind the head instead of on the knee (arms 0.0), and the raised arms dominate the photo."),
    "group_arms_around": "Kept: seven women in a line with their arms round each other's shoulders and waists (the pose "
                         "says it fits any number of people); the detector returns four people, so the score covers "
                         "three figures on four of seven.",
    "couple_sit_lean": (
        "Kept: on a bench she rests her head on his shoulder, the gesture the pose describes. A head-and-"
        "shoulders close-up (the seated legs are out of frame) where the detector separates one person of"
        " two, so the score does not measure the photo. Same actors and production as "
        "couple_forehead_touch and couple_hold_hands_facing (Mixkit 100733/100759/100799): one couple in "
        "3 of the 6 couple photos."),
    "couple_back_hug": "Kept: he hugs her from behind, both smiling at the camera; waist-up. The detector separates "
                       "one person of two, so the score does not measure the photo.",
    "sit_one_knee_up": "Kept: on stairs with one knee drawn up high and the other leg reaching down (legs 1.0 / 0.45, "
                       "torso 1.0); one hand rests on her shoulder instead of the knee. Its own pose ranks third, "
                       "within 0.03 of two other seated poses.",
    "floor_street_squat": (
        "Kept: a deep flat-footed squat with the forearms on the knees; its own pose is the nearest of "
        "all 82 single-person poses. The shipped frame shows the young man in the red shirt (the coach "
        "who checks his watch), not the woman named in the clip's title 'Young woman getting into "
        "position for sprinting'; Mixkit's description of clip 32800 mentions him ('a man telling her the"
        " time'). Page and file URLs are that clip's."),
    "selfie_mirror": (
        "Kept: a full-length mirror selfie; the free hand holds a woven hat and a wallet at the side "
        "instead of resting on the hip (arm 0.49). Lettering on the jacket chest and a mirrored EXIT sign"
        " are part of the scene. A self-portrait by the uploader (category 'ShiminUfesoj photos'), CC0."),
    "floor_cross_legged": "Kept: cross-legged on grass with the hands on the knees (three-quarter view); the crossed "
                          "shins are hard for the detector (leg 0.45).",
    "lean_foot_on_wall": "Kept: leaning on a wall with one foot planted on it behind him (legs 0.77 / 1.0); a phone in "
                         "both hands at the chest where the figure's arms hang (arms 0.55).",
    "walk_toward_camera": "Kept: walking straight at the camera; at this angle the stride is short, so standing and "
                          "kerb-step poses rank close.",
    "couple_hold_hands_facing": "Kept: facing each other an arm's length apart holding both hands, as drawn; thigh-up "
                                "rather than full length.",
    "stand_collar_touch": "Kept: full length, one hand at the neck and collarbone, the other arm down, as drawn (score "
                          "0.92); a walking pose with one arm bent to the chest (walk_coffee_run) ranks just above it.",
    "fun_disco_point": "Kept: one arm pointing high, the other hand on the hip, weight on one leg, as drawn (arms 1.0, "
                       "score 0.95); the high-angle selfie and door-frame lean (one arm up) rank just above it.",
    "stand_hand_on_hip": (
        "Kept: one hand on the hip, the other arm down, as drawn; a fashion shoot in a boutique. The legs"
        " stand wider than the figure's relaxed stance (it does not fit stand_power_pose, which needs "
        "both hands on the hips). The jumpsuit has a deep neckline; nothing is exposed."),
    "fun_twirl": (
        "Kept: mid-turn with one arm thrown high and the other out to the side, as drawn; the long skirt "
        "hides the heel flick, so the legs score low."),
}
PORTRAIT_NOTE = ("Kept: portrait and selfie templates need few segments (no legs), so several of them score alike on "
                 "a waist-up photo; its own pose scores {score:.2f} and ranks {rank} of {n}.")

LICENCE_URLS = {  # canonical deed URLs (https, no deed.xx suffix)
    "CC0": "https://creativecommons.org/publicdomain/zero/1.0/",
    "CC BY 2.0": "https://creativecommons.org/licenses/by/2.0/",
    "CC BY 3.0": "https://creativecommons.org/licenses/by/3.0/",
    "CC BY 4.0": "https://creativecommons.org/licenses/by/4.0/",
    "CC BY-SA 2.0": "https://creativecommons.org/licenses/by-sa/2.0/",
    "CC BY-SA 3.0": "https://creativecommons.org/licenses/by-sa/3.0/",
    "CC BY-SA 4.0": "https://creativecommons.org/licenses/by-sa/4.0/",
    "Mixkit Stock Video Free License": "https://mixkit.co/license/#videoFree",
}
LICENCE_FILES = {  # full texts shipped in app/src/main/assets/licenses/
    "CC0": "CC0-1.0.txt",
    "CC BY 2.0": "CC-BY-2.0.txt",
    "CC BY 3.0": "CC-BY-3.0.txt",
    "CC BY 4.0": "CC-BY-4.0.txt",
    "CC BY-SA 2.0": "CC-BY-SA-2.0.txt",
    "CC BY-SA 3.0": "CC-BY-SA-3.0.txt",
    "CC BY-SA 4.0": "CC-BY-SA-4.0.txt",
    "Mixkit Stock Video Free License": "Mixkit-Stock-Video-Free-License.txt",
}


def load_poses() -> list[dict]:
    return json.loads(POSES_JSON.read_text())["poses"]


def load_picks() -> dict[str, dict]:
    """pose id -> merged selection record (photo or the reason there is none)."""
    out: dict[str, dict] = {}
    for name in ORDER:
        for r in json.loads((TOOLS / "picks" / f"{name}.json").read_text()):
            r = {**r, "fragment": name}
            prev = out.get(r["pose_id"])
            if prev is None or r.get("photo") or not prev.get("photo"):
                out[r["pose_id"]] = r
    return out


def load_match() -> dict[str, dict]:
    p = TOOLS / "app-match.json"
    if not p.exists():
        return {}
    return {r["poseId"]: r for r in json.loads(p.read_text())["photos"]}


def flag_of(pose_id: str, m: dict | None) -> str | None:
    """Why the confirm stage looked at the photo again, or None."""
    if m is None or m.get("appScore") is None:
        return "no app score (the detector finds no usable person)"
    why = []
    if m["appScore"] < LOW:
        why.append(f"app score {m['appScore']:.2f} < {LOW}")
    if m.get("people", 0) < m.get("figures", 0):
        why.append(f"the detector separates {m['people']} of {m['figures']} people")
    elif m.get("nearestPose") and m["nearestPose"] != pose_id:
        why.append(f"nearest pose by rank is {m['nearestPose']} (own pose ranks {m.get('ownRankAmongPoses')} of "
                   f"{m.get('posesRanked')})")
    return "; ".join(why) or None


def flag_short(r: dict) -> str:
    out = []
    if r.get("appScore") is None or r["appScore"] < LOW:
        out.append("low score")
    if (r.get("peopleDetected") or 0) < r["people"]:
        out.append(f"{r.get('peopleDetected') or 0} of {r['people']} people detected")
    elif r.get("nearestPose") and r["nearestPose"] != r["id"]:
        out.append(f"nearest: {r['nearestPose']}")
    return ", ".join(out)


def note_of(pose_id: str, m: dict | None) -> str | None:
    if pose_id in NOTES:
        return NOTES[pose_id]
    if flag_of(pose_id, m) and m and m.get("appScore") is not None:
        return PORTRAIT_NOTE.format(score=m["appScore"], rank=m.get("ownRankAmongPoses"), n=m.get("posesRanked"))
    return None


def short_attribution(r: dict) -> str:
    """The line shown under the photo. Commons: author, licence, source and what was changed (CC BY / BY-SA ask for
    changes to be indicated; CC0 does not, but every Commons credit says it the same way); Mixkit: 'Photo: Mixkit'."""
    if r["source"] == "Mixkit":
        return "Photo: Mixkit"
    lic = r["licence"]["name"]
    author = r["author"].strip()
    m = re.search(r"\b(www\.[\w.-]+\.[a-z]{2,})", r.get("attribution_request") or "")
    if m and m.group(1) not in author:
        author += f" ({m.group(1)})"  # the author asks for this link in the file description
    changes = []
    if r.get("crop_px"):
        changes.append("cropped")
    if r.get("mirrored"):
        changes.append("mirrored")
    tail = f" ({' and '.join(changes)})" if changes else ""
    return f"Photo: {author}, {lic}, Wikimedia Commons{tail}"


def credit(r: dict, m: dict | None) -> dict:
    lic = r["licence"]["name"]
    text = LICENCE_FILES[lic]
    if not (REPO / "app/src/main/assets/licenses" / text).exists():
        sys.exit(f"{r['pose_id']}: licence text licenses/{text} missing")
    c = {
        "photo": Path(r["photo"]).name,
        "source": r["source"],
        "title": r.get("title"),
        "pageUrl": r["page_url"],
        "fileUrl": r["file_url"],
        "author": r["author"],
        "licence": lic,
        "licenceUrl": LICENCE_URLS[lic],
        "licenceText": f"licenses/{text}",
        "attribution": short_attribution(r),
        "changes": r["changes"],
        "mirrored": bool(r.get("mirrored")),
        "appScore": m.get("appScore") if m else None,
    }
    if r["source"] == "Mixkit":
        c["frameTimeS"] = r.get("frame_time_s")
        c["videoQuality"] = r.get("video_quality")
    if r.get("credit_line") and r["source"] != "Mixkit":
        c["sourceCredit"] = r["credit_line"]
    if r.get("attribution_request"):
        c["attributionRequest"] = r["attribution_request"]
    if lic.startswith("CC BY-SA"):
        c["shareAlike"] = f"this adapted photo file is shared under {lic}"
    restr = (r["licence"].get("restrictions") or "").strip()
    if restr:
        c["restrictions"] = restr
    n = note_of(r["pose_id"], m)
    if n:
        c["note"] = n
    return c


def short(text: str | None, limit: int = 110) -> str:
    """First clause of a reason, for the docs table (the whole text is in coverage.json)."""
    t = (text or "").replace("|", "/").strip()
    t = re.sub(r"^(Kept|Dropped at the confirm stage|Removed at the review|Removed at the second look): ", "", t)
    depth, cut = 0, len(t)
    for i, ch in enumerate(t):  # the first ": ", "; " or ". " outside brackets, after 20 characters
        depth += (ch == "(") - (ch == ")")
        if ch in ":;." and t[i + 1:i + 2] == " " and depth <= 0 and i > 20:
            cut = i
            break
    t = t[:cut]
    return t if len(t) <= limit else t[:limit - 3].rsplit(" ", 1)[0] + " ..."


def print_table():
    cov = json.loads((TOOLS / "coverage.json").read_text())["coverage"]
    print("| Pose | Category | Photo | Source | Licence | App score | Note |")
    print("| --- | --- | --- | --- | --- | --- | --- |")
    for r in cov:
        name = f"{r['name']} (`{r['id']}`)"
        cat = r["category"].capitalize() + (f", {r['people']} people" if r["people"] > 1 else "")
        if r["photo"]:
            src = "Mixkit" if r["source"] == "Mixkit" else "Commons"
            lic = "Mixkit Free" if r["licence"].startswith("Mixkit") else r["licence"]
            score = "-" if r["appScore"] is None else f"{r['appScore']:.2f}"
            note = f"flagged ({flag_short(r)}), kept: {short(r.get('note'), 80)}" if r.get("flag") else ""
            print(f"| {name} | {cat} | yes | {src} | {lic} | {score} | {note} |")
        else:
            reason = r.get("reason") or ""
            tag = ("removed at the review: " if reason.startswith("Removed at the review")
                   else "removed at the second look: " if reason.startswith("Removed at the second look")
                   else "dropped: " if r.get("verdict", "").startswith("dropped") else "")
            print(f"| {name} | {cat} | **no** | | | | {tag}{short(r.get('reason'))} |")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--table", action="store_true", help="print the coverage table (Markdown) from coverage.json")
    args = ap.parse_args()
    if args.table:
        print_table()
        return
    poses = load_poses()
    picks = load_picks()
    match = load_match()
    credits, coverage = {}, []
    for p in poses:
        r = picks.get(p["id"], {})
        m = match.get(p["id"])
        has = bool(r.get("photo")) and p["id"] not in DROPPED
        row = {"id": p["id"], "name": p["name"], "category": p["category"], "people": len(p["figures"]),
               "photo": has}
        if has:
            if not (REPO / r["photo"]).exists():
                sys.exit(f"missing photo file {r['photo']}")
            if m is None:
                sys.exit(f"{p['id']}: no app-match row (run pose-core PosePhotoMatch)")
            c = credit(r, m)
            credits[p["id"]] = c
            row.update({"source": c["source"], "licence": c["licence"], "attribution": c["attribution"],
                        "appScore": c["appScore"], "flippedScore": (m.get("flipped") or {}).get("score"),
                        "nearestPose": m.get("nearestPose"), "ownRank": m.get("ownRankAmongPoses"),
                        "posesRanked": m.get("posesRanked"), "peopleDetected": m.get("people"),
                        "bytes": (REPO / r["photo"]).stat().st_size, "flag": flag_of(p["id"], m),
                        "verdict": "kept after a second look" if flag_of(p["id"], m) else "ok",
                        "reason": r.get("reason"), "note": c.get("note")})
        else:
            if (REPO / f"app/src/main/assets/pose_photos/{p['id']}.webp").exists():
                sys.exit(f"{p['id']}: dropped or empty but its photo file is still there")
            row["reason"] = DROPPED.get(p["id"]) or r.get("reason")
            row["verdict"] = "dropped at the confirm stage" if p["id"] in DROPPED else "no photo"
        coverage.append(row)
    orphans = {f.stem for f in PHOTOS.glob("*.webp")} - set(credits)
    if orphans:
        sys.exit(f"photo files without a credit: {sorted(orphans)}")
    n = sum(1 for r in coverage if r["photo"])
    size = sum(r.get("bytes", 0) for r in coverage)
    flagged = sum(1 for r in coverage if r.get("flag"))
    print(f"{n} of {len(poses)} poses have a photo ({flagged} flagged and kept after a second look, "
          f"{len(DROPPED)} dropped), {size / 1024:.0f} KB")
    if args.check:
        return
    (PHOTOS / "credits.json").write_text(json.dumps(credits, indent=1, ensure_ascii=False) + "\n")
    (TOOLS / "coverage.json").write_text(json.dumps({"photos": n, "poses": len(poses), "bytes": size,
                                                    "coverage": coverage}, indent=1, ensure_ascii=False) + "\n")


if __name__ == "__main__":
    main()
