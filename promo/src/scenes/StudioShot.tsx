/**
 * StudioShot.tsx: act II, B2-B4 (f80-335), one continuous shot of the studio (STORYBOARD B2, B3, B4).
 *
 *   <StudioShot frame={global frame} />
 *     B2  f80   the hero lay figure exactly where the hook skeleton was; the camera snaps back and settles on the
 *               wide framing: 99 lay figures, all in the same stiff stance (studio/camera.ts, studio/chorus.ts)
 *     B3  f112  the chorus: every slot snaps through a category per beat, then per eighth (rippling back), then the
 *               couples and groups; the camera dollies in two pushes
 *     B4  f272  the unison: all 99 in Disco point, a one-frame scale cut; f288 the detail screen's frame marks
 *               snap around the hero and its name resolves under them; f304 the push into the hero: the marks
 *               open out past the frame edges and the name leaves under the bottom edge
 * The world: StudioPaper (shaded per pixel from the camera), StudioFigures (depth-sorted, contact shadows, depth of
 * field that follows the hero), FilmGrain. Frame marks and the name are Studio Ink on paper (DESIGN.md).
 */
import React from "react";
import { AbsoluteFill } from "remotion";
import { theme } from "../theme";
import { COPY } from "../beats";
import { poseById, poseInkBounds } from "../figure/poses";
import { StudioPaper } from "../world/StudioPaper";
import { StudioFigures } from "../world/StudioFigures";
import { FilmGrain } from "../world/Grain";
import { FrameMarks } from "../ui/Hud";
import { focusIn, tween } from "../motion";
import { focusLook, heroDepth, heroFrameAt, shallowAt, studioCamAt } from "./studio/camera";
import { chorusItems } from "./studio/chorus";
import { STUDIO_TIMING } from "./timing";

const { W, H } = theme.size;
const E = theme.ease;
const DISCO = poseById("fun_disco_point");
const INK = poseInkBounds(DISCO);

const MARKS_AT = STUDIO_TIMING.marks; // f288, a beat after the unison
const PUSH = STUDIO_TIMING.push;

/** The frame marks' rectangle: hugging the hero's ink (the detail screen's brackets), opening out with the push
 * and leaving past the frame edges before the cut. */
const marksRect = (f: number, hero: { x: number; y: number; height: number; width: number }) => {
  // flipped for display: ink x runs 1 - maxX .. 1 - minX of the frame
  const ix0 = hero.x + (1 - INK.maxX) * hero.width, ix1 = hero.x + (1 - INK.minX) * hero.width;
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
  const hero = heroFrameAt(cam, DISCO)!;
  const showMarks = frame >= MARKS_AT;
  const r = marksRect(frame, hero);
  const name = focusIn(frame, MARKS_AT);
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
