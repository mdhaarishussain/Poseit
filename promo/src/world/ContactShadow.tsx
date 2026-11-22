/**
 * ContactShadow.tsx: the one shadow in Studio, a soft oval under the feet that puts a figure on the floor
 * (app: PoseFigure.kt drawContactShadow). Ink radial falloff 18% at the centre, 10% at 55%, 0 at the rim.
 *
 *   <ShadowDefs id="cs" strength={1} />   one shared radial gradient (objectBoundingBox, so it fits any ellipse);
 *                                          put it once in the <svg>
 *   <ContactShadow cx cy rx ry gradientId="cs" opacity? />   an ellipse on the floor; in the studio, rx is the
 *                                          footprint's world width and ry its perspective-foreshortened depth
 *   contactShadowForFrame(skeletonJoints, frame)  the app's own placement for a flat tile: centre under the ankles
 *                                          + 1.4% of the frame height, rx = 0.19 frame width, ry = 0.16 rx
 */
import React from "react";
import { theme } from "../theme";
import type { Joints } from "../figure/types";

export const ShadowDefs: React.FC<{ id: string; strength?: number }> = ({ id, strength = 1 }) => (
  <defs>
    <radialGradient id={id} cx="0.5" cy="0.5" r="0.5" gradientUnits="objectBoundingBox">
      <stop offset="0" stopColor={theme.color.ink} stopOpacity={0.18 * strength} />
      <stop offset="0.55" stopColor={theme.color.ink} stopOpacity={0.1 * strength} />
      <stop offset="1" stopColor={theme.color.ink} stopOpacity={0} />
    </radialGradient>
  </defs>
);

export const ContactShadow: React.FC<{
  cx: number;
  cy: number;
  rx: number;
  ry: number;
  gradientId: string;
  opacity?: number;
}> = ({ cx, cy, rx, ry, gradientId, opacity = 1 }) => (
  <ellipse cx={cx} cy={cy} rx={Math.max(0, rx)} ry={Math.max(0, ry)} fill={`url(#${gradientId})`} opacity={opacity} />
);

/** The app's tile placement (frame = pixel rect of the pose frame). Null when the ankles are off the frame. */
export const contactShadowForFrame = (
  joints: Joints,
  frame: { x: number; y: number; w: number; h: number },
): { cx: number; cy: number; rx: number; ry: number } | null => {
  const l = joints.LEFT_ANKLE, r = joints.RIGHT_ANKLE;
  if (!l || !r) return null;
  const low = Math.max(l[1], r[1]);
  if (low < 0 || low > 1.02) return null;
  const rx = frame.w * 0.19;
  return { cx: frame.x + ((l[0] + r[0]) / 2) * frame.w, cy: frame.y + low * frame.h + frame.h * 0.014, rx, ry: rx * 0.16 };
};
