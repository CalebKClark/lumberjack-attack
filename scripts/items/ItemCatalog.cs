using System;
using System.Collections.Generic;
using LumberjackAttack.Core;
using LumberjackAttack.World.Placeable;

namespace LumberjackAttack.Items;

public enum ToolType
{
	None,
	Axe,
	FishingRod,
}

public sealed class ItemDef
{
	public string Id { get; init; } = "";
	public string Name { get; init; } = "";
	public string Category { get; init; } = "";
	public int MaxStack { get; init; } = 999;
	public ToolType ToolType { get; init; }
	public bool IsPlaceable { get; init; }
	public bool IsCurrency { get; init; }
	public PlaceableKind PlaceableKind { get; init; }
	public int TreeDamage { get; init; }
	public int FishingSpeed { get; init; }
	public int FishingXp { get; init; }
	public int FoodHealth { get; init; }
	public int FoodEnergy { get; init; }
	public int BonusDamage { get; init; }
	public double BonusCritChance { get; init; }
	public double BonusCritMultiplier { get; init; }
	public int BonusMaxHealth { get; init; }
	public double BonusDefense { get; init; }
	public double BonusFishingSpeed { get; init; }

	public bool IsEdible => FoodHealth > 0 || FoodEnergy > 0;

	/// <summary>Fishing speed used by rods — CSV bonus_fishing_speed (also mirrored in FishingSpeed).</summary>
	public double EffectiveFishingSpeedBonus =>
		BonusFishingSpeed != 0 ? BonusFishingSpeed : FishingSpeed;
}

public static class ItemCatalog
{
	public static IReadOnlyDictionary<string, ItemDef> Load(string path = "res://data/items.csv")
	{
		var items = new Dictionary<string, ItemDef>(StringComparer.Ordinal);
		foreach (var p in CsvUtil.ReadDataRows(path, minColumns: 8))
		{
			var id = CsvUtil.Cell(p, 0);
			var category = CsvUtil.Cell(p, 2);
			var isPlaceable = category.Equals("placeable", StringComparison.OrdinalIgnoreCase);
			items[id] = new ItemDef
			{
				Id = id,
				Name = CsvUtil.Cell(p, 1),
				Category = category,
				MaxStack = Math.Max(1, CsvUtil.ParseInt(p, 3, 999)),
				ToolType = ParseTool(CsvUtil.Cell(p, 7)),
				IsPlaceable = isPlaceable,
				IsCurrency = category.Equals("currency", StringComparison.OrdinalIgnoreCase),
				PlaceableKind = PlaceableKindParser.Parse(CsvUtil.Cell(p, 18), id, isPlaceable),
				BonusDamage = CsvUtil.ParseInt(p, 8),
				BonusCritChance = CsvUtil.ParseDouble(p, 9),
				BonusCritMultiplier = CsvUtil.ParseDouble(p, 10),
				BonusMaxHealth = CsvUtil.ParseInt(p, 11),
				BonusDefense = CsvUtil.ParseDouble(p, 12),
				BonusFishingSpeed = CsvUtil.ParseDouble(p, 13),
				FishingSpeed = CsvUtil.ParseInt(p, 13),
				FishingXp = CsvUtil.ParseInt(p, 14),
				TreeDamage = CsvUtil.ParseInt(p, 15),
				FoodHealth = CsvUtil.ParseInt(p, 16),
				FoodEnergy = CsvUtil.ParseInt(p, 17),
			};
		}

		return items;
	}

	private static ToolType ParseTool(string raw) =>
		raw.ToLowerInvariant() switch
		{
			"axe" => ToolType.Axe,
			"rod" => ToolType.FishingRod,
			_ => ToolType.None,
		};
}
