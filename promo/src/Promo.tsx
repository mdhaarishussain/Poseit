/**
 * Promo.tsx: the film (1080 x 1920, 30 fps, 664 f). SCAFFOLD: each act renders its world from the foundation so the
 * timeline, worlds and type can be checked end to end; the scene builder replaces each act with src/scenes/*.
 * Acts and beats come from beats.ts (STORYBOARD.md section 7).
 */
import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { theme } from "./theme";
import { ACTS, HOOK_WORDS } from "./beats";
import { LiveView } from "./world/LiveView";
import { StudioPaper } from "./world/StudioPaper";
import { StudioFigures } from "./world/StudioFigures";
import { FilmGrain } from "./world/Grain";
import { WIDE_CAM, LATTICE, slotPosition } from "./world/camera";
import { FigureG } from "./figure/Mannequin";
import { illustrative, poseInkBounds, skeletonOf } from "./figure/poses";
import { flipForDisplay } from "./figure/interpolate";
import { WordReveal } from "./motion/WordReveal";
import { CameraGroup } from "./motion/CameraGroup";
import { drift } from "./motion";

const { W, H } = theme.size;

const ActIScaffold: React.FC = () => {
  const frame = useCurrentFrame();
  const stiff = illustrative("promo_stiff");
  const ink = poseInkBounds(stiff);
  // storyboard B1: head top y 0.33, feet y 0.93, centred
  const fh = (0.6 * H) / (ink.maxY - ink.minY);
  const fx = W / 2 - 0.5 * fh * stiff.frameAspect;
  const fy = 0.33 * H - ink.minY * fh;
  return (
    <AbsoluteFill>
      <CameraGroup scale={drift(frame, [0, 80], [1, 1.04])} origin={[W / 2, 0.6 * H]}>
        <LiveView />
        <svg width={W} height={H} style={{ position: "absolute", inset: 0 }}>
          <FigureG skeleton={flipForDisplay(skeletonOf(stiff))} aspect={stiff.frameAspect} x={fx} y={fy} height={fh} style="stick" lineAlpha={0.7} lineWidth={2.5 * theme.hud.px} />
        </svg>
      </CameraGroup>
      <div style={{ position: "absolute", left: 0, right: 0, top: 0.205 * H - theme.type.hook.size * 0.9 }}>
        <WordReveal
          lines={HOOK_WORDS}
          mode="rise"
          lineGap={0.262 * H - 0.205 * H - theme.type.hook.size}
          style={{
            fontFamily: theme.type.hook.family,
            fontWeight: theme.type.hook.weight,
            fontSize: theme.type.hook.size,
            letterSpacing: theme.type.hook.tracking,
            lineHeight: 1,
            color: theme.color.mannequin,
          }}
        />
      </div>
    </AbsoluteFill>
  );
};

const StudioScaffold: React.FC = () => {
  const frame = useCurrentFrame();
  const stiff = illustrative("promo_stiff");
  const cam = { ...WIDE_CAM, z: drift(frame, [0, 256], [0, 0.6]) };
  const items = [];
  for (let r = 0; r < LATTICE.rows; r++) for (let c = 0; c < LATTICE.cols; c++) {
    const [x, z] = slotPosition(r, c);
    items.push({ pose: stiff, x, z, key: `${r}-${c}`, flipX: true });
  }
  return (
    <AbsoluteFill style={{ background: theme.color.paper }}>
      <StudioPaper cam={cam} />
      <StudioFigures cam={cam} items={items} />
      <FilmGrain />
    </AbsoluteFill>
  );
};

export const Promo: React.FC = () => (
  <AbsoluteFill style={{ background: theme.color.liveView }}>
    <Sequence from={ACTS.I.from} durationInFrames={ACTS.I.to - ACTS.I.from}><ActIScaffold /></Sequence>
    <Sequence from={ACTS.II.from} durationInFrames={ACTS.II.to - ACTS.II.from}><StudioScaffold /></Sequence>
    <Sequence from={ACTS.III.from} durationInFrames={ACTS.III.to - ACTS.III.from}><LiveView /></Sequence>
    <Sequence from={ACTS.IV.from} durationInFrames={ACTS.IV.to - ACTS.IV.from}>
      <AbsoluteFill><StudioPaper cam={WIDE_CAM} /><FilmGrain /></AbsoluteFill>
    </Sequence>
  </AbsoluteFill>
);
