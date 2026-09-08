package lumberjack.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lumberjack.data.CsvLoader;
import lumberjack.fishing.FishSpawnDefinition.SpawnKind;

/**
 * Loads fish/trash spawn stats from /fishing/fish.csv for the fishing minigame.
 */
public final class FishSpawnRegistry {

    private static final String DEFAULT_PATH = "/fishing/fish.csv";

    private final List<FishSpawnDefinition> spawns = new ArrayList<>();

    public FishSpawnRegistry() {
        this(DEFAULT_PATH);
    }

    public FishSpawnRegistry(String resourcePath) {
        load(resourcePath);
    }

    public List<FishSpawnDefinition> getAllSpawns() {
        return List.copyOf(spawns);
    }

    public FishSpawnDefinition getByItemId(String itemId) {
        for (FishSpawnDefinition definition : spawns) {
            if (definition.getItemId().equals(itemId)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Unknown fish spawn id: " + itemId);
    }

    public List<FishSpawnDefinition> getEligibleFishAtDepth(double depth) {
        return getEligibleAtDepth(depth, SpawnKind.FISH);
    }

    public FishSpawnDefinition rollFishAtDepth(double depth, ThreadLocalRandom random) {
        return rollWeighted(getEligibleFishAtDepth(depth), random);
    }

    public FishSpawnDefinition rollTrash(ThreadLocalRandom random) {
        List<FishSpawnDefinition> trash = getByKind(SpawnKind.TRASH);
        if (trash.isEmpty()) {
            throw new IllegalStateException("No trash entries defined in fish.csv");
        }
        return rollWeighted(trash, random);
    }

    private List<FishSpawnDefinition> getEligibleAtDepth(double depth, SpawnKind kind) {
        List<FishSpawnDefinition> eligible = new ArrayList<>();
        for (FishSpawnDefinition definition : spawns) {
            if (definition.getSpawnKind() == kind && depth >= definition.getMinDepth()) {
                eligible.add(definition);
            }
        }
        return eligible;
    }

    private List<FishSpawnDefinition> getByKind(SpawnKind kind) {
        List<FishSpawnDefinition> matches = new ArrayList<>();
        for (FishSpawnDefinition definition : spawns) {
            if (definition.getSpawnKind() == kind) {
                matches.add(definition);
            }
        }
        return matches;
    }

    private FishSpawnDefinition rollWeighted(List<FishSpawnDefinition> eligible, ThreadLocalRandom random) {
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No eligible fish spawns for roll");
        }

        double totalWeight = 0;
        for (FishSpawnDefinition definition : eligible) {
            totalWeight += definition.getWeight();
        }

        double roll = random.nextDouble(totalWeight);
        for (FishSpawnDefinition definition : eligible) {
            roll -= definition.getWeight();
            if (roll <= 0) {
                return definition;
            }
        }

        return eligible.get(eligible.size() - 1);
    }

    private void load(String resourcePath) {
        for (String[] row : CsvLoader.readRows(resourcePath)) {
            String itemId = CsvLoader.cell(row, 0);
            if (itemId.isEmpty()) {
                continue;
            }

            double weight = CsvLoader.parseDouble(row, 1, 1.0);
            double minDepth = CsvLoader.parseDouble(row, 2, 0);
            int speed = CsvLoader.parseInt(row, 3, 0);
            SpawnKind kind = parseKind(CsvLoader.cell(row, 4));

            spawns.add(new FishSpawnDefinition(itemId, weight, minDepth, speed, kind));
        }
    }

    private SpawnKind parseKind(String raw) {
        if (raw.equalsIgnoreCase("trash")) {
            return SpawnKind.TRASH;
        }
        return SpawnKind.FISH;
    }
}
