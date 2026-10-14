using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;
using Godot;
using LumberjackAttack.World;

namespace LumberjackAttack.Editor;

/// <summary>Load/save editor maps under <c>res://data/*.map</c> (project filesystem).</summary>
public static class EditorMapIo
{
	public const string MapsDirRes = "res://data";

	public static string MapsDirectoryAbsolute =>
		ProjectSettings.GlobalizePath(MapsDirRes);

	public static List<string> ListMapIds()
	{
		var ids = new List<string>();
		var dir = MapsDirectoryAbsolute;
		if (!Directory.Exists(dir))
		{
			return ids;
		}

		foreach (var path in Directory.GetFiles(dir, "*.map"))
		{
			ids.Add(Path.GetFileNameWithoutExtension(path));
		}

		ids.Sort(StringComparer.Ordinal);
		return ids;
	}

	public static EditableTileMap Load(string mapId)
	{
		var path = Path.Combine(MapsDirectoryAbsolute, mapId + ".map");
		if (!File.Exists(path))
		{
			throw new FileNotFoundException($"Map not found: {path}");
		}

		var lines = File.ReadAllLines(path, Encoding.UTF8);
		if (lines.Length == 0)
		{
			throw new InvalidOperationException($"Map file empty: {path}");
		}

		var dims = lines[0].Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
		var width = int.Parse(dims[0], CultureInfo.InvariantCulture);
		var height = int.Parse(dims[1], CultureInfo.InvariantCulture);
		var tiles = new int[height][];
		var rots = new int[height][];
		var marks = new bool[height][];
		for (var row = 0; row < height; row++)
		{
			if (row + 1 >= lines.Length)
			{
				throw new InvalidOperationException($"Map row missing at {row}");
			}

			var tokens = lines[row + 1].Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
			if (tokens.Length != width)
			{
				throw new InvalidOperationException($"Bad width at row {row}");
			}

			tiles[row] = new int[width];
			rots[row] = new int[width];
			marks[row] = new bool[width];
			for (var col = 0; col < width; col++)
			{
				tiles[row][col] = MapCellCodec.ParseTileId(tokens[col]);
				rots[row][col] = MapCellCodec.ParseRotation(tokens[col]);
				marks[row][col] = MapCellCodec.ParseTransitionMarker(tokens[col]);
			}
		}

		return new EditableTileMap(mapId, tiles, rots, marks);
	}

	public static void Save(EditableTileMap map)
	{
		Directory.CreateDirectory(MapsDirectoryAbsolute);
		var path = Path.Combine(MapsDirectoryAbsolute, map.MapId + ".map");
		File.WriteAllText(path, map.ToFileContents(), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
	}

	public static bool IsValidMapId(string? mapId)
	{
		if (string.IsNullOrWhiteSpace(mapId))
		{
			return false;
		}

		var t = mapId.Trim();
		if (t.Length > 40)
		{
			return false;
		}

		if (t[0] is < 'A' or > 'Z' and (< 'a' or > 'z'))
		{
			return false;
		}

		foreach (var c in t)
		{
			if (c is (>= 'A' and <= 'Z') or (>= 'a' and <= 'z') or (>= '0' and <= '9') or '_')
			{
				continue;
			}

			return false;
		}

		return true;
	}

	public static string NormalizeMapId(string mapId) => mapId.Trim().ToLowerInvariant();
}
