#!/usr/bin/env node
/**
 * gen-audio.mjs: the Poseit promo's sound, synthesized from scratch in plain Node. No samples, no network, no clock:
 * every noise source is a seeded PRNG, so the same events.json gives byte-identical WAVs on every run.
 *
 *   npm run audio                       (= npm run events && node scripts/gen-audio.mjs)
 *   node scripts/gen-audio.mjs [--events out/events.json] [--out public/audio] [--no-vo]
 *
 * Reads   out/events.json: the picture's frames by name (src/events.ts CUES) and the voice-over slot
 *         (src/audio/voiceover.ts). Every sound below is placed from those frames, never from a typed number, so a
 *         re-timed picture moves its sound with it. Optional: public/audio/vo/lines.index.json + lineNN.wav (the
 *         ElevenLabs voice-over from motionmaxxing's voice.py split; see README.md). When present it is placed on its
 *         frames, the music ducks under it and the mix is re-normalised. Nothing fakes a voice when it is absent.
 * Writes  public/audio/sfx/*.wav          the SFX kit, one file per sound, peak -3 dBFS (stereo, 48 kHz, 16-bit)
 *         public/audio/stems/music.wav    the score at the film's length, section levels baked in, before the VO duck
 *         public/audio/stems/sfx.wav      every SFX on its frame at mix level
 *         public/audio/mix.wav            the soundtrack Soundtrack.tsx plays: -14 LUFS integrated, true peak <= -1.5 dBTP
 *         public/audio/cues.json          the SFX placements [{f, t, sfx, gain, label}] (motionmaxxing mix.py --events format)
 *
 * The sound design is sound-plan.md; this file is its implementation. Three hand-placed hits (the unison, the lock,
 * the shutter), each with something cleared before it; a score on the film's 16-frame beat (112.5 BPM) that enters
 * on the f80 cut, builds through the chorus, stops dead for the drop, lands on the unison, thins to almost nothing
 * under the coaching cards, accelerates with the countdown, stops for half a beat and resolves on one chord.
 */
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// ------------------------------------------------------------------------------------------------ setup
const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const argv = process.argv.slice(2);
const opt = (name, def) => (argv.includes(name) ? argv[argv.indexOf(name) + 1] : def);
const EVENTS_PATH = resolve(ROOT, opt("--events", "out/events.json"));
const OUT = resolve(ROOT, opt("--out", "public/audio"));
const USE_VO = !argv.includes("--no-vo");

if (!existsSync(EVENTS_PATH)) {
  console.error(`gen-audio: ${relative(ROOT, EVENTS_PATH)} not found. Run "npm run events" first (or just "npm run audio").`);
  process.exit(2);
}
const EV = JSON.parse(readFileSync(EVENTS_PATH, "utf8"));
const C = EV.cues;
if (!C) {
  console.error("gen-audio: events.json has no `cues` block; re-run npm run events with the current src/events.ts.");
  process.exit(2);
}

const SR = 48000;
const TARGET_LUFS = -14;
const CEILING_DBTP = -1.5; // -1 dBTP delivery, 0.5 dB kept for the AAC encode
const SPF = SR / C.fps; // samples per frame: 1600
const N = C.frames * SPF; // the film, sample-exact (664 f = 1,062,400 samples)
const S = (f) => Math.round(f * SPF); // frame (may be fractional) -> sample
const sec = (s) => Math.round(s * SR);
const dB = (g) => Math.pow(10, g / 20);
const BEAT = C.grid.beat; // 16 f

// ------------------------------------------------------------------------------------------------ deterministic noise
const hashStr = (s) => {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619) >>> 0;
  return h;
};
/** mulberry32, one named stream per sound, so adding a sound never changes another one's noise */
const rng = (name) => {
  let a = hashStr(name);
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
};
const white = (r) => r() * 2 - 1;
const pinkGen = (r) => {
  let b0 = 0, b1 = 0, b2 = 0;
  return () => {
    const w = white(r);
    b0 = 0.99765 * b0 + w * 0.099046;
    b1 = 0.963 * b1 + w * 0.2965164;
    b2 = 0.57 * b2 + w * 1.0526913;
    return (b0 + b1 + b2 + w * 0.1848) * 0.22;
  };
};

// ------------------------------------------------------------------------------------------------ DSP blocks
class Biquad {
  constructor(type, f, q = 0.707, g = 0) {
    this.x1 = this.x2 = this.y1 = this.y2 = 0;
    this.set(type, f, q, g);
  }
  set(type, f, q = 0.707, g = 0) {
    f = Math.min(Math.max(f, 10), SR * 0.45);
    const w = (2 * Math.PI * f) / SR, c = Math.cos(w), al = Math.sin(w) / (2 * q);
    let b0, b1, b2, a0, a1, a2;
    if (type === "lp") { b0 = (1 - c) / 2; b1 = 1 - c; b2 = b0; a0 = 1 + al; a1 = -2 * c; a2 = 1 - al; }
    else if (type === "hp") { b0 = (1 + c) / 2; b1 = -(1 + c); b2 = b0; a0 = 1 + al; a1 = -2 * c; a2 = 1 - al; }
    else if (type === "bp") { b0 = al; b1 = 0; b2 = -al; a0 = 1 + al; a1 = -2 * c; a2 = 1 - al; }
    else { const A = Math.pow(10, g / 40); b0 = 1 + al * A; b1 = -2 * c; b2 = 1 - al * A; a0 = 1 + al / A; a1 = -2 * c; a2 = 1 - al / A; }
    this.b0 = b0 / a0; this.b1 = b1 / a0; this.b2 = b2 / a0; this.a1 = a1 / a0; this.a2 = a2 / a0;
  }
  run(x) {
    const y = this.b0 * x + this.b1 * this.x1 + this.b2 * this.x2 - this.a1 * this.y1 - this.a2 * this.y2;
    this.x2 = this.x1; this.x1 = x; this.y2 = this.y1; this.y1 = y;
    return y;
  }
}
const filt = (x, ...stages) => {
  const out = Float32Array.from(x);
  for (const st of stages) for (let i = 0; i < out.length; i++) out[i] = st.run(out[i]);
  return out;
};

/** A bank of decaying modes (2-pole resonators) driven by an excitation: how struck things ring. */
const resonate = (exc, modes) => {
  const out = new Float32Array(exc.length);
  for (const m of modes) {
    const r = Math.exp(-1 / (m.tau * SR)), w = (2 * Math.PI * m.f) / SR;
    const c1 = 2 * r * Math.cos(w), c2 = -r * r, gin = Math.sin(w) * (m.amp ?? 1);
    let y1 = 0, y2 = 0;
    for (let i = 0; i < exc.length; i++) {
      const y = c1 * y1 + c2 * y2 + exc[i] * gin;
      y2 = y1; y1 = y;
      out[i] += y;
    }
  }
  return out;
};
/** add a struck-noise burst (an impact) into an excitation buffer */
const strike = (exc, at, amp, tau, r) => {
  const n = Math.min(exc.length - at, Math.ceil(tau * SR * 8));
  for (let i = 0; i < n; i++) exc[at + i] += amp * white(r) * Math.exp(-i / (tau * SR));
};

const polyblep = (t, dt) => {
  if (t < dt) { t /= dt; return t + t - t * t - 1; }
  if (t > 1 - dt) { t = (t - 1) / dt; return t * t + t + t + 1; }
  return 0;
};

