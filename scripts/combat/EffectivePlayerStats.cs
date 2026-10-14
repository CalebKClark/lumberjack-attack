using System;
using System.Collections.Generic;
using LumberjackAttack.Core;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;

namespace LumberjackAttack.Combat;

/// <summary>Combat + fishing effective stats (Java EffectivePlayerStats).</summary>
public sealed class EffectivePlayerStats
{
	public EffectiveCombatStats Combat { get; }
	public double FishingSpeed { get; }
	public double BiteWaitMinMs { get; }
	public double BiteWaitMaxMs { get; }

	private EffectivePlayerStats(
		EffectiveCombatStats combat,
		double fishingSpeed,
		double biteWaitMinMs,
		double biteWaitMaxMs
	)
	{
		Combat = combat;
		FishingSpeed = fishingSpeed;
		BiteWaitMinMs = biteWaitMinMs;
		BiteWaitMaxMs = biteWaitMaxMs;
	}

	public static EffectivePlayerStats Resolve(
		PlayerInventory inventory,
		IReadOnlyDictionary<string, ItemDef> items
	)
	{
		var combat = EffectiveCombatStats.Resolve(inventory, items);
		var fishingSpeed = ResolveFishingSpeed(inventory, items);
		var capped = Math.Min(fishingSpeed, GameConfig.FishingSpeedCap);
		var range = GameConfig.FishingBiteMaxMs - GameConfig.FishingBiteMinMs;
		var effectiveRange = range * (1.0 - capped / 100.0);
		var biteMin = GameConfig.FishingBiteMinMs;
		var biteMax = GameConfig.FishingBiteMinMs + effectiveRange;
		return new EffectivePlayerStats(combat, fishingSpeed, biteMin, biteMax);
	}

	public static double ResolveFishingSpeed(
		PlayerInventory inventory,
		IReadOnlyDictionary<string, ItemDef> items
	)
	{
		var held = inventory.GetSlot(inventory.SelectedHotbarSlot);
		if (held.IsEmpty || held.ItemId == null || !items.TryGetValue(held.ItemId, out var def))
		{
			return 0;
		}

		return def.EffectiveFishingSpeedBonus;
	}
}
