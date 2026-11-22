/**
 * hooks.ts: the footage data in a component. Files are fetched from public/ once per page (staticFile) and the
 * render waits for them (delayRender), so every frame of a render has its data.
 *
 *   useFootageJson<T>(path, {optional}) any file under public/, e.g. "footage/nearest.json"; null until loaded
 *                                       (and, with optional, when the file does not exist)
 *   useLandmarks(id, frame?, playback?) the plate's landmarks on the global frame (default: the current frame;
 *                                       playback = the Footage's freezeAt / rate / start, if any):
 *                                       { n, record, people (smoothed app skeletons), drawn (what the overlay
 *                                       draws), raw, file } or null while loading
 *   useFootageMatch(id, frame?)         the plate's match on the global frame: { record, colors, guide, file }
 *
 * `frame` is a GLOBAL film frame (the scenes receive it as a prop). Inside a <Sequence>, pass it explicitly or the
 * hook would read the sequence's local frame.
 */
import { useEffect, useRef, useState } from "react";
import { cancelRender, continueRender, delayRender, staticFile, useCurrentFrame } from "remotion";
import { plateFrameFor, type PlaybackOptions } from "./plates";
import {
  drawable,
  guideFigures,
  matchRecordAt,
  partColors,
  peopleAt,
  rawAt,
  type FootageMatchFile,
  type LandmarksFile,
} from "./data";

const loaded = new Map<string, unknown>();
const pending = new Map<string, Promise<unknown>>();

const load = (path: string, optional: boolean): Promise<unknown> => {
  let p = pending.get(path);
  if (!p) {
    p = fetch(staticFile(path)).then((r) => {
      if (r.status === 404 && optional) return null;
      if (!r.ok) throw new Error(`footage: ${path} ${r.status} (run npm run landmarks / footage-match)`);
      return r.json();
    });
    p.then((d) => loaded.set(path, d)).catch(() => pending.delete(path));
    pending.set(path, p);
  }
  return p;
};

/** `optional`: a missing file (404) reads as null instead of failing the render (e.g. a plate with no match file). */
export const useFootageJson = <T,>(path: string | null, { optional = false }: { optional?: boolean } = {}): T | null => {
  // One delayRender handle per path this component waits for, taken during render (before the frame can be
  // captured) and released only after a commit that has the data, or when the component stops wanting that path.
  const handles = useRef(new Map<string, number>());
  const [tick, setTick] = useState(0);
  if (path && !loaded.has(path) && !handles.current.has(path)) handles.current.set(path, delayRender(`footage data ${path}`));
  useEffect(() => {
    if (!path) return;
    const release = (p: string) => {
      const h = handles.current.get(p);
      if (h !== undefined) {
        handles.current.delete(p);
        continueRender(h);
      }
    };
    let live = true;
    if (loaded.has(path)) release(path);
    else
      load(path, optional)
        .then(() => live && setTick((t) => t + 1))
        .catch((e) => cancelRender(e));
    return () => {
      live = false;
      release(path);
    };
  }, [path, tick, optional]);
  return path && loaded.has(path) ? ((loaded.get(path) as T | null) ?? null) : null;
};

export const useLandmarks = (id: string, frame?: number, playback: PlaybackOptions = {}) => {
  const current = useCurrentFrame();
  const file = useFootageJson<LandmarksFile>(`footage/${id}.landmarks.json`);
  if (!file) return null;
  const n = Math.min(file.frames.length - 1, plateFrameFor(id, frame ?? current, playback));
  const people = peopleAt(file, n);
  return { n, record: file.frames[n], people, drawn: people.map((p) => drawable(p)), raw: rawAt(file, n), file };
};

export const useFootageMatch = (id: string, frame?: number) => {
  const current = useCurrentFrame();
  const file = useFootageJson<FootageMatchFile>(`footage/${id}.match.json`);
  if (!file) return null;
  const record = matchRecordAt(file, frame ?? current);
  return { record, colors: partColors(record), guide: guideFigures(file), file };
};