const NOTE = { C: 0, D: 2, E: 4, F: 5, G: 7, A: 9, B: 11 };
const hz = (n) => {
  const m = /^([A-G])(#|b)?(-?\d)$/.exec(n);
  const midi = 12 * (Number(m[3]) + 1) + NOTE[m[1]] + (m[2] === "#" ? 1 : m[2] === "b" ? -1 : 0);
  return 440 * Math.pow(2, (midi - 69) / 12);
};

const St = (n) => ({ L: new Float32Array(n), R: new Float32Array(n) });
const lengthOf = (b) => (b instanceof Float32Array ? b.length : b.L.length);
/** add src (mono or stereo) into the stereo dst at sample `at`, with gain and equal-power pan (-1..1) */
const mixInto = (dst, src, at, gain = 1, pan = 0) => {
  const gl = Math.cos(((pan + 1) * Math.PI) / 4) * Math.SQRT2, gr = Math.sin(((pan + 1) * Math.PI) / 4) * Math.SQRT2;
  const L = src instanceof Float32Array ? src : src.L, R = src instanceof Float32Array ? src : src.R;
  const n = dst.L.length;
  for (let i = 0; i < L.length; i++) {
    const j = at + i;
    if (j < 0) continue;
    if (j >= n) break;
    dst.L[j] += L[i] * gain * gl;
    dst.R[j] += R[i] * gain * gr;
  }
};

/**
 * A small room (Schroeder/Moorer: 8 damped combs + 4 allpasses per side, after Freeverb's topology; our own code).
 * Each comb's feedback is set from the RT60, so `rt60` is the decay time in seconds. Mono in, stereo wet out.
 */
const room = (input, { rt60 = 0.4, size = 0.7, damp = 0.35, tail = rt60 * 1.2 } = {}) => {
  const COMBS = [1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617], APS = [556, 441, 341, 225];
  const n = input.length + sec(tail);
  const out = St(n);
  const scale = (size * SR) / 44100;
  [0, 23].forEach((spread, ch) => {
    const dst = ch ? out.R : out.L;
    const combs = COMBS.map((d) => {
      const len = Math.max(8, Math.round((d + spread) * scale));
      return { buf: new Float32Array(len), i: 0, fb: Math.pow(10, (-3 * len) / (rt60 * SR)), st: 0 };
    });
    const aps = APS.map((d) => ({ buf: new Float32Array(Math.max(4, Math.round((d + spread) * scale))), i: 0 }));
    for (let k = 0; k < n; k++) {
      const x = (k < input.length ? input[k] : 0) * 0.03;
      let y = 0;
      for (const c of combs) {
        const o = c.buf[c.i];
        c.st = o * (1 - damp) + c.st * damp;
        c.buf[c.i] = x + c.st * c.fb;
        if (++c.i >= c.buf.length) c.i = 0;
        y += o;
      }
      for (const a of aps) {
        const b = a.buf[a.i];
        const o = b - y;
        a.buf[a.i] = y + b * 0.5;
        if (++a.i >= a.buf.length) a.i = 0;
        y = o;
      }
      dst[k] = y;
    }
  });
  return out;
};
/** dry (mono) + room at `wetDb`, as stereo */
const withRoom = (dry, wetDb, opts) => {
  const wet = room(dry, opts);
  const out = St(wet.L.length);
  mixInto(out, dry, 0);
  mixInto(out, wet, 0, dB(wetDb));
  return out;
};

const peakOf = (b) => {
  let p = 0;
  for (const ch of b instanceof Float32Array ? [b] : [b.L, b.R]) for (let i = 0; i < ch.length; i++) p = Math.max(p, Math.abs(ch[i]));
  return p;
};
const scaleBuf = (b, g) => {
  for (const ch of b instanceof Float32Array ? [b] : [b.L, b.R]) for (let i = 0; i < ch.length; i++) ch[i] *= g;
  return b;
};
/** every SFX file is peak-normalised to -3 dBFS so the mix gains in the cue table mean something */
const norm3 = (b) => scaleBuf(b, dB(-3) / (peakOf(b) || 1));
const toStereo = (b) => (b instanceof Float32Array ? { L: b, R: Float32Array.from(b) } : b);
/** a 2 ms fade at both ends: nothing in the kit starts or stops on a step */
const edges = (b, fadeIn = 0.0005, fadeOut = 0.004) => {
  for (const ch of b instanceof Float32Array ? [b] : [b.L, b.R]) {
    const a = sec(fadeIn), z = sec(fadeOut), n = ch.length;
    for (let i = 0; i < a && i < n; i++) ch[i] *= i / a;
    for (let i = 0; i < z && i < n; i++) ch[n - 1 - i] *= i / z;
  }
  return b;
};

// ------------------------------------------------------------------------------------------------ instruments (score)
/** soft, round four-on-the-floor kick: a sine falling 132 -> 44 Hz, saturated for warmth (and for phone speakers) */
const KICK_R = rng("kick-click");
const kickAt = [], kickVel = []; // for the bass sidechain
const kick = (dst, at, vel = 1) => {
  kickAt.push(at);
  kickVel.push(Math.min(1, vel / 0.82));
  const n = sec(0.45), y = new Float32Array(n), bp = new Biquad("bp", 3200, 0.8);
  let ph = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    ph += (2 * Math.PI * (44 + 88 * Math.exp(-t / 0.026))) / SR;
    const amp = Math.exp(-t / 0.17) * (1 - Math.exp(-t / 0.0007));
    y[i] = Math.tanh(1.7 * Math.sin(ph) * amp) / Math.tanh(1.7) + bp.run(white(KICK_R)) * Math.exp(-t / 0.0012) * 0.5;
  }
  mixInto(dst, edges(y), at, vel);
};

/** handclap: four bursts 9-10 ms apart and a short tail, band-passed; half of it goes to the room */
const CLAP_R = rng("clap");
const clap = (dst, send, at, vel = 1) => {
  const n = sec(0.32), y = new Float32Array(n);
  const bp = new Biquad("bp", 1250, 0.75), hp = new Biquad("hp", 650, 0.7);
  const bursts = [0, 0.0105, 0.0195, 0.029];
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    let e = 0;
    for (const o of bursts) if (t >= o) e = Math.max(e, Math.exp(-(t - o) / 0.0032));
    if (t >= 0.029) e = Math.max(e, 0.55 * Math.exp(-(t - 0.029) / 0.085));
    y[i] = hp.run(bp.run(white(CLAP_R))) * e * 3;
  }
  edges(y);
  mixInto(dst, y, at, vel);
  mixInto(send, y, at, vel * 0.5);
};

/** the hats are shutter-click transients: a tiny double strike through three bright modes */
const HAT_R = rng("hat");
const clickHat = (dst, at, vel = 1, pan = 0.25, tone = 1) => {
  const n = sec(0.045), e = new Float32Array(n);
  strike(e, 0, 1, 0.0004, HAT_R);
  strike(e, sec(0.003), 0.45, 0.0003, HAT_R);
  const y = resonate(e, [{ f: 4800 * tone, tau: 0.007 }, { f: 6900 * tone, tau: 0.005, amp: 0.8 }, { f: 9300 * tone, tau: 0.0035, amp: 0.6 }]);
  const hp = new Biquad("hp", 3000, 0.7);
  for (let i = 0; i < n; i++) y[i] += hp.run(white(HAT_R)) * Math.exp(-i / (0.004 * SR)) * 0.35;
  mixInto(dst, edges(scaleBuf(y, 1 / (peakOf(y) || 1))), at, vel, pan);
};

/** air crash for the downbeat after the drop: high-passed noise with a ringing top, half a second */
const CRASH_R = rng("crash");
const crash = (dst, at, vel = 1) => {
  const n = sec(0.9), y = new Float32Array(n), hp = new Biquad("hp", 4500, 0.7), pk = new Biquad("peak", 8200, 2, 6);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    y[i] = pk.run(hp.run(white(CRASH_R))) * Math.exp(-t / 0.32) * (1 - Math.exp(-t / 0.002));
  }
  const wide = St(n);
  mixInto(wide, edges(y), 0, 1, -0.2);
  const r2 = rng("crash-r"), hp2 = new Biquad("hp", 4500, 0.7);
  for (let i = 0; i < n; i++) wide.R[i] = 0.6 * wide.R[i] + 0.4 * hp2.run(white(r2)) * Math.exp(-i / SR / 0.32);
  mixInto(dst, wide, at, vel);
};

