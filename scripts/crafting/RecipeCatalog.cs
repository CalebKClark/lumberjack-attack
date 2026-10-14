using System;
using System.Collections.Generic;
using System.Globalization;
using Godot;

namespace LumberjackAttack.Crafting;

public sealed class RecipeIngredient
{
	public string ItemId { get; init; } = "";
	public int Quantity { get; init; }
}

public sealed class RecipeDef
{
	public string Id { get; init; } = "";
	public string DisplayName { get; init; } = "";
	public string Category { get; init; } = "";
	public string OutputItemId { get; init; } = "";
	public int OutputQuantity { get; init; } = 1;
	public IReadOnlyList<RecipeIngredient> Ingredients { get; init; } = Array.Empty<RecipeIngredient>();
	public int SortOrder { get; init; }
	/// <summary>If set, unlocks when this item is discovered; else when all ingredients are discovered.</summary>
	public string? UnlockItemId { get; init; }
}

public static class RecipeCatalog
{
	public static IReadOnlyList<RecipeDef> Load(string path = "res://data/recipes.csv")
	{
		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			return Array.Empty<RecipeDef>();
		}

		file.GetLine();
		var list = new List<RecipeDef>();
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

			list.Add(new RecipeDef
			{
				Id = p[0].Trim(),
				DisplayName = p[1].Trim(),
				Category = p[2].Trim(),
				OutputItemId = p[3].Trim(),
				OutputQuantity = ParseInt(p[4], 1),
				Ingredients = ParseIngredients(p[5]),
				SortOrder = p.Length > 6 ? ParseInt(p[6], 0) : 0,
				UnlockItemId = p.Length > 7 && !string.IsNullOrWhiteSpace(p[7]) ? p[7].Trim() : null,
			});
		}

		list.Sort((a, b) => a.SortOrder.CompareTo(b.SortOrder));
		return list;
	}

	private static List<RecipeIngredient> ParseIngredients(string raw)
	{
		var result = new List<RecipeIngredient>();
		foreach (var part in raw.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
		{
			var pair = part.Split(':');
			if (pair.Length < 2)
			{
				continue;
			}

			result.Add(new RecipeIngredient
			{
				ItemId = pair[0].Trim(),
				Quantity = ParseInt(pair[1], 1),
			});
		}

		return result;
	}

	private static int ParseInt(string raw, int fallback) =>
		int.TryParse(raw.Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var n) ? n : fallback;
}
