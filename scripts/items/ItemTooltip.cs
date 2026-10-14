using System;
using System.Collections.Generic;
using Godot;

namespace LumberjackAttack.Items;

/// <summary>Builds colored tooltip lines for an item (Java ItemTooltipRenderer).</summary>
public static class ItemTooltip
{
	public static readonly Color NameColor = new(0.96f, 0.96f, 0.96f);
	public static readonly Color TypeColor = new(0.47f, 0.69f, 1f);
	public static readonly Color StatColor = new(0.47f, 0.86f, 0.55f);
	public static readonly Color BgColor = new(0.1f, 0.1f, 0.12f, 0.92f);

	public readonly struct Line
	{
		public string Text { get; init; }
		public Color Color { get; init; }
		public int FontSize { get; init; }
	}

	public static string TypeLabel(ItemDef item)
	{
		if (item.ToolType == ToolType.Axe)
		{
			return "Axe";
		}

		if (item.ToolType == ToolType.FishingRod)
		{
			return "Fishing Rod";
		}

		return item.Category switch
		{
			"tool" => "Tool",
			"placeable" => "Placeable",
			"currency" => "Currency",
			"consumable" => "Consumable",
			"material" => "Material",
			_ => string.IsNullOrEmpty(item.Category)
				? "Item"
				: char.ToUpperInvariant(item.Category[0]) + item.Category[1..],
		};
	}

	public static List<Line> BuildLines(ItemDef item)
	{
		var lines = new List<Line>
		{
			new() { Text = item.Name, Color = NameColor, FontSize = 15 },
			new() { Text = TypeLabel(item), Color = TypeColor, FontSize = 12 },
		};

		foreach (var stat in BuildStatTexts(item))
		{
			lines.Add(new Line { Text = stat, Color = StatColor, FontSize = 12 });
		}

		return lines;
	}

	public static List<string> BuildStatTexts(ItemDef item)
	{
		var lines = new List<string>();
		if (item.BonusDamage != 0)
		{
			lines.Add($"Damage: {Signed(item.BonusDamage)}");
		}

		if (item.BonusCritChance != 0)
		{
			lines.Add($"Crit Chance: {Signed(item.BonusCritChance)}%");
		}

		if (item.BonusCritMultiplier != 0)
		{
			lines.Add($"Crit Multiplier: {Signed(item.BonusCritMultiplier)}×");
		}

		if (item.BonusMaxHealth != 0)
		{
			lines.Add($"Max Health: {Signed(item.BonusMaxHealth)}");
		}

		if (item.BonusDefense != 0)
		{
			lines.Add($"Defense: {Signed(item.BonusDefense)}%");
		}

		if (item.BonusFishingSpeed != 0)
		{
			lines.Add($"Fishing Speed: {Signed(item.BonusFishingSpeed)}%");
		}

		if (item.TreeDamage > 0)
		{
			lines.Add($"Tree Damage: {item.TreeDamage}");
		}

		if (item.FoodHealth > 0)
		{
			lines.Add($"Health: +{item.FoodHealth}");
		}

		if (item.FoodEnergy > 0)
		{
			lines.Add($"Energy: +{item.FoodEnergy}");
		}

		return lines;
	}

	private static string Signed(double value)
	{
		if (value > 0)
		{
			return "+" + Trim(value);
		}

		return Trim(value);
	}

	private static string Trim(double value) =>
		value == (long)value ? ((long)value).ToString() : value.ToString(System.Globalization.CultureInfo.InvariantCulture);
}
