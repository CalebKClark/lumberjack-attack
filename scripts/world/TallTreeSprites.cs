using Godot;
using LumberjackAttack.Core;

namespace LumberjackAttack.World;

/// <summary>Layout helpers for tall tree sprites drawn above a stump tile (Java TallTreeSprites).</summary>
public static class TallTreeSprites
{
	public static bool IsTallTree(int tileId) =>
		tileId is GameConfig.PineTreeTileId or GameConfig.AspenTreeTileId;

	public static bool IsTreeStump(int tileId) =>
		tileId is GameConfig.PineStumpTileId or GameConfig.AspenStumpTileId;

	public static int StumpTileIdForTree(int treeTileId) =>
		treeTileId == GameConfig.AspenTreeTileId
			? GameConfig.AspenStumpTileId
			: GameConfig.PineStumpTileId;

	public static string SpritePath(int tileId) =>
		tileId == GameConfig.AspenTreeTileId
			? "res://assets/sprites/tiles/aspen_tree.png"
			: "res://assets/sprites/tiles/tree1.png";

	public static int SpriteWidth(int tileId) =>
		IsTallTree(tileId) ? GameConfig.PineTreeSpriteWidth : GameConfig.TileSize;

	public static int SpriteHeight(int tileId) =>
		IsTallTree(tileId) ? GameConfig.PineTreeSpriteHeight : GameConfig.TileSize;

	public static Vector2 DrawTopLeft(int col, int row, int tileId)
	{
		var tileSize = GameConfig.TileSize;
		var spriteW = SpriteWidth(tileId);
		var spriteH = SpriteHeight(tileId);
		return new Vector2(
			col * tileSize + (tileSize - spriteW) / 2f,
			row * tileSize + tileSize - spriteH
		);
	}

	public static float SortY(int row) => (row + 1) * GameConfig.TileSize;

	public static bool ContainsWorldPoint(int col, int row, int tileId, Vector2 world)
	{
		var topLeft = DrawTopLeft(col, row, tileId);
		var size = new Vector2(SpriteWidth(tileId), SpriteHeight(tileId));
		return world.X >= topLeft.X && world.X < topLeft.X + size.X
			&& world.Y >= topLeft.Y && world.Y < topLeft.Y + size.Y;
	}
}
