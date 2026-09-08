"""Generate 50x50 placeholder tile sprites for Lumberjack RPG."""

from __future__ import annotations

import math
import os
import random
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 50
OUTPUT = Path(__file__).resolve().parent.parent / "resources" / "tiles" / "sprites"

# --- palette ---
GRASS = ((62, 128, 54), (78, 148, 66), (52, 108, 46))
WATER_DEEP = (28, 78, 148)
WATER_MID = (42, 102, 176)
WATER_SHALLOW = (68, 138, 198)
FOAM = (196, 228, 238)
SAND = (194, 168, 108)
WOOD_FLOOR = ((139, 101, 66), (118, 86, 54), (158, 118, 78))
WALL = ((110, 110, 118), (88, 88, 96), (130, 130, 138))
DOOR = ((101, 67, 33), (72, 48, 24))
BED = ((200, 50, 50), (240, 240, 240), (120, 80, 50))
TRUNK = (92, 58, 32)
CANOPY = ((34, 110, 48), (48, 132, 58), (26, 88, 38))
VOID = (12, 12, 16)


def lerp(a: int, b: int, t: float) -> int:
    return int(a + (b - a) * t)


def blend_rgb(c1, c2, t: float):
    return (lerp(c1[0], c2[0], t), lerp(c1[1], c2[1], t), lerp(c1[2], c2[2], t))


def noise(x: int, y: int, seed: int = 0) -> float:
    v = math.sin((x + seed * 13) * 0.41) * math.cos((y + seed * 7) * 0.37)
    return (v + 1) * 0.5


def save(name: str, image: Image.Image) -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / f"{name}.png"
    image.save(path)
    print(f"wrote {path.name}")


def make_grass() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    random.seed(1)
    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, 1)
            if n < 0.33:
                c = GRASS[0]
            elif n < 0.66:
                c = GRASS[1]
            else:
                c = GRASS[2]
            if random.random() < 0.06:
                c = blend_rgb(c, GRASS[2], 0.6)
            px[x, y] = (*c, 255)
    return img


def make_floor() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE))
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, SIZE, SIZE), fill=(*WOOD_FLOOR[0], 255))
    plank_w = 10
    for x in range(0, SIZE, plank_w):
        shade = WOOD_FLOOR[(x // plank_w) % 3]
        draw.rectangle((x, 0, min(x + plank_w - 1, SIZE - 1), SIZE - 1), fill=(*shade, 255))
    for y in range(0, SIZE, 12):
        draw.line((0, y, SIZE - 1, y), fill=(90, 64, 38, 90), width=1)
    return img


def make_wall() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE))
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, SIZE, SIZE), fill=(*WALL[0], 255))
    for row in range(4):
        offset = 6 if row % 2 else 0
        for col in range(3):
            x0 = col * 18 + offset - 2
            y0 = row * 12 + 1
            shade = WALL[(row + col) % 3]
            draw.rectangle((x0, y0, x0 + 16, y0 + 10), fill=(*shade, 255))
            draw.rectangle((x0, y0, x0 + 16, y0 + 10), outline=(60, 60, 68, 255), width=1)
    return img


def make_door() -> Image.Image:
    img = make_floor().copy()
    draw = ImageDraw.Draw(img)
    draw.rectangle((14, 6, 35, 47), fill=(*DOOR[0], 255), outline=(*DOOR[1], 255), width=2)
    draw.rectangle((18, 10, 31, 43), fill=(*DOOR[1], 255))
    draw.ellipse((28, 28, 32, 32), fill=(220, 180, 60, 255))
    return img


def make_bed() -> Image.Image:
    img = make_floor().copy()
    draw = ImageDraw.Draw(img)
    draw.rectangle((6, 14, 43, 44), fill=(*BED[2], 255), outline=(70, 48, 28, 255), width=2)
    draw.rectangle((8, 16, 41, 34), fill=(*BED[0], 255))
    draw.rectangle((8, 34, 41, 42), fill=(170, 40, 40, 255))
    draw.rectangle((10, 18, 24, 30), fill=(*BED[1], 255))
    return img


def make_void() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), (*VOID, 255))
    return img


def make_tree() -> Image.Image:
    img = make_grass().copy()
    draw = ImageDraw.Draw(img)
    draw.ellipse((6, 4, 44, 42), fill=(*CANOPY[0], 255))
    draw.ellipse((10, 8, 40, 38), fill=(*CANOPY[1], 255))
    draw.ellipse((14, 12, 36, 34), fill=(*CANOPY[2], 255))
    draw.rectangle((21, 24, 28, 34), fill=(*TRUNK, 255))
    draw.ellipse((8, 6, 20, 18), fill=(*blend_rgb(CANOPY[1], (255, 255, 255), 0.15), 255))
    return img


def shore_strength(dist: float, depth: float = 16.0) -> float:
    if dist >= depth:
        return 0.0
    t = 1.0 - dist / depth
    return t * t * (3 - 2 * t)


def make_water(mask: int) -> Image.Image:
    """mask bits: N=1, S=2, E=4, W=8 (1 means water neighbor exists)."""
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    has_n = bool(mask & 1)
    has_s = bool(mask & 2)
    has_e = bool(mask & 4)
    has_w = bool(mask & 8)

    for y in range(SIZE):
        for x in range(SIZE):
            n = noise(x, y, 42 + mask)
            if n < 0.45:
                base = WATER_DEEP
            elif n < 0.78:
                base = WATER_MID
            else:
                base = WATER_SHALLOW

            shore = 0.0
            if not has_n:
                shore = max(shore, shore_strength(y))
            if not has_s:
                shore = max(shore, shore_strength(SIZE - 1 - y))
            if not has_w:
                shore = max(shore, shore_strength(x))
            if not has_e:
                shore = max(shore, shore_strength(SIZE - 1 - x))

            if shore > 0:
                if shore > 0.72:
                    color = blend_rgb(GRASS[1], SAND, 0.35)
                    color = blend_rgb(color, FOAM, min(1.0, (shore - 0.72) * 3.5))
                elif shore > 0.45:
                    color = blend_rgb(SAND, WATER_SHALLOW, (shore - 0.45) / 0.27)
                else:
                    color = blend_rgb(WATER_SHALLOW, base, 1.0 - shore * 1.4)
            else:
                color = base

            if not has_n and not has_w and x < 10 and y < 10:
                color = blend_rgb(color, GRASS[0], 0.25)
            if not has_n and not has_e and x > SIZE - 11 and y < 10:
                color = blend_rgb(color, GRASS[0], 0.25)
            if not has_s and not has_w and x < 10 and y > SIZE - 11:
                color = blend_rgb(color, GRASS[0], 0.25)
            if not has_s and not has_e and x > SIZE - 11 and y > SIZE - 11:
                color = blend_rgb(color, GRASS[0], 0.25)

            px[x, y] = (*color, 255)

    draw = ImageDraw.Draw(img)
    if not has_n:
        draw.arc((4, 2, 46, 24), 10, 170, fill=(*FOAM, 180), width=1)
    if not has_s:
        draw.arc((4, 26, 46, 48), 190, 350, fill=(*FOAM, 180), width=1)
    return img


def main() -> None:
    save("grass", make_grass())
    save("tree1", make_tree())
    save("indoor_wall", make_wall())
    save("indoor_back_wall", make_wall())
    save("door", make_door())
    save("cabin_floor", make_floor())
    save("bed", make_bed())
    save("void", make_void())

    for mask in range(16):
        save(f"water_{mask}", make_water(mask))

    # Legacy single-name fallback
    save("water", make_water(15))


if __name__ == "__main__":
    main()
