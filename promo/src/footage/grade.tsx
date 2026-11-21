/**
 * grade.tsx: the Studio ramp the footage is printed in until the shutter (manifest "grade", theme.footage.ramp).
 *
 *   rampCurve(l, trim)       luma 0..1 -> ramp position 0..1: the shot's trim (a midtone lift or lower that keeps
 *                            black and white: l + trim * 4 l (1 - l)), then the gentle S (theme.footage.ramp.contrast)
 *   <RampFilter id trim />   an SVG <filter> (sRGB maths): Rec.709 luma, then per channel a table from Ink (black
 *                            point) to Sheet (white point) through rampCurve. Saturation 0, no vignette, no glow.
 *                            Use it with style={{ filter: `url(#${id})` }} on the element to print.
 *
 * The colour look (the captured photo) is the plate as it is: no filter.
 */
import React from "react";
import { theme } from "../theme";

export type Look = "duotone" | "colour";

const smoothstep = (x: number) => x * x * (3 - 2 * x);
const hex = (c: string) => [1, 3, 5].map((i) => parseInt(c.slice(i, i + 2), 16) / 255);

/** How much of a smoothstep the S blends in for a given contrast token (1.06 -> 0.3: gentle). */
const sAmount = () => Math.min(1, Math.max(0, (theme.footage.ramp.contrast - 1) * 5));

export const rampCurve = (l: number, trim = 0): number => {
  const x = Math.min(1, Math.max(0, l));
  const lifted = Math.min(1, Math.max(0, x + trim * 4 * x * (1 - x)));
  return lifted + (smoothstep(lifted) - lifted) * sAmount();
};

const STEPS = 32;

export const RampFilter: React.FC<{ id: string; trim?: number }> = ({ id, trim = 0 }) => {
  const lo = hex(theme.footage.ramp.shadow);
  const hi = hex(theme.footage.ramp.highlight);
  const curve = Array.from({ length: STEPS + 1 }, (_, i) => rampCurve(i / STEPS, trim));
  const table = (c: number) => curve.map((v) => (lo[c] + (hi[c] - lo[c]) * v).toFixed(4)).join(" ");
  return (
    <svg width={0} height={0} style={{ position: "absolute" }} aria-hidden>
      <defs>
        <filter id={id} colorInterpolationFilters="sRGB" x="0" y="0" width="100%" height="100%">
          <feColorMatrix
            type="matrix"
            values={[
              "0.2126 0.7152 0.0722 0 0",
              "0.2126 0.7152 0.0722 0 0",
              "0.2126 0.7152 0.0722 0 0",
              "0 0 0 1 0",
            ].join(" ")}
          />
          <feComponentTransfer>
            <feFuncR type="table" tableValues={table(0)} />
            <feFuncG type="table" tableValues={table(1)} />
            <feFuncB type="table" tableValues={table(2)} />
          </feComponentTransfer>
        </filter>
      </defs>
    </svg>
  );
};
