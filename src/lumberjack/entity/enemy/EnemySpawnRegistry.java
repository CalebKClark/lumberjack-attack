package lumberjack.entity.enemy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads default enemy spawn points from /enemies/spawns.csv.
 */
public final class EnemySpawnRegistry {

    private static final String DEFAULT_PATH = "/enemies/spawns.csv";

    private final List<EnemySpawnDefinition> spawns = new ArrayList<>();

    public EnemySpawnRegistry() {
        this(DEFAULT_PATH);
    }

    public EnemySpawnRegistry(String resourcePath) {
        load(resourcePath);
    }

    public List<EnemySpawnDefinition> getSpawns() {
        return List.copyOf(spawns);
    }

    private void load(String resourcePath) {
        try (InputStream input = EnemySpawnRegistry.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Enemy spawn data not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = line.split(",");
                    spawns.add(new EnemySpawnDefinition(
                            parts[0].trim(),
                            parts[1].trim(),
                            Integer.parseInt(parts[2].trim()),
                            Integer.parseInt(parts[3].trim())
                    ));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load enemy spawn data: " + resourcePath, exception);
        }
    }
}
