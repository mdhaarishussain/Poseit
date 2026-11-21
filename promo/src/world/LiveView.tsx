/**
 * LiveView.tsx: the camera world (acts I and III): a true-black live view (Studio LiveView #000000) with the
 * camera's sensor noise and the rule-of-thirds grid (Mannequin white at 20%, 2 px).
 *
 *   <LiveView rect? grid? noise? gridOpacity? />
 *     rect   the preview rectangle in composition px (default the whole frame); the grid divides it in thirds
 *     grid   draw the thirds grid (default true); gridWidth px (default 2)
 *     noise  sensor noise amount (SensorNoise opacity; 0 = none)
 * Put it INSIDE the film-camera group (CameraGroup) so the grid scales with a creep or a step (it proves the scale);
 * draw the guide, the skeleton and the HUD above it.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { SensorNoise } from "./Grain";

export const LiveView: React.FC<{
  rect?: { x: number; y: number; w: number; h: number };
  grid?: boolean;
  gridWidth?: number;
  gridColor?: string;
  noise?: number;
  children?: React.ReactNode;
}> = ({ rect, grid = true, gridWidth = 2, gridColor = theme.color.gridLine, noise = 0.35, children }) => {
  const { W, H } = theme.size;
  const r = rect ?? { x: 0, y: 0, w: W, h: H };
  return (
    <AbsoluteFill style={{ background: theme.color.liveView }}>
      {noise > 0 && <SensorNoise amount={noise} />}
      {grid && (
        <svg width={W} height={H} style={{ position: "absolute", inset: 0 }}>
          {[1, 2].map((k) => (
            <g key={k} stroke={gridColor} strokeWidth={gridWidth}>
              <line x1={r.x + (r.w * k) / 3} y1={r.y} x2={r.x + (r.w * k) / 3} y2={r.y + r.h} />
              <line x1={r.x} y1={r.y + (r.h * k) / 3} x2={r.x + r.w} y2={r.y + (r.h * k) / 3} />
            </g>
          ))}
        </svg>
      )}
      {children}
    </AbsoluteFill>
  );
};
