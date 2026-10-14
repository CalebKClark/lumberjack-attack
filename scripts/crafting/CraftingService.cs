using System.Collections.Generic;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;

namespace LumberjackAttack.Crafting;

/// <summary>Recipe unlock checks + crafting (Java CraftingService).</summary>
public sealed class CraftingService
{
	private readonly IReadOnlyList<RecipeDef> _recipes;
	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private readonly HashSet<string> _notifiedRecipeIds = new();

	public IReadOnlyList<RecipeDef> Recipes => _recipes;

	public CraftingService(IReadOnlyList<RecipeDef> recipes, IReadOnlyDictionary<string, ItemDef> items)
	{
		_recipes = recipes;
		_items = items;
	}

	public bool IsRecipeUnlocked(RecipeDef recipe, PlayerDiscoveries discoveries)
	{
		if (!string.IsNullOrEmpty(recipe.UnlockItemId))
		{
			return discoveries.HasDiscovered(recipe.UnlockItemId);
		}

		foreach (var ing in recipe.Ingredients)
		{
			if (!discoveries.HasDiscovered(ing.ItemId))
			{
				return false;
			}
		}

		return true;
	}

	public bool CanCraft(RecipeDef recipe, PlayerInventory inventory, PlayerDiscoveries discoveries)
	{
		if (!IsRecipeUnlocked(recipe, discoveries) || !_items.ContainsKey(recipe.OutputItemId))
		{
			return false;
		}

		foreach (var ing in recipe.Ingredients)
		{
			if (inventory.CountItem(ing.ItemId) < ing.Quantity)
			{
				return false;
			}
		}

		return inventory.CanAddAfterRemoving(recipe.OutputItemId, recipe.OutputQuantity, recipe.Ingredients);
	}

	public bool TryCraft(RecipeDef recipe, PlayerInventory inventory, PlayerDiscoveries discoveries)
	{
		if (!CanCraft(recipe, inventory, discoveries))
		{
			return false;
		}

		foreach (var ing in recipe.Ingredients)
		{
			inventory.RemoveItem(ing.ItemId, ing.Quantity);
		}

		inventory.AddItem(recipe.OutputItemId, recipe.OutputQuantity);
		return true;
	}

	/// <summary>Toast newly unlocked recipes; returns display names that just unlocked.</summary>
	public List<string> CheckForNewUnlocks(PlayerDiscoveries discoveries)
	{
		var newly = new List<string>();
		foreach (var recipe in _recipes)
		{
			if (_notifiedRecipeIds.Contains(recipe.Id) || !IsRecipeUnlocked(recipe, discoveries))
			{
				continue;
			}

			_notifiedRecipeIds.Add(recipe.Id);
			newly.Add(recipe.DisplayName);
		}

		return newly;
	}

	public void SyncNotifiedUnlocks(PlayerDiscoveries discoveries)
	{
		_notifiedRecipeIds.Clear();
		foreach (var recipe in _recipes)
		{
			if (IsRecipeUnlocked(recipe, discoveries))
			{
				_notifiedRecipeIds.Add(recipe.Id);
			}
		}
	}

	public void ResetNotifiedUnlocks() => _notifiedRecipeIds.Clear();
}
