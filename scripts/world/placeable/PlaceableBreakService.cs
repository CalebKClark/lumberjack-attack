using System;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Entities;
using LumberjackAttack.Inventory;
using LumberjackAttack.Time;
using LumberjackAttack.World;

namespace LumberjackAttack.World.Placeable;

/// <summary>Axe-break placed objects (chest, chipper, bed) — same swing lock as chopping.</summary>
public sealed class PlaceableBreakService
{
	private readonly TileMapLayer _ground;
	private readonly PlayerInventory _inventory;
	private readonly PlayerEnergyState _energy;
	private readonly PlaceableLayer _placeables;
	private readonly LootLayer _loot;
	private readonly OverlayLayer _overlays;
	private readonly TileVisualLayer _tileVisuals;
	private readonly Func<string> _mapId;
	private readonly Action _closeChest;
	private PendingBreak? _pending;

	public PlaceableBreakService(
		TileMapLayer ground,
		PlayerInventory inventory,
		PlayerEnergyState energy,
		PlaceableLayer placeables,
		LootLayer loot,
		OverlayLayer overlays,
		TileVisualLayer tileVisuals,
		Func<string> mapId,
		Action closeChest
	)
	{
		_ground = ground;
		_inventory = inventory;
		_energy = energy;
		_placeables = placeables;
		_loot = loot;
		_overlays = overlays;
		_tileVisuals = tileVisuals;
		_mapId = mapId;
		_closeChest = closeChest;
	}

	/// <summary>Starts axe swing to break a placeable. Toast explains blocked breaks.</summary>
	public bool TryBreakAt(PlayerController player, Vector2 worldClick, out string toast)
	{
		toast = "";
		if (player.IsActionLocked || _pending != null)
		{
			return false;
		}

		if (_inventory.GetHeldAxe() == null)
		{
			return false;
		}

		var mapId = _mapId();
		var placed = _placeables.FindAtWorld(mapId, worldClick);
		if (placed == null)
		{
			return false;
		}

		var tileCenter = BreakTargetCenter(placed);
		var playerCenter = player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		if (playerCenter.DistanceTo(tileCenter) > GameConfig.ChopReachDistance)
		{
			toast = "Too far";
			return true;
		}

		var key = new PlacedObjectKey(mapId, placed.Col, placed.Row);
		if (!IsBreakableWhenEmpty(key, placed.ItemId, out var blockReason))
		{
			toast = blockReason;
			return true;
		}

		if (!_energy.TrySpend(GameConfig.ActionEnergyCost))
		{
			toast = "Too tired";
			return true;
		}

		_pending = new PendingBreak(mapId, placed.Col, placed.Row, placed.ItemId);
		player.FaceToward(tileCenter);
		player.TriggerAxeSwing(OnSwingCompleted);
		return true;
	}

	public void CancelPending() => _pending = null;

	private static Vector2 BreakTargetCenter(PlacedObjectData placed)
	{
		if (BedPlaceable.IsBed(placed.ItemId))
		{
			return new Vector2(
				placed.Col * GameConfig.TileSize + GameConfig.TileSize / 2f,
				BedPlaceable.HeadRow(placed.Row) * GameConfig.TileSize + BedPlaceable.SpriteHeight / 2f
			);
		}

		return new Vector2(
			placed.Col * GameConfig.TileSize + GameConfig.TileSize / 2f,
			placed.Row * GameConfig.TileSize + GameConfig.TileSize / 2f
		);
	}

	private void OnSwingCompleted(bool swingCompleted)
	{
		if (!swingCompleted || _pending == null)
		{
			_pending = null;
			return;
		}

		var pending = _pending;
		_pending = null;
		if (pending.MapId != _mapId())
		{
			return;
		}

		ApplyBreak(pending);
	}

	private void ApplyBreak(PendingBreak pending)
	{
		var key = new PlacedObjectKey(pending.MapId, pending.Col, pending.Row);
		if (!_placeables.HasAt(pending.MapId, pending.Col, pending.Row))
		{
			return;
		}

		if (!IsBreakableWhenEmpty(key, pending.ItemId, out _))
		{
			return;
		}

		_closeChest();

		var removed = _placeables.RemoveAt(pending.MapId, pending.Col, pending.Row);
		if (removed == null)
		{
			return;
		}

		if (BedPlaceable.IsBed(pending.ItemId) || _placeables.ResolveKind(pending.ItemId) == PlaceableKind.Bed)
		{
			ClearBedTilesUnderPlaceable(pending.Col, pending.Row);
		}

		var centerX = pending.Col * GameConfig.TileSize + GameConfig.TileSize / 2f;
		var centerY = pending.Row * GameConfig.TileSize + GameConfig.TileSize / 2f;
		_loot.Spawn(pending.ItemId, 1, centerX, centerY);
		GD.Print($"Broke {pending.ItemId} at ({pending.Col},{pending.Row})");
	}

	private void ClearBedTilesUnderPlaceable(int footCol, int footRow)
	{
		ClearBedTileIfPresent(footCol, footRow);
		ClearBedTileIfPresent(footCol, footRow - 1);
	}

	private void ClearBedTileIfPresent(int col, int row)
	{
		var cell = new Vector2I(col, row);
		var tileId = _ground.GetCellSourceId(cell);
		if (tileId != GameConfig.BedTileId)
		{
			return;
		}

		TileRotation.SetCell(_ground, cell, GameConfig.CabinFloorTileId);
		_overlays.RefreshCell(_ground, cell);
		_tileVisuals.RefreshCell(_ground, cell);
	}

	private bool IsBreakableWhenEmpty(PlacedObjectKey key, string itemId, out string reason)
	{
		reason = "";
		var kind = _placeables.ResolveKind(itemId);
		if (kind == PlaceableKind.Chest)
		{
			var chest = _placeables.GetChest(key);
			if (chest != null && !chest.IsEmpty)
			{
				reason = "Empty the chest first";
				return false;
			}

			return true;
		}

		if (kind == PlaceableKind.Machine)
		{
			var machine = _placeables.GetChipper(key);
			if (machine != null && !machine.IsEmpty)
			{
				reason = "Empty the chipper first";
				return false;
			}

			return true;
		}

		return true;
	}

	private sealed class PendingBreak
	{
		public string MapId { get; }
		public int Col { get; }
		public int Row { get; }
		public string ItemId { get; }

		public PendingBreak(string mapId, int col, int row, string itemId)
		{
			MapId = mapId;
			Col = col;
			Row = row;
			ItemId = itemId;
		}
	}
}
