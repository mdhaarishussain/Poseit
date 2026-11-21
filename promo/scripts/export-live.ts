/**
 * export-live.ts: writes public/live.json, the input of the app's real matcher for act III.
 *
 * For every frame of act III (B5-B7) it evaluates the keyframed performer (src/scenes/viewfinder/performer.ts) and
 * the Disco point guide, both in the app's scene space (9:16, photo orientation). The app-module test
 * PromoMatchExport.kt then runs PoseMatcher, HintStabilizer and CaptureCoach over them and writes public/match.json,
 * which the film reads. Run both with `npm run match` (bundles this file with esbuild, then runs the test).
 */
import fs from "node:fs";
import path from "node:path";
import { poseById, illustrative } from "../src/figure/poses";
import { SCENE_ASPECT, guideScene } from "../src/scenes/viewfinder/rig";
import { performerAt } from "../src/scenes/viewfinder/performer";
import type { Skeleton } from "../src/figure/types";

const poses = { disco: poseById("fun_disco_point"), stiff: illustrative("promo_stiff") };
const r = (v: number) => Math.round(v * 1e5) / 1e5;
const sk = (s: Skeleton) => ({
  joints: Object.fromEntries(Object.entries(s.joints).map(([k, p]) => [k, [r(p![0]), r(p![1])]])),
  visibility: s.visibility ?? {},
  unit: r(s.unit),
});

// v1's window for the keyframed performer (f336-553, plus a few frames in case the coach fires late). v2 replaces the
// performer with the live plate's detection (STORYBOARD.md v2 section 12, step 6; BUILDER LIVE) and then reads its
// window from the manifest shot `b5-live` and beats.ts ACTS.IV instead.
const V1_WINDOW = { from: 336, to: 554 };
const from = V1_WINDOW.from;
const to = V1_WINDOW.to + 8;
const frames = [];
for (let f = from; f < to; f++) {
  const p = performerAt(f, poses);
  frames.push({ f, person: p ? sk(p) : null });
}
const out = {
  source: "promo/scripts/export-live.ts (performer.ts keyframes, guide = fun_disco_point in scene space)",
  aspect: SCENE_ASPECT,
  fps: 30,
  from,
  guide: sk(guideScene(poses.disco)),
  frames,
};
const file = path.join(process.cwd(), "public", "live.json");
fs.writeFileSync(file, JSON.stringify(out));
console.log(`live.json: ${frames.length} frames (${from}-${to - 1}), aspect ${SCENE_ASPECT.toFixed(4)}`);
