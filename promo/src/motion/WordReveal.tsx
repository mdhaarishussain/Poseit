/**
 * WordReveal.tsx: whole words at speech rhythm (motionmaxxing type rules: whole words 2-12 f apart, never letters).
 *
 *   <WordReveal lines={[[{ text: "What", at: 2 }, { text: "do", at: 6 }], [...]]} mode="rise" style={...} />
 *   words(text, start, gaps)   -> [{ text, at }] from a sentence: gaps[i] = frames between word i and i + 1
 *                                 (a single number = an even rhythm)
 * Modes:
 *   "rise"   each word rises whole from `dist` (default 0.03 H) below its slot: softLand `frames` (10 f), with a
 *            4 f opacity helper (storyboard B1)
 *   "focus"  each word resolves in place: blur 8 -> 0 px and opacity 0.12 -> 1 over 5 f (storyboard B8 end line)
 *   "cut"    each word appears whole on its frame (a system event)
 * Lines are centred (statements paced to speech) unless `align` says otherwise; words before their onset keep
 * their space (no re-centring jumps). Line height and spacing are px-based (never em gaps around big type).
 */
import React from "react";
import { useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { focusIn, tween } from "./index";

export type Word = { text: string; at: number };

export const words = (text: string, start: number, gaps: number | number[]): Word[] => {
  let at = start;
  return text.split(/\s+/).filter(Boolean).map((t, i) => {
    const w = { text: t, at };
    at += typeof gaps === "number" ? gaps : (gaps[i] ?? gaps[gaps.length - 1] ?? 4);
    return w;
  });
};

export const WordReveal: React.FC<{
  lines: Word[][];
  mode?: "rise" | "focus" | "cut";
  dist?: number;
  frames?: number;
  align?: "center" | "left";
  lineGap?: number;
  wordGap?: number;
  style?: React.CSSProperties;
}> = ({ lines, mode = "rise", dist = 0.03 * theme.size.H, frames = 10, align = "center", lineGap, wordGap, style }) => {
  const frame = useCurrentFrame();
  const fontSize = typeof style?.fontSize === "number" ? style.fontSize : 64;
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: align === "center" ? "center" : "flex-start", gap: lineGap ?? 0, ...style }}>
      {lines.map((line, li) => (
        <div key={li} style={{ display: "flex", gap: wordGap ?? Math.round(fontSize * 0.26), whiteSpace: "nowrap" }}>
          {line.map((w, wi) => {
            let s: React.CSSProperties;
            if (mode === "rise") {
              s = {
                opacity: frame < w.at ? 0 : tween(frame, [w.at, w.at + 4], [0.2, 1], theme.ease.softInOut),
                transform: `translateY(${tween(frame, [w.at, w.at + frames], [dist, 0], theme.ease.softLand)}px)`,
              };
            } else if (mode === "focus") {
              const f = focusIn(frame, w.at);
              s = { opacity: f.shown ? f.opacity : 0, filter: f.blur > 0.05 ? `blur(${f.blur}px)` : undefined };
            } else {
              s = { opacity: frame < w.at ? 0 : 1 };
            }
            return (
              <span key={wi} style={{ display: "inline-block", ...s }}>
                {w.text}
              </span>
            );
          })}
        </div>
      ))}
    </div>
  );
};
