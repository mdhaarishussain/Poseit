/**
 * StudioPaper.tsx: the seamless studio paper (acts II and IV), shaded per pixel from the camera, not painted.
 *
 *   <StudioPaper cam={StudioCam} resolution?={0.5} />
 *
 * Each canvas pixel casts a ray from the studio camera (camera.ts) onto the sweep: floor (Studio Set), a soft
 * quarter-circle cove, then the wall (Studio Paper). The tone follows the surface's orientation through the cove
 * (Set on the floor, Paper on the wall) and one large softbox key, up and to the left of the set and in front of
 * it: inverse-square falloff with a Lambert term, mapped gently so the frame is a few percent lighter at the upper
 * left than at the lower right. Tone, not colour; no vignette, no radial wash. The paper's tooth is value noise
 * fixed to the SURFACE (metres along the paper), so it scales and recedes with the camera (it proves dolly vs zoom)
 * and fades out where it would be finer than a pixel, instead of shimmering.
 *
 * Exported for tests/tools: shadePaper(cam, w, h, rgba, opts) fills an RGBA buffer of w x h for a camera whose
 * coordinates are in 1080 x 1920 px.
 */
import React, { useLayoutEffect, useRef } from "react";
import { theme } from "../theme";
import { SWEEP, type StudioCam } from "./camera";

const rgb = (hex: string) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
const SET = rgb(theme.color.set);
const PAPER = rgb(theme.color.paper);

/** The softbox key: metres; upper left of the set, in front of it (toward the camera). */
export const KEY_LIGHT = { pos: [-20, 10, 14] as [number, number, number], ref: [0, 0, 12] as [number, number, number], gain: 0.04 };

const hash = (x: number, y: number) => {
  let h = (Math.imul(x, 374761393) + Math.imul(y, 668265263)) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
};
const vnoise = (x: number, y: number) => {
  const ix = Math.floor(x), iy = Math.floor(y);
  let fx = x - ix, fy = y - iy;
  fx = fx * fx * (3 - 2 * fx);
  fy = fy * fy * (3 - 2 * fy);
  const a = hash(ix, iy), b = hash(ix + 1, iy), c = hash(ix, iy + 1), d = hash(ix + 1, iy + 1);
  return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy; // 0..1
};
const smooth = (t: number) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));
const fade = (cell: number, footprint: number) => smooth((cell / footprint - 0.7) / 1.3);

const lightAt = (p: [number, number, number], n: [number, number, number]) => {
  const L = KEY_LIGHT.pos;
  const dx = L[0] - p[0], dy = L[1] - p[1], dz = L[2] - p[2];
  const d2 = dx * dx + dy * dy + dz * dz;
  const lam = Math.max(0.02, (n[0] * dx + n[1] * dy + n[2] * dz) / Math.sqrt(d2));
  return lam / d2;
};
const L_REF = lightAt(KEY_LIGHT.ref, [0, 1, 0]);

export type PaperOptions = { tooth?: number; mottle?: number };

