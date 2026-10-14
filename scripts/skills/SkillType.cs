namespace LumberjackAttack.Skills;

public enum SkillType
{
	Foraging,
	Combat,
	Fishing,
}

public static class SkillTypeExtensions
{
	public static string DisplayName(this SkillType skill) => skill switch
	{
		SkillType.Foraging => "Foraging",
		SkillType.Combat => "Combat",
		SkillType.Fishing => "Fishing",
		_ => skill.ToString(),
	};

	public static string Id(this SkillType skill) => skill.ToString().ToLowerInvariant();

	public static SkillType? FromId(string? id)
	{
		if (string.IsNullOrWhiteSpace(id))
		{
			return null;
		}

		return id.Trim().ToLowerInvariant() switch
		{
			"foraging" => SkillType.Foraging,
			"combat" => SkillType.Combat,
			"fishing" => SkillType.Fishing,
			_ => null,
		};
	}
}
