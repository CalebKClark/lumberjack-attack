using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.World;

namespace LumberjackAttack.World.Placeable;

/// <summary>Two-tile bed placeable — foot (south) is the anchor; head is row - 1 (Java BedPlaceable).</summary>
public static class BedPlaceable
{
	public const string ItemId = "bed";
	public const int HeightTiles = 2;
	public const int SpriteWidth = GameConfig.TileSize;
	public const int SpriteHeight = GameConfig.TileSize * HeightTiles;

	public static bool IsBed(string? itemId) => itemId == ItemId;

	public static int HeadRow(int footRow) => footRow - 1;

	public static int FootRow(int headRow) => headRow + 1;

	public static bool Occupies(int footCol, int footRow, int col, int row) =>
		col == footCol && (row == footRow || row == HeadRow(footRow));

	public static bool CanPlaceAt(
		TileMapLayer ground,
		System.Collections.Generic.IReadOnlyDictionary<int, TileDef> tiles,
		int footCol,
		int footRow
	)
	{
		var head = HeadRow(footRow);
		var footCell = new Godot.Vector2I(footCol, footRow);
		var headCell = new Godot.Vector2I(footCol, head);
		var used = ground.GetUsedRect();
		if (!used.HasPoint(footCell) || !used.HasPoint(headCell))
		{
			return false;
		}

		return IsCabinFloor(tiles, ground.GetCellSourceId(footCell))
			&& IsCabinFloor(tiles, ground.GetCellSourceId(headCell));
	}

	public static bool IsCabinFloor(
		System.Collections.Generic.IReadOnlyDictionary<int, TileDef> tiles,
		int tileId
	) =>
		tiles.TryGetValue(tileId, out var def) && def.Name == "cabin_floor";
}
