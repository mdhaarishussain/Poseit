/**
 * studio/camera.ts: the act II camera, one continuous shot from f80 to f335 (B2-B4). Pure.
 *
 *   HERO                     the hero slot (row 1, col 4) and its floor point
 *   dollyCam(f)              WIDE_CAM dollied forward in two pushes (f100-176, f176-272), 0.85 m (about 1.15 on
 *                            the hero), and trucking right from 0.45 m left of the hero to square on it (parallax)
 *   studioCamAt(f)           the shot:
 *     f80        a lens zoom about a fixed point so the hero's frame IS the hook skeleton's frame (hookScreenFrame)
 *     f81-100    a one-frame snap to 0.7 of that zoom, then a softLand tail to the wide framing (1.00)
 *     f100-271   the dolly (row parallax), lens at 1.00
 *     f272       a one-frame scale cut (x1.0926 = 1.08 -> 1.18) that re-frames the hero's feet to y 0.725,
 *                softLand tail to x1.111 (1.20) and y 0.72 by f284, then a slow drift to f304
 *     f304-335   the push into the hero: dolly forward 2.5 m (gentleIn) while the lens is solved every frame so the
 *                hero's frame follows a zoom about a fixed point, accelerating (gentleIn in log scale) into the
 *                guide's frame at the cut (HERO_AT_CUT)
 *   heroFrameAt(cam, pose)   the hero's frame on screen for a camera (placePose)
 *   focusLook(dHero, shallow) depth of field that follows the hero: sharp from 0.89x to 2.26x its distance (the
 *                            foundation's 5.5-14 m at the wide framing); shallowAt(f) racks it onto the hero's row
 *                            at the pick (f288) and deepens it through the push, so the army behind goes soft
 */
import { theme } from "../../theme";
import { illustrative, poseById, poseGround } from "../../figure/poses";
import type { Pose } from "../../figure/types";
import { WIDE_CAM, frameOn, placePose, project, slotPosition, zoomAbout, zoomed, type StudioCam } from "../../world/camera";
import type { DepthLook } from "../../world/StudioFigures";
import { keys, snapTail, tween } from "../../motion";
import { HERO_AT_CUT, hookScreenFrame, type Rect } from "../shots";
import { STUDIO_TIMING } from "../timing";

const E = theme.ease;

const [HX, HZ] = slotPosition(1, 4);
export const HERO = { row: 1, col: 4, x: HX, z: HZ };

const STIFF = illustrative("promo_stiff");
const DISCO = poseById("fun_disco_point");

export const dollyZ = (f: number) => keys(f, [[100, 0], [176, 0.4, E.drift], [272, 0.85, E.drift]]);
/** A slow truck right across the chorus (row parallax), arriving square on the hero for the unison. */
export const truckX = (f: number) => keys(f, [[100, -0.45], [272, 0, E.drift]]);
export const dollyCam = (f: number): StudioCam => ({ ...WIDE_CAM, x: truckX(f), z: dollyZ(f) });

export const heroFrameAt = (cam: StudioCam, pose: Pose): Rect | null => {
  const p = placePose(cam, pose, HX, HZ);
  if (!p) return null;
  return { ...p.frame, width: p.frame.height * pose.frameAspect };
};

/** Lens zoom (focal x k) then a lens shift that puts the hero's floor point on screen point s. */
const aim = (cam: StudioCam, k: number, s: [number, number]): StudioCam => frameOn(zoomed(cam, k), [HX, 0, HZ], s);

/** Screen point of the hero's floor point (its lowest ink, frame-centre x) for a camera. */
const heroGround = (cam: StudioCam): [number, number] => {
  const q = project(cam, [HX, 0, HZ])!;
  return [q.x, q.y];
};

// ---- B2: the zoom that matches the hook skeleton --------------------------------------------------------------
const B2 = (() => {
  const target = hookScreenFrame(80); // the skeleton's frame on the first studio frame (the cut keeps the push)
  const wide = heroFrameAt(dollyCam(80), STIFF)!;
  const k0 = target.height / wide.height;
  // fixed point P with P + (wide - P) * k0 = target, solved on the frame's top-left corner
  const px = (target.x - k0 * wide.x) / (1 - k0);
  const py = (target.y - k0 * wide.y) / (1 - k0);
  return { k0, p: [px, py] as [number, number] };
})();

const b2Zoom = (f: number) =>
  snapTail(f, STUDIO_TIMING.snapBack, { from: B2.k0, snap: 0.7 * B2.k0, to: 1, tail: STUDIO_TIMING.settled - STUDIO_TIMING.snapBack });

