using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World.Forage;

/// <summary>
/// Active-map foragables: drawn on grass, no collision, not magnet loot.
/// Collected by hovering the tile and right-clicking.
/// </summary>
public partial class ForageLayer : Node2D
{
	private readonly List<(ForageInstance Instance, Sprite2D Sprite)> _entries = new();

	public void Clear()
	{
		foreach (var child in GetChildren())
		{
			child.QueueFree();
		}

		_entries.Clear();
	}

	public List<ForageInstance> Export()
	{
		var list = new List<ForageInstance>(_entries.Count);
		foreach (var (instance, _) in _entries)
		{
			list.Add(instance);
		}

		return list;
	}

	public void Restore(IEnumerable<ForageInstance> forages)
	{
		Clear();
		foreach (var f in forages)
		{
			AddForage(f);
		}
	}

	public bool TryCollectAt(int col, int row, out string itemId)
	{
		itemId = "";
		for (var i = _entries.Count - 1; i >= 0; i--)
		{
			var (instance, sprite) = _entries[i];
			if (instance.Col != col || instance.Row != row)
			{
				continue;
			}

			itemId = instance.ItemId;
			_entries.RemoveAt(i);
			if (GodotObject.IsInstanceValid(sprite))
			{
				sprite.QueueFree();
			}

			return !string.IsNullOrEmpty(itemId);
		}

		return false;
	}

	public bool HasAt(int col, int row)
	{
		foreach (var (instance, _) in _entries)
		{
			if (instance.Col == col && instance.Row == row)
			{
				return true;
			}
		}

		return false;
	}

	private void AddForage(ForageInstance instance)
	{
		var path = ForageSpawner.WorldSpritePath(instance.ItemId);
		if (!ResourceLoader.Exists(path))
		{
			path = $"res://assets/sprites/items/{instance.ItemId}.png";
		}

		var tex = ResourceLoader.Exists(path) ? GD.Load<Texture2D>(path) : null;
		ForageSpawner.OccupiedSize(instance.ItemId, instance.RotationDegrees, out var ow, out var oh);
		var tileOrigin = new Vector2(
			instance.Col * GameConfig.TileSize,
			instance.Row * GameConfig.TileSize
		);
		var center = tileOrigin + new Vector2(
			instance.OffsetX + ow * 0.5f,
			instance.OffsetY + oh * 0.5f
		);

		var sprite = new Sprite2D
		{
			Name = $"Forage_{instance.ItemId}_{instance.Col}_{instance.Row}",
			Centered = true,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			Texture = tex,
			Position = center,
			RotationDegrees = instance.RotationDegrees,
		};
		AddChild(sprite);
		_entries.Add((instance, sprite));
	}
}
