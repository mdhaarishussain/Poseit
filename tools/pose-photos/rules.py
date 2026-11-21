"""Text rules shared by the harvesters: licence allow-list and the people rules (adults only, appropriate, no public
figures or events, photographs only, no watermarks). They run on titles, descriptions and categories, so they can only
reject or flag; every chosen photo is still looked at by a person before it ships (see docs/POSE_PHOTOS.md)."""
from __future__ import annotations

import html
import re

# ---------------------------------------------------------------------------------------------------------- licences

def commons_licence(short: str, url: str = "") -> str | None:
    """Normalised licence key for an allowed Commons licence, else None.
    Allowed: CC0, Public Domain / PDM, CC BY (any version), CC BY-SA (any version). Never NC / ND / GFDL-only /
    "Attribution"-only templates / fair use / unclear."""
    s = (short or "").strip().lower().replace("_", " ")
    u = (url or "").strip().lower()
    if not s:
        return None
    if "nc" in re.findall(r"[a-z]+", s) or "nd" in re.findall(r"[a-z]+", s) or "-nc" in s or "-nd" in s:
        return None
    if "fair use" in s or "non-free" in s or "copyrighted" in s:
        return None
    if s in ("cc0", "cc0 1.0", "cc-zero") or s.startswith("cc0") or "publicdomain/zero" in u:
        return "CC0"
    if s in ("public domain", "pd", "pdm", "public domain mark", "public domain mark 1.0") or s.startswith("pd-") \
            or "publicdomain/mark" in u:
        return "PD"
    m = re.match(r"^cc[ -]by[ -]sa[ -]?(\d\.\d)?", s)
    if m:
        return "CC BY-SA" + (" " + m.group(1) if m.group(1) else "")
    m = re.match(r"^cc[ -]by[ -]?(\d\.\d)?", s)
    if m and "sa" not in s:
        return "CC BY" + (" " + m.group(1) if m.group(1) else "")
    return None


# ------------------------------------------------------------------------------------------------------ people rules

def strip_html(s: str) -> str:
    s = re.sub(r"<[^>]+>", " ", s or "")
    return re.sub(r"\s+", " ", html.unescape(s)).strip()


def _words(*parts: str) -> str:
    return " " + re.sub(r"[^a-z0-9]+", " ", " ".join(p or "" for p in parts).lower()) + " "


MINOR = ["child", "children", "kid", "kids", "boy", "boys", "baby", "babies", "toddler", "infant", "teen", "teens",
         "teenager", "teenagers", "teenage", "adolescent", "school", "schoolgirl", "schoolboy", "pupil", "pupils",
         "daughter", "son", "sons", "little", "youth", "junior", "juniors", "minor", "minors", "kindergarten",
         "preschool", "family", "families", "niece", "nephew", "girls", "playground", "students", "student",
         "under 18", "u18", "u16", "u14", "scout", "scouts", "cheerleader", "cheerleaders", "prom", "graduation",
         "newborn", "childhood", "young girl", "young boy", "brother", "sister", "siblings"]
# "girl" alone is common for adult models in stock captions; it is only flagged
AGE_FLAG = ["girl", "young", "teen"]

SENSITIVE = ["nude", "nudity", "naked", "topless", "bottomless", "lingerie", "underwear", "bra", "panties", "bikini",
             "swimsuit", "swimwear", "bathing suit", "erotic", "erotica", "sexy", "sensual", "seductive", "boudoir",
             "fetish", "bdsm", "latex", "pin up", "pinup", "glamour", "playboy", "porn", "stripper", "striptease",
             "burlesque", "implied", "censored", "breast", "breasts", "cleavage", "buttocks", "butt", "thong", "corset",
             "bondage", "sex", "sexual", "shower", "bathtub", "bath", "bed", "bedroom", "drunk", "alcohol", "smoking",
             "cigarette", "vape", "drugs", "injury", "injured", "blood", "wound", "hospital", "patient", "funeral", "grave", "cemetery", "dead", "death", "corpse", "war", "soldier", "army",
             "military", "police", "arrest", "protest", "riot", "demonstration", "prison", "weapon", "gun", "rifle",
             "knife", "sword", "fight", "fighting", "violence", "crying", "cry", "sad", "depressed", "depression",
             "homeless", "poverty", "beggar", "religious", "church", "mosque", "temple", "prayer", "praying",
             "halloween", "zombie", "horror", "costume", "cosplay", "body paint", "bodypaint", "pregnant", "pregnancy", "diet", "obese", "obesity", "plastic surgery",
             "medical", "covid", "sick", "illness", "accident", "crime", "victim", "slave",
             "refugee", "migrant", "pride parade", "nsfw", "adult only", "18+"]

