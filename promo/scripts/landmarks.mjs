#!/usr/bin/env node
/**
 * landmarks.mjs: the app's pose detection on every prepared plate, then the app's own landmark smoothing.
 *
 *   npm run landmarks                     every plate in public/footage/plates.json (after npm run footage)
 *   node scripts/landmarks.mjs --only b5-live [--force] [--skip-detect]
 *
 * 1. scripts/detect.py (unchanged) on public/footage/<plate>.mp4 -> out/footage/<plate>.detect.json (gitignored):
 *    the app's live model and settings (pose_landmarker_lite.task, confidence 0.5, VIDEO mode, frames fed
 *    un-mirrored), one person, or two when the plate's planned pose is a couple template (the app runs the detector
 *    with the guide's person count, CameraViewModel.observeDetectorConfig). Skipped when the detection is newer than
 *    the plate (or with --skip-detect).
 * 2. The pose-core test PromoFootageLandmarks (Kotlin, the app's own code) converts each frame with
 *    LandmarkMapping.toScene (scene = the 9:16 plate) and runs the app's LandmarkSmoother (One Euro filter per joint,
 *    exponential visibility, CameraViewModel.onDetection) over the plate's 30 fps timestamps, and writes
 *    public/footage/<plate>.landmarks.json (committed): per plate frame the 33 raw landmarks and the smoothed app
 *    skeleton the overlay draws and the matcher reads.
 */
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const REPO = path.resolve(ROOT, "..");
const PLATES = path.join(ROOT, "public", "footage", "plates.json");
const MANIFEST = path.join(ROOT, "footage", "manifest.json");
const DETECT_DIR = path.join(ROOT, "out", "footage");
const PY = path.join(ROOT, ".venv", "bin", "python");

const args = process.argv.slice(2);
const flag = (n) => args.includes(`--${n}`);
const opt = (n) => {
  const i = args.indexOf(`--${n}`);
  return i >= 0 ? args[i + 1] : null;
};
const only = opt("only")?.split(",").map((s) => s.trim()).filter(Boolean) ?? null;

if (!fs.existsSync(PLATES)) throw new Error("public/footage/plates.json is missing: run npm run footage first");
const plates = JSON.parse(fs.readFileSync(PLATES, "utf8")).plates;
const manifest = JSON.parse(fs.readFileSync(MANIFEST, "utf8"));
const ids = Object.keys(plates).filter((id) => !only || only.some((o) => id === o || plates[id].shot === o));
if (!ids.length) throw new Error(`no plate matches ${only}`);

/** People to look for: the guide's person count (2 for a couple template, else 1). */
const numPoses = (id) => {
  const p = plates[id];
  const shot = manifest.shots.find((s) => s.id === p.shot);
  let pose = shot?.pose?.planned ?? shot?.guide?.poseId ?? "";
  if (p.alternateOf) {
    const k = Number(id.split("-alt")[1]) - 1;
    pose = shot?.alternates?.[k]?.pose ?? "";
  }
  return /couple/i.test(pose) ? 2 : 1;
};

fs.mkdirSync(DETECT_DIR, { recursive: true });
if (!flag("skip-detect")) {
  if (!fs.existsSync(PY)) throw new Error("promo/.venv is missing: python3 -m venv promo/.venv && promo/.venv/bin/pip install mediapipe opencv-python-headless numpy");
  for (const id of ids) {
    const video = path.join(ROOT, "public", "footage", `${id}.mp4`);
    const out = path.join(DETECT_DIR, `${id}.detect.json`);
    if (!fs.existsSync(video)) throw new Error(`${path.relative(ROOT, video)} is missing: run npm run footage`);
    const n = numPoses(id);
    if (!flag("force") && fs.existsSync(out) && fs.statSync(out).mtimeMs > fs.statSync(video).mtimeMs) {
      const prev = JSON.parse(fs.readFileSync(out, "utf8"));
      if (prev.num_poses === n && prev.frames === plates[id].frames) {
        console.log(`detect ${id.padEnd(10)} up to date`);
        continue;
      }
    }
    const t0 = Date.now();
    const r = spawnSync(PY, [path.join(ROOT, "scripts", "detect.py"), video, "-o", out, "--model", "lite", "--num-poses", String(n)],
      { encoding: "utf8", env: { ...process.env, GLOG_minloglevel: "2", TF_CPP_MIN_LOG_LEVEL: "2" } });
    if (r.status !== 0) throw new Error(`detect.py ${id} failed:\n${r.stderr}`);
    const brief = JSON.parse(r.stdout.trim().split("\n").pop());
    console.log(`detect ${id.padEnd(10)} ${n} pose(s)  detected ${brief.detection_rate}  full body ${brief.full_body_rate}  ${((Date.now() - t0) / 1000).toFixed(1)} s`);
  }
}