/** muted plucked bass: a polyBLEP saw + its sine through a closing low-pass, short decay, a little drive */
const bass = (dst, at, f, vel, len, { cutoff = 700, decay = 0.22, drive = 1.6 } = {}) => {
  const n = sec(len + 0.03), y = new Float32Array(n), lp = new Biquad("lp", cutoff * 2.6, 1.1);
  const dt = f / SR;
  let ph = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    if ((i & 15) === 0) lp.set("lp", cutoff * (0.45 + 2.2 * Math.exp(-t / 0.06)), 1.1);
    ph += dt;
    if (ph >= 1) ph -= 1;
    const saw = 2 * ph - 1 - polyblep(ph, dt);
    const x = lp.run(0.8 * saw) + 0.55 * Math.sin(2 * Math.PI * ph);
    const env = (1 - Math.exp(-t / 0.0025)) * Math.exp(-t / decay) * (t < len ? 1 : Math.max(0, 1 - (t - len) / 0.03));
    y[i] = (Math.tanh(drive * x * env) / Math.tanh(drive)) * vel;
  }
  mixInto(dst, y, at);
};

/** keys: a two-operator FM electric piano (the tine's bell on the attack), detuned a hair left/right */
const keysNote = (dst, at, f, vel, { len = 0.3, index = 1.2, tau = 1.0, rel = 0.07, phase = 0 } = {}) => {
  const n = sec(len + rel);
  const tauF = tau * Math.pow(220 / f, 0.3);
  [0.9988, 1.0012].forEach((det, ch) => {
    const out = ch ? dst.R : dst.L, fd = f * det;
    for (let i = 0; i < n; i++) {
      const j = at + i;
      if (j < 0 || j >= out.length) continue;
      const t = i / SR, w = 2 * Math.PI * fd * t + phase;
      const I = index * Math.exp(-t / 0.18) + 0.2;
      const tine = 0.12 * Math.sin(4 * w) * Math.exp(-t / 0.05);
      const env = (1 - Math.exp(-t / 0.0015)) * Math.exp(-t / tauF);
      const gate = t < len ? 1 : Math.pow(Math.max(0, 1 - (t - len) / rel), 2);
      out[j] += vel * (Math.sin(w + I * Math.sin(w)) + tine) * env * gate;
    }
  });
};
// each voice starts at its own (seeded) phase: voices that all start at sin(0) on one sample stack their attacks
const KEYS_PHASE = rng("keys-phase");
const chord = (dst, at, notes, vel, o) => notes.forEach((nm) => keysNote(dst, at, hz(nm), vel / Math.sqrt(notes.length), { ...o, phase: KEYS_PHASE() * 2 * Math.PI }));

// A minor with the dorian sixth (F#), so it stays cheeky rather than dark.
const AM9 = ["A2", "E3", "G3", "B3", "C4"];
const AM9_HI = ["E3", "A3", "C4", "G4", "B4"];
const D9 = ["D3", "F#3", "A3", "C4", "E4"];
const AM_ADD9 = ["A2", "E3", "B3", "C4", "E4"];

// ------------------------------------------------------------------------------------------------ the SFX kit
const KIT = {}; // name -> { buf (stereo), at (samples from file start to the transient / peak), about }

/** the chorus snap: a shutter-click transient, two micro-strikes 7 ms apart through a small plastic-and-metal body */
const sfxSnap = (pitch, name) => {
  const r = rng(name), n = sec(0.07), e = new Float32Array(n);
  strike(e, 0, 1, 0.0005, r);
  strike(e, sec(0.007), 0.5, 0.0004, r);
  const y = resonate(e, [
    { f: 2300 * pitch, tau: 0.012 }, { f: 3700 * pitch, tau: 0.008, amp: 0.8 },
    { f: 5600 * pitch, tau: 0.005, amp: 0.6 }, { f: 900 * pitch, tau: 0.01, amp: 0.4 },
  ]);
  const hp = new Biquad("hp", 2500, 0.7);
  for (let i = 0; i < n; i++) y[i] += hp.run(white(r)) * Math.exp(-i / (0.0025 * SR)) * 0.3;
  return toStereo(edges(y));
};

/**
 * THE shutter (hit 3), modelled on a camera's mechanical click (the app plays MediaActionSound.SHUTTER_CLICK):
 * mirror up + first curtain (a hard double strike and the body's thunk), then 68 ms later the second curtain and the
 * mirror's return (brighter, with the slap), a low thump for weight, a faint spring ting, a small room.
 */
const sfxShutter = () => {
  const r = rng("shutter"), n = sec(0.42);
  const e = new Float32Array(n);
  strike(e, 0, 1, 0.0005, r);
  strike(e, sec(0.004), 0.6, 0.0008, r);
  strike(e, sec(0.068), 0.85, 0.0005, r);
  strike(e, sec(0.071), 0.7, 0.0012, r);
  strike(e, sec(0.077), 0.3, 0.0005, r);
  const clack = resonate(e, [
    { f: 640, tau: 0.022, amp: 0.6 }, { f: 1350, tau: 0.018, amp: 0.9 }, { f: 2450, tau: 0.012 },
    { f: 3900, tau: 0.008, amp: 0.8 }, { f: 6100, tau: 0.005, amp: 0.6 },
  ]);
  const eLow = new Float32Array(n);
  strike(eLow, 0, 1, 0.002, r);
  strike(eLow, sec(0.068), 0.75, 0.0025, r);
  const thunk = resonate(eLow, [{ f: 180, tau: 0.03 }, { f: 310, tau: 0.02, amp: 0.7 }, { f: 470, tau: 0.015, amp: 0.5 }]);
  const eTing = new Float32Array(n);
  strike(eTing, sec(0.07), 1, 0.0004, r);
  const ting = resonate(eTing, [{ f: 3150, tau: 0.045 }, { f: 4720, tau: 0.03, amp: 0.6 }]);
  const y = new Float32Array(n);
  const pc = peakOf(clack), pt = peakOf(thunk), pg = peakOf(ting);
  let ph = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    ph += (2 * Math.PI * (48 + 47 * Math.exp(-t / 0.02))) / SR;
    const thump = Math.sin(ph) * Math.exp(-t / 0.05) * (1 - Math.exp(-t / 0.001));
    y[i] = clack[i] / pc + 0.6 * (thunk[i] / pt) + 0.55 * thump + 0.07 * (ting[i] / pg);
  }
  const dry = filt(y, new Biquad("hp", 40, 0.7));
  return edges(withRoom(dry, -12, { rt60: 0.42, size: 0.5, damp: 0.5 }));
};

/** the lock (hit 2): a latch seating (two ticks 16 ms apart) and a soft two-note confirmation, E5 then B5 */
const sfxLock = () => {
  const r = rng("lock"), n = sec(0.6), e = new Float32Array(n);
  strike(e, 0, 0.5, 0.0004, r);
  strike(e, sec(0.016), 1, 0.0006, r);
  const latch = resonate(e, [{ f: 1800, tau: 0.01 }, { f: 3200, tau: 0.007, amp: 0.8 }, { f: 5200, tau: 0.004, amp: 0.5 }, { f: 420, tau: 0.018, amp: 0.5 }]);
  const pl = peakOf(latch);
  const y = new Float32Array(n);
  for (let i = 0; i < n; i++) y[i] = (latch[i] / pl) * 0.8;
  for (const [f, t0] of [[hz("E5"), 0.016], [hz("B5"), 0.071]]) {
    for (let i = sec(t0); i < n; i++) {
      const t = i / SR - t0, w = 2 * Math.PI * f * t;
      const env = (1 - Math.exp(-t / 0.003)) * Math.exp(-t / 0.2);
      y[i] += 0.42 * env * (Math.sin(w) + 0.22 * Math.sin(2 * w) * Math.exp(-t / 0.08) + 0.07 * Math.sin(2.76 * w) * Math.exp(-t / 0.05));
    }
  }
  return edges(withRoom(y, -17, { rt60: 0.35, size: 0.5, damp: 0.45 }));
};

