/**
 * check-live.ts: fails when act IV would show verdicts for frames that are no longer on screen, or when the app's
 * verdicts no longer pass the manifest's acceptance list. Part of `npm run check`.
 *
 * Act IV draws the b5-live plate (public/footage/b5-live.mp4, gitignored), the app's smoothed detections of it
 * (public/footage/b5-live.landmarks.json) and the app's verdicts on them (public/footage/b5-live.match.json, read by
 * src/scenes/viewfinder/match.ts). They are written in that order by `npm run footage`, `npm run landmarks` and
 * `npm run footage-match`; this checks that they still describe the same frames:
 *   - plates.json's b5-live record has the manifest's crop and measured time remap (the plate was rebuilt after the
 *     manifest changed);
 *   - every landmark frame and every verdict sits on the plate frame and source frame plates.json says is shown on
 *     that film frame;
 *   - the verdicts were computed for the film's camera and guide (back camera, not mirrored, PHOTOGRAPHER audience,
 *     the picked pose), at a real strictness of at least 0.75;
 *   - acceptance: Holding, then the countdown, then Capture inside beats.ts CAPTURE_WINDOW, and at least one hint
 *     card (not "Perfect") shown before the countdown.
 */
import fs from "node:fs";
import path from "node:path";
import { CAPTURE_WINDOW, POSES } from "../src/beats";

const ROOT = process.cwd();
const read = (p: string) => JSON.parse(fs.readFileSync(path.join(ROOT, p), "utf8"));
const errors: string[] = [];
const fail = (m: string) => errors.push(m);

const manifest = read("footage/manifest.json");
const shot = manifest.shots.find((s: { id: string }) => s.id === "b5-live");
const plate = read("public/footage/plates.json").plates["b5-live"];
const lm = read("public/footage/b5-live.landmarks.json");
const match = read("public/footage/b5-live.match.json");

// the plate is the manifest's
const keys = shot.timeRemap?.measured?.keys ?? shot.timeRemap?.planned;
if (JSON.stringify(plate.crop) !== JSON.stringify(shot.crop)) fail(`plates.json b5-live crop ${JSON.stringify(plate.crop)} != manifest ${JSON.stringify(shot.crop)}: run npm run footage`);
if (JSON.stringify(plate.source.remap) !== JSON.stringify(keys)) fail(`plates.json b5-live remap ${JSON.stringify(plate.source.remap)} != manifest ${JSON.stringify(keys)}: run npm run footage`);

// landmarks and verdicts are on the frames the plate shows
const srcOf = (f: number) => plate.sourceFrames[f - plate.filmFrom];
let lmBad = 0;
for (const fr of lm.frames) if (fr.f - plate.filmFrom !== fr.n || plate.sourceFrames[fr.n] !== fr.src) lmBad++;
if (lm.frames.length !== plate.frames) fail(`b5-live.landmarks.json has ${lm.frames.length} frames, the plate ${plate.frames}: run npm run landmarks`);
if (lmBad) fail(`b5-live.landmarks.json: ${lmBad} frames are not on the plate's source frames: run npm run landmarks`);
let mBad = 0;
for (const r of match.frames) if (r.f - plate.filmFrom !== r.n || srcOf(r.f) !== r.src) mBad++;
if (mBad) fail(`b5-live.match.json: ${mBad} verdicts are not on the plate's frames: run npm run footage-match`);

// the film's camera, guide and a real strictness
if (match.camera.lens !== "BACK" || match.camera.mirroredView !== false || match.camera.audience !== "PHOTOGRAPHER") fail(`b5-live.match.json camera ${JSON.stringify(match.camera)}: expected the back camera, not mirrored, PHOTOGRAPHER`);
if (match.guide.poseId !== POSES.pick) fail(`b5-live.match.json guide ${match.guide.poseId} != the picked pose ${POSES.pick}`);
if (!(match.coach.threshold >= 0.75)) fail(`b5-live.match.json strictness ${match.coach.threshold} is under 0.75`);

// acceptance (manifest b5-live.acceptance)
const e = match.events;
const countdown = e.countdown?.["3"] ?? null;
const cardsBefore = (e.cards as { f: number; kind: string }[]).filter((c) => countdown != null && c.f < countdown && c.kind !== "PERFECT");
if (e.hold == null || countdown == null || !(e.hold < countdown)) fail(`no lock then countdown (hold ${e.hold}, countdown ${countdown})`);
if (e.capture == null || e.capture < CAPTURE_WINDOW[0] || e.capture > CAPTURE_WINDOW[1]) fail(`capture f${e.capture} is outside f${CAPTURE_WINDOW[0]}-${CAPTURE_WINDOW[1]}`);
if (!cardsBefore.length) fail("no hint card before the countdown");

if (errors.length) {
  for (const m of errors) console.error(`act IV: ${m}`);
  process.exit(1);
}
console.log(
  `act IV: b5-live plate, landmarks and verdicts agree (${match.frames.length} frames); ${match.guide.poseId} at ${match.coach.threshold}: ` +
    `hold f${e.hold}, countdown f${countdown}, capture f${e.capture}, ${cardsBefore.length} card(s) before the countdown`,
);
