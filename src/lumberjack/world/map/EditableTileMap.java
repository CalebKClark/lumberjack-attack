package lumberjack.world.map;

/**
 * Mutable tile grid for the world editor. Same on-disk format as gameplay maps,
 * with optional per-cell transfer markers ({@code t} suffix) that are invisible in-game.
 */
public final class EditableTileMap {

    public static final int DEFAULT_FILL_TILE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MIN_SIZE = 1;

    private String mapId;
    private int[][] tiles;
    private int[][] rotations;
    private boolean[][] transitionMarkers;

    public EditableTileMap(String mapId, int width, int height, int fillTileId) {
        this.mapId = mapId;
        this.tiles = new int[height][width];
        this.rotations = new int[height][width];
        this.transitionMarkers = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                tiles[row][col] = fillTileId;
            }
        }
    }

    public EditableTileMap(String mapId, int[][] tiles, int[][] rotations) {
        this(mapId, tiles, rotations, null);
    }

    public EditableTileMap(String mapId, int[][] tiles, int[][] rotations, boolean[][] transitionMarkers) {
        this.mapId = mapId;
        this.tiles = copy(tiles);
        this.rotations = rotations == null ? zerosLike(tiles) : copy(rotations);
        this.transitionMarkers = transitionMarkers == null
                ? boolZerosLike(tiles)
                : copyBool(transitionMarkers);
    }

    public static EditableTileMap createDefault(String mapId) {
        return new EditableTileMap(mapId, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_FILL_TILE);
    }

    public String getMapId() {
        return mapId;
    }

    public void setMapId(String mapId) {
        this.mapId = mapId;
    }

    public int getWidth() {
        return tiles[0].length;
    }

    public int getHeight() {
        return tiles.length;
    }

    public int getTile(int col, int row) {
        return tiles[row][col];
    }

    public int getRotation(int col, int row) {
        return rotations[row][col] & 3;
    }

    public boolean hasTransitionMarker(int col, int row) {
        return transitionMarkers[row][col];
    }

    public void setTransitionMarker(int col, int row, boolean marked) {
        transitionMarkers[row][col] = marked;
    }

    public void setTile(int col, int row, int tileId, int rotation) {
        tiles[row][col] = tileId;
        rotations[row][col] = rotation & 3;
    }

    public boolean inBounds(int col, int row) {
        return col >= 0 && row >= 0 && row < getHeight() && col < getWidth();
    }

    public int[][] copyTiles() {
        return copy(tiles);
    }

    public int[][] copyRotations() {
        return copy(rotations);
    }

    public boolean[][] copyTransitionMarkers() {
        return copyBool(transitionMarkers);
    }

    public void restore(int[][] tileSnapshot, int[][] rotationSnapshot) {
        restore(tileSnapshot, rotationSnapshot, null);
    }

    public void restore(int[][] tileSnapshot, int[][] rotationSnapshot, boolean[][] markerSnapshot) {
        this.tiles = copy(tileSnapshot);
        this.rotations = rotationSnapshot == null ? zerosLike(tileSnapshot) : copy(rotationSnapshot);
        this.transitionMarkers = markerSnapshot == null ? boolZerosLike(tileSnapshot) : copyBool(markerSnapshot);
    }

    public void extendUp(int fillTileId) {
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height + 1][width];
        int[][] nextRot = new int[height + 1][width];
        boolean[][] nextMark = new boolean[height + 1][width];
        for (int col = 0; col < width; col++) {
            nextTiles[0][col] = fillTileId;
        }
        for (int row = 0; row < height; row++) {
            System.arraycopy(tiles[row], 0, nextTiles[row + 1], 0, width);
            System.arraycopy(rotations[row], 0, nextRot[row + 1], 0, width);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row + 1], 0, width);
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
    }

    public void extendDown(int fillTileId) {
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height + 1][width];
        int[][] nextRot = new int[height + 1][width];
        boolean[][] nextMark = new boolean[height + 1][width];
        for (int row = 0; row < height; row++) {
            System.arraycopy(tiles[row], 0, nextTiles[row], 0, width);
            System.arraycopy(rotations[row], 0, nextRot[row], 0, width);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row], 0, width);
        }
        for (int col = 0; col < width; col++) {
            nextTiles[height][col] = fillTileId;
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
    }

    public void extendLeft(int fillTileId) {
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height][width + 1];
        int[][] nextRot = new int[height][width + 1];
        boolean[][] nextMark = new boolean[height][width + 1];
        for (int row = 0; row < height; row++) {
            nextTiles[row][0] = fillTileId;
            System.arraycopy(tiles[row], 0, nextTiles[row], 1, width);
            System.arraycopy(rotations[row], 0, nextRot[row], 1, width);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row], 1, width);
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
    }

    public void extendRight(int fillTileId) {
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height][width + 1];
        int[][] nextRot = new int[height][width + 1];
        boolean[][] nextMark = new boolean[height][width + 1];
        for (int row = 0; row < height; row++) {
            System.arraycopy(tiles[row], 0, nextTiles[row], 0, width);
            System.arraycopy(rotations[row], 0, nextRot[row], 0, width);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row], 0, width);
            nextTiles[row][width] = fillTileId;
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
    }

    /** @return false if the map is already at minimum height */
    public boolean shrinkUp() {
        if (getHeight() <= MIN_SIZE) {
            return false;
        }
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height - 1][width];
        int[][] nextRot = new int[height - 1][width];
        boolean[][] nextMark = new boolean[height - 1][width];
        for (int row = 1; row < height; row++) {
            System.arraycopy(tiles[row], 0, nextTiles[row - 1], 0, width);
            System.arraycopy(rotations[row], 0, nextRot[row - 1], 0, width);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row - 1], 0, width);
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
        return true;
    }

    /** @return false if the map is already at minimum height */
    public boolean shrinkDown() {
        if (getHeight() <= MIN_SIZE) {
            return false;
        }
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height - 1][width];
        int[][] nextRot = new int[height - 1][width];
        boolean[][] nextMark = new boolean[height - 1][width];
        for (int row = 0; row < height - 1; row++) {
            System.arraycopy(tiles[row], 0, nextTiles[row], 0, width);
            System.arraycopy(rotations[row], 0, nextRot[row], 0, width);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row], 0, width);
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
        return true;
    }

    /** @return false if the map is already at minimum width */
    public boolean shrinkLeft() {
        if (getWidth() <= MIN_SIZE) {
            return false;
        }
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height][width - 1];
        int[][] nextRot = new int[height][width - 1];
        boolean[][] nextMark = new boolean[height][width - 1];
        for (int row = 0; row < height; row++) {
            System.arraycopy(tiles[row], 1, nextTiles[row], 0, width - 1);
            System.arraycopy(rotations[row], 1, nextRot[row], 0, width - 1);
            System.arraycopy(transitionMarkers[row], 1, nextMark[row], 0, width - 1);
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
        return true;
    }

    /** @return false if the map is already at minimum width */
    public boolean shrinkRight() {
        if (getWidth() <= MIN_SIZE) {
            return false;
        }
        int width = getWidth();
        int height = getHeight();
        int[][] nextTiles = new int[height][width - 1];
        int[][] nextRot = new int[height][width - 1];
        boolean[][] nextMark = new boolean[height][width - 1];
        for (int row = 0; row < height; row++) {
            System.arraycopy(tiles[row], 0, nextTiles[row], 0, width - 1);
            System.arraycopy(rotations[row], 0, nextRot[row], 0, width - 1);
            System.arraycopy(transitionMarkers[row], 0, nextMark[row], 0, width - 1);
        }
        tiles = nextTiles;
        rotations = nextRot;
        transitionMarkers = nextMark;
        return true;
    }

    public String toFileContents() {
        StringBuilder builder = new StringBuilder();
        builder.append(getWidth()).append(' ').append(getHeight()).append('\n');
        for (int row = 0; row < getHeight(); row++) {
            for (int col = 0; col < getWidth(); col++) {
                if (col > 0) {
                    builder.append(' ');
                }
                builder.append(MapCellCodec.format(
                        tiles[row][col],
                        rotations[row][col],
                        transitionMarkers[row][col]
                ));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static int[][] zerosLike(int[][] source) {
        int[][] zeros = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            zeros[row] = new int[source[row].length];
        }
        return zeros;
    }

    private static boolean[][] boolZerosLike(int[][] source) {
        boolean[][] zeros = new boolean[source.length][];
        for (int row = 0; row < source.length; row++) {
            zeros[row] = new boolean[source[row].length];
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

    private static boolean[][] copyBool(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }
}
