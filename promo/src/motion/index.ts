/**
 * motion/index.ts: frame-pure motion helpers. Every value is a function of the frame; every interpolation is eased
 * (theme.ease, never linear) and clamped at both ends. Frames, not seconds (30 fps; theme.grid for the beat).
 *
 *   tween(frame, [f0, f1], [v0, v1], ease)      one eased, clamped segment (ease is required)
 *   keys(frame, [[f, v, ease?], ...])           piecewise keys; a key's ease shapes the segment ENDING at it
 *                                              (default softLand); holds before the first / after the last key
 *   land(frame, at, { from, to?, frames?, ease? })     arrival: most travel in the first 4-8 f (softLand 12 f)
 *   leave(frame, at, { from?, to, frames?, ease? })    exit: accelerates, ends at peak speed (accelExit 13 f)
 *   arrive(frame, at, { scale?, blur?, frames?, fade? })  arrival from oversize: { scale, blur, opacity, shown }
 *                                              (snapSettle 10 f from 1.5x and 6 px blur, 4 f opacity helper)
 *   focusIn(frame, at, { blur?, frames? })     resolve in place: { blur, opacity } (8 -> 0 px over 5 f)
 *   snapTail(frame, at, { from, snap, to, tail?, ease? })  camera snap: one-frame step to `snap` on `at`,
 *                                              then a decelerating tail to `to` (softLand 12 f)
 *   drift(frame, [f0, f1], [v0, v1])           creeps and drifts: sine in-out over the whole span (no linear)
 *   stepCounter(frame, { start, end, from, to, ease? })  an integer stepped once per frame, steps shrinking,
 *                                              landing exactly on `to` at `end`
 *   onTwos(frame)                              the posed clock: holds each pose 2 frames (even frames)
 *   shown(frame, from, to?)                    hard visibility window [from, to)
 *   sway(frame, amp, period, phase?)           living hold (breathing): a sine, amplitude amp, period in frames
 */
import { interpolate } from "remotion";
import { theme } from "../theme";

type Ease = (t: number) => number;
const clampOpts = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

export const tween = (frame: number, [f0, f1]: [number, number], [v0, v1]: [number, number], ease: Ease): number => {
  if (f1 <= f0) return frame < f0 ? v0 : v1;
  return interpolate(frame, [f0, f1], [v0, v1], { ...clampOpts, easing: ease });
};

export type Key = [frame: number, value: number, ease?: Ease];

export const keys = (frame: number, ks: Key[]): number => {
  if (ks.length === 0) throw new Error("keys: empty");
  if (frame <= ks[0][0]) return ks[0][1];
  for (let i = 1; i < ks.length; i++) {
    const [f1, v1, e] = ks[i];
    if (frame <= f1) {
      const [f0, v0] = ks[i - 1];
      return tween(frame, [f0, f1], [v0, v1], e ?? theme.ease.softLand);
    }
  }
  return ks[ks.length - 1][1];
};

export const land = (frame: number, at: number, o: { from: number; to?: number; frames?: number; ease?: Ease }) =>
  tween(frame, [at, at + (o.frames ?? theme.dur.enter)], [o.from, o.to ?? 0], o.ease ?? theme.ease.softLand);

export const leave = (frame: number, at: number, o: { from?: number; to: number; frames?: number; ease?: Ease }) =>
  tween(frame, [at, at + (o.frames ?? theme.dur.exit)], [o.from ?? 0, o.to], o.ease ?? theme.ease.accelExit);

export const arrive = (
  frame: number,
  at: number,
  o: { scale?: number; blur?: number; frames?: number; fade?: number; ease?: Ease } = {},
) => {
  const frames = o.frames ?? 10;
  const ease = o.ease ?? theme.ease.snapSettle;
  return {
    shown: frame >= at,
    scale: tween(frame, [at, at + frames], [o.scale ?? 1.5, 1], ease),
    blur: tween(frame, [at, at + Math.min(frames, 7)], [o.blur ?? 6, 0], ease),
    opacity: tween(frame, [at, at + (o.fade ?? 4)], [0.25, 1], theme.ease.softInOut),
  };
};

export const focusIn = (frame: number, at: number, o: { blur?: number; frames?: number } = {}) => ({
  shown: frame >= at,
  blur: tween(frame, [at, at + (o.frames ?? 5)], [o.blur ?? 8, 0], theme.ease.glide),
  opacity: tween(frame, [at, at + (o.frames ?? 5)], [0.12, 1], theme.ease.softInOut),
});

export const snapTail = (
  frame: number,
  at: number,
  o: { from: number; snap: number; to: number; tail?: number; ease?: Ease },
) => {
  if (frame < at) return o.from;
  return tween(frame, [at, at + (o.tail ?? 12)], [o.snap, o.to], o.ease ?? theme.ease.softLand);
};

export const drift = (frame: number, span: [number, number], values: [number, number]) =>
  tween(frame, span, values, theme.ease.drift);

export const stepCounter = (frame: number, o: { start: number; end: number; from: number; to: number; ease?: Ease }) => {
  if (frame <= o.start) return o.from;
  if (frame >= o.end) return o.to;
  const v = tween(Math.floor(frame), [o.start, o.end], [o.from, o.to], o.ease ?? theme.ease.softLand);
  return Math.round(v);
};

export const onTwos = (frame: number) => Math.floor(frame / 2) * 2;

export const shown = (frame: number, from: number, to = Infinity) => frame >= from && frame < to;

export const sway = (frame: number, amp: number, period: number, phase = 0) =>
  amp * Math.sin(((frame + phase) / period) * Math.PI * 2);
