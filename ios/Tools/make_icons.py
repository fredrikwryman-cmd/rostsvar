#!/usr/bin/env python3
"""Genererar iMessage-tilläggets ikoner för Tap & Talk.

Kör från mappen ios:

    python Tools/make_icons.py

Appikonen hör INTE hit. Den ritas ur Tools/appicon.svg av
Tools/render_appicon.py och skrivs till App/Assets.xcassets. Det här
skriptet rör bara Messages/Assets.xcassets.
"""
from __future__ import annotations

import json
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
IOS = os.path.dirname(HERE)

TOP = (24, 54, 122)
BOTTOM = (10, 24, 58)
INK = (255, 255, 255)
ACCENT = (90, 170, 255)

# iMessage-ikonens platser. Uppmätta mot actool i Xcode 16.4: ikonuppsättningen
# för ett Messages-tillägg har exakt dessa platser plus 1024x768. Storlekarna
# 27x20 och 32x24 hör till klisterpaket och matchar ingen plats här.
MESSAGE_SIZES = [
    ("iphone", 60, 45, 2),
    ("iphone", 60, 45, 3),
    ("ipad", 67, 50, 2),
    ("ipad", 74, 55, 2),
]


def backdrop(width: int, height: int) -> Image.Image:
    image = Image.new("RGB", (width, height), BOTTOM)
    pixels = image.load()
    for y in range(height):
        mix = y / max(height - 1, 1)
        row = tuple(round(TOP[i] + (BOTTOM[i] - TOP[i]) * mix) for i in range(3))
        for x in range(width):
            pixels[x, y] = row
    return image


def mark(width: int, height: int) -> Image.Image:
    """Talbubbla med tre ljudstaplar, ritad i fyra gånger upplösning."""
    scale = 4
    big = backdrop(width * scale, height * scale)
    draw = ImageDraw.Draw(big)
    w, h = big.size
    side = min(w, h)

    bubble_w = side * 0.74
    bubble_h = side * 0.56
    left = (w - bubble_w) / 2
    top = (h - bubble_h) / 2 - side * 0.04
    radius = bubble_h * 0.34
    draw.rounded_rectangle(
        [left, top, left + bubble_w, top + bubble_h], radius=radius, fill=INK
    )

    # Svansen på bubblan, nedre vänstra hörnet.
    tail = side * 0.15
    tip_x = left + bubble_w * 0.26
    base_y = top + bubble_h - 1
    draw.polygon(
        [
            (tip_x, base_y),
            (tip_x + tail * 0.95, base_y),
            (tip_x + tail * 0.18, base_y + tail),
        ],
        fill=INK,
    )

    # Tre staplar som antyder tal.
    bars = 3
    bar_w = bubble_w * 0.085
    gap = bar_w * 1.35
    total = bars * bar_w + (bars - 1) * gap
    start = left + (bubble_w - total) / 2
    mid = top + bubble_h / 2
    heights = [0.40, 0.70, 0.52]
    for index in range(bars):
        tall = bubble_h * heights[index]
        x0 = start + index * (bar_w + gap)
        draw.rounded_rectangle(
            [x0, mid - tall / 2, x0 + bar_w, mid + tall / 2],
            radius=bar_w / 2,
            fill=ACCENT if index == 1 else (30, 64, 140),
        )

    return big.resize((width, height), Image.LANCZOS)


def squircle(size: int) -> Image.Image:
    """Appikonen: samma märke på hel yta, utan rundade hörn (iOS maskar själv)."""
    return mark(size, size)


def write(image: Image.Image, path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.convert("RGB").save(path, "PNG", optimize=True)
    print("skrev", os.path.relpath(path, IOS).replace(os.sep, "/"))


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

    # Messages App Store-ikonen. Enda marknadsforingsplatsen i uppsattningen;
    # appens egen 1024x1024 ligger i App/Assets.xcassets.
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


if __name__ == "__main__":
    message_icon()
    print("klart")
