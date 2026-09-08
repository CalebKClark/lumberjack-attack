package lumberjack.world.placeable;

import lumberjack.core.GameConfig;
import lumberjack.item.ItemDefinition;
import lumberjack.item.ItemRegistry;
import lumberjack.world.map.TileMap;
import lumberjack.world.tile.TileRegistry;

/**
 * Two-tile-tall bed placeable. Anchor tile is the foot (southern tile);
 * the head occupies the tile directly above ({@code row - 1}).
 */
public final class BedPlaceable {

    public static final String ITEM_ID = "bed";
    /** Tile id used by the world editor / legacy map beds. */
    public static final int TILE_ID = 6;
    public static final int HEIGHT_TILES = 2;
    public static final int SPRITE_WIDTH = GameConfig.TILE_SIZE;
    public static final int SPRITE_HEIGHT = GameConfig.TILE_SIZE * HEIGHT_TILES;

    private BedPlaceable() {
    }

    public static boolean isBed(String itemId) {
        return ITEM_ID.equals(itemId);
    }

    public static boolean isBedTile(int tileId) {
        return tileId == TILE_ID;
    }

    public static boolean isBedItem(ItemRegistry itemRegistry, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        if (!itemRegistry.exists(itemId)) {
            return false;
        }
        ItemDefinition item = itemRegistry.get(itemId);
        return item.isPlaceable() && isBed(itemId);
    }

    /** Foot tile row for a bed whose head is at {@code headRow}. */
    public static int footRow(int headRow) {
        return headRow + 1;
    }

    /** Head tile row for a bed anchored at foot {@code footRow}. */
    public static int headRow(int footRow) {
        return footRow - 1;
    }

    public static boolean occupies(int footCol, int footRow, int col, int row) {
        return col == footCol && (row == footRow || row == headRow(footRow));
    }

    public static boolean canPlaceAt(
            TileMap map,
            TileRegistry tileRegistry,
            int footCol,
            int footRow
    ) {
        int head = headRow(footRow);
        if (!map.isInBounds(footCol, footRow) || !map.isInBounds(footCol, head)) {
            return false;
        }
        return isCabinFloor(tileRegistry, map.getTileIdAt(footCol, footRow))
                && isCabinFloor(tileRegistry, map.getTileIdAt(footCol, head));
    }

    public static boolean isCabinFloor(TileRegistry tileRegistry, int tileId) {
        return "cabin_floor".equals(tileRegistry.get(tileId).getName());
    }

    /** True when this bed tile is the foot of a stacked pair (head above). */
    public static boolean isStackedFoot(TileMap map, int col, int row) {
        if (!map.isInBounds(col, row) || !isBedTile(map.getTileIdAt(col, row))) {
            return false;
        }
        int head = headRow(row);
        return map.isInBounds(col, head) && isBedTile(map.getTileIdAt(col, head));
    }

    /** True when this bed tile is the head of a stacked pair (foot below). */
    public static boolean isStackedHead(TileMap map, int col, int row) {
        if (!map.isInBounds(col, row) || !isBedTile(map.getTileIdAt(col, row))) {
            return false;
        }
        int foot = footRow(row);
        return map.isInBounds(col, foot) && isBedTile(map.getTileIdAt(col, foot));
    }
}
