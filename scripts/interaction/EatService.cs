using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;
using LumberjackAttack.Time;
using LumberjackAttack.Ui;

namespace LumberjackAttack.Interaction;

/// <summary>Right-click while holding edible food → Yes/No → restore HP/energy (Java EatService).</summary>
public sealed class EatService
{
	private readonly PlayerInventory _inventory;
	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private readonly PlayerEnergyState _energy;
	private readonly PlayerHealthState _health;
	private readonly Action _onChanged;
	private readonly ConfirmationDialog _dialog;
	private bool _promptOpen;
	private string? _pendingItemId;

	public bool IsPromptOpen => _promptOpen;

	public EatService(
		PlayerInventory inventory,
		IReadOnlyDictionary<string, ItemDef> items,
		PlayerEnergyState energy,
		PlayerHealthState health,
		Node host,
		Action onChanged
	)
	{
		_inventory = inventory;
		_items = items;
		_energy = energy;
		_health = health;
		_onChanged = onChanged;

		_dialog = new ConfirmationDialog
		{
			Title = "Eat",
			DialogText = "Eat this?",
			OkButtonText = "Yes",
			CancelButtonText = "No",
		};
		host.AddChild(_dialog);
		_dialog.Confirmed += OnConfirmed;
		_dialog.Canceled += OnCanceled;
		GameFont.Apply(_dialog);
		MenuChrome.ApplyDialog(_dialog);
	}

	/// <summary>Right-click while holding edible food → Yes/No → restore HP/energy.</summary>
	public bool TryBeginOnClick()
	{
		if (_promptOpen)
		{
			return true;
		}

		var held = _inventory.GetSelectedHotbarStack();
		if (held.IsEmpty || !_items.TryGetValue(held.ItemId!, out var item) || !item.IsEdible)
		{
			return false;
		}

		_pendingItemId = item.Id;
		_dialog.DialogText = $"Eat \"{item.Name}\"?\n(+{item.FoodHealth} HP, +{item.FoodEnergy} energy)";
		_promptOpen = true;
		_dialog.PopupCentered();
		return true;
	}

	public void Cancel()
	{
		if (!_promptOpen)
		{
			return;
		}

		_dialog.Hide();
		_promptOpen = false;
		_pendingItemId = null;
	}

	private void OnConfirmed()
	{
		_promptOpen = false;
		var itemId = _pendingItemId;
		_pendingItemId = null;
		if (string.IsNullOrEmpty(itemId) || !_items.TryGetValue(itemId, out var item))
		{
			return;
		}

		var slot = _inventory.SelectedHotbarSlot;
		var held = _inventory.GetSlot(slot);
		if (held.IsEmpty || held.ItemId != item.Id)
		{
			return;
		}

		if (item.FoodHealth > 0)
		{
			_health.Heal(item.FoodHealth);
		}

		if (item.FoodEnergy > 0)
		{
			_energy.Restore(item.FoodEnergy);
		}

		_inventory.SetSlot(slot, held.Quantity <= 1 ? ItemStack.Empty : held.WithQuantity(held.Quantity - 1));
		_onChanged();
		GD.Print($"Ate {item.Name} (+{item.FoodHealth} HP, +{item.FoodEnergy} energy)");
	}

	private void OnCanceled()
	{
		_promptOpen = false;
		_pendingItemId = null;
	}
}
