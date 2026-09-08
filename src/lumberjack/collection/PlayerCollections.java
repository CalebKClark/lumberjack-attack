package lumberjack.collection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lumberjack.fishing.FishSpawnDefinition;
import lumberjack.fishing.FishSpawnRegistry;
import lumberjack.save.SaveData;

/**
 * Tracks which collection entries the player has discovered.
 */
public final class PlayerCollections {

    private final CollectionRegistry registry;
    private final Set<String> discoveredEntryIds = new HashSet<>();

    public PlayerCollections(CollectionRegistry registry) {
        this.registry = registry;
    }

    public void reset() {
        discoveredEntryIds.clear();
    }

    public boolean isDiscovered(String entryId) {
        return discoveredEntryIds.contains(entryId);
    }

    public int getDiscoveredCount(CollectionCategory category) {
        int count = 0;
        for (CollectionEntryDefinition entry : registry.getEntries(category)) {
            if (isDiscovered(entry.getEntryId())) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount(CollectionCategory category) {
        return registry.getTotalCount(category);
    }

    public void onItemAcquired(String itemId) {
        for (CollectionEntryDefinition entry : registry.getEntriesForItem(itemId)) {
            discoveredEntryIds.add(entry.getEntryId());
        }
    }

    /**
     * True if the player has discovered this item in the Collections tab.
     */
    public boolean hasDiscoveredItem(String itemId) {
        for (CollectionEntryDefinition entry : registry.getEntriesForItem(itemId)) {
            if (isDiscovered(entry.getEntryId())) {
                return true;
            }
        }
        return false;
    }

    public void onEnemyDefeated(String enemyId) {
        for (CollectionEntryDefinition entry : registry.getEntriesForEnemy(enemyId)) {
            discoveredEntryIds.add(entry.getEntryId());
        }
    }

    public String getTooltipText(
            CollectionEntryDefinition entry,
            FishSpawnRegistry fishSpawnRegistry
    ) {
        if (!isDiscovered(entry.getEntryId())) {
            return "";
        }

        StringBuilder tooltip = new StringBuilder(entry.getTooltip());
        if (entry.usesFishStats() && entry.getDiscoveryId() != null) {
            try {
                FishSpawnDefinition fish = fishSpawnRegistry.getByItemId(entry.getDiscoveryId());
                if (!tooltip.isEmpty()) {
                    tooltip.append(' ');
                }
                tooltip.append("Speed: ")
                        .append(fish.getSpeed())
                        .append(", Weight: ")
                        .append(trimWeight(fish.getWeight()))
                        .append(", Min depth: ")
                        .append((int) fish.getMinDepth());
            } catch (IllegalArgumentException ignored) {
                // Fish spawn data may not exist yet for this entry.
            }
        }
        return tooltip.toString();
    }

    public void exportTo(SaveData data) {
        data.getCollectionEntries().clear();
        for (String entryId : discoveredEntryIds) {
            data.getCollectionEntries().add(new SaveData.CollectionEntry(entryId));
        }
    }

    public void importFrom(SaveData data) {
        reset();
        for (SaveData.CollectionEntry entry : data.getCollectionEntries()) {
            discoveredEntryIds.add(entry.getEntryId());
        }
    }

    private static String trimWeight(double weight) {
        if (weight == (long) weight) {
            return String.valueOf((long) weight);
        }
        return String.valueOf(weight);
    }
}
