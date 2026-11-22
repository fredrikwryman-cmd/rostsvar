#!/usr/bin/env python3
"""Ritar Tap & Talks Android-ikon ur samma grafik som iOS-appen.

Kör från mappen android:

    python tools/make_icons.py

Utgångspunkten är den liggande grafiken i ios/Tools (appicon.svg och
make_icons.py), alltså kaninmikrofonen i grönt mot den mörka bakgrunden.
Här ritas den i kvadratisk variant, som Android vill ha den:

  mipmap-*/ic_launcher.png             kvadratisk helbild, äldre startprogram
  mipmap-*/ic_launcher_background.png  bakgrundslagret, 108 dp
  mipmap-*/ic_launcher_foreground.png  symbollagret, 108 dp, genomskinligt
  mipmap-anydpi-v26/ic_launcher.xml    lagren hopsatta (skrivs för hand)

Formerna är översatta ur SVG:ns koordinater, samma mått som den liggande
varianten, så att de två ikonerna är samma motiv.
"""
from __future__ import annotations

import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ANDROID = os.path.dirname(HERE)
RES = os.path.join(ANDROID, "app", "src", "main", "res")

# Bakgrunden från appicon.svg: #3b3e42 överst, #1c1e20 nederst.
TOP = (0x3B, 0x3E, 0x42)
BOTTOM = (0x1C, 0x1E, 0x20)
# Symbolen ritas i kupolens gröna, samma accent som skärmen använder.
INK = (0x3F, 0xE0, 0x8A)

# Symbolens yttermått i appicon.svg:s koordinater, med gruppens
# translate(0,40) inräknad. Öronens hörn är uppmätta efter rotationen.
SYMBOL = (376.0, 271.8, 648.0, 756.0)

# Täthet och sidmått. Helbilden är ikonens vanliga storlek, lagren är 108 dp.
DENSITIES = [
    ("mdpi", 48, 108),
    ("hdpi", 72, 162),
    ("xhdpi", 96, 216),
    ("xxhdpi", 144, 324),
    ("xxxhdpi", 192, 432),
]


def backdrop(width: int, height: int) -> Image.Image:
    """Den mörka bakgrunden, lodrät övergång som i den liggande grafiken."""
    image = Image.new("RGBA", (width, height), BOTTOM + (255,))
    draw = ImageDraw.Draw(image)
    for y in range(height):
        mix = y / max(height - 1, 1)
        draw.line(
            [(0, y), (width, y)],
            fill=tuple(round(TOP[i] + (BOTTOM[i] - TOP[i]) * mix) for i in range(3)) + (255,),
        )
    return image


def mark(size: int, share: float, transparent: bool) -> Image.Image:
    """Kaninmikrofonen centrerad på en kvadrat, ritad i fyra gångers upplösning.

    share är hur stor del av kvadratens höjd symbolen får ta. Lagret som
    Android maskar behöver marginal, helbilden kan ta mer plats.
    """
    scale = 4
    side = size * scale
    image = (Image.new("RGBA", (side, side), (0, 0, 0, 0))
             if transparent else backdrop(side, side))
    draw = ImageDraw.Draw(image)

    x0, y0, x1, y1 = SYMBOL
    sw, sh = x1 - x0, y1 - y0
    s = min((side * share) / sh, (side * share) / sw)
    ox = side / 2 - (x0 + sw / 2) * s
    oy = side / 2 - (y0 + sh / 2) * s

    def point(x: float, y: float) -> tuple[float, float]:
        return (x * s + ox, y * s + oy)

    def bounds(cx: float, cy: float, rx: float, ry: float) -> list[float]:
        left, top = point(cx - rx, cy - ry)
        right, bottom = point(cx + rx, cy + ry)
        return [left, top, right, bottom]

    stroke = max(1.0, 28 * s)
    cap = stroke / 2
    paint = INK + (255,)

    # Öronen: ellipser roterade kring sin egen mittpunkt, som i SVG:n.
    for cx, angle in ((462.0, -12.0), (562.0, 12.0)):
        layer = Image.new("L", (side, side), 0)
        ImageDraw.Draw(layer).ellipse(bounds(cx, 370, 40, 100), fill=255)
        layer = layer.rotate(angle, resample=Image.BICUBIC, center=point(cx, 370))
        image.paste(paint, mask=layer)

    # Mikrofonkroppen.
    left, top = point(427, 425)
    right, bottom = point(597, 625)
    draw.rounded_rectangle([left, top, right, bottom], radius=85 * s, fill=paint)

    # Bygeln: två korta lodräta streck och en halvcirkel under dem.
    for x in (390, 634):
        draw.line([point(x, 525), point(x, 560)], fill=paint, width=round(stroke))
        cx, cy = point(x, 525)
        draw.ellipse([cx - cap, cy - cap, cx + cap, cy + cap], fill=paint)
    draw.arc(bounds(512, 560, 122, 122), start=0, end=180, fill=paint, width=round(stroke))

    # Stativets stolpe och fot.
    draw.line([point(512, 682), point(512, 740)], fill=paint, width=round(stroke))
    draw.line([point(450, 742), point(574, 742)], fill=paint, width=round(stroke))
    for x in (450, 574):
        cx, cy = point(x, 742)
        draw.ellipse([cx - cap, cy - cap, cx + cap, cy + cap], fill=paint)

    return image.resize((size, size), Image.LANCZOS)


def write(image: Image.Image, folder: str, name: str) -> None:
    path = os.path.join(RES, folder, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, "PNG", optimize=True)
    print("skrev", os.path.relpath(path, ANDROID).replace(os.sep, "/"),
          f"{image.size[0]}x{image.size[1]}", image.mode)


def main() -> None:
    for density, full, layer in DENSITIES:
        folder = f"mipmap-{density}"
        # Helbilden: symbolen tar 70 procent, bakgrunden resten.
        write(mark(full, 0.70, transparent=False), folder, "ic_launcher.png")
        write(mark(full, 0.70, transparent=False), folder, "ic_launcher_round.png")
        # Lagren: 108 dp, där bara mitten på 72 dp alltid syns. Symbolen
        # hålls därför inom knappt halva ytan.
        write(backdrop(layer, layer).convert("RGB"), folder, "ic_launcher_background.png")
        write(mark(layer, 0.46, transparent=True), folder, "ic_launcher_foreground.png")
    print(f"{len(DENSITIES)} tätheter, fyra filer var.")


if __name__ == "__main__":
    main()
    print("klart")
