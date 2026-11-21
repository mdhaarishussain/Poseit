/**
 * studio/chorus.ts: what all 99 slots of the lattice are doing at a frame of act II (B2-B4). Pure.
 *
 *   slotPoseAt(f, row, col)   { pose, since }: the pose a slot shows and the frame it snapped to it
 *     f < 112      the illustrative stiff stance (everyone freezes, B2)
 *     f112-271     the chorus (beats.ts CHORUS): one category at a time, every slot a different pose of it,
 *                  slot pose = (col + 2 * row) mod n so neighbours differ and every pose of the category appears;
 *                  the eighth-note categories ripple from the front row back, 3 rows per frame
 *     f >= 272     fun_disco_point in unison (B4)
 *   chorusItems(f)            StudioItems for StudioFigures: every figure is flipped for display (the film's one
 *                             orientation) and is set down from about 2% H above its mark, settling in 6 f
 *   slotXZ(row, col)          the lattice with even rows staggered half a column
 *   CHORUS_SNAPS              every frame on which at least one slot changes pose (for events.ts)
 */
import { theme } from "../../theme";
import { CHORUS } from "../../beats";
import { illustrative, poseById, posesInCategory } from "../../figure/poses";
import type { CategoryId, Pose } from "../../figure/types";
import { LATTICE, slotPosition } from "../../world/camera";
import type { StudioItem } from "../../world/StudioFigures";
import { land } from "../../motion";

const STIFF = illustrative("promo_stiff");
const DISCO = poseById("fun_disco_point");
export const UNISON = 272;
const RIPPLE: CategoryId[] = ["FLOOR", "PORTRAIT", "SELFIE", "PROFESSIONAL", "FUN"];
const LIFT_M = 0.15; // about 2% H at the hero row (0.24 H is 1.71 m there): a visible set-down on every snap

const byCat = new Map<CategoryId, Pose[]>();
const listOf = (c: CategoryId) => {
  let l = byCat.get(c);
  if (!l) { l = posesInCategory(c); byCat.set(c, l); }
  return l;
};

/**
 * Slot floor position: the foundation lattice with the even rows staggered by half a column, so nobody stands
 * directly in front of or behind the hero (row 1, col 4): it stays whole on the match cut and the pick.
 */
export const slotXZ = (row: number, col: number): [number, number] => {
  const [x, z] = slotPosition(row, col);
  return [x + (row % 2 === 0 ? LATTICE.dx / 2 : 0), z];
};

const snapFrame = (at: number, cat: CategoryId, row: number) => at + (RIPPLE.includes(cat) ? Math.floor(row / 3) : 0);

export const slotPoseAt = (f: number, row: number, col: number): { pose: Pose; since: number } => {
  if (f >= UNISON) return { pose: DISCO, since: UNISON };
  let cur: { pose: Pose; since: number } = { pose: STIFF, since: -Infinity };
  for (const c of CHORUS) {
    const at = snapFrame(c.at, c.category as CategoryId, row);
    if (f < at) break;
    const list = listOf(c.category as CategoryId);
    cur = { pose: list[(col + 2 * row) % list.length], since: at };
  }
  return cur;
};

export const chorusItems = (f: number): StudioItem[] => {
  const items: StudioItem[] = [];
  for (let row = 0; row < LATTICE.rows; row++) {
    for (let col = 0; col < LATTICE.cols; col++) {
      const [x, z] = slotXZ(row, col);
      const { pose, since } = slotPoseAt(f, row, col);
      const lift = Number.isFinite(since) ? land(f, since, { from: LIFT_M, to: 0, frames: 6, ease: theme.ease.softLand }) : 0;
      items.push({ pose, x, z, lift, flipX: true, key: `${row}-${col}` });
    }
  }
  return items;
};

export const CHORUS_SNAPS: number[] = (() => {
  const s = new Set<number>();
  for (const c of CHORUS) for (let row = 0; row < LATTICE.rows; row++) s.add(snapFrame(c.at, c.category as CategoryId, row));
  s.add(UNISON);
  return [...s].sort((a, b) => a - b);
})();
