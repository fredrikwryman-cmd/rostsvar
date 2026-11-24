#!/usr/bin/env python3
"""Gör alla appikoner ur en enda källbild: design/ikon-taptalk.png.

Kör från projektmappen:

    python design/gor-ikoner.py

Källan är 1024 x 1024 utan alfakanal. Kaninen sitter i övre vänstra hörnet
och de tre gröna staplarna i mitten, så motivet når nästan ut i kanten på
två sidor. Det styr allt nedan.

Android
  mipmap-*/ic_launcher.png             hela motivet, kvadratiskt
  mipmap-*/ic_launcher_round.png       hela motivet inne i en cirkel
  mipmap-*/ic_launcher_background.png  bakgrundslagret, 108 dp, slät yta
  mipmap-*/ic_launcher_foreground.png  motivet krympt till den säkra mitten
  design/ikon-google-play-512.png      butiksikonen

Den adaptiva ikonens lager är 108 dp, men bara de mittersta 72 dp syns
alltid, och på runda ikonformer skärs hörnen av. Motivet läggs därför inom
66 procent av lagrets sida, centrerat efter sin egen ruta och inte efter
bildens mitt — källan är inte centrerad. Inget klipps bort.

iOS
  ios/App/Assets.xcassets/AppIcon.appiconset               alla storlekar
  ios/Messages/Assets.xcassets/iMessage App Icon.stickersiconset

Alla iOS-filer skrivs som RGB. En appikon med alfakanal avvisas vid
uppladdning, och rundade hörn ritas aldrig in: iOS rundar själv.

Skriptet ersätter android/tools/make_icons.py och ios/Tools/make_icons.py,
som ritar den äldre grafiken ur appicon.svg i stället för ur källbilden.
"""
from __future__ import annotations

import collections
import json
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SOURCE = os.path.join(HERE, "ikon-taptalk.png")

ANDROID_RES = os.path.join(ROOT, "android", "app", "src", "main", "res")
IOS_APP = os.path.join(ROOT, "ios", "App", "Assets.xcassets", "AppIcon.appiconset")
IOS_MSG = os.path.join(ROOT, "ios", "Messages", "Assets.xcassets",
                       "iMessage App Icon.stickersiconset")

# Täthet, ikonens vanliga sida, och den adaptiva ikonens lagersida (108 dp).
DENSITIES = [
    ("mdpi", 48, 108),
    ("hdpi", 72, 162),
    ("xhdpi", 96, 216),
    ("xxhdpi", 144, 324),
    ("xxxhdpi", 192, 432),
]

# Andelen av det adaptiva lagret som alltid syns, på varje ikonform.
SAFE = 0.66
# Marginal runt motivet i den runda ikonen, som andel av cirkelns diameter.
ROUND_SHARE = 0.74

# iOS-appikonen, i punkter och skala. Hela den klassiska uppsättningen, så
# att ikonen finns för varje plats iOS hämtar den från.
APP_ICON = [
    ("iphone", 20, 2), ("iphone", 20, 3),
    ("iphone", 29, 2), ("iphone", 29, 3),
    ("iphone", 40, 2), ("iphone", 40, 3),
    ("iphone", 60, 2), ("iphone", 60, 3),
    ("ipad", 20, 1), ("ipad", 20, 2),
    ("ipad", 29, 1), ("ipad", 29, 2),
    ("ipad", 40, 1), ("ipad", 40, 2),
    ("ipad", 76, 1), ("ipad", 76, 2),
    ("ipad", 83.5, 2),
    ("ios-marketing", 1024, 1),
]

# iMessage-tilläggets egna mått. Avlånga, så motivet får plats efter höjden.
MESSAGE_ICON = [
    ("Marketing-1024x768.png", 1024, 768, "ios-marketing", "1024x768", 1),
    ("universal-27x20@2x.png", 54, 40, "universal", "27x20", 2),
    ("universal-27x20@3x.png", 81, 60, "universal", "27x20", 3),
    ("universal-32x24@2x.png", 64, 48, "universal", "32x24", 2),
    ("universal-32x24@3x.png", 96, 72, "universal", "32x24", 3),
    ("iphone-60x45@2x.png", 120, 90, "iphone", "60x45", 2),
    ("iphone-60x45@3x.png", 180, 135, "iphone", "60x45", 3),
    ("ipad-67x50@2x.png", 134, 100, "ipad", "67x50", 2),
    ("ipad-74x55@2x.png", 148, 110, "ipad", "74x55", 2),
]


