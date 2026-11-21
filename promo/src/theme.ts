/**
 * theme.ts: the ONE theme object of the Poseit promo. Never inline a hex, an ease or a spring in a scene.
 *
 *   theme.size    W, H, fps, duration (frames) from STORYBOARD.md
 *   theme.color   Studio tokens (docs/DESIGN.md), signal colours (camera = bright variants), person tones
 *   theme.font    families registered by src/fonts.ts: text = "Archivo" (400/500/600),
 *                 wide = "Archivo Expanded" (500/600/700); numerals use tabular figures (font-feature "tnum")
 *   theme.type    type roles from STORYBOARD.md section 3, sizes in px at 1920 H (role.size = em as a fraction of H)
 *   theme.hud     the camera HUD's real dimensions (dp, from CameraHud.kt) and the magnification k (cue = 0.04 H)
 *   theme.grid    the music grid: 112.5 BPM = 16 f per beat, bar lines at f16 + 64n. grid.bar(n), grid.beat(n)
 *   theme.ease    measured curves (t in 0..1 -> 0..1), see "Eases" below; pass to interpolate({easing})
 *   theme.spring  Remotion spring() configs: `land` (no overshoot) and `theOne` (the film's single spring)
 *   theme.dur     default durations in frames (motionmaxxing motion.md R1.1 medians)
 *   theme.signal  forScore(score 0..1, onDark): the app's own per-limb colour ramp (Studio.forScore)
 *   theme.footage v2: the Studio ramp the stock footage is printed in until the shutter, the chorus guide, the photo
 *
 * Eases. The shapes are the measured eases of motionmaxxing's motion.md (Apache-2.0, Tejas Makwana): landing
 * decelerates with most travel in the first 4-8 frames, leaving accelerates and ends at peak speed. Re-written here
 * from the published curve shapes (% of travel done at 10/25/50/75% of the time):
 *   softLand   30/59/85/95   entries, text rises, panels (12 f)              expo-out k 3.41
 *   snapSettle 54/84/96/98   hero / CTA entries, oversize arrivals (11 f)   fast snap + soft tail
 *   glide      16/39/69/91   medium moves, pushes (10 f)                      power-out 1.7
 *   whip       23/52/83/97   big fast decelerating moves (7-8 f)            power-out 2.52
 *   softInOut   4/17/50/83   opacity, blur, colour ramps (9 f)              symmetric S
 *   gentleIn    1/6/24/55    slow push-ins into a cut, fade-out starts (9 f) power-in 2.07
 *   accelExit   0/3/16/47    exits off-frame, scale exits (13 f)             power-in 2.6
 *   crashOut    0/0/6/32     pulls that end in a hard cut at speed (17 f)   power-in 3.97
 *   drift       sine in-out  camera creeps and drifts over their whole span (the storyboard replaces "linear")
 * There is no linear ease and no bouncy ease: the film's one overshoot is spring.theOne (the live arm in B6).
 */
import { interpolateColors } from "remotion";

const W = 1080;
const H = 1920;
const FPS = 30;

// ---- eases (all f(0) = 0, f(1) = 1, clamped) ----------------------------------------------------------------
type Ease = (t: number) => number;
const clamp01 = (f: Ease): Ease => (t) => (t <= 0 ? 0 : t >= 1 ? 1 : f(t));
const powerOut = (p: number): Ease => (t) => 1 - Math.pow(1 - t, p);
const powerIn = (p: number): Ease => (t) => Math.pow(t, p);
const expoOut = (k: number): Ease => {
  const d = 1 - Math.exp(-k);
  return (t) => (1 - Math.exp(-k * t)) / d;
};
/** A share `w` of the travel arrives as a fast exponential (time constant tau), the rest as a straight tail. */
const snapRamp = (w: number, tau: number): Ease => {
  const n = w * (1 - Math.exp(-1 / tau)) + (1 - w);
  return (t) => (w * (1 - Math.exp(-t / tau)) + (1 - w) * t) / n;
};
const smoothP = (p: number): Ease => (t) => {
  const a = Math.pow(t, p);
  const b = Math.pow(1 - t, p);
  return a / (a + b);
};

const ease = {
  softLand: clamp01(expoOut(3.41)),
  snapSettle: clamp01(snapRamp(0.942, 0.118)),
  glide: clamp01(powerOut(1.7)),
  whip: clamp01(powerOut(2.52)),
  softInOut: clamp01(smoothP(1.42)),
  gentleIn: clamp01(powerIn(2.07)),
  accelExit: clamp01(powerIn(2.6)),
  crashOut: clamp01(powerIn(3.97)),
  drift: clamp01((t) => 0.5 - 0.5 * Math.cos(Math.PI * t)),
} satisfies Record<string, Ease>;

