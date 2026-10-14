"""Build lumberjack_font.png + .fnt from Cursor-uploaded glyph PNGs.

Uppercase tags: A–Z
Lowercase tags: a_–z_
Punctuation tags: . - colon semicolon quotations
Files named only '_' are disambiguated by ink shape → ! + ,
"""

from __future__ import annotations

from pathlib import Path
import re

from PIL import Image

SRC_DIR = Path(
	r"C:\Users\Caleb\.cursor\projects\c-Users-Caleb-eclipse-workspace-2DGame\assets"
)
OUT_DIR = Path(r"C:\Users\Caleb\LumberjackAttack\assets\fonts\custom_pixel")
PREFIX = (
	"c__Users_Caleb_AppData_Roaming_Cursor_User_workspaceStorage_"
	"af27a9f6c642e38500682321d3432307_images_"
)

SRC_W, SRC_H = 7, 10
# Extra rows below baseline so p/q/j/g/y descenders hang properly.
DESCENDER_PAD = 3
CELL_W, CELL_H = SRC_W, SRC_H + DESCENDER_PAD  # 7x13
BASELINE = SRC_H - 1  # row 9 — bottom of non-descender glyphs
# How many px to shift glyphs down from their source cell.
# g/y/q/j share one descender band (j is the reference).
DESCENDER_SHIFT = {
	"g": 2,
	"y": 2,
	"q": 2,
	"j": 2,
	"p": 3,
	",": 3,
	"!": 1,
	";": 1,
}

TAG_RE = re.compile(
	r"-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
	re.I,
)


def tag_of(path: Path) -> str:
	rest = path.name[len(PREFIX) :]
	stem = rest.rsplit(".", 1)[0]
	return TAG_RE.sub("", stem)


def to_white_glyph(im: Image.Image) -> Image.Image:
	"""Solid white ink (no soft alpha) so scaled UI text stays high-contrast."""
	im = im.convert("RGBA")
	out = Image.new("RGBA", im.size, (0, 0, 0, 0))
	sp = im.load()
	op = out.load()
	w, h = im.size
	for y in range(h):
		for x in range(w):
			r, g, b, a = sp[x, y]
			if a < 8:
				continue
			if r > 200 and g > 200 and b > 200:
				continue
			lum = (r + g + b) / 3
			# Hard threshold — gray fringes wash out when the font is upscaled.
			if lum > 160:
				continue
			op[x, y] = (255, 255, 255, 255)
	return out


def ink_bounds(im: Image.Image) -> tuple[int, int, int, int] | None:
	px = im.load()
	w, h = im.size
	xs: list[int] = []
	ys: list[int] = []
	for y in range(h):
		for x in range(w):
			if px[x, y][3] > 20:
				xs.append(x)
				ys.append(y)
	if not xs:
		return None
	return min(xs), min(ys), max(xs), max(ys)


