/**
 * camera.ts: the studio's perspective camera (acts II and IV). Pure math, no React.
 *
 * World: metres. Floor y = 0 (y up), x to the right, z away from the camera (depth). The seamless paper runs along
 * the floor to z = SWEEP.coveZ, curves up through a quarter-circle cove of radius SWEEP.coveR and becomes the wall
 * at z = coveZ + coveR. The camera has no yaw or roll, only a downward pitch, so floor lines of equal depth are
 * horizontal on screen and the paper can be shaded row by row.
 *
 *   StudioCam                 { x, z, height, pitch (deg down), focal (px at 1920 H), cx, cy (principal point px) }
 *   WIDE_CAM                  the storyboard's wide framing (section 9): row 1 feet at y 0.76 H and 0.24 H tall,
 *                             row 10 at y 0.35 H and 0.07 H tall (solved for LATTICE below)
 *   LATTICE                   { rows: 11, cols: 9, z0 (row 0), dz, dx }: slot (row, col) -> slotPosition()
 *   project(cam, [x, y, z])   -> { x, y, depth, scale } screen px; scale = px per metre at that depth; null behind
 *   floorAt(cam, sx, sy)      -> [x, z] floor point under a screen pixel, or null above the horizon
 *   floorEllipse(cam, x, z, rx, rz)  a floor ellipse projected: { cx, cy, rx, ry } (contact shadows)
 *   placePose(cam, pose, x, z, opts)  where to draw a pose frame so it stands on the floor at (x, z) as a
 *                             camera-facing card: { frame: {x, y, height} } for <FigureG>, depth, scale, and one
 *                             shadow ellipse per figure (null if behind the camera)
 *   zoomed(cam, k)            the same camera with focal * k (a lens zoom: no parallax)
 *   zoomAbout(cam, k, [sx, sy])  lens zoom about a fixed screen point (sharp; the studio's scale moves)
 *   frameOn(cam, [x, y, z], [sx, sy])  lens shift so a world point lands on a screen point
 *   BODY_UNIT_M               metres per body unit (torso length): 0.5, so a standing figure is about 1.71 m
 */
import { poseGround, poseUnit } from "../figure/poses";
import { buildShapes, shapeBounds } from "../figure/shapes";
import type { Pose } from "../figure/types";

export type StudioCam = {
  x: number;
  z: number;
  height: number;
  pitch: number;
  focal: number;
  cx: number;
  cy: number;
};

export const BODY_UNIT_M = 0.5;

export const SWEEP = { coveZ: 26, coveR: 3.2 };

/** Solved so the lattice matches STORYBOARD.md section 9 at the wide framing (20 deg down, 59 deg vertical FOV). */
export const WIDE_CAM: StudioCam = { x: 0, z: 0, height: 3.853, pitch: 20, focal: 1681.8, cx: 540, cy: 960 };

export const LATTICE = { rows: 11, cols: 9, z0: 5.2006 - 1.7817, dz: 1.7817, dx: 0.885 };

/** Floor position of slot (row 0 = front, col 0 = left). Column 4 is the centre column. */
export const slotPosition = (row: number, col: number): [number, number] => [
  (col - (LATTICE.cols - 1) / 2) * LATTICE.dx,
  LATTICE.z0 + row * LATTICE.dz,
];

const rad = (d: number) => (d * Math.PI) / 180;

export const project = (cam: StudioCam, p: [number, number, number]) => {
  const th = rad(cam.pitch);
  const dx = p[0] - cam.x, dy = p[1] - cam.height, dz = p[2] - cam.z;
  const zc = -dy * Math.sin(th) + dz * Math.cos(th);
  if (zc <= 1e-6) return null;
  const yc = dy * Math.cos(th) + dz * Math.sin(th);
  return { x: cam.cx + (cam.focal * dx) / zc, y: cam.cy - (cam.focal * yc) / zc, depth: zc, scale: cam.focal / zc };
};

export const floorAt = (cam: StudioCam, sx: number, sy: number): [number, number] | null => {
  const th = rad(cam.pitch);
  const u = (sx - cam.cx) / cam.focal, v = -(sy - cam.cy) / cam.focal;
  const dy = -Math.sin(th) + v * Math.cos(th);
  const dz = Math.cos(th) + v * Math.sin(th);
  if (dy >= -1e-9) return null;
  const t = -cam.height / dy;
  return [cam.x + t * u, cam.z + t * dz];
};

