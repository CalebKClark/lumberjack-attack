using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using Godot;

namespace LumberjackAttack.Collection;

public enum CollectionCategory
{
	Wood,
	Combat,
	Fish,
}

public enum DiscoveryType
{
	Item,
	Enemy,
}

public sealed class CollectionEntryDef
{
	public CollectionCategory Category { get; init; }
	public string EntryId { get; init; } = "";
	public string DisplayName { get; init; } = "";
	public DiscoveryType DiscoveryType { get; init; }
	public string? DiscoveryId { get; init; }
	public string Tooltip { get; init; } = "";
	public int SortOrder { get; init; }
}

public static class CollectionCategoryExtensions
{
	public static string DisplayName(this CollectionCategory category) => category switch
	{
		CollectionCategory.Wood => "Wood",
		CollectionCategory.Combat => "Combat",
		CollectionCategory.Fish => "Fish",
		_ => category.ToString(),
	};

	public static CollectionCategory? FromId(string? id) =>
		id?.Trim().ToLowerInvariant() switch
		{
			"wood" => CollectionCategory.Wood,
			"combat" => CollectionCategory.Combat,
			"fish" => CollectionCategory.Fish,
			_ => null,
		};
}

/// <summary>Loads collection entries from data/collections.csv (Java CollectionRegistry).</summary>
public static class CollectionCatalog
{
	public static IReadOnlyList<CollectionEntryDef> Load(string path = "res://data/collections.csv")
	{
		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			GD.PushError($"Missing collections catalog: {path}");
			return Array.Empty<CollectionEntryDef>();
		}

		file.GetLine();
		var list = new List<CollectionEntryDef>();
		while (!file.EofReached())
		{
			var line = file.GetLine();
			if (string.IsNullOrWhiteSpace(line))
			{
				continue;
			}

			var p = ParseCsvLine(line);
			if (p.Count < 7)
			{
				continue;
			}

			var category = CollectionCategoryExtensions.FromId(p[0]);
			if (category == null)
			{
				continue;
			}

			var discoveryType = p[3].Trim().ToLowerInvariant() switch
			{
				"enemy" => DiscoveryType.Enemy,
				_ => DiscoveryType.Item,
			};

			var discoveryId = p[4].Trim();
			list.Add(new CollectionEntryDef
			{
				Category = category.Value,
				EntryId = p[1].Trim(),
				DisplayName = p[2].Trim(),
				DiscoveryType = discoveryType,
				DiscoveryId = string.IsNullOrEmpty(discoveryId) ? null : discoveryId,
				Tooltip = p[5].Trim(),
				SortOrder = int.TryParse(p[6].Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var n)
					? n
					: 0,
			});
		}

		return list.OrderBy(e => e.Category).ThenBy(e => e.SortOrder).ToList();
	}

	private static List<string> ParseCsvLine(string line)
	{
		var parts = new List<string>();
		var current = new StringBuilder();
		var inQuotes = false;
		foreach (var ch in line)
		{
			if (ch == '"')
			{
				inQuotes = !inQuotes;
				continue;
			}

			if (ch == ',' && !inQuotes)
			{
				parts.Add(current.ToString());
				current.Clear();
				continue;
			}

			current.Append(ch);
		}

		parts.Add(current.ToString());
		return parts;
	}
}
