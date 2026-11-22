// Remotion CLI config. Rendering settings for the Poseit promo.
// The film is crisp (no motion blur), so JPEG frames are fine for drafts; finals use --crf 16.
import { Config } from "@remotion/cli/config";
import fs from "node:fs";

Config.setVideoImageFormat("jpeg");
Config.setJpegQuality(92);
Config.setOverwriteOutput(true);
Config.setConcurrency(4);

// Remotion downloads its own headless shell; in sandboxes without network use Playwright's.
// Override with REMOTION_BROWSER=/path/to/chrome-headless-shell, or pass --browser-executable.
const candidates = [
  process.env.REMOTION_BROWSER,
  "/opt/pw-browsers/chromium_headless_shell-1243/chrome-headless-shell-linux64/chrome-headless-shell",
].filter((p): p is string => Boolean(p));
const browser = candidates.find((p) => fs.existsSync(p));
if (browser) Config.setBrowserExecutable(browser);