export const shadePaper = (cam: StudioCam, w: number, h: number, out: Uint8ClampedArray, opts: PaperOptions = {}) => {
  const tooth = opts.tooth ?? 0.022; // peak-to-peak luma share of the fine tooth
  const mottle = opts.mottle ?? 0.012; // slower cloudiness of the paper
  const sx = theme.size.W / w; // composition px per canvas px
  const th = (cam.pitch * Math.PI) / 180, sinT = Math.sin(th), cosT = Math.cos(th);
  const { coveZ, coveR } = SWEEP;
  for (let j = 0; j < h; j++) {
    const syc = (j + 0.5) * sx;
    const v = -(syc - cam.cy) / cam.focal;
    const dy = -sinT + v * cosT, dz = cosT + v * sinT;
    // ---- hit the sweep: floor, cove, wall (y, z only: x never changes which part is hit) ----
    let t = Infinity, Y = 0, Z = 0, arc = 0, phiN = 0;
    let n: [number, number, number] = [0, 1, 0];
    if (dy < 0) {
      t = -cam.height / dy;
      Z = cam.z + t * dz;
      Y = 0;
      arc = Z;
    }
    if (!(dy < 0) || Z > coveZ) {
      // cove: circle centre (y = R, z = coveZ); take the far root and check its quarter
      const oy = cam.height - coveR, oz = cam.z - coveZ;
      const A = dy * dy + dz * dz, B = 2 * (oy * dy + oz * dz), C = oy * oy + oz * oz - coveR * coveR;
      const disc = B * B - 4 * A * C;
      let hit = false;
      if (disc >= 0) {
        const tc = (-B + Math.sqrt(disc)) / (2 * A);
        const yy = cam.height + tc * dy, zz = cam.z + tc * dz;
        if (tc > 0 && yy <= coveR && zz >= coveZ) {
          hit = true;
          t = tc; Y = yy; Z = zz;
          const phi = Math.atan2(yy - coveR, zz - coveZ); // -pi/2 (floor) .. 0 (wall)
          phiN = (phi + Math.PI / 2) / (Math.PI / 2);
          arc = coveZ + coveR * (phi + Math.PI / 2);
          n = [0, (coveR - yy) / coveR, (coveZ - zz) / coveR];
        }
      }
      if (!hit) {
        t = (coveZ + coveR - cam.z) / dz;
        Y = cam.height + t * dy; Z = coveZ + coveR;
        phiN = 1;
        arc = coveZ + (coveR * Math.PI) / 2 + (Y - coveR);
        n = [0, 0, -1];
      }
    }
    const m = smooth(phiN);
    const base0 = SET[0] + (PAPER[0] - SET[0]) * m;
    const base1 = SET[1] + (PAPER[1] - SET[1]) * m;
    const base2 = SET[2] + (PAPER[2] - SET[2]) * m;
    // footprint of one canvas px on the surface (metres), stretched where the view grazes the surface
    const len = Math.sqrt(1 + v * v);
    const graze = Math.max(0.12, Math.abs(n[1] * dy + n[2] * dz) / len);
    const fp = (t * len * sx) / cam.focal / graze;
    const aTooth = tooth * fade(0.0045, fp);
    const aMottle = mottle * fade(0.05, fp);
    let o = j * w * 4;
    for (let i = 0; i < w; i++) {
      const u = ((i + 0.5) * sx - cam.cx) / cam.focal;
      const X = cam.x + t * u;
      const lum = 1 + KEY_LIGHT.gain * Math.log(lightAt([X, Y, Z], n) / L_REF);
      let tex = 0;
      if (aTooth > 0.0005) tex += aTooth * (vnoise(X / 0.0045, arc / 0.0045) - 0.5);
      if (aMottle > 0.0005) tex += aMottle * (vnoise(X / 0.05 + 17.3, arc / 0.05 - 4.1) - 0.5);
      const k = lum * (1 + tex);
      out[o] = base0 * k;
      out[o + 1] = base1 * k;
      out[o + 2] = base2 * k;
      out[o + 3] = 255;
      o += 4;
    }
  }
};

export const StudioPaper: React.FC<{ cam: StudioCam; resolution?: number; options?: PaperOptions; style?: React.CSSProperties }> = ({
  cam,
  resolution = 0.5,
  options,
  style,
}) => {
  const ref = useRef<HTMLCanvasElement>(null);
  const w = Math.round(theme.size.W * resolution);
  const h = Math.round(theme.size.H * resolution);
  useLayoutEffect(() => {
    const ctx = ref.current?.getContext("2d");
    if (!ctx) return;
    const img = ctx.createImageData(w, h);
    shadePaper(cam, w, h, img.data, options);
    ctx.putImageData(img, 0, 0);
  }, [cam.x, cam.z, cam.height, cam.pitch, cam.focal, cam.cx, cam.cy, w, h, options?.tooth, options?.mottle]);
  return (
    <canvas
      ref={ref}
      width={w}
      height={h}
      style={{ position: "absolute", inset: 0, width: theme.size.W, height: theme.size.H, ...style }}
    />
  );
};
