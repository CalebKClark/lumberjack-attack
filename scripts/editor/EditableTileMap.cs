using System;
using System.Text;
using LumberjackAttack.Core;

namespace LumberjackAttack.Editor;

/// <summary>Mutable tile grid for the world editor (tiles, rotations, transfer markers).</summary>
public sealed class EditableTileMap
{
	public const int DefaultSize = 5;
	public const int MinSize = 1;

	private int[][] _tiles;
	private int[][] _rotations;
	private bool[][] _markers;

	public EditableTileMap(string mapId, int width, int height, int fillTileId)
	{
		MapId = mapId;
		_tiles = Alloc(height, width, fillTileId);
		_rotations = Alloc(height, width, 0);
		_markers = AllocBool(height, width);
	}

	public EditableTileMap(string mapId, int[][] tiles, int[][] rotations, bool[][]? markers = null)
	{
		MapId = mapId;
		_tiles = Copy(tiles);
		_rotations = rotations == null ? Alloc(tiles.Length, tiles[0].Length, 0) : Copy(rotations);
		_markers = markers == null ? AllocBool(tiles.Length, tiles[0].Length) : CopyBool(markers);
	}

	public static EditableTileMap CreateDefault(string mapId = "untitled") =>
		new(mapId, DefaultSize, DefaultSize, GameConfig.BlankTileId);

	public string MapId { get; set; }

	public int Width => _tiles[0].Length;

	public int Height => _tiles.Length;

	public int GetTile(int col, int row) => _tiles[row][col];

	public int GetRotation(int col, int row) => _rotations[row][col] & 3;

	public bool HasMarker(int col, int row) => _markers[row][col];

	public void SetMarker(int col, int row, bool marked) => _markers[row][col] = marked;

	public void SetTile(int col, int row, int tileId, int rotation)
	{
		_tiles[row][col] = tileId;
		_rotations[row][col] = rotation & 3;
	}

	public bool InBounds(int col, int row) =>
		col >= 0 && row >= 0 && row < Height && col < Width;

	public int[][] CopyTiles() => Copy(_tiles);

	public int[][] CopyRotations() => Copy(_rotations);

	public bool[][] CopyMarkers() => CopyBool(_markers);

	public void Restore(int[][] tiles, int[][] rotations, bool[][] markers)
	{
		_tiles = Copy(tiles);
		_rotations = Copy(rotations);
		_markers = CopyBool(markers);
	}

	public void ExtendUp(int fill) => InsertRow(0, fill);

	public void ExtendDown(int fill) => InsertRow(Height, fill);

	public void ExtendLeft(int fill) => InsertCol(0, fill);

	public void ExtendRight(int fill) => InsertCol(Width, fill);

	public bool ShrinkUp() => RemoveRow(0);

	public bool ShrinkDown() => RemoveRow(Height - 1);

	public bool ShrinkLeft() => RemoveCol(0);

	public bool ShrinkRight() => RemoveCol(Width - 1);

	public string ToFileContents()
	{
		var sb = new StringBuilder();
		sb.Append(Width).Append(' ').Append(Height).Append('\n');
		for (var row = 0; row < Height; row++)
		{
			for (var col = 0; col < Width; col++)
			{
				if (col > 0)
				{
					sb.Append(' ');
				}

				sb.Append(World.MapCellCodec.Format(
					_tiles[row][col],
					_rotations[row][col],
					_markers[row][col]
				));
			}

			sb.Append('\n');
		}

		return sb.ToString();
	}

