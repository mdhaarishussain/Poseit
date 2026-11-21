"""
posematch.py: a Python port of the app's pose matcher (pose-core com.poseit.app.pose.match: PoseMatcher.kt,
FigureEvaluation.kt, MatchGeometry.kt, Assignment.kt) and of SceneMapping.templateToScene, for ranking photos.

Faithful to the Kotlin, constant for constant:
  - segments TORSO (mid-hip -> mid-shoulder, w 1.6), HEAD (mid-shoulder -> nose, 0.6), SHOULDER_LINE (0.6),
    HIP_LINE (0.5), upper arms 1.2, forearms 1.0, thighs 1.2, shins 1.0; limb angle atan2(dx, dy) in isotropic scene
    units (x * aspect), 0 = straight down;
  - a template segment is required when its joints are present, visibility >= 0.5 (missing visibility = 1) and inside
    the scene (margin 0); lines shorter than 25 % and limbs shorter than 12 % of their normal length are skipped;
  - a detected joint counts when visibility >= 0.5 and inside the scene with a 0.02 margin; midpoints use only joints
    both poses show;
  - segment score 1 within 12 deg, 0 at 55 deg, Hermite smoothstep between; HEAD also loses score when the
    nose-to-shoulders length differs (0.10 .. 0.35 body units);
  - shape = weighted mean; worstFactor = 1 - 0.5 * max(min(w / 1.6, 1) * (1 - s)); coverage and
    coverageFactor = clamp((coverage - 0.4) / 0.5, 0, 1) ^ 0.7; criticalFactor 0.7 without the nose, x 0.8 without an
    ankle (when the template needs them); framing = offsetScore (0.06 .. 0.5 template body heights) x sizeScore
    (|ln ratio| ln 1.06 .. ln 1.6, median length ratio of non-line, non-head segments);
  - figure score = shape * worstFactor * coverageFactor * criticalFactor * (0.75 + 0.25 * framing); overall = mean
    over template figures, 0 for a figure without a person; people are assigned to figures by exhaustive search over
    cost (1 - poseQuality) + 0.5 * min(centreDistance / refHeight, 4), unassigned 10 (greedy above 6 x 6).
Hints, facing and coaching text are not ported (they do not change the score).

Skeletons here are dicts {"points": {JOINT: (x, y)}, "vis": {JOINT: v}, "unit": u} in SCENE space (0..1, y down).
"""
from __future__ import annotations

import math
from itertools import permutations

MIN_VIS = 0.5
PERFECT_DEG = 12.0
ZERO_DEG = 55.0
SCENE_MARGIN = 0.02
MIN_LINE_FRACTION = 0.25
MIN_LIMB_FRACTION = 0.12
WORST_SEGMENT_PENALTY = 0.5
MISSING_HEAD_FACTOR = 0.7
MISSING_FEET_FACTOR = 0.8
HEAD_LENGTH_OK, HEAD_LENGTH_ZERO = 0.10, 0.35
OFFSET_OK, OFFSET_ZERO = 0.06, 0.5
SIZE_LOG_OK, SIZE_LOG_ZERO = math.log(1.06), math.log(1.6)
MAX_ASSIGN_DISTANCE = 4.0
UNASSIGNED_COST = 10.0

# BodyProportions
TORSO_L, NECK, HEAD_RADIUS, SHOULDER_HALF, HIP_HALF = 1.0, 0.14, 0.23, 0.38, 0.19
UPPER_ARM, FOREARM, THIGH, SHIN = 0.60, 0.52, 0.88, 0.84
STANDING_HEIGHT = THIGH + SHIN + 0.06 + TORSO_L + NECK + 2 * HEAD_RADIUS + 0.04

