/**
 * Viewfinder.tsx: act III, B5-B7 (f336 to the capture), the camera screen (STORYBOARD B5, B6, B7).
 *
 *   <Viewfinder frame={global frame} />
 * The live view (black, thirds grid, sensor noise) under the film camera (shots.ts vfScale): the see-through Disco
 * point guide (Mannequin at the app's default 55% overlay opacity, per-limb colours once a person is assigned) and
 * the live stick skeleton (the app's paint: Mannequin lines at 70%, 2.5 dp, visible joints only). The HUD is screen
 * fixed: the score ring, the hint card and the countdown, rebuilt from CameraHud.kt and magnified (theme.hud).
 *
 * Every number, colour and word comes from public/match.json (the app's PoseMatcher, HintStabilizer and
 * CaptureCoach run over the performer, see viewfinder/match.ts). The HUD's motion is the app's: hint cards
 * crossfade (180 ms in, 120 ms out), the banner is removed when the countdown starts, each digit lands from 115%
 * (200 ms) with a 150 ms fade while the last one fades in 100 ms.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { illustrative, poseById } from "../figure/poses";
import { FigureG } from "../figure/Mannequin";
import { flipForDisplay } from "../figure/interpolate";
import { LiveView } from "../world/LiveView";
import { CameraGroup } from "../motion/CameraGroup";
import { CountdownNumeral, CueCard, ScoreRing, type HintKind as CardKind } from "../ui/Hud";
import { tween } from "../motion";
import { SCENE_ASPECT, guideScene, visibleOnly } from "./viewfinder/rig";
import { performerAt } from "./viewfinder/performer";
import { MATCH_EVENTS, matchAt, partColorsAt, type HintKind } from "./viewfinder/match";
import { VF, vfScale } from "./shots";

const { W, H, fps } = theme.size;
const E = theme.ease;
const POSES = { disco: poseById("fun_disco_point"), stiff: illustrative("promo_stiff") };
const GUIDE = flipForDisplay(guideScene(POSES.disco));
const ms = (v: number) => (v * fps) / 1000;

/** HUD layout (STORYBOARD section 9, safe zone 0.13-0.80 H): card on top, ring at the right below it. */
export const HUD_LAYOUT = {
  cardTop: 0.13 * H,
  cardMaxWidth: 0.84 * W,
  ring: [0.83 * W, 0.4 * H] as [number, number],
  countdown: [0.5 * W, 0.5 * H] as [number, number], // the preview's centre, as CameraScreen aligns it
};

const cardKind = (k: HintKind): CardKind =>
  k === "PERFECT" ? "perfect" : k === "POSE" ? "pose" : k === "FRAMING" ? "framing" : k === "VISIBILITY" ? "visibility" : "none";

/** Displayed-hint segments [{text, kind, from, to}] from match.json, for the crossfade. */
const SEGMENTS = (() => {
  const out: { text: string; kind: HintKind; from: number; to: number }[] = [];
  for (let f = 336; f < MATCH_EVENTS.capture; f++) {
    const s = matchAt(f).shown;
    const last = out[out.length - 1];
    if (s && (!last || last.text !== s.text || last.to !== f)) out.push({ text: s.text, kind: s.kind, from: f, to: f + 1 });
    else if (s && last) last.to = f + 1;
  }
  return out;
})();

const HintLayer: React.FC<{ frame: number }> = ({ frame }) => {
  if (frame >= MATCH_EVENTS.digit3) return null; // CameraScreen removes the banner during the countdown
  return (
    <>
      {SEGMENTS.map((s, i) => {
        if (frame < s.from) return null;
        const fadeOut = i < SEGMENTS.length - 1 ? tween(frame, [s.to, s.to + ms(120)], [1, 0], E.softInOut) : 1;
        if (fadeOut <= 0) return null;
        const fadeIn = tween(frame, [s.from - 1, s.from - 1 + ms(180)], [0, 1], E.softInOut); // first frame already fading in
        return (
          <div key={i} style={{ opacity: Math.min(fadeIn, fadeOut) }}>
            <CueCard text={s.text} kind={cardKind(s.kind)} y={HUD_LAYOUT.cardTop} maxWidth={HUD_LAYOUT.cardMaxWidth} />
          </div>
        );
      })}
    </>
  );
};

const DIGITS = [
  { v: 3, from: MATCH_EVENTS.digit3, to: MATCH_EVENTS.digit2 },
  { v: 2, from: MATCH_EVENTS.digit2, to: MATCH_EVENTS.digit1 },
  { v: 1, from: MATCH_EVENTS.digit1, to: MATCH_EVENTS.capture },
];

const CountdownLayer: React.FC<{ frame: number }> = ({ frame }) => (
  <>
    {DIGITS.map((d) => {
      if (frame < d.from || frame >= d.to + ms(100)) return null;
      const scale = tween(frame, [d.from, d.from + ms(200)], [1.15, 1], E.snapSettle);
      const inO = tween(frame, [d.from - 1, d.from - 1 + ms(150)], [0, 1], E.softInOut);
      const outO = tween(frame, [d.to, d.to + ms(100)], [1, 0], E.softInOut);
      return <CountdownNumeral key={d.v} value={d.v} cx={HUD_LAYOUT.countdown[0]} cy={HUD_LAYOUT.countdown[1]} scale={scale} opacity={Math.min(inO, outO)} />;
    })}
  </>
);

export const Viewfinder: React.FC<{ frame: number }> = ({ frame }) => {
  const m = matchAt(frame);
  const person = performerAt(frame, POSES);
  const live = person ? flipForDisplay(visibleOnly(person)) : null;
  const hold = m.capture.state === "Holding" ? m.capture.progress : null;
  return (
    <AbsoluteFill style={{ background: theme.color.liveView }}>
      <CameraGroup scale={vfScale(frame)} origin={VF.origin}>
        <LiveView />
        <svg width={W} height={H} style={{ position: "absolute", inset: 0, overflow: "visible" }}>
          <FigureG skeleton={GUIDE} aspect={SCENE_ASPECT} x={0} y={0} height={H} partColors={partColorsAt(frame)} opacity={theme.hud.guideAlpha} />
          {live && (
            <FigureG
              skeleton={live}
              aspect={SCENE_ASPECT}
              x={0}
              y={0}
              height={H}
              style="stick"
              fill={theme.color.mannequin}
              lineAlpha={theme.hud.liveAlpha}
              lineWidth={theme.hud.liveStroke * theme.hud.px}
              showFace={false}
            />
          )}
        </svg>
      </CameraGroup>
      <ScoreRing score={m.percent} hold={hold} cx={HUD_LAYOUT.ring[0]} cy={HUD_LAYOUT.ring[1]} />
      <HintLayer frame={frame} />
      <CountdownLayer frame={frame} />
    </AbsoluteFill>
  );
};
