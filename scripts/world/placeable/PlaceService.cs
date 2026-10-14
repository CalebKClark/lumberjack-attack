using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Entities;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;
using LumberjackAttack.World;

namespace LumberjackAttack.World.Placeable;

/// <summary>Place furniture from hotbar; open chest / machine UIs under cursor.</summary>
public sealed class PlaceService
{
	private readonly TileMapLayer _ground;
	private readonly IReadOnlyDictionary<int, TileDef> _tiles;
	private readonly IReadOnlyDictionary<string, ItemDef> _items;
	private readonly PlayerInventory _inventory;
	private readonly PlaceableLayer _placeables;
	private readonly System.Func<string> _mapId;

	public PlaceService(
		TileMapLayer ground,
		IReadOnlyDictionary<int, TileDef> tiles,
		IReadOnlyDictionary<string, ItemDef> items,
		PlayerInventory inventory,
		PlaceableLayer placeables,
		System.Func<string> mapId
	)
	{
		_ground = ground;
		_tiles = tiles;
		_items = items;
		_inventory = inventory;
		_placeables = placeables;
		_mapId = mapId;
	}

	public bool TryPlaceAt(PlayerController player, Vector2 worldClick, out string toast)
	{
		toast = "";
		if (!TryGetHeldPlaceable(out var itemId, out var kind))
		{
			return false;
		}

		var cell = _ground.LocalToMap(_ground.ToLocal(worldClick));
		if (kind == PlaceableKind.Bed)
		{
			return TryPlaceBed(player, cell.X, cell.Y, itemId, out toast);
		}

		if (kind is not (PlaceableKind.Chest or PlaceableKind.Machine or PlaceableKind.Generic))
		{
			toast = "Can't place that yet";
			return true;
		}

		var tileId = _ground.GetCellSourceId(cell);
		if (!_tiles.TryGetValue(tileId, out var tile) || !tile.Placeable)
		{
			toast = "Place on cabin floor";
			return true;
		}

		var mapId = _mapId();
		if (_placeables.HasAt(mapId, cell.X, cell.Y))
		{
			toast = "Tile occupied";
			return true;
		}

		var tileCenter = new Vector2(
			cell.X * GameConfig.TileSize + GameConfig.TileSize / 2f,
			cell.Y * GameConfig.TileSize + GameConfig.TileSize / 2f
		);
		var playerCenter = player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		if (playerCenter.DistanceTo(tileCenter) > GameConfig.ChopReachDistance)
		{
			toast = "Too far to place";
			return true;
		}

		if (PlayerOverlapsPlacement(player, cell.X, cell.Y))
		{
			toast = "Stand clear of the tile";
			return true;
		}

		_inventory.ConsumeOneFromSelectedHotbar();
		_placeables.Place(mapId, itemId, cell.X, cell.Y);
		toast = kind switch
		{
			PlaceableKind.Chest => "Placed chest",
			PlaceableKind.Machine => "Placed wood chipper",
			_ => "Placed",
		};
		return true;
	}

	private bool TryPlaceBed(PlayerController player, int footCol, int footRow, string itemId, out string toast)
	{
		toast = "";
		if (!BedPlaceable.CanPlaceAt(_ground, _tiles, footCol, footRow))
		{
			toast = "Need two cabin floor tiles";
			return true;
		}

		var mapId = _mapId();
		var headRow = BedPlaceable.HeadRow(footRow);
		if (_placeables.HasAt(mapId, footCol, footRow) || _placeables.HasAt(mapId, footCol, headRow))
		{
			toast = "Tile occupied";
			return true;
		}

		var tileCenter = new Vector2(
			footCol * GameConfig.TileSize + GameConfig.TileSize / 2f,
			(footRow * GameConfig.TileSize + headRow * GameConfig.TileSize + GameConfig.TileSize) / 2f
		);
		var playerCenter = player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		if (playerCenter.DistanceTo(tileCenter) > GameConfig.ChopReachDistance)
		{
			toast = "Too far to place";
			return true;
		}

		if (PlayerOverlapsBed(player, footCol, footRow))
		{
			toast = "Stand clear of the bed";
			return true;
		}

		_inventory.ConsumeOneFromSelectedHotbar();
		_placeables.Place(mapId, itemId, footCol, footRow);
		toast = "Placed bed";
		return true;
	}

