/**
 * Hud.tsx: the camera HUD rebuilt from the app's own code (app/.../ui/camera/CameraHud.kt, CameraOverlay.kt) and
 * tokens, magnified by theme.hud.k so the cue text is 0.04 H (G1). All sizes default to the app's dp x hud.px; pass
 * `scale` (multiplies) to change them together. Real strings only (MatchHints.kt).
 *
 *   <CueCard text kind? x y anchor? scale? />  Sheet card, Medium radius, Archivo Medium Ink text, a signal dot by hint
 *                    kind ("perfect" Match, "pose"/"framing" Near, "visibility" Miss, none = no dot). The dot uses the
 *                    normal (not camera-bright) signal colours, as the app does. Max two lines. x, y = anchor point
 *                    (default the card's top centre).
 *   <ScoreRing score? hold? cx cy scale? />    56 dp badge: 45% black scrim, a full 2 dp ring in the score colour
 *                    (Studio.forScore on dark; white 60% while nobody is detected), a 4 dp hold-progress arc from
 *                    12 o'clock clockwise, the numeral (Expanded Bold, tabular; 15 dp for "100").
 *   <CountdownNumeral value cx cy scale? />    96 sp Expanded Bold, Mannequin fill over an Ink 55% contour (6 dp stroke
 *                    drawn behind), like the lay figures. Animate its arrival with motion.arrive().
 *   <FrameMarks x y w h color? width? length? />  viewfinder corner brackets (DESIGN: 1.5 dp, 12 dp long, Ink on
 *                    paper; white on the camera)
 *   <CaptureFlash opacity />                     the capture flash (white, full frame)
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";

const { hud } = theme;
const px = (dp: number, scale = 1) => dp * hud.px * scale;

export type HintKind = "perfect" | "pose" | "framing" | "visibility" | "none";
const DOT: Record<HintKind, string | null> = {
  perfect: theme.color.signal.match,
  pose: theme.color.signal.near,
  framing: theme.color.signal.near,
  visibility: theme.color.signal.miss,
  none: null,
};

export const CueCard: React.FC<{
  text: string;
  kind?: HintKind;
  x?: number;
  y: number;
  /** which point of the card sits at (x, y) */
  anchor?: "top" | "center" | "bottom";
  scale?: number;
  maxWidth?: number;
  style?: React.CSSProperties;
}> = ({ text, kind = "none", x = theme.size.W / 2, y, anchor = "top", scale = 1, maxWidth = theme.size.W * 0.86, style }) => {
  const dot = DOT[kind];
  const ty = anchor === "top" ? "0%" : anchor === "center" ? "-50%" : "-100%";
  return (
    <div
      style={{
        position: "absolute",
        left: x - maxWidth / 2,
        top: y,
        width: maxWidth,
        display: "flex",
        justifyContent: "center",
        transform: `translateY(${ty})`,
      }}
    >
    <div
      style={{
        maxWidth,
        display: "flex",
        alignItems: "center",
        gap: px(hud.cueGap, scale),
        padding: `${px(hud.cuePadV, scale)}px ${px(hud.cuePadH, scale)}px`,
        background: theme.color.sheet,
        borderRadius: px(hud.cueRadius, scale),
        color: theme.color.ink,
        fontFamily: theme.font.text,
        fontWeight: theme.type.cue.weight,
        fontSize: px(hud.cueText, scale),
        lineHeight: theme.type.cue.lineHeight,
        whiteSpace: "pre-line",
        ...style,
      }}
    >
      {dot && <div style={{ flex: "none", width: px(hud.cueDot, scale), height: px(hud.cueDot, scale), borderRadius: "50%", background: dot }} />}
      <div style={{ display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>{text}</div>
    </div>
    </div>
  );
};

export const ScoreRing: React.FC<{
  /** 0..100, or null while nobody is detected */
  score: number | null;
  /** 0..1 hold progress before auto-capture, or null */
  hold?: number | null;
  cx: number;
  cy: number;
  scale?: number;
  /** override the ring colour (default Studio.forScore(score / 100) on dark) */
  color?: string;
}> = ({ score, hold = null, cx, cy, scale = 1, color }) => {
  const size = px(hud.ringSize, scale);
  const holdW = px(hud.holdStroke, scale);
  const ringW = px(hud.ringStroke, scale);
  const inset = holdW / 2 + px(2, scale);
  const r = size / 2 - inset;
  const ringColor = color ?? (score === null ? theme.color.ringIdle : theme.signal.forScore(score / 100, true));
  const p = hold === null ? 0 : Math.min(1, Math.max(0, hold));
  const arc = () => {
    if (p >= 0.9999) return <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={ringColor} strokeWidth={holdW} />;
    const a = -Math.PI / 2 + p * Math.PI * 2;
    const x0 = size / 2, y0 = size / 2 - r;
    const x1 = size / 2 + r * Math.cos(a), y1 = size / 2 + r * Math.sin(a);
    return <path d={`M${x0} ${y0}A${r} ${r} 0 ${p > 0.5 ? 1 : 0} 1 ${x1} ${y1}`} fill="none" stroke={ringColor} strokeWidth={holdW} strokeLinecap="butt" />;
  };
  const numeral = score === null ? null : Math.round(score);
  return (
    <div style={{ position: "absolute", left: cx - size / 2, top: cy - size / 2, width: size, height: size }}>
      <svg width={size} height={size} style={{ position: "absolute", inset: 0 }}>
        <circle cx={size / 2} cy={size / 2} r={size / 2} fill={hud.scrim} />
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={ringColor} strokeWidth={ringW} />
        {p > 0 && arc()}
      </svg>
      {numeral !== null && (
        <div
          style={{
            position: "absolute",
            inset: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: theme.color.mannequin,
            fontFamily: theme.font.wide,
            fontWeight: 700,
            fontFeatureSettings: theme.font.tnum,
            fontSize: px(numeral >= 100 ? 15 : hud.numeral, scale),
            lineHeight: 1,
          }}
        >
          {numeral}
        </div>
      )}
    </div>
  );
};

export const CountdownNumeral: React.FC<{
  value: number | string;
  cx: number;
  cy: number;
  scale?: number;
  opacity?: number;
  blur?: number;
}> = ({ value, cx, cy, scale = 1, opacity = 1, blur = 0 }) => {
  const size = px(hud.countdown, scale);
  const common: React.CSSProperties = {
    position: "absolute",
    left: 0,
    top: 0,
    transform: "translate(-50%, -50%)",
    fontFamily: theme.font.wide,
    fontWeight: 700,
    fontFeatureSettings: theme.font.tnum,
    fontSize: size,
    lineHeight: 1,
    whiteSpace: "nowrap",
  };
  return (
    <div style={{ position: "absolute", left: cx, top: cy, opacity, filter: blur > 0.05 ? `blur(${blur}px)` : undefined }}>
      <div
        style={{
          ...common,
          color: "transparent",
          WebkitTextStroke: `${px(hud.countdownContour, scale)}px ${theme.color.contour}`,
          opacity: theme.color.contourAlpha,
        }}
      >
        {value}
      </div>
      <div style={{ ...common, color: theme.color.mannequin }}>{value}</div>
    </div>
  );
};

export const FrameMarks: React.FC<{
  x: number;
  y: number;
  w: number;
  h: number;
  color?: string;
  width?: number;
  length?: number;
  opacity?: number;
}> = ({ x, y, w, h, color = theme.color.ink, width = 1.5 * theme.size.dp, length = 12 * theme.size.dp, opacity = 1 }) => {
  const L = length;
  const d = [
    `M${x} ${y + L}V${y}H${x + L}`,
    `M${x + w - L} ${y}H${x + w}V${y + L}`,
    `M${x + w} ${y + h - L}V${y + h}H${x + w - L}`,
    `M${x + L} ${y + h}H${x}V${y + h - L}`,
  ].join("");
  return (
    <svg width={theme.size.W} height={theme.size.H} style={{ position: "absolute", inset: 0, overflow: "visible" }}>
      <path d={d} fill="none" stroke={color} strokeWidth={width} strokeLinecap="butt" strokeLinejoin="miter" opacity={opacity} />
    </svg>
  );
};

export const CaptureFlash: React.FC<{ opacity: number }> = ({ opacity }) =>
  opacity <= 0 ? null : <AbsoluteFill style={{ background: theme.color.flash, opacity }} />;
