using System;
using System.Collections.Generic;
using LumberjackAttack.Save;

namespace LumberjackAttack.Skills;

/// <summary>Tracks XP/levels for foraging, combat, fishing (Java PlayerSkills).</summary>
public sealed class PlayerSkills
{
	private readonly Dictionary<SkillType, int> _totalXp = new();

	public Action<SkillType, int>? XpGained { get; set; }
	public Action<SkillType, int>? LevelUp { get; set; }

	public PlayerSkills() => Reset();

	public void Reset()
	{
		_totalXp.Clear();
		foreach (SkillType skill in Enum.GetValues(typeof(SkillType)))
		{
			_totalXp[skill] = 0;
		}
	}

	public int GetTotalXp(SkillType skill) =>
		_totalXp.TryGetValue(skill, out var xp) ? xp : 0;

	public int GetLevel(SkillType skill) => SkillXpTable.ResolveLevel(GetTotalXp(skill));

	public int GetXpToNextLevel(SkillType skill) => SkillXpTable.XpToNextLevel(GetTotalXp(skill));

	public float GetProgressInLevel(SkillType skill) => SkillXpTable.ProgressInLevel(GetTotalXp(skill));

	public void GrantXp(SkillType skill, int amount)
	{
		if (amount <= 0 || GetLevel(skill) >= SkillXpTable.MaxLevel)
		{
			return;
		}

		var previousLevel = GetLevel(skill);
		_totalXp[skill] = GetTotalXp(skill) + amount;
		var newLevel = GetLevel(skill);

		XpGained?.Invoke(skill, amount);
		if (newLevel > previousLevel)
		{
			LevelUp?.Invoke(skill, newLevel);
		}
	}

	public List<SkillDto> Export()
	{
		var list = new List<SkillDto>();
		foreach (SkillType skill in Enum.GetValues(typeof(SkillType)))
		{
			list.Add(new SkillDto { SkillId = skill.Id(), TotalXp = GetTotalXp(skill) });
		}

		return list;
	}

	public void Import(IEnumerable<SkillDto>? entries)
	{
		Reset();
		if (entries == null)
		{
			return;
		}

		foreach (var entry in entries)
		{
			var skill = SkillTypeExtensions.FromId(entry.SkillId);
			if (skill == null)
			{
				continue;
			}

			_totalXp[skill.Value] = Math.Max(0, entry.TotalXp);
		}
	}
}
