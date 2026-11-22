/**
 * check-shapes.ts: parity check of the TypeScript FigureShapes port (src/figure/shapes.ts) against the Kotlin
 * FigureShapes.build output exported to public/poses.json by pose-core's PromoPoseExport test.
 * Run: node --experimental-strip-types scripts/check-shapes.ts   (part of `npm run check`)
 */
import fs from "node:fs";
import path from "node:path";
import { buildShapes, noseMarker } from "../src/figure/shapes.ts";
import type { PoseData, Shape, Vec } from "../src/figure/types.ts";

const file = path.join(import.meta.dirname, "..", "public", "poses.json");
const data = JSON.parse(fs.readFileSync(file, "utf8")) as PoseData;
const TOL = 2e-4;
let worst = 0;
let failures = 0;
const d = (a: Vec, b: Vec) => Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));
const cmp = (a: Shape, b: Shape): number => {
  if (a.t !== b.t || a.part !== b.part || a.bone !== b.bone) return Infinity;
  if (a.t === "capsule" && b.t === "capsule") return Math.max(d(a.a, b.a), d(a.b, b.b), Math.abs(a.wa - b.wa), Math.abs(a.wb - b.wb));
  if (a.t === "circle" && b.t === "circle") return Math.max(d(a.c, b.c), Math.abs(a.r - b.r));
  if (a.t === "polygon" && b.t === "polygon") return a.pts.length !== b.pts.length ? Infinity : Math.max(...a.pts.map((p, i) => d(p, b.pts[i])));
  return Infinity;
};
for (const pose of [...data.poses, ...data.illustrative]) {
  pose.figures.forEach((fig, fi) => {
    const mine = buildShapes(fig, pose.frameAspect);
    let e = mine.length === fig.shapes.length ? Math.max(0, ...mine.map((s, i) => cmp(s, fig.shapes[i]))) : Infinity;
    const n = noseMarker(fig, pose.frameAspect);
    if ((n === null) !== (fig.nose === null)) e = Infinity;
    else if (n && fig.nose) e = Math.max(e, d(n.c, fig.nose.c), Math.abs(n.r - fig.nose.r));
    worst = Math.max(worst, e);
    if (e > TOL) { failures++; console.error(`MISMATCH ${pose.id} figure ${fi}: max error ${e}`); }
  });
}
const figures = [...data.poses, ...data.illustrative].reduce((n, p) => n + p.figures.length, 0);
console.log(`shapes parity: ${data.poses.length} poses + ${data.illustrative.length} illustrative, ${figures} figures, worst error ${worst.toExponential(2)} (tolerance ${TOL})`);
if (failures) process.exit(1);
