#!/usr/bin/env python3
"""
gaps-r2_search.py: gap filling, round 2: more targeted searches for the poses that round 1 left without a photo
(GAPS below), with the harvest's licence and people rules.

    PY=promo/.venv/bin/python
    $PY -I tools/pose-photos/picks/gaps-r2_search.py commons-search [--keys k1,k2]  # titles only (slow: rate limits)
    $PY -I tools/pose-photos/picks/gaps-r2_search.py commons-thumbs [--max 3000]    # 500 px thumbnails, title rules
    $PY -I tools/pose-photos/picks/gaps-r2_search.py commons-detect                 # full model on the thumbnails
    $PY -I tools/pose-photos/picks/gaps-r2_search.py commons-meta [--top 14]        # licence metadata, best first
    $PY -I tools/pose-photos/picks/gaps-r2_search.py mixkit-crawl [--tags a,b]      # more Mixkit tags and searches
    $PY -I tools/pose-photos/picks/gaps-r2_search.py mixkit-pick                    # unscanned clips by title
    $PY -I tools/pose-photos/picks/gaps-r2_search.py mixkit-scan [--plan f.json] [--max 80]
    $PY -I tools/pose-photos/picks/gaps-r2_search.py score                          # gap poses x new images
    $PY -I tools/pose-photos/picks/gaps-r2_search.py sheets [--poses a,b]

Everything goes to WORK/gaps-r2/ (apart from the harvest's and round 1's folders, whose candidates stay as scored).

Commons, done differently from round 1 because the API allows this environment about one request a minute (HTTP 429
with Retry-After ~50 s) while thumb.wikimedia.org serves standard-width thumbnails at about one a second:
commons-search  each query in COMMONS (key -> CirrusSearch phrase: whole categories with incategory:/deepcat:, the CC0
                stock imports without the "(review needed)" Unsplash files, and pose phrases), list=search in the File:
                namespace, up to 500 titles a request (srprop snippet), no metadata. Raw results:
                WORK/gaps-r2/commons/search/<key>.json.
commons-thumbs  every title found that is not already in the harvest's or round 1's records, whose title + snippet pass
                rules.people_check (no categories yet) and that is a JPEG/PNG/WebP: the 500 px standard thumbnail
                from thumb.wikimedia.org (path from the MD5 of the file name), queries in COMMONS order, at most
                --per-query each -> WORK/gaps-r2/commons/thumb/<pageid>.jpg.
commons-detect  the app's full model (IMAGE mode, up to 4 people) -> WORK/gaps-r2/landmarks/commons.jsonl.
commons-meta    scores the thumbnails against the gap poses, then fetches imageinfo + extmetadata (50 files a request)
                for the best --top per pose (and every file that scores >= --min-score against a gap pose): the harvest's
                record() (licence allow-list, Restrictions, people rules on title, description and categories, size)
                plus round 1's "review needed" reject -> WORK/gaps-r2/commons/files.json. Only files that pass reach
                the candidate lists.
Mixkit: round 1's crawl / pick / scan (gaps-r1_search.py), with CRAWL_TAGS and TITLE_RX below; clips scanned by any
earlier stage are skipped; each clip page's licence is checked before anything is downloaded.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import sys
import time
import urllib.parse
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parent
sys.path.insert(0, str(TOOLS))
import commons_harvest as ch  # noqa: E402
import rules  # noqa: E402
import score as sc  # noqa: E402
from common import WORK, Polite, load_poses, read_json, write_json  # noqa: E402

_spec = importlib.util.spec_from_file_location("gaps_r1_search", HERE / "gaps-r1_search.py")
g1 = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(g1)

OUT = WORK / "gaps-r2"
R1 = WORK / "gaps-r1"

GAPS = """stand_hands_in_pockets stand_look_away stand_arms_crossed stand_over_shoulder stand_power_pose stand_sunglasses
lean_shoulder_wall lean_hand_on_wall lean_railing_forward lean_door_frame lean_pole_swing lean_table_edge
pro_arms_crossed pro_desk_edge pro_open_palm pro_table_lean pro_cuff_adjust pro_folder_hold
walk_look_back walk_kerb_step walk_skip walk_hat_hold walk_pockets walk_coffee_run walk_jacket_shoulder
sit_one_knee_up sit_side_saddle sit_steps_sideways sit_hug_knees sit_armchair_lounge sit_chair_backwards
sit_ankle_on_knee sit_high_perch floor_low_crouch floor_feet_up floor_overhead_lie floor_sit_on_heels
port_chin_on_hand selfie_hand_on_cheek selfie_look_up selfie_lean_in fun_star_jump fun_tuck_jump fun_laugh_bend
fun_high_kick fun_peace_lean couple_side_hug couple_dip group_v_formation group_jump group_walk_line group_sitting_row
group_levels group_back_to_back""".split()

UNS = 'incategory:Images_from_Unsplash -incategory:"Images_from_Unsplash_(review_needed)"'
PEOPLE = "woman OR man OR girl OR guy OR people OR person OR couple OR friends OR lady OR model OR businessman OR " \
         "businesswoman OR men OR women"

# key -> (CirrusSearch phrase, sroffset). Order = priority (the API allows about one request a minute here).
COMMONS: dict[str, tuple[str, int]] = {
    # CC0 stock imports: every people photo (the reviewed Unsplash files, PxHere, Pixabay)
    "uns-people-0": (f"{UNS} {PEOPLE}", 0),
    "pxhere-all-0": ("incategory:Images_from_Pxhere", 0),
    "uns-woman": (f"{UNS} woman", 0),
    "pixabay-people-0": (f"incategory:Images_from_Pixabay {PEOPLE}", 0),
    "uns-man": (f"{UNS} man", 0),
    "pixabay-woman": ("incategory:Images_from_Pixabay woman", 0),
    "uns-girl": (f"{UNS} girl", 0),
    # whole pose categories (names checked with prop=categoryinfo first: WORK/gaps-r2/commons/catguess.json)
    "cat-crossed-arms": ("incategory:Men_with_crossed_arms|Women_with_crossed_arms|Crossed_arms", 0),
    "cat-leaning-walls": ("incategory:Women_leaning_against_walls|Men_leaning_against_walls|"
                          "People_leaning_against_walls", 0),
    "cat-stairs": ("incategory:Women_sitting_on_stairs|Men_sitting_on_stairs|People_sitting_on_steps|"
                   "People_sitting_on_stairs", 0),
    "cat-crouching": ("incategory:Women_crouching|Men_crouching|People_crouching|Crouching", 0),
    "cat-kneeling": ("incategory:Women_kneeling|Men_kneeling|People_kneeling|Kneeling|Seiza", 0),
    "cat-jumping": ("incategory:Women_jumping|Men_jumping", 0),
    "cat-hands-on-hips": ("incategory:Hands_on_hips|Men_with_hands_on_hips|Women_with_hands_on_hips", 0),
    "cat-chin-on-hand": ("incategory:Chin_on_hand", 0),
    "cat-desks-tables": ("incategory:People_sitting_on_desks|People_sitting_on_tables", 0),
    "cat-looking-back": ("incategory:People_looking_back|Looking_back", 0),
    "cat-looking-away": ("incategory:People_looking_away", 0),
    "cat-laughing": ("incategory:Women_laughing|Men_laughing", 0),
    "cat-armchairs-stools": ("incategory:People_sitting_on_armchairs|People_sitting_on_stools", 0),
    "cat-lying": ("incategory:People_lying_on_the_ground|People_lying_on_grass|Women_lying_on_grass|"
                  "Men_lying_on_grass", 0),
    "cat-floor-grass": ("incategory:People_sitting_on_the_floor|People_sitting_on_grass", 0),
    "cat-crossed-legs": ('deepcat:"Crossed legs"', 0),
    "cat-back-to-back": ('deepcat:"Back to back"', 0),
    "cat-dip-tango": ('incategory:"Dip_(dance_move)"|Tango_dancers', 0),
    "cat-hats-cups-skip": ("incategory:People_holding_hats|People_with_cups|Skipping", 0),
    "cat-clipboards": ("incategory:People_holding_clipboards", 0),
    "cat-selfies-taking": ("incategory:Women_taking_selfies|Men_taking_selfies", 0),
    "cat-sunglasses": ("incategory:People_with_sunglasses|People_wearing_sunglasses", 0),
    "uns-people": (f"{UNS} people OR friends OR couple", 0),
    "pixabay-man": ("incategory:Images_from_Pixabay man", 0),
    "pixabay-girl": ("incategory:Images_from_Pixabay girl", 0),
    "cat-chairs": ("incategory:People_sitting_on_chairs", 0),
    "cat-benches": ("incategory:People_sitting_on_benches", 0),
    "cat-people-leaning-deep": ('deepcat:"People leaning"', 0),
    "cat-jumping-deep": ('deepcat:"People jumping"', 0),
    "cat-selfies-women": ("incategory:Selfies woman OR women OR girl", 0),
    # a photographer's dress-code series with numbered poses found in this round (consenting model, CC BY-SA)
    "tomar-wear-pose": ('"wear - pose" OR "casual - pose" OR "attire - pose" OR "dress code" pose', 0),
    "tomar-cat": ('incategory:"Pictures_by_Tobias_\\"ToMar\\"_Maier" woman OR man OR wear OR pose', 0),
    # a clothing company's product photos with professional models, released under CC BY 2.0 (found in this round)
    "rydale": ('"Rydale Clothing" OR "rydale.com"', 0),
    "rydale-500": ('"Rydale Clothing" OR "rydale.com"', 500),
    # CC0 stock photos imported from rawpixel.com (most of the category is public-domain artwork scans) and photos
    # tagged as taken with the model's full consent
    "rawpixel-woman": ("incategory:Images_from_Rawpixel woman -painting -illustration -vintage", 0),
    "rawpixel-man": ("incategory:Images_from_Rawpixel man -painting -illustration -vintage", 0),
    "rawpixel-people": ("incategory:Images_from_Rawpixel people -painting -illustration -vintage", 0),
    "consent-woman": ("incategory:Full_consent woman", 0),
    "consent-man": ("incategory:Full_consent man", 0),
    # pose phrases (all of Commons)
    "q-chair-backwards": ("sitting backwards on a chair OR straddling a chair OR chair backwards", 0),
    "q-hug-knees": ('"hugging her knees" OR "hugging his knees" OR "hugging knees" OR "arms around knees"', 0),
    "q-railing": ('"leaning on the railing" OR "leaning on a railing" OR "leaning over the railing" OR '
                  '"leaning on railing"', 0),
    "q-lying-stomach": ('"lying on her stomach" OR "lying on his stomach" OR "lying on stomach" OR '
                        '"lying on the stomach" OR "lying on her belly"', 0),
    "q-star-jump": ('"star jump" OR "jumping jack" OR "jumping jacks" OR "jump for joy"', 0),
    "q-desk-perch": ('"sitting on a desk" OR "sitting on the desk" OR "sitting on the edge" OR "sitting on a table" '
                     'OR "sitting on the table"', 0),
    "q-doorway": ('"leaning in the doorway" OR "leaning in a doorway" OR "leaning against the door" OR '
                  '"leaning on the door" OR "standing in the doorway"', 0),
    "q-peace-sign": ('"peace sign" OR "v sign" OR "victory sign" woman OR man', 0),
    "q-squat": ('squatting OR crouching woman OR man -sport', 0),
    "q-kneel-heels": ('"sitting on her heels" OR "sitting on his heels" OR "kneeling" woman', 0),
    "q-group-jump": ('"jumping together" OR "friends jumping" OR "group jumping" OR "people jumping"', 0),
    "q-friends-sitting": ('"friends sitting" OR "three friends" OR "four friends" OR "group of friends"', 0),
    "q-back-to-back": ('"back to back" people OR friends OR women OR men', 0),
    "q-stool": ('"sitting on a stool" OR "sitting on a bar stool" OR "sitting on stool" OR "high stool"', 0),
    "q-armchair": ('"sitting in an armchair" OR "in an armchair" OR "sitting in armchair" woman OR man', 0),
    "q-ankle-knee": ('"legs crossed" OR "crossed legs" OR "ankle on knee" man sitting', 0),
    "q-chin-hand": ('"chin on hand" OR "chin in hand" OR "hand on chin" OR "chin resting" woman OR man', 0),
    "q-lamppost": ('lamppost OR "lamp post" OR "street light" woman OR man holding OR leaning OR swinging', 0),
    "q-dip": ('"dance dip" OR "tango" couple dancing street', 0),
    "q-side-hug": ('"arm around" OR "arms around" couple OR friends standing', 0),
    "q-walk-coffee": ('"walking with coffee" OR "coffee to go" OR "holding a coffee" OR "takeaway coffee" woman OR man', 0),
    "q-hat-hold": ('"holding her hat" OR "holding his hat" OR "hand on hat" OR "holding hat"', 0),
    "q-laugh": ('"laughing" woman OR man OR friends', 0),
    "q-sunglasses": ('"sunglasses" woman OR man standing OR street OR fashion', 0),
    "q-pockets": ('"hands in pockets" OR "hands in his pockets" OR "hands in her pockets" OR "hand in pocket"', 0),
    "q-skip": ('skipping OR "skips" woman OR girl OR man street OR beach OR field -rope', 0),
    "q-kick": ('"high kick" OR "front kick" OR "kicking" woman OR man', 0),
    "q-folder": ('"holding a folder" OR "holding folder" OR "holding a notebook" OR "holding a tablet" OR '
                 '"holding documents" businesswoman OR businessman OR woman OR man', 0),
    "q-speaker": ('"gesturing" OR "explaining" OR "presenting" businesswoman OR businessman OR woman OR man', 0),
    "q-wall-lean": ('"leaning against a wall" OR "leaning against the wall" OR "leaning on a wall" OR '
                    '"leaning on the wall" OR "against a wall"', 0),
    "q-look-back": ('"looking back" OR "looks back" OR "over her shoulder" OR "over his shoulder" woman OR man', 0),
    "q-arms-crossed": ('"arms crossed" OR "crossed arms" OR "folded arms" OR "arms folded" woman OR man standing', 0),
    "q-hands-hips": ('"hands on hips" OR "hands on her hips" OR "hands on his hips" OR akimbo', 0),
}


def norm_title(t: str) -> str:
    return t.replace("_", " ").strip()


def thumb_url(title: str, width: int = 500) -> str:
    name = norm_title(title).split(":", 1)[1].replace(" ", "_")
    h = hashlib.md5(name.encode("utf-8")).hexdigest()
    q = urllib.parse.quote(name)
    return f"https://thumb.wikimedia.org/wikipedia/commons/thumb/{h[0]}/{h[:2]}/{q}/{width}px-{q}"


def cmd_commons_search(args):
    client = ch.http()
    sdir = OUT / "commons" / "search"
    sdir.mkdir(parents=True, exist_ok=True)
    keys = args.keys.split(",") if args.keys else list(COMMONS)
    for k in keys:
        f = sdir / f"{k}.json"
        if f.exists():
            continue
        phrase, off = COMMONS[k]
        params = {"action": "query", "format": "json", "formatversion": "2", "list": "search",
                  "srsearch": phrase + " filetype:bitmap", "srnamespace": "6", "srlimit": "500",
                  "sroffset": str(off), "srprop": "snippet", "srinfo": "totalhits"}
        try:
            d = client.json(ch.API, params, timeout=90)
        except Exception as e:
            print(f"[{k}] {e}", flush=True)
            continue
        res = d.get("query", {}).get("search", [])
        write_json(f, {"key": k, "phrase": phrase, "offset": off, "when": time.strftime("%Y-%m-%d %H:%M"),
                       "totalhits": d.get("query", {}).get("searchinfo", {}).get("totalhits"),
                       "results": [{"pageid": r["pageid"], "title": r["title"],
                                    "snippet": rules.strip_html(r.get("snippet", ""))} for r in res]})
        print(f"[{k}] {len(res)} of {d.get('query', {}).get('searchinfo', {}).get('totalhits')}", flush=True)


BOILERPLATE = re.compile(r"(image at the |archive copy at the |gallery at the )?wayback machine|\bauthor\b|"
                         r"\bgallery\b(?= at)|archive copy|unavailable exif|exif infofield.*$", re.I)


def snippet_text(snippet: str) -> str:
    """A search snippet without the Commons template words ("Author", "Gallery at the Wayback Machine", EXIF) that the
    text rules would otherwise read as a public figure or an artwork; the final check reads the real description."""
    return BOILERPLATE.sub(" ", snippet or "")


def known_commons():
    """pageids the harvest or round 1 already recorded (their images were scored against the gap poses)."""
    known = set()
    for f in (WORK / "commons" / "files.json", R1 / "commons" / "files.json"):
        known |= set((read_json(f, {}) or {}).keys())
    return known


def search_hits():
    hits = {}
    for k in COMMONS:
        d = read_json(OUT / "commons" / "search" / f"{k}.json")
        if not d:
            continue
        for rank, r in enumerate(d["results"]):
            h = hits.setdefault(str(r["pageid"]), {"pageid": r["pageid"], "title": r["title"],
                                                   "snippet": r["snippet"], "found": []})
            h["found"].append({"key": k, "rank": rank + d["offset"]})
    return hits


def cmd_commons_thumbs(args):
    known = known_commons()
    hits = search_hits()
    tdir = OUT / "commons" / "thumb"
    tdir.mkdir(parents=True, exist_ok=True)
    order, seen, skipped = [], set(), {"known": 0, "rules": 0, "type": 0}
    by_key = {}
    for pid, h in hits.items():
        for f in h["found"]:
            by_key.setdefault(f["key"], []).append((f["rank"], pid))
    keys = args.keys.split(",") if args.keys else list(COMMONS)
    for k in keys:
        for rank, pid in sorted(by_key.get(k, []))[: args.per_query]:
            if pid in seen:
                continue
            seen.add(pid)
            h = hits[pid]
            if pid in known:
                skipped["known"] += 1
                continue
            if not re.search(r"\.(jpe?g|png|webp)$", h["title"], re.I):
                skipped["type"] += 1
                continue
            v = rules.people_check(h["title"], snippet_text(h["snippet"]), [])
            if v["reject"]:
                skipped["rules"] += 1
                continue
            order.append(pid)
    todo = [p for p in order if not (tdir / f"{p}.jpg").exists() and not (tdir / f"{p}.missing").exists()]
    print(f"{len(hits)} titles, {len(order)} to have, {len(todo)} to download; skipped {skipped}", flush=True)
    if args.reverse:  # a second downloader from the other end of the list
        todo.reverse()
    client = Polite(gaps={"thumb.wikimedia.org": 0.9})
    from PIL import Image
    import io
    for i, pid in enumerate(todo[: args.max]):
        if (tdir / f"{pid}.jpg").exists():  # the other downloader got it
            continue
        try:
            data = client.get(thumb_url(hits[pid]["title"]), tries=3, timeout=60)
            im = Image.open(io.BytesIO(data)).convert("RGB")
            im.save(tdir / f"{pid}.jpg", quality=90)
        except Exception as e:
            (tdir / f"{pid}.missing").write_text(str(e)[:300])
        if i % 50 == 0:
            print(f"  {i + 1}/{len(todo)} {hits[pid]['title'][:80]}", flush=True)
    write_json(OUT / "commons" / "hits.json", hits)
    print("done", flush=True)


def cmd_commons_detect(args):
    import cv2
    det = g1.Det()
    out = OUT / "landmarks" / "commons.jsonl"
    out.parent.mkdir(parents=True, exist_ok=True)
    seen = set()
    if out.exists():
        seen = {json.loads(l)["id"] for l in out.read_text().splitlines() if l.strip()}
    todo = [p for p in sorted((OUT / "commons" / "thumb").glob("*.jpg")) if f"r2commons:{p.stem}" not in seen]
    print(len(todo), "to detect", flush=True)
    with out.open("a") as f:
        for i, p in enumerate(todo):
            bgr = cv2.imread(str(p))
            if bgr is None:
                continue
            h, w = bgr.shape[:2]
            people = det.people(bgr)
            f.write(json.dumps({"id": f"r2commons:{p.stem}", "source": "commons", "image": str(p),
                                "w": w, "h": h, "people": people}) + "\n")
            if i % 200 == 0:
                print(f"  {i}/{len(todo)}", flush=True)
                f.flush()
    det.lm.close()


def gap_poses():
    return [p for p in load_poses() if p["id"] in GAPS]


def iter_landmarks():
    for lf in sorted((OUT / "landmarks").glob("*.jsonl")):
        for line in lf.read_text().splitlines():
            try:
                yield json.loads(line)
            except json.JSONDecodeError:
                continue


def cmd_commons_meta(args):
    """Score the thumbnails, then fetch licence metadata for the best ones."""
    poses = gap_poses()
    files = read_json(OUT / "commons" / "files.json", {}) or {}
    per = {p["id"]: [] for p in poses}
    for rec in iter_landmarks():
        if not rec["id"].startswith("r2commons:") or not rec.get("people"):
            continue
        for r in sc.score_image(rec, poses):
            per[r["pose"]].append((r["rank"], r["score"], rec["id"].split(":", 1)[1]))
    want = []
    for pid, lst in per.items():
        lst.sort(reverse=True)
        for rank, score, fid in lst[: args.top]:
            want.append(fid)
        want += [fid for rank, score, fid in lst if score >= args.min_score]
    want = [w for w in dict.fromkeys(want) if w not in files]
    print(f"{len(want)} files need metadata", flush=True)
    client = ch.http()
    for i in range(0, len(want), 50):
        chunk = want[i:i + 50]
        try:
            d = client.json(ch.API, {"action": "query", "format": "json", "formatversion": "2",
                                     "pageids": "|".join(chunk), "prop": "imageinfo",
                                     "iiprop": "url|size|mime|extmetadata", "iiurlwidth": str(ch.THUMB_W),
                                     "iiextmetadatafilter": ch.META, "iiextmetadatalanguage": "en"}, timeout=90)
        except Exception as e:
            print(f"meta chunk {i}: {e}", flush=True)
            continue
        for page in d.get("query", {}).get("pages", []):
            rec = g1.gap_record(page)
            if rec:
                files[str(rec["pageid"])] = rec
        write_json(OUT / "commons" / "files.json", files)
        print(f"  meta {i + len(chunk)}/{len(want)}: {sum(r['ok'] for r in files.values())} pass so far", flush=True)


# ------------------------------------------------------------------------------------------------ Mixkit
CRAWL_TAGS = {
    "office": (1, 6), "mirror": (1, 3), "car": (1, 3), "bicycle": (1, 3), "phone": (1, 6), "smartphone": (1, 4),
    "laptop": (1, 4), "fitness": (1, 4), "exercise": (1, 4), "workout": (1, 3), "stretching": (1, 3),
    "jump": (1, 4), "running": (1, 3), "dance": (1, 6), "dancer": (1, 4), "teamwork": (1, 2), "meeting": (1, 3),
    "fence": (1, 2), "bridge": (1, 2), "hipster": (1, 3), "outfit": (1, 3), "clothing": (1, 3), "denim": (1, 2),
    "coat": (1, 2), "winter": (1, 3), "autumn": (1, 3), "spring": (1, 2), "sunset": (1, 4), "field": (1, 3),
    "meadow": (1, 2), "forest": (1, 2), "mountain": (1, 2), "lake": (1, 2), "night": (1, 3), "neon": (1, 2),
    "umbrella": (1, 2), "rain": (1, 2), "camera": (1, 3), "photographer": (1, 3), "garden": (1, 2),
    "flowers": (1, 3), "window": (1, 3), "sofa": (1, 3), "couch": (1, 3), "living-room": (1, 3), "home": (1, 4),
    "table": (1, 3), "restaurant": (1, 3), "shopping": (1, 3), "store": (1, 2), "airport": (1, 2),
    "station": (1, 2), "train": (1, 2), "subway": (1, 2), "graffiti": (1, 3), "skateboard": (1, 2),
    "wall": (9, 14), "stairs": (14, 18), "bench": (9, 14), "steps": (5, 8), "lying": (9, 12), "grass": (5, 8),
    "beach": (9, 12), "happy": (7, 10), "smiling": (7, 10), "lifestyle": (9, 12), "woman": (1, 10),
    "man": (1, 8), "people": (1, 8), "girl": (1, 6), "guy": (1, 3), "young": (1, 4), "laugh": (1, 3),
    "relax": (1, 3), "rest": (1, 2), "chill": (1, 2), "cheerful": (1, 3), "carefree": (1, 3), "playful": (1, 3),
    "excited": (1, 3), "success": (1, 3), "boss": (1, 2), "manager": (1, 2), "executive": (1, 2),
    "entrepreneur": (1, 2), "corporate": (1, 3), "employee": (1, 3), "worker": (1, 2), "coworkers": (1, 2),
    "colleagues": (1, 2), "startup": (1, 2), "work": (1, 3), "headphones": (1, 3), "music": (1, 3),
    "discover/hugging-knees": (1, 2), "discover/sitting-on-a-chair": (1, 3), "discover/leaning-against-a-wall": (1, 3),
    "discover/leaning-on-railing": (1, 2), "discover/jumping-jacks": (1, 2), "discover/star-jump": (1, 1),
    "discover/jumping-in-the-air": (1, 3), "discover/doorway": (1, 2), "discover/lamppost": (1, 1),
    "discover/sitting-on-the-table": (1, 2), "discover/hands-on-hips": (1, 2), "discover/arms-crossed": (1, 2),
    "discover/hands-in-pockets": (1, 2), "discover/looking-back": (1, 2), "discover/taking-a-selfie": (1, 3),
    "discover/woman-taking-a-selfie": (1, 2), "discover/laughing-hard": (1, 1), "discover/skipping-down-the-street": (1, 1),
    "discover/coffee-to-go": (1, 2), "discover/holding-coffee": (1, 2), "discover/holding-a-folder": (1, 1),
    "discover/documents": (1, 2), "discover/clipboard": (1, 1), "discover/office-worker": (1, 2),
    "discover/businesswoman-standing": (1, 1), "discover/businessman-standing": (1, 1), "discover/public-speaking": (1, 2),
    "discover/crouch": (1, 1), "discover/tying-shoes": (1, 1), "discover/dancing-couple": (1, 3), "discover/dip": (1, 1),
    "discover/leaning-on-a-table": (1, 1), "discover/sitting-on-a-stool": (1, 1), "discover/bar-stool": (1, 1),
    "discover/lying-on-the-bed": (1, 1), "discover/feet-up": (1, 1), "discover/lying-down": (1, 2),
    "discover/sitting-on-the-stairs": (1, 2), "discover/sitting-on-the-steps": (1, 2), "discover/armchair": (1, 2),
    "discover/sitting-on-the-floor": (1, 2), "discover/sitting-on-the-ground": (1, 2), "discover/cross-legged": (1, 1),
    "discover/legs-crossed": (1, 1), "discover/chin": (1, 1), "discover/thinking": (1, 2), "discover/daydreaming": (1, 1),
    "discover/sunglasses-on": (1, 1), "discover/putting-on-sunglasses": (1, 1), "discover/adjusting-sunglasses": (1, 1),
    "discover/holding-her-hat": (1, 1), "discover/straw-hat": (1, 2), "discover/walking-away": (1, 2),
    "discover/turning-around": (1, 2), "discover/hand-on-wall": (1, 1), "discover/leaning-on-wall": (1, 2),
    "discover/friends-sitting": (1, 2), "discover/friends-jumping": (1, 1), "discover/group-jumping": (1, 1),
    "discover/friends-walking-together": (1, 2), "discover/friends-posing": (1, 2), "discover/group-photo": (1, 2),
    "discover/group-portrait": (1, 1), "discover/back-to-back": (1, 1), "discover/side-hug": (1, 1),
    "discover/couple-posing": (1, 2), "discover/couple-dancing": (1, 3), "discover/peace": (1, 1),
    "discover/victory": (1, 1), "discover/kick": (1, 2), "discover/martial-arts": (1, 2),
    "discover/skipping": (1, 2), "discover/happy-jump": (1, 1), "discover/jumping-for-joy": (1, 1),
    "discover/jumping-on-the-beach": (1, 1), "discover/jacket": (1, 2), "discover/leather-jacket": (1, 1),
    "discover/suit": (1, 2), "discover/fixing-tie": (1, 1), "discover/cufflinks": (1, 1),
    "discover/checking-watch": (1, 1), "discover/woman-standing": (1, 3), "discover/man-standing": (1, 3),
    "discover/woman-sitting": (1, 3), "discover/man-sitting": (1, 3), "discover/woman-walking": (1, 3),
    "discover/man-walking": (1, 3), "discover/woman-posing": (1, 3), "discover/man-posing": (1, 2),
}

TITLE_RX = dict(g1.TITLE_RX)
TITLE_RX.update({
    "stand_hands_in_pockets": r"pockets?\b(?! watch)|hands in (his|her|their) (jacket|coat|jeans|trousers|pants)",
    "walk_pockets": r"pockets?\b(?! watch)|hands in (his|her|their) (jacket|coat|jeans|trousers|pants)",
    "stand_look_away": r"(standing|posing|stands|poses).{0,40}(look\w*|gaz\w*|star\w*) (away|to the side|around|"
                       r"into the distance|at the horizon|at the view)|pensive|thoughtful|contemplat\w*",
    "stand_over_shoulder": r"over (her|his|the) shoulder|looks? back|looking back|turns? (around|back)|turning around"
                           r"|glanc\w*",
    "stand_power_pose": r"hands? on (her |his |the )?hips?|akimbo|superhero|power pose|confident\w* (pos|stand)",
    "stand_sunglasses": r"sunglasses|shades",
    "lean_railing_forward": r"railing|balcony|handrail|lean\w* (on|over) (a |the )?(fence|bridge|rail|parapet|"
                            r"balustrade|wall)|overlook\w*",
    "lean_door_frame": r"door ?(way|frame)|in the door|leaning .*door|entrance",
    "lean_table_edge": r"(sit\w*|lean\w*|perch\w*) (on|against) (the |a |his |her )?(edge of (a |the )?)?"
                       r"(desk|table|counter|kitchen counter|car|hood)",
    "pro_desk_edge": r"(sit\w*|lean\w*|perch\w*) (on|against) (the |a |his |her )?(edge of (a |the )?)?(desk|table)",
    "walk_coffee_run": r"(walk\w*|stroll\w*).{0,40}(coffee|cup|latte|drink)|(coffee|cup|latte).{0,40}(walk\w*|"
                       r"stroll\w*)|to go|takeaway|take-away",
    "walk_skip": r"skip\w*(?! rope)|hop\w*(?!ping)|bounc\w*|carefree.{0,30}walk\w*|happ\w*.{0,30}walk\w*",
    "sit_one_knee_up": r"sit\w* on (the |some |a )?(steps|stairs|staircase|step|stoop|ledge|wall|rock|bench)",
    "sit_steps_sideways": r"sit\w* on (the |some |a )?(steps|stairs|staircase|step|stoop|ledge)",
    "sit_hug_knees": r"knees|hugging (her|his) legs|hugs (her|his) legs|curled up",
    "sit_armchair_lounge": r"armchair|lounging|lounges|sofa|couch",
    "sit_chair_backwards": r"backwards|chair turned|straddl|backrest|on a chair",
    "sit_ankle_on_knee": r"sit\w* on (a |the )?(bench|chair|sofa|couch|armchair)|legs crossed|cross\w* (his|her) legs",
    "sit_high_perch": r"stool|counter|sitting on (a |the )?(high )?(wall|ledge|counter|railing|fence|table)",
    "floor_low_crouch": r"squat\w*|crouch\w*|ties? (his|her) (shoe|lace)|tying|kneel\w*",
    "floor_feet_up": r"lying on (her|his|their) (stomach|belly|front)|lies on (her|his) (stomach|belly)|face down|"
                     r"lying on the (floor|grass|carpet|rug|sand)",
    "port_chin_on_hand": r"chin|daydream\w*|cafe|café|thinking|pensive|bored",
    "selfie_hand_on_cheek": r"selfie|front camera|self[- ]portrait",
    "selfie_look_up": r"selfie|front camera",
    "selfie_lean_in": r"selfie|front camera",
    "fun_laugh_bend": r"laugh\w*|giggl\w*",
    "fun_high_kick": r"kick\w*|karate|martial|taekwondo|leg up",
    "fun_peace_lean": r"peace|v sign|victory sign|two fingers",
    "couple_side_hug": r"(couple|friends|two (women|men|people)|lovers).{0,50}(hug\w*|embrac\w*|arm around|arms "
                       r"around|posing|pose|cuddl\w*)",
    "couple_dip": r"\bdip\w*|tango|salsa|ballroom|waltz|bachata|couple danc\w*|danc\w* couple|lindy|swing danc\w*",
    "group_walk_line": r"(friends|group|three|four|five|people|team|colleagues|coworkers).{0,40}walk\w*",
    "group_sitting_row": r"(friends|group|three|four|five|colleagues).{0,40}(sit\w*|seated|bench|steps|stairs)",
    "group_back_to_back": r"back to back|backs to",
})


def r2_known_items():
    """Every card the harvest, the standing-leaning stage and round 1 crawled (round 2's own crawl is added on top)."""
    items = dict(read_json(WORK / "mixkit" / "items.json", {}) or {})
    items.update(read_json(WORK / "sl" / "items.json", {}) or {})
    items.update(read_json(R1 / "mixkit" / "items.json", {}) or {})
    return items


def r2_scanned_ids():
    done = set()
    for d in (WORK / "mixkit" / "clips", WORK / "couple-group" / "clips", R1 / "mixkit" / "clips",
              OUT / "mixkit" / "clips"):
        done |= {p.stem for p in d.glob("*.json")}
    done |= set(read_json(WORK / "sl" / "scan.json", {}) or {})
    return done


def wire_r1():
    g1.OUT = OUT
    g1.GAPS = GAPS
    g1.CRAWL_TAGS = CRAWL_TAGS
    g1.TITLE_RX = TITLE_RX
    g1.known_items = r2_known_items
    g1.scanned_ids = r2_scanned_ids


# ------------------------------------------------------------------------------------------------ score, sheets
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
        meta["r2commons:" + pid] = {
            "source": "Wikimedia Commons", "title": r["title"], "page": r["page"], "file_url": r["file_url"],
            "description": r.get("description", ""), "author": lic.get("artist"),
            "licence": {"name": lic.get("short_name"), "url": lic.get("url")},
            "flags": list(r["verdict"]["flags"]), "ok": r["ok"], "pageid": int(pid)}
    return meta


def cmd_score(args):
    poses = gap_poses()
    meta = gap_meta()
    per = {p["id"]: [] for p in poses}
    for rec in iter_landmarks():
        m = meta.get(rec["id"])
        if m is None or m.get("ok") is False or not rec.get("image") or not rec.get("people"):
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
    a = sub.add_parser("commons-search")
    a.add_argument("--keys")
    a = sub.add_parser("commons-thumbs")
    a.add_argument("--max", type=int, default=4000)
    a.add_argument("--per-query", type=int, default=500)
    a.add_argument("--reverse", action="store_true")
    a.add_argument("--keys", help="only these queries, in this order")
    sub.add_parser("commons-detect")
    a = sub.add_parser("commons-meta")
    a.add_argument("--top", type=int, default=14)
    a.add_argument("--min-score", type=float, default=0.62)
    a = sub.add_parser("mixkit-crawl")
    a.add_argument("--tags")
    sub.add_parser("mixkit-pick")
    a = sub.add_parser("mixkit-scan")
    a.add_argument("--ids")
    a.add_argument("--targets")
    a.add_argument("--only-pose")
    a.add_argument("--plan")
    a.add_argument("--max", type=int, default=80)
    a = sub.add_parser("score")
    a.add_argument("--top", type=int, default=24)
    a = sub.add_parser("sheets")
    a.add_argument("--poses")
    a.add_argument("--top", type=int, default=8)
    args = ap.parse_args()
    wire_r1()
    {"commons-search": cmd_commons_search, "commons-thumbs": cmd_commons_thumbs, "commons-detect": cmd_commons_detect,
     "commons-meta": cmd_commons_meta, "mixkit-crawl": g1.cmd_mixkit_crawl, "mixkit-pick": g1.cmd_mixkit_pick,
     "mixkit-scan": g1.cmd_mixkit_scan, "score": cmd_score, "sheets": cmd_sheets}[args.cmd](args)


if __name__ == "__main__":
    main()
