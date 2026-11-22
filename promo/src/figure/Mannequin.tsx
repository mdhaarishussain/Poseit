/**
 * Mannequin.tsx: draws a skeleton with the app's lay-figure geometry (shapes.ts = FigureShapes) and the app's paint
 * rules (app/.../ui/components/PoseFigure.kt drawPoseFigure).
 *
 *   <FigureG .../>     an SVG <g>, for use inside your own <svg> (many figures in one svg is much faster)
 *   <Mannequin .../>   a full-frame absolutely positioned <svg> containing one FigureG
 *
 * Props (both):
 *   skeleton       Skeleton (joints in frame coordinates, see types.ts)
 *   aspect         frame width / height (pose.frameAspect)
 *   x, y, height   where the FRAME goes: its top-left corner and pixel height (width = height * aspect)
 *   flipX          mirror for display inside the frame (front-camera preview), left/right colouring kept
 *   style          "silhouette" (default): fill + contour; "outline": contour line only, interior see-through;
 *                  "stick": the app's live-skeleton drawing (bone lines, joint dots, head ring)
 *   fill           body colour (silhouette) / line colour (outline, stick) when no part colour is given
 *   contour        contour colour (silhouette; null = none); contourAlpha (default Ink 55%)
 *   partColors     per body part colour (match feedback); overrides fill
 *   opacity        whole-figure opacity, applied once (overlaps don't darken), like the app's saveLayer
 *   contourWidth   px; default the app's auto rule: 3% of the body unit, clamped to 1.25-2.25 dp
 *   lineWidth      stick line px; default 9% of the unit clamped to 1.5-3 dp (the camera uses 2.5 dp)
 *   lineAlpha      stick only: per-element alpha like the app (live skeleton = 0.7)
 *   showFace       the face dot (silhouette: faceColor Ink; outline: the head's line colour)
 *   dp             px per dp for the clamps (default theme.size.dp)
 */
import React, { useId, useMemo } from "react";
import { theme } from "../theme";
import { BONES, buildShapes, estimatedUnit, headCenter, noseMarker, toIsotropic } from "./shapes";
import type { BodyPart, JointName, Shape, Skeleton, Vec } from "./types";

export type FigureStyle = "silhouette" | "outline" | "stick";

export type FigureProps = {
  skeleton: Skeleton;
  aspect: number;
  x?: number;
  y?: number;
  height: number;
  flipX?: boolean;
  style?: FigureStyle;
  fill?: string;
  contour?: string | null;
  contourAlpha?: number;
  partColors?: Partial<Record<BodyPart, string>>;
  opacity?: number;
  contourWidth?: number;
  lineWidth?: number;
  lineAlpha?: number;
  showFace?: boolean;
  faceColor?: string;
  dp?: number;
};

const n = (v: number) => Math.round(v * 100) / 100;

/** Clockwise (on screen, y down) polygon subpath, so nonzero fill unions every subpath. */
const polyD = (pts: Vec[]) => {
  let area = 0;
  for (let i = 0; i < pts.length; i++) {
    const a = pts[i], b = pts[(i + 1) % pts.length];
    area += a[0] * b[1] - b[0] * a[1];
  }
  const p = area < 0 ? [...pts].reverse() : pts;
  return `M${p.map((q) => `${n(q[0])} ${n(q[1])}`).join("L")}Z`;
};
const circleD = (c: Vec, r: number) =>
  `M${n(c[0] + r)} ${n(c[1])}A${n(r)} ${n(r)} 0 1 1 ${n(c[0] - r)} ${n(c[1])}A${n(r)} ${n(r)} 0 1 1 ${n(c[0] + r)} ${n(c[1])}Z`;

/** The app's tapered capsule: a circle at each end and a quad offset along the normal (PoseFigure.kt). */
const capsuleD = (a: Vec, b: Vec, ra: number, rb: number) => {
  let d = circleD(a, ra) + circleD(b, rb);
  const dx = b[0] - a[0], dy = b[1] - a[1];
  const len = Math.hypot(dx, dy);
  if (len < 0.5) return d;
  const nx = -dy / len, ny = dx / len;
  d += polyD([
    [a[0] + nx * ra, a[1] + ny * ra],
    [b[0] + nx * rb, b[1] + ny * rb],
    [b[0] - nx * rb, b[1] - ny * rb],
    [a[0] - nx * ra, a[1] - ny * ra],
  ]);
  return d;
};

