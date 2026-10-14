using System;
using System.Collections.Generic;
using System.Globalization;
using Godot;

namespace LumberjackAttack.World;

/// <summary>Loads legacy Java .map files (width height + space-separated cell tokens) into a TileMapLayer.</summary>
public static class MapFileLoader
{
	public static RotationGrid ApplyToLayer(TileMapLayer layer, string mapPath)
	{
		var grid = LoadGrid(mapPath);
		layer.Clear();
		for (var row = 0; row < grid.Height; row++)
		{
			for (var col = 0; col < grid.Width; col++)
			{
				var id = grid.Tiles[row * grid.Width + col];
				if (id < 0)
				{
					continue;
				}

				TileRotation.SetCell(layer, new Vector2I(col, row), id);
			}
		}

		return new RotationGrid(grid.Width, grid.Height, (int[])grid.Rotations.Clone());
	}

	/// <summary>Parse a .map file into a tile grid without touching a TileMapLayer.</summary>
	public static MapStateStore.MapGrid LoadGrid(string mapPath)
	{
		using var file = Godot.FileAccess.Open(mapPath, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			throw new InvalidOperationException($"Map not found: {mapPath}");
		}

		var header = file.GetLine().Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
		var width = int.Parse(header[0], CultureInfo.InvariantCulture);
		var height = int.Parse(header[1], CultureInfo.InvariantCulture);
		var tiles = new int[width * height];
		var rotations = new int[width * height];
		Array.Fill(tiles, -1);

		for (var row = 0; row < height; row++)
		{
			if (file.EofReached())
			{
				throw new InvalidOperationException($"Map row missing at {row} in {mapPath}");
			}

			var tokens = file.GetLine().Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
			if (tokens.Length != width)
			{
				throw new InvalidOperationException($"Bad width at row {row} in {mapPath}");
			}

			for (var col = 0; col < width; col++)
			{
				var (tileId, rotation) = ParseCell(tokens[col]);
				var i = row * width + col;
				tiles[i] = tileId;
				rotations[i] = rotation & 3;
			}
		}

		return new MapStateStore.MapGrid(width, height, tiles, rotations);
	}

	/// <summary>All <c>res://data/*.map</c> ids (world maps for daily systems).</summary>
	public static List<string> ListMapIds(string directory = "res://data")
	{
		var ids = new List<string>();
		using var dir = DirAccess.Open(directory);
		if (dir == null)
		{
			return ids;
		}

		dir.ListDirBegin();
		while (true)
		{
			var name = dir.GetNext();
			if (string.IsNullOrEmpty(name))
			{
				break;
			}

			if (dir.CurrentIsDir() || !name.EndsWith(".map", StringComparison.OrdinalIgnoreCase))
			{
				continue;
			}

			ids.Add(name[..^4]);
		}

		dir.ListDirEnd();
		ids.Sort(StringComparer.Ordinal);
		return ids;
	}

	private static (int tileId, int rotation) ParseCell(string token)
	{
		if (token.EndsWith('t') || token.EndsWith('T'))
		{
			token = token[..^1];
		}

		var colon = token.IndexOf(':');
		if (colon < 0)
		{
			return (int.Parse(token, CultureInfo.InvariantCulture), 0);
		}

		var id = int.Parse(token[..colon], CultureInfo.InvariantCulture);
		var rot = int.Parse(token[(colon + 1)..], CultureInfo.InvariantCulture) & 3;
		return (id, rot);
	}
}
