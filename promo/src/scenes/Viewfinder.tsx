/**
 * Viewfinder.tsx: act IV, B5-B7 (f336 to the f555 cut): the app's camera screen over a real woman in a real photo
 * studio, then the photo it takes (STORYBOARD v2 B5, B6, B7).
 *
 *   <Viewfinder frame={global frame} />
 *
 * The picture is the b5-live plate (Mixkit 39676, src/footage Footage: OffthreadVideo on the film's clock, printed in
 * the Studio ramp), zoomed by the film camera (shots.ts vfScale about the guide's mid-hip). Over it, through the same
 * transform so they stay on her, what the app's CameraOverlay draws with its default settings: the "Hands in pockets"
 * guide in the silhouette style at the default 55% overlay opacity, each part in Studio.forScore's bright camera
 * colour once a person is assigned, and the live skeleton (the app's own smoothed detections of her, stick style,
 * Mannequin at 70%, visible joints only). Screen-fixed on top: the score ring with its hold arc, the hint card, the
 * countdown, and the capture flash. The grid is set to Off (a real setting; the default thirds grid put a line
 * through her head and the countdown).
 *
 * Every number, colour, card, the hold, the countdown and the capture frame are the app's verdicts on this footage
 * (viewfinder/match.ts reads public/footage/b5-live.match.json, written by PromoFootageMatch.kt). The plate shows
 * exactly the frames those verdicts were computed on (b5-live's time remap is baked into the plate).
 *
 * Film choices, on top of the app's behaviour (NOTE.md, "Act IV as built"):
 *   - the HUD is CameraHud.kt's, magnified by theme.hud.k; its arrangement is the film's: the card and then the
 *     countdown sit in the headroom above her head (the plate is cropped low for it), the ring at the right below
 *     the softbox. The app centres the countdown in the preview, which here would sit on her hands.
 *   - one card at a time: a new hint replaces the old one on one frame and settles from 104% (the app crossfades
 *     180/120 ms, which at 1.95x reads as a double exposure); the first card arrives from 106% with a 4 f opacity
 *     helper. When the countdown starts the banner is gone in the same frame, as CameraScreen composes it out.
 *   - each digit lands from 115% over 200 ms (the app's scaleIn) and replaces the last on one frame (the app overlaps
 *     a 100 ms fade-out).
 *   - B7: under the app's capture flash (85% white, 280 ms decay) the frozen capture frame in colour, the whole frame
 *     (the photo is the full scene), no overlays, pushing 1.00 -> 1.06 to the cut (theme.footage.photoPush).
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import { Footage } from "../footage/Footage";
import { useLandmarks } from "../footage/hooks";
import type { FootageTransform } from "../footage/transform";
import { CaptureFlash, CountdownNumeral, CueCard, ScoreRing, type HintKind as CardKind } from "../ui/Hud";
import { FilmGrain } from "../world/Grain";
import { tween } from "../motion";
import { MATCH_EVENTS, MATCH_GUIDE, MATCH_PLATE, matchAt, partColorsAt, type HintKind } from "./viewfinder/match";
import { PHOTO, VF, photoScale, vfScale } from "./shots";
import { LIVE_PLAN } from "./timing";

const { W, H, fps } = theme.size;
const E = theme.ease;
const ASPECT = 9 / 16;
const ms = (v: number) => (v * fps) / 1000;

/** HUD layout (STORYBOARD section 9, adjusted to the footage): everything clear of her face and hands. */
export const HUD_LAYOUT = {
  cardTop: 0.13 * H,
  cardMaxWidth: 0.94 * W,
  /**
   * the countdown takes the card's place in the headroom, at 0.68 of CameraHud's 96 sp (review 1: at full size its
   * glyph reached 0.09 H, inside the top 12.5% that a feed's own UI covers). The glyph and contour now span about
   * 0.15-0.28 H settled and 0.14-0.29 H at the 115% landing: under the top safe line and clear of her hair (about
   * 0.33 H unpushed, 0.30 H under the film camera's last push).
   */
  countdown: [0.5 * W, 0.215 * H] as [number, number],
  countdownScale: 0.68,
  /** right of her body, under the softbox (a white ring on white would vanish there) */
  ring: [0.845 * W, 0.58 * H] as [number, number],
};

const cardKind = (k: HintKind): CardKind =>
  k === "PERFECT" ? "perfect" : k === "POSE" ? "pose" : k === "FRAMING" ? "framing" : k === "VISIBILITY" ? "visibility" : "none";