type Px = (p: Vec) => Vec;

const shapeD = (s: Shape, px: Px, h: number, inflate: number): string => {
  if (s.t === "circle") return circleD(px(s.c), s.r * h + inflate);
  if (s.t === "capsule") return capsuleD(px(s.a), px(s.b), (s.wa * h) / 2 + inflate, (s.wb * h) / 2 + inflate);
  return polyD(s.pts.map(px));
};

/** Consecutive shapes of the same part, in draw order (torso, legs, arms, head). */
const runsOf = (shapes: Shape[]) => {
  const runs: { part: BodyPart; shapes: Shape[] }[] = [];
  for (const s of shapes) {
    const last = runs[runs.length - 1];
    if (last && last.part === s.part) last.shapes.push(s);
    else runs.push({ part: s.part, shapes: [s] });
  }
  return runs;
};

const jointPart = (j: JointName): BodyPart => {
  if (j === "NOSE" || j.endsWith("_EYE") || j.endsWith("_EAR")) return "HEAD";
  if (j.endsWith("SHOULDER") || j.endsWith("HIP")) return "TORSO";
  const left = j.startsWith("LEFT_");
  if (j.endsWith("ELBOW") || j.endsWith("WRIST") || j.endsWith("HAND")) return left ? "LEFT_ARM" : "RIGHT_ARM";
  return left ? "LEFT_LEG" : "RIGHT_LEG";
};

