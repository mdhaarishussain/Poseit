#!/usr/bin/env python3
"""Usage: python3 scripts/render_readme.py [OUT_DIR] renders README.md as GitHub shows it and screenshots it.

Needs pip install markdown pymdown-extensions, and Node with Playwright's Chromium. Writes readme.html and
readme-light.png, readme-dark.png (1280 px wide) and readme-mobile.png (390 px) into OUT_DIR, by default
build/readme. GitHub's markdown CSS, Mermaid and the badges are downloaded once into OUT_DIR/cache.
"""

from __future__ import annotations

import hashlib
import html
import os
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

import markdown

ROOT = Path(__file__).resolve().parent.parent
OUT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / "build" / "readme"
CACHE = OUT / "cache"

VENDOR = {
    "light.css": "https://cdn.jsdelivr.net/npm/github-markdown-css@5.8.1/github-markdown-light.css",
    "dark.css": "https://cdn.jsdelivr.net/npm/github-markdown-css@5.8.1/github-markdown-dark.css",
    "mermaid.min.js": "https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.min.js",
}


def fetch(url: str, name: str | None = None) -> Path:
    CACHE.mkdir(parents=True, exist_ok=True)
    if name is None:
        ext = ".svg" if "shields.io" in url else Path(url.split("?")[0]).suffix
        name = hashlib.sha1(url.encode()).hexdigest()[:16] + ext
    path = CACHE / name
    if not path.exists():
        req = urllib.request.Request(url, headers={"User-Agent": "readme-render"})
        with urllib.request.urlopen(req, timeout=30) as r:
            path.write_bytes(r.read())
    return path


def mermaid_fence(source, language, css_class, options, md, **kwargs):
    return f'<pre class="mermaid">{html.escape(source)}</pre>'


def to_html(text: str) -> str:
    md = markdown.Markdown(
        extensions=["tables", "md_in_html", "pymdownx.superfences", "sane_lists"],
        # GFM nests a list item indented by the parent's marker width ("- " is 2), not by 4 spaces.
        tab_length=2,
        extension_configs={
            "pymdownx.superfences": {
                "custom_fences": [{"name": "mermaid", "class": "mermaid", "format": mermaid_fence}]
            }
        },
    )
    # GFM starts an HTML block at any tag alone on its line, so <picture> isn't wrapped in a paragraph.
    md.block_level_elements.extend(["picture", "source"])
    body = md.convert(text)
    # Remote images (shields.io badges) come from the cache, so the page renders the same offline.
    body = re.sub(
        r'src="(https?://[^"]+)"',
        lambda m: f'src="{fetch(html.unescape(m.group(1))).as_uri()}"',
        body,
    )
    return body


PAGE = """<!doctype html>
<html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<base href="{base}">
<link rel="stylesheet" href="{light}" media="(prefers-color-scheme: light)">
<link rel="stylesheet" href="{dark}" media="(prefers-color-scheme: dark)">
<style>
  body {{ margin: 0; background: #ffffff; }}
  @media (prefers-color-scheme: dark) {{ body {{ background: #0d1117; }} }}
  .markdown-body {{ box-sizing: border-box; min-width: 200px; max-width: 980px; margin: 0 auto; padding: 45px; }}
  @media (max-width: 767px) {{ .markdown-body {{ padding: 15px; }} }}
  pre.mermaid {{ background: transparent; text-align: center; }}
</style>
</head><body>
<article class="markdown-body">
{body}
</article>
<script src="{mermaid}"></script>
<script>
  const dark = matchMedia('(prefers-color-scheme: dark)').matches;
  mermaid.initialize({{ startOnLoad: false, theme: dark ? 'dark' : 'default' }});
  mermaid.run().then(() => document.body.dataset.ready = '1')
    .catch(e => {{ document.body.dataset.ready = 'error: ' + e; }});
</script>
</body></html>
"""

SHOOT = r"""
const { chromium } = require('playwright');
(async () => {
  const [page_url, out] = process.argv.slice(1);
  const browser = await chromium.launch();
  const shots = [['light', 1280, 'light'], ['dark', 1280, 'dark'], ['mobile', 390, 'light']];
  for (const [name, width, scheme] of shots) {
    const ctx = await browser.newContext({
      viewport: { width, height: 900 }, deviceScaleFactor: 1, colorScheme: scheme,
    });
    const page = await ctx.newPage();
    await page.goto(page_url);
    await page.waitForFunction(() => document.body.dataset.ready, null, { timeout: 30000 });
    const ready = await page.evaluate(() => document.body.dataset.ready);
    if (ready !== '1') console.error('mermaid: ' + ready);
    await page.screenshot({ path: `${out}/readme-${name}.png`, fullPage: true });
    console.log(`${out}/readme-${name}.png`);
    await ctx.close();
  }
  await browser.close();
})();
"""


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    vendor = {k: fetch(url, k).as_uri() for k, url in VENDOR.items()}
    body = to_html((ROOT / "README.md").read_text(encoding="utf-8"))
    page = OUT / "readme.html"
    page.write_text(
        PAGE.format(
            base=ROOT.as_uri() + "/",
            light=vendor["light.css"],
            dark=vendor["dark.css"],
            mermaid=vendor["mermaid.min.js"],
            body=body,
        ),
        encoding="utf-8",
    )
    print(page)
    env = dict(os.environ)
    npm_root = subprocess.run(["npm", "root", "-g"], capture_output=True, text=True).stdout.strip()
    env["NODE_PATH"] = os.pathsep.join(filter(None, [env.get("NODE_PATH"), npm_root]))
    subprocess.run(["node", "-e", SHOOT, page.as_uri(), str(OUT)], check=True, env=env)


if __name__ == "__main__":
    main()
