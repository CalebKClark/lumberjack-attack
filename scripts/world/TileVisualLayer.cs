using System.Collections.Generic;
using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>
/// Draws map tiles with Java-matching clockwise rotation (Sprite2D).
/// The TileMapLayer stays invisible and is used only for collision / logic ids.
/// </summary>
public partial class TileVisualLayer : Node2D
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
		if (tileId < 0 || !_catalog.TryGetValue(tileId, out var def))
		{
			return;
		}

		if (def.Name is "transition_marker")
		{
			return;
		}

		var path = ResolveSpritePath(ground, cell, tileId, def);
		var tex = GetTexture(path);
		if (tex == null)
		{
			return;
		}

		// Water auto-tiler owns shore orientation; overlay underlays stay unrotated.
		var rot = tileId == GameConfig.WaterTileId
			|| OverlayTiles.NeedsDrawnOverlay(tileId)
			|| TallTreeSprites.IsTallTree(tileId)
			? 0
			: _rotations.Get(cell.X, cell.Y);

		var sprite = new Sprite2D
		{
			Texture = tex,
			Centered = true,
			TextureFilter = CanvasItem.TextureFilterEnum.Nearest,
			Position = new Vector2(
				cell.X * GameConfig.TileSize + GameConfig.TileSize / 2f,
				cell.Y * GameConfig.TileSize + GameConfig.TileSize / 2f
			),
			RotationDegrees = TileRotation.Degrees(rot),
		};
		AddChild(sprite);
		_sprites[cell] = sprite;
	}

	private static string ResolveSpritePath(TileMapLayer ground, Vector2I cell, int tileId, TileDef def)
	{
		if (tileId == GameConfig.WaterTileId || OverlayTiles.IsBridge(tileId))
		{
			return WaterTileSprites.ResolveSpritePath(ground, cell.X, cell.Y);
		}

		return OverlayTiles.BaseSpritePath(tileId) ?? $"res://assets/sprites/tiles/{def.Name}.png";
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