def center_horizontally(im: Image.Image, width: int = CELL_W) -> Image.Image:
	"""Center ink in a CELL_W-wide canvas (keeps source height)."""
	bounds = ink_bounds(im)
	if bounds is None:
		return Image.new("RGBA", (width, im.height), (0, 0, 0, 0))
	x0, _, x1, _ = bounds
	ink_w = x1 - x0 + 1
	dest_x = max(0, (width - ink_w) // 2)
	canvas = Image.new("RGBA", (width, im.height), (0, 0, 0, 0))
	cropped = im.crop((x0, 0, x1 + 1, im.height))
	canvas.paste(cropped, (dest_x, 0), cropped)
	return canvas


def place_in_cell(src: Image.Image, *, shift_down: int = 0) -> Image.Image:
	cell = Image.new("RGBA", (CELL_W, CELL_H), (0, 0, 0, 0))
	cell.paste(src, (0, shift_down), src)
	return cell


def xadvance_for(im: Image.Image) -> int:
	"""Advance must match cell width so horizontally-centered ink stays centered.

	A tight advance (ink_w+2) with a full CELL_W glyph made narrow letters like
	`i` sit on the right edge of their slot.
	"""
	bounds = ink_bounds(im)
	if bounds is None:
		return max(3, CELL_W // 2)
	return CELL_W + 1


def collect_sources() -> dict[str, Path]:
	"""Newest 7x10 PNG per tag; '_' files remapped by shape."""
	by_tag: dict[str, tuple[float, Path]] = {}
	underscore_files: list[Path] = []

	for f in SRC_DIR.glob(PREFIX + "*.png"):
		try:
			size = Image.open(f).size
		except OSError:
			continue
		if size != (SRC_W, SRC_H):
			continue
		tag = tag_of(f)
		if tag == "_":
			underscore_files.append(f)
			continue
		mt = f.stat().st_mtime
		if tag not in by_tag or mt > by_tag[tag][0]:
			by_tag[tag] = (mt, f)

	# '_' uploads are punctuation whose filenames got mangled.
	for f in underscore_files:
		g = to_white_glyph(Image.open(f))
		b = ink_bounds(g)
		if b is None:
			continue
		_, y0, _, y1 = b
		# ! occupies top rows; + is a mid cross; , sits low with a curl.
		if y0 <= 1:
			key = "!"
		elif y0 >= 4:
			key = ","
		else:
			key = "+"
		mt = f.stat().st_mtime
		if key not in by_tag or mt > by_tag[key][0]:
			by_tag[key] = (mt, f)

	return {k: v[1] for k, v in by_tag.items()}


def main() -> None:
	OUT_DIR.mkdir(parents=True, exist_ok=True)
	sources = collect_sources()

	# Character → source tag
	char_tags: list[tuple[str, str]] = []
	for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
		char_tags.append((c, c))
	for c in "abcdefghijklmnopqrstuvwxyz":
		char_tags.append((c, f"{c}_"))
	punct = [
		(".", "."),
		("-", "-"),
		(":", "colon"),
		(";", "semicolon"),
		('"', "quotations"),
		("!", "!"),
		("+", "+"),
		(",", ","),
	]
	char_tags.extend(punct)

	missing = [ch for ch, tag in char_tags if tag not in sources]
	if missing:
		raise SystemExit(f"missing glyphs for: {missing}")

	# Build cells
	cells: dict[str, Image.Image] = {}
	advances: dict[str, int] = {}
	for ch, tag in char_tags:
		raw = to_white_glyph(Image.open(sources[tag]))
		if raw.size != (SRC_W, SRC_H):
			padded = Image.new("RGBA", (SRC_W, SRC_H), (0, 0, 0, 0))
			padded.paste(raw, (0, 0), raw)
			raw = padded
		centered = center_horizontally(raw)
		shift = DESCENDER_SHIFT.get(ch, 0)
		cell = place_in_cell(centered, shift_down=shift)
		cells[ch] = cell
		advances[ch] = xadvance_for(cell)

	# Smart quotes / dashes → nearest custom glyphs. Apostrophe stays on fallback.
	alias_chars: list[tuple[str, str]] = [
		("\u201c", '"'),
		("\u201d", '"'),
		("\u2014", "-"),  # em dash → hyphen
		("\u2013", "-"),  # en dash → hyphen
	]

	order = [ch for ch, _ in char_tags]
	cols = len(order)
	atlas = Image.new("RGBA", (CELL_W * cols, CELL_H), (0, 0, 0, 0))
	positions: dict[str, int] = {}
	for i, ch in enumerate(order):
		x = i * CELL_W
		atlas.paste(cells[ch], (x, 0), cells[ch])
		positions[ch] = x

	atlas_path = OUT_DIR / "lumberjack_font.png"
	atlas.save(atlas_path)

	line_height = CELL_H
	base = BASELINE
	xadv_default = CELL_W + 1

	char_entries: list[str] = [
		f"char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 "
		f"xadvance={max(3, CELL_W // 2)} page=0 chnl=15"
	]

	def add_char(ch: str, atlas_x: int, advance: int) -> None:
		char_entries.append(
			f"char id={ord(ch)} x={atlas_x} y=0 width={CELL_W} height={CELL_H} "
			f"xoffset=0 yoffset=0 xadvance={advance} page=0 chnl=15"
		)

	for ch in order:
		add_char(ch, positions[ch], advances[ch])

	for alias, target in alias_chars:
		if target in positions:
			add_char(alias, positions[target], advances[target])

	lines = [
		f'info face="Lumberjack" size={CELL_H} bold=0 italic=0 charset="" '
		f"unicode=1 stretchH=100 smooth=0 aa=0 padding=0,0,0,0 spacing=0,0 outline=0",
		f"common lineHeight={line_height} base={base} "
		f"scaleW={atlas.width} scaleH={atlas.height} pages=1 packed=0",
		'page id=0 file="lumberjack_font.png"',
		f"chars count={len(char_entries)}",
		*char_entries,
	]
	(OUT_DIR / "lumberjack_font.fnt").write_text("\n".join(lines) + "\n", encoding="utf-8")

	# Debug dumps
	gdir = OUT_DIR / "glyphs"
	gdir.mkdir(exist_ok=True)
	for ch, cell in cells.items():
		# Windows is case-insensitive — prefix case so A.png and a.png don't collide.
		safe = {
			".": "dot",
			":": "colon",
			";": "semicolon",
			'"': "quot",
			"!": "bang",
			"+": "plus",
			",": "comma",
			"-": "hyphen",
		}.get(ch)
		if safe is None:
			safe = f"U_{ch}" if ch.isupper() else f"l_{ch}"
		cell.save(gdir / f"{safe}.png")

	print(f"atlas {atlas.size} -> {atlas_path}")
	print(f"glyphs {len(order)} + aliases; lineHeight={line_height} base={base}")
	print("descender shifts:", DESCENDER_SHIFT)
	print("done")


if __name__ == "__main__":
	main()
