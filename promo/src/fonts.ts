/**
 * fonts.ts: registers the app's six static Archivo cuts (app/src/main/res/font, OFL; copied to public/fonts).
 *
 *   "Archivo"           400 regular, 500 medium, 600 semibold      (font-stretch: normal)
 *   "Archivo Expanded"  500 medium, 600 semibold, 700 bold          (the wide cut, wdth 125)
 * The expanded cuts are ALSO registered under "Archivo" with font-stretch 125%, so either
 *   { fontFamily: "Archivo Expanded", fontWeight: 600 }  or  { fontFamily: "Archivo", fontStretch: "125%", fontWeight: 600 }
 * selects the same file. Prefer the first (theme.font.wide); it cannot fall back to a synthesized width.
 *
 * Call ensureFonts() once at module load (Root.tsx does). It holds the render with delayRender() until every
 * face has loaded, so no frame is ever drawn in a fallback face.
 *
 * fitTextToWidth(text, family, weight, width) returns the font size (px) whose INK width (glyph bounds, not the
 * advance box) equals `width`, measured on a canvas once the fonts are loaded. Used for the wordmark (0.50 W).
 */
import { continueRender, delayRender, staticFile } from "remotion";

type Face = { file: string; family: string; weight: number; stretch: string };

const FACES: Face[] = [
  { file: "archivo_regular.ttf", family: "Archivo", weight: 400, stretch: "100%" },
  { file: "archivo_medium.ttf", family: "Archivo", weight: 500, stretch: "100%" },
  { file: "archivo_semibold.ttf", family: "Archivo", weight: 600, stretch: "100%" },
  { file: "archivo_expanded_medium.ttf", family: "Archivo Expanded", weight: 500, stretch: "100%" },
  { file: "archivo_expanded_semibold.ttf", family: "Archivo Expanded", weight: 600, stretch: "100%" },
  { file: "archivo_expanded_bold.ttf", family: "Archivo Expanded", weight: 700, stretch: "100%" },
  { file: "archivo_expanded_medium.ttf", family: "Archivo", weight: 500, stretch: "125%" },
  { file: "archivo_expanded_semibold.ttf", family: "Archivo", weight: 600, stretch: "125%" },
  { file: "archivo_expanded_bold.ttf", family: "Archivo", weight: 700, stretch: "125%" },
];

let started = false;

export const ensureFonts = () => {
  if (started || typeof document === "undefined") return;
  started = true;
  const handle = delayRender("Loading Archivo");
  Promise.all(
    FACES.map((f) => {
      const face = new FontFace(f.family, `url(${staticFile(`fonts/${f.file}`)}) format("truetype")`, {
        weight: String(f.weight),
        stretch: f.stretch,
        style: "normal",
        display: "block",
      });
      return face.load().then((loaded) => {
        document.fonts.add(loaded);
      });
    }),
  )
    .then(() => continueRender(handle))
    .catch((err) => {
      // Never render in a fallback face: fail loudly.
      console.error(err);
      throw err;
    });
};

let measureCtx: CanvasRenderingContext2D | null = null;

/** Font size (px) at which `text` has an ink width of `width` px. */
export const fitTextToWidth = (text: string, family: string, weight: number, width: number): number => {
  if (typeof document === "undefined") return width / Math.max(1, text.length * 0.6);
  measureCtx ??= document.createElement("canvas").getContext("2d");
  const ctx = measureCtx!;
  const probe = 200;
  ctx.font = `${weight} ${probe}px "${family}"`;
  const m = ctx.measureText(text);
  const ink = m.actualBoundingBoxLeft + m.actualBoundingBoxRight;
  return (probe * width) / Math.max(1, ink);
};
