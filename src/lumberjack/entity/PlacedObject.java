package lumberjack.entity;

import lumberjack.core.GameConfig;
import lumberjack.world.placeable.BedPlaceable;

/**
 * A placeable item sitting on a tile (e.g. wood chipper on cabin floor).
 * Beds occupy two tiles tall; anchor col/row is the foot.
 */
public final class PlacedObject extends Entity {

    private final String mapId;
    private final String itemId;
    private final int col;
    private final int row;

    public PlacedObject(String mapId, String itemId, int col, int row) {
        super(originX(itemId, col), originY(itemId, row), width(itemId), height(itemId));
        this.mapId = mapId;
        this.itemId = itemId;
        this.col = col;
        this.row = row;
    }

    private static int originX(String itemId, int col) {
        if (BedPlaceable.isBed(itemId)) {
            return col * GameConfig.TILE_SIZE;
        }
        return col * GameConfig.TILE_SIZE + (GameConfig.TILE_SIZE - GameConfig.PLACEABLE_HITBOX_SIZE) / 2;
    }

    private static int originY(String itemId, int row) {
        if (BedPlaceable.isBed(itemId)) {
            // Foot at {@code row}; sprite extends upward into the head tile.
            return BedPlaceable.headRow(row) * GameConfig.TILE_SIZE;
        }
        return row * GameConfig.TILE_SIZE + (GameConfig.TILE_SIZE - GameConfig.PLACEABLE_HITBOX_SIZE) / 2;
    }

    private static int width(String itemId) {
        if (BedPlaceable.isBed(itemId)) {
            return BedPlaceable.SPRITE_WIDTH;
        }
        return GameConfig.PLACEABLE_HITBOX_SIZE;
    }

    private static int height(String itemId) {
        if (BedPlaceable.isBed(itemId)) {
            return BedPlaceable.SPRITE_HEIGHT;
        }
        return GameConfig.PLACEABLE_HITBOX_SIZE;
    }

    public String getMapId() {
        return mapId;
    }

    public String getItemId() {
        return itemId;
    }

    /** Anchor column (foot for beds). */
    public int getCol() {
        return col;
    }

    /** Anchor row (foot for beds). */
    public int getRow() {
        return row;
    }

    public boolean occupiesTile(int tileCol, int tileRow) {
        if (BedPlaceable.isBed(itemId)) {
            return BedPlaceable.occupies(col, row, tileCol, tileRow);
        }
        return col == tileCol && row == tileRow;
    }

    public boolean blocksMovement() {
        return !BedPlaceable.isBed(itemId);
    }
}
