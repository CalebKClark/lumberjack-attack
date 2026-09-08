from PIL import Image, ImageDraw
from pathlib import Path

SRC = Path(
    r"C:\Users\Caleb\.cursor\projects\c-Users-Caleb-eclipse-workspace-2DGame\assets"
    r"\c__Users_Caleb_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images_"
    r"ChatGPT_Image_Aug_3__2026__02_56_39_PM-6a3b25e6-6d3c-465b-8cd5-fa8ba416e2bb.png"
)
OUT = Path(r"c:\Users\Caleb\eclipse-workspace\2DGame\resources\sprites\player")
DEBUG = Path(r"c:\Users\Caleb\eclipse-workspace\2DGame\tools\player_slice_debug.png")

NAMES = [
    # row 0
    [f"walk_down_{i}" for i in range(3)]
    + [f"walk_up_{i}" for i in range(3)]
    + [f"walk_left_{i}" for i in range(3)]
    + [f"walk_right_{i}" for i in range(3)],
    # row 1
    [f"axe_down_{i}" for i in range(3)]
    + [f"axe_up_{i}" for i in range(3)]
    + [f"axe_left_{i}" for i in range(3)]
    + [f"axe_right_{i}" for i in range(3)],
    # row 2
    [f"rod_down_{i}" for i in range(3)]
    + [f"rod_up_{i}" for i in range(3)]
    + [f"rod_left_{i}" for i in range(3)]
    + [f"rod_right_{i}" for i in range(3)],
]

img = Image.open(SRC).convert("RGBA")
w, h = img.size
px = img.load()


def is_ink(x, y, thresh=245):
    r, g, b, a = px[x, y]
    return a > 10 and (r < thresh or g < thresh or b < thresh)


# Build row ink counts ignoring pure black text-ish? keep simple
row_sum = [sum(1 for x in range(w) if is_ink(x, y)) for y in range(h)]

# Find 3 sprite bands: stretches with high ink, separated by low ink, preferring taller bands
# Use threshold relative to max
max_rs = max(row_sum)
print("max row ink", max_rs)

# Mark rows as content if above 20% of peak of a local max - use absolute
CONTENT = 80
bands = []
in_band = False
start = 0
for y, s in enumerate(row_sum):
    if s >= CONTENT and not in_band:
        in_band = True
        start = y
    elif s < CONTENT and in_band:
        in_band = False
        bands.append((start, y - 1, y - start))
if in_band:
    bands.append((start, h - 1, h - start))

print("bands:")
for b in bands:
    print(" ", b)

# Expect alternating sprite/label. Sprite bands are taller (~100px), labels shorter (~40)
sprite_bands = [b for b in bands if b[2] >= 70]
label_bands = [b for b in bands if b[2] < 70]
print("sprite_bands", sprite_bands)
print("label_bands", label_bands)

# If we got 3 sprite bands, use them. Else merge.
if len(sprite_bands) < 3:
    # take the 3 tallest
    sprite_bands = sorted(bands, key=lambda b: b[2], reverse=True)[:3]
    sprite_bands = sorted(sprite_bands, key=lambda b: b[0])
    print("fallback sprite_bands", sprite_bands)

assert len(sprite_bands) == 3, sprite_bands

# Column splits: within first sprite band, project columns
y0, y1, _ = sprite_bands[0]
col_sum = [sum(1 for y in range(y0, y1 + 1) if is_ink(x, y)) for x in range(w)]

# Find 12 content column clusters
THRESH_C = 5
cols = []
in_run = False
start = 0
for x, s in enumerate(col_sum):
    if s >= THRESH_C and not in_run:
        in_run = True
        start = x
    elif s < THRESH_C and in_run:
        in_run = False
        cols.append((start, x - 1))
if in_run:
    cols.append((start, w - 1))

print("raw col clusters", len(cols))
for i, c in enumerate(cols):
    print(f"  {i}: {c} w={c[1]-c[0]+1}")

# If more than 12 (noise), merge nearby clusters that are close
def merge_clusters(clusters, gap=8):
    if not clusters:
        return []
    merged = [list(clusters[0])]
    for a, b in clusters[1:]:
        if a - merged[-1][1] <= gap:
            merged[-1][1] = b
        else:
            merged.append([a, b])
    return [(a, b) for a, b in merged]

