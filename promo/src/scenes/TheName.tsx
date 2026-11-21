/**
 * TheName.tsx: act IV, B8 (f554-663), the photo, then the name (STORYBOARD B8).
 *
 *   <TheName frame={global frame} />
 *   f554       the capture flash, the app's own (CameraScreen: white snapped to 85%, decaying over 280 ms,
 *              LinearOutSlowIn). Under it the ground is now the studio paper and the guide is a solid lay figure at
 *              the same size and place (the guide's screen frame on the last viewfinder frame): the photo
 *   f576-588   the figure relaxes ON TWOS (the posed clock): the pointing arm drops to its side, the hip hand stays,
 *              and it lands in stand_hand_on_hip, the launcher icon's pose
 *   f600-624   the camera pulls back (a lens zoom about a fixed point, softLand 24 f) to reveal the cove; the figure
 *              ends 0.24 H tall with its feet at y 0.50
 *   f620       the wordmark lands below it from 1.3x and 6 px blur (snapSettle 10 f), ink width 0.50 W
 *   f628-645   "Free for Android." / "Works offline." resolve a word at a time (3 f apart, blur 8 -> 0)
 *   f630-663   the resolved hold (1.1 s from the wordmark's landing; the end line finishes resolving at f645),
 *              kept alive by a 0.8% camera creep; the film ends on it (no fade)
 * Mirroring: the flipped Disco point guide and mirrored(disco) drawn un-flipped are the same picture, and only the
 * mirrored one has its labels on the launcher pose's sides, so the relax is mirrored(disco) -> stand_hand_on_hip.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { COPY } from "../beats";
import { poseById, poseGround, poseInkBounds } from "../figure/poses";
import { mirrored, skeletonAt, mapJoints } from "../figure/interpolate";
import type { Pose, Skeleton } from "../figure/types";
import { WIDE_CAM, frameOn, placePose, zoomAbout, zoomed, type StudioCam } from "../world/camera";
import { StudioPaper } from "../world/StudioPaper";
import { StudioFigures } from "../world/StudioFigures";
import { FilmGrain } from "../world/Grain";
import { CaptureFlash } from "../ui/Hud";
import { CameraGroup } from "../motion/CameraGroup";
import { WordReveal, words } from "../motion/WordReveal";
import { arrive, drift, tween } from "../motion";
import { fitTextToWidth } from "../fonts";
import { guideScreenFrame, type Rect } from "./shots";
import { NAME_TIMING } from "./timing";

const { W, H, fps } = theme.size;
const E = theme.ease;
const Z = 18; // the figure stands 8 m in front of the cove
const ASPECT = 0.75;


const DISCO = poseById("fun_disco_point");
const ICON = poseById("stand_hand_on_hip");
const FROM = mirrored({ joints: DISCO.figures[0].joints, visibility: DISCO.figures[0].visibility, unit: DISCO.figures[0].unit });

/** stand_hand_on_hip re-expressed in the Disco frame: same body unit, feet planted where the Disco feet are. */
const TO: Skeleton = (() => {
  const f = ICON.figures[0];
  const k = FROM.unit / f.unit;
  const ankle = (s: Skeleton) => [(s.joints.LEFT_ANKLE![0] + s.joints.RIGHT_ANKLE![0]) / 2, Math.max(s.joints.LEFT_ANKLE![1], s.joints.RIGHT_ANKLE![1])];
  const a0 = ankle({ joints: f.joints, unit: f.unit }), a1 = ankle(FROM);
  const s = mapJoints({ joints: f.joints, visibility: f.visibility, unit: f.unit }, (p) => [a1[0] + (p[0] - a0[0]) * k, a1[1] + (p[1] - a0[1]) * k]);
  return { ...s, unit: FROM.unit };
})();

const asPose = (s: Skeleton, id: string): Pose => ({
  id,
  name: "relax",
  category: "STANDING",
  frameAspect: ASPECT,
  figures: [{ ...s, visibility: s.visibility ?? {}, shapes: [], nose: null }],
});

const figureAt = (f: number): Pose => {
  const ff = Math.floor(f / 2) * 2; // on twos
  if (ff <= NAME_TIMING.relax[0]) return asPose(FROM, "b8-from");
  if (ff >= NAME_TIMING.relax[1]) return asPose(TO, "b8-to");
  const s = skeletonAt(ff, [
    { f: NAME_TIMING.relax[0], skeleton: FROM },
    { f: NAME_TIMING.relax[1], skeleton: TO, ease: E.softInOut },
  ], { aspect: ASPECT });
  return asPose(s, `b8-${ff}`);
};

// ---- the camera: start on the guide's frame, end on the lockup framing ----------------------------------------
const frameOf = (cam: StudioCam, pose: Pose): Rect => {
  const p = placePose(cam, pose, 0, Z)!;
  return { ...p.frame, width: p.frame.height * ASPECT };
};

