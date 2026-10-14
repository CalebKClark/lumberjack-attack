using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>Tiles drawn as a sprite over base terrain (bridge over water, stump over grass).</summary>
public static class OverlayTiles
{
	public static bool IsGrassOverlay(int tileId) =>
		TallTreeSprites.IsTreeStump(tileId) || TallTreeSprites.IsTallTree(tileId);

	public static bool IsBridge(int tileId) =>
		tileId is GameConfig.BridgeTileId or GameConfig.BridgeHTileId;

	public static bool IsBed(int tileId) => tileId == GameConfig.BedTileId;

	/// <summary>True when the TileMap cell should show a base underlay + a separate overlay sprite.</summary>
	public static bool NeedsDrawnOverlay(int tileId) =>
		TallTreeSprites.IsTreeStump(tileId) || IsBridge(tileId) || IsBed(tileId);

	public static string? BaseSpritePath(int tileId)
	{
		if (IsGrassOverlay(tileId))
		{
			return "res://assets/sprites/tiles/grass.png";
		}

		if (IsBridge(tileId))
		{
			return "res://assets/sprites/tiles/water.png";
		}

		if (IsBed(tileId))
		{
			return "res://assets/sprites/tiles/cabin_floor.png";
		}

		return null;
	}

	public static string OverlaySpritePath(int tileId, string tileName) =>
		$"res://assets/sprites/tiles/{tileName}.png";
}
