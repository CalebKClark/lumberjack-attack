using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Entities;
using LumberjackAttack.Inventory;
using LumberjackAttack.Skills;
using LumberjackAttack.Time;

namespace LumberjackAttack.World;

/// <summary>Click-to-chop pine trees (Java ChopService).</summary>
public sealed class ChopService
{
	private readonly TreeHitTracker _hits = new();
	private readonly TreeRegrowthManager _regrowth = new();
	private readonly TileMapLayer _ground;
	private readonly IReadOnlyDictionary<int, TileDef> _catalog;
	private readonly TallTreeLayer _trees;
	private readonly OverlayLayer _overlays;
	private readonly TileVisualLayer _tileVisuals;
	private readonly LootLayer _loot;
	private readonly PlayerInventory _inventory;
	private readonly PlayerEnergyState _energy;
	private readonly PlayerSkills _skills;
	private readonly Func<string> _currentMapId;
	private PendingChop? _pending;

	public TreeRegrowthManager Regrowth => _regrowth;

	public ChopService(
		TileMapLayer ground,
		IReadOnlyDictionary<int, TileDef> catalog,
		TallTreeLayer trees,
		OverlayLayer overlays,
		TileVisualLayer tileVisuals,
		LootLayer loot,
		PlayerInventory inventory,
		PlayerEnergyState energy,
		PlayerSkills skills,
		Func<string> currentMapId
	)
	{
		_ground = ground;
		_catalog = catalog;
		_trees = trees;
		_overlays = overlays;
		_tileVisuals = tileVisuals;
		_loot = loot;
		_inventory = inventory;
		_energy = energy;
		_skills = skills;
		_currentMapId = currentMapId;
	}

	public bool TryStartChop(PlayerController player, Vector2 worldClick, out string toast)
	{
		toast = "";
		if (player.IsActionLocked || _pending != null)
		{
			return false;
		}

		var axe = _inventory.GetHeldAxe();
		if (axe == null)
		{
			return false;
		}

		var target = ResolveChopTarget(worldClick);
		if (target == null)
		{
			return false;
		}

		var (col, row) = target.Value;
		var tileId = _ground.GetCellSourceId(new Vector2I(col, row));
		if (!_catalog.TryGetValue(tileId, out var tile) || !tile.Choppable || tile.DropItemId == null)
		{
			return false;
		}

		var tileCenter = new Vector2(
			col * GameConfig.TileSize + GameConfig.TileSize / 2f,
			row * GameConfig.TileSize + GameConfig.TileSize / 2f
		);
		var playerCenter = player.GlobalPosition - new Vector2(0, GameConfig.PlayerHitboxHeight / 2f);
		if (playerCenter.DistanceTo(tileCenter) > GameConfig.ChopReachDistance)
		{
			return false;
		}

		if (!_energy.TrySpend(GameConfig.ActionEnergyCost))
		{
			toast = "Too tired";
			return true;
		}

		var damage = axe.TreeDamage > 0 ? axe.TreeDamage : GameConfig.DefaultAxeTreeDamage;
		var mapId = _currentMapId();
		_pending = new PendingChop(mapId, col, row, damage);
		player.FaceToward(tileCenter);
		player.TriggerAxeSwing(OnSwingCompleted);
		return true;
	}

	public void CancelPending() => _pending = null;

	private void OnSwingCompleted(bool swingCompleted)
	{
		if (!swingCompleted || _pending == null)
		{
			_pending = null;
			return;
		}

		var pending = _pending;
		_pending = null;
		if (pending.MapId != _currentMapId())
		{
			return;
		}

		ApplyPendingChop(pending);
	}

