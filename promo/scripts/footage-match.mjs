#!/usr/bin/env node
/**
 * footage-match.mjs: the app's matcher on the footage, and the live shot's time remap set from its verdicts.
 *
 *   npm run footage-match        (after npm run footage and npm run landmarks)
 *
 * 1. Runs the app-module test PromoFootageMatch (the app's PoseMatcher, HintStabilizer and CaptureCoach over every
 *    plate's smoothed landmarks): public/footage/nearest.json and public/footage/<plate>.match.json.
 * 2. The live shot (manifest b5-live, timeRemap.rule): real time from the shot's first frame until the countdown starts
 *    (the app's hold frame + 700 ms, read from the real-time plate b5-live.1x with the guide and strictness the
 *    acceptance procedure chose), then eased to 0.5x over 10 frames and held at 0.5x until the capture (countdown
 *    start + 90 frames). If those keys differ from the manifest's timeRemap.measured, they are written there and the
 *    remapped plate, its landmarks and the match are rebuilt (npm run footage / landmarks / the test, b5-live only).
 * 3. Prints the acceptance checks for act III (capture in the manifest window, a hint card before the lock).
 */
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const REPO = path.resolve(ROOT, "..");
const MANIFEST = path.join(ROOT, "footage", "manifest.json");
const FOOTAGE = path.join(ROOT, "public", "footage");
const FPS = 30;
const RAMP_FRAMES = 10; // "eased to 0.5x over 10 f"
const HOLD_RATE = 0.5;
const COUNTDOWN_FRAMES = 90; // the app's 3 s countdown at 30 fps

const sh = (cmd, args, cwd = ROOT) => {
  const r = spawnSync(cmd, args, { cwd, stdio: "inherit" });
  if (r.status !== 0) throw new Error(`${cmd} ${args.join(" ")} failed`);
};
const test = () =>
  sh("./gradlew", ["--console=plain", "-q", ":app:testDebugUnitTest", "--tests", "com.poseit.app.promo.PromoFootageMatch", "--rerun"], REPO);
const read = (f) => JSON.parse(fs.readFileSync(f, "utf8"));
const r4 = (v) => Math.round(v * 1e4) / 1e4;

/**
 * Writes b5-live's timeRemap.measured into the manifest as text (inserted before "rule", or replacing the previous
 * one), so the rest of the director's file keeps its exact formatting.
 */
const writeMeasured = (measured) => {
  const text = fs.readFileSync(MANIFEST, "utf8");
  const at = text.indexOf('"id": "b5-live"');
  const remap = text.indexOf('"timeRemap": {', at);
  const rule = text.indexOf('"rule":', remap);
  if (at < 0 || remap < 0 || rule < 0) throw new Error("manifest: b5-live timeRemap not found");
  const indent = text.slice(text.lastIndexOf("\n", rule) + 1, rule);
  const block = `"measured": ${JSON.stringify(measured, null, 2).replace(/\n/g, `\n${indent}`)},\n${indent}`;
  const head = text.slice(0, rule);
  const existing = head.lastIndexOf('"measured": {');
  const start = existing > remap ? existing : rule;
  fs.writeFileSync(MANIFEST, text.slice(0, start) + block + text.slice(rule));
  JSON.parse(fs.readFileSync(MANIFEST, "utf8")); // still valid
};

test();

