/**
 * StudioFigures.tsx: real poses standing on the studio floor, drawn back to front in one svg.
 *
 *   <StudioFigures cam={StudioCam} items={[{ pose, x, z, lift?, flipX?, tones?, key? }]} depth? />
 *     items   floor positions in metres (camera.ts slotPosition(row, col) for the lattice). Each pose is scaled by
 *             its body unit (all figures are people of one size), stands its lowest ink on the floor and gets one
 *             contact shadow per person (Ink 18% -> 0, nudged right, away from the key).
 *     tones   person tones for multi-person poses (default theme.color.personTones)
 *     depth   (depth metres) => { blur px, haze 0..1 }: defocus and haze toward Paper by distance from the camera.
 *             Default: sharp from 5.5 to 14 m, softening in front (6 px at 3 m) and behind (3 px, 10% haze at 24 m).
 *     shadowStrength  multiplies the shadow alpha (1 = the app's 18%)
 * Figures behind the camera or far outside the frame are skipped. Items are depth-sorted (far first).
 */
import React from "react";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import type { Pose } from "../figure/types";
import { placePose, type StudioCam } from "./camera";
import { ContactShadow, ShadowDefs } from "./ContactShadow";

export type StudioItem = {
  pose: Pose;
  x: number;
  z: number;
  lift?: number;
  flipX?: boolean;
  tones?: string[];
  key?: string;
  opacity?: number;
};

export type DepthLook = (depth: number) => { blur: number; haze: number };

const smooth = (t: number) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));

export const defaultDepth: DepthLook = (d) => ({
  blur: d < 5.5 ? 6 * smooth((5.5 - d) / 2.5) : d > 14 ? 3 * smooth((d - 14) / 10) : 0,
  haze: d > 14 ? 0.1 * smooth((d - 14) / 10) : 0,
});

export const StudioFigures: React.FC<{
  cam: StudioCam;
  items: StudioItem[];
  depth?: DepthLook;
  shadowStrength?: number;
  idPrefix?: string;
}> = ({ cam, items, depth = defaultDepth, shadowStrength = 1, idPrefix = "sf" }) => {
  const { W, H } = theme.size;
  const placed = items
    .map((it, i) => ({ it, i, p: placePose(cam, it.pose, it.x, it.z, { lift: it.lift, flipX: it.flipX }) }))
    .filter((e) => e.p !== null)
    .filter((e) => {
      const f = e.p!.frame;
      const w = f.height * e.it.pose.frameAspect;
      return f.x < W * 1.2 && f.x + w > -W * 0.2 && f.y < H * 1.2 && f.y + f.height > -H * 0.2;
    })
    .sort((a, b) => b.p!.depth - a.p!.depth);
  // one blur filter per distinct (rounded) blur radius
  const blurs = Array.from(new Set(placed.map((e) => Math.round(depth(e.p!.depth).blur * 2) / 2).filter((b) => b > 0)));
  const shadowId = `${idPrefix}-cs`;
  return (
    <svg width={W} height={H} style={{ position: "absolute", inset: 0, overflow: "visible" }}>
      <ShadowDefs id={shadowId} strength={shadowStrength} />
      <defs>
        {blurs.map((b) => (
          <filter key={b} id={`${idPrefix}-b${b * 10}`} x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation={b} />
          </filter>
        ))}
      </defs>
      {placed.map(({ it, i, p }) => {
        const look = depth(p!.depth);
        const b = Math.round(look.blur * 2) / 2;
        const tones = it.tones ?? theme.color.personTones;
        const hz = (c: string) => (look.haze > 0 ? theme.signal.mixOklab(c, theme.color.paper, look.haze) : c);
        return (
          <g key={it.key ?? i} filter={b > 0 ? `url(#${idPrefix}-b${b * 10})` : undefined} opacity={it.opacity ?? 1}>
            {p!.shadows.map((s, k) =>
              s ? <ContactShadow key={k} {...s} gradientId={shadowId} opacity={1 - look.haze} /> : null,
            )}
            {it.pose.figures.map((f, k) => (
              <FigureG
                key={k}
                skeleton={f}
                aspect={it.pose.frameAspect}
                x={p!.frame.x}
                y={p!.frame.y}
                height={p!.frame.height}
                flipX={it.flipX}
                fill={hz(tones[k % tones.length])}
                contourAlpha={theme.color.contourAlpha * (1 - look.haze)}
              />
            ))}
          </g>
        );
      })}
    </svg>
  );
};
