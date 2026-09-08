package lumberjack.world.map;

import java.awt.Rectangle;

import lumberjack.core.GameConfig;
import lumberjack.world.tile.TileRegistry;

/**
 * A single area map made of tile ids (and optional per-cell rotation).
 * Collision and dimensions live here so entities do not need to know map file layout.
 */
public final class TileMap {

    private final String mapId;
    private final int[][] tiles;
    private final int[][] rotations;

    public TileMap(String mapId, int[][] tiles) {
        this(mapId, tiles, null);
    }

    public TileMap(String mapId, int[][] tiles, int[][] rotations) {
        this.mapId = mapId;
        this.tiles = tiles;
        this.rotations = rotations == null ? zerosLike(tiles) : rotations;
    }

    public String getMapId() {
        return mapId;
    }

    public int getWidthInTiles() {
        return tiles[0].length;
    }

    public int getHeightInTiles() {
        return tiles.length;
    }

    public int getTileIdAt(int col, int row) {
        return tiles[row][col];
    }

    public int getRotationAt(int col, int row) {
        return rotations[row][col] & 3;
    }

    public void setTileIdAt(int col, int row, int tileId) {
        if (!isInBounds(col, row)) {
            throw new IndexOutOfBoundsException("Tile out of bounds: " + col + ", " + row);
        }
        tiles[row][col] = tileId;
        rotations[row][col] = 0;
    }

    public void setTileAt(int col, int row, int tileId, int rotation) {
        if (!isInBounds(col, row)) {
            throw new IndexOutOfBoundsException("Tile out of bounds: " + col + ", " + row);
        }
        tiles[row][col] = tileId;
        rotations[row][col] = rotation & 3;
    }

    public boolean isInBounds(int col, int row) {
        return row >= 0 && row < tiles.length && col >= 0 && col < tiles[row].length;
    }

    public int getTileCenterX(int col) {
        return col * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
    }

    public int getTileCenterY(int row) {
        return row * GameConfig.TILE_SIZE + GameConfig.TILE_SIZE / 2;
    }

    public int getColumnAtWorldX(int worldX) {
        return Math.floorDiv(worldX, GameConfig.TILE_SIZE);
    }

    public int getRowAtWorldY(int worldY) {
        return Math.floorDiv(worldY, GameConfig.TILE_SIZE);
    }

    public int[][] copyTiles() {
        return copy(tiles);
    }

    public int[][] copyRotations() {
        return copy(rotations);
    }

    public void replaceTiles(int[][] newTiles) {
        replaceTiles(newTiles, null);
    }

    public void replaceTiles(int[][] newTiles, int[][] newRotations) {
        if (newTiles.length != tiles.length || newTiles[0].length != tiles[0].length) {
            throw new IllegalArgumentException("Saved map size does not match current map.");
        }

        for (int row = 0; row < tiles.length; row++) {
            System.arraycopy(newTiles[row], 0, tiles[row], 0, tiles[row].length);
            if (newRotations != null) {
                System.arraycopy(newRotations[row], 0, rotations[row], 0, rotations[row].length);
            } else {
                for (int col = 0; col < rotations[row].length; col++) {
                    rotations[row][col] = 0;
                }
            }
        }
    }

    public boolean collidesWith(Rectangle bounds, TileRegistry tileRegistry) {
        int tileSize = GameConfig.TILE_SIZE;

        int startCol = Math.floorDiv(bounds.x, tileSize);
        int endCol = Math.floorDiv(bounds.x + bounds.width - 1, tileSize);
        int startRow = Math.floorDiv(bounds.y, tileSize);
        int endRow = Math.floorDiv(bounds.y + bounds.height - 1, tileSize);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (!isInBounds(col, row)) {
                    return true;
                }

                int tileId = tiles[row][col];
                if (tileRegistry.isSolid(tileId)) {
                    Rectangle tileBounds = new Rectangle(
                            col * tileSize,
                            row * tileSize,
                            tileSize,
                            tileSize
                    );

                    if (bounds.intersects(tileBounds)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public int getWidthInPixels() {
        return getWidthInTiles() * GameConfig.TILE_SIZE;
    }

    public int getHeightInPixels() {
        return getHeightInTiles() * GameConfig.TILE_SIZE;
    }

    private static int[][] zerosLike(int[][] source) {
        int[][] zeros = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            zeros[row] = new int[source[row].length];
        }
        return zeros;
    }

    private static int[][] copy(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }
}
