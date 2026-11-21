/**
 * plates.ts: the prepared plates (public/footage/plates.json, written by scripts/fetch-footage.mjs). Pure, no React.
 *
 * Every plate is a 30 fps file in FILM time: plate frame n is global film frame `filmFrom + n` (a 15-frame handle on
 * each side of the shot), and shows source frame `sourceFrames[n]` of the clip. So for every shot, real-time or
 * remapped, the plate frame of global frame f is f - filmFrom; nothing else is needed to keep the video, the
 * landmarks and the matcher's verdicts on the same frame.
 *
 *   PLATES                 plate id -> record (shot, clip, film window, filmFrom, frames, crop, source frames)
 *   plateById(id)          one record (throws on an unknown id)
 *   plateFrameOf(id, f)    the plate frame on global frame f (clamped to the plate)
 *   plateFrameFor(id, f, o) the same with playback options (freezeAt, rate, start): what Footage shows
 *   sourceSecondAt(id, f)  the source second the plate shows on global frame f
 *   plateFile(id)          "footage/<id>.mp4" (pass to staticFile)
 */
import raw from "../../public/footage/plates.json";
import type { Rect } from "./manifest";

export type Plate = {
  shot: string;
  clip: string;
  catalogue: string;
  /** null for a plain shot; "remap" (time-remapped live shot), "1x" (its real-time twin), "alternate" */
  variant: "remap" | "1x" | "alternate" | null;
  alternateOf?: string;
  keyTime?: number;
  film: [number, number];
  /** global film frame of plate frame 0 */
  filmFrom: number;
  frames: number;
  /** plate frame of the shot's first film frame (the handle length) */
  shotStart: number;
  crop: Rect;
  cropScale: number;
  source: { fps: number; frames: number; size: [number, number]; in?: number | null; remap?: [number, number][] };
  /** source frame index shown on each plate frame */
  sourceFrames: number[];
  hash: string;
};

export type PlatesFile = {
  version: number;
  fps: number;
  handle: number;
  width: number;
  height: number;
  plates: Record<string, Plate>;
};

export const PLATES_FILE = raw as unknown as PlatesFile;
export const PLATES = PLATES_FILE.plates;

export const plateById = (id: string): Plate => {
  const p = PLATES[id];
  if (!p) throw new Error(`footage: no plate "${id}" in public/footage/plates.json (run npm run footage)`);
  return p;
};

export const plateFrameOf = (id: string, f: number): number => {
  const p = plateById(id);
  return Math.max(0, Math.min(p.frames - 1, Math.floor(f) - p.filmFrom));
};

export type PlaybackOptions = {
  /** GLOBAL frame from which the plate holds still (the photo the app takes) */
  freezeAt?: number;
  /** playback speed relative to the plate (1 = film time, the default); from `start` on */
  rate?: number;
  /** GLOBAL frame where a rate other than 1 starts (default: the plate's first frame) */
  start?: number;
};

/**
 * The plate frame shown on global frame f, for the Footage component and the landmark hooks alike: f - filmFrom by
 * default; held after freezeAt; with a rate, (start - filmFrom) + (f - start) * rate, floored. Clamped to the plate.
 */
export const plateFrameFor = (id: string, f: number, o: PlaybackOptions = {}): number => {
  const p = plateById(id);
  const g = o.freezeAt != null && f >= o.freezeAt ? o.freezeAt : f;
  const start = o.start ?? p.filmFrom;
  const rate = o.rate ?? 1;
  const n = rate === 1 ? Math.floor(g) - p.filmFrom : Math.floor(start - p.filmFrom + (g - start) * rate + 1e-6);
  return Math.max(0, Math.min(p.frames - 1, n));
};

export const sourceSecondAt = (id: string, f: number): number => {
  const p = plateById(id);
  return p.sourceFrames[plateFrameOf(id, f)] / p.source.fps;
};

export const plateFile = (id: string): string => `footage/${id}.mp4`;