	private void ApplyPendingChop(PendingChop pending)
	{
		var cell = new Vector2I(pending.Col, pending.Row);
		var tileId = _ground.GetCellSourceId(cell);
		if (!_catalog.TryGetValue(tileId, out var tile) || !tile.Choppable || tile.DropItemId == null)
		{
			return;
		}

		var remaining = _hits.ApplyHit(
			pending.MapId,
			pending.Col,
			pending.Row,
			Math.Max(1, tile.Toughness),
			pending.TreeDamage
		);
		if (remaining > 0)
		{
			return;
		}

		FellTree(pending.MapId, pending.Col, pending.Row, tile);
	}

	private void FellTree(string mapId, int col, int row, TileDef tile)
	{
		_hits.Clear(mapId, col, row);

		var tileCenterY = row * GameConfig.TileSize + GameConfig.TileSize / 2f;
		var quantity = tile.RollDropQuantity();

		if (TallTreeSprites.IsTallTree(tile.Id))
		{
			var topLeft = TallTreeSprites.DrawTopLeft(col, row, tile.Id);
			_loot.SpawnCanopyRain(
				tile.DropItemId!,
				quantity,
				topLeft.X,
				topLeft.Y,
				TallTreeSprites.SpriteWidth(tile.Id),
				TallTreeSprites.SpriteHeight(tile.Id),
				tileCenterY
			);
		}

		var cell = new Vector2I(col, row);
		if (tile.RegrowthGameMinutes > 0)
		{
			TileRotation.SetCell(_ground, cell, TallTreeSprites.StumpTileIdForTree(tile.Id));
			_regrowth.Schedule(
				mapId,
				col,
				row,
				tile.Id,
				tile.RegrowthGameMinutes * (double)GameConfig.RealMsPerGameMinute
			);
		}
		else
		{
			TileRotation.SetCell(_ground, cell, GameConfig.GrassTileId);
		}

		_trees.RefreshCell(_ground, cell);
		_overlays.RefreshCell(_ground, cell);
		_tileVisuals.RefreshCell(_ground, cell);

		if (tile.ForagingXp > 0)
		{
			_skills.GrantXp(SkillType.Foraging, tile.ForagingXp);
		}
	}

	private (int Col, int Row)? ResolveChopTarget(Vector2 world)
	{
		var clickCell = _ground.LocalToMap(_ground.ToLocal(world));
		var searchRadius = Math.Max(2, GameConfig.PineTreeSpriteHeight / GameConfig.TileSize + 1);
		var bestCol = -1;
		var bestRow = -1;
		var bestDistSq = float.MaxValue;

		for (var row = clickCell.Y - searchRadius; row <= clickCell.Y + searchRadius; row++)
		{
			for (var col = clickCell.X - searchRadius; col <= clickCell.X + searchRadius; col++)
			{
				var cell = new Vector2I(col, row);
				var tileId = _ground.GetCellSourceId(cell);
				if (!TallTreeSprites.IsTallTree(tileId))
				{
					continue;
				}

				if (!TallTreeSprites.ContainsWorldPoint(col, row, tileId, world))
				{
					continue;
				}

				var center = new Vector2(
					col * GameConfig.TileSize + GameConfig.TileSize / 2f,
					row * GameConfig.TileSize + GameConfig.TileSize / 2f
				);
				var distSq = center.DistanceSquaredTo(world);
				if (distSq < bestDistSq)
				{
					bestDistSq = distSq;
					bestCol = col;
					bestRow = row;
				}
			}
		}

		if (bestCol >= 0)
		{
			return (bestCol, bestRow);
		}

		var id = _ground.GetCellSourceId(clickCell);
		if (id < 0 || !_catalog.TryGetValue(id, out var def) || !def.Choppable)
		{
			return null;
		}

		return (clickCell.X, clickCell.Y);
	}

	private sealed class PendingChop
	{
		public string MapId { get; }
		public int Col { get; }
		public int Row { get; }
		public int TreeDamage { get; }

		public PendingChop(string mapId, int col, int row, int treeDamage)
		{
			MapId = mapId;
			Col = col;
			Row = row;
			TreeDamage = treeDamage;
		}
	}
}
