#!/usr/bin/env python3
"""
commons_harvest.py: candidate photos of people demonstrating each pose, from Wikimedia Commons.

    python3 tools/pose-photos/commons_harvest.py search   [--poses id,id] [--limit 50]
    python3 tools/pose-photos/commons_harvest.py download [--per-query 10] [--max 2000]
    python3 tools/pose-photos/commons_harvest.py probe "search words"

search    for every pose its phrases in commons_queries.Q (--per-pose, --pexels), then the broad per-category phrases
          in commons_queries.BROAD (with "pexels": Commons holds CC0 stock photos imported from Pexels), each plus the
          global exclusions below, in the File: namespace via
          the MediaWiki API (generator=search, prop=imageinfo with extmetadata). Writes WORK/commons/queries/<n>.json
          (raw results) and WORK/commons/files.json: one record per file with title, page URL, thumbnail URL, size,
          licence (LicenseShortName, LicenseUrl, Artist, Credit, AttributionRequired, Restrictions, UsageTerms),
          description, categories, the poses and phrases that found it, and the rules verdict (licence allowed?
          people rules: rejects and flags).
download  fetches a rendition at a standard thumbnail width (1280, else 960, else 500 px wide; Wikimedia throttles other
          widths and originals) of the accepted files (licence allowed, no rejects, a photo at least 640 px on the
          short side), scaled to 1200 px on the long side, best search rank first, at most --per-query per phrase,
          into WORK/commons/img/<pageid>.jpg.
probe     prints one search (for tuning phrases).

Rate limits: Wikimedia allows about 10 requests a minute to clients whose User-Agent carries no contact details, so
requests are serial with an ~8 s gap and Retry-After is honoured; a full run is slow by design. Re-runs resume: queries
already saved and images already on disk are skipped.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from common import WORK, Polite, load_poses, read_json, write_json  # noqa: E402
from commons_queries import BROAD, Q  # noqa: E402
import rules  # noqa: E402

API = "https://commons.wikimedia.org/w/api.php"
OUT = WORK / "commons"
EXCLUDE = " -painting -drawing filetype:bitmap"  # more negations (e.g. -sculpture -illustration) derail CirrusSearch
META = ("LicenseShortName|LicenseUrl|Artist|Credit|AttributionRequired|Restrictions|ImageDescription|UsageTerms|"
        "Copyrighted|License|Categories|ObjectName|DateTimeOriginal")
THUMB_W = 1200


def http() -> Polite:
    return Polite(gaps={"commons.wikimedia.org": 8.0, "upload.wikimedia.org": 3.0, "thumb.wikimedia.org": 2.0})


def search(client: Polite, phrase: str, limit: int = 50) -> list[dict]:
    d = client.json(API, {
        "action": "query", "format": "json", "formatversion": "2",
        "generator": "search", "gsrsearch": phrase + EXCLUDE, "gsrnamespace": "6", "gsrlimit": str(limit),
        "prop": "imageinfo", "iiprop": "url|size|mime|extmetadata", "iiurlwidth": str(THUMB_W),
        "iiextmetadatafilter": META, "iiextmetadatalanguage": "en",
    })
    pages = d.get("query", {}).get("pages", [])
    return sorted(pages, key=lambda p: p.get("index", 0))


def meta_value(m: dict, k: str) -> str:
    v = m.get(k, {}).get("value", "")
    return v if isinstance(v, str) else str(v)


def record(page: dict) -> dict | None:
    ii = (page.get("imageinfo") or [None])[0]
    if not ii:
        return None
    m = ii.get("extmetadata", {})
    short = rules.strip_html(meta_value(m, "LicenseShortName"))
    url = meta_value(m, "LicenseUrl")
    cats = [c.strip() for c in rules.strip_html(meta_value(m, "Categories")).split("|") if c.strip()]
    desc = rules.strip_html(meta_value(m, "ImageDescription"))
    title = page["title"]
    lic = rules.commons_licence(short, url)
    restr = meta_value(m, "Restrictions")
    verdict = rules.people_check(title, desc[:2000], cats)
    w, h = ii.get("width", 0), ii.get("height", 0)
    if not lic:
        verdict["reject"].insert(0, f"licence not allowed: {short or '?'}")
    if restr:
        # personality = identifiable person (use must not imply endorsement); trademarked / insignia = third-party marks
        if any(r in restr for r in ("trademarked", "insignia", "coa", "ita-mibac")):
            verdict["reject"].append("restrictions: " + restr)
        else:
            verdict["flags"].append("restrictions: " + restr)
    if ii.get("mime") not in ("image/jpeg", "image/png", "image/webp"):
        verdict["reject"].append("mime " + str(ii.get("mime")))
    if min(w, h) < 640:
        verdict["reject"].append(f"small {w}x{h}")
    return {
        "pageid": page["pageid"],
        "title": title,
        "page": ii.get("descriptionurl"),
        "file_url": ii.get("url"),
        "thumb_url": ii.get("thumburl"),
        "width": w, "height": h, "mime": ii.get("mime"),
        "licence": {
            "key": lic, "short_name": short, "url": url,
            "artist": rules.strip_html(meta_value(m, "Artist")),
            "credit": rules.strip_html(meta_value(m, "Credit")),
            "attribution_required": meta_value(m, "AttributionRequired"),
            "usage_terms": rules.strip_html(meta_value(m, "UsageTerms")),
            "restrictions": restr,
        },
        "description": desc[:600],
        "categories": cats[:40],
        "date": rules.strip_html(meta_value(m, "DateTimeOriginal"))[:40],
        "verdict": verdict,
        "ok": not verdict["reject"],
    }


def cmd_search(args):
    client = http()
    qdir = OUT / "queries"
    qdir.mkdir(parents=True, exist_ok=True)
    files = read_json(OUT / "files.json", {}) or {}
    # each pose's own phrases (as written; with --pexels also with "pexels" added), then the broad "pexels" phrases
    plan = []
    for pid in (args.poses.split(",") if args.poses else list(Q)):
        for q in Q.get(pid, [])[: args.per_pose]:
            plan.append((pid, q))
            if args.pexels:
                plan.append((pid, q + " pexels"))
    if not args.poses:
        plan += [("cat:" + c, q) for c, qs in BROAD.items() for q in qs]
    n_new = 0
    for i, (pid, phrase) in enumerate(plan):
        key = hashlib.sha1(phrase.encode()).hexdigest()[:12]
        qf = qdir / f"{key}.json"
        saved = read_json(qf)
        if saved is None and args.cached_only:
            continue
        if saved is None:
            try:
                pages = search(client, phrase, args.limit)
            except Exception as e:  # keep going: a later run resumes
                print(f"[{pid}] '{phrase}': {e}", file=sys.stderr, flush=True)
                continue
            saved = {"phrase": phrase, "pose": pid, "pages": pages}
            write_json(qf, saved)
            n_new += 1
        for rank, page in enumerate(saved["pages"]):
            rec = record(page)
            if not rec:
                continue
            key2 = str(rec["pageid"])
            old = files.get(key2)
            if old:
                rec["found"] = old["found"]
            else:
                rec["found"] = []
            hit = {"pose": pid, "phrase": phrase, "rank": rank}
            if hit not in rec["found"]:
                rec["found"].append(hit)
            files[key2] = rec
        ok = sum(1 for p in saved["pages"] if (r := files.get(str(p["pageid"]))) and r["ok"])
        print(f"[{i + 1}/{len(plan)} {pid}] '{phrase}': {len(saved['pages'])} files, {ok} pass the rules",
              flush=True)
        if i % 5 == 4:
            write_json(OUT / "files.json", files)
    write_json(OUT / "files.json", files)
    print(f"{len(files)} files, {sum(1 for r in files.values() if r['ok'])} pass; {n_new} new queries; "
          f"{client.requests} requests", flush=True)


STANDARD_WIDTHS = (1280, 960, 500)  # Wikimedia's thumbnail steps (mediawiki.org/wiki/Common_thumbnail_sizes)


def standard_thumb(rec: dict) -> str:
    """A thumbnail URL at a standard width: upload.wikimedia.org rejects or rate-limits other widths and originals
    ("use thumbnail images in sizes listed on https://w.wiki/GHai"). The API already rounds iiurlwidth up to a step
    (1280) when the file is wider; narrower files come back as the original, so ask for the next smaller step."""
    thumb = (rec.get("thumb_url") or "").split("?")[0]
    if "/thumb/" in thumb and re.search(r"/\d+px-[^/]+$", thumb):
        return thumb
    w = rec.get("width") or 0
    step = next((s for s in STANDARD_WIDTHS if w > s), STANDARD_WIDTHS[-1])
    url = (rec.get("file_url") or "").split("?")[0]
    m = re.match(r"^https://upload\.wikimedia\.org/wikipedia/commons/([0-9a-f]/[0-9a-f]{2}/)([^/]+)$", url)
    if not m:
        return thumb or url
    return f"https://thumb.wikimedia.org/wikipedia/commons/thumb/{m.group(1)}{m.group(2)}/{step}px-{m.group(2)}"


def cmd_download(args):
    from PIL import Image

    client = http()
    files = read_json(OUT / "files.json", {}) or {}
    img_dir = OUT / "img"
    img_dir.mkdir(parents=True, exist_ok=True)
    # best rank first; at most per_query per phrase
    per_phrase: dict[str, list] = {}
    for rec in files.values():
        if not rec["ok"] or not rec.get("thumb_url"):
            continue
        for hit in rec["found"]:
            per_phrase.setdefault(hit["phrase"], []).append((hit["rank"], rec))
    chosen: dict[str, dict] = {}
    for depth in range(args.per_query):
        for phrase, lst in per_phrase.items():
            lst.sort(key=lambda t: t[0])
            if depth < len(lst):
                rec = lst[depth][1]
                chosen.setdefault(str(rec["pageid"]), rec)
    todo = [r for r in chosen.values() if not (img_dir / f"{r['pageid']}.jpg").exists()][: args.max]
    print(f"{len(chosen)} chosen, {len(todo)} to download", flush=True)
    for i, rec in enumerate(todo):
        dest = img_dir / f"{rec['pageid']}.jpg"
        try:
            data = client.get(standard_thumb(rec), timeout=90)
            im = Image.open(io.BytesIO(data))
            im = im.convert("RGB")
            s = THUMB_W / max(im.size)
            if s < 1:
                im = im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS)
            im.save(dest, quality=90)
        except Exception as e:
            print(f"  {rec['title']}: {e}", file=sys.stderr, flush=True)
            continue
        if i % 25 == 0:
            print(f"  {i + 1}/{len(todo)} {rec['title'][:80]}", flush=True)
    have = sum(1 for r in chosen.values() if (img_dir / f"{r['pageid']}.jpg").exists())
    print(f"done: {have} images on disk; {client.requests} requests", flush=True)


def cmd_probe(args):
    client = http()
    for page in search(client, args.phrase, 20):
        rec = record(page)
        if rec:
            print(("OK  " if rec["ok"] else "--  ") + rec["title"][:90], "|", rec["licence"]["short_name"], "|",
                  "; ".join(rec["verdict"]["reject"] + rec["verdict"]["flags"])[:120])


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("search")
    s.add_argument("--poses")
    s.add_argument("--limit", type=int, default=50)
    s.add_argument("--per-pose", type=int, default=2, help="phrases per pose (commons_queries.Q)")
    s.add_argument("--pexels", action="store_true", help="also search every pose phrase with 'pexels' added")
    s.add_argument("--cached-only", action="store_true",
                   help="no requests: rebuild files.json (verdicts with the current rules) from saved queries")
    d = sub.add_parser("download")
    d.add_argument("--per-query", type=int, default=10)
    d.add_argument("--max", type=int, default=3000)
    p = sub.add_parser("probe")
    p.add_argument("phrase")
    args = ap.parse_args()
    {"search": cmd_search, "download": cmd_download, "probe": cmd_probe}[args.cmd](args)


if __name__ == "__main__":
    main()
