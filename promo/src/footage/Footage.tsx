/**
 * Footage.tsx: one prepared stock plate, on the film's clock, optionally printed in the Studio ramp, with overlays
 * that stay on the person.
 *
 *   <Footage id="c1" frame={frame} push={...}>{({ px }) => <svg>...</svg>}</Footage>
 *
 * Props:
 *   id         plate id (public/footage/plates.json: a manifest shot, "b5-live.1x", "c9-alt2" ...)
 *   frame      GLOBAL film frame (default: the current frame + offset); the plate frame is frame - filmFrom
 *   offset     the global frame at which an enclosing <Sequence> starts (only if you nest it in one; default 0)
 *   look       "duotone" (the Studio ramp, default), "colour" (the plate as it is: the captured photo), "none" alias
 *   trim       the ramp's midtone lift for this plate (default: the manifest shot's grade.trim)
 *   transform  FootageTransform (scale about a scene anchor, then a px shift); or `push` for an eased push
 *   freezeAt   GLOBAL frame to hold from then on (the photo the app takes)
 *   rate       playback speed from `start` (GLOBAL frame) on, default 1 (plates are already in film time, with any
 *              time remap baked in; a rate is only for a builder's own retime; landmarks follow it, verdicts do not)
 *   children   overlays drawn in PLATE PIXELS (0..W, 0..H = scene x * W, y * H) through the same transform as the
 *              video, so they stick to the person; a function child gets { n, px, transform } (px = plate px per
 *              scene unit, [W, H])
 *
 * The video is an OffthreadVideo showing the computed plate frame (plates.ts plateFrameFor: frozen at 0, trimmed to
 * the frame), so the frame on screen, useLandmarks (pass the same options) and useFootageMatch always agree (plates are already in film time; a time remap is baked into the plate). Muted: the
 * film's sound is its own.
 */
import React, { useId } from "react";
import { AbsoluteFill, Freeze, OffthreadVideo, staticFile, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { shotById } from "./manifest";
import { plateById, plateFile, plateFrameFor } from "./plates";
import { RampFilter, type Look } from "./grade";
import { cssMatrix, IDENTITY_TRANSFORM, pushAt, type FootageTransform, type Push } from "./transform";

export type FootageChildContext = { n: number; px: [number, number]; transform: FootageTransform };

export type FootageProps = {
  id: string;
  frame?: number;
  offset?: number;
  look?: Look | "none";
  trim?: number;
  transform?: FootageTransform;
  push?: Push;
  freezeAt?: number;
  rate?: number;
  start?: number;
  style?: React.CSSProperties;
  children?: React.ReactNode | ((ctx: FootageChildContext) => React.ReactNode);
};

/** The transform a Footage with these props uses on global frame f (for drawing outside it with sceneToComp). */
export const footageTransformAt = (f: number, props: Pick<FootageProps, "transform" | "push">): FootageTransform =>
  props.transform ?? (props.push ? pushAt(f, props.push) : IDENTITY_TRANSFORM);

export const Footage: React.FC<FootageProps> = ({ id, frame, offset = 0, look, trim, transform, push, freezeAt, rate, start, style, children }) => {
  const current = useCurrentFrame();
  const f = frame ?? current + offset;
  const { W, H } = theme.size;
  const plate = plateById(id);
  const grade = shotById(plate.alternateOf ?? plate.shot).grade;
  const lookUsed: Look = look === "colour" || look === "none" ? "colour" : "duotone";
  const trimUsed = trim ?? grade.trim ?? 0;
  const n = plateFrameFor(id, f, { freezeAt, rate, start });
  const t = footageTransformAt(f, { transform, push });
  const filterId = `ramp-${useId().replace(/[^a-zA-Z0-9_-]/g, "")}`;
  const ctx: FootageChildContext = { n, px: [W, H], transform: t };
  return (
    <AbsoluteFill style={{ overflow: "hidden", ...style }}>
      {lookUsed === "duotone" && <RampFilter id={filterId} trim={trimUsed} />}
      <div style={{ position: "absolute", left: 0, top: 0, width: W, height: H, transformOrigin: "0 0", transform: cssMatrix(t, W, H) }}>
        <div style={{ position: "absolute", inset: 0, filter: lookUsed === "duotone" ? `url(#${filterId})` : undefined }}>
          {/* Freeze at 0 and trim to n, not Freeze at n: Remotion clamps a frozen frame to the composition's length */}
          <Freeze frame={0}>
            <OffthreadVideo src={staticFile(plateFile(id))} trimBefore={n} muted style={{ width: W, height: H, display: "block" }} />
          </Freeze>
        </div>
        {typeof children === "function" ? children(ctx) : children}
      </div>
    </AbsoluteFill>
  );
};
