/**
 * FigureSheet.tsx (dev still): 12 real poses drawn with the TS mannequin port, as library tiles (Set tile, contact
 * shadow, name in Archivo Medium, category in Graphite), to compare against app/build/screenshots/library.png.
 * Also proves the fonts: the title row uses Archivo Expanded SemiBold and tabular numerals.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import { CATEGORY_LABEL, poseById } from "../figure/poses";
import { ContactShadow, ShadowDefs, contactShadowForFrame } from "../world/ContactShadow";

const IDS = [
  "stand_hand_on_hip", "sit_crossed_legs", "lean_shoulder_wall", "walk_look_back",
  "floor_side_lie", "port_chin_rest", "selfie_peace", "pro_arms_crossed",
  "fun_disco_point", "fun_star_jump", "couple_back_hug", "group_v_formation",
];

export const FigureSheet: React.FC = () => {
  const { W } = theme.size;
  const gutter = 20 * theme.size.dp;
  const gap = 12 * theme.size.dp;
  const cols = 4;
  const tileW = (W - 2 * gutter - (cols - 1) * gap) / cols;
  const tileH = tileW / 0.75;
  const rowH = tileH + 150;
  const top = 180;
  return (
    <AbsoluteFill style={{ background: theme.color.paper, fontFamily: theme.font.text }}>
      <div style={{ position: "absolute", left: gutter, top: 60, fontFamily: theme.font.wide, fontWeight: 600, fontSize: 64, color: theme.color.ink, letterSpacing: "-0.01em" }}>
        Poseit <span style={{ fontFeatureSettings: theme.font.tnum, fontWeight: 700 }}>93</span>
      </div>
      <svg width={W} height={theme.size.H} style={{ position: "absolute", inset: 0 }}>
        <ShadowDefs id="cs" />
        {IDS.map((id, i) => {
          const pose = poseById(id);
          const col = i % cols, row = Math.floor(i / cols);
          const x = gutter + col * (tileW + gap), y = top + row * rowH;
          // the frame is letterboxed inside the 3:4 tile like PoseThumbnail
          const fh = pose.frameAspect >= 0.75 ? tileW / pose.frameAspect : tileH;
          const fw = fh * pose.frameAspect;
          const fx = x + (tileW - fw) / 2, fy = y + (tileH - fh) / 2;
          return (
            <g key={id}>
              <rect x={x} y={y} width={tileW} height={tileH} rx={12 * theme.size.dp} fill={theme.color.set} />
              <clipPath id={`clip${i}`}><rect x={fx} y={fy} width={fw} height={fh} /></clipPath>
              <g clipPath={`url(#clip${i})`}>
                {pose.figures.map((f, k) => {
                  const s = contactShadowForFrame(f.joints, { x: fx, y: fy, w: fw, h: fh });
                  return s ? <ContactShadow key={`s${k}`} {...s} gradientId="cs" /> : null;
                })}
                {pose.figures.map((f, k) => (
                  <FigureG key={k} skeleton={f} aspect={pose.frameAspect} x={fx} y={fy} height={fh} fill={theme.color.personTones[k % 4]} />
                ))}
              </g>
            </g>
          );
        })}
      </svg>
      {IDS.map((id, i) => {
        const pose = poseById(id);
        const col = i % cols, row = Math.floor(i / cols);
        return (
          <div key={id} style={{ position: "absolute", left: gutter + col * (tileW + gap), top: top + row * rowH + tileH + 14, width: tileW }}>
            <div style={{ fontWeight: 500, fontSize: 13 * theme.size.dp, color: theme.color.ink, lineHeight: 1.3 }}>{pose.name}</div>
            <div style={{ fontWeight: 500, fontSize: 12 * theme.size.dp, color: theme.color.graphite }}>{CATEGORY_LABEL[pose.category]}</div>
          </div>
        );
      })}
    </AbsoluteFill>
  );
};