// ---- signal ramp: the app's Studio.forScore, colour lerp in Oklab like Compose's lerp(Color, Color) ----------
const srgbToLinear = (c: number) => (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
const linearToSrgb = (c: number) => (c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055);
const hexToRgb = (hex: string) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
const toOklab = ([r, g, b]: number[]) => {
  const [lr, lg, lb] = [r, g, b].map(srgbToLinear);
  const l = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb);
  const m = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb);
  const s = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb);
  return [
    0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
    1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
    0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
  ];
};
const fromOklab = ([L, a, b]: number[]) => {
  const l = Math.pow(L + 0.3963377774 * a + 0.2158037573 * b, 3);
  const m = Math.pow(L - 0.1055613458 * a - 0.0638541728 * b, 3);
  const s = Math.pow(L - 0.0894841775 * a - 1.291485548 * b, 3);
  const rgb = [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ];
  return rgb.map((c) => Math.round(Math.min(1, Math.max(0, linearToSrgb(c))) * 255));
};
/** Mix two hex colours in Oklab (t 0..1). */
const mixOklab = (a: string, b: string, t: number): string => {
  const A = toOklab(hexToRgb(a));
  const B = toOklab(hexToRgb(b));
  const [r, g, bl] = fromOklab(A.map((v, i) => v + (B[i] - v) * Math.min(1, Math.max(0, t))));
  return `rgb(${r}, ${g}, ${bl})`;
};

const studio = {
  paper: "#E4E5E1", // the seamless backdrop (wall)
  set: "#D2D3CF", // the patch a figure stands on (floor)
  sheet: "#F3F3F0", // raised surfaces: cue card
  ink: "#17181A",
  graphite: "#5A5C5B",
  hairline: "rgba(23, 24, 26, 0.12)",
  mannequin: "#FBFBF9",
  /** Contour = Ink at 55%; Mannequin draws it opaque in its own group and fades the group (no darker overlaps). */
  contour: "#17181A",
  contourAlpha: 0.55,
  liveView: "#000000",
};

const signal = {
  miss: "#C8372D",
  near: "#D9930D",
  match: "#24935A",
};
const signalCam = {
  miss: "#FF6B5E",
  near: "#FFC24A",
  match: "#4FD18B",
};

/** The app's per-limb colour for a part score 0..1 (CameraOverlay -> Studio.forScore). */
const forScore = (score: number, onDark = true): string => {
  const s = onDark ? signalCam : signal;
  if (score >= 0.8) return s.match;
  if (score >= 0.6) return mixOklab(s.near, s.match, (score - 0.6) / 0.2);
  if (score >= 0.35) return mixOklab(s.miss, s.near, (score - 0.35) / 0.25);
  return s.miss;
};

// ---- music grid -------------------------------------------------------------------------------------------
const BPM = 112.5;
const beatFrames = (FPS * 60) / BPM; // 16
const grid = {
  bpm: BPM,
  beat: beatFrames, // 16 f
  eighth: beatFrames / 2, // 8 f
  bar: beatFrames * 4, // 64 f
  firstBar: beatFrames, // bar lines at f16 + 64n
  /** Frame of the downbeat of bar n (n = 0 -> f16, n = 1 -> f80 ...). */
  barAt: (n: number) => beatFrames + n * beatFrames * 4,
  /** Frame of beat n counted from f0. */
  beatAt: (n: number) => n * beatFrames,
};

