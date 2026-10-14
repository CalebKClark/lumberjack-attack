namespace LumberjackAttack.Combat;

/// <summary>Stat bonuses from the held hotbar item (Java StatModifiers).</summary>
public sealed class StatModifiers
{
	public static StatModifiers Zero { get; } = new(0, 0, 0, 0, 0, 0);

	public int BonusDamage { get; }
	public double BonusCritChance { get; }
	public double BonusCritMultiplier { get; }
	public int BonusMaxHealth { get; }
	public double BonusDefense { get; }
	public double BonusFishingSpeed { get; }

	public StatModifiers(
		int bonusDamage,
		double bonusCritChance,
		double bonusCritMultiplier,
		int bonusMaxHealth,
		double bonusDefense,
		double bonusFishingSpeed
	)
	{
		BonusDamage = bonusDamage;
		BonusCritChance = bonusCritChance;
		BonusCritMultiplier = bonusCritMultiplier;
		BonusMaxHealth = bonusMaxHealth;
		BonusDefense = bonusDefense;
		BonusFishingSpeed = bonusFishingSpeed;
	}

	public bool IsEmpty =>
		BonusDamage == 0
		&& BonusCritChance == 0
		&& BonusCritMultiplier == 0
		&& BonusMaxHealth == 0
		&& BonusDefense == 0
		&& BonusFishingSpeed == 0;

	public static StatModifiers FromItem(Items.ItemDef item) => new(
		item.BonusDamage,
		item.BonusCritChance,
		item.BonusCritMultiplier,
		item.BonusMaxHealth,
		item.BonusDefense,
		item.EffectiveFishingSpeedBonus
	);
}
