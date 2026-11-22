#!/usr/bin/env python3
"""Genererar iMessage-tilläggets ikoner för Tap & Talk.

Kör från mappen ios:

    python Tools/make_icons.py

Appikonen hör INTE hit. Den är original-PNG:en i App/Assets.xcassets och
rörs inte av det här skriptet. Här ritas bara Messages/Assets.xcassets.

Motivet är samma kaninmikrofon som appikonen (Tools/appicon.svg), men utan
den gröna knappen och metallringen: bara symbolen, centrerad på den mörka
bakgrunden, i liggande format. Formerna är översatta ur SVG:ns koordinater.

Alla platser som Contents.json listar får en fil. App Store Connect avvisade
tidigare bygget med "Missing App Icon" för 54x40, 64x48, 81x60 och 96x72,
alltså 27x20 och 32x24 i @2x och @3x. De finns med här.
"""
from __future__ import annotations

import json
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
IOS = os.path.dirname(HERE)

# Bakgrunden från appicon.svg: #3b3e42 överst, #1c1e20 nederst.
TOP = (0x3b, 0x3e, 0x42)
BOTTOM = (0x1c, 0x1e, 0x20)
# Symbolen ritas i kupolens gröna. Originalets #0f3d2e syns inte mot mörkt.
INK = (0x3f, 0xe0, 0x8a)

# Symbolens yttermått i appicon.svg:s koordinater, med gruppens
# translate(0,40) inräknad. Öronens hörn är uppmätta efter rotationen.
SYMBOL = (376.0, 271.8, 648.0, 756.0)

# Platserna i ikonuppsättningen. Pixelstorleken är punkter gånger skalan.
MESSAGE_SIZES = [
    ("iphone", 27, 20, 2),   # 54x40
    ("iphone", 27, 20, 3),   # 81x60
    ("iphone", 32, 24, 2),   # 64x48
    ("iphone", 32, 24, 3),   # 96x72
    ("iphone", 60, 45, 2),   # 120x90
    ("iphone", 60, 45, 3),   # 180x135
    ("ipad", 27, 20, 2),     # 54x40
    ("ipad", 32, 24, 2),     # 64x48
    ("ipad", 67, 50, 2),     # 134x100
    ("ipad", 74, 55, 2),     # 148x110
]


def backdrop(width: int, height: int) -> Image.Image:
    image = Image.new("RGB", (width, height), BOTTOM)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        mix = y / max(height - 1, 1)
        draw.line(
            [(0, y), (width, y)],
            fill=tuple(round(TOP[i] + (BOTTOM[i] - TOP[i]) * mix) for i in range(3)),
        )
    return image


def mark(width: int, height: int) -> Image.Image:
    """Kaninmikrofonen centrerad på bakgrunden, ritad i fyra gångers upplösning."""
    scale = 4
    w, h = width * scale, height * scale
    image = backdrop(w, h)
    draw = ImageDraw.Draw(image)

    x0, y0, x1, y1 = SYMBOL
    sw, sh = x1 - x0, y1 - y0
    # Fyller höjden med marginal, men aldrig bredare än ytan tillåter.
    s = min((h * 0.78) / sh, (w * 0.88) / sw)
    ox = w / 2 - (x0 + sw / 2) * s
    oy = h / 2 - (y0 + sh / 2) * s

    def point(x: float, y: float) -> tuple[float, float]:
        return (x * s + ox, y * s + oy)

    def bounds(cx: float, cy: float, rx: float, ry: float) -> list[float]:
        left, top = point(cx - rx, cy - ry)
        right, bottom = point(cx + rx, cy + ry)
        return [left, top, right, bottom]

    stroke = max(1.0, 28 * s)
    cap = stroke / 2

    # Öronen: ellipser roterade kring sin egen mittpunkt, som i SVG:n.
    for cx, angle in ((462.0, -12.0), (562.0, 12.0)):
        layer = Image.new("L", (w, h), 0)
        ImageDraw.Draw(layer).ellipse(bounds(cx, 370, 40, 100), fill=255)
        layer = layer.rotate(angle, resample=Image.BICUBIC, center=point(cx, 370))
        image.paste(INK, mask=layer)

    # Mikrofonkroppen.
    left, top = point(427, 425)
    right, bottom = point(597, 625)
    draw.rounded_rectangle([left, top, right, bottom], radius=85 * s, fill=INK)

    # Bygeln: två korta lodräta streck och en halvcirkel under dem.
    for x in (390, 634):
        draw.line([point(x, 525), point(x, 560)], fill=INK, width=round(stroke))
        cx, cy = point(x, 525)
        draw.ellipse([cx - cap, cy - cap, cx + cap, cy + cap], fill=INK)
    draw.arc(bounds(512, 560, 122, 122), start=0, end=180, fill=INK, width=round(stroke))

    # Stativets stolpe och fot.
    draw.line([point(512, 682), point(512, 740)], fill=INK, width=round(stroke))
    draw.line([point(450, 742), point(574, 742)], fill=INK, width=round(stroke))
    for x in (450, 574):
        cx, cy = point(x, 742)
        draw.ellipse([cx - cap, cy - cap, cx + cap, cy + cap], fill=INK)

    return image.resize((width, height), Image.LANCZOS)


def write(image: Image.Image, path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    flat = image.convert("RGB")
    flat.save(path, "PNG", optimize=True)
    print("skrev", os.path.relpath(path, IOS).replace(os.sep, "/"),
          f"{flat.size[0]}x{flat.size[1]}", flat.mode)


def write_json(payload: dict, path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print("skrev", os.path.relpath(path, IOS).replace(os.sep, "/"))


def message_icon() -> None:
    folder = os.path.join(
        IOS, "Messages", "Assets.xcassets", "iMessage App Icon.stickersiconset"
    )
    images = []

    # Messages App Store-ikonen. Appens egen 1024x1024 ligger i App/Assets.xcassets.
    write(mark(1024, 768), os.path.join(folder, "Marketing-1024x768.png"))
    images.append(
        {
            "filename": "Marketing-1024x768.png",
            "idiom": "ios-marketing",
            "platform": "ios",
            "scale": "1x",
            "size": "1024x768",
        }
    )

    for idiom, points_w, points_h, scale in MESSAGE_SIZES:
        name = f"{idiom}-{points_w}x{points_h}@{scale}x.png"
        write(mark(points_w * scale, points_h * scale), os.path.join(folder, name))
        images.append(
            {
                "filename": name,
                "idiom": idiom,
                "scale": f"{scale}x",
                "size": f"{points_w}x{points_h}",
            }
        )

    write_json(
        {"images": images, "info": {"author": "rostsvar", "version": 1}},
        os.path.join(folder, "Contents.json"),
    )
    write_json(
        {"info": {"author": "rostsvar", "version": 1}},
        os.path.join(IOS, "Messages", "Assets.xcassets", "Contents.json"),
    )

    # Varje plats i Contents.json ska ha en fil på disk, utan hål.
    missing = [i["filename"] for i in images
               if not os.path.exists(os.path.join(folder, i["filename"]))]
    assert not missing, missing
    print(f"{len(images)} platser, alla med fil.")


if __name__ == "__main__":
    message_icon()
    print("klart")
