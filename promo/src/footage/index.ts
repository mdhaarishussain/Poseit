/**
 * src/footage: the stock footage layer (STORYBOARD.md v2). Read-only for the scene builders.
 *
 *   manifest.ts    the typed manifest (shots, clips, source time on a film frame, remap keys)
 *   plates.ts      the prepared plates (film-time files, plate frame = global frame - filmFrom)
 *   transform.ts   the one plate -> composition transform (push about a scene anchor) and sceneToComp
 *   grade.tsx      the Studio ramp (RampFilter, rampCurve)
 *   data.ts        landmark / match / nearest file types and pure readers
 *   hooks.ts       useLandmarks, useFootageMatch, useFootageJson
 *   Footage.tsx    the Footage component (video + overlays through one transform)
 */
export * from "./manifest";
export * from "./plates";
export * from "./transform";
export * from "./grade";
export * from "./data";
export * from "./hooks";
export * from "./Footage";