const manifest = read(MANIFEST);
const shot = manifest.shots.find((s) => s.id === "b5-live");
const oneX = path.join(FOOTAGE, "b5-live.1x.match.json");
if (shot && fs.existsSync(oneX)) {
  const m1 = read(oneX);
  const chosen = m1.attempts?.find((a) => a.chosen) ?? null;
  const C = chosen?.events?.countdown?.["3"] ?? null;
  if (C == null) {
    console.warn("b5-live: no attempt locks on the real-time plate (no countdown): the time remap stays as it is");
  } else {
    const f0 = shot.film[0];
    const s0 = shot.source.in;
    const sC = s0 + (C - f0) / FPS;
    const sRamp = sC + (RAMP_FRAMES * (1 + HOLD_RATE)) / 2 / FPS; // linear 1x -> 0.5x over the ramp
    const sCap = sRamp + ((COUNTDOWN_FRAMES - RAMP_FRAMES) * HOLD_RATE) / FPS;
    const keys = [[f0, r4(s0)], [C, r4(sC)], [C + RAMP_FRAMES, r4(sRamp)], [C + COUNTDOWN_FRAMES, r4(sCap)]];
    const current = shot.timeRemap?.measured?.keys;
    const same = current && current.length === keys.length && current.every((k, i) => k[0] === keys[i][0] && Math.abs(k[1] - keys[i][1]) < 1e-4);
    if (!same) {
      shot.timeRemap.measured = {
        keys,
        from: {
          plate: "b5-live.1x",
          guide: chosen.poseId,
          strictness: chosen.threshold,
          hold: chosen.events.hold,
          countdown: C,
          note: "written by scripts/footage-match.mjs: real time to the app's countdown start, 1x -> 0.5x over 10 f, 0.5x to the capture",
        },
      };
      writeMeasured(shot.timeRemap.measured);
      console.log(`b5-live: time remap set from the app's countdown start f${C}: ${JSON.stringify(keys)} (planned ${JSON.stringify(shot.timeRemap.planned)})`);
      sh("node", ["scripts/fetch-footage.mjs", "--only", "b5-live"]);
      sh("node", ["scripts/landmarks.mjs", "--only", "b5-live"]);
      test();
    } else {
      console.log(`b5-live: time remap up to date ${JSON.stringify(keys)}`);
    }
  }

  // acceptance (manifest b5-live.acceptance)
  const live = read(path.join(FOOTAGE, "b5-live.match.json"));
  const e = live.events;
  const lock = e.countdown?.["3"] ?? null;
  const cardsBefore = e.cards.filter((c) => lock != null && c.f < lock && c.kind !== "PERFECT");
  console.log(`b5-live: guide ${live.guide.poseId} at strictness ${live.coach.threshold}; found f${e.found}; into match ${e.crossings.filter((c) => c.match).map((c) => "f" + c.f).join(", ") || "never"}`);
  console.log(`b5-live: cards ${e.cards.map((c) => `f${c.f} "${c.text}"`).join("; ")}`);
  console.log(`b5-live: hold f${e.hold}, countdown 3/2/1 f${e.countdown?.["3"]}/f${e.countdown?.["2"]}/f${e.countdown?.["1"]}, capture f${e.capture}`);
  const ok = e.capture != null && e.capture >= 505 && e.capture <= 531 && cardsBefore.length > 0;
  console.log(`b5-live acceptance: ${ok ? "PASS" : "FAIL"} (capture in f505-531: ${e.capture ?? "none"}; hint cards before the lock: ${cardsBefore.length})`);
}

if (fs.existsSync(path.join(FOOTAGE, "nearest.json"))) {
  const near = read(path.join(FOOTAGE, "nearest.json"));
  for (const s of near.snaps) {
    const pick = s.pick ? `${s.pick.poseId}${s.pick.mirrored ? " (flipped)" : ""} ${s.pick.score.toFixed(3)}` : `none (best ${s.ranking[0]?.poseId ?? "-"} ${s.ranking[0]?.score?.toFixed(3) ?? "-"})`;
    const planned = s.planned.score != null ? `${s.planned.score.toFixed(3)} rank ${s.planned.rank}` : "not ranked";
    console.log(`snap ${s.plate.padEnd(8)} t ${s.frame.sourceTime.toFixed(2)} s  people ${s.peopleDetected}/${s.peopleNeeded}  pick ${pick}  planned ${s.planned.poseId} ${planned}`);
  }
}
