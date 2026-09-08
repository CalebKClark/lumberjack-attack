package lumberjack.crafting;

/**
 * Recipe groupings shown on the crafting tab.
 */
public enum CraftingCategory {

    FORAGING("Foraging"),
    COMBAT("Combat"),
    FISHING("Fishing"),
    MISC("Misc");

    private final String displayName;

    CraftingCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CraftingCategory fromId(String id) {
        for (CraftingCategory category : values()) {
            if (category.name().equalsIgnoreCase(id) || category.displayName.equalsIgnoreCase(id)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown crafting category: " + id);
    }
}
