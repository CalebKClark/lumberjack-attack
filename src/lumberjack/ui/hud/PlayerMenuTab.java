package lumberjack.ui.hud;

/**
 * Tabs in the player menu (Stardew-style). Add new entries here as features ship.
 */
public enum PlayerMenuTab {

    INVENTORY("Inventory"),
    SKILLS("Skills"),
    CRAFTING("Crafting"),
    STATS("Stats & Misc"),
    COLLECTIONS("Collections");

    /** Reserved tab slots for future expansion (skills, map, etc.). */
    public static final int MAX_TABS = 8;

    private final String label;

    PlayerMenuTab(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PlayerMenuTab fromIndex(int index) {
        PlayerMenuTab[] tabs = values();
        if (index < 0 || index >= tabs.length) {
            return INVENTORY;
        }
        return tabs[index];
    }

    public static int indexOf(PlayerMenuTab tab) {
        return tab.ordinal();
    }
}
