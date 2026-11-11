#!/usr/bin/env python3
"""Regenerate Ghost Play's launcher icons from one square source image.

Usage:
    python3 tools/make_launcher_icons.py path/to/logo.png

Writes:
  * app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png + ic_launcher_round.png
  * app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml
    (adaptive icon: the artwork is inset to the safe zone of a 108dp canvas and
    the back layer is the current icon background colour)
  * app/src/main/res/drawable/ic_launcher_foreground.png (432dp xxhdpi foreground)
  * app/src/main/res/values/ic_launcher_background.xml (background colour)

Requires Pillow: python3 -m pip install Pillow
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"

LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive icons: 108dp canvas, artwork must stay inside the inner 72dp safe zone
# (66% of the canvas) so no launcher mask clips it.
ADAPTIVE_FOREGROUND_DP = 108
FOREGROUND_DENSITIES = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}
SAFE_ZONE = 0.66

ADAPTIVE_BG_COLOR = "#1B1B1F"


def load_square(source: Path) -> Image.Image:
    image = Image.open(source).convert("RGBA")
    side = min(image.width, image.height)
    left = (image.width - side) // 2
    top = (image.height - side) // 2
    return image.crop((left, top, left + side, top + side))


def write_legacy(icon: Image.Image) -> None:
    for folder, size in LEGACY_SIZES.items():
        target = RES / folder
        target.mkdir(parents=True, exist_ok=True)
        resized = icon.resize((size, size), Image.LANCZOS)
        resized.save(target / "ic_launcher.png")

        # Round variant: same artwork inside a circle, transparent elsewhere.
        mask = Image.new("L", (size, size), 0)
        from PIL import ImageDraw

        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        rounded = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        rounded.paste(resized, (0, 0), mask)
        rounded.save(target / "ic_launcher_round.png")


def write_adaptive_foreground(icon: Image.Image) -> None:
    for folder, size in FOREGROUND_DENSITIES.items():
        target = RES / folder
        target.mkdir(parents=True, exist_ok=True)
        canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        inner = int(size * SAFE_ZONE)
        artwork = icon.resize((inner, inner), Image.LANCZOS)
        offset = (size - inner) // 2
        canvas.paste(artwork, (offset, offset), artwork)
        canvas.save(target / "ic_launcher_foreground.png")

    background_values = RES / "values" / "ic_launcher_background.xml"
    background_values.parent.mkdir(parents=True, exist_ok=True)
    background_values.write_text(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        "<resources>\n"
        f"    <color name=\"ic_launcher_background\">{ADAPTIVE_BG_COLOR}</color>\n"
        "</resources>\n",
        encoding="utf-8",
    )


def write_adaptive_xml() -> None:
    folder = RES / "mipmap-anydpi-v26"
    folder.mkdir(parents=True, exist_ok=True)
    xml = (
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
        "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
        "    <background android:drawable=\"@color/ic_launcher_background\" />\n"
        "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
        "    <monochrome android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
        "</adaptive-icon>\n"
    )
    (folder / "ic_launcher.xml").write_text(xml, encoding="utf-8")
    (folder / "ic_launcher_round.xml").write_text(xml, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    source = Path(sys.argv[1])
    if not source.is_file():
        print(f"No such file: {source}")
        return 2
    icon = load_square(source)
    write_legacy(icon)
    write_adaptive_foreground(icon)
    write_adaptive_xml()
    print(f"Launcher icons regenerated from {source} ({icon.width}x{icon.height})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
