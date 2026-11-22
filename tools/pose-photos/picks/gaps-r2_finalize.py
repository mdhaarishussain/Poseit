#!/usr/bin/env python3
"""
gaps-r2_finalize.py: turn the gap-filling round 2 picks into the app's sample photos, landmarks and credits, and
record why the other gap poses stay empty.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/sitting-floor_finalize.py frame CLIP T1[,T2]   # licensed 1080p, sharpest frame
    $PY -I tools/pose-photos/picks/gaps-r2_finalize.py fetch [--poses id,id]       # Commons renditions
    $PY -I tools/pose-photos/picks/gaps-r2_finalize.py build [--poses id,id]

Same steps and outputs as gaps-r1_finalize.py (crop 3:4, or 4:3 for the landscape group poses, mirror when the photo
matches the flipped pose, 900 px long side, WebP without metadata, landmarks of the shipped file, posematch score),
with round 2's records: Commons licence data from WORK/gaps-r2/commons/files.json (the round 2 search), Mixkit licence
evidence from WORK/gaps-r2/mixkit/clips/<id>.json. Credits: tools/pose-photos/picks/gaps-r2.json.
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

CREDITS = HERE / "gaps-r2.json"
CDL = WORK / "picks_dl" / "gaps-r2" / "commons"

# "src": "mixkit" -> WORK/picks_dl/<clip>_<ms>.png made by `frame` (hold = the time passed to `frame`);
#        "commons" -> CDL/<pageid>_<w>.jpg. "crop_px" (optional): manual [x0, y0, x1, y1] in source pixels.
#        "mirror" (optional): force; otherwise mirrored when the matcher matched the flipped pose.
#        "quality" (optional): WebP quality when the default 78 would pass ~100 KB.
PICKS: list[dict] = [
    {"pose": "pro_arms_crossed", "src": "commons", "pageid": 87185005, "crop_px": [0, 160, 1920, 2720],
     "reason": "A woman in a white blouse, black pencil skirt and heels standing square to the camera with her arms "
               "folded and a level, confident look: the drawn confident crossed arms, whole body on a plain studio "
               "wall, sharp and evenly lit. A consenting model ('Full consent' category) photographed for a "
               "dress-code illustration ('Slightly business casual Informal wear - pose 2'); no name, text or logo. "
               "The 2:3 source cannot hold the whole figure in a 3:4 frame, so the crop trims the top of the hair; "
               "mirrored because the photo matches the flipped figure. Same model and series as photos of hers in "
               "other outfits by this photographer, some of which are lingerie studies: only this business-wear "
               "frame is used."},
    {"pose": "port_chin_on_hand", "src": "commons", "pageid": 61725617, "crop_px": [83, 0, 1883, 2400],
     "reason": "A young woman at a café window table resting her chin on the knuckles of one hand, the elbow on the "
               "table, the other forearm lying flat across it, looking into the camera with heart-shaped bokeh "
               "lights behind: exactly the drawn café chin rest, waist-up, sharp. Unsplash photo (CC0, reviewed "
               "import); her age is not stated, but she has a wrist tattoo and a nose piercing and reads as an "
               "adult."},
    {"pose": "sit_one_knee_up", "src": "commons", "pageid": 155246868, "crop_px": [0, 170, 1920, 2730],
     "author_link": "www.rydale.com",  # the file description asks for attribution as a link to www.rydale.com
     "reason": "A woman in a grey hoodie and joggers sitting on light wooden stairs, one foot drawn up on the step so "
               "that knee rises high, the other leg reaching down two steps, one hand resting near the knee and the "
               "other on her shoulder: the drawn 'one knee up' on steps, whole body, bright and sharp. A clothing "
               "company's product photo with a professional model ('Female models from England'), released under "
               "CC BY 2.0; no visible logo. The crop is tight (head to toes) because the 2:3 source is only just "
               "tall enough. The matcher scores it low (the photo's arms differ from the figure's), but the body "
               "shape reads as the pose."},
]

# Gap poses still without a photo after this round, and why. Round 2 searched (WORK/gaps-r2): 69 Commons queries
# (every people photo in the reviewed Unsplash, Pixabay and PxHere CC0 imports, 30 pose categories such as Men/Women
# with crossed arms, Women/Men leaning against walls, Women/Men sitting on stairs, Women/Men crouching and kneeling,
# Women/Men jumping, Hands on hips, Chin on hand, People sitting on desks/tables/armchairs/stools/chairs/benches,
# People looking back/away, Women/Men laughing, People lying on the ground, Selfies, sunglasses, rawpixel CC0 imports,
# Full consent, and 30 pose phrases); 14,000 titles, 6,475 thumbnails passed the title rules and were scored with the
# app's matcher, the best 20 per pose got their licence metadata (137 of 422 pass the rules). Mixkit: 2,518 new cards
# from 205 more tags and searches, 92 clips picked by title: 54 Restricted, 4 fail the people rules, 34 scanned.
GAPS: dict[str, str] = {
    # STANDING
    "stand_hands_in_pockets": "Round 2's best full-body hands-in-pockets photo ('Italian style 2013', CC0: both hands in "
        "her front jeans pockets against a wall) shows the same model and outfit as 'Italian girl 2013' by the same "
        "author, whose description calls her an 'Italian teenager', so both are dropped under the minors rule. The "
        "other full-length matches name the person ('Will Ng 1', 'Jérôme Beck 2022', 'Mr vera'), and in Mixkit 359 "
        "('Man modelling in different places') he stands far away in a doorway under graffiti lettering.",
    "stand_look_away": "No full-body relaxed stance with the head turned away: the matcher's best new matches are back "
        "views, named people ('Ana Belén - N3A1402') or event photos; the 'People looking away' category (563 files) "
        "is mostly named portraits; the new Mixkit model clips (41472, 41478, 41519) are close-ups.",
    "stand_arms_crossed": "The full-body crossed-arms photos found name the person ('Wena Opena, 2021', "
        "'Danielagalliano', 'Claudio-antonioli', 'Kuzzat Altay', a wrestler's watermarked 'BasedHRAEW2024'), come from "
        "a cosplay convention, or show two possibly teenage girls ('Cuba (6959893471)'). The one clean studio photo "
        "('Slightly Business casual Informal wear - pose 2') is used for pro_arms_crossed, which its business wear fits.",
    "stand_over_shoulder": "Nobody looks back over the shoulder: every high match in 'People looking back' and the stock "
        "imports is a back view walking away (the matcher cannot see that the head does not turn).",
    "stand_power_pose": "No clean wide-stance hands-on-hips: 'Hands on hips' and Men/Women with hands on hips (236 "
        "files) give celebrities at events ('Kiara Advani snapped in Mumbai', 'Pooja Hegde at ...'), paintings and "
        "archive photos; Mixkit 359 has hands on hips at 19 s but is cut at the thighs.",
    "stand_sunglasses": "No full-body hand-on-sunglasses: 'People with/wearing sunglasses' (262 files) are named people, "
        "events or head shots.",
    # LEANING
    "lean_shoulder_wall": "Closest is 'Cuba (6959893471)' (CC BY 2.0: shoulder on a door frame, arms and ankles "
        "crossed), but the two girls in it may be teenagers; Mixkit 359 at 10.5 s leans his back flat on a wall with "
        "one foot up, a different lean.",
    "lean_hand_on_wall": "'Italian girl 2013' (hand on the fence above a wall) is described as a teenager; the "
        "photographer series with a hand on a pillar ('Jumpsuit with pleated legs - leaning against a wall pose') is a "
        "glamour shoot leaning back on the pillar.",
    "lean_railing_forward": "The railing phrases gave 10 files and the leaning categories nobody bent forward on the "
        "forearms over a rail.",
    "lean_door_frame": "The doorway phrases (213 files) and the leaning categories gave nobody with an arm up on a door "
        "frame.",
    "lean_pole_swing": "Nobody swinging round a pole or lamp post (24 lamp-post files, the jumping and leaning "
        "categories).",
    "lean_table_edge": "'People sitting on desks' and 'People sitting on tables' (24 files) and the desk-perch phrases "
        "(27) gave nobody perched on an edge in a usable photo; Mixkit 48676 ('sitting on a table at the library') is "
        "Restricted.",
    # PROFESSIONAL
    "pro_desk_edge": "As lean_table_edge: no desk perch in the desk/table categories or phrases; the Mixkit desk clips "
        "found are people seated at desks or Restricted (48676, 49242).",
    "pro_open_palm": "The Mixkit speaker clips found in round 2 (36791, 36817, 36820, 37043, 45957) are all Restricted; "
        "the Commons 'gesturing/presenting' matches are conference photos (public events).",
    "pro_table_lean": "Nobody leaning on a table with both hands; the matcher's top matches are people standing with "
        "the arms down.",
    "pro_cuff_adjust": "No cuff adjust in any new candidate (the 'Black woman with women's suit' studio series has "
        "crossed arms and a jacket over the shoulder, thigh-up).",
    "pro_folder_hold": "'People holding clipboards' (69 files) gave workers and events; the Mixkit tablet clips "
        "(17442, 33507, 45706) are Restricted.",
    # WALKING
    "walk_look_back": "Nobody walking away and turning back: the high matches are plain back views.",
    "walk_kerb_step": "No step down from a kerb in any new candidate.",
    "walk_skip": "No skip: the Commons 'Skipping' category is rope skipping and children; no new Mixkit skip clip is "
        "Free (47519 is Restricted).",
    "walk_hat_hold": "'People holding hats' (21 files) has nobody walking; the Mixkit hat clips found (48663, 27802) are "
        "Restricted.",
    "walk_pockets": "Nobody walking with the hands in the pockets; the Mixkit walking clips found (33412, 35419, 46538) "
        "are Restricted.",
    "walk_coffee_run": "'People with cups' (42 files) has nobody walking; the Mixkit coffee clips are hands only "
        "(1726) or Restricted (21839, 36863).",
    "walk_jacket_shoulder": "'Black woman with women's suit 8' holds her jacket over her shoulder but stands still and "
        "is cut at the thighs; nothing walking.",
    # SITTING
    "sit_side_saddle": "No knees-together side sit: 'Ladysittingonbench' crosses knee over knee, and Mixkit 1859 sits "
        "square to the camera with the knees together.",
    "sit_steps_sideways": "Nearest is 'Man using smartphone, Passeio das Tágides' (CC BY-SA 4.0): he sits along a ledge "
        "with one knee up, but he is a small figure in a landscape, on his phone, with no wall at his back.",
    "sit_hug_knees": "Mixkit 323 ('Woman sitting on skateboard', Free) hugs one knee in a cross-legged sit, with the "
        "feet cut off at 1080p and an off-the-shoulder top in front of a graffiti wall: not the pose and borderline; "
        "the floor and grass sitting categories gave no knee hug.",
    "sit_armchair_lounge": "'People sitting on armchairs' (103 files) gave upright sitters and named people; Mixkit "
        "50764 lounges with the legs along a sofa, not over an armrest.",
    "sit_chair_backwards": "Nobody astride a backwards chair in the phrase search (38 files) or 'People sitting on "
        "chairs' (472).",
    "sit_ankle_on_knee": "'Crossed legs' (deep category) gives knee-over-knee sits; the only ankle-on-knee look-alikes "
        "are Harajuku street snaps of passers-by, not models.",
    "sit_high_perch": "'People sitting on stools' (35 files) and the stool phrases gave no clean high perch.",
    # FLOOR
    "floor_low_crouch": "Women/Men/People crouching (592 files) give named people ('TamaraAnnaPawlak', 'WikiTamara'), "
        "sport and street scenes; the studio squat 'Man with mustache in street clothes' has the knees apart and a "
        "brand logo across his shirt.",
    "floor_feet_up": "Nobody lying on the stomach with the feet up: the 'lying on stomach' phrases found 5 files and "
        "'People lying on the ground/grass' (229) none.",
    "floor_overhead_lie": "No overhead shot of someone lying down in the lying categories or the stock imports.",
    "floor_sit_on_heels": "'Seiza' and Women/Men/People kneeling (608 files) are religious, sport, martial-arts and "
        "named photos; no clean sit on the heels.",
    # PORTRAIT / SELFIE
    "selfie_hand_on_cheek": "'Woman in Eyeglasses (5)' (PxHere, CC0) rests her cheek on a hand, but its categories name "
        "the model ('Isabela Salvador'), which the named-person rule excludes; the selfie categories (76 files) are "
        "named people.",
    "selfie_look_up": "No selfie looking up past the phone in the selfie categories or the stock imports.",
    "selfie_lean_in": "No lean toward a phone with a hand on the hip; the studio suit series has hand-on-hip shots "
        "without the phone arm.",
    # FUN
    "fun_star_jump": "'Young ladies jump for joy Canberra-1' (CC BY-SA 2.0) has a clean star jump, but in front of "
        "Parliament House (the rules exclude politics) and the jumper would be about 500 px tall; 'Beautiful girl "
        "jumping on rail tracks' is 720 px with the jumper small and her age unclear.",
    "fun_tuck_jump": "No knee-tuck jump among Women/Men jumping (216 files) or the jump phrases.",
    "fun_laugh_bend": "Nobody doubled over laughing in Women/Men laughing (828 files): head-and-shoulders laughs.",
    "fun_high_kick": "No front kick with open arms (martial-arts competition photos only).",
    "fun_peace_lean": "The peace-sign phrase (91 files) gave named people and events.",
    # COUPLE / GROUP
    "couple_side_hug": "No side-by-side hug: the couple matches are events or families, and the Mixkit couple clips "
        "are Restricted (46487 'Two women smiling in front of a brick wall', 47906, 46905, 46904) or walk past "
        "(45383, 4617).",
    "couple_dip": "'Dip (dance move)' (10 files) and 'Tango dancers' are stage performances; no dip in the Mixkit "
        "dance clips (47032 is Restricted).",
    "group_v_formation": "No V-formation: the group matches are events, political groups or archive photos; the "
        "Mixkit friends clips found (36816, 36922, 46771, 8847, 9008) are Restricted.",
    "group_jump": "No group jump among the jump categories and phrases; Mixkit 5714 (couple jumping) is Restricted.",
    "group_walk_line": "No line of friends walking: Mixkit 46446 ('Three business people walk outside office') is "
        "Restricted, and the Commons group-walking photos are events.",
    "group_sitting_row": "Closest is 'HN007sRamon casaNroute' (three men sitting on steps), but one of them may be a "
        "teenager; the other rows are archive photos, families or political groups; Mixkit 35930 and 46771 are "
        "Restricted.",
    "group_levels": "No three head heights in any group candidate.",
    "group_back_to_back": "No three-person back-to-back: the 'Back to back' category search returned unrelated files "
        "and the phrase search events.",
}


def commons_record(pageid):
    for f in (WORK / "gaps-r2" / "commons" / "files.json", WORK / "gaps-r1" / "commons" / "files.json",
              WORK / "commons" / "files.json"):
        files = json.loads(f.read_text()) if f.exists() else {}
        if str(pageid) in files:
            rec = files[str(pageid)]
            if not rec.get("ok"):
                raise SystemExit(f"Commons file {pageid} does not pass the rules: {rec['verdict']}")
            return rec
    raise SystemExit(f"no record for Commons file {pageid}")


def clip_record(clip):
    for d in (WORK / "gaps-r2" / "mixkit" / "clips", WORK / "gaps-r1" / "mixkit" / "clips",
              WORK / "mixkit" / "clips", WORK / "couple-group" / "clips"):
        f = d / f"{clip}.json"
        if f.exists():
            rec = json.loads(f.read_text())
            if rec.get("licence", {}).get("ok"):
                return rec, str(f.relative_to(WORK))
    raise SystemExit(f"no licence-checked record for Mixkit clip {clip}")


def wire():
    f1.PICKS = PICKS
    f1.GAPS = GAPS
    f1.CREDITS = CREDITS
    f1.CDL = CDL
    f1.commons_record = commons_record
    f1.clip_record = clip_record


def finish_attribution():
    """Say in the displayed attribution what was changed (CC BY / BY-SA ask to indicate changes), that a BY-SA crop is
    shared under the same licence, and add an author's requested link."""
    credits = json.loads(CREDITS.read_text())
    picks = {p["pose"]: p for p in PICKS}
    for c in credits:
        p = picks.get(c.get("pose_id"))
        if not p or not c.get("photo") or c.get("source") != "Wikimedia Commons":
            continue
        lic = (c.get("licence") or {}).get("name") or ""
        att = f"Photo: {c['author']}" + (f" ({p['author_link']})" if p.get("author_link") else "")
        att += f", {lic}" + (f" ({c['licence']['url']})" if c["licence"].get("url") else "")
        att += f", via Wikimedia Commons ({c['page_url']})"
        if "CC0" not in lic and "Public domain" not in lic:
            att += "; cropped" + (" and mirrored" if c.get("mirrored") else "")
        if "BY-SA" in lic:
            att += f"; this adaptation is shared under {lic}"
        c["attribution"] = att
        if p.get("author_link"):
            c["attribution_request"] = f"the file description asks for attribution as a link to {p['author_link']}"
    CREDITS.write_text(json.dumps(credits, indent=1, ensure_ascii=False) + "\n")


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("fetch")
    a.add_argument("--poses")
    b = sub.add_parser("build")
    b.add_argument("--poses")
    args = ap.parse_args()
    wire()
    only = set(args.poses.split(",")) if args.poses else None
    {"fetch": f1.cmd_fetch, "build": f1.cmd_build}[args.cmd](only)
    if args.cmd == "build":
        finish_attribution()


if __name__ == "__main__":
    main()