const CAM_START: StudioCam = (() => {
  const target = guideScreenFrame(NAME_TIMING.flash - 1);
  const pose = asPose(FROM, "b8-from");
  const h0 = frameOf(WIDE_CAM, pose).height;
  const cam = zoomed(WIDE_CAM, target.height / h0);
  return frameOn(cam, [0, 0, Z], [target.x + target.width / 2, target.y + poseGround(pose) * target.height]);
})();

const CAM_END: StudioCam = (() => {
  const pose = asPose(TO, "b8-to");
  const ink = poseInkBounds(pose);
  let cam = frameOn(WIDE_CAM, [0, 0, Z], [W / 2, 0.5 * H]);
  const fr = frameOf(cam, pose);
  cam = zoomAbout(cam, (0.24 * H) / ((ink.maxY - ink.minY) * fr.height), [W / 2, 0.5 * H]);
  // centre the ink (not the frame) on the centre line
  const fr2 = frameOf(cam, pose);
  const inkCx = fr2.x + ((ink.minX + ink.maxX) / 2) * fr2.width;
  return { ...cam, cx: cam.cx + (W / 2 - inkCx) };
})();

const PULL = (() => {
  const k = CAM_END.focal / CAM_START.focal;
  // the fixed point of the zoom that takes CAM_START's principal point to CAM_END's
  const p: [number, number] = [(CAM_END.cx - k * CAM_START.cx) / (1 - k), (CAM_END.cy - k * CAM_START.cy) / (1 - k)];
  return { k, p };
})();

export const LOCKUP_ORIGIN: [number, number] = [W / 2, 0.58 * H];
const creep = (f: number) => drift(f, [NAME_TIMING.wordmarkLanded, NAME_TIMING.end], [1, 1.008]);

const camAt = (f: number): StudioCam => {
  const e = tween(f, NAME_TIMING.pull, [0, 1], E.softLand);
  const settle = drift(f, [NAME_TIMING.flash, NAME_TIMING.pull[0]], [1, 1.004]); // the held photo breathes
  const cam = zoomAbout(CAM_START, Math.pow(PULL.k, e) * settle, PULL.p);
  return zoomAbout(cam, creep(f), LOCKUP_ORIGIN);
};

const ms = (v: number) => (v * fps) / 1000;
const flashAt = (f: number) => (f < NAME_TIMING.flash ? 0 : tween(f, [NAME_TIMING.flash, NAME_TIMING.flash + ms(280)], [0.85, 0], E.softLand));
export { NAME_TIMING };

export const TheName: React.FC<{ frame: number }> = ({ frame }) => {
  const cam = camAt(frame);
  const wm = theme.type.wordmark;
  const size = fitTextToWidth(COPY.wordmark, wm.family, wm.weight, wm.inkWidth);
  const a = arrive(frame, NAME_TIMING.wordmark, { scale: 1.3, blur: 6, frames: 10 });
  const line = theme.type.endLine;
  const lines = [words(COPY.endLine[0], NAME_TIMING.line, 3), words(COPY.endLine[1], NAME_TIMING.line + 9, 3)];
  return (
    <AbsoluteFill style={{ background: theme.color.paper }}>
      <StudioPaper cam={cam} />
      <StudioFigures cam={cam} items={[{ pose: figureAt(frame), x: 0, z: Z, key: "hero" }]} depth={() => ({ blur: 0, haze: 0 })} />
      <CameraGroup scale={creep(frame)} origin={LOCKUP_ORIGIN}>
        {a.shown && (
          <div
            style={{
              position: "absolute",
              left: 0,
              width: W,
              top: 0.62 * H - size * 0.74,
              textAlign: "center",
              fontFamily: wm.family,
              fontWeight: wm.weight,
              fontSize: size,
              letterSpacing: wm.tracking,
              lineHeight: 1,
              color: theme.color.ink,
              opacity: a.opacity,
              transform: `scale(${a.scale})`,
              transformOrigin: `${W / 2}px ${size * 0.4}px`,
              filter: a.blur > 0.05 ? `blur(${a.blur}px)` : undefined,
            }}
          >
            {COPY.wordmark}
          </div>
        )}
        <div style={{ position: "absolute", left: 0, right: 0, top: 0.685 * H - line.size * 0.74 }}>
          <WordReveal
            lines={lines}
            mode="focus"
            lineGap={(0.725 - 0.685) * H - line.size}
            style={{ fontFamily: line.family, fontWeight: line.weight, fontSize: line.size, lineHeight: 1, color: theme.color.graphite }}
          />
        </div>
      </CameraGroup>
      <FilmGrain />
      <CaptureFlash opacity={flashAt(frame)} />
    </AbsoluteFill>
  );
};

