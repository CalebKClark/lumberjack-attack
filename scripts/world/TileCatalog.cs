using System;
using System.Collections.Generic;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

public sealed class TileDef
{
	public int Id { get; init; }
	public string Name { get; init; } = "";
	public bool Solid { get; init; }
	public bool Choppable { get; init; }
	public bool Fishable { get; init; }
	public bool Bed { get; init; }
	public bool Placeable { get; init; }
	public string? DropItemId { get; init; }
	public int RegrowthGameMinutes { get; init; }
	public int ForagingXp { get; init; }
	public int Toughness { get; init; }
	public int DropQtyMin { get; init; }
	public int DropQtyMax { get; init; }

	public int RollDropQuantity()
	{
		var min = Math.Max(1, DropQtyMin);
		var max = Math.Max(min, DropQtyMax);
		return Random.Shared.Next(min, max + 1);
	}
}

/// <summary>Loads tile metadata from data/tiles.csv (same columns as the Java build).</summary>
public static class TileCatalog
{
	public static IReadOnlyDictionary<int, TileDef> Load(string csvPath = "res://data/tiles.csv")
	{
		var tiles = new Dictionary<int, TileDef>();
		foreach (var p in CsvUtil.ReadDataRows(csvPath, minColumns: 12))
		{
			var id = CsvUtil.ParseInt(p, 0, -1);
			if (id < 0)
			{
				continue;
			}

			var drop = CsvUtil.Cell(p, 7);
			tiles[id] = new TileDef
			{
				Id = id,
				Name = CsvUtil.Cell(p, 1),
				Solid = CsvUtil.ParseBool(p, 2),
				Choppable = CsvUtil.ParseBool(p, 6),
				DropItemId = string.IsNullOrWhiteSpace(drop) ? null : drop,
				RegrowthGameMinutes = CsvUtil.ParseInt(p, 8),
				Bed = CsvUtil.ParseBool(p, 9),
				Placeable = CsvUtil.ParseBool(p, 10),
				Fishable = CsvUtil.ParseBool(p, 11),
				ForagingXp = CsvUtil.ParseInt(p, 12),
				Toughness = CsvUtil.ParseInt(p, 13),
				DropQtyMin = CsvUtil.ParseInt(p, 14, 1),
				DropQtyMax = CsvUtil.ParseInt(p, 15, 1),
			};
		}

		return tiles;
	}
}
