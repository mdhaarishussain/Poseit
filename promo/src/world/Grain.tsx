/**
 * Grain.tsx: the film's temporal grain, one level for the whole film (storyboard section 3).
 *
 *   <FilmGrain />             light grounds (the studio): a fine neutral noise tile, mix-blend overlay, re-seeded
 *                             every frame (deterministic jumps, never a slide)
 *   <SensorNoise />           the black live view: sparse luma speckle added with mix-blend screen, so black stays
 *                             nearly black (mean lift well under 1%), like a dark phone preview
 * Props: amount (opacity, defaults tuned on renders), seed (to decorrelate two layers), tile (px, 256).
 * The tiles are generated once per tab from a seeded hash (no binary assets, identical in every render).
 */
import React, { useMemo } from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";

const hash = (x: number) => {
  let h = Math.imul(x ^ 0x9e3779b9, 0x85ebca6b);
  h = Math.imul(h ^ (h >>> 13), 0xc2b2ae35);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
};

const tiles = new Map<string, string>();

/** A seeded noise tile as a data URL. kind "mid": gaussian around grey; kind "speck": positive tail only. */
const noiseTile = (kind: "mid" | "speck", size: number, seed: number): string => {
  const key = `${kind}-${size}-${seed}`;
  const hit = tiles.get(key);
  if (hit) return hit;
  if (typeof document === "undefined") return "";
  const c = document.createElement("canvas");
  c.width = size;
  c.height = size;
  const ctx = c.getContext("2d")!;
  const img = ctx.createImageData(size, size);
  for (let i = 0; i < size * size; i++) {
    // approx gaussian: sum of 4 uniforms
    const g = (hash(i * 4 + seed * 7919) + hash(i * 4 + 1 + seed * 7919) + hash(i * 4 + 2 + seed * 7919) + hash(i * 4 + 3 + seed * 7919) - 2) * 1.732;
    const v = kind === "mid" ? 128 + g * 46 : Math.max(0, g - 1.6) * 90;
    img.data[i * 4] = img.data[i * 4 + 1] = img.data[i * 4 + 2] = Math.max(0, Math.min(255, v));
    img.data[i * 4 + 3] = 255;
  }
  ctx.putImageData(img, 0, 0);
  const url = c.toDataURL("image/png");
  tiles.set(key, url);
  return url;
};

const useJitter = (seed: number, tile: number) => {
  const frame = useCurrentFrame();
  return [Math.floor(hash(frame * 2 + seed * 101) * tile), Math.floor(hash(frame * 2 + 1 + seed * 101) * tile)];
};

export const FilmGrain: React.FC<{ amount?: number; seed?: number; tile?: number }> = ({ amount = 0.07, seed = 1, tile = 256 }) => {
  const url = useMemo(() => noiseTile("mid", tile, seed), [tile, seed]);
  const [ox, oy] = useJitter(seed, tile);
  return (
    <AbsoluteFill
      style={{
        pointerEvents: "none",
        backgroundImage: `url(${url})`,
        backgroundSize: `${tile}px ${tile}px`,
        backgroundPosition: `${ox}px ${oy}px`,
        mixBlendMode: "overlay",
        opacity: amount,
      }}
    />
  );
};

export const SensorNoise: React.FC<{ amount?: number; seed?: number; tile?: number }> = ({ amount = 0.35, seed = 2, tile = 256 }) => {
  const url = useMemo(() => noiseTile("speck", tile, seed), [tile, seed]);
  const [ox, oy] = useJitter(seed, tile);
  return (
    <AbsoluteFill
      style={{
        pointerEvents: "none",
        backgroundImage: `url(${url})`,
        backgroundSize: `${tile}px ${tile}px`,
        backgroundPosition: `${ox}px ${oy}px`,
        mixBlendMode: "screen",
        opacity: amount,
      }}
    />
  );
};
