/**
 * shots.ts: the geometry the acts share at their seams (pure; no React). Each seam is solved from the shot on
 * the other side, so the carried figure is the same size in the same place on both sides of a cut.
 *
 *   HOOK                 B1: the stiff skeleton's frame on screen (head top y 0.33, feet y 0.93, centred) and the
 *                        film camera's push (1.00 -> 1.04 about (0.5 W, 0.6 H)); hookScreenFrame(f) = where it is
 *   VF                   act III: the film camera on the viewfinder (scale about the guide's body)
 *   vfScale(f)           act III: lands from the B4 push (0.975 -> 1.00 in 6 f), creeps to 1.03 by the countdown,
 *                        then on to 1.07 by the capture while stepping 2% on each digit (one frame + an 8 f tail)
 *   guideScreenFrame(f)  the Disco point guide's template frame on screen at frame f (act III camera applied)
 *   scaleRect(r, s, o)   a frame rectangle scaled about a screen point
 *   HERO_AT_CUT          the studio hero's frame on f335 (the guide's frame, one frame of push short)
 *   MATCH_EVENTS         re-exported: the real act III timeline from match.json (found, cards, hold, digits, capture)
 */
import { theme } from "../theme";
import { illustrative, poseById, poseInkBounds } from "../figure/poses";
import { drift, keys, snapTail } from "../motion";
import { guideFrameFor } from "./viewfinder/rig";
import { MATCH_EVENTS } from "./viewfinder/match";

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

export const HOOK = {
  frame: { x: W / 2 - 0.5 * hookH * stiff.frameAspect, y: 0.33 * H - stiffInk.minY * hookH, height: hookH, width: hookH * stiff.frameAspect } as Rect,
  origin: [W / 2, 0.6 * H] as [number, number],
  scale: (f: number) => drift(f, [0, 80], [1, 1.04]),
};

/** The stiff figure's frame on screen at frame f of act I (the film camera's push applied). */
export const hookScreenFrame = (f: number): Rect => scaleRect(HOOK.frame, HOOK.scale(f), HOOK.origin);

// ---- act III: the viewfinder ---------------------------------------------------------------------------------
const disco = poseById("fun_disco_point");
export const GUIDE_FRAME: Rect = (() => {
  const g = guideFrameFor(disco);
  return { x: g.x, y: g.y, height: g.height, width: g.width };
})();

export const VF = {
  origin: [W / 2, 0.62 * H] as [number, number],
  /** the push from B4 carries on across the cut: f336 is at 0.975 and lands at 1.00 by f342 */
  landFrom: 0.975,
  /** the studio's last frame shows the hero at the guide's size times this (one frame of push before the cut) */
  seamScale: 0.95,
  stepAt: [MATCH_EVENTS.digit3, MATCH_EVENTS.digit2, MATCH_EVENTS.digit1],
};

export const vfScale = (f: number) => {
  const base = keys(f, [[336, VF.landFrom], [342, 1, E.softLand], [MATCH_EVENTS.digit3, 1.03, E.drift], [MATCH_EVENTS.capture, 1.07, E.drift]]);
  const steps = VF.stepAt.reduce((s, at) => s * snapTail(f, at, { from: 1, snap: 1.014, to: 1.02, tail: 8 }), 1);
  return base * steps;
};

export const guideScreenFrame = (f: number): Rect => scaleRect(GUIDE_FRAME, vfScale(f), VF.origin);

/** Where the studio's hero frame must be on the last studio frame (f335): the guide, one frame of push short. */
export const HERO_AT_CUT: Rect = scaleRect(GUIDE_FRAME, VF.seamScale, VF.origin);

export { MATCH_EVENTS };
