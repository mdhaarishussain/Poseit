#!/usr/bin/env python3
"""
gaps-r1_search.py: gap filling, round 1: targeted searches for the poses that the first selection stages left without
a photo (listed in GAPS below), with the harvest's licence and people rules.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/gaps-r1_search.py commons            # targeted Commons queries (slow: rate limits)
    $PY -I tools/pose-photos/picks/gaps-r1_search.py commons-dl         # standard-width renditions of passing files
    $PY -I tools/pose-photos/picks/gaps-r1_search.py mixkit-crawl       # more pages of pose-related Mixkit tags
    $PY -I tools/pose-photos/picks/gaps-r1_search.py mixkit-pick        # unscanned clips whose titles fit a gap pose
    $PY -I tools/pose-photos/picks/gaps-r1_search.py mixkit-scan [--ids 1,2] [--max 80]
    $PY -I tools/pose-photos/picks/gaps-r1_search.py detect             # full model on the new Commons images
    $PY -I tools/pose-photos/picks/gaps-r1_search.py score              # gap poses x new images -> candidates
    $PY -I tools/pose-photos/picks/gaps-r1_search.py sheets [--poses a,b]

Everything goes to WORK/gaps-r1/ (kept apart from the harvest's WORK/mixkit, WORK/commons and WORK/candidates so the
shared candidate lists stay as the first stages scored them).

commons      every phrase in QUERIES (pose -> phrases; several restrict to the CC0 stock imports on Commons with
             incategory:Files_from_Pexels / incategory:Images_from_Unsplash), File: namespace, 50 results, the same
             request and record as commons_harvest.py (licence from each file's extmetadata, rules.people_check on
             title, description and categories). Extra rule here: files in a "review needed" category (e.g. "Images
             from Unsplash (review needed)": the CC0 status has not been confirmed) are rejected. Raw results:
             WORK/gaps-r1/commons/queries/<sha1>.json; records: WORK/gaps-r1/commons/files.json.
commons-dl   1280/960/500 px renditions (Wikimedia's standard widths) of files that pass, at most --per-query per
             phrase, best rank first -> WORK/gaps-r1/commons/img/<pageid>.jpg (1200 px long side).
mixkit-crawl pages --from..--to of the tags in CRAWL_TAGS (the harvest read pages 1-4 of 56 tags) ->
             WORK/gaps-r1/mixkit/items.json (cards not already in WORK/mixkit/items.json or WORK/sl/items.json).
mixkit-pick  matches every crawled card (harvest + standing-leaning stage + this crawl) that no stage has scanned
             against TITLE_RX (pose -> regex on title + description), drops people-rule rejects, and writes
             WORK/gaps-r1/mixkit/picked.json (clip id -> target poses).
mixkit-scan  per picked clip: the clip page must pass mixkit_harvest.check_licence (Mixkit Stock Video Free License);
             downloads the 360p hover preview, samples 2 frames a second, runs the app's full model (IMAGE mode, up
             to 4 people), scores every sample against the clip's target poses with score.score_image, keeps the best
             2 samples per target pose (score >= 0.35) as WORK/gaps-r1/mixkit/frames/<id>_<ms>.jpg (+ landmarks in
             WORK/gaps-r1/landmarks/mixkit.jsonl), writes a 1-fps contact strip WORK/gaps-r1/mixkit/strips/<id>.jpg
             for looking at, and deletes the preview. Record: WORK/gaps-r1/mixkit/clips/<id>.json.
detect       detect_images.py's settings on WORK/gaps-r1/commons/img -> WORK/gaps-r1/landmarks/commons.jsonl.
score        score.score_image for every new image against every gap pose -> WORK/gaps-r1/candidates/<poseId>.json
             (best 24, at most 2 frames per clip), index.json.
sheets       sheets.make_sheet on those -> WORK/gaps-r1/sheets/<poseId>.jpg.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parent
sys.path.insert(0, str(TOOLS))
import commons_harvest as ch  # noqa: E402
import mixkit_harvest as mh  # noqa: E402
import rules  # noqa: E402
import score as sc  # noqa: E402
from common import MODELS, WORK, load_poses, read_json, write_json  # noqa: E402

OUT = WORK / "gaps-r1"

GAPS = """stand_hands_in_pockets stand_look_away stand_arms_crossed stand_over_shoulder stand_power_pose stand_sunglasses
lean_shoulder_wall lean_hand_on_wall lean_railing_forward lean_door_frame lean_pole_swing lean_table_edge
pro_arms_crossed pro_desk_edge pro_open_palm pro_table_lean pro_cuff_adjust pro_folder_hold
walk_look_back walk_kerb_step walk_skip walk_hat_hold walk_pockets walk_coffee_run walk_jacket_shoulder
sit_one_knee_up sit_side_saddle sit_steps_sideways sit_hug_knees sit_armchair_lounge sit_chair_backwards
sit_ankle_on_knee sit_high_perch floor_street_squat floor_low_crouch floor_one_knee floor_feet_up floor_overhead_lie
floor_sit_on_heels port_chin_on_hand selfie_hand_on_cheek selfie_look_up selfie_lean_in fun_star_jump fun_tuck_jump
fun_twirl fun_laugh_bend fun_high_kick fun_peace_lean couple_side_hug couple_dip couple_cheek_kiss group_arms_around
group_v_formation group_jump group_walk_line group_sitting_row group_levels group_back_to_back""".split()

UP = "incategory:Images_from_Unsplash|Images_from_Pixabay"  # CC0 stock imports (each file's own licence still checked)

# Whole categories read with generator=categorymembers (50 files a request): (category, max requests)
CATEGORIES = [("Files from Pexels", 10), ("Hands in pockets", 5), ("People leaning", 4), ("Crossed arms", 1),
              ("Cheek kissing", 2), ("Kneeling", 1), ("Leaning", 1)]

# pose -> Commons search phrases (most promising first; the run goes round-robin over the poses after CATEGORIES)
QUERIES = {
    "stand_hands_in_pockets": [f"pockets {UP}"],
    "stand_arms_crossed": [f"crossed arms {UP}", "woman standing with arms crossed full body"],
    "pro_arms_crossed": [f"businessman {UP}", "businesswoman crossed arms standing office full length"],
    "stand_power_pose": [f"hands on hips {UP}", "woman standing hands on hips full length"],
    "stand_over_shoulder": [f"looking back {UP}", "woman looking back over her shoulder full length"],
    "walk_look_back": [f"walking away {UP}"],
    "stand_look_away": [f"woman standing street {UP}"],
    "stand_sunglasses": [f"sunglasses {UP}"],
    "lean_shoulder_wall": [f"leaning wall {UP}"],
    "lean_railing_forward": [f"railing {UP}", "woman leaning on railing forearms view"],
    "lean_door_frame": [f"doorway {UP}", "woman standing in doorway leaning"],
    "lean_pole_swing": [f"pole woman {UP}", "woman swinging around lamppost"],
    "lean_table_edge": [f"sitting on desk {UP}"],
    "pro_desk_edge": ["businessman sitting on edge of desk"],
    "pro_table_lean": [f"leaning table {UP}"],
    "pro_open_palm": [f"speaker gesture {UP}"],
    "pro_cuff_adjust": [f"suit watch {UP}"],
    "pro_folder_hold": [f"folder {UP}", f"notebook woman standing {UP}"],
    "walk_coffee_run": [f"walking coffee {UP}", "woman walking with coffee street"],
    "walk_pockets": [f"walking pockets {UP}"],
    "walk_hat_hold": [f"hat walking {UP}", "woman holding her hat walking"],
    "walk_skip": [f"skipping {UP}"],
    "walk_kerb_step": [f"curb {UP}"],
    "walk_jacket_shoulder": [f"jacket shoulder {UP}"],
    "sit_one_knee_up": [f"sitting stairs {UP}"],
    "sit_steps_sideways": [f"sitting steps {UP}"],
    "sit_hug_knees": [f"knees {UP}", "woman sitting hugging her knees"],
    "sit_side_saddle": [f"woman sitting bench {UP}"],
    "sit_armchair_lounge": [f"armchair {UP}"],
    "sit_chair_backwards": [f"chair backwards {UP}", "man sitting on chair backwards arms on backrest"],
    "sit_ankle_on_knee": [f"man sitting bench {UP}"],
    "sit_high_perch": [f"stool {UP}"],
    "floor_street_squat": [],  # filled in round 1
    "floor_low_crouch": [f"crouching {UP}"],
    "floor_one_knee": [],  # filled in round 1
    "floor_feet_up": [f"lying on stomach {UP}"],
    "floor_overhead_lie": [f"lying grass {UP}"],
    "floor_sit_on_heels": ["seiza woman", "woman kneeling sitting on her heels"],
    "port_chin_on_hand": [f"chin hand {UP}"],
    "selfie_hand_on_cheek": [f"selfie {UP}"],
    "selfie_look_up": ["woman selfie looking up sky"],
    "selfie_lean_in": ["woman taking selfie hand on hip"],
    "fun_star_jump": [f"jumping {UP}", "jumping jacks exercise", "woman jumping arms and legs spread"],
    "fun_tuck_jump": [f"jump beach {UP}"],
    "fun_twirl": [f"twirling {UP}"],
    "fun_laugh_bend": [f"laughing {UP}"],
    "fun_high_kick": [f"kick {UP}"],
    "fun_peace_lean": [f"peace sign {UP}"],
    "couple_side_hug": [f"couple {UP}", "couple standing arms around each other full length"],
    "couple_dip": [f"dancing couple {UP}", "tango dip", "dance dip couple"],
    "couple_cheek_kiss": [],  # filled in round 1
    "group_arms_around": [],  # filled in round 1
    "group_v_formation": [f"group people standing {UP}"],
    "group_jump": [f"friends jumping {UP}"],
    "group_walk_line": [f"friends walking {UP}"],
    "group_sitting_row": [f"friends sitting {UP}"],
    "group_levels": [f"group portrait {UP}"],
    "group_back_to_back": [f"back to back {UP}"],
}

# Mixkit tags to read further pages of (the harvest read pages 1-4 of its 56 tags; new tags start at page 1)
CRAWL_TAGS = {
    # tag: (first page, last page)
    "leaning": (5, 16), "jumping": (5, 6), "kneeling": (1, 2), "hug": (5, 12), "stairs": (5, 13), "selfie": (5, 8),
    "skipping": (5, 12), "crouching": (5, 8), "walking": (5, 12), "posing": (5, 12), "couple": (5, 10),
    "friends": (5, 10), "group": (5, 8), "businessman": (5, 8), "businesswoman": (5, 8), "laughing": (5, 8),
    "sunglasses": (5, 8), "jacket": (5, 8), "wall": (5, 8), "bench": (5, 8), "standing": (5, 8), "dancing": (5, 8),
    "coffee": (1, 6), "desk": (1, 4), "tablet": (1, 3), "railing": (1, 3), "balcony": (1, 3), "door": (1, 3),
    "chair": (1, 4), "stool": (1, 2), "squat": (1, 2), "proposal": (1, 2), "hat": (1, 4), "kick": (1, 3),
    "peace": (1, 2), "tango": (1, 2), "kiss": (1, 4), "steps": (1, 4), "cafe": (1, 3), "notebook": (1, 2),
    "presentation": (1, 3), "confident": (1, 4), "folder": (1, 2), "armchair": (1, 2), "lying": (5, 8),
    "grass": (1, 4), "street": (5, 8), "fashion": (5, 8), "model": (5, 8), "suit": (1, 4), "watch": (1, 2),
    "spinning": (5, 6), "celebration": (5, 6), "salsa": (1, 2), "ballroom": (1, 2), "doorway": (1, 2),
    "pocket": (1, 2), "pockets": (1, 1), "arms-crossed": (1, 1), "crossed-arms": (1, 1), "hands-on-hips": (1, 1),
    "peace-sign": (1, 1), "piggyback": (1, 1), "lamppost": (1, 1), "pole": (1, 2), "portrait": (5, 8),
    "smiling": (5, 6), "happy": (5, 6), "urban": (5, 6), "city": (5, 6), "park": (5, 6),
    "wedding": (1, 6), "yoga": (1, 4), "meditation": (1, 3), "beach": (5, 8), "wind": (1, 3), "joy": (1, 2),
    "freedom": (1, 3), "pier": (1, 2), "terrace": (1, 2), "rooftop": (1, 2), "bride": (1, 3), "engagement": (1, 2),
    "picnic": (1, 2), "tourist": (1, 3), "vacation": (1, 3), "lifestyle": (5, 8), "outdoors": (1, 3),
}

# pose -> regex on a Mixkit card's title + description
TITLE_RX = {
    "stand_hands_in_pockets": r"pockets?\b(?! watch)",
    "walk_pockets": r"pockets?\b(?! watch)",
    "stand_arms_crossed": r"cross(ed|es|ing)? (her |his |their )?arms|arms (are )?(crossed|folded)|folded arms|folds (her|his) arms",
    "pro_arms_crossed": r"cross(ed|es|ing)? (her |his |their )?arms|arms (are )?(crossed|folded)|folded arms|confident (business|entrepreneur|executive)",
    "stand_power_pose": r"hands? on (her |his |the )?hips?|akimbo|superhero|power pose",
    "stand_over_shoulder": r"over (her|his|the) shoulder|looks? back|looking back|turns? (around|back) (to|and)",
    "walk_look_back": r"walk\w* away|looks? back|looking back|over (her|his|the) shoulder|turns? (around|back)",
    "stand_look_away": r"(standing|posing).{0,30}(looking|looks) (away|to the side|around)|pensive|thoughtful (woman|man) standing",
    "stand_sunglasses": r"sunglasses",
    "lean_shoulder_wall": r"lean\w* (on|against) (a |the )?\w* ?wall|against (a |the )?wall",
    "lean_hand_on_wall": r"lean\w* (on|against) (a |the )?\w* ?wall|hand on (the |a )?wall",
    "lean_railing_forward": r"railing|balcony|handrail|lean\w* (on|over) (a |the )?(fence|bridge|rail)",
    "lean_door_frame": r"door ?(way|frame)|in the door|leaning .*door",
    "lean_pole_swing": r"\bpole\b|lamp ?post|street ?light",
    "lean_table_edge": r"(sit\w*|lean\w*|perch\w*) (on|against) (the |a |his |her )?(edge of (a |the )?)?(desk|table)",
    "pro_desk_edge": r"(sit\w*|lean\w*|perch\w*) (on|against) (the |a |his |her )?(edge of (a |the )?)?(desk|table)",
    "pro_table_lean": r"lean\w* (on|over) (the |a )?(desk|table)|hands on (the |a )?(desk|table)",
    "pro_open_palm": r"present\w*|explain\w*|speech|speaker|gestur\w*|talking to the camera|pitch",
    "pro_cuff_adjust": r"cuff|sleeve|watch(?! (a|the|tv|television|movie|video|film))|checks? the time|adjust\w* (his|her) (tie|jacket|suit)",
    "pro_folder_hold": r"folder|tablet|notebook|clipboard|documents|papers|files|binder",
    "walk_coffee_run": r"(walk\w*|stroll\w*).{0,40}(coffee|cup|latte)|(coffee|cup|latte).{0,40}(walk\w*|stroll\w*)|to go",
    "walk_hat_hold": r"\bhats?\b",
    "walk_skip": r"skip\w*(?! rope)|hop\w*(?!ping)",
    "walk_kerb_step": r"curb|kerb|step\w* (down|off)|crosswalk|crossing the street|steps? down",
    "walk_jacket_shoulder": r"jacket over|over (his|her) shoulder|holding (a |his |her )?jacket",
    "sit_one_knee_up": r"sit\w* on (the |some )?(steps|stairs|staircase)",
    "sit_steps_sideways": r"sit\w* on (the |some )?(steps|stairs|staircase)",
    "sit_hug_knees": r"knees|hugging (her|his) legs|hugs (her|his) legs",
    "sit_side_saddle": r"(sit\w*) on (a |the )?(bench|chair|stool|wall)",
    "sit_armchair_lounge": r"armchair|lounging|lounges",
    "sit_chair_backwards": r"backwards|chair turned|straddl",
    "sit_ankle_on_knee": r"sit\w* on (a |the )?(bench|chair|sofa|couch)|legs crossed",
    "sit_high_perch": r"stool|bar counter|sitting on (a |the )?(high )?(wall|ledge|counter)",
    "floor_street_squat": r"squat\w*|crouch\w*",
    "floor_low_crouch": r"squat\w*|crouch\w*|ties? (his|her) (shoe|lace)|tying",
    "floor_one_knee": r"kneel\w*|on one knee|propos\w*|knee down",
    "floor_feet_up": r"lying on (her|his|their) (stomach|belly|front)|lies on (her|his) (stomach|belly)|face down",
    "floor_overhead_lie": r"(lying|lies|laying) on (the )?(grass|sand|floor|ground|blanket|back)|from above|top view|overhead",
    "floor_sit_on_heels": r"kneel\w*|on (her|his) knees|sitting on (her|his) heels",
    "port_chin_on_hand": r"chin|daydream\w*|cafe|café",
    "selfie_hand_on_cheek": r"selfie",
    "selfie_look_up": r"selfie",
    "selfie_lean_in": r"selfie",
    "fun_star_jump": r"jump\w*|leap\w*",
    "fun_tuck_jump": r"jump\w*|leap\w*",
    "fun_twirl": r"twirl\w*|spin\w*",
    "fun_laugh_bend": r"laugh\w*",
    "fun_high_kick": r"kick\w*|karate|martial",
    "fun_peace_lean": r"peace|v sign|victory sign",
    "couple_side_hug": r"(couple|friends|two (women|men|people)).{0,40}(hug\w*|embrac\w*|arm around|arms around|posing|pose)",
    "couple_dip": r"\bdip\w*|tango|salsa|ballroom|waltz|bachata|couple danc\w*|danc\w* couple",
    "couple_cheek_kiss": r"kiss\w* (her|his|him|on the) cheek|cheek kiss|kiss\w* on the cheek",
    "group_arms_around": r"(friends|group|three|four|five).{0,50}(pos\w*|photo|picture|hug\w*|embrac\w*|arms around)",
    "group_v_formation": r"(friends|group|three|four|five|team).{0,50}(pos\w*|photo|standing)",
    "group_jump": r"(friends|group|people|team).{0,40}jump\w*|jump\w*.{0,40}(friends|group|together)",
    "group_walk_line": r"(friends|group|three|four|five).{0,40}walk\w*",
    "group_sitting_row": r"(friends|group|three|four|five).{0,40}(sit\w*|seated)",
    "group_levels": r"(friends|group|three|four|five).{0,50}(pos\w*|photo|portrait)",
    "group_back_to_back": r"back to back",
}


# ------------------------------------------------------------------------------------------------ Commons
def gap_record(page):
    rec = ch.record(page)
    if rec is None:
        return None
    review = [c for c in rec["categories"] if "review needed" in c.lower() or "license review" in c.lower()
              or "licence review" in c.lower() or "flickrreview" in c.lower() and "fail" in c.lower()]
    if review:
        rec["verdict"]["reject"].append("licence review pending: " + "; ".join(review[:2]))
        rec["ok"] = False
    return rec


def read_category(client, cat, max_req, files, qdir, cached_only):
    cont = None
    for req in range(max_req):
        key = f"category:{cat}:{req}"
        qf = qdir / (hashlib.sha1(key.encode()).hexdigest()[:12] + ".json")
        saved = read_json(qf)
        if saved is None:
            if cached_only:
                return
            params = {"action": "query", "format": "json", "formatversion": "2", "generator": "categorymembers",
                      "gcmtitle": "Category:" + cat, "gcmtype": "file", "gcmlimit": "50", "prop": "imageinfo",
                      "iiprop": "url|size|mime|extmetadata", "iiurlwidth": str(ch.THUMB_W),
                      "iiextmetadatafilter": ch.META, "iiextmetadatalanguage": "en"}
            if cont:
                params.update(cont)
            try:
                d = client.json(ch.API, params)
            except Exception as e:
                print(f"[category {cat}] {e}", flush=True)
                return
            saved = {"phrase": key, "pose": "category", "pages": d.get("query", {}).get("pages", []),
                     "continue": d.get("continue"), "when": time.strftime("%Y-%m-%d %H:%M")}
            write_json(qf, saved)
        n_ok = 0
        for rank, page in enumerate(saved["pages"]):
            rec = gap_record(page)
            if not rec:
                continue
            old = files.get(str(rec["pageid"]))
            rec["found"] = old["found"] if old else []
            hit = {"pose": "category", "phrase": key, "rank": rank}
            if hit not in rec["found"]:
                rec["found"].append(hit)
            files[str(rec["pageid"])] = rec
            n_ok += rec["ok"]
        print(f"[category {cat} #{req}] {len(saved['pages'])} files, {n_ok} pass", flush=True)
        write_json(OUT / "commons" / "files.json", files)
        cont = saved.get("continue")
        if not cont:
            return
        cont = {k: v for k, v in cont.items() if k != "continue"} | {"continue": cont.get("continue", "")}


def cmd_commons(args):
    client = ch.http()
    qdir = OUT / "commons" / "queries"
    qdir.mkdir(parents=True, exist_ok=True)
    files = read_json(OUT / "commons" / "files.json", {}) or {}
    poses = args.poses.split(",") if args.poses else list(QUERIES)
    if not args.poses and not args.no_categories:
        for cat, max_req in CATEGORIES:
            read_category(client, cat, max_req, files, qdir, args.cached_only)
    depth = max(len(QUERIES[p]) for p in poses)
    plan = [(p, QUERIES[p][k]) for k in range(depth) for p in poses if k < len(QUERIES[p])]
    if args.priority:  # run these (pose|phrase per line) first, in this order: the rate limit allows ~1 query a minute
        first = [tuple(l.split("|", 1)) for l in Path(args.priority).read_text().splitlines() if "|" in l]
        plan = first + [x for x in plan if x not in first]
    seen_phrase = set()
    for i, (pid, phrase) in enumerate(plan):
        if phrase in seen_phrase:
            continue
        seen_phrase.add(phrase)
        qf = qdir / (hashlib.sha1(phrase.encode()).hexdigest()[:12] + ".json")
        saved = read_json(qf)
        if saved is None:
            if args.cached_only:
                continue
            try:
                pages = ch.search(client, phrase, 50)
            except Exception as e:
                print(f"[{pid}] {phrase!r}: {e}", flush=True)
                continue
            saved = {"phrase": phrase, "pose": pid, "pages": pages, "when": time.strftime("%Y-%m-%d %H:%M")}
            write_json(qf, saved)
        n_ok = 0
        for rank, page in enumerate(saved["pages"]):
            rec = gap_record(page)
            if not rec:
                continue
            old = files.get(str(rec["pageid"]))
            rec["found"] = old["found"] if old else []
            hit = {"pose": pid, "phrase": phrase, "rank": rank}
            if hit not in rec["found"]:
                rec["found"].append(hit)
            files[str(rec["pageid"])] = rec
            n_ok += rec["ok"]
        print(f"[{i + 1}/{len(plan)} {pid}] {phrase!r}: {len(saved['pages'])} files, {n_ok} pass", flush=True)
        write_json(OUT / "commons" / "files.json", files)
    print(f"{len(files)} files, {sum(r['ok'] for r in files.values())} pass; {client.requests} requests", flush=True)


def cmd_commons_dl(args):
    from PIL import Image
    client = ch.http()
    files = read_json(OUT / "commons" / "files.json", {}) or {}
    harvest = read_json(WORK / "commons" / "files.json", {}) or {}
    img_dir = OUT / "commons" / "img"
    img_dir.mkdir(parents=True, exist_ok=True)
    per_phrase = {}
    for rec in files.values():
        if not rec["ok"] or not rec.get("thumb_url"):
            continue
        if (WORK / "commons" / "img" / f"{rec['pageid']}.jpg").exists() and str(rec["pageid"]) in harvest:
            continue  # the harvest already has (and scored) it
        for hit in rec["found"]:
            per_phrase.setdefault(hit["phrase"], []).append((hit["rank"], rec))
    chosen = {}
    for d in range(200):
        for phrase, lst in per_phrase.items():
            if d >= args.per_query and not phrase.startswith("category:"):
                continue
            lst.sort(key=lambda t: t[0])
            if d < len(lst):
                chosen.setdefault(str(lst[d][1]["pageid"]), lst[d][1])
    todo = [r for r in chosen.values() if not (img_dir / f"{r['pageid']}.jpg").exists()]
    print(f"{len(chosen)} chosen, {len(todo)} to download", flush=True)
    for i, rec in enumerate(todo):
        try:
            data = client.get(ch.standard_thumb(rec), timeout=90)
            im = Image.open(io.BytesIO(data)).convert("RGB")
            s = 1200 / max(im.size)
            if s < 1:
                im = im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS)
            im.save(img_dir / f"{rec['pageid']}.jpg", quality=90)
        except Exception as e:
            print(f"  {rec['title']}: {e}", flush=True)
            continue
        if i % 20 == 0:
            print(f"  {i + 1}/{len(todo)} {rec['title'][:80]}", flush=True)
    print("done", flush=True)


# ------------------------------------------------------------------------------------------------ Mixkit
def known_items():
    items = dict(read_json(WORK / "mixkit" / "items.json", {}) or {})
    items.update(read_json(WORK / "sl" / "items.json", {}) or {})
    return items


def scanned_ids():
    done = set()
    for d in (WORK / "mixkit" / "clips", WORK / "couple-group" / "clips", OUT / "mixkit" / "clips"):
        done |= {p.stem for p in d.glob("*.json")}
    done |= set(read_json(WORK / "sl" / "scan.json", {}) or {})
    return done


def cmd_mixkit_crawl(args):
    client = mh.http()
    known = known_items()
    out = read_json(OUT / "mixkit" / "items.json", {}) or {}
    tags = args.tags.split(",") if args.tags else list(CRAWL_TAGS)
    for tag in tags:
        first, last = CRAWL_TAGS.get(tag, (1, 2))
        for page in range(first, last + 1):
            url = f"{mh.BASE}/free-stock-video/{tag}/" + (f"?page={page}" if page > 1 else "")
            try:
                text = client.get(url).decode("utf-8", "replace")
            except Exception as e:
                print(f"[{tag}] page {page}: {e}", flush=True)
                break
            found = mh.parse_grid(text)
            new = 0
            for it in found:
                k = str(it["id"])
                if k in known:
                    continue
                if k not in out:
                    it["tags_seen"] = [tag]
                    out[k] = it
                    new += 1
                elif tag not in out[k]["tags_seen"]:
                    out[k]["tags_seen"].append(tag)
            print(f"[{tag}] page {page}: {len(found)} cards, {new} new (total new {len(out)})", flush=True)
            if not found or f"page={page + 1}" not in text:
                break
        write_json(OUT / "mixkit" / "items.json", out)
    print(f"{len(out)} new cards", flush=True)


def cmd_mixkit_pick(args):
    items = known_items()
    items.update(read_json(OUT / "mixkit" / "items.json", {}) or {})
    done = scanned_ids()
    rx = {p: re.compile(r, re.I) for p, r in TITLE_RX.items()}
    picked, rej = {}, 0
    for k, it in items.items():
        if k in done:
            continue
        text = it["title"] + " | " + it.get("description", "")
        poses = [p for p, r in rx.items() if r.search(text)]
        if not poses:
            continue
        verdict = rules.people_check(it["title"], it.get("description", ""), [])
        verdict["reject"] = [r for r in verdict["reject"] if not r.startswith(("public/event", "not a photo"))]
        if verdict["reject"]:
            rej += 1
            continue
        if re.search(r"\b(close[- ]?up|closeup|hands of|detail|drone|aerial|animation|animated|3d|abstract|render|"
                     r"silhouette|timelapse|time lapse)\b", text, re.I) and not re.search(r"top view|overhead", text, re.I):
            continue
        picked[k] = {"title": it["title"], "poses": poses, "vertical": bool(it.get("vertical"))}
    write_json(OUT / "mixkit" / "picked.json", picked, indent=1)
    by_pose = {}
    for k, v in picked.items():
        for p in v["poses"]:
            by_pose.setdefault(p, []).append(k)
    for p in GAPS:
        print(f"{p}: {len(by_pose.get(p, []))}")
    print(f"{len(picked)} clips picked, {rej} rejected by the people rules", flush=True)


class Det:
    def __init__(self):
        from mediapipe.tasks.python import BaseOptions
        from mediapipe.tasks.python.vision import PoseLandmarker, PoseLandmarkerOptions, RunningMode
        self.lm = PoseLandmarker.create_from_options(PoseLandmarkerOptions(
            base_options=BaseOptions(model_asset_path=str(MODELS / "pose_landmarker_full.task")),
            running_mode=RunningMode.IMAGE, num_poses=4, min_pose_detection_confidence=0.5,
            min_pose_presence_confidence=0.5, min_tracking_confidence=0.5, output_segmentation_masks=False))

    def people(self, bgr):
        import cv2
        import numpy as np
        from mediapipe import Image, ImageFormat
        h, w = bgr.shape[:2]
        s = 1280 / max(h, w)
        if s < 1:
            bgr = cv2.resize(bgr, (round(w * s), round(h * s)), interpolation=cv2.INTER_AREA)
        rgb = np.ascontiguousarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
        res = self.lm.detect(Image(image_format=ImageFormat.SRGB, data=rgb))
        people = [[[round(l.x, 5), round(l.y, 5), round(l.z, 5),
                    round(l.visibility if l.visibility is not None else 1.0, 4),
                    round(l.presence if l.presence is not None else 1.0, 4)] for l in q] for q in res.pose_landmarks]

        def area(q):
            pts = [(l[0], l[1]) for l in q if l[3] >= 0.5]
            if len(pts) < 2:
                return 0.0
            xs, ys = zip(*pts)
            return (max(xs) - min(xs)) * (max(ys) - min(ys))
        people.sort(key=lambda q: -area(q))
        return people


def cmd_mixkit_scan(args):
    import cv2
    from PIL import Image, ImageDraw
    picked = read_json(OUT / "mixkit" / "picked.json", {}) or {}
    items = known_items()
    items.update(read_json(OUT / "mixkit" / "items.json", {}) or {})
    poses = {p["id"]: p for p in load_poses()}
    for d in ("clips", "frames", "strips", "video"):
        (OUT / "mixkit" / d).mkdir(parents=True, exist_ok=True)
    (OUT / "landmarks").mkdir(parents=True, exist_ok=True)
    plan = {}
    if args.plan:
        plan = {str(k): v for k, v in json.loads(Path(args.plan).read_text()).items()}
    ids = args.ids.split(",") if args.ids else (list(plan) if plan else list(picked))
    if args.only_pose:
        ids = [k for k in ids if k in picked and set(picked[k]["poses"]) & set(args.only_pose.split(","))]
    client = mh.http()
    det = Det()
    n = 0
    lmf = (OUT / "landmarks" / "mixkit.jsonl").open("a")
    for k in ids:
        if n >= args.max:
            break
        dest = OUT / "mixkit" / "clips" / f"{k}.json"
        if dest.exists():
            continue
        it = items.get(k)
        if not it:
            print(k, "unknown", flush=True)
            continue
        targets = plan.get(k) or picked.get(k, {}).get("poses") or GAPS
        if args.targets:
            targets = args.targets.split(",")
        rec = {"id": int(k), "page": it["page"], "title": it["title"], "description": it.get("description", ""),
               "preview": it["preview"], "targets": targets, "checked": time.strftime("%Y-%m-%d"), "scanned": False}
        verdict = rules.people_check(it["title"], it.get("description", ""), [])
        verdict["reject"] = [r for r in verdict["reject"] if not r.startswith(("public/event", "not a photo"))]
        rec["flags"] = verdict["flags"]
        if verdict["reject"]:
            rec["skip"] = "people rules: " + "; ".join(verdict["reject"])
            write_json(dest, rec, indent=1)
            continue
        try:
            lic = mh.check_licence(client.get(it["page"]).decode("utf-8", "replace"))
        except Exception as e:
            print(k, "page error", e, flush=True)
            continue
        rec["licence"] = lic
        if not lic["ok"]:
            rec["skip"] = "not Mixkit Stock Video Free License"
            write_json(dest, rec, indent=1)
            print(f"{k}: licence {lic['panel_key']} / {lic.get('copyright_notice')} -> skip", flush=True)
            continue
        vpath = OUT / "mixkit" / "video" / f"{k}.mp4"
        try:
            client.download(it["preview"], vpath, timeout=120)
            cap = cv2.VideoCapture(str(vpath))
            fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
            step_score = max(1, round(fps / 2))
            samples, strip = [], []
            i = 0
            while True:
                ok = cap.grab()
                if not ok:
                    break
                if i % step_score == 0:
                    ok, bgr = cap.retrieve()
                    if not ok:
                        break
                    t = i / fps
                    if (i // step_score) % 2 == 0:
                        strip.append((t, bgr.copy()))
                    people = det.people(bgr)
                    h, w = bgr.shape[:2]
                    lrec = {"id": f"gmixkit:{k}_{round(t * 1000):06d}", "source": "mixkit", "w": w, "h": h,
                            "people": people}
                    res = sc.score_image(lrec, [poses[p] for p in targets if p in poses]) if people else []
                    samples.append((t, bgr, lrec, res))
                i += 1
            cap.release()
        except Exception as e:
            print(k, "video error", e, flush=True)
            vpath.unlink(missing_ok=True)
            continue
        vpath.unlink(missing_ok=True)
        keep = {}
        for t, bgr, lrec, res in samples:
            for r in res:
                if r["score"] >= 0.35:
                    keep.setdefault(r["pose"], []).append((r["rank"], r["score"], t, bgr, lrec))
        frames = {}
        for p, lst in keep.items():
            lst.sort(key=lambda x: -x[0])
            for rank, score, t, bgr, lrec in lst[:2]:
                name = f"{k}_{round(t * 1000):06d}.jpg"
                if name not in frames:
                    cv2.imwrite(str(OUT / "mixkit" / "frames" / name), bgr, [cv2.IMWRITE_JPEG_QUALITY, 92])
                    lrec = dict(lrec, image=str(OUT / "mixkit" / "frames" / name))
                    lmf.write(json.dumps(lrec) + "\n")
                    frames[name] = {"file": name, "t": round(t, 3), "poses": []}
                frames[name]["poses"].append({"pose": p, "score": round(score, 3)})
        # 1-fps contact strip for looking at (detection can miss hugs, jumps and lying poses)
        if strip:
            h0, w0 = strip[0][1].shape[:2]
            tw, th = (110, 196) if h0 > w0 else (196, 110)
            cols = 10
            rows = (len(strip) + cols - 1) // cols
            sheet = Image.new("RGB", (cols * tw, rows * (th + 12) + 14), (16, 16, 20))
            d = ImageDraw.Draw(sheet)
            d.text((3, 1), f"{k} {it['title'][:120]}", fill=(255, 255, 255))
            for j, (t, bgr) in enumerate(strip):
                im = Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
                im.thumbnail((tw, th))
                x, y = (j % cols) * tw, 14 + (j // cols) * (th + 12)
                sheet.paste(im, (x, y + 12))
                d.text((x + 2, y), f"{t:.1f}s", fill=(255, 255, 0))
            sheet.save(OUT / "mixkit" / "strips" / f"{k}.jpg", quality=80)
        rec.update({"scanned": True, "video": {"fps": round(fps, 3), "samples": len(samples)},
                    "frames": list(frames.values()),
                    "best": {p: round(max(x[1] for x in lst), 3) for p, lst in keep.items()}})
        write_json(dest, rec, indent=1)
        lmf.flush()
        n += 1
        print(f"[{n}] {k} {it['title'][:60]!r}: {len(samples)} samples, frames {len(frames)}, best "
              f"{ {p: round(max(x[1] for x in lst), 2) for p, lst in keep.items()} }", flush=True)
    lmf.close()
    det.lm.close()


# ------------------------------------------------------------------------------------------------ detect, score
def cmd_detect(args):
    import cv2
    det = Det()
    out = OUT / "landmarks" / "commons.jsonl"
    out.parent.mkdir(parents=True, exist_ok=True)
    seen = set()
    if out.exists():
        seen = {json.loads(l)["id"] for l in out.read_text().splitlines() if l.strip()}
    todo = [p for p in sorted((OUT / "commons" / "img").glob("*.jpg")) if f"gcommons:{p.stem}" not in seen]
    print(len(todo), "to detect", flush=True)
    with out.open("a") as f:
        for p in todo:
            bgr = cv2.imread(str(p))
            if bgr is None:
                continue
            h, w = bgr.shape[:2]
            people = det.people(bgr)
            s = min(1.0, 1280 / max(h, w))
            f.write(json.dumps({"id": f"gcommons:{p.stem}", "source": "commons", "image": str(p),
                                "w": round(w * s), "h": round(h * s), "people": people}) + "\n")
    det.lm.close()


def gap_meta():
    meta = {}
    for p in (OUT / "mixkit" / "clips").glob("*.json"):
        c = read_json(p)
        if not c or not c.get("scanned"):
            continue
        for f in c["frames"]:
            meta["gmixkit:" + Path(f["file"]).stem] = {
                "source": "Mixkit", "clip": c["id"], "time_s": f["t"], "title": c["title"], "page": c["page"],
                "description": c.get("description", ""),
                "licence": {"name": c["licence"]["panel_licence"], "url": "https://mixkit.co/license/#videoFree"},
                "flags": list(c.get("flags", [])), "author": c["licence"].get("author")}
    files = read_json(OUT / "commons" / "files.json", {}) or {}
    for pid, r in files.items():
        lic = r["licence"]
        meta["gcommons:" + pid] = {
            "source": "Wikimedia Commons", "title": r["title"], "page": r["page"], "file_url": r["file_url"],
            "description": r.get("description", ""), "author": lic.get("artist"),
            "licence": {"name": lic.get("short_name"), "url": lic.get("url")},
            "flags": list(r["verdict"]["flags"]), "ok": r["ok"], "pageid": int(pid)}
    return meta


def cmd_score(args):
    poses = [p for p in load_poses() if p["id"] in GAPS]
    meta = gap_meta()
    per = {p["id"]: [] for p in poses}
    for lf in (OUT / "landmarks").glob("*.jsonl"):
        for line in lf.read_text().splitlines():
            try:
                rec = json.loads(line)
            except json.JSONDecodeError:
                continue
            m = meta.get(rec["id"])
            if m is None or m.get("ok") is False or not rec.get("image"):
                continue
            for r in sc.score_image(rec, poses):
                r.update({"id": rec["id"], "image": rec["image"], "w": rec["w"], "h": rec["h"]})
                per[r["pose"]].append(r)
    (OUT / "candidates").mkdir(parents=True, exist_ok=True)
    index = []
    for pose in poses:
        cat_re, own_re = sc.text_terms(pose)
        for r in per[pose["id"]]:
            m = meta[r["id"]]
            text = (m.get("title") or "") + " " + (m.get("description") or "")
            r["text_match"] = [k for k, rx in (("category", cat_re), ("name", own_re)) if rx and rx.search(text)]
            r["rank"] = round(r["rank"] + 0.08 * len(r["text_match"]), 4)
        lst = sorted(per[pose["id"]], key=lambda r: -r["rank"])
        kept, per_clip = [], {}
        for r in lst:
            m = meta[r["id"]]
            if m["source"] == "Mixkit":
                if per_clip.get(m["clip"], 0) >= 2:
                    continue
                per_clip[m["clip"]] = per_clip.get(m["clip"], 0) + 1
            kept.append({**r, **{k: v for k, v in m.items() if k != "ok"}})
            if len(kept) >= args.top:
                break
        elig = [r for r in lst if r["eligible"]]
        write_json(OUT / "candidates" / f"{pose['id']}.json", {
            "pose": {"id": pose["id"], "name": pose["name"], "category": pose["category"],
                     "people": len(pose["figures"]), "frameAspect": pose["frameAspect"]},
            "scored": len(lst), "eligible": len(elig), "candidates": kept}, indent=1)
        index.append({"id": pose["id"], "best": max((r["score"] for r in elig), default=0.0), "eligible": len(elig),
                      "scored": len(lst)})
        print(f"{pose['id']}: {len(lst)} scored, {len(elig)} eligible, best {index[-1]['best']:.3f}", flush=True)
    write_json(OUT / "candidates" / "index.json", index, indent=1)


def cmd_sheets(args):
    import sheets as sh
    poses_by_id = {p["id"]: p for p in load_poses()}
    want = set(args.poses.split(",")) if args.poses else None
    for cfile in sorted((OUT / "candidates").glob("*.json")):
        if cfile.name == "index.json" or (want and cfile.stem not in want):
            continue
        sh.make_sheet(cfile, poses_by_id, args.top, OUT / "sheets")
    print(OUT / "sheets", flush=True)


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("commons")
    a.add_argument("--poses")
    a.add_argument("--cached-only", action="store_true")
    a.add_argument("--no-categories", action="store_true")
    a.add_argument("--priority", help="file of 'pose|phrase' lines to search first")
    a = sub.add_parser("commons-dl")
    a.add_argument("--per-query", type=int, default=12)
    a = sub.add_parser("mixkit-crawl")
    a.add_argument("--tags")
    sub.add_parser("mixkit-pick")
    a = sub.add_parser("mixkit-scan")
    a.add_argument("--ids")
    a.add_argument("--targets")
    a.add_argument("--only-pose")
    a.add_argument("--plan", help="JSON file {clip id: [target pose ids]} (chosen by hand from the titles)")
    a.add_argument("--max", type=int, default=80)
    sub.add_parser("detect")
    a = sub.add_parser("score")
    a.add_argument("--top", type=int, default=24)
    a = sub.add_parser("sheets")
    a.add_argument("--poses")
    a.add_argument("--top", type=int, default=8)
    args = ap.parse_args()
    {"commons": cmd_commons, "commons-dl": cmd_commons_dl, "mixkit-crawl": cmd_mixkit_crawl,
     "mixkit-pick": cmd_mixkit_pick, "mixkit-scan": cmd_mixkit_scan, "detect": cmd_detect, "score": cmd_score,
     "sheets": cmd_sheets}[args.cmd](args)


if __name__ == "__main__":
    main()
