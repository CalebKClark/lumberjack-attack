package lumberjack.world.regrowth;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks remaining toughness on living trees between chops.
 * Missing entries mean the tree is at full toughness.
 */
public final class TreeHitTracker {

    private final Map<String, Integer> remainingToughness = new HashMap<>();

    /**
     * Applies axe tree-damage and returns remaining toughness after the hit (0 = felled).
     */
    public int applyHit(String mapId, int col, int row, int maxToughness, int treeDamage) {
        String key = key(mapId, col, row);
        int remaining = remainingToughness.getOrDefault(key, maxToughness);
        remaining = Math.max(0, remaining - Math.max(1, treeDamage));
        if (remaining <= 0) {
            remainingToughness.remove(key);
            return 0;
        }
        remainingToughness.put(key, remaining);
        return remaining;
    }

    public void clear(String mapId, int col, int row) {
        remainingToughness.remove(key(mapId, col, row));
    }

    public void clearAll() {
        remainingToughness.clear();
    }

    private static String key(String mapId, int col, int row) {
        return mapId + ':' + col + ':' + row;
    }
}