# name, from joints, to joints, weight, normal length, is line, is size reference, body part
SEGMENTS = [
    ("TORSO", ("LEFT_HIP", "RIGHT_HIP"), ("LEFT_SHOULDER", "RIGHT_SHOULDER"), 1.6, TORSO_L, False, True, "TORSO"),
    ("HEAD", ("LEFT_SHOULDER", "RIGHT_SHOULDER"), ("NOSE",), 0.6, NECK + HEAD_RADIUS * 0.82, False, False, "HEAD"),
    ("SHOULDER_LINE", ("RIGHT_SHOULDER",), ("LEFT_SHOULDER",), 0.6, 2 * SHOULDER_HALF, True, False, "TORSO"),
    ("HIP_LINE", ("RIGHT_HIP",), ("LEFT_HIP",), 0.5, 2 * HIP_HALF, True, False, "TORSO"),
    ("LEFT_UPPER_ARM", ("LEFT_SHOULDER",), ("LEFT_ELBOW",), 1.2, UPPER_ARM, False, True, "LEFT_ARM"),
    ("LEFT_FOREARM", ("LEFT_ELBOW",), ("LEFT_WRIST",), 1.0, FOREARM, False, True, "LEFT_ARM"),
    ("RIGHT_UPPER_ARM", ("RIGHT_SHOULDER",), ("RIGHT_ELBOW",), 1.2, UPPER_ARM, False, True, "RIGHT_ARM"),
    ("RIGHT_FOREARM", ("RIGHT_ELBOW",), ("RIGHT_WRIST",), 1.0, FOREARM, False, True, "RIGHT_ARM"),
    ("LEFT_THIGH", ("LEFT_HIP",), ("LEFT_KNEE",), 1.2, THIGH, False, True, "LEFT_LEG"),
    ("LEFT_SHIN", ("LEFT_KNEE",), ("LEFT_ANKLE",), 1.0, SHIN, False, True, "LEFT_LEG"),
    ("RIGHT_THIGH", ("RIGHT_HIP",), ("RIGHT_KNEE",), 1.2, THIGH, False, True, "RIGHT_LEG"),
    ("RIGHT_SHIN", ("RIGHT_KNEE",), ("RIGHT_ANKLE",), 1.0, SHIN, False, True, "RIGHT_LEG"),
]
TORSO_W = 1.6
TORSO_JOINTS = ("LEFT_SHOULDER", "RIGHT_SHOULDER", "LEFT_HIP", "RIGHT_HIP")
ALL_JOINTS = ("NOSE", "LEFT_EYE", "RIGHT_EYE", "LEFT_EAR", "RIGHT_EAR", "LEFT_SHOULDER", "RIGHT_SHOULDER",
              "LEFT_ELBOW", "RIGHT_ELBOW", "LEFT_WRIST", "RIGHT_WRIST", "LEFT_HAND", "RIGHT_HAND", "LEFT_HIP",
              "RIGHT_HIP", "LEFT_KNEE", "RIGHT_KNEE", "LEFT_ANKLE", "RIGHT_ANKLE", "LEFT_FOOT", "RIGHT_FOOT")


def smoothstep(e0, e1, x):
    if not e1 > e0:
        return 1.0 if x >= e0 else 0.0
    t = min(max((x - e0) / (e1 - e0), 0.0), 1.0)
    return t * t * (3 - 2 * t)


def segment_score(err, perfect=PERFECT_DEG, zero=ZERO_DEG):
    if not math.isfinite(err):
        return 0.0
    return 1.0 - smoothstep(perfect, zero, err)


def limb_angle(a, b):
    return math.degrees(math.atan2(b[0] - a[0], b[1] - a[1]))


def angle_diff(a, b):
    d = math.fmod(a - b, 360.0)
    if d > 180:
        d -= 360
    if d <= -180:
        d += 360
    return d


def mean(pts):
    if not pts:
        return None
    return (sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts))


def dist(a, b):
    return math.hypot(a[0] - b[0], a[1] - b[1])


def median(v):
    if not v:
        return None
    s = sorted(v)
    m = len(s) // 2
    return s[m] if len(s) % 2 else (s[m - 1] + s[m]) / 2


def in_scene(p, margin):
    return -margin <= p[0] <= 1 + margin and -margin <= p[1] <= 1 + margin


def is_visible(s, j, min_vis=MIN_VIS):
    return j in s["points"] and s["vis"].get(j, 1.0) >= min_vis