/** soft UI tick for a hint card: a short 1.6 kHz blip on a low "tok" */
const sfxTick = () => {
  const r = rng("tick"), n = sec(0.05), y = new Float32Array(n), hp = new Biquad("hp", 3000, 0.7);
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    y[i] = Math.sin(2 * Math.PI * 1600 * t) * Math.exp(-t / 0.008) + 0.6 * Math.sin(2 * Math.PI * 520 * t) * Math.exp(-t / 0.012)
      + hp.run(white(r)) * Math.exp(-t / 0.001) * 0.2;
  }
  return toStereo(edges(y));
};

/**
 * the countdown beep: the app's own (VoiceCoach.tick: ToneGenerator TONE_PROP_BEEP, a 1200 Hz tone, 120 ms), tuned
 * 37 cents down to D6 so it sits in the score's A minor as a suspended fourth over the A pedal; softened edges
 */
const sfxBeep = () => {
  const n = sec(0.12), y = new Float32Array(n), f = hz("D6");
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = Math.min(1, t / 0.004) * (1 - 0.2 * (t / 0.12)) * Math.min(1, (0.12 - t) / 0.025);
    y[i] = env * (Math.sin(2 * Math.PI * f * t) + 0.12 * Math.sin(4 * Math.PI * f * t));
  }
  return edges(withRoom(y, -20, { rt60: 0.25, size: 0.4 }));
};

/** air whoosh: pink noise through a band-pass that sweeps up to the peak and falls after; returns the peak offset */
const sfxWhoosh = (name, len, peak, fLo, fHi, tailTau) => {
  const n = sec(len), out = St(n);
  [0, 1].forEach((ch) => {
    const pink = pinkGen(rng(`${name}-${ch}`)), bp = new Biquad("bp", fLo, 1.3), lp = new Biquad("lp", 900, 0.7);
    const dst = ch ? out.R : out.L;
    for (let i = 0; i < n; i++) {
      const t = i / SR;
      const u = t <= peak ? t / peak : Math.max(0, 1 - (t - peak) / (len - peak));
      if ((i & 15) === 0) bp.set("bp", fLo * Math.pow(fHi / fLo, Math.pow(u, 1.6)), 1.3);
      const env = t <= peak ? Math.pow(t / peak, 2.6) : Math.exp(-(t - peak) / tailTau);
      const x = pink();
      dst[i] = (bp.run(x) * 1.6 + lp.run(x) * 0.5) * env;
    }
  });
  return { buf: edges(out, 0.001, 0.01), at: sec(peak) };
};

/** the reverse swell into the unison: the hit chord's own room tail played backwards, plus rising noise; cut dead */
const sfxSwell = (len) => {
  const n = sec(len);
  const stab = St(sec(0.4));
  chord(stab, 0, AM9_HI, 1, { len: 0.25, index: 1.6, tau: 0.6 });
  const mono = new Float32Array(stab.L.length);
  for (let i = 0; i < mono.length; i++) mono[i] = (stab.L[i] + stab.R[i]) / 2;
  const tail = room(mono, { rt60: 1.4, size: 1.0, damp: 0.3, tail: len + 0.2 });
  const out = St(n);
  const pk = Math.max(peakOf(tail), 1e-9);
  [0, 1].forEach((ch) => {
    const src = ch ? tail.R : tail.L, dst = ch ? out.R : out.L;
    const pink = pinkGen(rng(`swell-${ch}`)), lp = new Biquad("lp", 500, 0.7);
    for (let i = 0; i < n; i++) {
      const u = i / n; // 0 -> 1 toward the hit
      if ((i & 15) === 0) lp.set("lp", 500 + 6500 * u * u, 0.7);
      const rev = src[Math.min(src.length - 1, sec(0.05) + (n - 1 - i))] / pk; // reversed: the tail grows into the cut
      dst[i] = rev * 0.8 * Math.pow(u, 0.6) + lp.run(pink()) * 1.8 * Math.pow(u, 3);
    }
  });
  return { buf: edges(out, 0.01, 0.0015), at: n };
};

/** the low hit for the hero landing (the unison): a sub falling 72 -> 40 Hz, driven for harmonics, an air transient */
const sfxHit = () => {
  const r = rng("hit"), n = sec(1.1), y = new Float32Array(n), lp = new Biquad("lp", 1200, 0.7);
  let ph = 0;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    ph += (2 * Math.PI * (40 + 32 * Math.exp(-t / 0.08))) / SR;
    const sub = Math.tanh(2.2 * Math.sin(ph) * Math.exp(-t / 0.35) * (1 - Math.exp(-t / 0.001))) / Math.tanh(2.2);
    y[i] = sub + 0.35 * Math.sin(2 * Math.PI * 110 * t) * Math.exp(-t / 0.06) + lp.run(white(r)) * Math.exp(-t / 0.012) * 0.6;
  }
  return edges(withRoom(y, -14, { rt60: 0.5, size: 0.8 }));
};

/** a sleeve's flick: band-passed noise with a grainy, uneven envelope (cloth), for the hook's fidget */
const sfxFabric = (name, len, fc, bright) => {
  const r = rng(name), n = sec(len), y = new Float32Array(n);
  const bp = new Biquad("bp", fc, 0.9), hp = new Biquad("hp", 600, 0.7);
  let grain = 0, target = 0;
  for (let i = 0; i < n; i++) {
    const u = i / n;
    if (i % sec(0.004) === 0) target = 0.35 + 0.65 * r() * r();
    grain += (target - grain) * 0.02;
    const env = Math.pow(Math.sin(Math.PI * Math.pow(u, 0.55)), 2) * grain;
    y[i] = hp.run(bp.run(white(r))) * env * (1 + bright * Math.sin(Math.PI * u));
  }
  return toStereo(edges(y, 0.003, 0.01));
};

/** a lay figure's friction joint giving: a low, dry wooden tick (the click family, pitched down, more body) */
const sfxWood = () => {
  const r = rng("wood"), n = sec(0.05), e = new Float32Array(n);
  strike(e, 0, 1, 0.0006, r);
  const y = resonate(e, [{ f: 1050, tau: 0.009 }, { f: 1780, tau: 0.006, amp: 0.7 }, { f: 2650, tau: 0.004, amp: 0.5 }, { f: 430, tau: 0.008, amp: 0.4 }]);
  return toStereo(edges(y));
};

const kit = (name, made, about) => {
  const b = made.buf ? made : { buf: made, at: 0 };
  KIT[name] = { buf: norm3(toStereo(b.buf)), at: b.at, about };
};
kit("snap", sfxSnap(1, "snap"), "chorus snap: shutter-click transient");
for (let k = 1; k <= 4; k++) kit(`snap_up${k}`, sfxSnap(Math.pow(2, k / 12), `snap${k}`), `chorus snap, +${k} semitone`);
kit("pop", sfxSnap(1.5, "pop"), "the frame marks: the click family's smallest");
kit("shutter", sfxShutter(), "THE shutter: mirror up + curtains, slap, thump, room");
kit("lock", sfxLock(), "skeleton locks onto the guide: latch + E5/B5");
kit("tick", sfxTick(), "hint card arrives: soft UI tick");
kit("beep", sfxBeep(), "countdown: the app's TONE_PROP_BEEP, tuned to D6");
kit("whoosh_cut", sfxWhoosh("whoosh_cut", 0.3, 0.2, 350, 2600, 0.03), "into the f80 cut: short in-breath of air, peak on the cut");
kit("whoosh_push", sfxWhoosh("whoosh_push", 0.62, 0.42, 280, 3200, 0.06), "the push into the viewfinder, peak on the match cut");
kit("swell", sfxSwell((C.unison - C.drop) / C.fps), "the drop: the hit chord's room, reversed, cut dead at the unison");
kit("hit", sfxHit(), "the hero landing: low hit");
kit("fabric_flick", sfxFabric("fabric_flick", 0.24, 2600, 0.6), "hook: the wrists flick up");
kit("fabric_flop", sfxFabric("fabric_flop", 0.2, 1700, 0.2), "hook: the hands flop back");
kit("wood", sfxWood(), "the figure relaxes on twos: a joint gives");

