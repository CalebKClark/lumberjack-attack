using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World.Forage;

public static class ForageSpawner
{
	private static readonly Dictionary<string, Vector2I> NativeSizeCache = new(StringComparer.Ordinal);

	/// <summary>
	/// One day's forage roll: keep existing spawns, roll grass tiles that are still empty.
	/// </summary>
	public static List<ForageInstance> RollDayAdditive(
		MapStateStore.MapGrid grid,
		IReadOnlyList<ForageDef> table,
		IReadOnlyList<ForageInstance> existing
	)
	{
		var occupied = new HashSet<long>();
		var list = new List<ForageInstance>(existing.Count + 16);
		foreach (var f in existing)
		{
			occupied.Add(PackCell(f.Col, f.Row));
			list.Add(f);
		}

		for (var row = 0; row < grid.Height; row++)
		{
			for (var col = 0; col < grid.Width; col++)
			{
				if (occupied.Contains(PackCell(col, row)))
				{
					continue;
				}

				var tileId = grid.Tiles[row * grid.Width + col];
				TrySpawnAt(list, table, col, row, tileId);
			}
		}

		return list;
	}

	public static List<ForageInstance> RollDayAdditiveFromLayer(
		TileMapLayer ground,
		IReadOnlyList<ForageDef> table,
		IReadOnlyList<ForageInstance> existing
	)
	{
		var occupied = new HashSet<long>();
		var list = new List<ForageInstance>(existing.Count + 16);
		foreach (var f in existing)
		{
			occupied.Add(PackCell(f.Col, f.Row));
			list.Add(f);
		}

		var used = ground.GetUsedRect();
		for (var row = used.Position.Y; row < used.Position.Y + used.Size.Y; row++)
		{
			for (var col = used.Position.X; col < used.Position.X + used.Size.X; col++)
			{
				if (occupied.Contains(PackCell(col, row)))
				{
					continue;
				}

				var tileId = ground.GetCellSourceId(new Vector2I(col, row));
				TrySpawnAt(list, table, col, row, tileId);
			}
		}

		return list;
	}

	private static long PackCell(int col, int row) => ((long)col << 32) ^ (uint)row;

	private static void TrySpawnAt(
		List<ForageInstance> list,
		IReadOnlyList<ForageDef> table,
		int col,
		int row,
		int tileId
	)
	{
		if (tileId != GameConfig.GrassTileId)
		{
			return;
		}

		if (Random.Shared.NextDouble() >= GameConfig.ForageSpawnChancePerGrass)
		{
			return;
		}

		var itemId = ForageCatalog.Roll(table);
		if (string.IsNullOrEmpty(itemId))
		{
			return;
		}

		var rot = Random.Shared.Next(4) * 90;
		OccupiedSize(itemId, rot, out var ow, out var oh);
		var maxX = GameConfig.TileSize - ow;
		var maxY = GameConfig.TileSize - oh;
		var ox = maxX > 0 ? Random.Shared.Next(0, maxX + 1) : 0;
		var oy = maxY > 0 ? Random.Shared.Next(0, maxY + 1) : 0;
		list.Add(new ForageInstance(itemId, col, row, ox, oy, rot));
	}

	public static void OccupiedSize(string itemId, int rotationDegrees, out int width, out int height)
	{
		var native = NativeSize(itemId);
		var upright = rotationDegrees % 180 == 0;
		width = upright ? native.X : native.Y;
		height = upright ? native.Y : native.X;
		width = Math.Clamp(width, 1, GameConfig.TileSize);
		height = Math.Clamp(height, 1, GameConfig.TileSize);
	}

	public static string WorldSpritePath(string itemId) =>
		$"res://assets/sprites/forage/{itemId}.png";

	private static Vector2I NativeSize(string itemId)
	{
		if (NativeSizeCache.TryGetValue(itemId, out var cached))
		{
			return cached;
		}

		var path = WorldSpritePath(itemId);
		if (ResourceLoader.Exists(path))
		{
			var tex = GD.Load<Texture2D>(path);
			if (tex != null)
			{
				var size = new Vector2I(Math.Max(1, tex.GetWidth()), Math.Max(1, tex.GetHeight()));
				NativeSizeCache[itemId] = size;
				return size;
			}
		}

		var fallback = new Vector2I(2, 8);
		NativeSizeCache[itemId] = fallback;
		return fallback;
	}
}
