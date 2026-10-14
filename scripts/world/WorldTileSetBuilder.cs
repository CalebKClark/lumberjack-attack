using System.Collections.Generic;
using Godot;

namespace LumberjackAttack.World;

/// <summary>
/// Builds a Godot TileSet from assets/sprites/tiles/{name}.png using CSV tile ids as source ids.
/// Overlay tiles use base terrain textures; OverlayLayer / TallTreeLayer / TileVisualLayer draw the rest.
/// </summary>
public static class WorldTileSetBuilder
{
	public const int TileSize = 16;

	public static TileSet Build(IReadOnlyDictionary<int, TileDef> catalog)
	{
		var tileSet = new TileSet();
		tileSet.TileSize = new Vector2I(TileSize, TileSize);

		foreach (var (id, def) in catalog)
		{
			if (def.Name is "transition_marker")
			{
				continue;
			}

			var path = OverlayTiles.BaseSpritePath(id) ?? $"res://assets/sprites/tiles/{def.Name}.png";
			if (!ResourceLoader.Exists(path))
			{
				GD.PushWarning($"Missing tile sprite for '{def.Name}' ({path})");
				continue;
			}

			var texture = GD.Load<Texture2D>(path);
			var source = new TileSetAtlasSource
			{
				Texture = texture,
				TextureRegionSize = new Vector2I(TileSize, TileSize),
			};
			source.CreateTile(Vector2I.Zero);
			tileSet.AddSource(source, id);
		}

		return tileSet;
	}
}
