/**
 * EndDemo.tsx (dev still): the B8 end frame from foundation parts: one lay figure (stand_hand_on_hip, the
 * launcher pose) alone on the paper, feet at y 0.50 and about 0.24 H tall, the cove behind it, the wordmark at
 * ink width 0.50 W (fitTextToWidth) and the end line. Checks the paper at close range and the type roles.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { poseById, poseInkBounds } from "../figure/poses";
import { WIDE_CAM, frameOn, placePose, zoomAbout, type StudioCam } from "../world/camera";
import { StudioPaper } from "../world/StudioPaper";
import { StudioFigures } from "../world/StudioFigures";
import { FilmGrain } from "../world/Grain";
import { fitTextToWidth } from "../fonts";
import { COPY } from "../beats";

export const EndDemo: React.FC = () => {
  const { W, H } = theme.size;
  const pose = poseById("stand_hand_on_hip");
  const Z = 18;
  // aim: feet at (0.5 W, 0.50 H), figure ink 0.24 H tall
  let cam: StudioCam = frameOn(WIDE_CAM, [0, 0, Z], [W / 2, 0.5 * H]);
  const p = placePose(cam, pose, 0, Z)!;
  const ink = poseInkBounds(pose);
  const inkH = (ink.maxY - ink.minY) * p.frame.height;
  cam = zoomAbout(cam, (0.24 * H) / inkH, [W / 2, 0.5 * H]);
  const wm = theme.type.wordmark;
  const size = fitTextToWidth(COPY.wordmark, wm.family, wm.weight, wm.inkWidth);
  return (
    <AbsoluteFill style={{ background: theme.color.paper }}>
      <StudioPaper cam={cam} />
      <StudioFigures cam={cam} items={[{ pose, x: 0, z: Z }]} depth={() => ({ blur: 0, haze: 0 })} />
      <div style={{ position: "absolute", left: 0, right: 0, top: 0.62 * H - size * 0.74, textAlign: "center", fontFamily: wm.family, fontWeight: wm.weight, fontSize: size, letterSpacing: wm.tracking, lineHeight: 1, color: theme.color.ink }}>
        {COPY.wordmark}
      </div>
      {COPY.endLine.map((line, i) => (
        <div key={i} style={{ position: "absolute", left: 0, right: 0, top: [0.685, 0.725][i] * H - theme.type.endLine.size * 0.75, textAlign: "center", fontFamily: theme.font.text, fontWeight: 400, fontSize: theme.type.endLine.size, lineHeight: 1, color: theme.color.graphite }}>
          {line}
        </div>
      ))}
      <FilmGrain />
    </AbsoluteFill>
  );
};
