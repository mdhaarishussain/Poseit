/** export-events.ts: prints src/events.ts (and the VO slot, src/audio/voiceover.ts) as JSON to out/events.json (npm run events). */
import fs from "node:fs";
import path from "node:path";
import { CUES, CUTS, EVENTS, HITS, SILENCES } from "../src/events";
import { VO_LINES } from "../src/audio/voiceover";

const out = path.join(process.cwd(), "out", "events.json");
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify({ fps: 30, frames: 664, hits: HITS, cuts: CUTS, silences: SILENCES, cues: CUES, vo: VO_LINES, events: EVENTS }, null, 1));
console.log(`events.json: ${EVENTS.length} events; hits ${JSON.stringify(HITS)}; cuts ${CUTS.join(", ")}`);
