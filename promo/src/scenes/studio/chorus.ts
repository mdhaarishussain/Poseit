/**
 * studio/chorus.ts: what all 99 slots of the lattice show in act III (B4, f272-335). Pure.
 *
 * v2: the studio is on screen only from the unison (the chorus of real people is scenes/People.tsx). On f272 every
 * slot is the pick, `stand_hands_in_pockets` (beats.ts POSES.pick), drawn unflipped (the back camera, as act IV's
 * guide), and each figure is set down from about 2% H above its mark, settling in 6 f: 99 figures landing at once.
 *
 *   UNISON          f272
 *   slotXZ(row, col) the lattice with even rows staggered half a column (nobody stands in front of the hero)
 *   chorusItems(f)  StudioItems for StudioFigures
 */
import { theme } from "../../theme";
import { POSES } from "../../beats";
import { poseById } from "../../figure/poses";
import { LATTICE, slotPosition } from "../../world/camera";
import type { StudioItem } from "../../world/StudioFigures";
import { land } from "../../motion";
import { STUDIO_TIMING } from "../timing";

const PICK = poseById(POSES.pick);
export const UNISON = STUDIO_TIMING.unison;
const LIFT_M = 0.15; // about 2% H at the hero row (0.24 H is 1.71 m there): a visible set-down on the unison

/**
 * Slot floor position: the foundation lattice with the even rows staggered by half a column, so nobody stands
 * directly in front of or behind the hero (row 1, col 4): it stays whole at the pick.
 */
export const slotXZ = (row: number, col: number): [number, number] => {
  const [x, z] = slotPosition(row, col);
  return [x + (row % 2 === 0 ? LATTICE.dx / 2 : 0), z];
};

export const chorusItems = (f: number): StudioItem[] => {
  const items: StudioItem[] = [];
  const lift = land(f, UNISON, { from: LIFT_M, to: 0, frames: 6, ease: theme.ease.softLand });
  for (let row = 0; row < LATTICE.rows; row++) {
    for (let col = 0; col < LATTICE.cols; col++) {
      const [x, z] = slotXZ(row, col);
      items.push({ pose: PICK, x, z, lift, flipX: false, key: `${row}-${col}` });
    }
  }
  return items;
};
