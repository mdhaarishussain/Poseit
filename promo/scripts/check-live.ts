/**
 * check-live.ts: fails when public/live.json (the matcher's input) is stale, i.e. the performer or the guide was
 * edited without re-running `npm run match`, so the film would show verdicts for a body that is no longer on
 * screen. Part of `npm run check`.
 */
import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";

const file = path.join(process.cwd(), "public", "live.json");
const before = fs.readFileSync(file, "utf8");
execSync("node out/export-live.mjs", { stdio: "ignore" });
const after = fs.readFileSync(file, "utf8");
if (before !== after) {
  fs.writeFileSync(file, before);
  console.error("live.json is stale: the performer changed. Run `npm run match` to re-run the app's matcher.");
  process.exit(1);
}
console.log("live.json: matches the performer keyframes");
