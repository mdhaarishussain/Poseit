/** export-events.ts: prints src/events.ts as JSON to out/events.json (npm run events). */
import fs from "node:fs";
import path from "node:path";
import { CUTS, EVENTS, HITS, SILENCES } from "../src/events";

const out = path.join(process.cwd(), "out", "events.json");
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify({ fps: 30, frames: 664, hits: HITS, cuts: CUTS, silences: SILENCES, events: EVENTS }, null, 1));
console.log(`events.json: ${EVENTS.length} events; hits ${JSON.stringify(HITS)}; cuts ${CUTS.join(", ")}`);
