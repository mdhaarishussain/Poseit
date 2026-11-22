/**
 * Promo.tsx: the film (1080 x 1920, 30 fps, 664 f), assembled from src/scenes/*. Every act receives the GLOBAL
 * frame (beats.ts and events.ts are in global frames); only the act on screen renders. v2 acts (beats.ts ACTS):
 *   act I    f0-79     Hook (B1)                       scenes/Hook.tsx
 *   act II   f80-271   People (B2-B3)                  scenes/People.tsx (real footage, the app's skeleton and guides)
 *   act III  f272-335  StudioShot (B4, the pick)       scenes/StudioShot.tsx
 *   act IV   f336-554  Viewfinder (B5-B7)              scenes/Viewfinder.tsx (live view, capture, colour photo)
 *   act V    f555-663  TheName (B8)                    scenes/TheName.tsx
 *   sound    f0-663    Soundtrack                      audio/Soundtrack.tsx (public/audio/mix.wav from scripts/gen-audio.mjs)
 */
import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { theme } from "./theme";
import { ACTS } from "./beats";
import { Hook } from "./scenes/Hook";
import { People } from "./scenes/People";
import { StudioShot } from "./scenes/StudioShot";
import { Viewfinder } from "./scenes/Viewfinder";
import { TheName } from "./scenes/TheName";
import { Soundtrack } from "./audio/Soundtrack";

export const Promo: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: theme.color.liveView }}>
      {frame < ACTS.II.from && <Hook frame={frame} />}
      {frame >= ACTS.II.from && frame < ACTS.III.from && <People frame={frame} />}
      {frame >= ACTS.III.from && frame < ACTS.IV.from && <StudioShot frame={frame} />}
      {frame >= ACTS.IV.from && frame < ACTS.V.from && <Viewfinder frame={frame} />}
      {frame >= ACTS.V.from && <TheName frame={frame} />}
      <Soundtrack />
    </AbsoluteFill>
  );
};
