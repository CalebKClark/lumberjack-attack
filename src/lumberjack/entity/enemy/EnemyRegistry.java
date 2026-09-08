package lumberjack.entity.enemy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@link EnemyDefinition} entries from /enemies/enemies.csv.
 */
public final class EnemyRegistry {

    private static final String DEFAULT_PATH = "/enemies/enemies.csv";

    private final Map<String, EnemyDefinition> enemiesById = new HashMap<>();

    public EnemyRegistry() {
        this(DEFAULT_PATH);
    }

    public EnemyRegistry(String resourcePath) {
        load(resourcePath);
    }

    public EnemyDefinition get(String enemyId) {
        EnemyDefinition enemy = enemiesById.get(enemyId);
        if (enemy == null) {
            throw new IllegalArgumentException("Unknown enemy id: " + enemyId);
        }
        return enemy;
    }

    public boolean exists(String enemyId) {
        return enemiesById.containsKey(enemyId);
    }

    private void load(String resourcePath) {
        try (InputStream input = EnemyRegistry.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Enemy data not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = line.split(",");
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    double maxHealth = Double.parseDouble(parts[2].trim());
                    double contactDamage = Double.parseDouble(parts[3].trim());
                    int speed = Integer.parseInt(parts[4].trim());
                    String dropItemId = parts[5].trim();
                    int dropQuantity = Integer.parseInt(parts[6].trim());
                    int width = Integer.parseInt(parts[7].trim());
                    int height = Integer.parseInt(parts[8].trim());
                    int r = Integer.parseInt(parts[9].trim());
                    int g = Integer.parseInt(parts[10].trim());
                    int b = Integer.parseInt(parts[11].trim());
                    double hitFlashMs = Double.parseDouble(parts[12].trim());
                    double contactCooldownMs = Double.parseDouble(parts[13].trim());
                    int combatXp = parts.length > 14 && !parts[14].trim().isEmpty()
                            ? Integer.parseInt(parts[14].trim())
                            : 0;

                    enemiesById.put(id, new EnemyDefinition(
                            id,
                            name,
                            maxHealth,
                            contactDamage,
                            speed,
                            dropItemId,
                            dropQuantity,
                            width,
                            height,
                            new Color(r, g, b),
                            hitFlashMs,
                            contactCooldownMs,
                            combatXp
                    ));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load enemy data: " + resourcePath, exception);
        }
    }
}
