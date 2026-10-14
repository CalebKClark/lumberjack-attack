using System.Collections.Generic;
using System.Globalization;
using Godot;

namespace LumberjackAttack.World;

public readonly record struct MapTransition(
	string SourceMapId,
	int Col,
	int Row,
	string TargetMapId,
	int SpawnCol,
	int SpawnRow
);

public static class TransitionCatalog
{
	public static IReadOnlyList<MapTransition> Load(string path = "res://data/transitions.csv")
	{
		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			return System.Array.Empty<MapTransition>();
		}

		file.GetLine();
		var list = new List<MapTransition>();
		while (!file.EofReached())
		{
			var line = file.GetLine();
			if (string.IsNullOrWhiteSpace(line))
			{
				continue;
			}

			var p = line.Split(',');
			if (p.Length < 6)
			{
				continue;
			}

			var source = p[0].Trim();
			var target = p[3].Trim();
			if (!MapExists(source) || !MapExists(target))
			{
				continue; // skip maps not copied into data/ yet (e.g. dungeon)
			}

			list.Add(new MapTransition(
				source,
				int.Parse(p[1], CultureInfo.InvariantCulture),
				int.Parse(p[2], CultureInfo.InvariantCulture),
				target,
				int.Parse(p[4], CultureInfo.InvariantCulture),
				int.Parse(p[5], CultureInfo.InvariantCulture)
			));
		}

		return list;
	}

	// .map is raw data, not a Godot Resource — ResourceLoader.Exists is false and was
	// dropping cabin_interior → homestead (enter still worked via the building door).
	public static bool MapExists(string mapId) =>
		Godot.FileAccess.FileExists($"res://data/{mapId}.map");
}
