using System;
using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>
/// Draws full-size pine/aspen sprites above grass underlays. TileMap keeps logical tree ids for collision/chop.
/// Tree feet are parented into a shared Y-sort root so they depth-sort with the player (Java drawYSorted).
/// Fades when the player is behind the canopy (TREE_BEHIND_PLAYER_ALPHA).
/// </summary>
public partial class TallTreeLayer : Node2D
{
	private readonly Dictionary<Vector2I, TreeFoot> _feet = new();
	private Func<(Vector2 Feet, float HalfW, float HitH)>? _playerHitbox;
	private Node2D? _sortParent;

	public void Configure(Func<(Vector2 Feet, float HalfW, float HitH)> playerHitbox)
	{
		_playerHitbox = playerHitbox;
	}

	/// <summary>Parent for tree feet (shared WorldSort). Defaults to this node.</summary>
	public void SetSortParent(Node2D parent) => _sortParent = parent;

	private Node2D SortParent => _sortParent ?? this;

	public void Rebuild(TileMapLayer ground)
	{
		ClearFeet();

		foreach (var cell in ground.GetUsedCells())
		{
			RefreshCell(ground, cell);
		}
	}

	public void RefreshCell(TileMapLayer ground, Vector2I cell)
	{
		if (_feet.TryGetValue(cell, out var existing))
		{
			existing.Root.QueueFree();
			_feet.Remove(cell);
		}

		var tileId = ground.GetCellSourceId(cell);
		if (!TallTreeSprites.IsTallTree(tileId))
		{
			return;
		}

		var foot = new Node2D
		{
			Name = $"Tree_{cell.X}_{cell.Y}",
			Position = new Vector2(
				cell.X * GameConfig.TileSize + GameConfig.TileSize / 2f,
				TallTreeSprites.SortY(cell.Y)
			),
		};
		SortParent.AddChild(foot);

		var topLeft = TallTreeSprites.DrawTopLeft(cell.X, cell.Y, tileId);
		var sprite = new Sprite2D
		{
			Texture = GD.Load<Texture2D>(TallTreeSprites.SpritePath(tileId)),
			Centered = false,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			Position = topLeft - foot.Position,
		};
		foot.AddChild(sprite);
		_feet[cell] = new TreeFoot(foot, sprite, cell, tileId);
	}

	public override void _Process(double delta)
	{
		if (_playerHitbox == null || _feet.Count == 0)
		{
			return;
		}

		var (feet, halfW, hitH) = _playerHitbox();
		var playerSortY = feet.Y;
		var playerLeft = feet.X - halfW;
		var playerTop = feet.Y - hitH;
		var playerW = halfW * 2f;

		foreach (var entry in _feet.Values)
		{
			if (!GodotObject.IsInstanceValid(entry.Sprite))
			{
				continue;
			}

			var fade = ShouldFade(
				playerSortY,
				playerLeft,
				playerTop,
				playerW,
				hitH,
				entry.Cell.X,
				entry.Cell.Y,
				entry.TileId
			);
			var a = fade ? GameConfig.TreeBehindPlayerAlpha : 1f;
			entry.Sprite.Modulate = new Color(1f, 1f, 1f, a);
		}
	}

	private void ClearFeet()
	{
		foreach (var entry in _feet.Values)
		{
			if (GodotObject.IsInstanceValid(entry.Root))
			{
				entry.Root.QueueFree();
			}
		}

		_feet.Clear();
	}

	/// <summary>Java GdxEntityRenderer.shouldFadeTreeForPlayer — stump-column overlap only.</summary>
	private static bool ShouldFade(
		float playerSortY,
		float playerLeft,
		float playerTop,
		float playerW,
		float playerH,
		int col,
		int row,
		int tileId
	)
	{
		var treeSortY = TallTreeSprites.SortY(row);
		if (playerSortY > treeSortY)
		{
			return false;
		}

		var topLeft = TallTreeSprites.DrawTopLeft(col, row, tileId);
		var columnX = col * GameConfig.TileSize;
		var columnY = topLeft.Y;
		var columnW = GameConfig.TileSize;
		var columnH = treeSortY - columnY;

		return playerLeft < columnX + columnW
			&& playerLeft + playerW > columnX
			&& playerTop < columnY + columnH
			&& playerTop + playerH > columnY;
	}

	private sealed class TreeFoot
	{
		public Node2D Root { get; }
		public Sprite2D Sprite { get; }
		public Vector2I Cell { get; }
		public int TileId { get; }

		public TreeFoot(Node2D root, Sprite2D sprite, Vector2I cell, int tileId)
		{
			Root = root;
			Sprite = sprite;
			Cell = cell;
			TileId = tileId;
		}
	}
}
