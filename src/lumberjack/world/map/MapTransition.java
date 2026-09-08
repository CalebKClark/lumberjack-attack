package lumberjack.world.map;

/**
 * A doorway or trigger tile that moves the player to another map.
 */
public final class MapTransition {

    private final String sourceMapId;
    private final int col;
    private final int row;
    private final String targetMapId;
    private final int spawnCol;
    private final int spawnRow;

    public MapTransition(
            String sourceMapId,
            int col,
            int row,
            String targetMapId,
            int spawnCol,
            int spawnRow
    ) {
        this.sourceMapId = sourceMapId;
        this.col = col;
        this.row = row;
        this.targetMapId = targetMapId;
        this.spawnCol = spawnCol;
        this.spawnRow = spawnRow;
    }

    public String getSourceMapId() {
        return sourceMapId;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public String getTargetMapId() {
        return targetMapId;
    }

    public int getSpawnCol() {
        return spawnCol;
    }

    public int getSpawnRow() {
        return spawnRow;
    }
}
