package lumberjack.fishing;

/**
 * Minigame spawn data for a catchable fish or trash object.
 */
public final class FishSpawnDefinition {

    public enum SpawnKind {
        FISH,
        TRASH
    }

    private final String itemId;
    private final double weight;
    private final double minDepth;
    private final int speed;
    private final SpawnKind spawnKind;

    public FishSpawnDefinition(
            String itemId,
            double weight,
            double minDepth,
            int speed,
            SpawnKind spawnKind
    ) {
        this.itemId = itemId;
        this.weight = weight;
        this.minDepth = minDepth;
        this.speed = speed;
        this.spawnKind = spawnKind;
    }

    public String getItemId() {
        return itemId;
    }

    public double getWeight() {
        return weight;
    }

    public double getMinDepth() {
        return minDepth;
    }

    public int getSpeed() {
        return speed;
    }

    public SpawnKind getSpawnKind() {
        return spawnKind;
    }

    public boolean isTrash() {
        return spawnKind == SpawnKind.TRASH;
    }
}
