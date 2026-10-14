using System;

namespace LumberjackAttack.Economy;

/// <summary>Currency balances not held in inventory (Java PlayerBank).</summary>
public sealed class PlayerBank
{
	public const string WoodChipsId = "wood_chips";

	public long WoodChips { get; private set; }

	public void SetWoodChips(long amount) => WoodChips = Math.Max(0, amount);

	public void DepositWoodChips(long amount)
	{
		if (amount > 0)
		{
			WoodChips += amount;
		}
	}

	public void Reset() => WoodChips = 0;
}
