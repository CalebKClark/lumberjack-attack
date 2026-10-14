using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>Active-map ground loot container.</summary>
public partial class LootLayer : Node2D
{
	private readonly List<DroppedLoot> _drops = new();

	public void Clear()
	{
		foreach (var child in GetChildren())
		{
			child.QueueFree();
		}

		_drops.Clear();
	}

	public void SpawnCanopyRain(
		string itemId,
		int quantity,
		float treeLeft,
		float treeTop,
		float treeWidth,
		float treeHeight,
		float landCenterY
	)
	{
		var count = Math.Max(1, quantity);
		var stumpOverlap = Math.Min(treeHeight - 1f, GameConfig.PineTreeStumpOverlapPx);
		var canopyHeight = Math.Max(8f, treeHeight - stumpOverlap);
		var stumpLeft = treeLeft + (treeWidth - GameConfig.TileSize) / 2f;

		for (var i = 0; i < count; i++)
		{
			var startX = treeLeft + (float)(Random.Shared.NextDouble() * treeWidth);
			var startY = treeTop + (float)(Random.Shared.NextDouble() * canopyHeight);
			var landX = stumpLeft + (float)(Random.Shared.NextDouble() * GameConfig.TileSize);
			var delay = Random.Shared.NextDouble() * GameConfig.TreeRainMaxDelayMs;
			var fallMs = GameConfig.TreeRainFallMinMs
				+ Random.Shared.NextDouble() * (GameConfig.TreeRainFallMaxMs - GameConfig.TreeRainFallMinMs);
			var drop = DroppedLoot.CanopyRain(itemId, startX, startY, landX, landCenterY, delay, fallMs);
			AddChild(drop);
			_drops.Add(drop);
		}
	}

	/// <summary>Spawn ground loot with Java-style bounce scatter (placeable break, etc.).</summary>
	public void Spawn(string itemId, int quantity, float centerX, float centerY)
	{
		var drop = DroppedLoot.ScatterFrom(itemId, Math.Max(1, quantity), centerX, centerY);
		AddChild(drop);
		_drops.Add(drop);
	}

	public List<StoredDrop> ExportSettled()
	{
		var list = new List<StoredDrop>();
		foreach (var drop in _drops)
		{
			if (!GodotObject.IsInstanceValid(drop))
			{
				continue;
			}

			// Save mid-air drops at their current center too so they aren't lost.
			list.Add(new StoredDrop(drop.ItemId, drop.Quantity, drop.Center.X, drop.Center.Y));
		}

		return list;
	}

	public void RestoreSettled(IEnumerable<StoredDrop> drops)
	{
		Clear();
		foreach (var d in drops)
		{
			var drop = DroppedLoot.Settled(d.ItemId, d.Quantity, d.X, d.Y);
			AddChild(drop);
			_drops.Add(drop);
		}
	}

	public void TickAndCollect(
		Vector2 playerFeet,
		float halfW,
		float hitH,
		double elapsedMs,
		float magnetRadiusPx,
		System.Action<string, int> onCollect
	)
	{
		for (var i = _drops.Count - 1; i >= 0; i--)
		{
			var drop = _drops[i];
			if (!GodotObject.IsInstanceValid(drop))
			{
				_drops.RemoveAt(i);
				continue;
			}

			drop.Tick(elapsedMs);
			if (!drop.UpdateMagnet(playerFeet, halfW, hitH, elapsedMs, magnetRadiusPx))
			{
				continue;
			}

			onCollect(drop.ItemId, drop.Quantity);
			_drops.RemoveAt(i);
			drop.QueueFree();
		}
	}
}
