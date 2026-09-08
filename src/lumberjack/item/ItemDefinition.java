package lumberjack.item;

import java.awt.Color;

import lumberjack.combat.StatModifiers;

/**
 * Immutable template for one item type (e.g. "Oak Log").
 * Loaded from content files — never hardcode item stats in gameplay code.
 */
public final class ItemDefinition {

    private final String id;
    private final String name;
    private final ItemCategory category;
    private final int maxStack;
    private final Color debugColor;
    private final ToolType toolType;
    private final StatModifiers statModifiers;
    private final int fishingXp;
    private final int treeDamage;
    private final int foodHealth;
    private final int foodEnergy;

    public ItemDefinition(
            String id,
            String name,
            ItemCategory category,
            int maxStack,
            Color debugColor,
            ToolType toolType,
            StatModifiers statModifiers,
            int fishingXp,
            int treeDamage,
            int foodHealth,
            int foodEnergy
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.maxStack = maxStack;
        this.debugColor = debugColor;
        this.toolType = toolType;
        this.statModifiers = statModifiers == null ? StatModifiers.ZERO : statModifiers;
        this.fishingXp = fishingXp;
        this.treeDamage = Math.max(0, treeDamage);
        this.foodHealth = Math.max(0, foodHealth);
        this.foodEnergy = Math.max(0, foodEnergy);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public Color getDebugColor() {
        return debugColor;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public boolean isAxe() {
        return toolType == ToolType.AXE;
    }

    public boolean isFishingRod() {
        return toolType == ToolType.FISHING_ROD;
    }

    public boolean isPlaceable() {
        return category == ItemCategory.PLACEABLE;
    }

    public boolean isCurrency() {
        return category == ItemCategory.CURRENCY;
    }

    public StatModifiers getStatModifiers() {
        return statModifiers;
    }

    /** Fishing skill XP granted when this item is caught. */
    public int getFishingXp() {
        return fishingXp;
    }

    /**
     * Hidden chop power against tree toughness. Axes default to
     * {@link lumberjack.core.GameConfig#DEFAULT_AXE_TREE_DAMAGE} when unset in data.
     */
    public int getTreeDamage() {
        if (treeDamage > 0) {
            return treeDamage;
        }
        if (isAxe()) {
            return lumberjack.core.GameConfig.DEFAULT_AXE_TREE_DAMAGE;
        }
        return 0;
    }

    /** Health restored when this item is eaten. */
    public int getFoodHealth() {
        return foodHealth;
    }

    /** Energy restored when this item is eaten. */
    public int getFoodEnergy() {
        return foodEnergy;
    }

    public boolean isEdible() {
        return foodHealth > 0 || foodEnergy > 0;
    }

    /** User-facing type label for tooltips (e.g. "Axe", "Crafting Material"). */
    public String getTypeLabel() {
        if (category == ItemCategory.TOOL && toolType == ToolType.AXE) {
            return "Axe";
        }
        if (category == ItemCategory.TOOL && toolType == ToolType.FISHING_ROD) {
            return "Fishing Rod";
        }
        return category.getDisplayName();
    }
}
