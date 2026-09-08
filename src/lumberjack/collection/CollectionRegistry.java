package lumberjack.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads collection entries from /collections/collections.csv.
 */
public final class CollectionRegistry {

    private static final String DEFAULT_PATH = "/collections/collections.csv";

    private final Map<String, CollectionEntryDefinition> entriesById = new HashMap<>();
    private final Map<CollectionCategory, List<CollectionEntryDefinition>> entriesByCategory = new EnumMap<>(CollectionCategory.class);

    public CollectionRegistry() {
        this(DEFAULT_PATH);
    }

    public CollectionRegistry(String resourcePath) {
        for (CollectionCategory category : CollectionCategory.values()) {
            entriesByCategory.put(category, new ArrayList<>());
        }
        load(resourcePath);
    }

    public CollectionEntryDefinition get(String entryId) {
        CollectionEntryDefinition entry = entriesById.get(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown collection entry: " + entryId);
        }
        return entry;
    }

    public List<CollectionEntryDefinition> getEntries(CollectionCategory category) {
        return List.copyOf(entriesByCategory.get(category));
    }

    public int getTotalCount(CollectionCategory category) {
        return entriesByCategory.get(category).size();
    }

    public List<CollectionEntryDefinition> getEntriesForItem(String itemId) {
        List<CollectionEntryDefinition> matches = new ArrayList<>();
        for (CollectionEntryDefinition entry : entriesById.values()) {
            if (entry.getDiscoveryType() == DiscoveryType.ITEM && itemId.equals(entry.getDiscoveryId())) {
                matches.add(entry);
            }
        }
        return matches;
    }

    public List<CollectionEntryDefinition> getEntriesForEnemy(String enemyId) {
        List<CollectionEntryDefinition> matches = new ArrayList<>();
        for (CollectionEntryDefinition entry : entriesById.values()) {
            if (entry.getDiscoveryType() == DiscoveryType.ENEMY && enemyId.equals(entry.getDiscoveryId())) {
                matches.add(entry);
            }
        }
        return matches;
    }

    private void load(String resourcePath) {
        try (InputStream input = CollectionRegistry.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Collection data not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                reader.readLine(); // header

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] parts = parseCsvLine(line);
                    CollectionCategory category = CollectionCategory.fromId(parts[0].trim());
                    String entryId = parts[1].trim();
                    String displayName = parts[2].trim();
                    DiscoveryType discoveryType = DiscoveryType.valueOf(parts[3].trim().toUpperCase());
                    String discoveryId = parts[4].trim();
                    String tooltip = parts[5].trim();
                    int sortOrder = Integer.parseInt(parts[6].trim());

                    CollectionEntryDefinition entry = new CollectionEntryDefinition(
                            category,
                            entryId,
                            displayName,
                            discoveryType,
                            discoveryId.isEmpty() ? null : discoveryId,
                            tooltip,
                            sortOrder
                    );

                    entriesById.put(entryId, entry);
                    entriesByCategory.get(category).add(entry);
                }
            }

            for (CollectionCategory category : CollectionCategory.values()) {
                entriesByCategory.get(category).sort(Comparator.comparingInt(CollectionEntryDefinition::getSortOrder));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load collection data: " + resourcePath, exception);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (character == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        parts.add(current.toString());
        return parts.toArray(String[]::new);
    }
}
