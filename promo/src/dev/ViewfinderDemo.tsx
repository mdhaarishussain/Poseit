/**
 * ViewfinderDemo.tsx (dev, 2 stills): the camera world with the rebuilt HUD, to compare against
 * app/build/repo-assets/screen-3-camera.png (the matched still) and app/build/screenshots/camera-hud.png.
 *   f0  coaching: the see-through Disco point guide (flipped for display) with mixed per-limb colours, the stiff
 *       live skeleton too close, cue card "Your feet are cut off. Step back", ring at 41
 *   f1  matched: every limb green, "Perfect. Hold it", ring 93 with the hold arc, countdown "3"
 * Numbers here are placeholders for the look only; the film shows PoseMatcher's values.
 */
import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import { poseById, illustrative, poseInkBounds, skeletonOf } from "../figure/poses";
import { flipForDisplay, mapJoints } from "../figure/interpolate";
import { LiveView } from "../world/LiveView";
import { CueCard, ScoreRing, CountdownNumeral } from "../ui/Hud";
import type { BodyPart } from "../figure/types";

export const ViewfinderDemo: React.FC = () => {
  const frame = useCurrentFrame();
  const { W, H } = theme.size;
  const matched = frame >= 1;
  const disco = poseById("fun_disco_point");
  const ink = poseInkBounds(disco);
  // storyboard section 9: guide head top y 0.38, feet y 0.94, centred
  const fh = (0.56 * H) / (ink.maxY - ink.minY);
  const fw = fh * disco.frameAspect;
  const fy = 0.38 * H - ink.minY * fh;
  const fx = W / 2 - (1 - (ink.minX + ink.maxX) / 2) * fw; // flipped: ink centre mirrors
  const sig = theme.color.signalCam;
  const parts: Partial<Record<BodyPart, string>> = matched
    ? { HEAD: sig.match, TORSO: sig.match, LEFT_ARM: sig.match, RIGHT_ARM: sig.match, LEFT_LEG: sig.match, RIGHT_LEG: sig.match }
    : { HEAD: sig.match, TORSO: theme.signal.forScore(0.7), LEFT_ARM: sig.miss, RIGHT_ARM: theme.signal.forScore(0.5), LEFT_LEG: sig.miss, RIGHT_LEG: sig.miss };
  // the live skeleton: the stiff stance, too close (1.3x about the frame centre) for f0; the disco pose for f1
  const live = matched
    ? flipForDisplay(skeletonOf(disco))
    : mapJoints(flipForDisplay(skeletonOf(illustrative("promo_stiff"))), (p) => [0.5 + (p[0] - 0.5) * 1.3, 0.42 + (p[1] - 0.42) * 1.3]);
  const liveUnit = matched ? disco.figures[0].unit : illustrative("promo_stiff").figures[0].unit * 1.3;
  return (
    <AbsoluteFill>
      <LiveView />
      <svg width={W} height={H} style={{ position: "absolute", inset: 0 }}>
        <FigureG skeleton={flipForDisplay(skeletonOf(disco))} aspect={disco.frameAspect} x={fx} y={fy} height={fh} partColors={parts} opacity={0.55} />
        <FigureG
          skeleton={{ ...live, unit: liveUnit }}
          aspect={disco.frameAspect}
          x={fx}
          y={fy}
          height={fh}
          style="stick"
          fill={theme.color.mannequin}
          lineAlpha={0.7}
          lineWidth={2.5 * theme.hud.px}
        />
      </svg>
      <ScoreRing score={matched ? 93 : 41} hold={matched ? 0.62 : null} cx={0.83 * W} cy={0.33 * H} />
      <CueCard
        text={matched ? "Perfect. Hold it" : "Your feet are cut off. Step back"}
        kind={matched ? "perfect" : "visibility"}
        y={0.135 * H}
        maxWidth={0.8 * W}
      />
      {matched && <CountdownNumeral value={3} cx={W / 2} cy={0.6 * H} />}
    </AbsoluteFill>
  );
};
