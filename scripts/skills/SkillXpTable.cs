using System;
using System.Globalization;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.Skills;

/// <summary>Cumulative XP thresholds per level (Java SkillXpTable).</summary>
public static class SkillXpTable
{
	private static readonly int[] TotalXpByLevel = Load();

	public static int MaxLevel => GameConfig.SkillMaxLevel;

	public static int TotalXpRequiredForLevel(int level)
	{
		if (level <= 1)
		{
			return 0;
		}

		if (level > MaxLevel)
		{
			return TotalXpByLevel[MaxLevel];
		}

		return TotalXpByLevel[level];
	}

	public static int ResolveLevel(int totalXp)
	{
		var level = 1;
		for (var candidate = 2; candidate <= MaxLevel; candidate++)
		{
			if (totalXp >= TotalXpRequiredForLevel(candidate))
			{
				level = candidate;
			}
			else
			{
				break;
			}
		}

		return level;
	}

	public static int XpToNextLevel(int totalXp)
	{
		var level = ResolveLevel(totalXp);
		if (level >= MaxLevel)
		{
			return 0;
		}

		return TotalXpRequiredForLevel(level + 1) - totalXp;
	}

	public static float ProgressInLevel(int totalXp)
	{
		var level = ResolveLevel(totalXp);
		if (level >= MaxLevel)
		{
			return 1f;
		}

		var floor = TotalXpRequiredForLevel(level);
		var next = TotalXpRequiredForLevel(level + 1);
		var span = next - floor;
		return span <= 0 ? 1f : Math.Clamp((totalXp - floor) / (float)span, 0f, 1f);
	}

	private static int[] Load(string path = "res://data/skill_xp.csv")
	{
		var table = new int[GameConfig.SkillMaxLevel + 1];
		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			GD.PushError($"Missing skill XP table: {path}");
			return table;
		}

		file.GetLine();
		while (!file.EofReached())
		{
			var line = file.GetLine();
			if (string.IsNullOrWhiteSpace(line))
			{
				continue;
			}

			var p = line.Split(',');
			if (p.Length < 2
				|| !int.TryParse(p[0].Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var level)
				|| !int.TryParse(p[1].Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var xp))
			{
				continue;
			}

			if (level is >= 1 and <= GameConfig.SkillMaxLevel)
			{
				table[level] = xp;
			}
		}

		return table;
	}
}
