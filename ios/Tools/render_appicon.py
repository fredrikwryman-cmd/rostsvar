#!/usr/bin/env python3
"""Renderar appikonen ur Tools/appicon.svg till AppIcon.appiconset.

Kör från mappen ios:

    python Tools/render_appicon.py

Byggaren saknar SVG-rasterare, så formerna i appicon.svg ritas om med Pillow,
samma verktyg som resten av projektets ikoner. Ritas i fyra gångers upplösning
och skalas ned. Resultatet sparas utan alfakanal, vilket App Store kräver.

Ändras appicon.svg måste ritningen här ändras med den. Filen är avsiktligt en
ordagrann översättning av SVG:ns element, i samma ordning.
"""

from __future__ import annotations

import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
IOS = os.path.dirname(HERE)
TARGET = os.path.join(IOS, "App", "Assets.xcassets", "AppIcon.appiconset", "AppIcon-1024.png")

SIDE = 1024
SCALE = 4
BIG = SIDE * SCALE

INK = (15, 61, 46)  # #0f3d2e


def px(value: float) -> float:
    """SVG-koordinat till pixel i arbetsupplösningen."""
    return value * SCALE


def box(cx: float, cy: float, rx: float, ry: float | None = None):
    ry = rx if ry is None else ry
    return [px(cx - rx), px(cy - ry), px(cx + rx), px(cy + ry)]


def ramp(stops):
    """256 steg ur (offset, (r, g, b))-stopp, linjärt mellan dem."""
    table = []
    for index in range(256):
        t = index / 255
        lower = stops[0]
        upper = stops[-1]
        for first, second in zip(stops, stops[1:]):
            if first[0] <= t <= second[0]:
                lower, upper = first, second
                break
        span = upper[0] - lower[0]
        local = 0.0 if span == 0 else (t - lower[0]) / span
        table.append(tuple(
            round(lower[1][i] + (upper[1][i] - lower[1][i]) * local) for i in range(3)
        ))
    return table


def vertical(top: float, bottom: float, stops) -> Image.Image:
    """Lodrät linjär gradient över hela ytan, mättad utanför top..bottom."""
    table = ramp(stops)
    image = Image.new("RGB", (BIG, BIG))
    draw = ImageDraw.Draw(image)
    y0, y1 = px(top), px(bottom)
    for y in range(BIG):
        t = 0.0 if y1 == y0 else (y - y0) / (y1 - y0)
        draw.line([(0, y), (BIG, y)], fill=table[max(0, min(255, round(t * 255)))])
    return image


def radial(cx: float, cy: float, radius: float, stops) -> Image.Image:
    """Radiell gradient. Ritas som koncentriska cirklar utifrån och in."""
    table = ramp(stops)
    image = Image.new("RGB", (BIG, BIG), table[255])
    draw = ImageDraw.Draw(image)
    steps = 512
    for step in range(steps, 0, -1):
        t = step / steps
        r = radius * t
        draw.ellipse(box(cx, cy, r), fill=table[round(t * 255)])
    return image


def rotated(canvas: Image.Image, bounds, angle: float, pivot, fill):
    """Ellips som roteras kring sin egen mittpunkt, som SVG:ns rotate()."""
    layer = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(layer).ellipse(bounds, fill=255)
    layer = layer.rotate(angle, resample=Image.BICUBIC,
                         center=(px(pivot[0]), px(pivot[1])))
    canvas.paste(fill, mask=layer)


