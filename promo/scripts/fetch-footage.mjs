#!/usr/bin/env node
/**
 * fetch-footage.mjs: downloads the stock clips in footage/manifest.json and prepares one film-ready plate per shot.
 *
 *   npm run footage                       every clip and every plate (skips what is already up to date)
 *   node scripts/fetch-footage.mjs --only b5-live,c9 [--force] [--size 1080x1920] [--no-download]
 *
 * Clips: each manifest clip's file URL -> public/footage/src/<catalogue>.mp4 (gitignored; never committed).
 *
 * Plates: public/footage/<plate>.mp4 (gitignored). 30 fps constant frame rate, H.264 yuv420p (BT.709 tags), a
 * keyframe every 15 frames, no audio, the manifest's 9:16 crop scaled to 1080x1920 (lanczos). Every plate is in FILM
 * time with a 15-frame (0.5 s) handle at each end:
 *
 *     plate frame n  =  global film frame (shot.film[0] - 15 + n)
 *
 * and shows source frame j(n) = floor(S(f) * sourceFps), clamped to the clip, where S(f) is the source second the
 * manifest puts on film frame f: in + rate * (f - film[0]) / 30 for real-time shots, the time-remap keys (measured,
 * else planned; linear between keys, held before the first and after the last, as src/footage/manifest.ts
 * sourceTimeAt) for the live shot.
 * 24 -> 30 fps is plain frame repetition, never interpolation. For a real-time shot this is exactly manifest.ts
 * plateFrameAt (plate frame = f - film[0] + 15). Chorus plates run on past the shot to cover the in-point window, so
 * the matcher can look at every candidate key time.
 *
 * Variants and alternates:
 *   <shot>.1x       a shot with a time remap also gets a real-time plate (same window, rate 1): the matcher runs on it
 *                   first to measure the hold frame the remap is set from (manifest b5-live timeRemap.rule)
 *   <shot>-alt<k>   a shot's alternates that name their own crop (or reuse the shot's clip), same film window, the
 *                   in-point moved so the alternate's keyTime sits where the shot's keyTime does
 *
 * public/footage/plates.json (committed) records every plate: its film window, frame count, the source frame shown
 * on each plate frame, crop, size and a hash of the parameters. A plate is rebuilt only when that hash changes, the
 * file is missing or its frame count is wrong (or with --force).
 */
import { spawn, spawnSync } from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const MANIFEST = path.join(ROOT, "footage", "manifest.json");
const OUT_DIR = path.join(ROOT, "public", "footage");
const SRC_DIR = path.join(OUT_DIR, "src");
const PLATES = path.join(OUT_DIR, "plates.json");
const FPS = 30;
export const HANDLE = 15; // frames; src/footage/manifest.ts PLATE_HANDLE = 0.5 s
const VERSION = 1; // bump to rebuild every plate after a change to the encoding below

// ------------------------------------------------------------------------------------------------------ arguments
const args = process.argv.slice(2);
const flag = (name) => args.includes(`--${name}`);
const opt = (name, dflt) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 && args[i + 1] ? args[i + 1] : dflt;
};
const only = opt("only", null)?.split(",").map((s) => s.trim()).filter(Boolean) ?? null;
const force = flag("force");
const noDownload = flag("no-download");
const [OUT_W, OUT_H] = opt("size", "1080x1920").split("x").map(Number);
if (!(OUT_W > 0 && OUT_H > 0) || Math.abs(OUT_W / OUT_H - 9 / 16) > 1e-3) throw new Error("--size must be 9:16, e.g. 1080x1920");

const manifest = JSON.parse(fs.readFileSync(MANIFEST, "utf8"));
fs.mkdirSync(SRC_DIR, { recursive: true });

// --------------------------------------------------------------------------------------------------------- helpers
const run = (cmd, argv, opts = {}) => {
  const r = spawnSync(cmd, argv, { encoding: "utf8", maxBuffer: 1 << 26, ...opts });
  if (r.status !== 0) throw new Error(`${cmd} ${argv.join(" ")}\n${r.stderr || r.stdout}`);
  return r.stdout;
};