// ------------------------------------------------------------------------------------------------ the cue sheet (SFX)
const cues = []; // { f, sfx, gainDb, pan, label }
const cue = (f, sfx, gainDb, label, pan = 0) => cues.push({ f, sfx, gainDb, pan, label });

// act I: the freeze. No music; the room, and the fidget.
cue(C.flick[0], "fabric_flick", -18, "wrists flick up", -0.1);
cue(C.flop[0], "fabric_flop", -22, "hands flop back", 0.1);
// the cut: a short in-breath that peaks on the cut, where the groove's first kick lands
cue(C.cut1, "whoosh_cut", -15, "the rush: hard cut black -> the first real person (peak on the cut)");
// the chorus: one click per guide landing, one sound family. On-beat landings sit on the kick; the eighth-note ones
// rise a semitone each (v1 also rippled the next lattice rows 1-3 f later: v2 has one person per shot, so CUES.ripple
// is empty); couples click twice, groups three times.
const eighthSnaps = C.chorus.filter((c, k) => k > 0 && c.f - C.chorus[k - 1].f === BEAT / 2 || (C.chorus[k + 1] && C.chorus[k + 1].f - c.f === BEAT / 2));
C.chorus.forEach((c) => {
  const k = eighthSnaps.indexOf(c);
  if (c.category === "COUPLE") {
    cue(c.f, "snap", -11, "Couple snap");
    cue(c.f + 2, "snap", -16, "Couple, second click");
  } else if (c.category === "GROUP") {
    cue(c.f, "snap", -11, "Group snap");
    cue(c.f + 2, "snap", -16, "Group, second click");
    cue(c.f + 4, "snap", -20, "Group, third click");
  } else if (k >= 0) {
    cue(c.f, k === 0 ? "snap" : `snap_up${Math.min(4, k)}`, -12 + k * 0.5, `${c.category} snap (eighth, +${k})`);
    C.ripple.filter((r) => r > c.f && r <= c.f + 3).forEach((r) => {
      cue(r, k === 0 ? "snap" : `snap_up${Math.min(4, k)}`, -14 - 4 * (r - c.f), `${c.category} ripple +${r - c.f} f`, (r - c.f) % 2 ? 0.3 : -0.3);
    });
  } else {
    cue(c.f, "snap", -13, `${c.category} snap`);
  }
});
// the drop, then HIT 1
cue(C.unison, "swell", -13, "the drop: reverse swell, cut dead on the unison");
cue(C.unison, "hit", -9, "HIT 1: all 99 snap to Hands in pockets (low hit)");
cue(C.unison, "snap", -11, "HIT 1: the unison snap");
cue(C.marks, "pop", -17, "frame marks snap on");
cue(C.cut2, "whoosh_push", -12, "accelerating push, peak on the match cut");
// act IV: the coaching. Cards tick; the lock is hit 2.
// ("Perfect. Hold it" arrives 12 f after the lock and is up for 9 f: the lock already says it, so it gets no tick;
// lock, tick and beep inside 21 frames would be wall-to-wall)
C.cards.forEach((f, k) => { if (!(f > C.lock && f - C.lock < 20)) cue(f, "tick", -17, `hint card ${k + 1}`); });
cue(C.lock, "lock", -7, "HIT 2: the lock (the app's hold frame: the score reaches the threshold, the hold arc starts)");
C.digits.forEach((f, k) => cue(f, "beep", -14, `countdown ${3 - k}`));
// HIT 3: the shutter, the loudest sound in the film, after half a beat of silence
cue(C.capture, "shutter", 2.5, "HIT 3: the shutter and the capture flash");
// the relax on twos: a joint gives on each step, louder where the arm moves most (softInOut: the middle steps)
C.relaxSteps.forEach((f, k) => {
  const u = (k + 0.5) / C.relaxSteps.length;
  cue(f, "wood", -30 + 7 * Math.sin(Math.PI * u), `relax step ${k + 1}`, 0.15);
});

// ------------------------------------------------------------------------------------------------ the score
const drums = St(N), bassBus = St(N), keys = St(N), fx = St(N), send = St(N);
const beats = (from, to, step = BEAT, phase = 0) => {
  const out = [];
  for (let f = from + phase; f < to; f += step) out.push(f);
  return out;
};
const bar = (n) => C.grid.firstBar + n * 4 * BEAT; // bar n's downbeat frame (n = 1 -> f80)

// ---- groove A: the cut to the drop (f80 - f248). Kick, shutter-click hats, octave bass; the clap joins on bar 3;
// a 16th tick layer joins on the eighth-note snaps while the bass climbs; keys only tease (offbeat chops).
const A0 = C.cut1, A1 = C.drop;
beats(A0, A1).forEach((f) => kick(drums, S(f), f === A0 ? 0.95 : 0.82));
beats(A0, A1, BEAT, BEAT / 2).forEach((f, k) => clickHat(drums, S(f), 0.16 + 0.03 * (k % 2), 0.25));
beats(bar(2), A1, 2 * BEAT, BEAT).forEach((f) => clap(drums, send, S(f), 0.27));
const eighthFrom = eighthSnaps[0].f, eighthTo = eighthSnaps[eighthSnaps.length - 1].f + BEAT / 2;
beats(eighthFrom, eighthTo, BEAT / 4).forEach((f, k) => clickHat(drums, S(f), k % 2 ? 0.07 : 0.1, k % 2 ? -0.35 : -0.15, 1.25));
const octave = (from, to, lo, hi, vel = 0.5) => beats(from, to, BEAT / 2).forEach((f, k) => bass(bassBus, S(f), hz(k % 2 ? hi : lo), vel * (k % 2 ? 0.8 : 1), 0.12));
octave(bar(1), bar(1) + 3 * BEAT, "A1", "A2");
bass(bassBus, S(bar(1) + 3 * BEAT), hz("C2"), 0.48, 0.12); // a chromatic walk-up into the IV: C, C#, D
bass(bassBus, S(bar(1) + 3.5 * BEAT), hz("C#2"), 0.5, 0.12);
octave(bar(2), eighthFrom, "D2", "D3");
["D2", "E2", "F#2", "G#2"].forEach((nm, k) => bass(bassBus, S(eighthFrom + k * BEAT / 2), hz(nm), 0.52 + 0.03 * k, 0.24)); // climbs with the snaps
octave(Math.max(bar(3), eighthFrom + 2 * BEAT), C.chorus[C.chorus.length - 1].f, "A1", "A2"); // after the climb (v2: the eighths start on bar 3)
bass(bassBus, S(C.chorus[C.chorus.length - 1].f), hz("E2"), 0.55, (A1 - C.chorus[C.chorus.length - 1].f) / C.fps - 0.02); // held into the drop
[bar(2) + 1.5 * BEAT, bar(2) + 3.5 * BEAT].forEach((f) => chord(keys, S(f), D9, 0.2, { len: 0.1, index: 1.4, tau: 0.5 }));
chord(keys, S(bar(3) + 1.5 * BEAT), AM9, 0.2, { len: 0.1, index: 1.4, tau: 0.5 });

// ---- the drop (f248 - f272): nothing but the reverse swell (an SFX cue) and the room.

