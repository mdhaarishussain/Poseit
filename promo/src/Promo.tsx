/**
 * Promo.tsx: the film (1080 x 1920, 30 fps, 664 f), assembled from src/scenes/*. Every act receives the GLOBAL
 * frame (beats.ts and events.ts are in global frames); only the act on screen renders.
 *   act I    f0-79     Hook (B1)                 scenes/Hook.tsx
 *   act II   f80-335   StudioShot (B2-B4)        scenes/StudioShot.tsx
 *   act III  f336-553  Viewfinder (B5-B7)        scenes/Viewfinder.tsx   (ends on the app's real capture frame)
 *   act IV   f554-663  TheName (B8)              scenes/TheName.tsx      (begins with the capture flash)
 *   sound    f0-663    Soundtrack                audio/Soundtrack.tsx    (public/audio/mix.wav from scripts/gen-audio.mjs)
 */
import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { theme } from "./theme";
import { ACTS } from "./beats";
import { Hook } from "./scenes/Hook";
import { StudioShot } from "./scenes/StudioShot";
import { Viewfinder } from "./scenes/Viewfinder";
import { TheName } from "./scenes/TheName";
import { Soundtrack } from "./audio/Soundtrack";

export const Promo: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: theme.color.liveView }}>
      {frame < ACTS.II.from && <Hook frame={frame} />}
      {frame >= ACTS.II.from && frame < ACTS.III.from && <StudioShot frame={frame} />}
      {frame >= ACTS.III.from && frame < ACTS.IV.from && <Viewfinder frame={frame} />}
      {frame >= ACTS.IV.from && <TheName frame={frame} />}
      <Soundtrack />
    </AbsoluteFill>
  );
};