// 1b. the act III guide's defining frame through the app's "pose from a photo" detection (full model, a single image:
// CreateFromPhotoViewModel.detect -> PoseDetector.detectStill), for PromoFootageMatch's photo-guide comparison.
// A one-frame video in VIDEO mode is a fresh detection with no tracking, the same as IMAGE mode.
if (!flag("skip-detect")) {
  for (const shot of manifest.shots) {
    const t = shot.guide?.definingFrame?.sourceTime;
    if (t == null) continue;
    const plateId = plates[`${shot.id}.1x`] ? `${shot.id}.1x` : shot.id;
    if (!ids.includes(plateId) && !ids.includes(shot.id)) continue;
    const plate = plates[plateId];
    const target = Math.round(t * plate.source.fps);
    let n = 0;
    plate.sourceFrames.forEach((j, k) => (Math.abs(j - target) < Math.abs(plate.sourceFrames[n] - target) ? (n = k) : null));
    const still = path.join(DETECT_DIR, `${shot.id}.guide-frame.mp4`);
    const r1 = spawnSync("ffmpeg", ["-v", "error", "-y", "-i", path.join(ROOT, "public", "footage", `${plateId}.mp4`), "-vf", `select=eq(n\\,${n})`,
      "-frames:v", "1", "-c:v", "libx264", "-crf", "10", "-pix_fmt", "yuv420p", "-an", still], { encoding: "utf8" });
    if (r1.status !== 0) throw new Error(`guide frame ${shot.id}: ${r1.stderr}`);
    const out = path.join(ROOT, "public", "footage", `${shot.id}.guide-photo.json`);
    const r2 = spawnSync(PY, [path.join(ROOT, "scripts", "detect.py"), still, "-o", out, "--model", "full", "--num-poses", "1"],
      { encoding: "utf8", env: { ...process.env, GLOG_minloglevel: "2" } });
    if (r2.status !== 0) throw new Error(`detect.py guide frame ${shot.id}: ${r2.stderr}`);
    const d = JSON.parse(fs.readFileSync(out, "utf8"));
    d.source = `public/footage/${plateId}.mp4 plate frame ${n} (source frame ${plate.sourceFrames[n]}, ${t} s)`;
    d.plate = plateId;
    d.plateFrame = n;
    fs.writeFileSync(out, JSON.stringify(d));
    console.log(`guide photo ${shot.id}: plate ${plateId} frame ${n}, full model, ${d.frames_data[0].people.length} person`);
  }
}

// 2. the app's mapping and smoothing, in Kotlin (every plate with a detection: it takes a moment, so no filter)
const gradle = spawnSync("./gradlew", ["--console=plain", "-q", ":pose-core:test", "--tests", "com.poseit.app.tools.PromoFootageLandmarks", "--rerun"],
  { cwd: REPO, stdio: "inherit" });
if (gradle.status !== 0) throw new Error("PromoFootageLandmarks failed");
for (const id of ids) {
  const f = path.join(ROOT, "public", "footage", `${id}.landmarks.json`);
  const d = JSON.parse(fs.readFileSync(f, "utf8"));
  console.log(`landmarks ${id.padEnd(10)} ${d.frames.length} f  detected ${d.summary.detectionRate}  people max ${d.summary.peopleMax}`);
}