# events, red carpets, politics, sport, entertainment: likely public figures
PUBLIC = ["red carpet", "premiere", "festival", "award", "awards", "gala", "oscars", "grammy", "cannes", "berlinale",
          "comic con", "comiccon", "convention", "expo", "politician", "president", "minister", "senator", "mayor",
          "parliament", "congress", "governor", "election", "campaign", "rally", "summit", "conference", "speech",
          "ceremony", "olympic", "olympics", "championship", "championships", "world cup", "league", "match",
          "tournament", "fifa", "uefa", "nba", "nfl", "wwe", "ufc", "athlete", "footballer", "player", "players",
          "coach", "actor", "actress", "singer", "rapper", "musician", "band", "concert", "celebrity",
          "celebrities", "model agency", "miss universe", "miss world", "pageant", "beauty contest", "fashion week",
          "runway", "catwalk", "press", "interview", "wikimania", "wikimedia", "wikipedia", "editathon",
          "meetup", "hackathon", "portrait of", "photographed by", "selfie with", "with fans", "autograph",
          "royal", "prince", "princess", "king", "queen", "duke", "duchess", "ambassador", "official portrait",
          "influencer", "youtuber", "tiktoker", "instagrammer", "blogger", "vlogger", "streamer", "dj", "tv host",
          "presenter", "anchor", "journalist", "author", "writer", "poet", "artist", "painter", "sculptor",
          "director", "producer", "ceo", "founder", "entrepreneur", "businessman named", "deputy", "chancellor",
          "vs", "fc", "football", "soccer", "basketball", "baseball", "volleyball", "handball", "wrestling", "boxing",
          "tennis", "golf", "cricket", "rugby", "hockey", "cycling", "marathon", "race", "racing", "games", "sport",
          "sports", "stadium", "arena", "fans", "supporters", "club", "fashion show", "parade", "carnival",
          "pride", "meeting with", "delegation", "event", "events", "congress", "forum",
          "biennale", "salon", "salão", "photocall", "signing"]

ART = ["painting", "paintings", "drawing", "drawings", "sketch", "sculpture", "sculptures", "statue", "statues",
       "illustration", "illustrations", "engraving", "etching", "lithograph", "woodcut", "poster",
       "postcard", "cartoon", "comic", "anime", "manga", "render", "rendering", "3d model", "cgi", "ai generated",
       "ai-generated", "midjourney", "stable diffusion", "dall e", "dalle", "generated", "mannequin", "mannequins",
       "doll", "dolls", "figurine", "toy", "action figure", "fresco", "mural", "graffiti", "relief", "bust",
       "oil on canvas", "canvas", "watercolor", "watercolour", "museum", "gallery", "exhibition", "artwork",
       "vintage", "antique", "historical", "history", "19th century", "18th century", "20th century", "1800s",
       "1900s", "1910s", "1920s", "1930s", "1940s", "1950s", "1960s", "1970s", "victorian", "carte de visite",
       "daguerreotype", "ambrotype", "tintype", "archive", "archives", "library of congress", "screenshot",
       "diagram", "chart", "infographic", "clipart", "clip art", "vector", "svg", "logo", "icon", "silhouette",
       "x ray", "xray", "anatomy", "skeleton", "stock footage"]

WATERMARK = ["watermark", "watermarked", "logo", "copyright notice", "text overlay", "caption overlay"]


def has_any(text: str, words: list[str]) -> list[str]:
    """Words (or phrases) present as whole words, plural forms included ("bikinis", "dresses")."""
    return [w for w in words if f" {w} " in text or f" {w}s " in text or f" {w}es " in text]