def estimated_unit(s, aspect):
    """Skeleton.estimatedUnit: torso length in scene heights (s["unit"] when > 0)."""
    if s.get("unit", 0) > 0:
        return s["unit"]
    iso = {j: (p[0] * aspect, p[1]) for j, p in s["points"].items()}

    def mid(a, b):
        return ((iso[a][0] + iso[b][0]) / 2, (iso[a][1] + iso[b][1]) / 2) if a in iso and b in iso else None
    c = []
    ms, mh = mid("LEFT_SHOULDER", "RIGHT_SHOULDER"), mid("LEFT_HIP", "RIGHT_HIP")
    if ms and mh:
        c.append(dist(ms, mh))
    if "LEFT_SHOULDER" in iso and "RIGHT_SHOULDER" in iso:
        c.append(dist(iso["LEFT_SHOULDER"], iso["RIGHT_SHOULDER"]) / 0.76)
    if "LEFT_EAR" in iso and "RIGHT_EAR" in iso:
        c.append(dist(iso["LEFT_EAR"], iso["RIGHT_EAR"]) / 0.42)
    for sh, el in (("LEFT_SHOULDER", "LEFT_ELBOW"), ("RIGHT_SHOULDER", "RIGHT_ELBOW")):
        if sh in iso and el in iso:
            c.append(dist(iso[sh], iso[el]) / UPPER_ARM)
    m = max(c) if c else 0
    return m if m > 1e-4 else 0.15


def rough_centre(s, aspect, accept):
    def iso_of(js):
        return [(s["points"][j][0] * aspect, s["points"][j][1]) for j in js if j in s["points"] and accept(j)]
    return mean(iso_of(TORSO_JOINTS)) or mean(iso_of(s["points"].keys())) or \
        mean([(p[0] * aspect, p[1]) for p in s["points"].values()]) or (0.5 * aspect, 0.5)


class TemplateInfo:
    def __init__(self, index, s, aspect):
        self.index, self.s, self.aspect = index, s, aspect
        u = estimated_unit(s, aspect)
        self.unit = u if (math.isfinite(u) and u > 1e-4) else 0.15
        self.ref_height = min(max(self.unit * STANDING_HEIGHT, 0.15), 1.0)
        self.required = {}
        for name, fj, tj, w, nl, line, sizeref, part in SEGMENTS:
            fr = [j for j in fj if self.usable(j)]
            to = [j for j in tj if self.usable(j)]
            a, b = mean([self.iso(j) for j in fr]), mean([self.iso(j) for j in to])
            if a and b:
                frac = MIN_LINE_FRACTION if line else MIN_LIMB_FRACTION
                if dist(a, b) >= frac * nl * self.unit:
                    self.required[name] = (fr, to, limb_angle(a, b))
        self.required_weight = sum(w for (n, _, _, w, *_r) in SEGMENTS if n in self.required)
        self.centre = rough_centre(s, aspect, lambda j: True)

    def iso(self, j):
        p = self.s["points"].get(j)
        return (p[0] * self.aspect, p[1]) if p is not None else None

    def usable(self, j):
        p = self.s["points"].get(j)
        return p is not None and is_visible(self.s, j) and in_scene(p, 0.0)


class DetectionInfo:
    def __init__(self, index, s, aspect):
        self.index, self.s, self.aspect = index, s, aspect
        self.has_usable = any(self.usable(j) for j in ALL_JOINTS)
        self.centre = rough_centre(s, aspect, self.visible)

    def iso(self, j):
        p = self.s["points"].get(j)
        return (p[0] * self.aspect, p[1]) if p is not None else None

    def visible(self, j):
        return is_visible(self.s, j)

    def usable(self, j):
        p = self.s["points"].get(j)
        return p is not None and self.visible(j) and in_scene(p, SCENE_MARGIN)


SEG_BY_NAME = {s[0]: s for s in SEGMENTS}


