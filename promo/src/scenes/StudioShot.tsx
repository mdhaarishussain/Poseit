/**
 * StudioShot.tsx: act III, B4 (f272-335), the pick (STORYBOARD v2 B4).
 *
 *   <StudioShot frame={global frame} />
 *     f272  hard cut on the unison hit after the drop: all 99 slots of the lit studio in "Hands in pockets" (the pick,
 *           drawn unflipped like act IV's guide), set down at once (studio/chorus.ts), the one-frame scale cut and
 *           its softLand tail (studio/camera.ts)
 *     f288  the detail screen's frame marks snap around the hero and its real name resolves under them (blur, rise
 *           and opacity, 5 f); the focus racks onto the hero's row
 *     f304  the accelerating push into the hero, solved every frame onto act IV's guide frame (shots.ts HERO_AT_CUT):
 *           the marks open out past the frame edges and the name leaves under the bottom edge; hard cut at f336 at
 *           peak speed
 * The world: StudioPaper (shaded per pixel from the camera), StudioFigures (depth-sorted, contact shadows, depth of
 * field that follows the hero), FilmGrain. Frame marks and the name are Studio Ink on paper (DESIGN.md).
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { COPY, POSES } from "../beats";
import { poseById, poseInkBounds } from "../figure/poses";
import { StudioPaper } from "../world/StudioPaper";
import { StudioFigures } from "../world/StudioFigures";
import { FilmGrain } from "../world/Grain";
import { FrameMarks } from "../ui/Hud";
import { focusIn, land, tween } from "../motion";
import { focusLook, heroDepth, heroFrameAt, shallowAt, studioCamAt } from "./studio/camera";
import { chorusItems } from "./studio/chorus";
import { STUDIO_TIMING } from "./timing";

const { W, H } = theme.size;
const E = theme.ease;
const PICK = poseById(POSES.pick);
const INK = poseInkBounds(PICK);

const MARKS_AT = STUDIO_TIMING.marks; // f288, a beat after the unison
const PUSH = STUDIO_TIMING.push;

/** The frame marks' rectangle: hugging the hero's ink (the detail screen's brackets), opening out with the push
 * and leaving past the frame edges before the cut. */
const marksRect = (f: number, hero: { x: number; y: number; height: number; width: number }) => {
  // drawn unflipped (the back camera): ink x runs minX .. maxX of the frame
  const ix0 = hero.x + INK.minX * hero.width, ix1 = hero.x + INK.maxX * hero.width;
  const iy0 = hero.y + INK.minY * hero.height, iy1 = hero.y + INK.maxY * hero.height;
  const pad = 0.08 * (iy1 - iy0);
  const around = { x: ix0 - pad, y: iy0 - pad, w: ix1 - ix0 + 2 * pad, h: iy1 - iy0 + 2 * pad };
  // past the frame edges by the cut: the marks open out with the push and leave, never parking as frame corners
  const out = 0.09 * W;
  const edge = { x: -out, y: -out * 1.4, w: W + 2 * out, h: H + 2.8 * out };
  const t = E.gentleIn(tween(f, PUSH, [0, 1], (x) => x));
  const mix = (a: number, b: number) => a + (b - a) * t;
  return { x: mix(around.x, edge.x), y: mix(around.y, edge.y), w: mix(around.w, edge.w), h: mix(around.h, edge.h) };
};

export const StudioShot: React.FC<{ frame: number }> = ({ frame }) => {
  const cam = studioCamAt(frame);
  const dh = heroDepth(cam);
  const items = chorusItems(frame);
  const hero = heroFrameAt(cam, PICK)!;
  const showMarks = frame >= MARKS_AT;
  const r = marksRect(frame, hero);
  const name = focusIn(frame, MARKS_AT);
  const rise = land(frame, MARKS_AT, { from: 0.012 * H, frames: 8 });
  const nameSize = theme.type.poseName.size;
  const markLen = Math.max(12 * theme.size.dp, 0.075 * r.w);
  const markW = Math.max(1.5 * theme.size.dp, 0.0065 * r.w);
  return (
    <AbsoluteFill style={{ background: theme.color.paper }}>
      <StudioPaper cam={cam} />
      <StudioFigures cam={cam} items={items} depth={focusLook(dh, shallowAt(frame))} />
      {showMarks && (
        <>
          <FrameMarks x={r.x} y={r.y} w={r.w} h={r.h} color={theme.color.ink} width={markW} length={markLen} />
          <div
            style={{
              position: "absolute",
              left: 0,
              width: W,
              top: r.y + r.h + 0.022 * H,
              textAlign: "center",
              fontFamily: theme.type.poseName.family,
              fontWeight: theme.type.poseName.weight,
              fontSize: nameSize,
              letterSpacing: theme.type.poseName.tracking,
              lineHeight: 1,
              color: theme.color.ink,
              opacity: name.opacity,
              filter: name.blur > 0.05 ? `blur(${name.blur}px)` : undefined,
              transform: `translateY(${rise}px)`,
            }}
          >
            {COPY.poseName}
          </div>
        </>
      )}
      <FilmGrain />
    </AbsoluteFill>
  );
};
