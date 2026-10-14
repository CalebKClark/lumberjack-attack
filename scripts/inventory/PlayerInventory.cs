using System;
using System.Collections.Generic;
using System.Globalization;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Crafting;
using LumberjackAttack.Items;
using LumberjackAttack.Save;

namespace LumberjackAttack.Inventory;

/// <summary>36-slot inventory; slots 0-8 are the hotbar (Java Inventory).</summary>
public sealed class PlayerInventory
{
	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private readonly ItemStack[] _slots;
	private int _selectedHotbar;

	/// <summary>Fired when AddItem successfully places at least one of an item (discovery / unlocks).</summary>
	public Action<string>? ItemAcquired { get; set; }

	/// <summary>Fired with quantity actually added (feed notifications).</summary>
	public Action<string, int>? ItemsAdded { get; set; }

	/// <summary>When true, ItemsAdded is not fired (load / starter / craft consume).</summary>
	public bool SuppressNotifications { get; set; }

	public PlayerInventory(IReadOnlyDictionary<string, ItemDef> items)
	{
		_items = items;
		_slots = new ItemStack[GameConfig.InventorySlots];
		for (var i = 0; i < _slots.Length; i++)
		{
			_slots[i] = ItemStack.Empty;
		}
	}

	public int SelectedHotbarSlot => _selectedHotbar;

	public void SelectHotbarSlot(int slot)
	{
		if (slot < 0 || slot >= GameConfig.HotbarSlots)
		{
			return;
		}

		_selectedHotbar = slot;
	}

	public ItemStack GetSlot(int index) => _slots[index];

	public void SetSlot(int index, ItemStack stack)
	{
		if (!stack.IsEmpty && _items.TryGetValue(stack.ItemId!, out var def) && def.IsCurrency)
		{
			return;
		}

		_slots[index] = stack.IsEmpty ? ItemStack.Empty : stack;
	}

	public ItemStack GetSelectedHotbarStack() => _slots[_selectedHotbar];

	public void ConsumeOneFromSelectedHotbar()
	{
		var stack = _slots[_selectedHotbar];
		if (stack.IsEmpty)
		{
			return;
		}

		_slots[_selectedHotbar] = stack.Quantity <= 1
			? ItemStack.Empty
			: stack.WithQuantity(stack.Quantity - 1);
	}

	public ItemDef? GetHeldAxe()
	{
		var stack = GetSelectedHotbarStack();
		if (stack.IsEmpty || !_items.TryGetValue(stack.ItemId!, out var def))
		{
			return null;
		}

		return def.ToolType == ToolType.Axe ? def : null;
	}

	public ItemDef? GetHeldRod()
	{
		var stack = GetSelectedHotbarStack();
		if (stack.IsEmpty || !_items.TryGetValue(stack.ItemId!, out var def))
		{
			return null;
		}

		return def.ToolType == ToolType.FishingRod ? def : null;
	}

	public void ApplyStarterLoadout(string path = "res://data/starter_loadout.csv")
	{
		for (var i = 0; i < _slots.Length; i++)
		{
			_slots[i] = ItemStack.Empty;
		}

		_selectedHotbar = 0;
		using var file = Godot.FileAccess.Open(path, Godot.FileAccess.ModeFlags.Read);
		if (file == null)
		{
			_slots[0] = ItemStack.Of("starter_axe", 1);
			return;
		}

		file.GetLine();
		var minSlot = int.MaxValue;
		while (!file.EofReached())
		{
			var line = file.GetLine();
			if (string.IsNullOrWhiteSpace(line))
			{
				continue;
			}

			var p = line.Split(',');
			if (p.Length < 3)
			{
				continue;
			}

			if (!int.TryParse(p[0].Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var slot))
			{
				continue;
			}

			var itemId = p[1].Trim();
			if (!int.TryParse(p[2].Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var qty) || qty <= 0)
			{
				continue;
			}

			if (slot < 0 || slot >= _slots.Length || !_items.ContainsKey(itemId))
			{
				continue;
			}

			_slots[slot] = ItemStack.Of(itemId, qty);
			minSlot = Math.Min(minSlot, slot);
		}

		if (minSlot < GameConfig.HotbarSlots)
		{
			_selectedHotbar = minSlot;
		}
	}

