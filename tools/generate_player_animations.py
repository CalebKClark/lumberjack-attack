"""Generate placeholder player animation frames (walk, axe, rod)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "resources" / "sprites" / "player"
LEGACY = ROOT / "resources" / "sprites" / "player.png"

W = 50
H = 75
WALK_FRAMES = 3
ACTION_FRAMES = 3

SKIN = (224, 188, 152)
SHIRT = (48, 96, 168)
PANTS = (72, 58, 42)
BOOTS = (48, 36, 28)
HAIR = (92, 58, 34)
BEARD = (118, 82, 52)
AXE_HANDLE = (120, 88, 52)
AXE_BLADE = (180, 180, 190)
ROD = (101, 67, 33)
ROD_LINE = (210, 210, 210)


def save(name: str, image: Image.Image) -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / name
    image.save(path)
    print(f"wrote {path.relative_to(ROOT)}")


def blank_canvas() -> Image.Image:
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def draw_body(
    draw: ImageDraw.ImageDraw,
    direction: str,
    walk_phase: int = 0,
    action: str | None = None,
    action_frame: int = 0,
) -> None:
    left_leg_offset = 0
    right_leg_offset = 0
    arm_swing = 0
    torso_shift = 0

    if action is None:
        if walk_phase == 1:
            left_leg_offset, right_leg_offset = -2, 2
            arm_swing = 2
        elif walk_phase == 2:
            left_leg_offset, right_leg_offset = 2, -2
            arm_swing = -2
    elif action == "axe":
        arm_swing = 4 + action_frame * 5
        torso_shift = action_frame * 2
    elif action == "rod":
        arm_swing = 6 + action_frame * 4

    if direction == "left":
        torso_shift = -torso_shift
        arm_swing = -arm_swing

    # Boots / legs
    if direction in {"down", "up"}:
        draw.rectangle((14 + torso_shift, 66 + left_leg_offset, 22 + torso_shift, 74), fill=(*BOOTS, 255))
        draw.rectangle((28 + torso_shift, 66 + right_leg_offset, 36 + torso_shift, 74), fill=(*BOOTS, 255))
        draw.rectangle((16 + torso_shift, 48 + left_leg_offset, 23 + torso_shift, 66), fill=(*PANTS, 255))
        draw.rectangle((27 + torso_shift, 48 + right_leg_offset, 34 + torso_shift, 66), fill=(*PANTS, 255))
    elif direction == "left":
        draw.rectangle((12, 66 + left_leg_offset, 20, 74), fill=(*BOOTS, 255))
        draw.rectangle((18, 66 + right_leg_offset, 26, 74), fill=(*BOOTS, 255))
        draw.rectangle((13, 48 + left_leg_offset, 20, 66), fill=(*PANTS, 255))
        draw.rectangle((19, 48 + right_leg_offset, 26, 66), fill=(*PANTS, 255))
    else:  # right
        draw.rectangle((24, 66 + left_leg_offset, 32, 74), fill=(*BOOTS, 255))
        draw.rectangle((30, 66 + right_leg_offset, 38, 74), fill=(*BOOTS, 255))
        draw.rectangle((25, 48 + left_leg_offset, 32, 66), fill=(*PANTS, 255))
        draw.rectangle((31, 48 + right_leg_offset, 38, 66), fill=(*PANTS, 255))

    # Torso
    draw.rectangle((14 + torso_shift, 28, 36 + torso_shift, 48), fill=(*SHIRT, 255))

    if direction == "down":
        draw.rectangle((8, 30 + arm_swing, 13, 52), fill=(*SHIRT, 255))
        draw.rectangle((37, 30 - arm_swing, 42, 52), fill=(*SHIRT, 255))
        draw.rectangle((16 + torso_shift, 10, 34 + torso_shift, 28), fill=(*SKIN, 255))
        draw.rectangle((16 + torso_shift, 4, 34 + torso_shift, 12), fill=(*HAIR, 255))
        draw.rectangle((18 + torso_shift, 20, 22 + torso_shift, 22), fill=(40, 40, 48, 255))
        draw.rectangle((28 + torso_shift, 20, 32 + torso_shift, 22), fill=(40, 40, 48, 255))
        draw.rectangle((18 + torso_shift, 24, 32 + torso_shift, 27), fill=(*BEARD, 255))
    elif direction == "up":
        draw.rectangle((8, 30 - arm_swing, 13, 52), fill=(*SHIRT, 255))
        draw.rectangle((37, 30 + arm_swing, 42, 52), fill=(*SHIRT, 255))
        draw.rectangle((16 + torso_shift, 10, 34 + torso_shift, 28), fill=(*HAIR, 255))
        draw.rectangle((20 + torso_shift, 14, 30 + torso_shift, 22), fill=(*SHIRT, 255))
    elif direction == "left":
        draw.rectangle((10 + torso_shift, 30, 18 + torso_shift, 52), fill=(*SHIRT, 255))
        draw.rectangle((4 + arm_swing, 32, 10 + arm_swing, 48), fill=(*SKIN, 255))
        draw.rectangle((12 + torso_shift, 8, 28 + torso_shift, 26), fill=(*SKIN, 255))
        draw.rectangle((10 + torso_shift, 4, 26 + torso_shift, 12), fill=(*HAIR, 255))
        draw.rectangle((12 + torso_shift, 18, 16 + torso_shift, 20), fill=(40, 40, 48, 255))
    else:  # right
        draw.rectangle((32 + torso_shift, 30, 40 + torso_shift, 52), fill=(*SHIRT, 255))
        draw.rectangle((40 - arm_swing, 32, 46 - arm_swing, 48), fill=(*SKIN, 255))
        draw.rectangle((22 + torso_shift, 8, 38 + torso_shift, 26), fill=(*SKIN, 255))
        draw.rectangle((24 + torso_shift, 4, 40 + torso_shift, 12), fill=(*HAIR, 255))
        draw.rectangle((34 + torso_shift, 18, 38 + torso_shift, 20), fill=(40, 40, 48, 255))


def draw_axe_overlay(draw: ImageDraw.ImageDraw, direction: str, frame: int) -> None:
    swing = frame * 8
    if direction == "down":
        draw.rectangle((36, 18 - swing // 2, 40, 40 - swing), fill=(*AXE_HANDLE, 255))
        draw.polygon([(41, 12 - swing), (48, 20 - swing // 2), (41, 28 - swing // 3)], fill=(*AXE_BLADE, 255))
    elif direction == "up":
        draw.rectangle((10, 20 + swing // 2, 14, 42 + swing), fill=(*AXE_HANDLE, 255))
        draw.polygon([(4, 14 + swing), (12, 22 + swing // 2), (4, 30 + swing // 3)], fill=(*AXE_BLADE, 255))
    elif direction == "left":
        draw.rectangle((2 - swing, 24, 18 - swing, 28), fill=(*AXE_HANDLE, 255))
        draw.polygon([(0 - swing, 18), (10 - swing, 24), (0 - swing, 30)], fill=(*AXE_BLADE, 255))
    else:
        draw.rectangle((32 + swing, 24, 48 + swing, 28), fill=(*AXE_HANDLE, 255))
        draw.polygon([(48 + swing, 18), (50 + swing, 26), (48 + swing, 34)], fill=(*AXE_BLADE, 255))


def draw_rod_overlay(draw: ImageDraw.ImageDraw, direction: str, frame: int) -> None:
    cast = frame * 6
    if direction == "down":
        draw.line((40, 30, 46 + cast, 8 - cast), fill=(*ROD, 255), width=2)
        draw.line((46 + cast, 8 - cast, 46 + cast, 0), fill=(*ROD_LINE, 255), width=1)
    elif direction == "up":
        draw.line((10, 30, 4 - cast, 8 + cast), fill=(*ROD, 255), width=2)
    elif direction == "left":
        draw.line((8, 34, 0 - cast, 20 - cast), fill=(*ROD, 255), width=2)
        draw.line((0 - cast, 20 - cast, 0 - cast, 10 - cast), fill=(*ROD_LINE, 255), width=1)
    else:
        draw.line((42, 34, 50 + cast, 20 - cast), fill=(*ROD, 255), width=2)
        draw.line((50 + cast, 20 - cast, 50 + cast, 10 - cast), fill=(*ROD_LINE, 255), width=1)


def render_frame(direction: str, walk_phase: int = 0, action: str | None = None, action_frame: int = 0) -> Image.Image:
    img = blank_canvas()
    draw = ImageDraw.Draw(img)
    draw_body(draw, direction, walk_phase, action, action_frame)
    if action == "axe":
        draw_axe_overlay(draw, direction, action_frame)
    elif action == "rod":
        draw_rod_overlay(draw, direction, action_frame)
    return img


def main() -> None:
    directions = ("down", "up", "left", "right")

    for direction in directions:
        for frame in range(WALK_FRAMES):
            image = render_frame(direction, walk_phase=frame)
            save(f"walk_{direction}_{frame}.png", image)

        for frame in range(ACTION_FRAMES):
            image = render_frame(direction, action="axe", action_frame=frame)
            save(f"axe_{direction}_{frame}.png", image)

        for frame in range(ACTION_FRAMES):
            image = render_frame(direction, action="rod", action_frame=frame)
            save(f"rod_{direction}_{frame}.png", image)

    # Keep legacy single-frame fallback for older code paths.
    legacy = render_frame("down", walk_phase=0)
    LEGACY.parent.mkdir(parents=True, exist_ok=True)
    legacy.save(LEGACY)
    print(f"wrote {LEGACY.relative_to(ROOT)}")
    print("done")


if __name__ == "__main__":
    main()
