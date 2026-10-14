using System;
using System.Collections.Generic;
using LumberjackAttack.Core;
using LumberjackAttack.Items;
using LumberjackAttack.Save;

namespace LumberjackAttack.Inventory;

/// <summary>Fixed-size storage (chests). Same slot rules as player inventory; no currency.</summary>
public sealed class ItemContainer
{
	public const string ChestItemId = "chest";

	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private readonly ItemStack[] _slots;

	public ItemContainer(IReadOnlyDictionary<string, ItemDef> items, int slotCount)
	{
		_items = items;
		_slots = new ItemStack[slotCount];
		Clear();
	}

	public static ItemContainer CreateChest(IReadOnlyDictionary<string, ItemDef> items) =>
		new(items, GameConfig.ChestSlotCount);

	public int SlotCount => _slots.Length;

	public ItemStack GetSlot(int index) => _slots[index];

	public void SetSlot(int index, ItemStack stack)
	{
		Validate(index);
		if (!stack.IsEmpty && !CanStore(stack.ItemId!))
		{
			return;
		}

		_slots[index] = stack.IsEmpty ? ItemStack.Empty : stack;
	}

	public void Clear()
	{
		for (var i = 0; i < _slots.Length; i++)
		{
			_slots[i] = ItemStack.Empty;
		}
	}

	public bool IsEmpty
	{
		get
		{
			foreach (var stack in _slots)
			{
				if (!stack.IsEmpty)
				{
					return false;
				}
			}

			return true;
		}
	}

	public List<ChestSlotDto> ExportSlots(string mapId, int col, int row)
	{
		var list = new List<ChestSlotDto>();
		for (var i = 0; i < _slots.Length; i++)
		{
			var stack = _slots[i];
			if (stack.IsEmpty)
			{
				continue;
			}

			list.Add(new ChestSlotDto
			{
				MapId = mapId,
				Col = col,
				Row = row,
				Slot = i,
				ItemId = stack.ItemId!,
				Qty = stack.Quantity,
			});
		}

		return list;
	}

	public void ImportSlot(int slot, string itemId, int qty)
	{
		if (slot < 0 || slot >= _slots.Length || qty <= 0 || !_items.ContainsKey(itemId))
		{
			return;
		}

		if (!CanStore(itemId))
		{
			return;
		}

		_slots[slot] = ItemStack.Of(itemId, qty);
	}

	private bool CanStore(string itemId) =>
		_items.TryGetValue(itemId, out var def) && !def.IsCurrency;

	private void Validate(int index)
	{
		if (index < 0 || index >= _slots.Length)
		{
			throw new ArgumentOutOfRangeException(nameof(index));
		}
	}
}
