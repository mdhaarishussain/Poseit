"""Shared paths, pose library loading, landmark conversion and a polite HTTP client for the pose-photo tools."""
from __future__ import annotations

import json
import os
import random
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
POSES_JSON = ROOT / "promo" / "public" / "poses.json"
MODELS = ROOT / "app" / "src" / "main" / "assets" / "models"
if not (MODELS / "pose_landmarker_full.task").exists():
    # the models are fetched, not committed; a worktree reads them from the main checkout (or POSE_MODELS)
    MODELS = Path(os.environ.get("POSE_MODELS", "/home/user/Poseit/app/src/main/assets/models"))
WORK = Path(os.environ.get("POSE_PHOTOS_WORK", "/home/user/poseit-photos-work"))

# The app's 21 joints (pose-core Joint.kt) and their MediaPipe BlazePose indices.
JOINTS = [
    ("NOSE", 0), ("LEFT_EYE", 2), ("RIGHT_EYE", 5), ("LEFT_EAR", 7), ("RIGHT_EAR", 8),
    ("LEFT_SHOULDER", 11), ("RIGHT_SHOULDER", 12), ("LEFT_ELBOW", 13), ("RIGHT_ELBOW", 14),
    ("LEFT_WRIST", 15), ("RIGHT_WRIST", 16), ("LEFT_HAND", 19), ("RIGHT_HAND", 20),
    ("LEFT_HIP", 23), ("RIGHT_HIP", 24), ("LEFT_KNEE", 25), ("RIGHT_KNEE", 26),
    ("LEFT_ANKLE", 27), ("RIGHT_ANKLE", 28), ("LEFT_FOOT", 31), ("RIGHT_FOOT", 32),
]
JOINT_NAMES = [j for j, _ in JOINTS]


def opposite(j: str) -> str:
    if j.startswith("LEFT_"):
        return "RIGHT_" + j[5:]
    if j.startswith("RIGHT_"):
        return "LEFT_" + j[6:]
    return j


def load_poses() -> list[dict]:
    """The 99 built-in poses as exported by pose-core (frame space, y down, frameAspect = width / height)."""
    return json.loads(POSES_JSON.read_text())["poses"]


def mp_to_app(person33: list) -> dict:
    """One MediaPipe person ([[x, y, z, visibility, presence] x 33]) -> {"points": {joint: [x, y]}, "vis": {joint: v}}
    with visibility = min(visibility, presence), exactly as LandmarkMapping.toScene with sceneAspect = image aspect."""
    pts, vis = {}, {}
    for name, idx in JOINTS:
        if idx >= len(person33):
            continue
        l = person33[idx]
        x, y = float(l[0]), float(l[1])
        if not (x == x and y == y):
            continue
        v = l[3] if len(l) > 3 and l[3] is not None else 1.0
        p = l[4] if len(l) > 4 and l[4] is not None else 1.0
        pts[name] = [x, y]
        vis[name] = max(0.0, min(1.0, min(v, p)))
    return {"points": pts, "vis": vis}


# ------------------------------------------------------------------------------------------------------------- HTTP

class Polite:
    """Serial HTTP client: a minimum gap between requests to one host, Retry-After honoured on 429/503, a few retries.

    Wikimedia limits clients without contact details in the User-Agent to about 10 requests a minute; the default gap
    for commons.wikimedia.org is therefore about 8 s. The User-Agent names the tool only (no personal details)."""

    UA = "PoseitPhotoHarvest/0.1 (pose-guide app sample-photo search; python-urllib)"

    def __init__(self, gaps: dict[str, float] | None = None, default_gap: float = 1.5, ua: str | None = None,
                 log=None):
        self.gaps = gaps or {}
        self.default_gap = default_gap
        self.last: dict[str, float] = {}
        self.ua = ua or self.UA
        self.log = log or (lambda *a: print(*a, file=sys.stderr, flush=True))
        self.requests = 0

    def _wait(self, host: str):
        gap = self.gaps.get(host, self.default_gap)
        dt = time.time() - self.last.get(host, 0.0)
        if dt < gap:
            time.sleep(gap - dt + random.uniform(0, 0.3))

    def get(self, url: str, params: dict | None = None, tries: int = 6, timeout: float = 60.0,
            headers: dict | None = None) -> bytes:
        if params:
            url = url + ("&" if "?" in url else "?") + urllib.parse.urlencode(params)
        host = urllib.parse.urlparse(url).netloc
        last_err = None
        for attempt in range(tries):
            self._wait(host)
            self.last[host] = time.time()
            self.requests += 1
            req = urllib.request.Request(url, headers={"User-Agent": self.ua, **(headers or {})})
            try:
                with urllib.request.urlopen(req, timeout=timeout) as r:
                    return r.read()
            except urllib.error.HTTPError as e:
                last_err = e
                if e.code in (429, 503, 502, 504):
                    ra = e.headers.get("Retry-After")
                    try:
                        wait = float(ra) if ra else 10.0 * (attempt + 1)
                    except ValueError:
                        wait = 10.0 * (attempt + 1)
                    wait = min(max(wait, 2.0), 180.0) + random.uniform(0.5, 2.0)
                    self.log(f"  {e.code} from {host}, waiting {wait:.0f} s")
                    time.sleep(wait)
                    self.last[host] = time.time()
                    continue
                if e.code in (403, 404, 410):
                    raise
                time.sleep(3.0 * (attempt + 1))
            except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as e:
                last_err = e
                time.sleep(3.0 * (attempt + 1))
        raise RuntimeError(f"giving up on {url}: {last_err}")

    def json(self, url: str, params: dict | None = None, **kw):
        return json.loads(self.get(url, params, **kw).decode("utf-8"))

    def download(self, url: str, dest: Path, **kw) -> Path:
        data = self.get(url, **kw)
        dest.parent.mkdir(parents=True, exist_ok=True)
        tmp = dest.with_suffix(dest.suffix + ".part")
        tmp.write_bytes(data)
        tmp.replace(dest)
        return dest


def write_json(path: Path, obj, indent=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(obj, indent=indent, ensure_ascii=False))
    tmp.replace(path)


def read_json(path: Path, default=None):
    try:
        return json.loads(Path(path).read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        return default
