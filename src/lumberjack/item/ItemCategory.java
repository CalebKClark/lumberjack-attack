package lumberjack.item;

/**
 * High-level grouping for items. Used by crafting, shops, and UI filters later.
 */
public enum ItemCategory {

    TOOL,
    WEAPON,
    MATERIAL,
    CONSUMABLE,
    PLACEABLE,
    CURRENCY,
    MISC;

    public String getDisplayName() {
        return switch (this) {
            case TOOL -> "Tool";
            case WEAPON -> "Weapon";
            case MATERIAL -> "Crafting Material";
            case CONSUMABLE -> "Consumable";
            case PLACEABLE -> "Placeable";
            case CURRENCY -> "Currency";
            case MISC -> "Misc";
        };
    }
}