export const FigureG: React.FC<FigureProps> = ({
  skeleton,
  aspect,
  x = 0,
  y = 0,
  height: h,
  flipX = false,
  style = "silhouette",
  fill = theme.color.mannequin,
  contour = theme.color.contour,
  contourAlpha = theme.color.contourAlpha,
  partColors,
  opacity = 1,
  contourWidth,
  lineWidth,
  lineAlpha = 1,
  showFace = true,
  faceColor = theme.color.ink,
  dp = theme.size.dp,
}) => {
  const maskId = "fm" + useId().replace(/[^a-zA-Z0-9_-]/g, "");
  const frameW = h * aspect;
  const px: Px = (p) => (flipX ? [x + frameW - p[0] * h, y + p[1] * h] : [x + p[0] * h, y + p[1] * h]);
  const unitPx = estimatedUnit(skeleton, aspect) * h;
  const colorFor = (part: BodyPart) => partColors?.[part] ?? fill;

  const geo = useMemo(() => {
    if (style === "stick") return null;
    const shapes = buildShapes(skeleton, aspect);
    const stroke = contourWidth ?? Math.min(2.25 * dp, Math.max(1.25 * dp, unitPx * 0.03));
    const runs = runsOf(shapes).map((r) => ({
      part: r.part,
      fillD: r.shapes.map((s) => shapeD(s, px, h, 0)).join(""),
      inflatedD: r.shapes.map((s) => shapeD(s, px, h, s.t === "polygon" ? 0 : stroke)).join(""),
      polyD: r.shapes.filter((s) => s.t === "polygon").map((s) => shapeD(s, px, h, 0)).join(""),
    }));
    const nose = showFace ? noseMarker(skeleton, aspect) : null;
    return { runs, stroke, nose };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [skeleton, aspect, x, y, h, flipX, style, contourWidth, dp, showFace]);

  if (style === "stick") {
    const iso = toIsotropic(skeleton, aspect);
    const w = lineWidth ?? Math.min(3 * dp, Math.max(1.5 * dp, unitPx * 0.09));
    const u = estimatedUnit(skeleton, aspect);
    const ls = skeleton.joints.LEFT_SHOULDER, rs = skeleton.joints.RIGHT_SHOULDER;
    const ms: Vec | null = ls && rs ? [((ls[0] + rs[0]) / 2) * aspect, (ls[1] + rs[1]) / 2] : null;
    const head = headCenter(iso, u, ms);
    const els: React.ReactNode[] = [];
    BONES.forEach(([name, from, to, part]) => {
      const a = iso[from], b = iso[to];
      if (!a || !b) return;
      const [x1, y1] = px(a), [x2, y2] = px(b);
      els.push(<line key={name} x1={n(x1)} y1={n(y1)} x2={n(x2)} y2={n(y2)} stroke={colorFor(part)} strokeOpacity={lineAlpha} strokeWidth={w} strokeLinecap="round" />);
    });
    if (head) {
      const [hx, hy] = px(head);
      if (ms) {
        const [mx, my] = px(ms);
        els.push(<line key="neck" x1={n(mx)} y1={n(my)} x2={n(hx)} y2={n(hy)} stroke={colorFor("HEAD")} strokeOpacity={lineAlpha} strokeWidth={w} strokeLinecap="round" />);
      }
      els.push(<circle key="head" cx={n(hx)} cy={n(hy)} r={n(unitPx * 0.2)} fill="none" stroke={colorFor("HEAD")} strokeOpacity={lineAlpha} strokeWidth={w} />);
    }
    (Object.entries(iso) as [JointName, Vec][]).forEach(([j, p]) => {
      if (jointPart(j) === "HEAD") return;
      const [cx, cy] = px(p);
      els.push(<circle key={`j-${j}`} cx={n(cx)} cy={n(cy)} r={n(w * 0.75)} fill={colorFor(jointPart(j))} fillOpacity={lineAlpha} />);
    });
    return <g opacity={opacity}>{els}</g>;
  }

  const { runs, stroke, nose } = geo!;
  const allInflated = runs.map((r) => r.inflatedD).join("");
  const allFill = runs.map((r) => r.fillD).join("");
  const allPoly = runs.map((r) => r.polyD).join("");
  const faceDot = nose
    ? (() => {
        const [cx, cy] = px(nose.c);
        const c = style === "outline" ? colorFor("HEAD") : faceColor;
        return <circle cx={n(cx)} cy={n(cy)} r={n(Math.max(2, nose.r * h))} fill={c} />;
      })()
    : null;

  if (style === "outline") {
    return (
      <g opacity={opacity}>
        <defs>
          <mask id={maskId} maskUnits="userSpaceOnUse" x={-1e4} y={-1e4} width={3e4} height={3e4}>
            <path d={allInflated} fill="#fff" />
            {allPoly && <path d={allPoly} fill="none" stroke="#fff" strokeWidth={stroke * 2} strokeLinejoin="round" />}
            <path d={allFill} fill="#000" />
          </mask>
        </defs>
        <g mask={`url(#${maskId})`}>
          {runs.map((r, i) => (
            <g key={i}>
              <path d={r.inflatedD} fill={colorFor(r.part)} />
              {r.polyD && <path d={r.polyD} fill="none" stroke={colorFor(r.part)} strokeWidth={stroke * 2} strokeLinejoin="round" />}
            </g>
          ))}
        </g>
        {faceDot}
      </g>
    );
  }

  return (
    <g opacity={opacity}>
      {contour && (
        <g opacity={contourAlpha}>
          <path d={allInflated} fill={contour} />
          {allPoly && <path d={allPoly} fill="none" stroke={contour} strokeWidth={stroke * 2} strokeLinejoin="round" />}
        </g>
      )}
      {runs.map((r, i) => (
        <path key={i} d={r.fillD} fill={colorFor(r.part)} />
      ))}
      {faceDot}
    </g>
  );
};

/** One figure in its own full-frame svg (absolutely positioned over the composition). */
export const Mannequin: React.FC<FigureProps & { style?: FigureStyle; svgStyle?: React.CSSProperties }> = ({ svgStyle, ...props }) => (
  <svg
    width={theme.size.W}
    height={theme.size.H}
    viewBox={`0 0 ${theme.size.W} ${theme.size.H}`}
    style={{ position: "absolute", inset: 0, overflow: "visible", ...svgStyle }}
  >
    <FigureG {...props} />
  </svg>
);
