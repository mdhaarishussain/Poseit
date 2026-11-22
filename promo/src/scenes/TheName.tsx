/**
 * TheName.tsx: act V, B8 (f555-663), the name (STORYBOARD v2 B8).
 *
 *   <TheName frame={global frame} />
 *   f555       hard cut from the colour photo to grey Studio paper: a solid lay figure in "Hands in pockets"
 *              (the pick, unflipped) stands exactly where she stood in the photo on f554 (shots.ts
 *              PHOTO_FIGURE_AT_CUT: the guide's frame with the photo's push), with its contact shadow. f555-576 a
 *              designed breath (0.7 s): the colour has gone and the pose stands alone, and the photo's push carries
 *              across the cut (review 1): the camera keeps pushing in about the same point (her mid-hip), near the
 *              photo's speed, and settles by f600 (softLand, 2%; more puts the feet on the bottom edge), instead of the
 *              0.4% creep look.py counted as still
 *   f576-588   the figure relaxes ON TWOS (the posed clock): one hand leaves its pocket for the hip, landing in
 *              stand_hand_on_hip, the launcher icon's pose
 *   f600-624   the camera pulls back (a lens zoom about a fixed point, softLand 24 f) to reveal the cove; the figure
 *              ends 0.24 H tall with its feet at y 0.50
 *   f620       the wordmark lands below it from 1.3x and 6 px blur (snapSettle 10 f), ink width 0.50 W
 *   f628-645   "Free for Android." / "Works offline." resolve a word at a time (3 f apart, blur 8 -> 0)
 *   f630-663   the resolved hold, kept alive by a 0.8% camera creep; the film ends on it (no fade)
 * Both poses are drawn unflipped (the back camera, as act IV's guide), so the blend keeps every label on its side.
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { COPY, POSES } from "../beats";
import { poseById, poseGround, poseInkBounds } from "../figure/poses";
import { skeletonAt, mapJoints } from "../figure/interpolate";
import type { Pose, Skeleton } from "../figure/types";
import { WIDE_CAM, frameOn, placePose, zoomAbout, zoomed, type StudioCam } from "../world/camera";
import { StudioPaper } from "../world/StudioPaper";
import { StudioFigures } from "../world/StudioFigures";
import { FilmGrain } from "../world/Grain";
import { CameraGroup } from "../motion/CameraGroup";
import { WordReveal, words } from "../motion/WordReveal";
import { arrive, drift, tween } from "../motion";
import { fitTextToWidth } from "../fonts";
import { PHOTO, PHOTO_FIGURE_AT_CUT, type Rect } from "./shots";
import { NAME_TIMING } from "./timing";

const { W, H } = theme.size;
const E = theme.ease;
const Z = 18; // the figure stands 8 m in front of the cove
const ASPECT = 0.75;


const PICK = poseById(POSES.pick);
const ICON = poseById(POSES.icon);
const FROM: Skeleton = { joints: PICK.figures[0].joints, visibility: PICK.figures[0].visibility, unit: PICK.figures[0].unit };

/** stand_hand_on_hip re-expressed in the pick's frame: same body unit, feet planted where the pick's feet are. */
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
  const target = PHOTO_FIGURE_AT_CUT;
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

/**
 * The breath (f555-600): the photo's push carried across the cut. The photo pushes about her mid-hip at about 0.21% a
 * frame on f554 (shots.ts photoScale); a softLand of 2% over the breath starts near that speed (0.16% a frame) about
 * the same screen point and settles before the pull back.
 */
const SETTLE = { to: 1.02, about: [PHOTO.anchor[0] * W, PHOTO.anchor[1] * H] as [number, number] };
const CAM_SETTLED: StudioCam = zoomAbout(CAM_START, SETTLE.to, SETTLE.about);

const PULL = (() => {
  const k = CAM_END.focal / CAM_SETTLED.focal;
  // the fixed point of the zoom that takes CAM_SETTLED's principal point to CAM_END's
  const p: [number, number] = [(CAM_END.cx - k * CAM_SETTLED.cx) / (1 - k), (CAM_END.cy - k * CAM_SETTLED.cy) / (1 - k)];
  return { k, p };
})();

export const LOCKUP_ORIGIN: [number, number] = [W / 2, 0.58 * H];
const creep = (f: number) => drift(f, [NAME_TIMING.wordmarkLanded, NAME_TIMING.end], [1, 1.008]);

const camAt = (f: number): StudioCam => {
  const cam =
    f < NAME_TIMING.pull[0]
      ? zoomAbout(CAM_START, tween(f, [NAME_TIMING.cut, NAME_TIMING.pull[0]], [1, SETTLE.to], E.softLand), SETTLE.about)
      : zoomAbout(CAM_SETTLED, Math.pow(PULL.k, tween(f, NAME_TIMING.pull, [0, 1], E.softLand)), PULL.p);
  return zoomAbout(cam, creep(f), LOCKUP_ORIGIN);
};

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
    </AbsoluteFill>
  );
};