const probe = (file) => {
  const j = JSON.parse(
    run("ffprobe", ["-v", "error", "-select_streams", "v:0", "-count_packets", "-show_entries",
      "stream=width,height,r_frame_rate,avg_frame_rate,nb_read_packets,nb_frames,pix_fmt,duration,start_time,sample_aspect_ratio",
      "-of", "json", file]),
  );
  const s = j.streams[0];
  const rate = (r) => {
    const [a, b] = r.split("/").map(Number);
    return b ? a / b : a;
  };
  return {
    width: s.width,
    height: s.height,
    fps: rate(s.avg_frame_rate !== "0/0" ? s.avg_frame_rate : s.r_frame_rate),
    rFps: rate(s.r_frame_rate),
    frames: Number(s.nb_read_packets ?? s.nb_frames),
    pixFmt: s.pix_fmt,
    duration: Number(s.duration),
    start: Number(s.start_time ?? 0),
    sar: s.sample_aspect_ratio ?? "1:1",
  };
};

const download = (clip) => {
  const file = path.join(SRC_DIR, `${clip.catalogue}.mp4`);
  if (fs.existsSync(file) && fs.statSync(file).size > 0) {
    try {
      probe(file);
      return file;
    } catch {
      fs.rmSync(file); // a truncated download: fetch it again
    }
  }
  if (noDownload) throw new Error(`missing ${path.relative(ROOT, file)} and --no-download given`);
  const tmp = `${file}.part`;
  console.log(`download ${clip.catalogue}  ${clip.file}`);
  run("curl", ["-L", "--fail", "--silent", "--show-error", "--retry", "3", "--retry-delay", "2", "-o", tmp, clip.file], {
    stdio: ["ignore", "inherit", "inherit"],
  });
  fs.renameSync(tmp, file);
  probe(file);
  return file;
};

/** S(f): the source second on global film frame f (src/footage/manifest.ts sourceTimeAt, extended past the shot). */
const sourceTimeFn = (shot, { realTime = false, inPoint = shot.source.in } = {}) => {
  const keys = !realTime ? (shot.timeRemap?.measured?.keys ?? shot.timeRemap?.planned) : null;
  if (keys && keys.length > 1) {
    return (f) => {
      if (f <= keys[0][0]) return keys[0][1];
      for (let i = 1; i < keys.length; i++) {
        const [f1, s1] = keys[i];
        const [f0, s0] = keys[i - 1];
        if (f <= f1) return s0 + ((s1 - s0) * (f - f0)) / (f1 - f0);
      }
      return keys[keys.length - 1][1];
    };
  }
  const rate = shot.rate ?? 1;
  return (f) => inPoint + (rate * (f - shot.film[0])) / FPS;
};

/** Every plate to build: the manifest shots, their real-time variants and their prepared alternates. */
const planPlates = () => {
  const plates = [];
  for (const shot of manifest.shots) {
    const clip = manifest.clips[shot.clip];
    if (!clip) throw new Error(`shot ${shot.id}: no clip ${shot.clip}`);
    const filmLen = shot.film[1] - shot.film[0];
    // run on past the shot so the whole in-point window is covered (chorus key-time search)
    const rate = shot.rate ?? 1;
    const windowLen = shot.source.window ? Math.ceil(((shot.source.window[1] - shot.source.in) * FPS) / rate) : 0;
    const len = Math.max(filmLen, windowLen);
    const base = { shot: shot.id, clip: shot.clip, film: shot.film, crop: shot.crop, len };
    if (shot.timeRemap?.planned) {
      const keys = shot.timeRemap.measured?.keys ?? shot.timeRemap.planned;
      plates.push({ ...base, id: shot.id, variant: "remap", S: sourceTimeFn(shot), remap: keys });
      plates.push({ ...base, id: `${shot.id}.1x`, variant: "1x", S: sourceTimeFn(shot, { realTime: true }) });
    } else {
      plates.push({ ...base, id: shot.id, variant: null, S: sourceTimeFn(shot), inPoint: shot.source.in });
    }
    (shot.alternates ?? []).forEach((alt, k) => {
      const crop = alt.crop ?? (alt.clip === shot.clip ? shot.crop : null);
      if (!crop || alt.keyTime == null || shot.keyTime == null) return;
      const inPoint = Math.max(0, alt.keyTime - (shot.keyTime - shot.source.in));
      plates.push({
        ...base,
        id: `${shot.id}-alt${k + 1}`,
        clip: alt.clip,
        crop,
        variant: "alternate",
        alternateOf: shot.id,
        keyTime: alt.keyTime,
        inPoint,
        len: filmLen,
        S: sourceTimeFn(shot, { inPoint }),
      });
    });
  }
  return plates;
};