_CAP_GENERIC = set("""Photographs Photos Photo Images Pictures People Persons Men Man Women Woman Portrait Portraits
Standing Sitting Walking Leaning Lying Jumping Dancing Smiling Laughing Posing Poses Pose Models Model Fashion Street
Streets City Park Parks Beach Beaches Studio Studios Office Offices Business Lifestyle Couples Couple Friends Group
Groups Selfies Selfie Unsplash Pexels Pixabay Flickr Stock Free CC0 CC BY SA License Licensed Public Domain Quality
Valued Featured Images Uploaded Media Needing Categories Files From By With In On At Of The And For To Female Male Adults
Adult Young Old Black White Red Blue Green Yellow Pink Purple Orange Brown Gray Grey Dresses Dress Shirts Jeans Hats
Sunglasses Glasses Hair Hands Arms Legs Feet Faces Smiles Clothing Shoes Boots Jackets Coats Suits Skirts Trousers
Summer Winter Spring Autumn Fall Day Night Morning Evening Sunset Sunrise Outdoor Outdoors Indoor Indoors Nature Urban
Bench Benches Stairs Steps Walls Wall Doors Door Windows Window Chairs Chair Sofas Sofa Tables Table Floor Grass Sand
Sea Ocean Lake River Mountains Mountain Forest Trees Tree Flowers Flower Field Fields Road Roads Bridge Bridges
Buildings Building Architecture Interior Exterior Cafe Cafes Coffee Restaurant Bar Shop Shops Market Railings Railing
Horizontal Vertical Close Ups Up Full Length Body Half Length Head Shoulders Profile Back Views View Front Side
Rear Cropped Taken Shot Shots Camera Cameras Phone Phones Mobile Smartphones Smartphone Using Holding Playing Reading
Looking Wearing Two Three Four Five Several Many Unidentified Unknown Anonymous Generic Description Germany France
Italy Spain Poland Russia Ukraine India China Japan Brazil Mexico Canada Australia Africa America American European
Asian African Indian Chinese Japanese New York London Paris Berlin Tokyo Moscow Kyiv Rome Madrid Barcelona Amsterdam
Vienna Prague Warsaw Istanbul Delhi Mumbai Bangkok Seoul Sydney Toronto Los Angeles San Francisco Chicago United
States Kingdom Republic Province State County District Region Northern Southern Eastern Western North South East
West Central Upper Lower Saint St Mount Lake Wikimedia Commons Wiki Loves Art Earth Monuments Scanned Taken""".split())


def looks_like_person_category(cat: str) -> bool:
    """A category such as "Jane Doe" or "Jane Doe in 2019": two or three capitalised words that are not generic."""
    c = re.sub(r"\s+in \d{4}.*$|\s+\(\d{4}\)$|\s+\d{4}$", "", cat.strip())
    words = c.split()
    if not (2 <= len(words) <= 3):
        return False
    if not all(re.match(r"^[A-Z][a-z'\-]+\.?$", w) for w in words):
        return False
    return not any(w.strip(".") in _CAP_GENERIC for w in words)


def people_check(title: str, description: str, categories: list[str], extra: str = "") -> dict:
    """Returns {"reject": [reasons], "flags": [flags]} for a candidate's text."""
    cats = " | ".join(categories or [])
    text = _words(title, description, cats, extra)
    reject, flags = [], []
    if m := has_any(text, MINOR):
        reject.append("minor? " + ",".join(m[:3]))
    if m := has_any(text, SENSITIVE):
        reject.append("sensitive: " + ",".join(m[:3]))
    if m := re.findall(r" (sex|nude|naked|erotic|lingerie|boudoir|porn|fetish|topless|sensual|bikini|swimsuit|"
                       r"swimwear|underwear|urinat|piss|toilet|bathing|nightwear|pajama|pyjama|negligee)[a-z]*", text):
        reject.append("sensitive (word root): " + ",".join(sorted(set(m))[:3]))
    if m := has_any(text, PUBLIC):
        reject.append("public/event: " + ",".join(m[:3]))
    if m := has_any(text, ART):
        reject.append("not a photo: " + ",".join(m[:3]))
    if m := has_any(text, WATERMARK):
        flags.append("watermark? " + ",".join(m[:2]))
    if m := has_any(text, AGE_FLAG):
        flags.append("check age: " + ",".join(m[:2]))
    if re.search(r"[\u0400-\u04FF\u0590-\u06FF\u0E00-\u0E7F\u1100-\u11FF\u3040-\u30FF\u3130-\u318F\u4E00-\u9FFF\uAC00-\uD7AF]",
                 title or ""):
        reject.append("title in a script the rules cannot check")
    named = [c for c in (categories or []) if looks_like_person_category(c)]
    if named:
        reject.append("named person category: " + "; ".join(named[:2]))
    return {"reject": reject, "flags": flags}
