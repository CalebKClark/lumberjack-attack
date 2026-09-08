package lumberjack.world.tile;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lumberjack.data.CsvLoader;

/**
 * Loads and provides lookup for all tile types defined in /tiles/tiles.csv.
 */
public final class TileRegistry {

    private static final String DEFAULT_TILES_PATH = "/tiles/tiles.csv";

    private final Map<Integer, TileDefinition> tilesById = new HashMap<>();

    public TileRegistry() {
        this(DEFAULT_TILES_PATH);
    }

    public TileRegistry(String resourcePath) {
        load(resourcePath);
    }

    public TileDefinition get(int tileId) {
        TileDefinition tile = tilesById.get(tileId);
        if (tile == null) {
            throw new IllegalArgumentException("Unknown tile id: " + tileId);
        }
        return tile;
    }

    public boolean isSolid(int tileId) {
        return get(tileId).isSolid();
    }

    public boolean exists(int tileId) {
        return tilesById.containsKey(tileId);
    }

    /** All tiles sorted by id — used by the world editor palette. */
    public List<TileDefinition> getAllSorted() {
        List<TileDefinition> tiles = new ArrayList<>(tilesById.values());
        tiles.sort(Comparator.comparingInt(TileDefinition::getId));
        return tiles;
    }

    private void load(String resourcePath) {
        for (String[] row : CsvLoader.readRows(resourcePath)) {
            int id = CsvLoader.parseInt(row, 0, -1);
            if (id < 0) {
                continue;
            }

            String name = CsvLoader.cell(row, 1);
            boolean solid = CsvLoader.parseBoolean(row, 2, false);
            int r = CsvLoader.parseInt(row, 3, 0);
            int g = CsvLoader.parseInt(row, 4, 0);
            int b = CsvLoader.parseInt(row, 5, 0);
            boolean choppable = CsvLoader.parseBoolean(row, 6, false);
            String dropItemId = CsvLoader.cell(row, 7);
            if (dropItemId.isEmpty()) {
                dropItemId = null;
            }
            int regrowthGameMinutes = CsvLoader.parseInt(row, 8, 0);
            boolean bed = CsvLoader.parseBoolean(row, 9, false);
            boolean placeable = CsvLoader.parseBoolean(row, 10, false);
            boolean fishable = CsvLoader.parseBoolean(row, 11, false);
            int foragingXp = CsvLoader.parseInt(row, 12, 0);
            int toughness = CsvLoader.parseInt(row, 13, 0);
            int dropQuantityMin = CsvLoader.parseInt(row, 14, 0);
            int dropQuantityMax = CsvLoader.parseInt(row, 15, 0);

            tilesById.put(id, new TileDefinition(
                    id,
                    name,
                    solid,
                    new Color(r, g, b),
                    choppable,
                    dropItemId,
                    regrowthGameMinutes,
                    bed,
                    placeable,
                    fishable,
                    foragingXp,
                    toughness,
                    dropQuantityMin,
                    dropQuantityMax
            ));
        }
    }
}