def render() -> Image.Image:
    # <rect fill="url(#bg)"/>
    image = vertical(0, SIDE, [(0.0, (0x3b, 0x3e, 0x42)), (1.0, (0x1c, 0x1e, 0x20))])

    # <circle r="402" fill="url(#ring)" filter="url(#shadow)"/>
    # feDropShadow dx=0 dy=22 stdDeviation=26 flood-opacity=0.55
    shadow = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(shadow).ellipse(box(512, 512, 402), fill=round(255 * 0.55))
    shadow = ImageChops.offset(shadow, 0, round(px(22)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(px(26)))
    image.paste((0, 0, 0), mask=shadow)

    ring = vertical(110, 914, [
        (0.0, (0xd9, 0xdc, 0xdf)),
        (0.5, (0x7d, 0x82, 0x87)),
        (1.0, (0x3a, 0x3d, 0x41)),
    ])
    mask = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(mask).ellipse(box(512, 512, 402), fill=255)
    image.paste(ring, mask=mask)

    draw = ImageDraw.Draw(image)

    # <circle r="376" fill="#1a1c1e"/>
    draw.ellipse(box(512, 512, 376), fill=(0x1a, 0x1c, 0x1e))

    # <circle r="362" fill="url(#dome)"/>
    # radialGradient i objectBoundingBox: bbox 150..874, sida 724
    dome = radial(150 + 0.42 * 724, 150 + 0.35 * 724, 0.75 * 724, [
        (0.00, (0x8d, 0xff, 0xbd)),
        (0.55, (0x3f, 0xe0, 0x8a)),
        (1.00, (0x16, 0xa3, 0x5a)),
    ])
    mask = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(mask).ellipse(box(512, 512, 362), fill=255)
    image.paste(dome, mask=mask)

    # <ellipse cx="512" cy="300" rx="270" ry="130" fill="url(#gloss)"/>
    # Vit med alfa 0.55 överst och 0 nederst, över ellipsens bbox 170..430.
    gloss = Image.new("L", (BIG, BIG), 0)
    gdraw = ImageDraw.Draw(gloss)
    top, bottom = px(170), px(430)
    for y in range(BIG):
        t = 0.0 if y <= top else (1.0 if y >= bottom else (y - top) / (bottom - top))
        gdraw.line([(0, y), (BIG, y)], fill=round(255 * 0.55 * (1 - t)))
    shape = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(shape).ellipse(box(512, 300, 270, 130), fill=255)
    image.paste((255, 255, 255), mask=ImageChops.multiply(gloss, shape))

    # <g transform="translate(0,40)" fill="#0f3d2e">
    dy = 40

    # Öronen: ellipser roterade kring sin egen mittpunkt.
    rotated(image, box(462, 330 + dy, 40, 100), -12, (462, 330 + dy), INK)
    rotated(image, box(562, 330 + dy, 40, 100), 12, (562, 330 + dy), INK)

    # <rect x="427" y="385" width="170" height="200" rx="85"/>
    draw.rounded_rectangle(
        [px(427), px(385 + dy), px(427 + 170), px(385 + dy + 200)],
        radius=px(85), fill=INK)

    # <path d="M390 485 V520 A122 122 0 0 0 634 520 V485" stroke-width="28"
    #       stroke-linecap="round"/>  — en U-form med rundade ändar upptill.
    stroke = px(28)
    cap = stroke / 2
    for x in (390, 634):
        draw.line([(px(x), px(485 + dy)), (px(x), px(520 + dy))],
                  fill=INK, width=round(stroke))
        draw.ellipse([px(x) - cap, px(485 + dy) - cap,
                      px(x) + cap, px(485 + dy) + cap], fill=INK)
    # Halvcirkeln: mitt (512, 520+dy), radie 122, nedre halvan.
    draw.arc([px(512 - 122), px(520 + dy - 122), px(512 + 122), px(520 + dy + 122)],
             start=0, end=180, fill=INK, width=round(stroke))

    # <line x1="512" y1="642" x2="512" y2="700"/>  — stativets stolpe
    draw.line([(px(512), px(642 + dy)), (px(512), px(700 + dy))],
              fill=INK, width=round(stroke))

    # <line x1="450" y1="702" x2="574" y2="702" stroke-linecap="round"/>  — foten
    draw.line([(px(450), px(702 + dy)), (px(574), px(702 + dy))],
              fill=INK, width=round(stroke))
    for x in (450, 574):
        draw.ellipse([px(x) - cap, px(702 + dy) - cap,
                      px(x) + cap, px(702 + dy) + cap], fill=INK)

    return image.resize((SIDE, SIDE), Image.LANCZOS)


if __name__ == "__main__":
    icon = render()
    assert icon.mode == "RGB", "appikonen får inte ha alfakanal"
    assert icon.size == (SIDE, SIDE)
    os.makedirs(os.path.dirname(TARGET), exist_ok=True)
    icon.save(TARGET, "PNG", optimize=True)
    print("skrev", os.path.relpath(TARGET, IOS).replace(os.sep, "/"),
          icon.size, icon.mode)
