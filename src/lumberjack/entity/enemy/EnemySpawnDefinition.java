package lumberjack.entity.enemy;

/**
 * A default spawn point for an enemy on a map. Loaded from /enemies/spawns.csv.
 */
public final class EnemySpawnDefinition {

    private final String mapId;
    private final String enemyId;
    private final int col;
    private final int row;

    public EnemySpawnDefinition(String mapId, String enemyId, int col, int row) {
        this.mapId = mapId;
        this.enemyId = enemyId;
        this.col = col;
        this.row = row;
    }

    public String getMapId() {
        return mapId;
    }

    public String getEnemyId() {
        return enemyId;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }
}
