package lumberjack.world.tile;

import lumberjack.core.GameConfig;
import lumberjack.world.map.TileMap;

/**
 * Picks the correct water shore sprite based on neighboring tiles.
 *
 * <p>Orthogonal bitmask: N=1, S=2, E=4, W=8 (bit set when that neighbor is water).
 * Outer corners use masks 5/6/9/10. Inner corners fill the grass/water gap at a
 * concave land corner: all four orthogonal neighbors are water (those may be
 * edges and/or outer corners), but exactly one diagonal is land.
 */
public final class WaterTileSprites {

    private WaterTileSprites() {
    }

    public static int buildMask(TileMap map, int col, int row) {
        int mask = 0;
        if (isWater(map, col, row - 1)) {
            mask |= 1;
        }
        if (isWater(map, col, row + 1)) {
            mask |= 2;
        }
        if (isWater(map, col + 1, row)) {
            mask |= 4;
        }
        if (isWater(map, col - 1, row)) {
            mask |= 8;
        }
        return mask;
    }

    /**
     * Asset path under {@code tiles/sprites/} without extension, e.g. {@code water_6}
     * or {@code water_inner_6}.
     */
    public static String spriteBaseName(TileMap map, int col, int row) {
        int mask = buildMask(map, col, row);

        // Inner corner: ortho neighbors are all water (edges and/or outer corners),
        // with land on one diagonal so open-water would wrongly touch grass.
        if (mask == 15) {
            String inner = resolveInnerCorner(map, col, row);
            if (inner != null) {
                return inner;
            }
        }

        return "water_" + mask;
    }

    /**
     * Land on a diagonal means that corner of this tile should show the inner-corner art.
     * Orientation matches outer-corner mask numbers (6 = land toward NW / top-left).
     */
    private static String resolveInnerCorner(TileMap map, int col, int row) {
        boolean nwLand = isLand(map, col - 1, row - 1);
        boolean neLand = isLand(map, col + 1, row - 1);
        boolean swLand = isLand(map, col - 1, row + 1);
        boolean seLand = isLand(map, col + 1, row + 1);

        int landDiagonals = (nwLand ? 1 : 0) + (neLand ? 1 : 0) + (swLand ? 1 : 0) + (seLand ? 1 : 0);
        if (landDiagonals != 1) {
            return null;
        }

        if (nwLand) {
            return "water_inner_6";
        }
        if (neLand) {
            return "water_inner_10";
        }
        if (swLand) {
            return "water_inner_5";
        }
        return "water_inner_9";
    }

    private static boolean isLand(TileMap map, int col, int row) {
        if (!map.isInBounds(col, row)) {
            // Outside the map counts as non-water (same as land for shore purposes).
            return true;
        }
        return !isWaterTileId(map.getTileIdAt(col, row));
    }

    private static boolean isWater(TileMap map, int col, int row) {
        if (!map.isInBounds(col, row)) {
            return false;
        }
        return isWaterTileId(map.getTileIdAt(col, row));
    }

    /** Water and bridge overlays both count as water for shore adjacency. */
    public static boolean isWaterTileId(int tileId) {
        return tileId == GameConfig.WATER_TILE_ID
                || tileId == GameConfig.BRIDGE_TILE_ID
                || tileId == GameConfig.BRIDGE_H_TILE_ID;
    }

    public static boolean isBridgeTileId(int tileId) {
        return tileId == GameConfig.BRIDGE_TILE_ID || tileId == GameConfig.BRIDGE_H_TILE_ID;
    }
}
