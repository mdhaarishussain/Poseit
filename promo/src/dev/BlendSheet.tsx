/**
 * BlendSheet.tsx (dev still): skeleton blends at t = 0, .25, .5, .75, 1.
 *   row 1  promo_stiff -> fun_disco_point, chain blend (limbs rotate at full length)
 *   row 2  the same with mode "linear" (raw joint lerp: limbs shorten mid-blend), for comparison
 *   row 3  mirrored(fun_disco_point) -> stand_hand_on_hip, chain, on the posed clock (the B8 relax). Blend the
 *          MIRRORED Disco point (labels swapped, drawn the same as flipForDisplay) so left/right labels sit on the
 *          same screen side as the launcher pose and nothing crosses over.
 *   row 4  stick style (the live skeleton): promo_stiff -> promo_stiff_flick
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import { blendSkeletons, mirrored } from "../figure/interpolate";
import { illustrative, poseById, skeletonOf } from "../figure/poses";
import type { Skeleton } from "../figure/types";

const T = [0, 0.25, 0.5, 0.75, 1];

export const BlendSheet: React.FC = () => {
  const stiff = skeletonOf(illustrative("promo_stiff"));
  const flick = skeletonOf(illustrative("promo_stiff_flick"));
  const disco = skeletonOf(poseById("fun_disco_point"));
  const hip = skeletonOf(poseById("stand_hand_on_hip"));
  const rows: { a: Skeleton; b: Skeleton; mode?: "linear"; stick?: boolean }[] = [
    { a: stiff, b: disco },
    { a: stiff, b: disco, mode: "linear" },
    { a: mirrored(disco), b: hip },
    { a: stiff, b: flick, stick: true },
  ];
  const cellH = 440;
  const cellW = theme.size.W / 5;
  return (
    <AbsoluteFill style={{ background: theme.color.paper }}>
      <svg width={theme.size.W} height={theme.size.H} style={{ position: "absolute", inset: 0 }}>
        {rows.map((r, ri) => (
          <g key={ri}>
            {ri === 3 && <rect x={0} y={60 + ri * (cellH + 20)} width={theme.size.W} height={cellH} fill={theme.color.liveView} />}
            {T.map((t, ti) => (
              <FigureG
                key={ti}
                skeleton={blendSkeletons(r.a, r.b, t, { aspect: 0.75, mode: r.mode })}
                aspect={0.75}
                x={ti * cellW + (cellW - cellH * 0.75) / 2}
                y={60 + ri * (cellH + 20)}
                height={cellH}
                style={r.stick ? "stick" : "silhouette"}
                lineAlpha={r.stick ? 0.7 : 1}
              />
            ))}
          </g>
        ))}
      </svg>
    </AbsoluteFill>
  );
};