const writeAll = (stream, buf) =>
  new Promise((resolve, reject) => {
    const ok = stream.write(buf, (err) => (err ? reject(err) : null));
    if (ok) resolve();
    else stream.once("drain", resolve);
  });

/**
 * Decodes the clip (crop + scale, every decoded frame passed through) and writes the plate's frames in order,
 * repeating source frames as the frame map says. Only one or two raw frames are held in memory.
 */
const encodePlate = async (srcFile, src, plate, map, outFile) => {
  const { x, y, w, h } = plate.crop;
  const maxJ = Math.max(...map);
  const frameBytes = (OUT_W * OUT_H * 3) / 2;
  const vf = `crop=${w}:${h}:${x}:${y},scale=${OUT_W}:${OUT_H}:flags=lanczos+accurate_rnd+full_chroma_int,setsar=1`;
  const dec = spawn("ffmpeg", ["-v", "error", "-i", srcFile, "-map", "0:v:0", "-vf", vf, "-fps_mode", "passthrough",
    "-frames:v", String(maxJ + 1), "-f", "rawvideo", "-pix_fmt", "yuv420p", "pipe:1"], { stdio: ["ignore", "pipe", "pipe"] });
  const enc = spawn("ffmpeg", ["-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "yuv420p", "-s", `${OUT_W}x${OUT_H}`,
    "-framerate", String(FPS), "-i", "pipe:0", "-an", "-c:v", "libx264", "-preset", "slow", "-crf", "14",
    "-pix_fmt", "yuv420p", "-g", "15", "-keyint_min", "15", "-sc_threshold", "0",
    "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709", "-color_range", "tv",
    "-movflags", "+faststart", "-f", "mp4", outFile], { stdio: ["pipe", "ignore", "pipe"] });
  let decErr = "";
  let encErr = "";
  dec.stderr.on("data", (d) => (decErr += d));
  enc.stderr.on("data", (d) => (encErr += d));
  const encDone = new Promise((resolve, reject) =>
    enc.on("close", (code) => (code === 0 ? resolve() : reject(new Error(`encoder failed (${code}): ${encErr}`)))));

  let n = 0;
  let j = 0;
  let pending = Buffer.alloc(0);
  let last = null;
  for await (const chunk of dec.stdout) {
    pending = pending.length ? Buffer.concat([pending, chunk]) : chunk;
    while (pending.length >= frameBytes) {
      const frame = Buffer.from(pending.subarray(0, frameBytes));
      pending = pending.subarray(frameBytes);
      while (n < map.length && map[n] === j) {
        await writeAll(enc.stdin, frame);
        n++;
      }
      last = frame;
      j++;
    }
  }
  await new Promise((resolve) => (dec.exitCode !== null ? resolve() : dec.on("close", resolve)));
  if (dec.exitCode !== 0) throw new Error(`decoder failed (${dec.exitCode}): ${decErr}`);
  if (!last) throw new Error(`no frames decoded from ${srcFile}`);
  // the clip ended before the map did (clamped to its last frame)
  while (n < map.length) {
    await writeAll(enc.stdin, last);
    n++;
  }
  enc.stdin.end();
  await encDone;
  return { decoded: j };
};

// ------------------------------------------------------------------------------------------------------------ main
const previous = fs.existsSync(PLATES) ? JSON.parse(fs.readFileSync(PLATES, "utf8")) : { plates: {} };
const record = {
  about:
    "Prepared plates (public/footage/<id>.mp4, gitignored), written by scripts/fetch-footage.mjs from footage/manifest.json. " +
    "Plate frame n is global film frame filmFrom + n (30 fps, a 15-frame handle each side of the shot) and shows source frame " +
    "sourceFrames[n] of the clip (frame repetition, no interpolation). Landmarks and matches are computed on these exact frames.",
  version: VERSION,
  fps: FPS,
  handle: HANDLE,
  width: OUT_W,
  height: OUT_H,
  plates: {},
};