/** Displayed-hint segments [{text, kind, from, to}] from the verdicts, until the countdown removes the banner. */
const SEGMENTS = (() => {
  const out: { text: string; kind: HintKind; from: number; to: number }[] = [];
  for (let f = LIVE_PLAN.cut; f < MATCH_EVENTS.countdown; f++) {
    const s = matchAt(f).shown;
    const last = out[out.length - 1];
    if (s && (!last || last.text !== s.text || last.to !== f)) out.push({ text: s.text, kind: s.kind, from: f, to: f + 1 });
    else if (s && last) last.to = f + 1;
  }
  return out;
})();

const HintLayer: React.FC<{ frame: number }> = ({ frame }) => {
  const i = SEGMENTS.findIndex((s) => frame >= s.from && frame < s.to);
  if (i < 0) return null;
  const s = SEGMENTS[i];
  const first = i === 0 || SEGMENTS[i - 1].to !== s.from;
  const scale = tween(frame, [s.from, s.from + (first ? 8 : 6)], [first ? 1.06 : 1.04, 1], E.snapSettle);
  const opacity = first ? tween(frame, [s.from, s.from + 4], [0.3, 1], E.softInOut) : 1;
  return (
    <div style={{ position: "absolute", inset: 0, opacity, transform: `scale(${scale})`, transformOrigin: `${W / 2}px ${HUD_LAYOUT.cardTop}px` }}>
      <CueCard text={s.text} kind={cardKind(s.kind)} y={HUD_LAYOUT.cardTop} maxWidth={HUD_LAYOUT.cardMaxWidth} />
    </div>
  );
};

const DIGITS = MATCH_EVENTS.digits.map((d, i, all) => ({ v: d.v, from: d.from, to: i < all.length - 1 ? all[i + 1].from : MATCH_EVENTS.capture }));

const CountdownLayer: React.FC<{ frame: number }> = ({ frame }) => {
  const d = DIGITS.find((x) => frame >= x.from && frame < x.to);
  if (!d) return null;
  const scale = tween(frame, [d.from, d.from + ms(200)], [1.15, 1], E.snapSettle);
  const opacity = tween(frame, [d.from, d.from + 3], [0.35, 1], E.softInOut);
  return <CountdownNumeral value={d.v} cx={HUD_LAYOUT.countdown[0]} cy={HUD_LAYOUT.countdown[1]} scale={scale * HUD_LAYOUT.countdownScale} opacity={opacity} />;
};

const GUIDE = MATCH_GUIDE.figures[0];

export const Viewfinder: React.FC<{ frame: number }> = ({ frame }) => {
  const photo = frame >= PHOTO.from;
  const lm = useLandmarks(MATCH_PLATE, frame, { freezeAt: PHOTO.from });
  const m = matchAt(frame);
  const hold = m.capture.state === "Holding" ? m.capture.progress : null;
  const transform: FootageTransform = photo
    ? { scale: photoScale(frame), anchor: PHOTO.anchor, dx: 0, dy: 0 }
    : { scale: vfScale(frame), anchor: VF.anchor, dx: 0, dy: 0 };
  const live = lm?.drawn[0];
  const flash = photo ? tween(frame, [PHOTO.from, PHOTO.from + ms(280)], [0.85, 0], E.softLand) : 0;
  return (
    <AbsoluteFill style={{ background: theme.color.liveView }}>
      <Footage id={MATCH_PLATE} frame={frame} transform={transform} look={photo ? "colour" : "duotone"} freezeAt={PHOTO.from}>
        {!photo && (
          <svg width={W} height={H} style={{ position: "absolute", inset: 0, overflow: "visible" }}>
            <FigureG skeleton={GUIDE} aspect={ASPECT} height={H} style="silhouette" partColors={partColorsAt(frame)} opacity={theme.hud.guideAlpha} />
            {live && (
              <FigureG
                skeleton={live}
                aspect={ASPECT}
                height={H}
                style="stick"
                fill={theme.color.mannequin}
                lineAlpha={theme.hud.liveAlpha}
                lineWidth={theme.hud.liveStroke * theme.hud.px}
                showFace={false}
              />
            )}
          </svg>
        )}
      </Footage>
      {!photo && (
        <>
          <ScoreRing score={m.percent} hold={hold} cx={HUD_LAYOUT.ring[0]} cy={HUD_LAYOUT.ring[1]} />
          <HintLayer frame={frame} />
          <CountdownLayer frame={frame} />
        </>
      )}
      <CaptureFlash opacity={flash} />
      <FilmGrain />
    </AbsoluteFill>
  );
};
