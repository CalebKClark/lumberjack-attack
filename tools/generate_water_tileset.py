from PIL import Image
from pathlib import Path

src_dir = Path(r"C:\Users\Caleb\.cursor\projects\c-Users-Caleb-eclipse-workspace-2DGame\assets")
out_dir = Path(r"c:\Users\Caleb\eclipse-workspace\2DGame\resources\tiles\sprites")
out_dir.mkdir(parents=True, exist_ok=True)

corner_path = src_dir / (
    "c__Users_Caleb_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images_"
    "waterCorner-b4bfe885-6166-4cac-bdc1-3deca41be87a.png"
)
water_path = src_dir / (
    "c__Users_Caleb_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images_"
    "water-8f3b8986-a5ff-4923-9f4a-e6390144c349.png"
)
edge_path = src_dir / (
    "c__Users_Caleb_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images_"
    "waterEdge-fc02bf29-4670-48c7-997c-f4b94dc34e14.png"
)
inner_path = src_dir / (
    "c__Users_Caleb_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images_"
    "waterInnerCorner-945096c8-cc22-4e1b-a017-3ae6947f3ad2.png"
)

water = Image.open(water_path).convert("RGBA")
corner_tl = Image.open(corner_path).convert("RGBA")  # land N+W, water S+E -> mask 6 outer
edge_left = Image.open(edge_path).convert("RGBA")  # land W, water N+S+E -> mask 7
inner_tl = Image.open(inner_path).convert("RGBA")  # land NW only -> mask 6 inner


def flip_h(img: Image.Image) -> Image.Image:
    return img.transpose(Image.FLIP_LEFT_RIGHT)


def flip_v(img: Image.Image) -> Image.Image:
    return img.transpose(Image.FLIP_TOP_BOTTOM)


def rot_cw(img: Image.Image) -> Image.Image:
    return img.transpose(Image.ROTATE_270)


def rot_ccw(img: Image.Image) -> Image.Image:
    return img.transpose(Image.ROTATE_90)


tiles: dict[int, Image.Image] = {}

tiles[15] = water.copy()

# Outer corners
tiles[6] = corner_tl.copy()
tiles[10] = flip_h(corner_tl)
tiles[5] = flip_v(corner_tl)
tiles[9] = flip_h(flip_v(corner_tl))

# Straight edges
tiles[7] = edge_left.copy()
tiles[11] = flip_h(edge_left)
tiles[14] = rot_cw(edge_left)
tiles[13] = rot_ccw(edge_left)

for mask in range(16):
    if mask not in tiles:
        tiles[mask] = water.copy()

# Inner corners (same masks as outer; selected at runtime via diagonals)
inner_tiles = {
    6: inner_tl.copy(),
    10: flip_h(inner_tl),
    5: flip_v(inner_tl),
    9: flip_h(flip_v(inner_tl)),
}

water.save(out_dir / "water.png")
for mask, img in tiles.items():
    if img.size != (16, 16):
        raise SystemExit(f"bad size for water_{mask}: {img.size}")
    img.save(out_dir / f"water_{mask}.png")

for mask, img in inner_tiles.items():
    if img.size != (16, 16):
        raise SystemExit(f"bad size for water_inner_{mask}: {img.size}")
    img.save(out_dir / f"water_inner_{mask}.png")

assets_dir = Path(r"c:\Users\Caleb\eclipse-workspace\2DGame\assets\tiles\sprites")
assets_dir.mkdir(parents=True, exist_ok=True)
for path in out_dir.glob("water*.png"):
    (assets_dir / path.name).write_bytes(path.read_bytes())

print("Wrote:")
for path in sorted(out_dir.glob("water*.png")):
    print(f"  {path.name}")
print("Done.")
