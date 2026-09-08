package lumberjack.world.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads map transition triggers from /maps/transitions.csv.
 */
public final class MapTransitionRegistry {

    private static final String TRANSITIONS_PATH = "/maps/transitions.csv";

    private final Map<String, MapTransition> transitionsByKey = new HashMap<>();

    public MapTransitionRegistry() {
        load();
    }

    public Optional<MapTransition> find(String mapId, int col, int row) {
        return Optional.ofNullable(transitionsByKey.get(key(mapId, col, row)));
    }

    private void load() {
        try (InputStream input = MapTransitionRegistry.class.getResourceAsStream(TRANSITIONS_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Transitions data not found: " + TRANSITIONS_PATH);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = line.split(",");
                    MapTransition transition = new MapTransition(
                            parts[0].trim(),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            parts[3].trim(),
                            Integer.parseInt(parts[4].trim()),
                            Integer.parseInt(parts[5].trim())
                    );

                    transitionsByKey.put(
                            key(transition.getSourceMapId(), transition.getCol(), transition.getRow()),
                            transition
                    );
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load map transitions.", exception);
        }
    }

    private static String key(String mapId, int col, int row) {
        return mapId + "@" + col + "," + row;
    }
}
