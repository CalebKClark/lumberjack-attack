package lumberjack.world.regrowth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lumberjack.core.GameConfig;
import lumberjack.save.SaveData;
import lumberjack.world.tile.TallTreeSprites;
import lumberjack.world.map.TileMap;
import lumberjack.world.map.WorldMapManager;

/**
 * Tracks chopped tiles that should regrow after an in-game delay, per map.
 * Timers advance only while the game clock is running.
 */
public final class TreeRegrowthManager {

    private final Map<String, List<PendingRegrowth>> pendingByMap = new HashMap<>();

    public void schedule(String mapId, int col, int row, int tileId, double regrowthMs) {
        pendingByMap.computeIfAbsent(mapId, ignored -> new ArrayList<>())
                .add(new PendingRegrowth(col, row, tileId, regrowthMs));
    }

    /**
     * @return true if any tile changed on any map
     */
    public boolean updateAll(double elapsedMs, WorldMapManager worldMaps) {
        boolean changed = false;

        for (Map.Entry<String, List<PendingRegrowth>> entry : pendingByMap.entrySet()) {
            TileMap map = worldMaps.getMapOrNull(entry.getKey());
            if (map == null) {
                continue;
            }

            changed |= updateMap(entry.getValue(), elapsedMs, map);
        }

        return changed;
    }

    public void clear() {
        pendingByMap.clear();
    }

    public List<SaveData.RegrowthEntry> exportRegrowths() {
        List<SaveData.RegrowthEntry> entries = new ArrayList<>();

        for (Map.Entry<String, List<PendingRegrowth>> entry : pendingByMap.entrySet()) {
            for (PendingRegrowth regrowth : entry.getValue()) {
                entries.add(new SaveData.RegrowthEntry(
                        entry.getKey(),
                        regrowth.col,
                        regrowth.row,
                        regrowth.tileId,
                        regrowth.remainingMs
                ));
            }
        }

        return entries;
    }

    public void restoreRegrowths(List<SaveData.RegrowthEntry> entries) {
        pendingByMap.clear();

        if (entries == null) {
            return;
        }

        for (SaveData.RegrowthEntry entry : entries) {
            String mapId = entry.getMapId() == null || entry.getMapId().isBlank()
                    ? GameConfig.HOMESTEAD_MAP
                    : entry.getMapId();

            pendingByMap.computeIfAbsent(mapId, ignored -> new ArrayList<>())
                    .add(new PendingRegrowth(
                            entry.getCol(),
                            entry.getRow(),
                            entry.getTileId(),
                            entry.getRemainingMs()
                    ));
        }
    }

    /**
     * Ensures regenerating spots show as stumps (also migrates older grass leftovers).
     *
     * @return true if any tile changed
     */
    public boolean applyStumpTiles(WorldMapManager worldMaps) {
        boolean changed = false;
        for (Map.Entry<String, List<PendingRegrowth>> entry : pendingByMap.entrySet()) {
            TileMap map = worldMaps.getMapOrNull(entry.getKey());
            if (map == null) {
                continue;
            }
            for (PendingRegrowth regrowth : entry.getValue()) {
                if (!map.isInBounds(regrowth.col, regrowth.row)) {
                    continue;
                }
                int current = map.getTileIdAt(regrowth.col, regrowth.row);
                int stumpId = TallTreeSprites.stumpTileIdForTree(regrowth.tileId);
                if (current == GameConfig.GRASS_TILE_ID || TallTreeSprites.isTreeStump(current)) {
                    if (current != stumpId) {
                        map.setTileIdAt(regrowth.col, regrowth.row, stumpId);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private boolean updateMap(List<PendingRegrowth> pending, double elapsedMs, TileMap map) {
        boolean changed = false;
        Iterator<PendingRegrowth> iterator = pending.iterator();

        while (iterator.hasNext()) {
            PendingRegrowth regrowth = iterator.next();
            regrowth.remainingMs -= elapsedMs;

            if (regrowth.remainingMs > 0) {
                continue;
            }

            int current = map.getTileIdAt(regrowth.col, regrowth.row);
            // Matching stump is the normal regenerating tile; grass covers older saves.
            int stumpId = TallTreeSprites.stumpTileIdForTree(regrowth.tileId);
            if (current == stumpId || current == GameConfig.GRASS_TILE_ID || TallTreeSprites.isTreeStump(current)) {
                map.setTileIdAt(regrowth.col, regrowth.row, regrowth.tileId);
                changed = true;
            }

            iterator.remove();
        }

        return changed;
    }

    private static final class PendingRegrowth {

        private final int col;
        private final int row;
        private final int tileId;
        private double remainingMs;

        private PendingRegrowth(int col, int row, int tileId, double remainingMs) {
            this.col = col;
            this.row = row;
            this.tileId = tileId;
            this.remainingMs = remainingMs;
        }
    }
}
