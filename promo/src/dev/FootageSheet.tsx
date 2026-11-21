/**
 * FootageSheet.tsx (dev, one still per prepared plate): proves the footage pipeline lines up. Not part of the film.
 *
 * Frame k shows plate SHEET_PLATES[k] in four cells, each the real Footage component (OffthreadVideo of the plate,
 * Studio ramp) at one plate frame, with what the app would draw over it:
 *   - the smoothed live skeleton (the app's stick paint, 70%, visible joints only) from useLandmarks;
 *   - the 33 raw MediaPipe landmarks as small red dots (so smoothing lag shows as a gap between dot and joint);
 *   - where the plate has a match file (a guide), the guide (the snap or act III's) in the app's see-through paint,
 *     coloured by the matcher's part scores.
 * Cells: the shot's first frame, the key frame (the guide's defining frame, else the middle), the frame where the body
 * moves fastest, and the last frame with a 1.35x push about the mid-hip (the overlay must stay on the person through
 * the shared transform).
 *
 *   npx remotion still src/index.ts Dev-FootageSheet out/footage-sheet-<k>.png --frame <k>
 */
import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { FigureG } from "../figure/Mannequin";
import type { Skeleton, Vec } from "../figure/types";
import { Footage } from "../footage/Footage";
import { plateById } from "../footage/plates";
import { drawable, guideFigures, matchRecordAt, partColors, peopleAt, type FootageMatchFile, type LandmarksFile } from "../footage/data";
import { useFootageJson } from "../footage/hooks";

export const SHEET_PLATES = [
  "b2-alive", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c9-alt1", "c9-alt2", "c1-alt1", "b5-live", "b5-live.1x",
];

const ASPECT = 9 / 16;

const midHip = (s: Skeleton | undefined): Vec | null => {
  const l = s?.joints.LEFT_HIP;
  const r = s?.joints.RIGHT_HIP;
  return l && r ? [(l[0] + r[0]) / 2, (l[1] + r[1]) / 2] : null;
};

/** Mean joint speed (scene units per frame) of the first person between consecutive plate frames. */
const fastestFrame = (lm: LandmarksFile, from: number, to: number): number => {
  let best = from;
  let bestSpeed = -1;
  for (let n = Math.max(1, from); n <= to; n++) {
    const a = peopleAt(lm, n - 1)[0];
    const b = peopleAt(lm, n)[0];
    if (!a || !b) continue;
    let sum = 0;
    let k = 0;
    for (const j of Object.keys(b.joints) as (keyof typeof b.joints)[]) {
      const p = a.joints[j];
      const q = b.joints[j];
      if (!p || !q) continue;
      sum += Math.hypot(q[0] - p[0], (q[1] - p[1]) / ASPECT);
      k++;
    }
    const v = k ? sum / k : 0;
    if (v > bestSpeed) {
      bestSpeed = v;
      best = n;
    }
  }
  return best;
};

const Label: React.FC<{ lines: string[] }> = ({ lines }) => (
  <div
    style={{
      position: "absolute",
      left: 16,
      top: 16,
      padding: "10px 14px",
      background: "rgba(23, 24, 26, 0.78)",
      color: theme.color.sheet,
      fontFamily: "Archivo",
      fontWeight: 500,
      fontSize: 30,
      lineHeight: 1.25,
      borderRadius: 8,
    }}
  >
    {lines.map((l) => (
      <div key={l}>{l}</div>
    ))}
  </div>
);

const Cell: React.FC<{
  id: string;
  n: number;
  lm: LandmarksFile;
  match: FootageMatchFile | null;
  title: string;
  push?: boolean;
}> = ({ id, n, lm, match, title, push }) => {
  const { W, H } = theme.size;
  const plate = plateById(id);
  const f = plate.filmFrom + n;
  const people = peopleAt(lm, n);
  const raw = lm.frames[n].raw;
  const hip = midHip(people[0]) ?? [0.5, 0.55];
  const record = match ? matchRecordAt(match, f) : null;
  const sourceT = (plate.sourceFrames[n] / plate.source.fps).toFixed(2);
  const dot = 7;
  return (
    <div style={{ position: "relative", width: W, height: H, overflow: "hidden" }}>
      <Footage
        id={id}
        frame={f}
        transform={push ? { scale: 1.35, anchor: hip as Vec, dx: 0, dy: 0 } : undefined}
      >
        <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ position: "absolute", left: 0, top: 0 }}>
          {match &&
            guideFigures(match).map((g, i) => (
              <FigureG
                key={`g${i}`}
                skeleton={g}
                aspect={ASPECT}
                height={H}
                style="silhouette"
                partColors={record ? partColors(record) : undefined}
                opacity={theme.hud.guideAlpha}
              />
            ))}
          {people.map((p, i) => (
            <FigureG
              key={`p${i}`}
              skeleton={drawable(p)}
              aspect={ASPECT}
              height={H}
              style="stick"
              fill={theme.color.mannequin}
              lineAlpha={theme.hud.liveAlpha}
              lineWidth={theme.hud.liveStroke * theme.hud.px}
              showFace={false}
            />
          ))}
          {raw.map((person, i) =>
            person.map((l, k) =>
              l[3] >= 0.5 ? <circle key={`r${i}-${k}`} cx={l[0] * W} cy={l[1] * H} r={dot} fill={theme.color.signalCam.miss} /> : null,
            ),
          )}
        </svg>
      </Footage>
      <Label
        lines={[
          `${id}  ${title}`,
          `film f${f}  plate n${n}  src ${sourceT} s`,
          `${people.length} person${people.length === 1 ? "" : "s"}${record ? `  score ${record.percent ?? "-"}  ${record.capture.state}` : ""}${push ? "  push 1.35" : ""}`,
        ]}
      />
    </div>
  );
};

export const FootageSheet: React.FC = () => {
  const k = Math.min(SHEET_PLATES.length - 1, useCurrentFrame());
  const id = SHEET_PLATES[k];
  const lm = useFootageJson<LandmarksFile>(`footage/${id}.landmarks.json`);
  // a plate has a match file only when it has a guide (act III, or a chorus snap that passed the snap rule)
  const match = useFootageJson<FootageMatchFile>(`footage/${id}.match.json`, { optional: true });
  const { W, H } = theme.size;
  if (!lm) return <AbsoluteFill style={{ background: theme.color.ink }} />;
  const plate = plateById(id);
  const first = plate.shotStart;
  const last = Math.min(plate.frames - 1, plate.shotStart + (plate.film[1] - plate.film[0]) - 1);
  const key = match ? Math.max(0, Math.min(plate.frames - 1, match.guide.definedAt.f - plate.filmFrom)) : Math.round((first + last) / 2);
  const fast = fastestFrame(lm, first, last);
  const cells: { n: number; title: string; push?: boolean }[] = [
    { n: first, title: "first frame" },
    { n: key, title: match ? "guide frame" : "middle" },
    { n: fast, title: "fastest move" },
    { n: last, title: "last frame", push: true },
  ];
  return (
    <AbsoluteFill style={{ background: theme.color.ink }}>
      {cells.map((c, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            left: (i % 2) * (W / 2),
            top: Math.floor(i / 2) * (H / 2),
            width: W,
            height: H,
            transform: "scale(0.5)",
            transformOrigin: "0 0",
          }}
        >
          <Cell id={id} n={c.n} lm={lm} match={match} title={c.title} push={c.push} />
        </div>
      ))}
    </AbsoluteFill>
  );
};