	public bool TryOpenChest(PlayerController player, Vector2 worldClick, out ItemContainer? chest, out string toast)
	{
		chest = null;
		toast = "";
		var mapId = _mapId();
		var placed = _placeables.FindAtWorld(mapId, worldClick);
		if (placed == null || KindOf(placed.ItemId) != PlaceableKind.Chest)
		{
			return false;
		}

		if (!InReach(player, placed.Col, placed.Row))
		{
			toast = "Too far";
			return true;
		}

		chest = _placeables.GetOrCreateChest(new PlacedObjectKey(mapId, placed.Col, placed.Row));
		return true;
	}

	public bool TryOpenChipper(
		PlayerController player,
		Vector2 worldClick,
		out WoodChipperMachine? machine,
		out string toast
	)
	{
		machine = null;
		toast = "";
		var mapId = _mapId();
		var placed = _placeables.FindAtWorld(mapId, worldClick);
		if (placed == null || KindOf(placed.ItemId) != PlaceableKind.Machine)
		{
			return false;
		}

		if (!InReach(player, placed.Col, placed.Row))
		{
			toast = "Too far";
			return true;
		}

		var key = new PlacedObjectKey(mapId, placed.Col, placed.Row);
		machine = _placeables.GetOrCreateChipper(key);
		return true;
	}

	private bool TryGetHeldPlaceable(out string itemId, out PlaceableKind kind)
	{
		itemId = "";
		kind = PlaceableKind.None;
		var stack = _inventory.GetSelectedHotbarStack();
		if (stack.IsEmpty || !_items.TryGetValue(stack.ItemId!, out var def) || !def.IsPlaceable)
		{
			return false;
		}

		itemId = stack.ItemId!;
		kind = def.PlaceableKind;
		return kind != PlaceableKind.None;
	}

	private PlaceableKind KindOf(string itemId) =>
		_items.TryGetValue(itemId, out var def)
			? def.PlaceableKind
			: PlaceableKindParser.Parse(null, itemId, isPlaceableCategory: true);

	private static bool InReach(PlayerController player, int col, int row)
	{
		var tileCenter = new Vector2(
			col * GameConfig.TileSize + GameConfig.TileSize / 2f,
			row * GameConfig.TileSize + GameConfig.TileSize / 2f
		);
		var playerCenter = player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		return playerCenter.DistanceTo(tileCenter) <= GameConfig.ChopReachDistance;
	}

	private static bool PlayerOverlapsPlacement(PlayerController player, int col, int row)
	{
		var size = GameConfig.PlaceableHitboxSize;
		var pad = (GameConfig.TileSize - size) / 2f;
		var placeRect = new Rect2(
			col * GameConfig.TileSize + pad,
			row * GameConfig.TileSize + pad,
			size,
			size
		);
		return placeRect.Intersects(PlayerRect(player));
	}

	private static bool PlayerOverlapsBed(PlayerController player, int footCol, int footRow)
	{
		var bedRect = new Rect2(
			footCol * GameConfig.TileSize,
			BedPlaceable.HeadRow(footRow) * GameConfig.TileSize,
			BedPlaceable.SpriteWidth,
			BedPlaceable.SpriteHeight
		);
		return bedRect.Intersects(PlayerRect(player));
	}

	private static Rect2 PlayerRect(PlayerController player)
	{
		var halfW = GameConfig.PlayerHitboxWidth / 2f;
		return new Rect2(
			player.GlobalPosition.X - halfW,
			player.GlobalPosition.Y - GameConfig.PlayerHitboxHeight,
			GameConfig.PlayerHitboxWidth,
			GameConfig.PlayerHitboxHeight
		);
	}
}
