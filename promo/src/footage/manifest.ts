/**
 * manifest.ts: the typed view of footage/manifest.json (STORYBOARD.md v2). Pure, no React, so scripts can bundle it.
 * Read-only for both builders: change the JSON, not this file's data.
 *
 *   MANIFEST               the whole manifest
 *   clipById(id)           a clip (source, page, file URL, licence, size, fps)
 *   shotById(id)           a shot (film window, source in-point, rate or time remap, crop, grade, pose plan)
 *   sourceTimeAt(shot, f)  the source second shown on global film frame f (time remap keys, else in + rate * t)
 *   plateFrameAt(shot, f)  the frame index inside the shot's prepared plate (public/footage/<shot>.mp4, 30 fps,
 *                          starting 0.5 s before the in-point: the handle)
 *   cropScale(shot)        how much the crop is enlarged to fill 1080 x 1920 (> 1 = upscaled)
 */
import raw from "../../footage/manifest.json";

export type Rect = { x: number; y: number; w: number; h: number };

export type Clip = {
  catalogue: string;
  title: string;
  source: "Mixkit";
  page: string;
  file: string;
  fileRes: string;
  fps: number;
  duration: number;
  licence: { name: string; url: string };
  alternateOnly?: boolean;
};

export type PosePlan = { planned: string; name: string; approxScore: number | null; who?: string };

export type Shot = {
  id: string;
  beat: string;
  clip: string;
  film: [number, number];
  source: { in: number; out?: number; window?: [number, number] };
  rate?: number;
  /** [filmFrame, sourceSeconds] keys, linear between them (the live shot) */
  timeRemap?: { planned: [number, number][]; rule: string; interpolation: string };
  crop: Rect;
  grade: { look: string; trim: number };
  keyTime?: number;
  pose?: PosePlan;
  role?: string;
  notes?: string;
};

export type Snap = { shot: string; clip: string; sourceTime: number; pose: string; film: number };

export type Manifest = {
  version: number;
  film: { width: number; height: number; fps: number; frames: number };
  rules: string[];
  clips: Record<string, Clip>;
  shots: Shot[];
  snaps: Snap[];
  snapRule: string;
};

export const MANIFEST = raw as unknown as Manifest;

/** Seconds of handle the prepared plate carries before the in-point (STORYBOARD.md v2 section 12, step 2). */
export const PLATE_HANDLE = 0.5;

export const clipById = (id: string): Clip => {
  const c = MANIFEST.clips[id];
  if (!c) throw new Error(`footage manifest: no clip "${id}"`);
  return c;
};

export const shotById = (id: string): Shot => {
  const s = MANIFEST.shots.find((x) => x.id === id);
  if (!s) throw new Error(`footage manifest: no shot "${id}"`);
  return s;
};

/** The source second on global frame f. Clamped to the shot's window; the remap keys win over in + rate. */
export const sourceTimeAt = (shot: Shot, f: number): number => {
  const fps = MANIFEST.film.fps;
  const keys = shot.timeRemap?.planned;
  if (keys && keys.length > 1) {
    if (f <= keys[0][0]) return keys[0][1];
    for (let i = 1; i < keys.length; i++) {
      const [f1, s1] = keys[i];
      const [f0, s0] = keys[i - 1];
      if (f <= f1) return s0 + ((s1 - s0) * (f - f0)) / (f1 - f0);
    }
    return keys[keys.length - 1][1];
  }
  const t = Math.max(0, Math.min(f, shot.film[1]) - shot.film[0]) / fps;
  return shot.source.in + (shot.rate ?? 1) * t;
};

/** Frame index in the prepared plate (30 fps, handle first) for global frame f, for real-time shots. */
export const plateFrameAt = (shot: Shot, f: number): number =>
  Math.round((sourceTimeAt(shot, f) - shot.source.in + PLATE_HANDLE) * MANIFEST.film.fps);

export const cropScale = (shot: Shot): number => MANIFEST.film.height / shot.crop.h;
