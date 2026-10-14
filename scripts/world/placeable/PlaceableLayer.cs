using System;
using System.Collections.Generic;
using System.Linq;
using Godot;
using LumberjackAttack.Core;
using LumberjackAttack.Inventory;
using LumberjackAttack.Items;
using LumberjackAttack.Save;

namespace LumberjackAttack.World.Placeable;

public readonly record struct PlacedObjectKey(string MapId, int Col, int Row);

public sealed class PlacedObjectData
{
	public string MapId { get; init; } = "";
	public string ItemId { get; init; } = "";
	public int Col { get; init; }
	public int Row { get; init; }
}

/// <summary>Tracks placed objects, chippers, and chests across maps; draws the active map.</summary>
public partial class PlaceableLayer : Node2D
{
	private readonly List<PlacedObjectData> _objects = new();
	private readonly Dictionary<PlacedObjectKey, WoodChipperMachine> _chippers = new();
	private readonly Dictionary<PlacedObjectKey, ItemContainer> _chests = new();
	private readonly Dictionary<string, Texture2D> _textures = new();
	private readonly List<Node2D> _visuals = new();
	private string _mapId = "";
	private Func<string, int> _maxStackOf = _ => 999;
	private IReadOnlyDictionary<string, ItemDef> _items = new Dictionary<string, ItemDef>();

	public void Configure(IReadOnlyDictionary<string, ItemDef> items, Func<string, int> maxStackOf)
	{
		_items = items;
		_maxStackOf = maxStackOf;
	}

	public void ClearAll()
	{
		_objects.Clear();
		_chippers.Clear();
		_chests.Clear();
		RebuildVisuals();
	}

	public void SetActiveMap(string mapId)
	{
		_mapId = mapId;
		RebuildVisuals();
	}

	public bool HasAt(string mapId, int col, int row) =>
		_objects.Any(o => o.MapId == mapId && OccupiesTile(o, col, row));

	public PlacedObjectData? FindAt(string mapId, int col, int row) =>
		_objects.FirstOrDefault(o => o.MapId == mapId && OccupiesTile(o, col, row));

	public PlacedObjectData? FindAtWorld(string mapId, Vector2 world)
	{
		foreach (var o in _objects)
		{
			if (o.MapId != mapId)
			{
				continue;
			}

			if (WorldBounds(o).HasPoint(world))
			{
				return o;
			}
		}

		return null;
	}

	private static Rect2 WorldBounds(PlacedObjectData o)
	{
		if (BedPlaceable.IsBed(o.ItemId))
		{
			return new Rect2(
				o.Col * GameConfig.TileSize,
				BedPlaceable.HeadRow(o.Row) * GameConfig.TileSize,
				BedPlaceable.SpriteWidth,
				BedPlaceable.SpriteHeight
			);
		}

		var pad = (GameConfig.TileSize - GameConfig.PlaceableHitboxSize) / 2f;
		return new Rect2(
			o.Col * GameConfig.TileSize + pad,
			o.Row * GameConfig.TileSize + pad,
			GameConfig.PlaceableHitboxSize,
			GameConfig.PlaceableHitboxSize
		);
	}

	private static bool OccupiesTile(PlacedObjectData o, int col, int row)
	{
		if (BedPlaceable.IsBed(o.ItemId))
		{
			return BedPlaceable.Occupies(o.Col, o.Row, col, row);
		}

		return o.Col == col && o.Row == row;
	}