class PairEval:
    def __init__(self, t: TemplateInfo, d: DetectionInfo):
        geo, missing = {}, set()
        for name, (fr_t, to_t, _ang) in t.required.items():
            fr = [j for j in fr_t if d.usable(j)]
            to = [j for j in to_t if d.usable(j)]
            if not fr:
                missing.update(fr_t)
            if not to:
                missing.update(to_t)
            if not fr or not to:
                continue
            geo[name] = (mean([t.iso(j) for j in fr]), mean([t.iso(j) for j in to]),
                         mean([d.iso(j) for j in fr]), mean([d.iso(j) for j in to]))
        self.missing = missing

        def ratios(filt):
            out = []
            for name, (tf, tt, af, at) in geo.items():
                if not filt(name):
                    continue
                tl = dist(tf, tt)
                if tl > 1e-5:
                    out.append(dist(af, at) / tl)
            return out
        size_ratios = ratios(lambda n: SEG_BY_NAME[n][6]) or ratios(lambda n: True)
        size_known = bool(size_ratios)
        self.size_ratio = min(max(median(size_ratios) or 1.0, 0.05), 20.0)
        det_unit = t.unit * self.size_ratio

        visible_w = weighted = worst = 0.0
        part_w, part_s = {}, {}
        self.segments = {}
        for name, fj, tj, w, nl, line, sizeref, part in SEGMENTS:
            if name not in t.required or name not in geo:
                continue
            tf, tt, af, at = geo[name]
            ta, aa = limb_angle(tf, tt), limb_angle(af, at)
            err = angle_diff(aa, ta)
            sc = segment_score(abs(err))
            if name == "HEAD":
                len_t = dist(tf, tt) / t.unit
                len_a = dist(af, at) / det_unit
                sc = min(sc, 1 - smoothstep(HEAD_LENGTH_OK, HEAD_LENGTH_ZERO, abs(len_a - len_t)))
            self.segments[name] = (round(err, 1), round(sc, 3))
            worst = max(worst, min(w / TORSO_W, 1.0) * (1 - sc))
            visible_w += w
            weighted += w * sc
            part_w[part] = part_w.get(part, 0) + w
            part_s[part] = part_s.get(part, 0) + w * sc
        self.shape = weighted / visible_w if visible_w > 0 else 0.0
        self.worst_factor = 1 - WORST_SEGMENT_PENALTY * worst
        self.coverage = min(max(visible_w / t.required_weight, 0), 1) if t.required_weight > 0 else 0.0
        self.coverage_factor = min(max((self.coverage - 0.4) / 0.5, 0.0), 1.0) ** 0.7
        self.critical = (MISSING_HEAD_FACTOR if "NOSE" in missing else 1.0) * \
            (MISSING_FEET_FACTOR if ("LEFT_ANKLE" in missing or "RIGHT_ANKLE" in missing) else 1.0)
        self.part_scores = {p: part_s[p] / part_w[p] for p in part_w}
        self.pose_quality = self.shape * self.worst_factor * self.coverage_factor
        self.framing = self._framing(t, d, size_known)
        fs = self.framing if self.framing is not None else 1.0
        self.score = min(max(self.pose_quality * self.critical * (0.75 + 0.25 * fs), 0.0), 1.0)

    def _framing(self, t, d, size_known):
        common = {j for j in ALL_JOINTS if t.usable(j) and d.usable(j)}
        if all(j in common for j in TORSO_JOINTS):
            anchor = TORSO_JOINTS
        elif "LEFT_SHOULDER" in common and "RIGHT_SHOULDER" in common:
            anchor = ("LEFT_SHOULDER", "RIGHT_SHOULDER")
        elif "LEFT_HIP" in common and "RIGHT_HIP" in common:
            anchor = ("LEFT_HIP", "RIGHT_HIP")
        else:
            anchor = tuple(j for j in ALL_JOINTS if j in common)
        ta = mean([t.iso(j) for j in anchor])
        da = mean([d.iso(j) for j in anchor])
        if ta is None or da is None:
            return None
        nx, ny = (da[0] - ta[0]) / t.ref_height, (da[1] - ta[1]) / t.ref_height
        off = 1 - smoothstep(OFFSET_OK, OFFSET_ZERO, math.hypot(nx, ny))
        size = 1 - smoothstep(SIZE_LOG_OK, SIZE_LOG_ZERO, abs(math.log(self.size_ratio))) if size_known else 1.0
        return off * size


