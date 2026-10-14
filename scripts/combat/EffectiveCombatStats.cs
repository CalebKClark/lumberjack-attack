using System;
using System.Collections.Generic;
using LumberjackAttack.Core;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;

namespace LumberjackAttack.Combat;

/// <summary>Base + held-item combat stats (Java EffectiveCombatStats).</summary>
public sealed class EffectiveCombatStats
{
	public int Damage { get; }
	public double CritChance { get; }
	public double CritMultiplier { get; }
	public int MaxHealth { get; }
	public double Defense { get; }

	public int BaseDamage { get; }
	public double BaseCritChance { get; }
	public double BaseCritMultiplier { get; }
	public int BaseMaxHealth { get; }
	public double BaseDefense { get; }

	public StatModifiers Equipment { get; }
	public string? EquipmentName { get; }

	public bool HasEquipmentBonus => Equipment is { IsEmpty: false };

	private EffectiveCombatStats(
		int damage,
		double critChance,
		double critMultiplier,
		int maxHealth,
		double defense,
		StatModifiers equipment,
		string? equipmentName
	)
	{
		Damage = damage;
		CritChance = critChance;
		CritMultiplier = critMultiplier;
		MaxHealth = maxHealth;
		Defense = defense;
		BaseDamage = GameConfig.BaseDamage;
		BaseCritChance = GameConfig.BaseCritChance;
		BaseCritMultiplier = GameConfig.BaseCritMultiplier;
		BaseMaxHealth = GameConfig.BaseMaxHealth;
		BaseDefense = GameConfig.BaseDefense;
		Equipment = equipment;
		EquipmentName = equipmentName;
	}

	public static EffectiveCombatStats Resolve(
		PlayerInventory inventory,
		IReadOnlyDictionary<string, ItemDef> items
	)
	{
		var equipment = StatModifiers.Zero;
		string? equipmentName = null;
		var held = inventory.GetSlot(inventory.SelectedHotbarSlot);
		if (!held.IsEmpty && held.ItemId != null && items.TryGetValue(held.ItemId, out var def))
		{
			equipment = StatModifiers.FromItem(def);
			equipmentName = def.Name;
		}

		var damage = GameConfig.BaseDamage + equipment.BonusDamage;
		var critChance = Math.Clamp(
			GameConfig.BaseCritChance + equipment.BonusCritChance,
			0,
			GameConfig.CritChanceCap
		);
		var critMultiplier = GameConfig.BaseCritMultiplier + equipment.BonusCritMultiplier;
		var maxHealth = GameConfig.BaseMaxHealth + equipment.BonusMaxHealth;
		var defense = Math.Clamp(
			GameConfig.BaseDefense + equipment.BonusDefense,
			0,
			GameConfig.DefenseCap
		);

		return new EffectiveCombatStats(
			damage,
			critChance,
			critMultiplier,
			maxHealth,
			defense,
			equipment,
			equipmentName
		);
	}
}