	/// <summary>True if player hitbox intersects a blocking placeable (chests/chippers; not beds).</summary>
	public bool CollidesWith(string mapId, Vector2 feetCenter, float halfW, float hitH)
	{
		var playerRect = new Rect2(
			feetCenter.X - halfW,
			feetCenter.Y - hitH,
			halfW * 2f,
			hitH
		);

		foreach (var o in _objects)
		{
			if (o.MapId != mapId || BedPlaceable.IsBed(o.ItemId) || KindOf(o.ItemId) == PlaceableKind.Bed)
			{
				continue;
			}

			var pad = (GameConfig.TileSize - GameConfig.PlaceableHitboxSize) / 2f;
			var placeRect = new Rect2(
				o.Col * GameConfig.TileSize + pad,
				o.Row * GameConfig.TileSize + pad,
				GameConfig.PlaceableHitboxSize,
				GameConfig.PlaceableHitboxSize
			);
			if (placeRect.Intersects(playerRect))
			{
				return true;
			}
		}

		return false;
	}

	/// <summary>Removes placeable + linked chest/chipper state. Returns removed object, or null.</summary>
	public PlacedObjectData? RemoveAt(string mapId, int col, int row)
	{
		var idx = _objects.FindIndex(o => o.MapId == mapId && OccupiesTile(o, col, row));
		if (idx < 0)
		{
			return null;
		}

		var removed = _objects[idx];
		_objects.RemoveAt(idx);

		var key = new PlacedObjectKey(mapId, removed.Col, removed.Row);
		_chippers.Remove(key);
		_chests.Remove(key);

		if (mapId == _mapId)
		{
			RebuildVisuals();
		}

		return removed;
	}

	public void Place(string mapId, string itemId, int col, int row)
	{
		_objects.Add(new PlacedObjectData
		{
			MapId = mapId,
			ItemId = itemId,
			Col = col,
			Row = row,
		});

		RegisterStorageFor(itemId, new PlacedObjectKey(mapId, col, row));

		if (mapId == _mapId)
		{
			RebuildVisuals();
		}
	}

	private void RegisterStorageFor(string itemId, PlacedObjectKey key)
	{
		switch (KindOf(itemId))
		{
			case PlaceableKind.Machine:
				GetOrCreateChipper(key);
				break;
			case PlaceableKind.Chest:
				GetOrCreateChest(key);
				break;
		}
	}

	private PlaceableKind KindOf(string itemId) =>
		_items.TryGetValue(itemId, out var def)
			? def.PlaceableKind
			: PlaceableKindParser.Parse(null, itemId, isPlaceableCategory: true);

	public PlaceableKind ResolveKind(string itemId) => KindOf(itemId);

	public WoodChipperMachine GetOrCreateChipper(PlacedObjectKey key)
	{
		if (!_chippers.TryGetValue(key, out var machine))
		{
			machine = new WoodChipperMachine();
			_chippers[key] = machine;
		}

		return machine;
	}

	public WoodChipperMachine? GetChipper(PlacedObjectKey key) =>
		_chippers.TryGetValue(key, out var m) ? m : null;

	public ItemContainer GetOrCreateChest(PlacedObjectKey key)
	{
		if (!_chests.TryGetValue(key, out var chest))
		{
			chest = ItemContainer.CreateChest(_items);
			_chests[key] = chest;
		}

		return chest;
	}

	public ItemContainer? GetChest(PlacedObjectKey key) =>
		_chests.TryGetValue(key, out var c) ? c : null;

	public void UpdateAll(double elapsedMs)
	{
		if (elapsedMs <= 0)
		{
			return;
		}

		foreach (var machine in _chippers.Values)
		{
			machine.Update(elapsedMs, _maxStackOf);
		}
	}

	public List<PlacedObjectDto> ExportPlaced() =>
		_objects.Select(o => new PlacedObjectDto
		{
			MapId = o.MapId,
			ItemId = o.ItemId,
			Col = o.Col,
			Row = o.Row,
		}).ToList();

