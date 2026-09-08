package lumberjack.world.placeable;

/**
 * Identifies a single placed object on a map tile.
 */
public record PlacedObjectKey(String mapId, int col, int row) {
}
