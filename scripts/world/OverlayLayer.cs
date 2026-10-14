using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>
/// Draws stump/bridge/bed sprites over grass/water/floor underlays on the TileMap.
/// Living trees stay on TallTreeLayer. Beds are 16×32 — one sprite for a stacked head+foot pair.
/// </summary>
public partial class OverlayLayer : Node2D
{
	private readonly Dictionary<Vector2I, Sprite2D> _sprites = new();
	private readonly Dictionary<string, Texture2D> _textures = new();
	private IReadOnlyDictionary<int, TileDef> _catalog = null!;
	private RotationGrid _rotations = new(0, 0);

	public void Configure(IReadOnlyDictionary<int, TileDef> catalog)
	{
		_catalog = catalog;
	}

	public void SetRotations(RotationGrid rotations) => _rotations = rotations;

	public void Rebuild(TileMapLayer ground)
	{
		foreach (var child in GetChildren())
		{
			child.QueueFree();
		}

		_sprites.Clear();

		foreach (var cell in ground.GetUsedCells())
		{
			RefreshCell(ground, cell);
		}
	}

	public void RefreshCell(TileMapLayer ground, Vector2I cell)
	{
		if (_sprites.TryGetValue(cell, out var existing))
		{
			existing.QueueFree();
			_sprites.Remove(cell);
		}

		var tileId = ground.GetCellSourceId(cell);
		if (!OverlayTiles.NeedsDrawnOverlay(tileId))
		{
			return;
		}

		// Stacked bed head: floor underlay only; foot cell draws the full 16×32 sprite.
		if (OverlayTiles.IsBed(tileId) && IsStackedBedHead(ground, cell))
		{
			return;
		}

		if (!_catalog.TryGetValue(tileId, out var def))
		{
			return;
		}

		var path = OverlayTiles.OverlaySpritePath(tileId, def.Name);
		var tex = GetTexture(path);
		if (tex == null)
		{
			return;
		}

		var sprite = new Sprite2D
		{
			Texture = tex,
			Centered = false,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			ZIndex = 1,
		};

		var rot = OverlayTiles.IsBed(tileId) ? 0 : _rotations.Get(cell.X, cell.Y);

		if (OverlayTiles.IsBed(tileId))
		{
			// Full bed art sits on the head tile (north); foot cell owns the draw.
			var headRow = IsStackedBedFoot(ground, cell) ? cell.Y - 1 : cell.Y;
			sprite.Position = new Vector2(cell.X * GameConfig.TileSize, headRow * GameConfig.TileSize);
		}
		else
		{
			sprite.Centered = true;
			sprite.Position = new Vector2(
				cell.X * GameConfig.TileSize + GameConfig.TileSize / 2f,
				cell.Y * GameConfig.TileSize + GameConfig.TileSize / 2f
			);
			sprite.RotationDegrees = TileRotation.Degrees(rot);
		}

		AddChild(sprite);
		_sprites[cell] = sprite;
	}

	private static bool IsStackedBedFoot(TileMapLayer ground, Vector2I foot)
	{
		if (!OverlayTiles.IsBed(ground.GetCellSourceId(foot)))
		{
			return false;
		}

		var head = new Vector2I(foot.X, foot.Y - 1);
		return OverlayTiles.IsBed(ground.GetCellSourceId(head));
	}

	private static bool IsStackedBedHead(TileMapLayer ground, Vector2I head)
	{
		if (!OverlayTiles.IsBed(ground.GetCellSourceId(head)))
		{
			return false;
		}

		var foot = new Vector2I(head.X, head.Y + 1);
		return OverlayTiles.IsBed(ground.GetCellSourceId(foot));
	}

	private Texture2D? GetTexture(string path)
	{
		if (_textures.TryGetValue(path, out var cached))
		{
			return cached;
		}

		if (!ResourceLoader.Exists(path))
		{
			return null;
		}

		var tex = GD.Load<Texture2D>(path);
		_textures[path] = tex;
		return tex;
	}
}