const plates = planPlates();
const wanted = only ? plates.filter((p) => only.some((o) => p.id === o || p.shot === o)) : plates;
if (only && !wanted.length) throw new Error(`--only ${only.join(",")}: no such plate`);

// keep the records of plates this run does not touch
for (const p of plates) if (previous.plates?.[p.id] && !wanted.includes(p)) record.plates[p.id] = previous.plates[p.id];

const clipsNeeded = new Set(only ? wanted.map((p) => p.clip) : Object.keys(manifest.clips));
const files = {};
for (const key of clipsNeeded) files[key] = download(manifest.clips[key]);

for (const plate of wanted) {
  const clip = manifest.clips[plate.clip];
  const srcFile = files[plate.clip];
  const src = probe(srcFile);
  if (src.sar !== "1:1" && src.sar !== "0:1" && src.sar !== "N/A") console.warn(`${plate.id}: source SAR ${src.sar}`);
  const filmFrom = plate.film[0] - HANDLE;
  const frames = plate.len + 2 * HANDLE;
  const times = [];
  const map = [];
  for (let n = 0; n < frames; n++) {
    const S = plate.S(filmFrom + n);
    times.push(S);
    map.push(Math.min(src.frames - 1, Math.max(0, Math.floor(S * src.fps + 1e-6))));
  }
  const params = {
    VERSION, OUT_W, OUT_H, file: clip.file, crop: plate.crop, map,
    srcFrames: src.frames, srcFps: src.fps,
  };
  const hash = crypto.createHash("sha1").update(JSON.stringify(params)).digest("hex").slice(0, 12);
  const outFile = path.join(OUT_DIR, `${plate.id}.mp4`);
  const prev = previous.plates?.[plate.id];
  let upToDate = !force && prev?.hash === hash && fs.existsSync(outFile);
  if (upToDate) {
    try {
      const p = probe(outFile);
      upToDate = p.frames === frames && p.width === OUT_W && p.height === OUT_H;
    } catch {
      upToDate = false;
    }
  }
  if (!upToDate) {
    const t0 = Date.now();
    const tmp = `${outFile}.part.mp4`;
    await encodePlate(srcFile, src, plate, map, tmp);
    const p = probe(tmp);
    if (p.frames !== frames) throw new Error(`${plate.id}: wrote ${p.frames} frames, expected ${frames}`);
    if (Math.abs(p.rFps - FPS) > 1e-3) throw new Error(`${plate.id}: ${p.rFps} fps`);
    fs.renameSync(tmp, outFile);
    console.log(`plate ${plate.id.padEnd(10)} ${frames} f  film ${filmFrom}-${filmFrom + frames - 1}  source ${times[0].toFixed(3)}-${times[frames - 1].toFixed(3)} s  ${((Date.now() - t0) / 1000).toFixed(1)} s`);
  } else {
    console.log(`plate ${plate.id.padEnd(10)} up to date`);
  }
  const film0 = plate.film[0];
  record.plates[plate.id] = {
    shot: plate.shot,
    clip: plate.clip,
    catalogue: clip.catalogue,
    variant: plate.variant,
    ...(plate.alternateOf ? { alternateOf: plate.alternateOf, keyTime: plate.keyTime } : {}),
    film: plate.film,
    filmFrom,
    frames,
    /** plate frame of the shot's first film frame */
    shotStart: film0 - filmFrom,
    crop: plate.crop,
    cropScale: Math.round((OUT_H / plate.crop.h) * 1e4) / 1e4,
    source: {
      fps: Math.round(src.fps * 1e5) / 1e5,
      frames: src.frames,
      size: [src.width, src.height],
      ...(plate.remap ? { remap: plate.remap } : { in: plate.inPoint ?? null }),
    },
    sourceFrames: map,
    hash,
  };
}

// stable key order: manifest order
const ordered = {};
for (const p of plates) if (record.plates[p.id]) ordered[p.id] = record.plates[p.id];
record.plates = ordered;
fs.writeFileSync(PLATES, JSON.stringify(record, null, 1).replace(/\[\n\s+([\d.,\s-]+?)\n\s+\]/g, (_, s) => `[${s.replace(/\s+/g, "")}]`) + "\n");
console.log(`plates.json: ${Object.keys(ordered).length} plates`);
