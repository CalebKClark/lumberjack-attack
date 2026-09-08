"""Generate placeholder sprites for the player, items, and placeables."""

from __future__ import annotations

import csv
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ITEM_SPRITES = ROOT / "resources" / "items" / "sprites"
PLAYER_SPRITE = ROOT / "resources" / "sprites" / "player.png"
ITEMS_CSV = ROOT / "resources" / "items" / "items.csv"

TILE = 50
PLAYER_W = 50
PLAYER_H = 75
DROP_SIZE = 32
PLACEABLE_SIZE = 50


def save(path: Path, image: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
    print(f"wrote {path.relative_to(ROOT)}")


def shade(rgb: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(max(0, min(255, int(c * amount))) for c in rgb)


def draw_player() -> None:
    img = Image.new("RGBA", (PLAYER_W, PLAYER_H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    skin = (224, 188, 152)
    shirt = (48, 96, 168)
    pants = (72, 58, 42)
    boots = (48, 36, 28)
    hair = (92, 58, 34)
    beard = (118, 82, 52)

    # Boots near the bottom (feet anchor).
    draw.rectangle((14, 66, 22, 74), fill=(*boots, 255))
    draw.rectangle((28, 66, 36, 74), fill=(*boots, 255))

    # Legs.
    draw.rectangle((16, 48, 23, 66), fill=(*pants, 255))
    draw.rectangle((27, 48, 34, 66), fill=(*pants, 255))

    # Torso.
    draw.rectangle((14, 28, 36, 48), fill=(*shirt, 255))
    draw.line((25, 28, 25, 48), fill=(*shade(shirt, 0.75), 255), width=1)

    # Arms.
    draw.rectangle((8, 30, 13, 46), fill=(*shirt, 255))
    draw.rectangle((37, 30, 42, 46), fill=(*shirt, 255))
    draw.rectangle((8, 46, 13, 52), fill=(*skin, 255))
    draw.rectangle((37, 46, 42, 52), fill=(*skin, 255))

    # Head.
    draw.rectangle((16, 10, 34, 28), fill=(*skin, 255))
    draw.rectangle((16, 4, 34, 12), fill=(*hair, 255))
    draw.rectangle((18, 20, 22, 22), fill=(40, 40, 48, 255))
    draw.rectangle((28, 20, 32, 22), fill=(40, 40, 48, 255))
    draw.rectangle((18, 24, 32, 27), fill=(*beard, 255))

    # Axe on back.
    draw.rectangle((38, 14, 41, 34), fill=(120, 88, 52, 255))
    draw.polygon([(42, 12), (48, 18), (42, 24)], fill=(180, 180, 190, 255))

    save(PLAYER_SPRITE, img)


def draw_item_icon(item_id: str, rgb: tuple[int, int, int], category: str) -> None:
    size = PLACEABLE_SIZE if category == "placeable" else DROP_SIZE
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    base = rgb
    dark = shade(base, 0.65)
    light = shade(base, 1.15)

    margin = max(2, size // 8)
    inner = size - margin * 2

    if item_id == "starter_axe":
        draw.rectangle((margin + 2, margin + 8, margin + 8, size - margin - 4), fill=(*dark, 255))
        draw.polygon(
            [
                (margin + 8, margin + 2),
                (size - margin - 2, margin + inner // 2),
                (margin + 8, size - margin - 4),
            ],
            fill=(*light, 255),
        )
    elif item_id == "starter_rod":
        draw.line((margin + 4, size - margin - 2, size - margin - 4, margin + 4), fill=(*dark, 255), width=2)
        draw.ellipse((size - margin - 8, margin + 2, size - margin + 2, margin + 12), fill=(*light, 255))
    elif item_id == "pine_log":
        draw.rectangle((margin, margin + 6, size - margin, size - margin), fill=(*base, 255))
        for y in range(margin + 8, size - margin, 4):
            draw.line((margin + 2, y, size - margin - 2, y), fill=(*dark, 120), width=1)
    elif item_id == "wood_chips":
        for i, ox in enumerate((margin, margin + 8, margin + 14)):
            oy = margin + (i * 5)
            draw.ellipse((ox, oy, ox + 12, oy + 8), fill=(*shade(base, 0.85 + i * 0.08), 255))
    elif item_id == "wood_chipper":
        draw.rectangle((margin, margin + 10, size - margin, size - margin), fill=(*dark, 255))
        draw.rectangle((margin + 4, margin, size - margin - 4, margin + 12), fill=(*base, 255))
        draw.rectangle((margin + 10, margin + 16, size - margin - 10, size - margin - 4), fill=(40, 40, 48, 255))
        draw.rectangle((margin + 14, margin + 20, size - margin - 14, margin + 24), fill=(*light, 255))
    elif item_id == "chest":
        draw.rectangle((margin, margin + 8, size - margin, size - margin), fill=(*base, 255))
        draw.rectangle((margin, margin + 8, size - margin, margin + 16), fill=(*dark, 255))
        draw.rectangle((size // 2 - 3, margin + 18, size // 2 + 3, margin + 26), fill=(220, 180, 60, 255))
        draw.arc((margin, margin + 2, size - margin, margin + 18), start=180, end=0, fill=(*dark, 255), width=2)
    elif item_id == "slimeball":
        draw.ellipse((margin, margin, size - margin, size - margin), fill=(*base, 255))
        draw.ellipse((margin + 6, margin + 8, margin + 12, margin + 14), fill=(255, 255, 255, 90))
    elif item_id == "trash":
        draw.rectangle((margin + 4, margin + 8, size - margin - 4, size - margin), fill=(*base, 255))
        draw.polygon(
            [(margin + 8, margin + 8), (size - margin - 8, margin + 8), (size - margin - 4, margin + 4), (margin + 4, margin + 4)],
            fill=(*dark, 255),
        )
    elif item_id == "rusty_can":
        draw.rectangle((margin + 6, margin + 10, size - margin - 6, size - margin), fill=(*base, 255))
        draw.rectangle((margin + 8, margin + 6, size - margin - 8, margin + 12), fill=(*light, 255))
    elif item_id in {"bass", "trout", "perch"}:
        cx = size // 2
        cy = size // 2 + 2
        draw.ellipse((margin, cy - 8, size - margin - 6, cy + 8), fill=(*base, 255))
        draw.polygon([(size - margin - 6, cy), (size - margin, cy - 6), (size - margin, cy + 6)], fill=(*dark, 255))
        draw.ellipse((cx - 4, cy - 2, cx, cy + 2), fill=(255, 255, 255, 255))
    else:
        draw.rounded_rectangle((margin, margin, size - margin, size - margin), radius=4, fill=(*base, 255))

    save(ITEM_SPRITES / f"{item_id}.png", img)


def main() -> None:
    draw_player()

    with ITEMS_CSV.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        for row in reader:
            item_id = row["id"].strip()
            if not item_id:
                continue
            rgb = (int(row["r"]), int(row["g"]), int(row["b"]))
            category = row["category"].strip()
            draw_item_icon(item_id, rgb, category)

    print("done")


if __name__ == "__main__":
    main()
