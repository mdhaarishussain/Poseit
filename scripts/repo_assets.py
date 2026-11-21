#!/usr/bin/env python3
"""Turns the renders of RepoAssetsTest into the repository's images in docs/images/.

    ./gradlew :app:testDebugUnitTest --tests 'com.poseit.app.screens.RepoAssetsTest'
    python3 scripts/repo_assets.py

Needs Pillow and qrcode (pip install pillow qrcode). If OpenCV is installed (opencv-python-headless), the QR code
is decoded again to check it.

- banner-light.png, banner-dark.png (1600x560) and social-preview.png (1280x640) are copied as rendered.
- screens/*.png: the phone renders (1200x2580) scaled to 560 px wide, with the Studio Medium radius (12dp, 17 px at
  this scale) on the corners.
- icon.png: the adaptive launcher icon, flat, 256x256, cut to the disc the app ships for launchers without
  adaptive icons (res/mipmap/ic_launcher.xml).
- qr-latest-release.png: the latest release page as a QR code, Ink on Paper, 4-module quiet zone.

Each image is saved as a palette PNG when that keeps every pixel within a small colour distance of the original
(no visible banding), otherwise as an optimised full-colour PNG.
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RENDERS = ROOT / "app" / "build" / "repo-assets"
OUT = ROOT / "docs" / "images"

RELEASE_URL = "https://github.com/mdhaarishussain/Poseit/releases/latest"
INK = (0x17, 0x18, 0x1A)
PAPER = (0xE4, 0xE5, 0xE1)

SCREEN_WIDTH = 560
# The phone renders are 400dp wide, so 560 px is 1.4 px per dp: Medium (12dp) is 17 px.
SCREEN_RADIUS = round(12 * SCREEN_WIDTH / 400)

# Render name -> published name, in the order the README shows them.
SCREENS = {
    "screen-1-library": "1-library",
    "screen-2-detail": "2-pose-detail",
    "screen-3-camera": "3-camera",
    "screen-4-editor": "4-editor",
    "screen-5-create": "5-pose-from-photo",
    "screen-6-settings": "6-settings",
}

# Largest per-channel difference a palette version may introduce before we keep full colour.
MAX_PALETTE_ERROR = 6


def palette_or_full(im: Image.Image) -> Image.Image:
    """A 256-colour version of [im] if it is visually identical, else [im] itself."""
    rgba = im.convert("RGBA")
    quant = rgba.quantize(colors=256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
    back = quant.convert("RGBA")
    # Compare only where the pixel is visible: the colour of a fully transparent pixel doesn't matter.
    visible = rgba.getchannel("A").point(lambda a: 255 if a > 0 else 0)
    diff = 0
    for band in range(4):
        d = ImageChops.difference(rgba.getchannel(band), back.getchannel(band))
        diff = max(diff, ImageChops.multiply(d, visible).getextrema()[1])
    return quant if diff <= MAX_PALETTE_ERROR else rgba


def save(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    out = palette_or_full(im)
    if out.mode == "RGBA" and out.getextrema()[3][0] == 255:
        out = out.convert("RGB")
    out.save(path, optimize=True)
    kind = "palette" if out.mode == "P" else out.mode
    print(f"{path.relative_to(ROOT)}  {out.width}x{out.height}  {path.stat().st_size / 1024:.0f} KB  {kind}")


def rounded(im: Image.Image, radius: int) -> Image.Image:
    """[im] with transparent, anti-aliased rounded corners (the mask is drawn at 4x and scaled down).
    A radius of half the size or more gives a circle."""
    scale = 4
    mask = Image.new("L", (im.width * scale, im.height * scale), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, mask.width - 1, mask.height - 1), radius=radius * scale, fill=255
    )
    out = im.convert("RGBA")
    out.putalpha(mask.resize(im.size, Image.Resampling.LANCZOS))
    return out


def render(name: str) -> Image.Image:
    path = RENDERS / f"{name}.png"
    if not path.exists():
        sys.exit(f"Missing {path}. Run RepoAssetsTest first.")
    return Image.open(path)


def qr_code() -> Image.Image:
    import qrcode
    from qrcode.constants import ERROR_CORRECT_M

    qr = qrcode.QRCode(error_correction=ERROR_CORRECT_M, box_size=1, border=4)
    qr.add_data(RELEASE_URL)
    qr.make(fit=True)
    modules = qr.get_matrix()  # includes the quiet zone
    box = max(1, round(360 / len(modules)))
    img = qr.make_image(fill_color=INK, back_color=PAPER).get_image().convert("RGB")
    return img.resize((len(modules) * box, len(modules) * box), Image.Resampling.NEAREST)


def check_qr(path: Path) -> None:
    try:
        import cv2
    except ImportError:
        print("  (OpenCV not installed: QR code not decoded again)")
        return
    text, _, _ = cv2.QRCodeDetector().detectAndDecode(cv2.imread(str(path)))
    if text != RELEASE_URL:
        sys.exit(f"QR code decodes to {text!r}, expected {RELEASE_URL!r}")
    print(f"  decodes to {text}")


def main() -> None:
    for name in ("banner-light", "banner-dark", "social-preview"):
        save(render(name), OUT / f"{name}.png")

    for src, dst in SCREENS.items():
        im = render(src).convert("RGB")
        h = round(im.height * SCREEN_WIDTH / im.width)
        im = im.resize((SCREEN_WIDTH, h), Image.Resampling.LANCZOS)
        save(rounded(im, SCREEN_RADIUS), OUT / "screens" / f"{dst}.png")

    # The disc of res/mipmap/ic_launcher.xml, the app's own flat form of the adaptive icon.
    icon = render("icon")
    save(rounded(icon, icon.width), OUT / "icon.png")

    qr_path = OUT / "qr-latest-release.png"
    save(qr_code(), qr_path)
    check_qr(qr_path)


if __name__ == "__main__":
    main()
