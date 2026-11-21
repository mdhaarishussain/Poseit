/**
 * poses.ts: the real pose library (public/poses.json, exported from pose-core by PromoPoseExport.kt).
 *
 *   POSES                      all 99 BuiltInPoses.all templates, library order
 *   poseById(id)               throws on an unknown id (typos fail loudly)
 *   posesInCategory(cat)       library order within the category
 *   CATEGORY_ORDER             the storyboard's chorus order (B3)
 *   CATEGORY_LABEL             the app's labels ("Fun & jumping", "On the ground" ...)
 *   illustrative(id)           DSL stances that are not library poses ("promo_stiff", "promo_stiff_flick")
 *   poseGround(pose)           lowest ink point of all figures, frame y (stand the pose on a floor with it)
 *   poseInkBounds(pose)        ink bounds of all figures, in frame coordinates (x 0..1, y 0..1)
 *   skeletonOf(pose, i)        a plain Skeleton (joints, visibility, unit) of figure i
 */
import raw from "../../public/poses.json";
import { buildShapes, shapeBounds } from "./shapes";
import type { CategoryId, Pose, PoseData, Skeleton } from "./types";

const data = raw as unknown as PoseData;

export const POSES: Pose[] = data.poses;

const byId = new Map<string, Pose>([...data.poses, ...data.illustrative].map((p) => [p.id, p]));

export const poseById = (id: string): Pose => {
  const p = byId.get(id);
  if (!p) throw new Error(`Unknown pose id: ${id}`);
  return p;
};

export const illustrative = (id: "promo_stiff" | "promo_stiff_flick"): Pose => poseById(id);

export const CATEGORY_LABEL = Object.fromEntries(data.categories.map((c) => [c.id, c.label])) as Record<CategoryId, string>;

/** Storyboard B3 order: on beats, then the eighth-note ripple, then the crowd doubling. */
export const CATEGORY_ORDER: CategoryId[] = [
  "STANDING", "SITTING", "LEANING", "WALKING",
  "FLOOR", "PORTRAIT", "SELFIE", "PROFESSIONAL", "FUN",
  "COUPLE", "GROUP",
];

export const posesInCategory = (cat: CategoryId): Pose[] => POSES.filter((p) => p.category === cat);

export const skeletonOf = (pose: Pose, i = 0): Skeleton => {
  const f = pose.figures[i];
  return { joints: f.joints, visibility: f.visibility, unit: f.unit };
};

const boundsCache = new Map<string, { minX: number; minY: number; maxX: number; maxY: number }>();

/** Ink bounds of every figure of the pose, frame coordinates (x 0..1 of width, y 0..1 of height). */
export const poseInkBounds = (pose: Pose) => {
  let b = boundsCache.get(pose.id);
  if (!b) {
    const all = pose.figures.flatMap((f) => buildShapes(f, pose.frameAspect));
    const iso = shapeBounds(all);
    b = { minX: iso.minX / pose.frameAspect, maxX: iso.maxX / pose.frameAspect, minY: iso.minY, maxY: iso.maxY };
    boundsCache.set(pose.id, b);
  }
  return b;
};

/** The floor line of a pose: its lowest ink point (feet, or the body for lying poses), frame y. */
export const poseGround = (pose: Pose): number => poseInkBounds(pose).maxY;

/** Mean body unit of the pose's figures (frame heights per torso length). */
export const poseUnit = (pose: Pose): number =>
  pose.figures.reduce((s, f) => s + f.unit, 0) / Math.max(1, pose.figures.length);
