using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>
/// Picks the correct water shore sprite from neighbors (Java WaterTileSprites).
/// Orthogonal bitmask: N=1, S=2, E=4, W=8 (bit set when that neighbor is water).
/// </summary>
public static class WaterTileSprites
{
	public static int BuildMask(TileMapLayer ground, int col, int row)
	{
		var mask = 0;
		if (IsWater(ground, col, row - 1))
		{
			mask |= 1;
		}

		if (IsWater(ground, col, row + 1))
		{
			mask |= 2;
		}

		if (IsWater(ground, col + 1, row))
		{
			mask |= 4;
		}

		if (IsWater(ground, col - 1, row))
		{
			mask |= 8;
		}

		return mask;
	}

	/// <summary>Asset base name under tiles/, e.g. water_6 or water_inner_6.</summary>
	public static string SpriteBaseName(TileMapLayer ground, int col, int row)
	{
		var mask = BuildMask(ground, col, row);
		if (mask == 15)
		{
			var inner = ResolveInnerCorner(ground, col, row);
			if (inner != null)
			{
				return inner;
			}
		}

		return $"water_{mask}";
	}

	public static string ResolveSpritePath(TileMapLayer ground, int col, int row)
	{
		var baseName = SpriteBaseName(ground, col, row);
		var path = $"res://assets/sprites/tiles/{baseName}.png";
		if (ResourceLoader.Exists(path))
		{
			return path;
		}

		var mask = BuildMask(ground, col, row);
		path = $"res://assets/sprites/tiles/water_{mask}.png";
		if (ResourceLoader.Exists(path))
		{
			return path;
		}

		return "res://assets/sprites/tiles/water.png";
	}

	private static string? ResolveInnerCorner(TileMapLayer ground, int col, int row)
	{
		var nwLand = IsLand(ground, col - 1, row - 1);
		var neLand = IsLand(ground, col + 1, row - 1);
		var swLand = IsLand(ground, col - 1, row + 1);
		var seLand = IsLand(ground, col + 1, row + 1);

		var landDiagonals = (nwLand ? 1 : 0) + (neLand ? 1 : 0) + (swLand ? 1 : 0) + (seLand ? 1 : 0);
		if (landDiagonals != 1)
		{
			return null;
		}

		if (nwLand)
		{
			return "water_inner_6";
		}

		if (neLand)
		{
			return "water_inner_10";
		}

		if (swLand)
		{
			return "water_inner_5";
		}

		return "water_inner_9";
	}

	private static bool IsLand(TileMapLayer ground, int col, int row)
	{
		var cell = new Vector2I(col, row);
		var used = ground.GetUsedRect();
		if (!used.HasPoint(cell))
		{
			return true;
		}

		return !IsWaterTileId(ground.GetCellSourceId(cell));
	}

	private static bool IsWater(TileMapLayer ground, int col, int row)
	{
		var cell = new Vector2I(col, row);
		var used = ground.GetUsedRect();
		if (!used.HasPoint(cell))
		{
			return false;
		}

		return IsWaterTileId(ground.GetCellSourceId(cell));
	}

	public static bool IsWaterTileId(int tileId) =>
		tileId is GameConfig.WaterTileId or GameConfig.BridgeTileId or GameConfig.BridgeHTileId;

	public static bool IsBridgeTileId(int tileId) =>
		tileId is GameConfig.BridgeTileId or GameConfig.BridgeHTileId;
}