export const floorEllipse = (cam: StudioCam, x: number, z: number, rx: number, rz: number) => {
  const c = project(cam, [x, 0, z]);
  const n = project(cam, [x, 0, z - rz]);
  const f = project(cam, [x, 0, z + rz]);
  if (!c || !n || !f) return null;
  return { cx: c.x, cy: (n.y + f.y) / 2, rx: rx * c.scale, ry: Math.abs(n.y - f.y) / 2 };
};

export const zoomed = (cam: StudioCam, k: number): StudioCam => ({ ...cam, focal: cam.focal * k });

/**
 * Lens zoom by k about a screen point that stays put (the studio's "scale" moves: a 2.50 -> 1.75 snap is
 * zoomAbout(cam, 2.5, p) -> zoomAbout(cam, 1.75, p)). Renders natively (sharp paper, crisp figures), unlike a
 * CSS scale of the whole frame. No parallax: use cam.z / cam.x for a dolly.
 */
export const zoomAbout = (cam: StudioCam, k: number, [sx, sy]: [number, number]): StudioCam => ({
  ...cam,
  focal: cam.focal * k,
  cx: sx + (cam.cx - sx) * k,
  cy: sy + (cam.cy - sy) * k,
});

/** Shift the image (lens shift) so a world point lands on a screen point; keeps depth and perspective. */
export const frameOn = (cam: StudioCam, p: [number, number, number], [sx, sy]: [number, number]): StudioCam => {
  const q = project(cam, p);
  if (!q) return cam;
  return { ...cam, cx: cam.cx + (sx - q.x), cy: cam.cy + (sy - q.y) };
};

/**
 * A pose frame standing on the floor at (x, z), facing the camera (a card, so the figure keeps the app's
 * proportions). The frame's horizontal centre (x = 0.5, where the DSL centres a figure) sits over the point; its
 * lowest ink sits on the floor. Every pose is scaled by its own body unit, so all figures are people of one size.
 *   opts.lift   metres above the floor (a figure "set down" from 1% H above its mark)
 *   opts.nudge  shadow offset in metres along +x (away from a key light at the upper left)
 *   opts.flipX  the figure is drawn mirrored for display (shadows follow)
 */
export const placePose = (
  cam: StudioCam,
  pose: Pose,
  x: number,
  z: number,
  opts: { lift?: number; nudge?: number; flipX?: boolean } = {},
) => {
  const ground = project(cam, [x, opts.lift ?? 0, z]);
  if (!ground) return null;
  const frameM = BODY_UNIT_M / poseUnit(pose); // metres per frame height
  const height = frameM * ground.scale; // frame height in px
  const width = height * pose.frameAspect;
  const gy = poseGround(pose);
  const frame = { x: ground.x - width / 2, y: ground.y - gy * height, height };
  const shadows = figureFootprints(pose).map((fp) => {
    const cxF = opts.flipX ? 1 - fp.cx : fp.cx;
    const sx = x + (cxF - 0.5) * pose.frameAspect * frameM + (opts.nudge ?? 0.05);
    const rx = Math.max(0.19 * pose.frameAspect * frameM * fp.scale, 0.6 * fp.inkW * pose.frameAspect * frameM);
    return floorEllipse(cam, sx, z, rx, rx * 0.42);
  });
  return { frame, depth: ground.depth, scale: ground.scale, shadows };
};

const footprintCache = new Map<string, { cx: number; inkW: number; scale: number }[]>();

/**
 * Per figure: where its contact shadow goes (frame x), from the app's rule: under the ankles when they are on the
 * ground; under the figure's ink (lying, crouching) otherwise. inkW is in frame widths (0 when the ankles rule).
 */
const figureFootprints = (pose: Pose) => {
  let fps = footprintCache.get(pose.id);
  if (fps) return fps;
  const gy = poseGround(pose);
  fps = pose.figures.map((f) => {
    const b = shapeBounds(buildShapes(f, pose.frameAspect));
    const minX = b.minX / pose.frameAspect, maxX = b.maxX / pose.frameAspect;
    const la = f.joints.LEFT_ANKLE, ra = f.joints.RIGHT_ANKLE;
    const anklesDown = !!la && !!ra && Math.max(la[1], ra[1]) > gy - 0.06;
    // multi-person frames: each person gets a slightly smaller oval so neighbours don't merge into a slab
    const scale = pose.figures.length > 1 ? 0.75 : 1;
    return anklesDown
      ? { cx: (la![0] + ra![0]) / 2, inkW: 0, scale }
      : { cx: (minX + maxX) / 2, inkW: maxX - minX, scale };
  });
  footprintCache.set(pose.id, fps);
  return fps;
};
