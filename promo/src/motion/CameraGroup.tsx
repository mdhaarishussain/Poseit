/**
 * CameraGroup.tsx: ONE transform for everything the film camera sees (motionmaxxing R6.1: one group reads as a lens,
 * many tweens read as animation). Put the world and its objects inside; keep screen-fixed things (HUD that must not
 * zoom, the grain) outside.
 *
 *   <CameraGroup scale={1.04} origin={[540, 1100]} x={0} y={0}>...</CameraGroup>
 *     scale   zoom about `origin` (composition px; default the frame centre)
 *     x, y    screen-space offset applied after the zoom (px)
 *   cameraPoint(p, cam)   where a composition point lands on screen under the same transform (for match cuts:
 *                         solve the next shot's framing from the last frame of this one)
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";

export type Cam2D = { scale?: number; origin?: [number, number]; x?: number; y?: number };

export const cameraPoint = (p: [number, number], cam: Cam2D): [number, number] => {
  const s = cam.scale ?? 1;
  const [ox, oy] = cam.origin ?? [theme.size.W / 2, theme.size.H / 2];
  return [ox + (p[0] - ox) * s + (cam.x ?? 0), oy + (p[1] - oy) * s + (cam.y ?? 0)];
};

export const CameraGroup: React.FC<Cam2D & { children: React.ReactNode; style?: React.CSSProperties }> = ({
  scale = 1,
  origin,
  x = 0,
  y = 0,
  children,
  style,
}) => {
  const [ox, oy] = origin ?? [theme.size.W / 2, theme.size.H / 2];
  return (
    <AbsoluteFill
      style={{
        transformOrigin: `${ox}px ${oy}px`,
        transform: `translate(${x}px, ${y}px) scale(${scale})`,
        ...style,
      }}
    >
      {children}
    </AbsoluteFill>
  );
};
