/**
 * People.tsx: act II, B2-B3 (f80-271): real people, with the app's graphics on them (STORYBOARD v2 B2, B3).
 *
 *   <People frame={global frame} />
 *   B2  f80-143   the first real person (plate b2-alive, printed in the Studio ramp). The plate arrives from 1.06
 *                 (landing by f88: the hook's rush ends on her) and creeps to 1.02. On f80 the hook's stiff stick
 *                 skeleton stands exactly on her body; over f80-86 it becomes HER skeleton, the app's live skeleton
 *                 from the app's own model and smoothing (useLandmarks), and then follows her as she lifts both
 *                 hands behind her head, landing on the f144 downbeat.
 *   B3  f144-271  the chorus: a hard cut on every grid line (beats f144-192, eighths f208-232, f240 holds through the
 *                 drop f248-272). On each cut the white see-through lay figure the app's matcher found for that
 *                 person (people/data.ts CHORUS_SHOTS, from chorus.json) lands on them from 1.25x about their
 *                 mid-hip (softLand 5 f on beats, 4 f on eighths): a veil settling 0.70 -> 0.34 and, a frame later,
 *                 a white edge closing from 1.32x (theme.footage.chorusVeil), then stays put as the person moves
 *                 under it, as the app's guide does. The film camera pushes 1.00 -> 1.08
 *                 across every shot (one curve, so its velocity carries over the cuts), then only drifts in the drop.
 * Every overlay is drawn in plate pixels inside <Footage>, through the same transform as the video, so it stays on the
 * person under every push. No names, numbers or colour in this act: the figures are white.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import { blendSkeletons, mapJoints, mirrored } from "../figure/interpolate";
import { estimatedUnit } from "../figure/shapes";
import type { Skeleton, Vec } from "../figure/types";
import { Footage } from "../footage/Footage";
import { useLandmarks } from "../footage/hooks";
import { FilmGrain } from "../world/Grain";
import { tween } from "../motion";
import { hookSkeleton } from "./Hook";
import { HOOK } from "./shots";
import { PEOPLE_TIMING } from "./timing";
import { B2, chorusScale, chorusShotAt, guideLanding, type ChorusShot } from "./people/data";

const { W, H } = theme.size;
const E = theme.ease;
const SCENE = W / H;

/** The hook's skeleton on its last frame (f79), in B2's scene space on f80 (the plate at 1.06 about her mid-hip). */
const STIFF_AT_CUT: Skeleton = (() => {
  const f = PEOPLE_TIMING.cut - 1;
  const cam = HOOK.cam(f);
  // the hook draws flipForDisplay(stiff); mirrored(stiff) is the same picture with the labels on her sides (the
  // subject's left on the image right, as in her scene skeleton), so the morph never crosses the limbs
  const s = mirrored(hookSkeleton(f));
  const fr = HOOK.frame;
  const [ax, ay] = B2.anchor;
  const toScene = (p: Vec): Vec => {
    const sx = cam.s * (fr.x + fr.width * p[0]) + cam.x;
    const sy = cam.s * (fr.y + fr.height * p[1]) + cam.y;
    return [ax + (sx / W - ax) / B2.landFrom, ay + (sy / H - ay) / B2.landFrom];
  };
  const out = mapJoints(s, toScene);
  // body unit: frame heights -> scene heights (the frame is cam.s * fr.height px tall on screen, 1.06x the plate)
  return { ...out, unit: (s.unit * cam.s * fr.height) / H / B2.landFrom };
})();

const liveStroke = theme.hud.liveStroke * theme.hud.px;

const B2Skeleton: React.FC<{ frame: number }> = ({ frame }) => {
  const lm = useLandmarks(B2.plate, frame);
  const hers = lm?.drawn[0];
  if (!hers) return null;
  const her = { ...hers, unit: estimatedUnit(hers, SCENE) };
  const t = tween(frame, B2.morph, [0, 1], E.softLand);
  const s = t >= 1 ? her : blendSkeletons({ ...STIFF_AT_CUT, visibility: undefined }, her, t, { aspect: SCENE });
  return (
    <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ position: "absolute", left: 0, top: 0 }}>
      <FigureG
        skeleton={s}
        aspect={SCENE}
        height={H}
        style="stick"
        fill={theme.color.mannequin}
        lineAlpha={theme.hud.liveAlpha}
        lineWidth={liveStroke}
        showFace={false}
      />
    </svg>
  );
};

const B2Alive: React.FC<{ frame: number }> = ({ frame }) => (
  <Footage id={B2.plate} frame={frame} transform={{ scale: B2.scale(frame), anchor: B2.anchor, dx: 0, dy: 0 }}>
    <B2Skeleton frame={frame} />
  </Footage>
);

/**
 * The chorus guide: the app's lay figure (its geometry and flip), printed so the person stays readable through it:
 * a Mannequin veil (silhouette, opacity settling from the landing value to the chorus value) under a crisp edge
 * (the figure's own contour shape, outline style). No face dot: on a real face it reads as a mark on the person.
 */
const ChorusGuide: React.FC<{ frame: number; shot: ChorusShot }> = ({ frame, shot }) => {
  const land = guideLanding(frame, shot);
  const [ax, ay] = [shot.anchor[0] * W, shot.anchor[1] * H];
  return (
    <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ position: "absolute", left: 0, top: 0, overflow: "visible" }}>
      <g transform={`translate(${ax} ${ay}) scale(${land.scale}) translate(${-ax} ${-ay})`}>
        {shot.guide.map((g, i) => (
          <FigureG key={i} skeleton={g} aspect={SCENE} height={H} style="silhouette" fill={theme.color.mannequin} contour={null} showFace={false} opacity={land.alpha} />
        ))}
      </g>
      <g transform={`translate(${ax} ${ay}) scale(${land.edgeScale}) translate(${-ax} ${-ay})`}>
        {shot.guide.map((g, i) => (
          <FigureG key={i} skeleton={g} aspect={SCENE} height={H} style="outline" fill={theme.color.mannequin} showFace={false} contourWidth={2 * theme.size.dp} opacity={land.edgeAlpha} />
        ))}
      </g>
    </svg>
  );
};

const Chorus: React.FC<{ frame: number }> = ({ frame }) => {
  const shot = chorusShotAt(frame);
  return (
    <Footage id={shot.plate} frame={frame + shot.offset} transform={{ scale: chorusScale(frame), anchor: shot.anchor, dx: 0, dy: 0 }}>
      <ChorusGuide frame={frame} shot={shot} />
    </Footage>
  );
};

export const People: React.FC<{ frame: number }> = ({ frame }) => (
  <AbsoluteFill style={{ background: theme.color.ink }}>
    {frame < PEOPLE_TIMING.chorus ? <B2Alive frame={frame} /> : <Chorus frame={frame} />}
    <FilmGrain />
  </AbsoluteFill>
);
