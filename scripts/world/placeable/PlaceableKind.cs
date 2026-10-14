using LumberjackAttack.Inventory;

namespace LumberjackAttack.World.Placeable;

/// <summary>
/// Placeable behavior family from items.csv <c>placeable_kind</c>.
/// Add new kinds here + a handler — avoid hardcoding item ids in PlaceService.
/// </summary>
public enum PlaceableKind
{
	None,
	/// <summary>Opens storage UI (chest).</summary>
	Chest,
	/// <summary>Processing machine UI (wood chipper today; furnace later).</summary>
	Machine,
	/// <summary>Two-tile sleep furniture.</summary>
	Bed,
	/// <summary>Placeable with no special UI yet.</summary>
	Generic,
}

public static class PlaceableKindParser
{
	public static PlaceableKind Parse(string? raw, string itemId, bool isPlaceableCategory)
	{
		var key = (raw ?? "").Trim().ToLowerInvariant();
		if (!string.IsNullOrEmpty(key))
		{
			return key switch
			{
				"chest" => PlaceableKind.Chest,
				"machine" or "chipper" => PlaceableKind.Machine,
				"bed" => PlaceableKind.Bed,
				"generic" or "prop" => PlaceableKind.Generic,
				"none" or "" => PlaceableKind.None,
				_ => PlaceableKind.Generic,
			};
		}

		// Legacy fallback when CSV column is missing — keep old saves/content working.
		if (!isPlaceableCategory)
		{
			return PlaceableKind.None;
		}

		return itemId switch
		{
			ItemContainer.ChestItemId => PlaceableKind.Chest,
			WoodChipperMachine.MachineItemId => PlaceableKind.Machine,
			BedPlaceable.ItemId => PlaceableKind.Bed,
			_ => PlaceableKind.Generic,
		};
	}
}