// ---- B4: the scale cut and the push --------------------------------------------------------------------------
const CUT = STUDIO_TIMING.unison;
const PUSH = STUDIO_TIMING.push;
const S271 = heroGround(dollyCam(CUT - 1));
const cutZoom = (f: number) => snapTail(f, CUT, { from: 1, snap: 1.18 / 1.08, to: 1.2 / 1.08, tail: 12 }) * tween(f, [284, PUSH[0]], [1, 1.008], E.drift);
const cutY = (f: number) => snapTail(f, CUT, { from: S271[1], snap: 0.725 * theme.size.H, to: 0.72 * theme.size.H, tail: 12 });
const holdCam = (f: number): StudioCam => aim(dollyCam(CUT), cutZoom(f), [S271[0], cutY(f)]);

const PUSH_DOLLY = 2.5; // metres forward during the push (parallax: the front row sweeps out first)
const pushStart = (() => {
  const cam = holdCam(PUSH[0]);
  const fr = heroFrameAt(cam, DISCO)!;
  return { cam, frame: fr };
})();
/** Ground point of a pose frame: frame-centre x, lowest ink y (placePose stands the lowest ink on the floor). */
const groundOf = (r: Rect, pose: Pose): [number, number] => [r.x + r.width / 2, r.y + poseGround(pose) * r.height];
const PUSH_PATH = (() => {
  const a = pushStart.frame, b = HERO_AT_CUT;
  const r = b.height / a.height;
  const ga = groundOf(a, DISCO), gb = groundOf(b, DISCO);
  const p: [number, number] = [(gb[0] - r * ga[0]) / (1 - r), (gb[1] - r * ga[1]) / (1 - r)];
  return { a, r, ga, p };
})();

const pushCam = (f: number): StudioCam => {
  const u = tween(f, PUSH, [0, 1], (t) => t);
  const e = E.gentleIn(u);
  const base: StudioCam = { ...pushStart.cam, z: pushStart.cam.z + PUSH_DOLLY * e, focal: pushStart.cam.focal };
  const scale = Math.pow(PUSH_PATH.r, e); // hero frame height relative to the push start
  const g: [number, number] = [
    PUSH_PATH.p[0] + (PUSH_PATH.ga[0] - PUSH_PATH.p[0]) * scale,
    PUSH_PATH.p[1] + (PUSH_PATH.ga[1] - PUSH_PATH.p[1]) * scale,
  ];
  // solve the focal: the hero frame height is proportional to the focal at a fixed depth
  const now = heroFrameAt(base, DISCO)!;
  const k = (PUSH_PATH.a.height * scale) / now.height;
  return aim(base, k, g);
};

export const studioCamAt = (f: number): StudioCam => {
  if (f < 100) return zoomAbout(dollyCam(f), b2Zoom(f), B2.p);
  if (f < CUT) return dollyCam(f);
  if (f < PUSH[0]) return holdCam(f);
  return pushCam(Math.min(f, PUSH[1]));
};

export const heroDepth = (cam: StudioCam) => project(cam, [HX, 0, HZ])?.depth ?? 6;

const smooth = (t: number) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));
/**
 * Depth of field on the hero. `shallow` 0 = the wide look (sharp 0.89x-2.26x the hero's distance, the foundation's
 * 5.5-14 m); 1 = focus racked onto the hero's row (everything behind it softens, up to 8 px); above 1 on the push.
 */
export const focusLook = (dh: number, shallow = 0): DepthLook => (d) => {
  const s = Math.max(0, shallow);
  const near = 0.89 * dh;
  const far = dh * (1.08 + 1.18 * Math.max(0, 1 - s));
  const span = dh * Math.max(0.45, 1.6 - 1.0 * s);
  const maxFar = 3 + 5 * Math.min(1.4, s);
  const nearSpan = dh * (0.4 - 0.15 * Math.min(1, s));
  const maxNear = 6 + 4 * Math.min(1, s);
  return {
    blur: d < near ? maxNear * smooth((near - d) / nearSpan) : d > far ? maxFar * smooth((d - far) / span) : 0,
    haze: d > 14 ? 0.1 * smooth((d - 14) / 10) : 0,
  };
};

/** The rack focus of the pick: f288-296 onto the hero's row, deepening through the push. */
export const shallowAt = (f: number) =>
  keys(f, [[STUDIO_TIMING.marks, 0], [STUDIO_TIMING.rackEnd, 1, E.softInOut], [PUSH[0], 1], [PUSH[1], 1.4, E.gentleIn]]);

/** Debug/print: the solved seam numbers. */
export const STUDIO_SEAMS = { b2Zoom0: B2.k0, b2Point: B2.p, pushRatio: PUSH_PATH.r };
