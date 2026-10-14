using System;
using LumberjackAttack.Core;

namespace LumberjackAttack.Time;

/// <summary>Player HP. Max health can grow from held gear via <see cref="SetMaxHealth"/>.</summary>
public sealed class PlayerHealthState
{
	public int MaxHealth { get; private set; } = GameConfig.BaseMaxHealth;
	public double CurrentHealth { get; private set; } = GameConfig.BaseMaxHealth;

	public bool IsDead => CurrentHealth <= 0;

	public void ResetToFull() => CurrentHealth = MaxHealth;

	public void SetCurrentHealth(double health) =>
		CurrentHealth = Math.Clamp(health, 0, MaxHealth);

	/// <summary>
	/// Updates max HP from resolved gear stats and clamps current HP into the new range.
	/// Prefer keeping current when max increases; shrink current if max drops.
	/// </summary>
	public void SetMaxHealth(int maxHealth, bool fillToMax = false)
	{
		MaxHealth = Math.Max(1, maxHealth);
		if (fillToMax)
		{
			CurrentHealth = MaxHealth;
			return;
		}

		CurrentHealth = Math.Clamp(CurrentHealth, 0, MaxHealth);
	}

	public void Heal(double amount)
	{
		if (amount <= 0)
		{
			return;
		}

		CurrentHealth = Math.Min(MaxHealth, CurrentHealth + amount);
	}

	/// <summary>Applies raw damage (defense reductions come with combat). Returns true if HP changed.</summary>
	public bool TakeDamage(double rawDamage)
	{
		if (rawDamage <= 0 || CurrentHealth <= 0)
		{
			return false;
		}

		CurrentHealth = Math.Max(0, CurrentHealth - rawDamage);
		return true;
	}
}
