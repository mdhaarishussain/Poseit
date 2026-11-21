/**
 * Hook.tsx: act I, B1 (f0-79), "What do I do with my hands?" (STORYBOARD B1).
 *
 *   <Hook frame={global frame} />
 * The camera's live view (black, thirds grid, sensor noise) with nobody's pose picked yet: only the live stick
 * skeleton (the app's paint), standing stiff, arms dead straight (the illustrative promo_stiff stance). It fidgets
 * (weight shifts, the head turns one way then the other), and on "hands?" the wrists flick up to the waist and flop
 * back (promo_stiff_flick): the body acts out the question. Then the awkward pause. The film camera pushes
 * 1.00 -> 1.04 (shots.ts HOOK). The question rises a word at a time at speech rhythm (WordReveal "rise"), screen
 * fixed, white, in the top of the safe zone.
 *
 * Seam: on f80 the studio's hero lay figure takes this skeleton's exact frame (studio/camera.ts solves its zoom from
 * hookScreenFrame), so the fidget is back to neutral at f80 and the head is passing through centre (mid-turn).
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { HOOK_WORDS } from "../beats";
import { illustrative, skeletonOf } from "../figure/poses";
import { FigureG } from "../figure/Mannequin";
import { blendSkeletons, flipForDisplay, mapJoints } from "../figure/interpolate";
import type { JointName, Skeleton, Vec } from "../figure/types";
import { LiveView } from "../world/LiveView";
import { CameraGroup } from "../motion/CameraGroup";
import { WordReveal } from "../motion/WordReveal";
import { keys, sway, tween } from "../motion";
import { HOOK } from "./shots";
import { HOOK_TIMING } from "./timing";

const { W, H } = theme.size;
const E = theme.ease;
const STIFF = illustrative("promo_stiff");
const FLICK = illustrative("promo_stiff_flick");
const A = STIFF.frameAspect;
const FACE: JointName[] = ["NOSE", "LEFT_EYE", "RIGHT_EYE", "LEFT_EAR", "RIGHT_EAR"];


const rotate = (s: Skeleton, deg: number, c: Vec): Skeleton => {
  const r = (deg * Math.PI) / 180, cs = Math.cos(r), sn = Math.sin(r);
  return mapJoints(s, (p) => {
    const dx = (p[0] - c[0]) * A, dy = p[1] - c[1];
    return [c[0] + (dx * cs - dy * sn) / A, c[1] + dx * sn + dy * cs];
  });
};

/** The stiff body at frame f (frame coordinates of promo_stiff, photo orientation). */
export const hookSkeleton = (f: number): Skeleton => {
  const T = HOOK_TIMING;
  const t = f < T.flop[0] ? tween(f, T.flickUp, [0, 1], E.softLand) : tween(f, T.flop, [1, 0], E.gentleIn);
  let s = blendSkeletons(skeletonOf(STIFF), skeletonOf(FLICK), t, { aspect: A });
  // weight shifts: a slow rock about the ankles, back to neutral by the cut
  const rock = keys(f, [[-8, 0], [10, 1.3, E.softInOut], [30, -0.9, E.softInOut], [52, 0.8, E.softInOut], [80, 0, E.softInOut]]);
  const la = s.joints.LEFT_ANKLE!, ra = s.joints.RIGHT_ANKLE!;
  s = rotate(s, rock, [(la[0] + ra[0]) / 2, (la[1] + ra[1]) / 2]);
  // the head turns one way, then the other (still turning through centre at the cut)
  const turn = keys(f, [[-6, 0.1], [16, -0.11, E.softInOut], [26, -0.11], [44, 0.06, E.softInOut], [62, 0.13, E.softInOut], [96, -0.13, E.softInOut]]);
  const u = STIFF.figures[0].unit;
  s = mapJoints(s, (p, j) => (FACE.includes(j) ? [p[0] + (turn * u) / A, p[1]] : p));
  // breathing in the shoulders
  const b = sway(f, 0.006 * u, 46);
  return mapJoints(s, (p, j) => (j.endsWith("SHOULDER") || FACE.includes(j) || j.includes("ELBOW") || j.includes("WRIST") || j.includes("HAND") ? [p[0], p[1] + b] : p));
};

export const Hook: React.FC<{ frame: number }> = ({ frame }) => {
  const type = theme.type.hook;
  // baselines at y 0.205 and 0.262: the line box top sits a cap-ascent above the first baseline
  const lineGap = (0.262 - 0.205) * H - type.size;
  return (
    <AbsoluteFill>
      <CameraGroup scale={HOOK.scale(frame)} origin={HOOK.origin}>
        <LiveView />
        <svg width={W} height={H} style={{ position: "absolute", inset: 0 }}>
          <FigureG
            skeleton={flipForDisplay(hookSkeleton(frame))}
            aspect={A}
            x={HOOK.frame.x}
            y={HOOK.frame.y}
            height={HOOK.frame.height}
            style="stick"
            fill={theme.color.mannequin}
            lineAlpha={theme.hud.liveAlpha}
            lineWidth={theme.hud.liveStroke * theme.hud.px}
            showFace={false}
          />
        </svg>
      </CameraGroup>
      <div style={{ position: "absolute", left: 0, right: 0, top: 0.205 * H - type.size * 0.92 }}>
        <WordReveal
          lines={HOOK_WORDS}
          mode="rise"
          lineGap={lineGap}
          style={{
            fontFamily: type.family,
            fontWeight: type.weight,
            fontSize: type.size,
            letterSpacing: type.tracking,
            lineHeight: 1,
            color: theme.color.mannequin,
          }}
        />
      </div>
    </AbsoluteFill>
  );
};
