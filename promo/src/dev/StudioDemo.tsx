/**
 * StudioDemo.tsx (dev, 96 f): the studio world with all 99 slots of the lattice, to check light, depth, shadows and
 * parallax. f0-47: everyone in the illustrative stiff stance while the camera dollies forward 1.5 m (parallax: front
 * rows travel faster than back rows; the paper tooth recedes). f48+: slot pose = (col + 2 * row) mod n of a category
 * (STORYBOARD B3), switching category every 16 f. Not the film: a reference for the scene builder.
 */
import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { CATEGORY_ORDER, illustrative, posesInCategory } from "../figure/poses";
import { LATTICE, WIDE_CAM, slotPosition } from "../world/camera";
import { StudioPaper } from "../world/StudioPaper";
import { StudioFigures, type StudioItem } from "../world/StudioFigures";
import { FilmGrain } from "../world/Grain";
import { drift } from "../motion";

export const StudioDemo: React.FC = () => {
  const frame = useCurrentFrame();
  const cam = { ...WIDE_CAM, z: drift(frame, [0, 95], [0, 1.5]) };
  const catIndex = frame < 48 ? -1 : Math.min(CATEGORY_ORDER.length - 1, Math.floor((frame - 48) / 16));
  const stiff = illustrative("promo_stiff");
  const items: StudioItem[] = [];
  for (let row = 0; row < LATTICE.rows; row++) {
    for (let col = 0; col < LATTICE.cols; col++) {
      const [x, z] = slotPosition(row, col);
      let pose = stiff;
      if (catIndex >= 0) {
        const list = posesInCategory(CATEGORY_ORDER[catIndex]);
        pose = list[(col + 2 * row) % list.length];
      }
      items.push({ pose, x, z, key: `${row}-${col}`, flipX: true });
    }
  }
  return (
    <AbsoluteFill style={{ background: theme.color.paper }}>
      <StudioPaper cam={cam} />
      <StudioFigures cam={cam} items={items} />
      <FilmGrain />
    </AbsoluteFill>
  );
};
