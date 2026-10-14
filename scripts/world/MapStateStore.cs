using System.Collections.Generic;
using Godot;

namespace LumberjackAttack.World;

/// <summary>Caches edited map tile grids so cabin↔homestead and saves keep stumps/edits/rotations.</summary>
public sealed class MapStateStore
{
	private readonly Dictionary<string, MapGrid> _maps = new();

	public bool Has(string mapId) => _maps.ContainsKey(mapId);

	public void CaptureFromLayer(string mapId, TileMapLayer layer, RotationGrid rotations)
	{
		var used = layer.GetUsedRect();
		if (used.Size.X <= 0 || used.Size.Y <= 0)
		{
			return;
		}

		var width = used.Position.X + used.Size.X;
		var height = used.Position.Y + used.Size.Y;
		var tiles = new int[height * width];
		var rots = new int[height * width];
		for (var row = 0; row < height; row++)
		{
			for (var col = 0; col < width; col++)
			{
				var cell = new Vector2I(col, row);
				var id = layer.GetCellSourceId(cell);
				tiles[row * width + col] = id;
				rots[row * width + col] = rotations.Get(col, row);
			}
		}

		_maps[mapId] = new MapGrid(width, height, tiles, rots);
	}

	public RotationGrid ApplyToLayer(string mapId, TileMapLayer layer)
	{
		if (!_maps.TryGetValue(mapId, out var grid))
		{
			return new RotationGrid(0, 0);
		}

		layer.Clear();
		for (var row = 0; row < grid.Height; row++)
		{
			for (var col = 0; col < grid.Width; col++)
			{
				var i = row * grid.Width + col;
				var id = grid.Tiles[i];
				if (id < 0)
				{
					continue;
				}

				TileRotation.SetCell(layer, new Vector2I(col, row), id);
			}
		}

		return new RotationGrid(grid.Width, grid.Height, (int[])grid.Rotations.Clone());
	}

	public void Put(string mapId, int width, int height, IReadOnlyList<int> tiles, IReadOnlyList<int>? rotations = null)
	{
		var copy = new int[width * height];
		var rotCopy = new int[width * height];
		for (var i = 0; i < copy.Length && i < tiles.Count; i++)
		{
			copy[i] = tiles[i];
		}

		if (rotations != null)
		{
			for (var i = 0; i < rotCopy.Length && i < rotations.Count; i++)
			{
				rotCopy[i] = rotations[i] & 3;
			}
		}

		_maps[mapId] = new MapGrid(width, height, copy, rotCopy);
	}

	public int GetTileId(string mapId, int col, int row)
	{
		if (!_maps.TryGetValue(mapId, out var grid))
		{
			return -1;
		}

		if (col < 0 || row < 0 || col >= grid.Width || row >= grid.Height)
		{
			return -1;
		}

		return grid.Tiles[row * grid.Width + col];
	}

	public IReadOnlyDictionary<string, MapGrid> All => _maps;

	public void Clear() => _maps.Clear();

	public readonly struct MapGrid
	{
		public int Width { get; }
		public int Height { get; }
		public int[] Tiles { get; }
		public int[] Rotations { get; }

		public MapGrid(int width, int height, int[] tiles, int[]? rotations = null)
		{
			Width = width;
			Height = height;
			Tiles = tiles;
			Rotations = rotations ?? new int[width * height];
		}
	}
}
