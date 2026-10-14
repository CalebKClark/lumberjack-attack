using System;
using System.Collections.Generic;
using LumberjackAttack.Economy;
using LumberjackAttack.Items;

namespace LumberjackAttack.Inventory;

/// <summary>Minecraft-style carried stack + left/right click transfers (Java StackTransfer).</summary>
public sealed class InventoryCursor
{
	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private ItemStack _carried = ItemStack.Empty;

	public InventoryCursor(IReadOnlyDictionary<string, ItemDef> items)
	{
		_items = items;
	}

	public ItemStack Carried => _carried;
	public bool IsCarrying => !_carried.IsEmpty;

	public void Clear() => _carried = ItemStack.Empty;

	public void SetCarried(ItemStack stack) =>
		_carried = stack.IsEmpty ? ItemStack.Empty : stack;

	public void HandleClick(PlayerInventory inventory, int slotIndex, bool rightClick)
	{
		var slot = inventory.GetSlot(slotIndex);
		var updated = rightClick ? RightClick(slot) : LeftClick(slot);
		inventory.SetSlot(slotIndex, updated);
	}

	public void HandleClick(ItemContainer container, int slotIndex, bool rightClick)
	{
		var beforeCarried = _carried;
		var slot = container.GetSlot(slotIndex);
		var updated = rightClick ? RightClick(slot) : LeftClick(slot);
		if (!updated.IsEmpty && IsCurrency(updated.ItemId!))
		{
			_carried = beforeCarried;
			return;
		}

		container.SetSlot(slotIndex, updated);
	}

	/// <summary>Left/right click on a slot that only accepts one item id (chipper input).</summary>
	public ItemStack HandleFilteredClick(ItemStack slot, string allowedItemId, bool rightClick)
	{
		if (IsCarrying && _carried.ItemId != allowedItemId)
		{
			return slot;
		}

		if (!slot.IsEmpty && slot.ItemId != allowedItemId)
		{
			return slot;
		}

		return rightClick ? RightClick(slot) : LeftClick(slot);
	}

	/// <summary>Output-only: take items onto cursor; placing into the slot is blocked.</summary>
	public ItemStack HandleTakeOnlyClick(ItemStack slot, bool rightClick)
	{
		if (slot.IsEmpty)
		{
			return slot;
		}

		if (IsCarrying)
		{
			if (_carried.ItemId != slot.ItemId || !_items.TryGetValue(slot.ItemId!, out var def))
			{
				return slot;
			}

			var space = def.MaxStack - _carried.Quantity;
			if (space <= 0)
			{
				return slot;
			}

			var take = rightClick ? Math.Min(1, slot.Quantity) : Math.Min(space, slot.Quantity);
			_carried = ItemStack.Of(slot.ItemId!, _carried.Quantity + take);
			return slot.Quantity <= take
				? ItemStack.Empty
				: ItemStack.Of(slot.ItemId!, slot.Quantity - take);
		}

		if (rightClick)
		{
			_carried = ItemStack.Of(slot.ItemId!, 1);
			return slot.Quantity <= 1 ? ItemStack.Empty : ItemStack.Of(slot.ItemId!, slot.Quantity - 1);
		}

		_carried = slot;
		return ItemStack.Empty;
	}

	private bool IsCurrency(string itemId) =>
		_items.TryGetValue(itemId, out var def) && def.IsCurrency;

	/// <returns>True if cursor is empty afterward.</returns>
	public bool StowInto(PlayerInventory inventory, PlayerBank? bank = null)
	{
		if (!IsCarrying)
		{
			return true;
		}

		var itemId = _carried.ItemId!;
		var quantity = _carried.Quantity;

		if (bank != null && IsCurrency(itemId))
		{
			if (itemId == PlayerBank.WoodChipsId)
			{
				bank.DepositWoodChips(quantity);
			}

			Clear();
			return true;
		}

		var remaining = inventory.AddItem(itemId, quantity);
		if (remaining <= 0)
		{
			Clear();
			return true;
		}

		_carried = ItemStack.Of(itemId, remaining);
		return false;
	}

	private ItemStack LeftClick(ItemStack slot)
	{
		if (!IsCarrying)
		{
			if (slot.IsEmpty)
			{
				return slot;
			}

			_carried = slot;
			return ItemStack.Empty;
		}

		if (slot.IsEmpty)
		{
			var placed = _carried;
			Clear();
			return placed;
		}

		if (slot.ItemId == _carried.ItemId)
		{
			return Merge(slot, _carried.Quantity);
		}

		var swap = slot;
		var place = _carried;
		_carried = swap;
		return place;
	}

	private ItemStack RightClick(ItemStack slot)
	{
		if (!IsCarrying)
		{
			if (slot.IsEmpty)
			{
				return slot;
			}

			var take = (slot.Quantity + 1) / 2;
			_carried = ItemStack.Of(slot.ItemId!, take);
			var left = slot.Quantity - take;
			return left <= 0 ? ItemStack.Empty : ItemStack.Of(slot.ItemId!, left);
		}

		if (slot.IsEmpty)
		{
			var id = _carried.ItemId!;
			PlaceOne();
			return ItemStack.Of(id, 1);
		}

		if (slot.ItemId == _carried.ItemId)
		{
			if (!_items.TryGetValue(slot.ItemId!, out var def) || slot.Quantity >= def.MaxStack)
			{
				return slot;
			}

			var id = slot.ItemId!;
			PlaceOne();
			return ItemStack.Of(id, slot.Quantity + 1);
		}

		var swap = slot;
		var place = _carried;
		_carried = swap;
		return place;
	}

	private ItemStack Merge(ItemStack slot, int moveRequested)
	{
		if (!_items.TryGetValue(slot.ItemId!, out var def))
		{
			return slot;
		}

		var space = def.MaxStack - slot.Quantity;
		if (space <= 0)
		{
			return slot;
		}

		var moved = Math.Min(space, moveRequested);
		var newQty = _carried.Quantity - moved;
		_carried = newQty <= 0 ? ItemStack.Empty : ItemStack.Of(_carried.ItemId!, newQty);
		return ItemStack.Of(slot.ItemId!, slot.Quantity + moved);
	}

	private void PlaceOne()
	{
		if (_carried.Quantity <= 1)
		{
			Clear();
			return;
		}

		_carried = ItemStack.Of(_carried.ItemId!, _carried.Quantity - 1);
	}
}
