/**
 * shots.ts: the geometry the acts share at their seams (pure; no React). Each seam is solved from the shot on
 * the other side, so the carried figure is the same size in the same place on both sides of a cut.
 *
 *   HOOK                 B1 (WORLD): the stiff skeleton's frame (head top y 0.33, feet y 0.93, centred) and the film
 *                        camera HOOK.cam(f): the drift 1.00 -> 1.03 about (0.5 W, 0.6 H) to f70, then the rush f70-79
 *                        solved so the skeleton lands on B2's body on f80; hookScreenFrame(f) = where it is
 *   B2_BODY_AT_CUT       re-exported from people/data.ts: her nose, lower ankle and mid-hip x on screen on f80
 *   GUIDE_FRAME          act IV (LIVE): the "Hands in pockets" template frame where the app placed it over her
 *                        (SceneMapping.fitFrame + the solved OverlayTransform, back camera, unflipped), film px
 *   VF                   act IV: the film camera on the live view (a zoom of the plate about the guide's mid-hip)
 *   vfScale(f)           act IV: never under 1.00 (the plate's edge); f336 1.00 lands at 1.025 by f342 (the B4
 *                        push carried across the cut), creeps to 1.035 by the countdown and 1.045 by capture - 8,
 *                        stepping 1.5% on the lock and on each digit (one frame + an 8 f tail); still for the last
 *                        8 f; 1.11 at most
 *   guideScreenFrame(f)  the guide's template frame on screen at frame f of the live view (film camera applied)
 *   PHOTO, photoScale(f) B7: the captured photo (the whole frame, colour) and its slow push about her mid-hip,
 *                        1.00 at the capture, about 1.06 on the f555 cut (theme.footage.photoPush), still moving at the cut
 *   PHOTO_FIGURE_AT_CUT  the guide's template frame on the photo on f554 (photo push applied): where B8's figure stands
 *   scaleRect(r, s, o)   a frame rectangle scaled about a screen point
 *   HERO_AT_CUT          the studio hero's frame on f335 (the guide's frame, one frame of push short)
 *   MATCH_EVENTS         re-exported: the real act III timeline from match.json (found, cards, hold, digits, capture)
 */
import { theme } from "../theme";
import { illustrative, poseInkBounds } from "../figure/poses";
import { drift, keys, snapTail } from "../motion";
import { templateFrameRect } from "./viewfinder/rig";
import { MATCH_EVENTS, MATCH_GUIDE } from "./viewfinder/match";
import { B2_BODY_AT_CUT } from "./people/data";
import { HOOK_TIMING } from "./timing";

const { W, H } = theme.size;
const E = theme.ease;

export type Rect = { x: number; y: number; height: number; width: number };

export const scaleRect = (r: Rect, s: number, [ox, oy]: [number, number]): Rect => ({
  x: ox + (r.x - ox) * s,
  y: oy + (r.y - oy) * s,
  height: r.height * s,
  width: r.width * s,
});

// ---- act I: the hook -----------------------------------------------------------------------------------------
const stiff = illustrative("promo_stiff");
const stiffInk = poseInkBounds(stiff);
const hookH = (0.93 - 0.33) * H / (stiffInk.maxY - stiffInk.minY);

const hookFrame = { x: W / 2 - 0.5 * hookH * stiff.frameAspect, y: 0.33 * H - stiffInk.minY * hookH, height: hookH, width: hookH * stiff.frameAspect } as Rect;
const hookOrigin: [number, number] = [W / 2, 0.6 * H];
/** v2 (WORLD): the rush f70-79 lands the stiff skeleton on B2's body on f80 (nose, lower ankle, mid-hip x). */
const HOOK_RUSH = (() => {
  const j = stiff.figures[0].joints;
  const disp = (p: [number, number]): [number, number] => [hookFrame.x + hookFrame.width * (1 - p[0]), hookFrame.y + hookFrame.height * p[1]];
  const nose = disp(j.NOSE!), ankle = Math.max(disp(j.LEFT_ANKLE!)[1], disp(j.RIGHT_ANKLE!)[1]);
  const hipX = disp([(j.LEFT_HIP![0] + j.RIGHT_HIP![0]) / 2, 0])[0];
  const sb = (B2_BODY_AT_CUT.ankleY - B2_BODY_AT_CUT.noseY) / (ankle - nose[1]);
  const tb: [number, number] = [B2_BODY_AT_CUT.hipX - sb * hipX, B2_BODY_AT_CUT.noseY - sb * nose[1]];
  const sa = 1.03;
  const ta: [number, number] = [hookOrigin[0] * (1 - sa), hookOrigin[1] * (1 - sa)];
  const r = sb / sa;
  const p: [number, number] = [(tb[0] - r * ta[0]) / (1 - r), (tb[1] - r * ta[1]) / (1 - r)];
  return { sa, ta, r, p, sb, tb };
})();

