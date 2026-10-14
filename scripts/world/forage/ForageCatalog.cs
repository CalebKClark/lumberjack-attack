using System;
using System.Collections.Generic;
using LumberjackAttack.Core;

namespace LumberjackAttack.World.Forage;

public sealed class ForageDef
{
	public string ItemId { get; init; } = "";
	public double Weight { get; init; } = 1;
	public string Pool { get; init; } = "ground";
}

/// <summary>Universal foragable drop pools (seasonal/area locks later).</summary>
public static class ForageCatalog
{
	public const string GroundPool = "ground";

	public static IReadOnlyList<ForageDef> Load(string path = "res://data/foragables.csv")
	{
		var list = new List<ForageDef>();
		foreach (var p in CsvUtil.TryReadDataRows(path, minColumns: 3))
		{
			list.Add(new ForageDef
			{
				ItemId = CsvUtil.Cell(p, 0),
				Weight = CsvUtil.ParseDouble(p, 1, 1),
				Pool = CsvUtil.Cell(p, 2, GroundPool).ToLowerInvariant(),
			});
		}

		return list;
	}

	public static string? Roll(IReadOnlyList<ForageDef> table, string pool = GroundPool)
	{
		var candidates = new List<ForageDef>();
		foreach (var row in table)
		{
			if (row.Pool.Equals(pool, StringComparison.OrdinalIgnoreCase)
				&& !string.IsNullOrEmpty(row.ItemId))
			{
				candidates.Add(row);
			}
		}

		if (candidates.Count == 0)
		{
			return null;
		}

		var total = 0.0;
		foreach (var row in candidates)
		{
			total += Math.Max(0, row.Weight);
		}

		if (total <= 0)
		{
			return candidates[^1].ItemId;
		}

		var roll = Random.Shared.NextDouble() * total;
		foreach (var row in candidates)
		{
			roll -= Math.Max(0, row.Weight);
			if (roll <= 0)
			{
				return row.ItemId;
			}
		}

		return candidates[^1].ItemId;
	}
}