// ---- HIT 1 + groove B: the unison to the match cut (f272 - f336). Everything at once on the unison, the bright
// stab; the last beat lifts (no kick, 16th hats, the bass held) so the push's whoosh owns the cut.
const B0 = C.unison, B1 = C.cut2;
kick(drums, S(B0), 0.45); // the low hit carries the bottom of the unison
kickAt.push(S(B0)); kickVel.push(1); // and ducks the bass like a kick
clap(drums, send, S(B0), 0.32);
chord(keys, S(B0), AM9_HI, 0.68, { len: 0.45, index: 1.8, tau: 0.7 });
crash(drums, S(B0), 0.16);
mixInto(send, keys.L.subarray(S(B0), S(B0) + sec(0.6)), S(B0), 0.4);
beats(B0 + BEAT, B1 - BEAT).forEach((f) => kick(drums, S(f), 0.82));
beats(B0 + BEAT, B1, 2 * BEAT).forEach((f) => clap(drums, send, S(f), 0.27));
beats(B0, B1 - BEAT, BEAT, BEAT / 2).forEach((f) => clickHat(drums, S(f), 0.17, 0.25));
beats(B1 - BEAT, B1, BEAT / 4).forEach((f, k) => clickHat(drums, S(f), 0.09 + 0.03 * k, k % 2 ? -0.3 : 0.3, 1.1));
octave(B0, B0 + 2 * BEAT, "A1", "A2", 0.52);
octave(B0 + 2 * BEAT, B1 - BEAT, "G1", "G2", 0.52);
bass(bassBus, S(B1 - BEAT), hz("E2"), 0.55, BEAT / C.fps - 0.02, { cutoff: 1100, decay: 0.5 });
chord(keys, S(B0 + 1.5 * BEAT), AM9, 0.22, { len: 0.1, index: 1.4, tau: 0.5 });

// ---- coaching (f336 - f464): no kick, no clap: a sparse bass figure, soft hats and two warm chords under the
// cards. The bus sits 6 dB down for the reading (automation below).
const K0 = C.cut2, K1 = C.digits[0];
chord(keys, S(K0), D9, 0.46, { len: 1.6, index: 0.7, tau: 1.6, rel: 0.4 });
chord(keys, S(K0 + 4 * BEAT), AM9, 0.46, { len: 1.6, index: 0.7, tau: 1.6, rel: 0.4 });
const figure = (from, notes) => notes.forEach(([b, nm, len]) => bass(bassBus, S(from + b * BEAT), hz(nm), 0.62, len * BEAT / C.fps, { cutoff: 650, decay: 0.3 }));
figure(K0, [[0, "D2", 0.75], [1.375, "D2", 0.25], [2, "A1", 0.375], [3, "C2", 0.375], [3.5, "C#2", 0.375]]);
figure(K0 + 4 * BEAT, [[0, "A1", 0.75], [1.375, "A1", 0.25], [2, "E2", 0.375], [3, "G1", 0.375], [3.5, "G#1", 0.375]]);
beats(K0, K1, BEAT, BEAT / 2).forEach((f, k) => clickHat(drums, S(f), 0.13 + 0.02 * (k % 2), 0.25));

// ---- the countdown (f464 - f546): the clock becomes the app's 1 s digit. A kick every half second, the bass an A
// pedal in eighths with its filter opening, the shutter-click hats speeding up like a motor drive winding up
// (eighths, then 16ths), two claps, a riser. Then everything stops for half a beat.
const D0 = C.digits[0], STILL = C.still, P = C.digits[1] - C.digits[0]; // 30 f
beats(D0, STILL, P / 2).forEach((f, k) => kick(drums, S(f), 0.7 + 0.035 * k));
beats(D0, STILL, P / 4).forEach((f) => {
  const u = (f - D0) / (STILL - D0);
  bass(bassBus, S(f), hz("A1"), 0.42 + 0.18 * u, (P / 4) / C.fps * 0.8, { cutoff: 300 * Math.pow(8, u), decay: 0.2 });
});
beats(D0, C.digits[1], P / 2, P / 4).forEach((f) => clickHat(drums, S(f), 0.12, 0.25)); // offbeat eighths
beats(C.digits[1], C.digits[2], P / 4).forEach((f) => clickHat(drums, S(f), 0.13, 0.25));
beats(C.digits[2], STILL, P / 8).forEach((f, k) => clickHat(drums, S(f), 0.12 + 0.012 * k, k % 2 ? -0.3 : 0.3, 1.05));
[D0 + P / 2, C.digits[1] + P / 2].forEach((f) => clap(drums, send, S(f), 0.34));
(() => {
  const a = S(C.digits[1]), b = S(STILL);
  [0, 1].forEach((ch) => {
    const pink = pinkGen(rng(`riser-${ch}`)), bp = new Biquad("bp", 350, 1.2), dst = ch ? fx.R : fx.L;
    for (let i = a; i < b; i++) {
      const u = (i - a) / (b - a);
      if ((i & 15) === 0) bp.set("bp", 350 * Math.pow(12, u), 1.2);
      dst[i] += bp.run(pink()) * 0.55 * Math.pow(u, 2.2);
    }
  });
})();

// ---- the end (v2): the final chord blooms with the colour, on the first colour frame after the capture flash
// (CUES.colour), and rings over the photo and through the cut to paper; a soft low A re-strike under the pull back
// keeps the name off a dying tail, ringing to the last frame.
const COLOUR = C.colour ?? C.pull[0];
chord(keys, S(COLOUR), AM_ADD9, 0.36, { len: 3, index: 0.8, tau: 1.25 });
bass(bassBus, S(COLOUR), hz("A1"), 0.36, (C.pull[0] - COLOUR) / C.fps, { cutoff: 500, decay: 0.7 });
mixInto(send, keys.L.subarray(S(COLOUR), S(C.end)), S(COLOUR), 0.25);
if (COLOUR < C.pull[0]) {
  bass(bassBus, S(C.pull[0]), hz("A1"), 0.3, (C.end - C.pull[0]) / C.fps, { cutoff: 420, decay: 0.9 });
  chord(keys, S(C.pull[0]), ["A2", "E3"], 0.16, { len: 2.2, index: 0.5, tau: 1.6 });
}

// ---- the room on the music bus (clap and keys sends)
const sendMono = new Float32Array(N);
for (let i = 0; i < N; i++) sendMono[i] = (send.L[i] + send.R[i]) / 2;
const musicRoom = room(sendMono, { rt60: 0.45, size: 0.6, damp: 0.4, tail: 0 });

// ---- stems -> the music bus, with the section automation (dB keyframes over frames, linear in dB between them)
/**
 * the bass ducks under every kick (sidechain: 7 dB, 4 ms in, 90 ms back), the disco way: the two never pile up in the
 * low end, so the master limiter has little to do and the groove breathes with the kick
 */
for (let i = 0; i < N; i++) {
  let g = 1;
  for (let k = kickAt.length - 1; k >= 0; k--) {
    const d = i - kickAt[k];
    if (d < -sec(0.004)) continue;
    if (d > sec(0.4)) break;
    const env = d < 0 ? 1 + d / sec(0.004) : Math.exp(-d / sec(0.09));
    g = Math.min(g, 1 - (1 - dB(-7)) * env * kickVel[k]);
  }
  bassBus.L[i] *= g;
  bassBus.R[i] *= g;
}
const music = St(N);
mixInto(music, drums, 0, 1);
mixInto(music, bassBus, 0, dB(0));
mixInto(music, keys, 0, dB(-2));
mixInto(music, fx, 0, dB(-6));
mixInto(music, musicRoom, 0, dB(-4));
const MUTE = -90;
const AUTOMATION = [
  [0, 0],
  [C.nameRead[0], 0], [C.nameRead[0] + 3, -4], // "Hands in pockets" is read: the groove makes room
  [C.push[0] + 6, -4], [C.push[0] + 18, 0], // and comes back for the push
  [C.cut2, 0], [C.cut2 + 1, -6], // the coaching: 6 dB down under the cards
  [C.digits[0], -6], [C.digits[0] + 2, -7], [C.still, -3], // the countdown builds, but stays under the shutter
  [C.still + 0.3, MUTE], [COLOUR - 1, MUTE], [COLOUR, 0], // half a beat of silence; the shutter alone, then the colour chord
  [C.end, 0],
];
const gainAt = (keysDb, f) => {
  if (f <= keysDb[0][0]) return keysDb[0][1];
  for (let k = 1; k < keysDb.length; k++) {
    const [f1, g1] = keysDb[k];
    if (f <= f1) {
      const [f0, g0] = keysDb[k - 1];
      return g0 + ((g1 - g0) * (f - f0)) / Math.max(1e-9, f1 - f0);
    }
  }
  return keysDb[keysDb.length - 1][1];
};
for (let i = 0; i < N; i++) {
  const g = gainAt(AUTOMATION, i / SPF);
  const lin = g <= MUTE ? 0 : dB(g);
  music.L[i] *= lin;
  music.R[i] *= lin;
}