def assign(cost, n_det):
    n = len(cost)
    if n == 0:
        return []
    if n_det <= 0:
        return [-1] * n
    if n <= 6 and n_det <= 6:
        best, best_cost = [-1] * n, float("inf")
        cur, used = [-1] * n, [False] * n_det

        def search(i, acc):
            nonlocal best, best_cost
            if acc >= best_cost:
                return
            if i == n:
                best_cost, best = acc, cur.copy()
                return
            for j in range(n_det):
                if used[j]:
                    continue
                used[j] = True
                cur[i] = j
                c = cost[i][j]
                search(i + 1, acc + (min(max(c, 0), UNASSIGNED_COST - 1) if math.isfinite(c) else UNASSIGNED_COST - 1))
                used[j] = False
            cur[i] = -1
            search(i + 1, acc + UNASSIGNED_COST)
        search(0, 0.0)
        return best
    res, used = [-1] * n, [False] * n_det
    for c, i, j in sorted((cost[i][j], i, j) for i in range(n) for j in range(n_det)):
        if res[i] == -1 and not used[j]:
            res[i], used[j] = j, True
    return res


def match(templates, detected, aspect):
    """PoseMatcher.match score: returns (score, figures) with figures = [(PairEval or None, detIndex or None)]."""
    aspect = aspect if (math.isfinite(aspect) and 0.05 < aspect < 20) else 0.75
    tis = [TemplateInfo(i, s, aspect) for i, s in enumerate(templates)]
    people = [di for di in (DetectionInfo(i, s, aspect) for i, s in enumerate(detected)) if di.has_usable]
    if not tis or not people:
        return 0.0, [(None, None) for _ in tis]
    evals = [[PairEval(t, d) for d in people] for t in tis]
    cost = [[(1 - evals[i][j].pose_quality) + 0.5 * min(dist(t.centre, people[j].centre) / t.ref_height,
                                                         MAX_ASSIGN_DISTANCE)
             for j in range(len(people))] for i, t in enumerate(tis)]
    a = assign(cost, len(people))
    figs = [(evals[i][j], people[j].index) if j >= 0 else (None, None) for i, j in enumerate(a)]
    score = sum(e.score if e else 0.0 for e, _ in figs) / len(figs)
    return min(max(score, 0.0), 1.0), figs


# ---------------------------------------------------------------------------------------------------- scene mapping

def opposite(j):
    return "RIGHT_" + j[5:] if j.startswith("LEFT_") else ("LEFT_" + j[6:] if j.startswith("RIGHT_") else j)


def mirrored(fig):
    """Skeleton.mirrored: x -> 1 - x and left/right labels swapped (the app's flip toggle)."""
    return {"points": {opposite(j): (1 - p[0], p[1]) for j, p in fig["points"].items()},
            "vis": {opposite(j): v for j, v in fig["vis"].items()}, "unit": fig["unit"]}


def fit_frame(frame_aspect, scene_aspect):
    if frame_aspect > scene_aspect:
        h = scene_aspect / frame_aspect
        top = (1 - h) / 2
        return (0.0, top, 1.0, top + h)
    w = frame_aspect / scene_aspect
    left = (1 - w) / 2
    return (left, 0.0, left + w, 1.0)


def template_to_scene(figures, frame_aspect, scene_aspect, scale=1.0, offset=(0.0, 0.0), mirror=False):
    """SceneMapping.templateToScene with OverlayTransform(offsetX, offsetY, scale, rotation = 0)."""
    l, t, r, b = fit_frame(frame_aspect, scene_aspect)
    cx, cy = (l + r) / 2, (t + b) / 2
    fw, fh = r - l, b - t
    out = []
    for fig in figures:
        f = mirrored(fig) if mirror else fig
        pts = {}
        for j, (x, y) in f["points"].items():
            fx, fy = l + x * fw, t + y * fh
            dx, dy = (fx - cx) * scene_aspect * scale, (fy - cy) * scale
            pts[j] = (cx + dx / scene_aspect + offset[0], cy + dy + offset[1])
        out.append({"points": pts, "vis": dict(f["vis"]), "unit": f["unit"] * fh * scale})
    return out
