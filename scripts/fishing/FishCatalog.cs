using System;
using System.Collections.Generic;
using LumberjackAttack.Core;

namespace LumberjackAttack.Fishing;

public sealed class FishSpawnDef
{
	public string ItemId { get; init; } = "";
	public double Weight { get; init; }
	public int MinDepth { get; init; }
	public int Speed { get; init; }
	public string Kind { get; init; } = "fish";

	public bool IsTrash => Kind == "trash";
}

public static class FishCatalog
{
	public static IReadOnlyList<FishSpawnDef> Load(string path = "res://data/fish.csv")
	{
		var list = new List<FishSpawnDef>();
		foreach (var p in CsvUtil.TryReadDataRows(path, minColumns: 5))
		{
			list.Add(new FishSpawnDef
			{
				ItemId = CsvUtil.Cell(p, 0),
				Weight = CsvUtil.ParseDouble(p, 1, 1),
				MinDepth = CsvUtil.ParseInt(p, 2),
				Speed = CsvUtil.ParseInt(p, 3),
				Kind = CsvUtil.Cell(p, 4).ToLowerInvariant(),
			});
		}

		return list;
	}

	public static FishSpawnDef? FindByItemId(IReadOnlyList<FishSpawnDef> table, string? itemId)
	{
		if (string.IsNullOrEmpty(itemId))
		{
			return null;
		}

		foreach (var row in table)
		{
			if (row.ItemId == itemId)
			{
				return row;
			}
		}

		return null;
	}

	public static FishSpawnDef RollTrash(IReadOnlyList<FishSpawnDef> table)
	{
		var pool = new List<FishSpawnDef>();
		foreach (var row in table)
		{
			if (row.IsTrash)
			{
				pool.Add(row);
			}
		}

		return WeightedPick(pool) ?? FallbackTrash();
	}

	public static FishSpawnDef RollFishAtDepth(IReadOnlyList<FishSpawnDef> table, double depth)
	{
		var pool = new List<FishSpawnDef>();
		foreach (var row in table)
		{
			if (!row.IsTrash && depth >= row.MinDepth)
			{
				pool.Add(row);
			}
		}

		return WeightedPick(pool) ?? FallbackFish();
	}

	private static FishSpawnDef? WeightedPick(List<FishSpawnDef> pool)
	{
		if (pool.Count == 0)
		{
			return null;
		}

		var total = 0.0;
		foreach (var row in pool)
		{
			total += Math.Max(0, row.Weight);
		}

		if (total <= 0)
		{
			return pool[^1];
		}

		var roll = Random.Shared.NextDouble() * total;
		foreach (var row in pool)
		{
			roll -= Math.Max(0, row.Weight);
			if (roll <= 0)
			{
				return row;
			}
		}

		return pool[^1];
	}

	private static FishSpawnDef FallbackFish() => new()
	{
		ItemId = "bass",
		Weight = 1,
		MinDepth = 0,
		Speed = 1,
		Kind = "fish",
	};

	private static FishSpawnDef FallbackTrash() => new()
	{
		ItemId = "trash",
		Weight = 1,
		MinDepth = 0,
		Speed = 1,
		Kind = "trash",
	};
}