// ------------------------------------------------------------------------------------------------ the voice-over slot
let vo = null;
const voIndex = join(OUT, "vo", "lines.index.json");
const voDuck = new Float32Array(N).fill(0); // dB, <= 0
if (USE_VO && existsSync(voIndex)) {
  const index = JSON.parse(readFileSync(voIndex, "utf8"));
  vo = St(N);
  for (const ln of index) {
    const slot = EV.vo.find((v) => v.i === ln.i);
    if (!slot) { console.warn(`vo: line ${ln.i} has no slot in src/audio/voiceover.ts; skipped`); continue; }
    let clip = [resolve(ROOT, ln.clip), join(OUT, "vo", `line${String(ln.i).padStart(2, "0")}.wav`)].find(existsSync);
    if (!clip) { console.warn(`vo: clip for line ${ln.i} not found; skipped`); continue; }
    const raw = execFileSync("ffmpeg", ["-v", "error", "-i", clip, "-ac", "1", "-ar", String(SR), "-f", "f32le", "-"], { maxBuffer: 1 << 28 });
    const x = new Float32Array(raw.buffer, raw.byteOffset, raw.byteLength >> 2).slice();
    // level: each line to -16 LUFS (pre-master), so the voice is the loudest sustained layer
    const l = lufs({ L: x, R: x });
    if (Number.isFinite(l)) scaleBuf(x, dB(-16 - l));
    const at = S(slot.at) - sec(ln.lead ?? 0);
    mixInto(vo, x, at);
    // the spoken regions (10 ms windows above -40 dBFS, gaps under 250 ms bridged) duck the music
    const win = sec(0.01), regions = [];
    for (let i = 0; i + win <= x.length; i += win) {
      let s2 = 0;
      for (let k = 0; k < win; k++) s2 += x[i + k] * x[i + k];
      if (10 * Math.log10(s2 / win + 1e-12) > -40) {
        const last = regions[regions.length - 1];
        if (last && i - last[1] < sec(0.25)) last[1] = i + win; else regions.push([i, i + win]);
      }
    }
    for (const [a, b] of regions) {
      const s0 = at + a, s1 = at + b;
      for (let i = Math.max(0, s0 - sec(0.08)); i < Math.min(N, s1 + sec(0.3)); i++) {
        const g = i < s0 ? -slot.duckDb * (1 - (s0 - i) / sec(0.08)) : i < s1 ? -slot.duckDb : -slot.duckDb * (1 - (i - s1) / sec(0.3));
        voDuck[i] = Math.min(voDuck[i], g);
      }
    }
    const speechEnd = regions.length ? at + regions[regions.length - 1][1] : at;
    if (speechEnd > S(slot.window[1])) console.warn(`vo: line ${ln.i} ("${slot.text}") speaks until f${(speechEnd / SPF).toFixed(1)}, past its window's end f${slot.window[1]}: move its \`at\` in src/audio/voiceover.ts`);
    console.log(`vo: line ${ln.i} placed at f${slot.at} (clip start f${(at / SPF).toFixed(2)}), duck ${slot.duckDb} dB`);
  }
}

// ------------------------------------------------------------------------------------------------ SFX bus + room tone
const sfx = St(N);
const cueJson = [];
for (const c of cues.sort((a, b) => a.f - b.f)) {
  const k = KIT[c.sfx];
  const at = S(c.f) - k.at;
  mixInto(sfx, k.buf, at, dB(c.gainDb), c.pan);
  cueJson.push({ f: c.f, t: +(at / SR).toFixed(4), sfx: `public/audio/sfx/${c.sfx}.wav`, gain: +c.gainDb.toFixed(1), label: c.label });
}
// room tone: a quiet studio, the whole film, so no silence is ever dead air (about -48 dBFS rms: present, not hiss)
const roomTone = St(N);
[0, 1].forEach((ch) => {
  const pink = pinkGen(rng(`roomtone-${ch}`)), lp = new Biquad("lp", 2000, 0.6), hp = new Biquad("hp", 90, 0.6), dst = ch ? roomTone.R : roomTone.L;
  for (let i = 0; i < N; i++) dst[i] = hp.run(lp.run(pink())) * dB(-32.5);
});

// ------------------------------------------------------------------------------------------------ master: loudness + true peak
function kweight(x) {
  const a = new Biquad("lp", 1000), b = new Biquad("lp", 1000); // coefficients replaced: BS.1770-4 at 48 kHz
  Object.assign(a, { b0: 1.53512485958697, b1: -2.69169618940638, b2: 1.19839281085285, a1: -1.69065929318241, a2: 0.73248077421585 });
  Object.assign(b, { b0: 1, b1: -2, b2: 1, a1: -1.99004745483398, a2: 0.99007225036621 });
  return filt(x, a, b);
}
/** integrated loudness, ITU-R BS.1770-4 / EBU R128 (400 ms blocks, 75 % overlap, -70 LUFS and -10 LU gates) */
function lufs(b) {
  const kl = kweight(b.L), kr = kweight(b.R), n = kl.length, blk = sec(0.4), step = sec(0.1);
  const cum = new Float64Array(n + 1);
  for (let i = 0; i < n; i++) cum[i + 1] = cum[i] + kl[i] * kl[i] + kr[i] * kr[i];
  const z = [];
  for (let s = 0; s + blk <= n; s += step) z.push((cum[s + blk] - cum[s]) / blk);
  const L = (p) => -0.691 + 10 * Math.log10(p);
  const abs = z.filter((p) => L(p) > -70);
  if (!abs.length) return -Infinity;
  const rel = L(abs.reduce((a, p) => a + p, 0) / abs.length) - 10;
  const g = abs.filter((p) => L(p) > rel);
  return L(g.reduce((a, p) => a + p, 0) / g.length);
}
/** short-term loudness (3 s) at a window ending at sample e, for the section report */
function shortTerm(b, from, to) {
  const s = { L: b.L.subarray(from, to), R: b.R.subarray(from, to) };
  const kl = kweight(s.L), kr = kweight(s.R);
  let p = 0;
  for (let i = 0; i < kl.length; i++) p += kl[i] * kl[i] + kr[i] * kr[i];
  return -0.691 + 10 * Math.log10(p / kl.length + 1e-20);
}
// 4x oversampling interpolator (windowed sinc, 16 taps per phase) for the true-peak estimate
const TP_TAPS = 8;
const TP_KERNEL = [1, 2, 3].map((p) => {
  const h = [];
  for (let k = -TP_TAPS + 1; k <= TP_TAPS; k++) {
    const x = k - p / 4, w = 0.5 + 0.5 * Math.cos((Math.PI * x) / (TP_TAPS + 0.5));
    h.push((x === 0 ? 1 : Math.sin(Math.PI * x) / (Math.PI * x)) * w);
  }
  return h;
});
/** per-sample true-peak envelope: max |x| over the sample and the 3 interpolated points after it, both channels */
function tpEnvelope(b) {
  const n = b.L.length, env = new Float32Array(n);
  for (const x of [b.L, b.R]) {
    for (let i = 0; i < n; i++) {
      let m = Math.abs(x[i]);
      if (m > 0.05 || Math.abs(x[i + 1] ?? 0) > 0.05) {
        for (const h of TP_KERNEL) {
          let y = 0;
          for (let k = 0; k < h.length; k++) {
            const j = i + k - TP_TAPS + 1;
            if (j >= 0 && j < n) y += x[j] * h[k];
          }
          m = Math.max(m, Math.abs(y));
        }
      }
      if (m > env[i]) env[i] = m;
    }
  }
  return env;
}
/** look-ahead limiter on the true-peak envelope: sliding min (+/- 1 ms), averaged over the same span, 80 ms release */
function limit(b, ceiling) {
  const n = b.L.length, env = tpEnvelope(b), L = sec(0.001);
  const tgt = new Float32Array(n);
  for (let i = 0; i < n; i++) tgt[i] = env[i] > ceiling ? ceiling / env[i] : 1;
  const mn = new Float32Array(n), dq = new Int32Array(n);
  let h = 0, t = 0;
  for (let i = 0; i < n + L; i++) { // window [i - 2L, i] -> written at i - L (centred)
    if (i < n) {
      while (t > h && tgt[dq[t - 1]] >= tgt[i]) t--;
      dq[t++] = i;
    }
    while (dq[h] < i - 2 * L) h++;
    if (i - L >= 0 && i - L < n) mn[i - L] = tgt[dq[h]];
  }
  const avg = new Float32Array(n);
  let acc = 0;
  for (let i = 0; i < n + L; i++) {
    if (i < n) acc += mn[i]; else acc += 1;
    if (i - 2 * L - 1 >= 0) acc -= i - 2 * L - 1 < n ? mn[i - 2 * L - 1] : 1;
    const c = i - L;
    if (c >= 0 && c < n) avg[c] = acc / Math.min(2 * L + 1, i + 1);
  }
  const rel = Math.exp(-1 / (0.08 * SR));
  let g = 1, maxGr = 0;
  const out = St(n), grByFrame = new Float32Array(Math.ceil(n / SPF));
  for (let i = 0; i < n; i++) {
    g = avg[i] <= g ? avg[i] : avg[i] - (avg[i] - g) * rel;
    g = Math.min(g, tgt[i]);
    const grDb = -20 * Math.log10(g);
    maxGr = Math.max(maxGr, grDb);
    const fr = Math.floor(i / SPF);
    if (grDb > grByFrame[fr]) grByFrame[fr] = grDb;
    out.L[i] = b.L[i] * g;
    out.R[i] = b.R[i] * g;
  }
  return { out, maxGr, grByFrame };
}
const truePeakDb = (b) => 20 * Math.log10(tpEnvelope(b).reduce((a, v) => Math.max(a, v), 0));