	/// <returns>Quantity that did not fit. Currency items are rejected (use PlayerBank).</returns>
	public int AddItem(string itemId, int quantity)
	{
		if (quantity <= 0 || !_items.TryGetValue(itemId, out var def) || def.IsCurrency)
		{
			return quantity;
		}

		var remaining = quantity;
		var maxStack = def.MaxStack;

		for (var i = 0; i < _slots.Length && remaining > 0; i++)
		{
			var stack = _slots[i];
			if (stack.IsEmpty || stack.ItemId != itemId)
			{
				continue;
			}

			var space = maxStack - stack.Quantity;
			if (space <= 0)
			{
				continue;
			}

			var add = Math.Min(space, remaining);
			_slots[i] = stack.WithQuantity(stack.Quantity + add);
			remaining -= add;
		}

		for (var i = 0; i < _slots.Length && remaining > 0; i++)
		{
			if (!_slots[i].IsEmpty)
			{
				continue;
			}

			var add = Math.Min(maxStack, remaining);
			_slots[i] = ItemStack.Of(itemId, add);
			remaining -= add;
		}

		if (remaining < quantity)
		{
			var added = quantity - remaining;
			ItemAcquired?.Invoke(itemId);
			if (!SuppressNotifications)
			{
				ItemsAdded?.Invoke(itemId, added);
			}
		}

		return remaining;
	}

	public int CountItem(string itemId)
	{
		var total = 0;
		foreach (var stack in _slots)
		{
			if (!stack.IsEmpty && stack.ItemId == itemId)
			{
				total += stack.Quantity;
			}
		}

		return total;
	}

	public void ClearAllSlots()
	{
		for (var i = 0; i < _slots.Length; i++)
		{
			_slots[i] = ItemStack.Empty;
		}

		_selectedHotbar = 0;
	}

	public List<InventorySlotDto> ExportSlots()
	{
		var list = new List<InventorySlotDto>();
		for (var i = 0; i < _slots.Length; i++)
		{
			var stack = _slots[i];
			if (stack.IsEmpty)
			{
				continue;
			}

			list.Add(new InventorySlotDto
			{
				Slot = i,
				ItemId = stack.ItemId!,
				Qty = stack.Quantity,
			});
		}

		return list;
	}

	public void ImportSlots(IEnumerable<InventorySlotDto> slots, int selectedHotbar)
	{
		ClearAllSlots();
		foreach (var entry in slots)
		{
			if (entry.Slot < 0 || entry.Slot >= _slots.Length || entry.Qty <= 0)
			{
				continue;
			}

			if (!_items.ContainsKey(entry.ItemId))
			{
				continue;
			}

			_slots[entry.Slot] = ItemStack.Of(entry.ItemId, entry.Qty);
		}

		SelectHotbarSlot(selectedHotbar);
	}

	public int RemoveItem(string itemId, int quantity)
	{
		if (quantity <= 0 || string.IsNullOrEmpty(itemId))
		{
			return 0;
		}

		var remaining = quantity;
		for (var i = 0; i < _slots.Length && remaining > 0; i++)
		{
			var stack = _slots[i];
			if (stack.IsEmpty || stack.ItemId != itemId)
			{
				continue;
			}

			var take = Math.Min(stack.Quantity, remaining);
			_slots[i] = stack.Quantity <= take
				? ItemStack.Empty
				: stack.WithQuantity(stack.Quantity - take);
			remaining -= take;
		}

		return quantity - remaining;
	}

	/// <summary>Removes every stack of itemId; returns total removed.</summary>
	public int RemoveAllOfItem(string itemId) => RemoveItem(itemId, int.MaxValue);

	/// <summary>True if output fits after ingredients are removed (simple simulation).</summary>
	public bool CanAddAfterRemoving(
		string outputItemId,
		int outputQuantity,
		IReadOnlyList<RecipeIngredient> toRemove
	)
	{
		if (!_items.TryGetValue(outputItemId, out var outDef))
		{
			return false;
		}

		var sim = new ItemStack[_slots.Length];
		for (var i = 0; i < _slots.Length; i++)
		{
			sim[i] = _slots[i];
		}

		foreach (var ing in toRemove)
		{
			var need = ing.Quantity;
			for (var i = 0; i < sim.Length && need > 0; i++)
			{
				if (sim[i].IsEmpty || sim[i].ItemId != ing.ItemId)
				{
					continue;
				}

				var take = Math.Min(sim[i].Quantity, need);
				var left = sim[i].Quantity - take;
				sim[i] = left <= 0 ? ItemStack.Empty : ItemStack.Of(ing.ItemId, left);
				need -= take;
			}

			if (need > 0)
			{
				return false;
			}
		}

		var remaining = outputQuantity;
		var maxStack = outDef.MaxStack;
		for (var i = 0; i < sim.Length && remaining > 0; i++)
		{
			if (sim[i].IsEmpty || sim[i].ItemId != outputItemId)
			{
				continue;
			}

			var space = maxStack - sim[i].Quantity;
			if (space > 0)
			{
				remaining -= Math.Min(space, remaining);
			}
		}

		for (var i = 0; i < sim.Length && remaining > 0; i++)
		{
			if (sim[i].IsEmpty)
			{
				remaining -= Math.Min(maxStack, remaining);
			}
		}

		return remaining <= 0;
	}
}
