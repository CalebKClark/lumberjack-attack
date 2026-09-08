package lumberjack.world.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lumberjack.core.GameConfig;
import lumberjack.entity.Player;

/**
 * Keeps tile state for every loaded map and tracks which map is active.
 */
public final class WorldMapManager {

    private final Map<String, TileMap> maps = new HashMap<>();
    private String currentMapId;

    public WorldMapManager(String startingMapId) {
        ensureMapLoaded(startingMapId);
        currentMapId = startingMapId;
    }

    public TileMap getCurrentMap() {
        return maps.get(currentMapId);
    }

    public String getCurrentMapId() {
        return currentMapId;
    }

    public Collection<TileMap> getAllMaps() {
        return maps.values();
    }

    public void ensureMapLoaded(String mapId) {
        maps.computeIfAbsent(mapId, MapLoader::load);
    }

    public TileMap getMapOrNull(String mapId) {
        return maps.get(mapId);
    }

    public void switchToMap(String mapId) {
        ensureMapLoaded(mapId);
        currentMapId = mapId;
    }

    public void resetMap(String mapId) {
        maps.put(mapId, MapLoader.load(mapId));
    }

    public void resetMaps(String... mapIds) {
        maps.clear();
        for (String mapId : mapIds) {
            resetMap(mapId);
        }
        currentMapId = GameConfig.STARTING_MAP;
    }

    public void replaceMapTiles(String mapId, int[][] tiles) {
        replaceMapTiles(mapId, tiles, null);
    }

    public void replaceMapTiles(String mapId, int[][] tiles, int[][] rotations) {
        ensureMapLoaded(mapId);
        TileMap current = maps.get(mapId);
        // Map was resized in world-edit — drop stale save tiles and use the file on disk.
        if (tiles.length != current.getHeightInTiles() || tiles[0].length != current.getWidthInTiles()) {
            resetMap(mapId);
            return;
        }
        current.replaceTiles(tiles, rotations);
    }

    public static void placePlayerAtTile(Player player, TileMap map, int col, int row) {
        int tileSize = GameConfig.TILE_SIZE;
        int x = col * tileSize + (tileSize - player.getWidth()) / 2;
        int y = row * tileSize + (tileSize - player.getHeight()) / 2;
        player.setPosition(x, y);
    }
}