const ducked = St(N);
for (let i = 0; i < N; i++) {
  const g = dB(voDuck[i]);
  ducked.L[i] = music.L[i] * g;
  ducked.R[i] = music.R[i] * g;
}
const pre = St(N);
mixInto(pre, ducked, 0);
mixInto(pre, sfx, 0);
if (vo) mixInto(pre, vo, 0);
mixInto(pre, roomTone, 0);

let gain = dB(TARGET_LUFS - lufs(pre)), master, loud, gr, grByFrame;
for (let iter = 0; iter < 8; iter++) {
  const g = St(N);
  mixInto(g, pre, 0, gain);
  ({ out: master, maxGr: gr, grByFrame } = limit(g, dB(CEILING_DBTP)));
  loud = lufs(master);
  if (Math.abs(loud - TARGET_LUFS) < 0.05) break;
  gain *= dB(TARGET_LUFS - loud);
}
const tp = truePeakDb(master);

// ------------------------------------------------------------------------------------------------ write
const wav = (b, path, dither) => {
  const st = toStereo(b), n = st.L.length, buf = Buffer.alloc(44 + n * 4), r = rng(`dither-${relative(OUT, path)}`);
  buf.write("RIFF", 0); buf.writeUInt32LE(36 + n * 4, 4); buf.write("WAVE", 8);
  buf.write("fmt ", 12); buf.writeUInt32LE(16, 16); buf.writeUInt16LE(1, 20); buf.writeUInt16LE(2, 22);
  buf.writeUInt32LE(SR, 24); buf.writeUInt32LE(SR * 4, 28); buf.writeUInt16LE(4, 32); buf.writeUInt16LE(16, 34);
  buf.write("data", 36); buf.writeUInt32LE(n * 4, 40);
  for (let i = 0; i < n; i++) {
    for (const [ch, x] of [[0, st.L[i]], [1, st.R[i]]]) {
      const d = dither ? (r() - r()) : 0; // TPDF, 1 LSB
      buf.writeInt16LE(Math.max(-32768, Math.min(32767, Math.round(x * 32767 + d))), 44 + i * 4 + ch * 2);
    }
  }
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, buf);
  return createHash("sha256").update(buf).digest("hex").slice(0, 12);
};
for (const [name, k] of Object.entries(KIT)) wav(k.buf, join(OUT, "sfx", `${name}.wav`), false);
const stemGain = gain; // stems at the level they sit at in the mix (before the limiter)
wav(scaleBuf(music, stemGain), join(OUT, "stems", "music.wav"), true);
wav(scaleBuf(sfx, stemGain), join(OUT, "stems", "sfx.wav"), true);
const mixHash = wav(master, join(OUT, "mix.wav"), true);
writeFileSync(join(OUT, "cues.json"), JSON.stringify(cueJson, null, 1));

// ------------------------------------------------------------------------------------------------ report
const sections = [
  ["act I hook (room, fidget)", 0, C.cut1], ["groove A + chorus", C.cut1, C.drop], ["the drop", C.drop, C.unison],
  ["unison + groove B", C.unison, C.cut2], ["coaching (music -6 dB)", C.cut2, C.digits[0]], ["countdown build", C.digits[0], C.still],
  ["stillness", C.still, C.capture], ["shutter + flash", C.capture, COLOUR], ["colour chord, photo, relax", COLOUR, C.pull[0]],
  ["low A re-strike + name", C.pull[0], C.end],
];
console.log(`gen-audio: ${Object.keys(KIT).length} SFX, ${cues.length} cues, ${C.frames} f (${(N / SR).toFixed(3)} s) at ${SR} Hz${vo ? ", with voice-over" : ", no voice-over (slot empty)"}`);
console.log(`  mix.wav  integrated ${loud.toFixed(2)} LUFS  true peak ${tp.toFixed(2)} dBTP  master gain ${(20 * Math.log10(gain)).toFixed(1)} dB  limiter max ${gr.toFixed(1)} dB  sha256 ${mixHash}`);
for (const [name, a, b] of sections) {
  const lv = shortTerm(master, S(a), S(b));
  console.log(`  ${name.padEnd(28)} f${String(a).padStart(3)}-${String(b).padEnd(3)} ${Number.isFinite(lv) ? lv.toFixed(1).padStart(6) : "  -inf"} LUFS (mean)`);
}
const momentary = (f) => shortTerm(master, S(f), S(f) + sec(0.4));
const samplePeak = (f, len = 0.4) => {
  let p = 0;
  for (let i = S(f); i < Math.min(N, S(f) + sec(len)); i++) p = Math.max(p, Math.abs(master.L[i]), Math.abs(master.R[i]));
  return 20 * Math.log10(p + 1e-12);
};
const chorusMax = Math.max(...C.chorus.map((c) => momentary(c.f)));
console.log(`  hits (momentary 400 ms / sample peak): unison f${C.unison} ${momentary(C.unison).toFixed(1)} LUFS ${samplePeak(C.unison).toFixed(1)} dBFS | lock f${C.lock} ${momentary(C.lock).toFixed(1)} / ${samplePeak(C.lock).toFixed(1)} | shutter f${C.capture} ${momentary(C.capture).toFixed(1)} / ${samplePeak(C.capture).toFixed(1)} | loudest chorus snap ${chorusMax.toFixed(1)} LUFS`);
const grList = [...grByFrame].map((g, f) => [f, g]).filter(([, g]) => g >= 1).map(([f, g]) => `f${f} ${g.toFixed(1)}`);
console.log(`  limiter >= 1 dB on ${grList.length} frames${grList.length ? ": " + grList.join(", ") : ""}`);
console.log(`  cues -> ${relative(ROOT, join(OUT, "cues.json"))}; SFX kit -> ${relative(ROOT, join(OUT, "sfx"))}/`);