cols = merge_clusters(cols, gap=12)
print("merged cols", len(cols))
for i, c in enumerate(cols):
    print(f"  {i}: {c} w={c[1]-c[0]+1}")

# If still not 12, fall back to equal split of content bbox
if len(cols) != 12:
    xs = [i for i, s in enumerate(col_sum) if s >= THRESH_C]
    left, right = xs[0], xs[-1]
    cell_w = (right - left + 1) / 12
    cols = []
    for i in range(12):
        a = int(round(left + i * cell_w))
        b = int(round(left + (i + 1) * cell_w)) - 1
        cols.append((a, b))
    print("equal split cols")
    for i, c in enumerate(cols):
        print(f"  {i}: {c} w={c[1]-c[0]+1}")


def crop_content(box):
    """Tight crop non-white inside box, return RGBA with white->transparent."""
    x0, y0, x1, y1 = box
    x0 = max(0, x0)
    y0 = max(0, y0)
    x1 = min(w - 1, x1)
    y1 = min(h - 1, y1)
    region = img.crop((x0, y0, x1 + 1, y1 + 1))
    rp = region.load()
    rw, rh = region.size
    # find ink bbox
    minx, miny, maxx, maxy = rw, rh, -1, -1
    for yy in range(rh):
        for xx in range(rw):
            r, g, b, a = rp[xx, yy]
            if a > 10 and (r < 245 or g < 245 or b < 245):
                if xx < minx:
                    minx = xx
                if yy < miny:
                    miny = yy
                if xx > maxx:
                    maxx = xx
                if yy > maxy:
                    maxy = yy
    if maxx < 0:
        return Image.new("RGBA", (16, 24), (0, 0, 0, 0))
    tight = region.crop((minx, miny, maxx + 1, maxy + 1))
    return tight


def to_game_frame(sprite: Image.Image) -> Image.Image:
    """Fit sprite into 16x24, bottom-center, white->transparent."""
    # make white transparent
    sp = sprite.convert("RGBA")
    pixels = sp.load()
    for yy in range(sp.height):
        for xx in range(sp.width):
            r, g, b, a = pixels[xx, yy]
            if r >= 245 and g >= 245 and b >= 245:
                pixels[xx, yy] = (0, 0, 0, 0)

    # scale to fit inside 16x24 preserving aspect, nearest
    max_w, max_h = 16, 24
    scale = min(max_w / sp.width, max_h / sp.height)
    nw = max(1, int(round(sp.width * scale)))
    nh = max(1, int(round(sp.height * scale)))
    # Prefer integer-ish scale if nearly 16x24 already after scale
    scaled = sp.resize((nw, nh), Image.Resampling.NEAREST)

    canvas = Image.new("RGBA", (16, 24), (0, 0, 0, 0))
    ox = (16 - nw) // 2
    oy = 24 - nh  # bottom align
    canvas.paste(scaled, (ox, oy), scaled)
    return canvas


OUT.mkdir(parents=True, exist_ok=True)
assets_out = Path(r"c:\Users\Caleb\eclipse-workspace\2DGame\assets\sprites\player")
assets_out.mkdir(parents=True, exist_ok=True)

# pad each cell a bit
debug = img.convert("RGBA").copy()
draw = ImageDraw.Draw(debug)

saved = 0
for row_i, (sy0, sy1, _) in enumerate(sprite_bands):
    for col_i, (sx0, sx1) in enumerate(cols):
        # expand cell slightly to catch overhangs (axe/rod)
        pad_x = 4
        pad_y = 2
        box = (sx0 - pad_x, sy0 - pad_y, sx1 + pad_x, sy1 + pad_y)
        draw.rectangle(box, outline=(255, 0, 0, 255))
        raw = crop_content(box)
        frame = to_game_frame(raw)
        name = NAMES[row_i][col_i] + ".png"
        frame.save(OUT / name)
        frame.save(assets_out / name)
        saved += 1
        print(f"saved {name} from tight {raw.size} -> 16x24")

debug.save(DEBUG)
print("saved", saved, "frames")
print("debug", DEBUG)