def background(image: Image.Image) -> tuple[int, int, int]:
    """Källbildens bakgrund: den vanligaste färgen i en ram runt kanten."""
    width, height = image.size
    pixels = image.load()
    edge = collections.Counter()
    band = max(4, width // 128)
    for x in range(width):
        for y in list(range(band)) + list(range(height - band, height)):
            edge[pixels[x, y]] += 1
    for x in list(range(band)) + list(range(width - band, width)):
        for y in range(height):
            edge[pixels[x, y]] += 1
    return edge.most_common(1)[0][0]


def motif(image: Image.Image, paper: tuple[int, int, int]) -> tuple[int, int, int, int]:
    """Rutan runt allt som inte är bakgrund, med en punkts luft runt om."""
    width, height = image.size
    pixels = image.load()

    def drawn(point: tuple[int, int, int]) -> bool:
        # Källan har lite brus i den vita ytan, därav tröskeln.
        return max(abs(point[i] - paper[i]) for i in range(3)) > 24

    left, top, right, bottom = width, height, -1, -1
    for y in range(height):
        for x in range(width):
            if drawn(pixels[x, y]):
                left = min(left, x)
                right = max(right, x)
                top = min(top, y)
                bottom = max(bottom, y)
    if right < 0:
        raise SystemExit("hittar inget motiv i " + SOURCE)
    return (max(0, left - 1), max(0, top - 1),
            min(width, right + 2), min(height, bottom + 2))


def fitted(cut: Image.Image, side: int, share: float,
           paper: tuple[int, int, int] | None) -> Image.Image:
    """Motivet skalat så att dess längsta sida tar share av kvadraten.

    paper är None när lagret ska vara genomskinligt.
    """
    board = (Image.new("RGBA", (side, side), (0, 0, 0, 0)) if paper is None
             else Image.new("RGB", (side, side), paper))
    room = side * share
    scale = room / max(cut.size)
    size = (max(1, round(cut.size[0] * scale)), max(1, round(cut.size[1] * scale)))
    small = cut.resize(size, Image.LANCZOS)
    board.paste(small, ((side - size[0]) // 2, (side - size[1]) // 2))
    return board


def oblong(cut: Image.Image, width: int, height: int,
           paper: tuple[int, int, int]) -> Image.Image:
    """Motivet helt och hållet på en avlång yta, med luft runt om."""
    board = Image.new("RGB", (width, height), paper)
    room = min(width, height) * 0.82
    scale = room / max(cut.size)
    size = (max(1, round(cut.size[0] * scale)), max(1, round(cut.size[1] * scale)))
    small = cut.resize(size, Image.LANCZOS)
    board.paste(small, ((width - size[0]) // 2, (height - size[1]) // 2))
    return board


def circle(cut: Image.Image, side: int, paper: tuple[int, int, int]) -> Image.Image:
    """Hela motivet inne i en fylld cirkel, genomskinligt utanför den.

    Lint fällde den gamla runda ikonen för att den var en kvadrat: den
    fyllde varje punkt och hade ingen rund form.
    """
    scale = 4
    big = side * scale
    board = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, big - 1, big - 1], fill=255)
    board.paste(Image.new("RGB", (big, big), paper), mask=mask)

    room = big * ROUND_SHARE
    k = room / max(cut.size)
    size = (max(1, round(cut.size[0] * k)), max(1, round(cut.size[1] * k)))
    small = cut.resize(size, Image.LANCZOS).convert("RGB")
    board.paste(small, ((big - size[0]) // 2, (big - size[1]) // 2))
    board.putalpha(mask)
    return board.resize((side, side), Image.LANCZOS)


def save(image: Image.Image, *parts: str) -> None:
    path = os.path.join(*parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, "PNG", optimize=True)
    print("  %-62s %sx%s %s" % (os.path.relpath(path, ROOT).replace(os.sep, "/"),
                                image.size[0], image.size[1], image.mode))


def main() -> None:
    whole = Image.open(SOURCE).convert("RGB")
    if whole.size != (1024, 1024):
        print("varning: källan är %sx%s, väntade 1024x1024" % whole.size)
    paper = background(whole)
    box = motif(whole, paper)
    cut = whole.crop(box)
    print("källa %s, bakgrund #%02X%02X%02X, motivets ruta %s (%sx%s)"
          % (os.path.basename(SOURCE), paper[0], paper[1], paper[2], box,
             cut.size[0], cut.size[1]))

    print("Android:")
    for density, full, layer in DENSITIES:
        folder = os.path.join(ANDROID_RES, "mipmap-" + density)
        save(whole.resize((full, full), Image.LANCZOS), folder, "ic_launcher.png")
        save(circle(cut, full, paper), folder, "ic_launcher_round.png")
        save(Image.new("RGB", (layer, layer), paper), folder,
             "ic_launcher_background.png")
        save(fitted(cut, layer, SAFE, None), folder, "ic_launcher_foreground.png")
    save(whole.resize((512, 512), Image.LANCZOS), HERE, "ikon-google-play-512.png")

    print("iOS, appikonen:")
    entries = []
    for idiom, points, scale in APP_ICON:
        side = round(points * scale)
        # Måttet räcker inte som filnamn: iPhone och iPad delar flera
        # storlekar, och två poster får inte peka på samma fil.
        name = "AppIcon-%s-%gx%g@%dx.png" % (idiom, points, points, scale)
        if idiom == "ios-marketing":
            name = "AppIcon-1024.png"
        save(whole.resize((side, side), Image.LANCZOS), IOS_APP, name)
        entry = {
            "filename": name,
            "idiom": idiom,
            "scale": "%dx" % scale,
            "size": "%gx%g" % (points, points),
        }
        if idiom == "ios-marketing":
            entry["platform"] = "ios"
        entries.append(entry)
    catalogue(IOS_APP, entries)

    print("iOS, iMessage-tillägget:")
    entries = []
    for name, width, height, idiom, size, scale in MESSAGE_ICON:
        save(oblong(cut, width, height, paper), IOS_MSG, name)
        entry = {"filename": name, "idiom": idiom, "scale": "%dx" % scale,
                 "size": size}
        if idiom in ("universal", "ios-marketing"):
            entry["platform"] = "ios"
        entries.append(entry)
    catalogue(IOS_MSG, entries)


def catalogue(folder: str, entries: list[dict]) -> None:
    path = os.path.join(folder, "Contents.json")
    with open(path, "w", encoding="utf-8") as file:
        json.dump({"images": entries, "info": {"author": "rostsvar", "version": 1}},
                  file, indent=2, ensure_ascii=False)
        file.write("\n")
    print("  %-62s %d poster" % (os.path.relpath(path, ROOT).replace(os.sep, "/"),
                                 len(entries)))


if __name__ == "__main__":
    main()
    print("klart")
