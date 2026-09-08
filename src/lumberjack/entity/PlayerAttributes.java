package lumberjack.entity;

import lumberjack.core.GameConfig;

/**
 * Persistent player attributes (not gear-derived). Stored in the save file.
 */
public final class PlayerAttributes {

    /** Pickup magnet radius in tiles (gap between player and drop hitboxes). */
    private double pickupMagnetTiles = GameConfig.DEFAULT_PICKUP_MAGNET_TILES;

    public double getPickupMagnetTiles() {
        return pickupMagnetTiles;
    }

    public void setPickupMagnetTiles(double pickupMagnetTiles) {
        this.pickupMagnetTiles = Math.max(0, pickupMagnetTiles);
    }

    public double getPickupMagnetPixels() {
        return pickupMagnetTiles * GameConfig.TILE_SIZE;
    }

    public void resetDefaults() {
        pickupMagnetTiles = GameConfig.DEFAULT_PICKUP_MAGNET_TILES;
    }
}
