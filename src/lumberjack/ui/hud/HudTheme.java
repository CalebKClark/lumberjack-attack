package lumberjack.ui.hud;

/**
 * Visual constants for HUD layout. Kept separate so art tweaks don't touch game logic.
 */
public final class HudTheme {

    private HudTheme() {
    }

    public static final int SLOT_SIZE = 64;
    public static final int SLOT_GAP = 8;
    public static final int HOTBAR_BOTTOM_MARGIN = 24;
    public static final int HEALTH_BAR_WIDTH = 200;
    public static final int HEALTH_BAR_HEIGHT = 28;
    public static final int HEALTH_BAR_MARGIN_RIGHT = 24;
    public static final int HEALTH_BAR_MARGIN_BOTTOM = 24;
    /** Gap between stacked health (top) and energy (bottom) bars. */
    public static final int VITAL_BAR_GAP = 6;

    public static final int INVENTORY_PANEL_PADDING = 48;
    public static final int INVENTORY_TITLE_HEIGHT = 48;

    public static final int MENU_PANEL_WIDTH = 920;
    public static final int MENU_PANEL_HEIGHT = 620;
    public static final int MENU_TAB_BAR_WIDTH = 148;

    /** Padding inside a square inventory slot before drawing the icon. */
    public static final int SLOT_ICON_PADDING = 10;
}
