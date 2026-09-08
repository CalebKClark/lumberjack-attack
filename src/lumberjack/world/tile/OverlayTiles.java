package lumberjack.world.tile;

import lumberjack.core.GameConfig;

/**
 * Tiles that draw as a sprite over a base terrain tile (bridge over water, stump over grass).
 */
public final class OverlayTiles {

    private OverlayTiles() {
    }

    public static boolean isGrassOverlay(int tileId) {
        return TallTreeSprites.isTreeStump(tileId)
                || TallTreeSprites.isTallTree(tileId);
    }
}
