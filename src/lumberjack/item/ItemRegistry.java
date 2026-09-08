package lumberjack.item;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import lumberjack.combat.StatModifiers;

/**
 * Loads every {@link ItemDefinition} from /items/items.csv and provides lookup by id.
 * Same pattern as {@link lumberjack.world.tile.TileRegistry}.
 */
public final class ItemRegistry {

    private static final String DEFAULT_ITEMS_PATH = "/items/items.csv";

    private final Map<String, ItemDefinition> itemsById = new HashMap<>();

    public ItemRegistry() {
        this(DEFAULT_ITEMS_PATH);
    }

    public ItemRegistry(String resourcePath) {
        load(resourcePath);
    }

    public ItemDefinition get(String itemId) {
        ItemDefinition item = itemsById.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown item id: " + itemId);
        }
        return item;
    }

    public boolean exists(String itemId) {
        return itemsById.containsKey(itemId);
    }

    private void load(String resourcePath) {
        try (InputStream input = ItemRegistry.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Item data not found: " + resourcePath);
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
                    ItemCategory category = ItemCategory.valueOf(parts[2].trim().toUpperCase());
                    int maxStack = parts.length > 3 && !parts[3].trim().isEmpty()
                            ? Integer.parseInt(parts[3].trim())
                            : lumberjack.core.GameConfig.DEFAULT_ITEM_STACK_SIZE;
                    int r = Integer.parseInt(parts[4].trim());
                    int g = Integer.parseInt(parts[5].trim());
                    int b = Integer.parseInt(parts[6].trim());
                    ToolType toolType = parts.length > 7
                            ? parseToolType(parts[7].trim())
                            : ToolType.NONE;
                    StatModifiers statModifiers = parseStatModifiers(parts);
                    int fishingXp = parseOptionalInt(parts, 14);
                    int treeDamage = parseOptionalInt(parts, 15);
                    int foodHealth = parseOptionalInt(parts, 16);
                    int foodEnergy = parseOptionalInt(parts, 17);

                    itemsById.put(id, new ItemDefinition(
                            id,
                            name,
                            category,
                            maxStack,
                            new Color(r, g, b),
                            toolType,
                            statModifiers,
                            fishingXp,
                            treeDamage,
                            foodHealth,
                            foodEnergy
                    ));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load item data: " + resourcePath, exception);
        }
    }

    private ToolType parseToolType(String raw) {
        if (raw.equalsIgnoreCase("rod")) {
            return ToolType.FISHING_ROD;
        }
        return ToolType.valueOf(raw.toUpperCase());
    }

    private StatModifiers parseStatModifiers(String[] parts) {
        int bonusDamage = parseOptionalInt(parts, 8);
        double bonusCritChance = parseOptionalDouble(parts, 9);
        double bonusCritMultiplier = parseOptionalDouble(parts, 10);
        int bonusMaxHealth = parseOptionalInt(parts, 11);
        double bonusDefense = parseOptionalDouble(parts, 12);
        double bonusFishingSpeed = parseOptionalDouble(parts, 13);

        if (bonusDamage == 0
                && bonusCritChance == 0
                && bonusCritMultiplier == 0
                && bonusMaxHealth == 0
                && bonusDefense == 0
                && bonusFishingSpeed == 0) {
            return StatModifiers.ZERO;
        }

        return new StatModifiers(
                bonusDamage,
                bonusCritChance,
                bonusCritMultiplier,
                bonusMaxHealth,
                bonusDefense,
                bonusFishingSpeed
        );
    }

    private int parseOptionalInt(String[] parts, int index) {
        if (parts.length <= index || parts[index].trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(parts[index].trim());
    }

    private double parseOptionalDouble(String[] parts, int index) {
        if (parts.length <= index || parts[index].trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(parts[index].trim());
    }
}
