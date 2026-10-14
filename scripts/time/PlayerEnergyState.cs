using System;
using LumberjackAttack.Core;

namespace LumberjackAttack.Time;

public sealed class PlayerEnergyState
{
	public int MaxEnergy { get; private set; } = GameConfig.BaseMaxEnergy;
	public double CurrentEnergy { get; private set; } = GameConfig.BaseMaxEnergy;

	public bool TrySpend(int amount)
	{
		if (amount <= 0)
		{
			return true;
		}

		if (CurrentEnergy < amount)
		{
			return false;
		}

		CurrentEnergy -= amount;
		return true;
	}

	public void ResetToFull() => CurrentEnergy = MaxEnergy;

	public void Restore(double amount)
	{
		if (amount <= 0)
		{
			return;
		}

		CurrentEnergy = Math.Min(MaxEnergy, CurrentEnergy + amount);
	}

	public void SetCurrentEnergy(double energy) =>
		CurrentEnergy = Math.Clamp(energy, 0, MaxEnergy);

	public bool IsLowEnergy => CurrentEnergy < MaxEnergy * GameConfig.LowEnergySpeedThreshold;

	public double MoveSpeedMultiplier =>
		IsLowEnergy ? GameConfig.LowEnergySpeedMultiplier : 1.0;
}