	private void InsertRow(int at, int fill)
	{
		var w = Width;
		var h = Height;
		var tiles = new int[h + 1][];
		var rots = new int[h + 1][];
		var marks = new bool[h + 1][];
		for (var r = 0; r <= h; r++)
		{
			if (r < at)
			{
				tiles[r] = (int[])_tiles[r].Clone();
				rots[r] = (int[])_rotations[r].Clone();
				marks[r] = (bool[])_markers[r].Clone();
			}
			else if (r == at)
			{
				tiles[r] = new int[w];
				rots[r] = new int[w];
				marks[r] = new bool[w];
				Array.Fill(tiles[r], fill);
			}
			else
			{
				tiles[r] = (int[])_tiles[r - 1].Clone();
				rots[r] = (int[])_rotations[r - 1].Clone();
				marks[r] = (bool[])_markers[r - 1].Clone();
			}
		}

		_tiles = tiles;
		_rotations = rots;
		_markers = marks;
	}

	private void InsertCol(int at, int fill)
	{
		var w = Width;
		var h = Height;
		var tiles = new int[h][];
		var rots = new int[h][];
		var marks = new bool[h][];
		for (var r = 0; r < h; r++)
		{
			tiles[r] = new int[w + 1];
			rots[r] = new int[w + 1];
			marks[r] = new bool[w + 1];
			for (var c = 0; c <= w; c++)
			{
				if (c < at)
				{
					tiles[r][c] = _tiles[r][c];
					rots[r][c] = _rotations[r][c];
					marks[r][c] = _markers[r][c];
				}
				else if (c == at)
				{
					tiles[r][c] = fill;
				}
				else
				{
					tiles[r][c] = _tiles[r][c - 1];
					rots[r][c] = _rotations[r][c - 1];
					marks[r][c] = _markers[r][c - 1];
				}
			}
		}

		_tiles = tiles;
		_rotations = rots;
		_markers = marks;
	}

	private bool RemoveRow(int at)
	{
		if (Height <= MinSize)
		{
			return false;
		}

		var w = Width;
		var h = Height;
		var tiles = new int[h - 1][];
		var rots = new int[h - 1][];
		var marks = new bool[h - 1][];
		var dest = 0;
		for (var r = 0; r < h; r++)
		{
			if (r == at)
			{
				continue;
			}

			tiles[dest] = (int[])_tiles[r].Clone();
			rots[dest] = (int[])_rotations[r].Clone();
			marks[dest] = (bool[])_markers[r].Clone();
			dest++;
		}

		_tiles = tiles;
		_rotations = rots;
		_markers = marks;
		return true;
	}

	private bool RemoveCol(int at)
	{
		if (Width <= MinSize)
		{
			return false;
		}

		var w = Width;
		var h = Height;
		var tiles = new int[h][];
		var rots = new int[h][];
		var marks = new bool[h][];
		for (var r = 0; r < h; r++)
		{
			tiles[r] = new int[w - 1];
			rots[r] = new int[w - 1];
			marks[r] = new bool[w - 1];
			var dest = 0;
			for (var c = 0; c < w; c++)
			{
				if (c == at)
				{
					continue;
				}

				tiles[r][dest] = _tiles[r][c];
				rots[r][dest] = _rotations[r][c];
				marks[r][dest] = _markers[r][c];
				dest++;
			}
		}

		_tiles = tiles;
		_rotations = rots;
		_markers = marks;
		return true;
	}

	private static int[][] Alloc(int h, int w, int fill)
	{
		var a = new int[h][];
		for (var r = 0; r < h; r++)
		{
			a[r] = new int[w];
			Array.Fill(a[r], fill);
		}

		return a;
	}

	private static bool[][] AllocBool(int h, int w)
	{
		var a = new bool[h][];
		for (var r = 0; r < h; r++)
		{
			a[r] = new bool[w];
		}

		return a;
	}

	private static int[][] Copy(int[][] src)
	{
		var a = new int[src.Length][];
		for (var r = 0; r < src.Length; r++)
		{
			a[r] = (int[])src[r].Clone();
		}

		return a;
	}

	private static bool[][] CopyBool(bool[][] src)
	{
		var a = new bool[src.Length][];
		for (var r = 0; r < src.Length; r++)
		{
			a[r] = (bool[])src[r].Clone();
		}

		return a;
	}
}