export const theme = {
  size: {
    W,
    H,
    fps: FPS,
    duration: 664,
    /** One phone dp at the film's width: the film frame is a 411 dp wide phone canvas at 1080 px. */
    dp: W / 411,
  },
  color: {
    ...studio,
    signal,
    signalCam,
    /** Multi-person figures differ by tone (DESIGN.md). Index with i % 4. */
    personTones: [studio.mannequin, "#C9CBC6", "#A6A8A3", "#83857F"],
    /** The camera's thirds grid and frame marks: Mannequin white at these alphas. */
    gridLine: "rgba(251, 251, 249, 0.2)",
    ringIdle: "rgba(251, 251, 249, 0.6)",
    flash: "#FFFFFF",
  },
  font: {
    text: "Archivo",
    wide: "Archivo Expanded",
    weight: { regular: 400, medium: 500, semibold: 600, bold: 700 },
    tnum: '"tnum" 1',
  },
  /** Type roles: px at 1920 H. Sentence case everywhere; no caps, no tracking-out. */
  type: {
    hook: { family: "Archivo", weight: 600, size: 0.05 * H, tracking: "-0.015em", lineHeight: 1.1 },
    poseName: { family: "Archivo Expanded", weight: 600, size: 0.045 * H, tracking: "-0.01em", lineHeight: 1.1 },
    /** The app's cue card uses bodyStrong (Archivo Medium); 0.04 H = 15 sp at hud.k. */
    cue: { family: "Archivo", weight: 500, size: 0.04 * H, tracking: "0em", lineHeight: 22 / 15 },
    score: { family: "Archivo Expanded", weight: 700, size: 0.042 * H, tracking: "0em", lineHeight: 1 },
    countdown: { family: "Archivo Expanded", weight: 700, size: 0.26 * H, tracking: "0em", lineHeight: 1 },
    /** Sized by ink width (0.50 W), not by font size: use fitTextToWidth() from src/fonts.ts. */
    wordmark: { family: "Archivo Expanded", weight: 600, inkWidth: 0.5 * W, tracking: "-0.01em", lineHeight: 1 },
    endLine: { family: "Archivo", weight: 400, size: 0.034 * H, tracking: "0em", lineHeight: 1.2 },
  },
  /**
   * The camera HUD, rebuilt from the app (CameraHud.kt) and magnified by `k` so the cue text is 0.04 H (G1).
   * All sizes are app dp; multiply by hud.px (= dp * k) for film px.
   */
  hud: {
    k: (0.04 * H) / (15 * (W / 411)),
    px: (0.04 * H) / 15,
    ringSize: 56, // ScoreBadgeSize
    ringStroke: 2, // full thin ring in the score colour
    holdStroke: 4, // hold-progress arc from 12 o'clock, clockwise
    numeral: 18, // score numeral (15 for "100")
    cueText: 15, // StudioType.bodyStrong: Archivo Medium 15/22
    cuePadH: 16,
    cuePadV: 12,
    cueDot: 8,
    cueGap: 8,
    cueRadius: 12, // StudioShapes.Medium
    countdown: 96, // CountdownSize, Expanded Bold, Mannequin with an Ink 55% contour of 6 dp
    countdownContour: 6,
    scrim: "rgba(0, 0, 0, 0.45)", // Studio.ControlScrim
    guideAlpha: 0.55, // the template guide over the camera: SettingsRepository overlayOpacity default
    liveAlpha: 0.7, // the live skeleton: CameraOverlay LIVE_SKELETON_ALPHA
    liveStroke: 2.5, // dp, the live skeleton's line width
  },
  grid,
  ease,
  spring: {
    /** Critically damped, no overshoot: 52% of the travel by 4 f, 74% by 6 f, 93% by 10 f (measured). */
    land: { mass: 1, stiffness: 170, damping: 26, overshootClamping: true },
    /**
     * The film's ONE visible spring (the live arm's reach in B6): 4.3% past the mark at f10, within 1% by f15
     * (measured). Stretch it with spring({ durationInFrames: 30 }) for the storyboard's 24 f reach + settle.
     */
    theOne: { mass: 1, stiffness: 180, damping: 19, overshootClamping: false },
  },
  dur: {
    move: 10, // pooled median move
    enter: 12, // translate / scale entries
    opacity: 6, // opacity helper
    blur: 7,
    exit: 13,
    ctaEnter: 6,
  },
  signal: { forScore, mixOklab },
  /**
   * v2 footage (STORYBOARD.md v2, footage/manifest.json). Until the shutter every plate is printed in the Studio ramp:
   * Rec.709 luma (after the shot's `trim`) mapped from `ramp.shadow` (black point) to `ramp.highlight` (white point)
   * with a gentle S (`ramp.contrast`), saturation 0. Colour appears only as match feedback (act IV) and in the
   * captured photo (B7). No vignette, no glow.
   */
  footage: {
    ramp: { shadow: studio.ink, highlight: studio.sheet, contrast: 1.06, saturation: 0 },
    /** the chorus guides (B3): Mannequin fill at this alpha with the usual contour; act IV keeps hud.guideAlpha */
    chorusGuideAlpha: 0.62,
    /** a chorus guide lands from this scale about the person's mid-hip (softLand 5 f on beats, 4 f on eighths) */
    guideLandFrom: 1.25,
    guideLandAlpha: 0.9,
    /** the colour photo's slow push (B7) */
    photoPush: [1, 1.03] as [number, number],
    /** the chorus film camera's push across f144-248 */
    chorusPush: [1, 1.08] as [number, number],
  },
  /** Remotion's own colour interpolation, re-exported so scenes don't import it ad hoc. */
  interpolateColors,
} as const;

export type Theme = typeof theme;
export type EaseName = keyof typeof theme.ease;
