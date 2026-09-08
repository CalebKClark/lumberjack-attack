package lumberjack.world.tile;

import lumberjack.core.GameConfig;

/**
 * Layout helpers for tall tree sprites drawn above a stump tile.
 * Sprite bottom is aligned to the stump tile bottom; horizontally centered.
 */
public final class TallTreeSprites {

    private TallTreeSprites() {
    }

    public static boolean isTallTree(int tileId) {
        return tileId == GameConfig.PINE_TREE_TILE_ID;
    }

    public static boolean isTreeStump(int tileId) {
        return tileId == GameConfig.TREE_STUMP_TILE_ID;
    }

    /** Stump tile used while this tree is regenerating. */
    public static int stumpTileIdForTree(int treeTileId) {
        return GameConfig.TREE_STUMP_TILE_ID;
    }

    /** Asset path under {@code tiles/sprites/}. */
    public static String spritePath(int tileId) {
        return "tiles/sprites/tree1.png";
    }

    public static int spriteWidth(int tileId) {
        if (isTallTree(tileId)) {
            return GameConfig.PINE_TREE_SPRITE_WIDTH;
        }
        return GameConfig.TILE_SIZE;
    }

    public static int spriteHeight(int tileId) {
        if (isTallTree(tileId)) {
            return GameConfig.PINE_TREE_SPRITE_HEIGHT;
        }
        return GameConfig.TILE_SIZE;
    }

    /** World X of the sprite's top-left. */
    public static float drawX(int col, int tileId) {
        int tileSize = GameConfig.TILE_SIZE;
        int spriteW = spriteWidth(tileId);
        return col * tileSize + (tileSize - spriteW) / 2f;
    }

    /** World Y of the sprite's top-left (Y increases downward). */
    public static float drawY(int row, int tileId) {
        int tileSize = GameConfig.TILE_SIZE;
        int spriteH = spriteHeight(tileId);
        return row * tileSize + tileSize - spriteH;
    }

    /** Foot Y used for walk-behind sorting (bottom of stump tile). */
    public static int sortY(int row) {
        return (row + 1) * GameConfig.TILE_SIZE;
    }

    public static boolean containsWorldPoint(int col, int row, int tileId, int worldX, int worldY) {
        float x = drawX(col, tileId);
        float y = drawY(row, tileId);
        float w = spriteWidth(tileId);
        float h = spriteHeight(tileId);
        return worldX >= x && worldX < x + w && worldY >= y && worldY < y + h;
    }
}
