/**
 * types.ts: the pose data model, mirroring pose-core (com.poseit.app.pose).
 *
 * Skeleton joints are in FRAME coordinates: x 0..1 across the frame width, y 0..1 down the frame height, in photo
 * orientation with anatomical labels (the subject's LEFT shoulder is on the image right when facing the camera).
 * Values outside 0..1 are allowed (a portrait's legs). The frame's shape is `aspect` = width / height.
 *
 * Shapes (FigureShapes.build output) are in ISOTROPIC HEIGHT UNITS: x 0..aspect, y 0..1; multiply both by the
 * frame's pixel height and add the frame's pixel offset.
 */

export const JOINTS = [
  "NOSE", "LEFT_EYE", "RIGHT_EYE", "LEFT_EAR", "RIGHT_EAR",
  "LEFT_SHOULDER", "RIGHT_SHOULDER", "LEFT_ELBOW", "RIGHT_ELBOW", "LEFT_WRIST", "RIGHT_WRIST",
  "LEFT_HAND", "RIGHT_HAND", "LEFT_HIP", "RIGHT_HIP", "LEFT_KNEE", "RIGHT_KNEE",
  "LEFT_ANKLE", "RIGHT_ANKLE", "LEFT_FOOT", "RIGHT_FOOT",
] as const;
export type JointName = (typeof JOINTS)[number];

export type BodyPart = "HEAD" | "TORSO" | "LEFT_ARM" | "RIGHT_ARM" | "LEFT_LEG" | "RIGHT_LEG";
export const BODY_PARTS: BodyPart[] = ["HEAD", "TORSO", "LEFT_ARM", "RIGHT_ARM", "LEFT_LEG", "RIGHT_LEG"];

export type BoneName =
  | "SHOULDERS" | "HIPS" | "LEFT_FLANK" | "RIGHT_FLANK"
  | "LEFT_UPPER_ARM" | "LEFT_FOREARM" | "LEFT_HAND" | "RIGHT_UPPER_ARM" | "RIGHT_FOREARM" | "RIGHT_HAND"
  | "LEFT_THIGH" | "LEFT_SHIN" | "LEFT_FOOT" | "RIGHT_THIGH" | "RIGHT_SHIN" | "RIGHT_FOOT";

export type Vec = [number, number];
export type Joints = Partial<Record<JointName, Vec>>;

export type Skeleton = {
  joints: Joints;
  /** Per-joint confidence 0..1; absent = fully visible. */
  visibility?: Partial<Record<JointName, number>>;
  /** Body unit (torso length) in frame heights; 0 = estimate from the points. */
  unit: number;
};

export type Shape =
  | { t: "capsule"; a: Vec; b: Vec; wa: number; wb: number; part: BodyPart; bone: BoneName | null }
  | { t: "circle"; c: Vec; r: number; part: BodyPart; bone: BoneName | null }
  | { t: "polygon"; pts: Vec[]; part: BodyPart; bone: BoneName | null };

export type PoseFigure = Skeleton & {
  /** FigureShapes.build exported from Kotlin (the reference the TS port is checked against). */
  shapes: Shape[];
  nose: { c: Vec; r: number } | null;
};

export type CategoryId =
  | "STANDING" | "SITTING" | "LEANING" | "WALKING" | "PORTRAIT" | "SELFIE"
  | "PROFESSIONAL" | "FUN" | "FLOOR" | "COUPLE" | "GROUP";

export type Pose = {
  id: string;
  name: string;
  category: CategoryId;
  frameAspect: number;
  figures: PoseFigure[];
};

export type PoseData = {
  source: string;
  joints: JointName[];
  categories: { id: CategoryId; label: string }[];
  poses: Pose[];
  illustrative: Pose[];
};
