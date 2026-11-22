#!/usr/bin/env python3
"""
gaps-r1_finalize.py: turn the gap-filling round 1 picks into the app's sample photos, landmarks and credits, and
record why the other gap poses stay empty.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/sitting-floor_finalize.py frame CLIP T1[,T2]   # licensed 1080p, sharpest frame
    $PY -I tools/pose-photos/picks/gaps-r1_finalize.py fetch [--poses id,id]       # Commons renditions
    $PY -I tools/pose-photos/picks/gaps-r1_finalize.py build [--poses id,id]

frame  (the sitting-floor stage's command) downloads the clip page's 1080p file (720p fallback) into WORK/picks_dl,
       keeps the frame with the highest Laplacian variance within 0.3 s of the hold as WORK/picks_dl/<clip>_<ms>.png
       plus a .json note (download page, video URL, exact frame time) and deletes the video.
fetch  a standard 1920 px rendition (or the original when smaller) of each Commons pick ->
       WORK/picks_dl/gaps-r1/commons/<pageid>_<w>.jpg. Licence data comes from WORK/gaps-r1/commons/files.json (the
       gap search) or WORK/commons/files.json (the harvest).
build  per pick: detects people on the source (the app's full model, IMAGE mode, up to 4 people), scores it against
       the pose (posematch, the Python port of PoseMatcher), crops 3:4 around the person (union of visible landmarks
       plus margin, or a manual crop), mirrors when the photo matches the flipped pose (or as set), resizes to 900 px
       on the long side, saves WebP quality 78 without metadata to app/src/main/assets/pose_photos/<poseId>.webp,
       detects and scores the saved file (tools/pose-photos/landmarks/<poseId>.json) and writes
       tools/pose-photos/picks/gaps-r1.json: one object per gap pose, the credit (source, page and file URLs, author,
       licence, attribution, changes, matcher score, reason) or the reason it stays empty.
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
_spec2 = importlib.util.spec_from_file_location("couple_group_finalize", HERE / "couple-group_finalize.py")
cgf = importlib.util.module_from_spec(_spec2)
_spec2.loader.exec_module(cgf)

ROOT = TOOLS.parent.parent
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "pose_photos"
LM_DIR = TOOLS / "landmarks"
CREDITS = HERE / "gaps-r1.json"
DL = WORK / "picks_dl"
CDL = DL / "gaps-r1" / "commons"
LONG_SIDE, QUALITY = 900, 78
CHECKED = "2026-11-21"

# "src": "mixkit" -> WORK/picks_dl/<clip>_<ms>.png made by `frame` (hold = the time passed to `frame`);
#        "commons" -> CDL/<pageid>_<w>.jpg. "crop_px" (optional): manual [x0, y0, x1, y1] in source pixels (3:4).
#        "mirror" (optional): force; otherwise mirrored when the matcher matched the flipped pose.
#        "quality" (optional): WebP quality when the default 78 would pass ~100 KB.
PICKS: list[dict] = [
    {"pose": "floor_street_squat", "src": "mixkit", "clip": 32800, "hold": 10.0, "crop_px": [665, 80, 1415, 1080], "quality": 68,
     "reason": "A man in a deep, flat-footed squat on a running track, knees wide and forearms resting on the knees "
               "(checking his watch): exactly the drawn street squat, whole body in frame, sharp and evenly lit. "
               "Chosen from a clip found by its title in round 1 (the sprinter in the clip is out of frame at this "
               "moment)."},
    {"pose": "floor_one_knee", "src": "mixkit", "clip": 51645, "hold": 9.0, "crop_px": [330, 280, 930, 1080], "mirror": False,
     "reason": "A man kneeling on one knee, side-on, front knee up with one hand resting on it: the drawn 'one knee "
               "down' from the same side. It is a proposal scene (the raised hand holds a ring box); the crop leaves "
               "his partner out. Evening light by a fire pit, but the figure is well exposed and sharp."},
    {"pose": "fun_twirl", "src": "mixkit", "clip": 276, "hold": 9.2, "crop_px": [430, 0, 1240, 1080], "mirror": True, "quality": 74,
     "reason": "A woman in a long floral dress spinning in front of a graffiti wall, one arm raised high overhead with "
               "her shirt flying out and one heel flicked up behind: the drawn heel-flick twirl, whole body in "
               "frame, sharp daylight. Mirrored so the raised arm is on the same side as the figure's. The frame "
               "was picked by hand (9.200 s): the sharpest one nearby (9.242 s) cuts the raised hand at the top."},
    {"pose": "couple_cheek_kiss", "src": "mixkit", "clip": 39962, "hold": 7.0, "crop_px": [600, 0, 1410, 1080],
     "reason": "She kisses him on the cheek while he smiles with his eyes lowered: the gesture of the drawn cheek "
               "kiss, kisser on the left as drawn. A tight close-up of the two faces rather than the drawn three-"
               "quarter framing, and he does not look at the camera. Both are adults (the clip's own description "
               "calls him 'a man'; the related clip 39960 says 'young man'); no text or logos."},
    {"pose": "group_arms_around", "src": "commons", "pageid": 100324356, "crop_px": [328, 273, 1672, 1281],
     "reason": "Seven women standing shoulder to shoulder against a plain white wall, arms wrapped around each "
               "other's shoulders and waists, all laughing: the shoulder line-up the pose describes ('it fits any "
               "number of people'), whole bodies in frame, sharp and evenly lit. Seven people against three drawn "
               "figures: the detector (up to 4 people) finds four, so the matcher score is only indicative."},
]

# Gap poses still without a photo after this round, and why.
GAPS: dict[str, str] = {
    # STANDING
    "stand_hands_in_pockets": "Round 1 read the whole Commons category 'Hands in pockets' (222 files): the full-length "
        "photos that pass the licence rules name the person ('Valentine at the university of Zambia', 'Paja2019' = a "
        "named singer, 'KhristenkoPS'), the clean stock one ('Striped Mock Neck Top, Leather Jacket, and Wayfarers', "
        "CC BY-SA 2.0) is cut above the knees, and '35mm 2017' (Unsplash) may show a teenager. Mixkit has no hands-in-"
        "pockets title other than pocket watches.",
    "stand_look_away": "Still no full-body relaxed stance with the head turned away: the new looks-away (Mixkit 32742 "
        "woman with a flower, Commons 'Calvin Lupiya 2016' by a lamp post, Mixkit 1296 on a balcony) are thigh- or "
        "knee-up, and Mixkit 460 is a low-angle hand-on-headphones shot.",
    "stand_arms_crossed": "No full-body crossed arms: Mixkit 43806 (green screen) and Commons 'Karat é' (karate gi) cut "
        "at the thighs or knees, 50924 is a waist-up couple, and the three titled 'crossing her arms' / 'confident "
        "businesswoman' (45753, 45748, 45988) are Mixkit Restricted.",
    "stand_over_shoulder": "Nobody looks back over the shoulder in a full-body shot: the round-1 Mixkit 'looking over "
        "shoulder' results are close-ups (100146) or people at computers, and the Commons 'looking back' search "
        "(Unsplash/Pixabay imports) gave no full-length look-back.",
    "stand_power_pose": "No wide-stance hands-on-hips: the only new hands-on-hips photo (Commons 'Indian man standing "
        "in doorway', Unsplash CC0) is a travel portrait of an elderly local man with his feet together, not the bold "
        "stance; Mixkit 45680 and 49423 ('standing confidently', 'posing confidently') are Restricted and 43186 is a "
        "face close-up.",
    "stand_sunglasses": "No full-body sunglasses touch: Mixkit 436 and 43181 are seated or close-ups, the 'puts on "
        "sunglasses' clips (24293, 8846) and 27637 / 24707 are Restricted, and Mixkit 695 (hand on a hat brim) is a "
        "backlit near-silhouette.",
    # LEANING
    "lean_shoulder_wall": "No full-body shoulder lean: Commons 'Man in hat and black vest' (Pexels CC0) leans his "
        "shoulder on a wall but is cut at the hips; Mixkit 2553 walks past a graffiti wall, 325 is a face close-up, "
        "and 'Pair of girls leaning against a wall with graffiti' (33908) fails the minors rule.",
    "lean_hand_on_wall": "No palm on a wall: the best new frame (Mixkit 42577 at 10.5 s) is a woman holding a bus "
        "handrail overhead with a seated man beside her.",
    "lean_railing_forward": "Nobody bent forward over a railing on the forearms: the balcony clips (Mixkit 3202, "
        "10440, 1296, 42729, 43183, 12169) show people standing at the rail, from behind or on the phone; 25531 and "
        "27871 are Restricted.",
    "lean_door_frame": "No doorway lean: the one doorway photo found (Commons 'Indian man standing in doorway') stands "
        "in the doorway with both hands on his hips, no hand on the frame; the Mixkit doorway titles are a worried "
        "woman peering out and door details.",
    "lean_pole_swing": "Nobody swinging around a pole: the Mixkit 'pole' titles are pole dancing, pole vault, flags "
        "and a barber pole; the Commons 'pole woman' search gave nothing that passes.",
    "lean_table_edge": "No table-edge perch: the Mixkit 'sitting on desk' search returns people seated at desks, and "
        "the Commons search found two desk photos without a person perched.",
    # PROFESSIONAL
    "pro_arms_crossed": "As for stand_arms_crossed: no full-body confident crossed arms; the businesswoman clip "
        "(45748) is Restricted and the other crossed-arms frames are cut at the thighs.",
    "pro_desk_edge": "No desk perch in any candidate; the Commons phrase 'businessman sitting on edge of desk' returns "
        "nothing and the Mixkit desk clips are people seated at computers.",
    "pro_open_palm": "No open-palm speaker in a three-quarter shot: every new speaker/presentation clip found by title "
        "(23861, 23117, 37031, 37035) is Mixkit Restricted.",
    "pro_table_lean": "Nobody leaning on a table with both hands; the Commons 'leaning table' search gave one photo "
        "without the pose.",
    "pro_cuff_adjust": "No full-body cuff adjust: Mixkit 28888 shows only hands and a watch, 5185 is a bow-tie "
        "close-up, and 45749 / 24581 / 48959 are Restricted.",
    "pro_folder_hold": "Nobody holds a folder or tablet to the chest full length: 1810 shows only hands, and the "
        "eleven businessman/businesswoman-with-tablet clips found in round 1 (17698, 34098, 35428, 35408, 33040-33042, "
        "33299, 14097, 9081, 25262) are all Mixkit Restricted.",
    # WALKING
    "walk_look_back": "No one walking away and looking back: Mixkit 49670 (walking on a red pier) does not turn, and "
        "the Commons 'walking away' search found one file without the pose.",
    "walk_kerb_step": "No step down from a kerb: Mixkit 4832 is a head-and-shoulders back view, 98 shows legs on a "
        "crosswalk, and the Commons 'curb' search found nobody stepping.",
    "walk_skip": "No skip with a knee lifted: the Mixkit 'skipping' tag is mostly rope skipping (11538 Restricted), "
        "and 4585 (jumping with a smoke flare) hides the legs in wheat and may show a teenager.",
    "walk_hat_hold": "No hand on a hat while walking in daylight: Mixkit 42374 is a sunset silhouette, 695 is backlit "
        "and standing, and 'Woman holding her hat in a meadow' (33498) is Restricted.",
    "walk_pockets": "No walking with hands in pockets: nothing in the Commons 'Hands in pockets' category walks, and "
        "the Mixkit 'businessman walking' clips (21371, 33034) are Restricted.",
    "walk_coffee_run": "No one walking with a cup: all six clips titled 'walking ... with coffee' (6247, 6306, 32004, "
        "33045, 45560, 8969) plus 6282 are Mixkit Restricted, 46650 / 46360 fail the minors rule ('girls'), and the "
        "Commons search found nobody walking with a cup.",
    "walk_jacket_shoulder": "No jacket over the shoulder: Mixkit 1391 ('holding a black leather jacket') is a close-up "
        "of an arm; the Commons search returned nothing that passes.",
    # SITTING
    "sit_one_knee_up": "No one sitting on steps with one foot a step higher: Mixkit 47996 (sitting on steps) is "
        "Restricted, 552 sits forward with both feet level, and Mixkit 436 sits on a floor against a wall.",
    "sit_side_saddle": "No knees-together sit to one side: Mixkit 48066 (woman on a park bench) is Restricted and 965 "
        "is a close-up through flowers.",
    "sit_steps_sideways": "No one sitting along a step with the back to the wall; the nearest new frame (Mixkit 436 at "
        "13 s, one leg long and one knee up) sits facing the camera on a floor.",
    "sit_hug_knees": "No both-arms knee hug: the yoga/meditation clips scanned in round 1 (32628, 42904, 32751, 32753, "
        "43798, 5058 and others) sit cross-legged; Commons 'Girl with Afro' (Pixabay) crouches with the arms hidden "
        "and the feet cut off.",
    "sit_armchair_lounge": "No one draped over an armchair: Mixkit 4959 sits upright with a tablet, 4958 lounges along a sofa "
        "seat (legs not over an armrest), 406 is a "
        "face close-up, and 49537 / 48843 are Restricted.",
    "sit_chair_backwards": "No backwards chair in any new candidate.",
    "sit_ankle_on_knee": "No clean ankle-on-knee: Mixkit 41188 (man with a newspaper) and 406 do not show it, 34130 is "
        "Restricted.",
    "sit_high_perch": "No stool perch: the bar titles found are alcohol scenes (excluded) or Restricted.",
    # FLOOR
    "floor_low_crouch": "No knees-together crouch on the balls of the feet: Mixkit 32800's runner crouches into a "
        "sprint start with the hands on the track, Mixkit 699 (woman crouching in a junkyard) is cut off at the feet, "
        "and Commons 'Girl with Afro' hides the arms and feet.",
    "floor_feet_up": "No one lying on the stomach with the feet up in any candidate.",
    "floor_overhead_lie": "No overhead shot of someone lying down: Mixkit 2301 is a couple, head and shoulders; "
        "14303 ('lying on the meadow') and 36043 are Restricted.",
    "floor_sit_on_heels": "No one sitting back on the heels: sixteen yoga and meditation clips were scanned in round 1 "
        "(e.g. 32628, 43798, 5058, 32625, 4947) and all sit cross-legged or stand; the kneeling titles are prayer or "
        "church scenes (excluded) and plasticine close-ups.",
    # PORTRAIT / SELFIE
    "port_chin_on_hand": "No chin on the knuckles over a table; the round-1 title matches were café and laptop scenes "
        "without the gesture.",
    "selfie_hand_on_cheek": "No cheek-rest selfie: Mixkit 41181 holds the phone up without a hand on the cheek, "
        "43388 and 43382 are dark purple-lit frames, and the other selfie clips found (27891, 34865, 48665, 16241, "
        "28095, 36915) are Restricted.",
    "selfie_look_up": "No selfie looking up past the phone: in Mixkit 41181 the phone leaves the frame when she looks "
        "up; the other selfie clips found are Restricted or night/purple-lit.",
    "selfie_lean_in": "No lean toward the phone with a hand on the hip: the new selfie frames are seated (41181) or "
        "dark (43388).",
    # FUN
    "fun_star_jump": "No star jump: 'Woman jumping in a field' (6388) and 'Women jumping and making exercise' (23643) "
        "are Mixkit Restricted, and 4585 (jumping with a smoke flare) hides the legs in wheat and may show a teenager.",
    "fun_tuck_jump": "No knee-tuck jump: the jump clips found are Restricted (6388, 16233, 24880) or gym box jumps.",
    "fun_laugh_bend": "No one doubled over laughing: the three laughing-woman clips found (45468, 6153, 28914) are "
        "Mixkit Restricted.",
    "fun_high_kick": "No front kick with open arms: the karate clips show side kicks with guard fists (Mixkit 49630 at "
        "1.9 s is sharp, but the kicker may be a teenager and the arms are in guard), and 12971 / 12973 are "
        "Restricted.",
    "fun_peace_lean": "No double peace sign: 'Happy girl on top of mountain making peace sign' (49342) is Restricted; "
        "the other peace-sign titles are doctors in masks.",
    # COUPLE
    "couple_side_hug": "No side-by-side hug: the green-screen embraces (Mixkit 100746, 100742, 100734) are front-on "
        "hugs, 932 is two women seated, and the park-hug titles (29955, 45974, 45467, 45465, 26113) are Restricted.",
    "couple_dip": "No dance dip: the dancing couples scanned (4637, 4667, 42262, 4841, 4639) never dip, and the "
        "ballroom/salsa/wedding-dance clips (39551, 49514, 49530, 49532, 5382) are Restricted.",
    # GROUP
    "group_v_formation": "No V-formation: Mixkit 30003 shows two men standing and a woman seated higher behind them, "
        "thigh-up; 'A group of friends standing in the countryside' (23606) is Restricted.",
    "group_jump": "No group jump: 'Dreamy sunset view of people jumping' (13191) is Restricted and the other jump "
        "titles are concerts.",
    "group_walk_line": "No friends walking toward the camera: the eleven 'friends walking' clips found in round 1 "
        "(21423, 9211, 21368, 8985, 6690, 8845, 9043, 8979, 23884, 33182, 36044) are all Mixkit Restricted, and 3185 is "
        "two women walking away.",
    "group_sitting_row": "No relaxed sitting row: the round-1 titles are a seminar, board games at a table and a car "
        "trunk picnic.",
    "group_levels": "No three head heights: the nearest (Mixkit 30003) has two levels, thigh-up.",
    "group_back_to_back": "No three-person back-to-back: the Mixkit 'back to back' search gives couples (340, 4737, "
        "48038) and silhouetted dancers (870).",
}


def commons_record(pageid):
    for f in (WORK / "gaps-r1" / "commons" / "files.json", WORK / "commons" / "files.json"):
        files = json.loads(f.read_text()) if f.exists() else {}
        if str(pageid) in files:
            return files[str(pageid)]
    raise SystemExit(f"no record for Commons file {pageid}")


def commons_rendition(pageid, width=1920):
    r = commons_record(pageid)
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
            try:
                client.download(url, out, timeout=180)
            except Exception as e:  # a non-standard width can be refused: fall back to 1280
                print(f"{url}: {e}; trying 1280", flush=True)
                r, url, w = commons_rendition(p["pageid"], 1280)
                out = CDL / f"{p['pageid']}_{w}.jpg"
                client.download(url, out, timeout=180)
        print(out, url, flush=True)


def clip_record(clip):
    for d in (WORK / "gaps-r1" / "mixkit" / "clips", WORK / "mixkit" / "clips", WORK / "couple-group" / "clips"):
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
    if not (CDL / f"{p['pageid']}_{w}.jpg").exists():
        r, url, w = commons_rendition(p["pageid"], 1280)
    lic = r["licence"]
    return CDL / f"{p['pageid']}_{w}.jpg", {
        "source": "Wikimedia Commons", "title": r["title"], "page_url": r["page"],
        "file_url": r["file_url"].split("?")[0], "rendition_used": url,
        "author": lic.get("artist") or "unknown", "credit_line": lic.get("credit"),
        "licence": {"name": lic.get("short_name"), "url": lic.get("url"),
                    "attribution_required": lic.get("attribution_required"), "restrictions": lic.get("restrictions"),
                    "usage_terms": lic.get("usage_terms"), "checked": CHECKED},
        "attribution": f"Photo: {lic.get('artist')}, {lic.get('short_name')}"
                       + (f" ({lic.get('url')})" if lic.get("url") else "") + f", via Wikimedia Commons ({r['page']})",
    }


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
        union, n_people = cgf.people_union(rec)
        aspect = 0.75 if pose["frameAspect"] < 1 else 4 / 3
        if p.get("crop_px"):
            box = list(p["crop_px"])
            assert 0 <= box[0] < box[2] <= W and 0 <= box[1] < box[3] <= H, (p["pose"], box, W, H)
            assert abs((box[2] - box[0]) / (box[3] - box[1]) - aspect) < 0.006, (p["pose"], box)
        else:
            if union is None:
                raise SystemExit(f"{p['pose']}: nobody detected in {img_path}; give a manual crop")
            box = cgf.auto_crop(union, W, H, aspect)
        mirror = p["mirror"] if p.get("mirror") is not None else bool(r and r["mirror_photo"])
        out = src.crop(tuple(box))
        if mirror:
            out = out.transpose(Image.FLIP_LEFT_RIGHT)
        s = LONG_SIDE / max(out.size)
        out = out.resize((round(out.width * s), round(out.height * s)), resample=Image.LANCZOS)
        clean = Image.new("RGB", out.size)
        clean.paste(out)
        dest = OUT_DIR / f"{p['pose']}.webp"
        q = p.get("quality", QUALITY)  # lower for a busy background, to stay under ~100 KB
        clean.save(dest, "WEBP", quality=q, method=6)
        saved = Image.open(dest)
        rel = str(dest.relative_to(ROOT))
        lm = det.detect(saved, f"pose_photo:{p['pose']}", rel)
        lm.update({"model": "pose_landmarker_full.task", "mode": "IMAGE", "num_poses": 4})
        (LM_DIR / f"{p['pose']}.json").write_text(json.dumps(lm, separators=(",", ":")) + "\n")
        fin = sc.score_image(lm, [pose])
        f = fin[0] if fin else {}
        n_saved = cgf.people_union(lm)[1]
        changes = [f"still frame at {info['frame_time_s']} s of the {info['video_quality']} download"] \
            if p["src"] == "mixkit" else []
        changes.append(f"cropped to [{box[0]}, {box[1]}, {box[2]}, {box[3]}] px of the {W}x{H} source "
                       f"({'3:4' if aspect < 1 else '4:3'})")
        if mirror:
            changes.append("mirrored horizontally")
        changes.append(f"resized to {saved.width}x{saved.height}, WebP quality {q}, metadata stripped")
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
        if pid in {p["pose"] for p in PICKS}:
            continue
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