export const HOOK = {
  frame: hookFrame,
  origin: hookOrigin,
  /**
   * The film camera on the hook as comp = s * p + (x, y): the v1 drift 1.00 -> 1.03 about `origin` (f0-70), then
   * the rush (gentleIn, peak speed on f79): a zoom about a fixed point onto B2's body (people/data.ts B2_BODY_AT_CUT).
   */
  cam: (f: number) => {
    const R = HOOK_RUSH;
    if (f < HOOK_TIMING.push[0]) {
      const s = drift(f, [0, HOOK_TIMING.push[0]], [1, R.sa]);
      return { s, x: hookOrigin[0] * (1 - s), y: hookOrigin[1] * (1 - s) };
    }
    const e = theme.ease.gentleIn(Math.min(1, (f - HOOK_TIMING.push[0]) / (HOOK_TIMING.push[1] - 1 - HOOK_TIMING.push[0])));
    const k = Math.pow(R.r, e);
    return { s: R.sa * k, x: R.p[0] * (1 - k) + k * R.ta[0], y: R.p[1] * (1 - k) + k * R.ta[1] };
  },
};

/** The stiff figure's frame on screen at frame f of act I (the film camera applied). */
export const hookScreenFrame = (f: number): Rect => {
  const c = HOOK.cam(f);
  return { x: c.s * HOOK.frame.x + c.x, y: c.s * HOOK.frame.y + c.y, height: HOOK.frame.height * c.s, width: HOOK.frame.width * c.s };
};

export { B2_BODY_AT_CUT };

// ---- act IV: the live view (BUILDER LIVE) ----------------------------------------------------------------------
/** The guide's template frame as the app placed it over her (b5-live.match.json guide), film px, unpushed. */
export const GUIDE_FRAME: Rect = templateFrameRect(MATCH_GUIDE.frameAspect, MATCH_GUIDE.transform);

const guideJ = MATCH_GUIDE.figures[0].joints;
const guideHip: [number, number] = [(guideJ.LEFT_HIP![0] + guideJ.RIGHT_HIP![0]) / 2, (guideJ.LEFT_HIP![1] + guideJ.RIGHT_HIP![1]) / 2];
/**
 * The film camera's zoom centre: the guide's mid-hip x, 0.06 H above its mid-hip. Her hair is at about 0.33 H and her
 * shoes at 0.94 H unpushed; about this point the largest push (1.11, the countdown's end) keeps the hair under the
 * countdown's glyph (0.28 H) and the shoes inside the frame (0.98 H).
 */
const guideChest: [number, number] = [guideHip[0], guideHip[1] - 0.06];

export const VF = {
  /** the film camera zooms the plate about this point (scene units; `origin` is the same point in px) */
  anchor: guideChest,
  origin: [guideChest[0] * W, guideChest[1] * H] as [number, number],
  /** the push from B4 carries on across the cut: f336 shows the plate at 1.00 (never less: its edge would show) */
  landFrom: 1,
  /** ... and lands here by f342 */
  landTo: 1.025,
  /** the studio's last frame shows the hero at the guide's size times this: the same 2.6% step per frame as v1 */
  seamScale: 0.975,
  /** one-frame steps (1% + a 0.5% tail): the lock (the picture's half of hit 2) and each countdown digit */
  stepAt: [MATCH_EVENTS.hold, ...MATCH_EVENTS.digits.map((d) => d.from)],
  /** the last 8 frames before the capture: nothing moves but the footage */
  still: MATCH_EVENTS.capture - 8,
};

export const vfScale = (f: number) => {
  const base = keys(f, [[336, VF.landFrom], [342, VF.landTo, E.softLand], [MATCH_EVENTS.countdown, 1.035, E.drift], [VF.still, 1.045, E.drift]]);
  const steps = VF.stepAt.reduce((s, at) => s * snapTail(f, at, { from: 1, snap: 1.01, to: 1.015, tail: 8 }), 1);
  return base * steps;
};

export const guideScreenFrame = (f: number): Rect => scaleRect(GUIDE_FRAME, vfScale(f), VF.origin);

/**
 * B7: the photo the app saves is the whole scene, so under the capture flash the film camera lets go of the live
 * view's zoom and the frozen colour frame pushes 1.00 -> 1.06 about her mid-hip by the f555 cut (a sine drift over
 * twice the span, so it is still moving at the cut).
 */
export const PHOTO = { from: MATCH_EVENTS.capture, cut: 555, anchor: guideHip, push: theme.footage.photoPush };
export const photoScale = (f: number) =>
  drift(f, [PHOTO.from, 2 * PHOTO.cut - PHOTO.from], [PHOTO.push[0], 2 * PHOTO.push[1] - PHOTO.push[0]]);

/** The guide's frame on the photo on f554, the last photo frame: B8's figure stands exactly here. */
export const PHOTO_FIGURE_AT_CUT: Rect = scaleRect(GUIDE_FRAME, photoScale(PHOTO.cut - 1), [PHOTO.anchor[0] * W, PHOTO.anchor[1] * H]);

/** Where the studio's hero frame must be on the last studio frame (f335): the guide, one frame of push short. */
export const HERO_AT_CUT: Rect = scaleRect(GUIDE_FRAME, VF.seamScale, VF.origin);

export { MATCH_EVENTS };