	public List<WoodChipperDto> ExportChippers()
	{
		var list = new List<WoodChipperDto>();
		foreach (var (key, machine) in _chippers)
		{
			list.Add(new WoodChipperDto
			{
				MapId = key.MapId,
				Col = key.Col,
				Row = key.Row,
				InputItemId = machine.InputStack.IsEmpty ? "" : machine.InputStack.ItemId!,
				InputQty = machine.InputStack.IsEmpty ? 0 : machine.InputStack.Quantity,
				OutputItemId = machine.OutputStack.IsEmpty ? "" : machine.OutputStack.ItemId!,
				OutputQty = machine.OutputStack.IsEmpty ? 0 : machine.OutputStack.Quantity,
				ProgressMs = machine.ProgressMs,
				Processing = machine.IsProcessing,
			});
		}

		return list;
	}

	public List<ChestSlotDto> ExportChests()
	{
		var list = new List<ChestSlotDto>();
		foreach (var (key, chest) in _chests)
		{
			list.AddRange(chest.ExportSlots(key.MapId, key.Col, key.Row));
		}

		return list;
	}

	public void ImportAll(
		IEnumerable<PlacedObjectDto> placed,
		IEnumerable<WoodChipperDto> chippers,
		IEnumerable<ChestSlotDto> chestSlots
	)
	{
		_objects.Clear();
		_chippers.Clear();
		_chests.Clear();

		foreach (var p in placed)
		{
			if (string.IsNullOrWhiteSpace(p.MapId) || string.IsNullOrWhiteSpace(p.ItemId))
			{
				continue;
			}

			_objects.Add(new PlacedObjectData
			{
				MapId = p.MapId,
				ItemId = p.ItemId,
				Col = p.Col,
				Row = p.Row,
			});

			var key = new PlacedObjectKey(p.MapId, p.Col, p.Row);
			RegisterStorageFor(p.ItemId, key);
		}

		foreach (var c in chippers)
		{
			var key = new PlacedObjectKey(c.MapId, c.Col, c.Row);
			var machine = GetOrCreateChipper(key);
			machine.Restore(
				c.InputItemId,
				c.InputQty,
				c.OutputItemId,
				c.OutputQty,
				c.ProgressMs,
				c.Processing
			);
		}

		foreach (var slot in chestSlots)
		{
			var key = new PlacedObjectKey(slot.MapId, slot.Col, slot.Row);
			GetOrCreateChest(key).ImportSlot(slot.Slot, slot.ItemId, slot.Qty);
		}

		RebuildVisuals();
	}

	private void RebuildVisuals()
	{
		foreach (var node in _visuals)
		{
			if (GodotObject.IsInstanceValid(node))
			{
				node.QueueFree();
			}
		}

		_visuals.Clear();

		foreach (var o in _objects.Where(o => o.MapId == _mapId))
		{
			// Java draws placeables under the player layer — keep them on this layer (below WorldSort).
			Vector2 position;
			if (BedPlaceable.IsBed(o.ItemId))
			{
				position = new Vector2(
					o.Col * GameConfig.TileSize + GameConfig.TileSize / 2f,
					BedPlaceable.HeadRow(o.Row) * GameConfig.TileSize + BedPlaceable.SpriteHeight / 2f
				);
			}
			else
			{
				position = new Vector2(
					o.Col * GameConfig.TileSize + GameConfig.TileSize / 2f,
					o.Row * GameConfig.TileSize + GameConfig.TileSize / 2f
				);
			}

			var sprite = new Sprite2D
			{
				Name = $"Placeable_{o.ItemId}_{o.Col}_{o.Row}",
				Texture = GetTexture(o.ItemId),
				Centered = true,
				TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
				Position = position,
			};
			AddChild(sprite);
			_visuals.Add(sprite);
		}
	}

	private Texture2D? GetTexture(string itemId)
	{
		if (_textures.TryGetValue(itemId, out var cached))
		{
			return cached;
		}

		var path = $"res://assets/sprites/items/{itemId}.png";
		if (!ResourceLoader.Exists(path))
		{
			return null;
		}

		var tex = GD.Load<Texture2D>(path);
		_textures[itemId] = tex;
		return tex;
	}
}
